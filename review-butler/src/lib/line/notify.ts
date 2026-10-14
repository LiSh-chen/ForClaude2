import { randomUUID } from "node:crypto";
import { and, desc, eq, inArray, isNull, sql, type SQL } from "drizzle-orm";
import { getLine } from "@/lib/adapters";
import type { LineAdapter, LineMessage } from "@/lib/adapters/line/interface";
import type { Db } from "@/lib/db/client";
import { lineBindingLocations, lineBindings, lineMessages, locations, notifications, replies, replyDrafts, reviewAnalyses, reviews } from "@/lib/db/schema";
import { withBackoff, type BackoffOptions } from "@/lib/retry";
import { isBatchEligible, MESSAGES, newReviewsMessage, replyRejectedMessage, textMessage, type BubbleInput } from "./flex";

/** 同一店點在這段時間內只推播一次，期間內累積的評論合併成一則（SPEC F5）。第一則通知不延遲。 */
export const MERGE_WINDOW_MS = 10 * 60 * 1000;

export interface NotifyDeps {
  line?: LineAdapter;
  now?: () => Date;
  backoff?: BackoffOptions;
  /** 只處理這個店點的新評論通知（同步完成後立刻送，不必等排程） */
  locationId?: string;
}

// ---------- 排入佇列 ----------

/** 新評論 → 排入通知。歷史評論、已回覆的評論不通知；重複排入會被忽略。 */
export async function enqueueNewReview(db: Db, reviewId: string): Promise<boolean> {
  const [row] = await db
    .select({ review: reviews, orgId: locations.organizationId })
    .from(reviews)
    .innerJoin(locations, eq(locations.id, reviews.locationId))
    .where(eq(reviews.id, reviewId));
  if (!row || row.review.isHistorical || row.review.status !== "pending") return false;
  const dup = await db.select({ id: notifications.id }).from(notifications).where(and(eq(notifications.reviewId, reviewId), eq(notifications.kind, "new_review")));
  if (dup.length) return false;
  await db.insert(notifications).values({ organizationId: row.orgId, locationId: row.review.locationId, reviewId, kind: "new_review" });
  return true;
}

export async function enqueueReplyRejected(db: Db, reviewId: string): Promise<void> {
  const [row] = await db
    .select({ locationId: reviews.locationId, orgId: locations.organizationId })
    .from(reviews)
    .innerJoin(locations, eq(locations.id, reviews.locationId))
    .where(eq(reviews.id, reviewId));
  if (row) await db.insert(notifications).values({ organizationId: row.orgId, locationId: row.locationId, reviewId, kind: "reply_rejected" });
}

export async function enqueueAuthRevoked(db: Db, organizationId: string): Promise<void> {
  await db.insert(notifications).values({ organizationId, kind: "auth_revoked" });
}

// ---------- 收件者 ----------

async function recipientsForLocation(db: Db, locationId: string) {
  return db
    .select({ binding: lineBindings })
    .from(lineBindingLocations)
    .innerJoin(lineBindings, eq(lineBindings.id, lineBindingLocations.lineBindingId))
    .where(and(eq(lineBindingLocations.locationId, locationId), eq(lineBindings.following, true)))
    .then((r) => r.map((x) => x.binding));
}

const recipientsForOrg = (db: Db, orgId: string) =>
  db.select().from(lineBindings).where(and(eq(lineBindings.organizationId, orgId), eq(lineBindings.following, true)));

// ---------- 組裝卡片所需資料 ----------

export async function bubbleInput(db: Db, reviewId: string): Promise<(BubbleInput & { createTime: Date; status: string }) | null> {
  const [row] = await db
    .select({ review: reviews, loc: locations, analysis: reviewAnalyses })
    .from(reviews)
    .innerJoin(locations, eq(locations.id, reviews.locationId))
    .leftJoin(reviewAnalyses, eq(reviewAnalyses.reviewId, reviews.id))
    .where(eq(reviews.id, reviewId));
  if (!row) return null;
  const [draft] = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, reviewId)).orderBy(desc(replyDrafts.version)).limit(1);
  return {
    locationName: row.loc.name,
    review: { id: reviewId, stars: row.review.starRating, comment: row.review.comment, riskFlags: row.analysis?.riskFlags ?? [] },
    draft: draft ? { id: draft.id, content: draft.content } : null,
    aiStatus: row.review.aiStatus,
    createTime: row.review.createTime,
    status: row.review.status,
  };
}

// ---------- 送出 ----------

interface SendCtx { db: Db; line: LineAdapter; backoff?: BackoffOptions }

async function deliver(
  c: SendCtx,
  binding: typeof lineBindings.$inferSelect,
  meta: { id: string; kind: "new_review" | "reply_rejected" | "auth_revoked"; locationId: string | null; items: { reviewId: string; draftId: string | null }[] },
  message: LineMessage,
): Promise<boolean> {
  let error: string | null = null;
  try {
    // 以訊息 id 當 retry key：重試不會讓使用者收到兩次
    await withBackoff(() => c.line.pushMessage(binding.lineUserId, [message], { retryKey: meta.id }), c.backoff);
  } catch (e) {
    error = e instanceof Error ? e.message : String(e);
  }
  await c.db.insert(lineMessages).values({
    id: meta.id,
    organizationId: binding.organizationId,
    bindingId: binding.id,
    locationId: meta.locationId,
    kind: meta.kind,
    items: meta.items,
    status: error ? "failed" : "sent",
    error,
  });
  if (!error && meta.items[0]) await c.db.update(lineBindings).set({ lastReviewId: meta.items[0].reviewId }).where(eq(lineBindings.id, binding.id));
  return !error;
}

async function claim(db: Db, where: SQL, now: Date) {
  return db.update(notifications).set({ sentAt: now }).where(and(isNull(notifications.sentAt), where)).returning();
}

export interface FlushResult { sent: number; held: number; failed: number }

/**
 * 送出佇列中的通知。可重複、併發呼叫：
 * - 以單一 UPDATE 認領待送通知，同一筆不會被兩個流程各送一次
 * - 新評論：同一店點 10 分鐘內只推播一次，期間累積的合併成一則（輪播）
 * - 全部收件者都失敗時，把通知放回佇列，下次排程再試
 */
export async function flushNotifications(db: Db, deps: NotifyDeps = {}): Promise<FlushResult> {
  const c: SendCtx = { db, line: deps.line ?? getLine(), backoff: deps.backoff };
  const now = deps.now?.() ?? new Date();
  const out: FlushResult = { sent: 0, held: 0, failed: 0 };

  // 1) 帳號層級與單則型（立即送，不合併）
  if (!deps.locationId) {
    for (const n of await claim(db, inArray(notifications.kind, ["reply_rejected", "auth_revoked"]), now)) {
      const ok = n.kind === "auth_revoked" ? await sendAuthRevoked(c, n.organizationId) : await sendReplyRejected(c, n);
      if (ok === null) continue; // 沒有收件者
      if (ok) out.sent++; else { out.failed++; await db.update(notifications).set({ sentAt: null }).where(eq(notifications.id, n.id)); }
    }
  }

  // 2) 新評論：依店點合併
  const pending = await db
    .selectDistinct({ locationId: notifications.locationId })
    .from(notifications)
    .where(and(eq(notifications.kind, "new_review"), isNull(notifications.sentAt), deps.locationId ? eq(notifications.locationId, deps.locationId) : sql`true`));
  for (const { locationId } of pending) {
    if (!locationId) continue;
    const [last] = await db
      .select({ at: sql<Date | null>`max(${lineMessages.createdAt})` })
      .from(lineMessages)
      .where(and(eq(lineMessages.locationId, locationId), eq(lineMessages.kind, "new_review"), eq(lineMessages.status, "sent")));
    if (last?.at && now.getTime() - new Date(last.at).getTime() < MERGE_WINDOW_MS) { out.held++; continue; }

    const claimed = await claim(db, and(eq(notifications.locationId, locationId), eq(notifications.kind, "new_review"))!, now);
    if (!claimed.length) continue;
    const loaded = (await Promise.all(claimed.map((n) => bubbleInput(db, n.reviewId!)))).filter((x): x is NonNullable<typeof x> => x !== null && x.status === "pending");
    if (!loaded.length) continue;
    loaded.sort((a, b) => b.createTime.getTime() - a.createTime.getTime());

    const recipients = await recipientsForLocation(db, locationId);
    if (!recipients.length) continue; // 沒人綁定：通知作廢（之後才綁定的人不需要收到舊通知）
    let anyOk = false;
    for (const binding of recipients) {
      const id = randomUUID();
      const message = newReviewsMessage({ locationName: loaded[0]!.locationName, items: loaded, messageId: id });
      const ok = await deliver(c, binding, { id, kind: "new_review", locationId, items: loaded.map((l) => ({ reviewId: l.review.id, draftId: l.draft?.id ?? null })) }, message);
      anyOk ||= ok;
    }
    if (anyOk) out.sent++;
    else { out.failed++; await db.update(notifications).set({ sentAt: null }).where(inArray(notifications.id, claimed.map((n) => n.id))); }
  }
  return out;
}

async function sendAuthRevoked(c: SendCtx, orgId: string): Promise<boolean | null> {
  const recipients = await recipientsForOrg(c.db, orgId);
  if (!recipients.length) return null;
  let anyOk = false;
  for (const b of recipients) anyOk = (await deliver(c, b, { id: randomUUID(), kind: "auth_revoked", locationId: null, items: [] }, textMessage(MESSAGES.authRevoked))) || anyOk;
  return anyOk;
}

async function sendReplyRejected(c: SendCtx, n: typeof notifications.$inferSelect): Promise<boolean | null> {
  if (!n.reviewId || !n.locationId) return null;
  const input = await bubbleInput(c.db, n.reviewId);
  if (!input) return null;
  const [reply] = await c.db.select().from(replies).where(eq(replies.reviewId, n.reviewId));
  const recipients = await recipientsForLocation(c.db, n.locationId);
  if (!recipients.length) return null;
  const message = replyRejectedMessage({ locationName: input.locationName, reviewId: n.reviewId, stars: input.review.stars, reason: reply?.errorMessage ?? "Google 沒有顯示這則回覆" });
  let anyOk = false;
  for (const b of recipients) anyOk = (await deliver(c, b, { id: randomUUID(), kind: "reply_rejected", locationId: n.locationId, items: [{ reviewId: n.reviewId, draftId: null }] }, message)) || anyOk;
  return anyOk;
}

export { isBatchEligible };
