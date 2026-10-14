import { and, eq, isNull, lt, or, sql } from "drizzle-orm";
import { getGoogleBusiness } from "@/lib/adapters";
import { AuthRevokedError } from "@/lib/adapters/errors";
import type { GoogleBusinessAdapter, GoogleReview } from "@/lib/adapters/google-business/interface";
import type { LlmAdapter } from "@/lib/adapters/llm/interface";
import { processNewReview } from "@/lib/ai/pipeline";
import type { Db } from "@/lib/db/client";
import { locations, replies, reviews } from "@/lib/db/schema";
import { getGoogleAuth, markCredentialRevoked } from "@/lib/google-auth";
import { enqueueNewReview, flushNotifications } from "@/lib/line/notify";
import type { LineAdapter } from "@/lib/adapters/line/interface";
import { withBackoff, type BackoffOptions } from "@/lib/retry";

/** 首次啟用店點時匯入的歷史評論數（SPEC F3） */
export const INITIAL_IMPORT_COUNT = 50;
/** 增量同步最多往回翻幾頁（防止異常情況下無限翻頁） */
const MAX_PAGES = 5;
const PAGE_SIZE = 50;

export interface SyncDeps {
  google?: GoogleBusinessAdapter;
  llm?: LlmAdapter;
  backoff?: BackoffOptions;
  /** 是否在同步後對新評論產生 AI 草稿（預設 true） */
  draft?: boolean;
  /** 是否在同步後推播 LINE 通知（預設 true） */
  notify?: boolean;
  line?: LineAdapter;
  now?: () => Date;
  /** syncAllLocations 的最小輪詢間隔（預設 30 分鐘，SPEC F3）。排程可以每分鐘呼叫，不會每分鐘打 Google。 */
  minIntervalMs?: number;
}

export const SYNC_INTERVAL_MS = 30 * 60 * 1000;

export interface SyncResult {
  locationId: string;
  imported: number;
  created: number;
  updated: number;
  drafted: number;
  error?: string;
}

type ReviewRow = typeof reviews.$inferSelect;

/** 寫入或更新一則評論。回傳 'created' | 'updated' | 'unchanged'。 */
async function upsertReview(
  db: Db,
  locationId: string,
  r: GoogleReview,
  historical: boolean,
  /** 增量同步時：比已知最舊評論還舊、且資料庫沒有的，是首次匯入（上限 50 則）之外的更早評論，不當成新評論 */
  oldestKnown: Date | null = null,
): Promise<{ kind: "created" | "updated" | "unchanged"; id: string }> {
  const [existing] = await db
    .select()
    .from(reviews)
    .where(and(eq(reviews.locationId, locationId), eq(reviews.googleReviewId, r.googleReviewId)));

  if (!existing) {
    if (oldestKnown && r.createTime < oldestKnown) return { kind: "unchanged", id: "" };
    const [row] = await db
      .insert(reviews)
      .values({
        googleReviewId: r.googleReviewId,
        locationId,
        starRating: r.starRating,
        comment: r.comment,
        reviewerDisplayName: r.reviewerDisplayName,
        createTime: r.createTime,
        updateTime: r.updateTime,
        isHistorical: historical,
        status: r.reply ? "replied" : "pending",
      })
      // 併發同步時以唯一索引擋下重複，不會建立同一則評論兩次
      .onConflictDoNothing()
      .returning();
    if (!row) return { kind: "unchanged", id: "" };
    if (r.reply) await recordExternalReply(db, row.id, r.reply.comment, r.reply.updateTime);
    return { kind: "created", id: row.id };
  }

  const changed = existing.comment !== r.comment || existing.starRating !== r.starRating;
  const patch: Partial<ReviewRow> = {};
  if (changed) Object.assign(patch, { comment: r.comment, starRating: r.starRating, updateTime: r.updateTime, isEdited: true });
  // 顧客沒改、但店家在 Google 後台（或別的工具）已回覆 → 同步成已回覆
  if (r.reply && existing.status === "pending") {
    patch.status = "replied";
    await recordExternalReply(db, existing.id, r.reply.comment, r.reply.updateTime);
  }
  if (Object.keys(patch).length === 0) return { kind: "unchanged", id: existing.id };
  await db.update(reviews).set(patch).where(eq(reviews.id, existing.id));
  return { kind: changed ? "updated" : "unchanged", id: existing.id };
}

async function recordExternalReply(db: Db, reviewId: string, content: string, at: Date) {
  await db
    .insert(replies)
    .values({ reviewId, content, publishedAt: at, publishStatus: "published", publishedBy: "external" })
    .onConflictDoNothing();
}

/** 首次啟用：匯入最近 N 則，標為歷史評論，不產生草稿、不通知。 */
async function importInitial(db: Db, google: GoogleBusinessAdapter, deps: SyncDeps, loc: typeof locations.$inferSelect): Promise<number> {
  const auth = await getGoogleAuth(db, loc.organizationId);
  let imported = 0;
  let pageToken: string | undefined;
  do {
    const page = await withBackoff(() => google.listReviews(auth, loc.googleLocationId, { pageSize: PAGE_SIZE, pageToken }), deps.backoff);
    for (const r of page.reviews) {
      if (imported >= INITIAL_IMPORT_COUNT) break;
      const res = await upsertReview(db, loc.id, r, true);
      if (res.kind === "created") imported++;
    }
    pageToken = imported < INITIAL_IMPORT_COUNT ? page.nextPageToken : undefined;
  } while (pageToken);
  await db.update(locations).set({ initialImportDone: true, lastSyncedAt: new Date() }).where(eq(locations.id, loc.id));
  return imported;
}

/** 同步單一店點。授權失效時標記並拋出 AuthRevokedError；其他錯誤也會拋出，由呼叫端隔離。 */
export async function syncLocation(db: Db, locationId: string, deps: SyncDeps = {}): Promise<SyncResult> {
  const google = deps.google ?? getGoogleBusiness();
  const [loc] = await db.select().from(locations).where(eq(locations.id, locationId));
  if (!loc) throw new Error(`Location not found: ${locationId}`);
  const result: SyncResult = { locationId, imported: 0, created: 0, updated: 0, drafted: 0 };

  try {
    if (!loc.initialImportDone) {
      result.imported = await importInitial(db, google, deps, loc);
      return result;
    }

    const auth = await getGoogleAuth(db, loc.organizationId);
    const [known] = await db.select({ oldest: sql<Date | null>`min(${reviews.createTime})` }).from(reviews).where(eq(reviews.locationId, loc.id));
    const oldestKnown = known?.oldest ? new Date(known.oldest) : null;
    const newIds: string[] = [];
    let pageToken: string | undefined;
    for (let page = 0; page < MAX_PAGES; page++) {
      const res = await withBackoff(() => google.listReviews(auth, loc.googleLocationId, { pageSize: PAGE_SIZE, pageToken }), deps.backoff);
      let touched = 0;
      for (const r of res.reviews) {
        const u = await upsertReview(db, loc.id, r, false, oldestKnown);
        if (u.kind === "created") { result.created++; newIds.push(u.id); touched++; }
        else if (u.kind === "updated") { result.updated++; touched++; }
      }
      // 這一頁全都是已知且沒變的評論 → 更舊的也不會有新東西，停止翻頁
      if (touched === 0 || !res.nextPageToken) break;
      pageToken = res.nextPageToken;
    }
    await db.update(locations).set({ lastSyncedAt: new Date() }).where(eq(locations.id, loc.id));

    if (deps.draft !== false) {
      for (const id of newIds) {
        try {
          const r = await processNewReview(db, id, { llm: deps.llm });
          if (r?.status === "ok") result.drafted++;
        } catch (e) {
          // 單一評論的 AI 失敗不影響其他評論；該則會停留在「待處理、尚無草稿」，店家可手動處理
          console.error(`[sync] AI processing failed for review ${id}:`, e);
        }
      }
    }

    if (deps.notify !== false) {
      try {
        // 不論 AI 有沒有寫出草稿都要通知：額度用完或 AI 失敗時，卡片會提示「請自己回覆」
        for (const id of newIds) await enqueueNewReview(db, id);
        await flushNotifications(db, { line: deps.line, locationId, backoff: deps.backoff });
      } catch (e) {
        console.error(`[sync] LINE notification failed for location ${locationId}:`, e);
      }
    }
    return result;
  } catch (e) {
    if (e instanceof AuthRevokedError) await markCredentialRevoked(db, loc.organizationId);
    throw e;
  }
}

/** 同步所有啟用中的店點；每個店點獨立 try/catch，單點失敗不影響其他（SPEC F3 驗收）。 */
export async function syncAllLocations(db: Db, deps: SyncDeps = {}): Promise<SyncResult[]> {
  const now = deps.now?.() ?? new Date();
  const due = new Date(now.getTime() - (deps.minIntervalMs ?? SYNC_INTERVAL_MS));
  const active = await db
    .select({ id: locations.id })
    .from(locations)
    .where(and(eq(locations.enabled, true), or(isNull(locations.lastSyncedAt), lt(locations.lastSyncedAt, due))));
  const out: SyncResult[] = [];
  for (const { id } of active) {
    try {
      out.push(await syncLocation(db, id, deps));
    } catch (e) {
      out.push({ locationId: id, imported: 0, created: 0, updated: 0, drafted: 0, error: e instanceof Error ? e.message : String(e) });
    }
  }
  return out;
}
