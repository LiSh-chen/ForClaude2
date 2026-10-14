import { and, eq, or, sql } from "drizzle-orm";
import { getGoogleBusiness } from "@/lib/adapters";
import { AuthRevokedError } from "@/lib/adapters/errors";
import type { GoogleBusinessAdapter } from "@/lib/adapters/google-business/interface";
import type { Db } from "./db/client";
import { locations, replies, reviewAnalyses, reviews } from "./db/schema";
import { getGoogleAuth, markCredentialRevoked } from "./google-auth";
import { withBackoff } from "./retry";

/** Google 回覆長度上限 */
const MAX_REPLY_LENGTH = 4096;
/** 發布流程卡在 pending 超過這段時間，視為程序中斷，允許重新發布 */
const STALE_PENDING_MINUTES = 5;

export type PublishInput = {
  reviewId: string;
  organizationId: string;
  content: string;
  publishedBy: "user" | "line_user";
  lineUserId?: string;
  /** 有風險標記的評論必須由店家明確確認才能發布（SPEC F4 規則 9） */
  confirmRisk?: boolean;
};

export type PublishResult =
  | { status: "published" }
  | { status: "already_published" }
  | { status: "in_progress" }
  | { status: "risk_confirmation_required" }
  | { status: "invalid"; reason: string }
  | { status: "not_found" }
  | { status: "failed"; error: string };

/**
 * 發布回覆到 Google。冪等：同一則評論只會成功發布一次——
 * 以 replies.review_id 的唯一索引搶佔，重複點擊或併發請求不會重複呼叫 Google。
 */
export async function publishReply(db: Db, input: PublishInput, deps: { google?: GoogleBusinessAdapter } = {}): Promise<PublishResult> {
  const google = deps.google ?? getGoogleBusiness();
  const content = input.content.trim();
  if (!content) return { status: "invalid", reason: "回覆內容不可為空" };
  if (content.length > MAX_REPLY_LENGTH) return { status: "invalid", reason: `回覆超過 ${MAX_REPLY_LENGTH} 字上限` };

  // 權限：評論必須屬於該店家
  const [row] = await db
    .select({ review: reviews, location: locations })
    .from(reviews)
    .innerJoin(locations, eq(reviews.locationId, locations.id))
    .where(and(eq(reviews.id, input.reviewId), eq(locations.organizationId, input.organizationId)));
  if (!row) return { status: "not_found" };

  const [analysis] = await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, input.reviewId));
  if ((analysis?.riskFlags.length ?? 0) > 0 && !input.confirmRisk) return { status: "risk_confirmation_required" };

  // 搶佔發布權
  const base = { reviewId: input.reviewId, content, publishStatus: "pending" as const, publishedBy: input.publishedBy, publishedByLineUserId: input.lineUserId ?? null };
  const [claimed] = await db.insert(replies).values(base).onConflictDoNothing().returning();
  let replyId = claimed?.id;
  if (!replyId) {
    const [existing] = await db.select().from(replies).where(eq(replies.reviewId, input.reviewId));
    if (existing?.publishStatus === "published") return { status: "already_published" };
    // 上次失敗／被拒絕、或 pending 卡太久 → 以單一 UPDATE 原子地重新搶佔
    const [reclaimed] = await db
      .update(replies)
      .set({ content, publishStatus: "pending", publishedBy: input.publishedBy, publishedByLineUserId: input.lineUserId ?? null, errorMessage: null, verifiedAt: null, createdAt: new Date() })
      .where(
        and(
          eq(replies.reviewId, input.reviewId),
          or(
            sql`${replies.publishStatus} in ('failed', 'rejected')`,
            and(eq(replies.publishStatus, "pending"), sql`${replies.createdAt} < now() - make_interval(mins => ${STALE_PENDING_MINUTES})`),
          ),
        ),
      )
      .returning();
    if (!reclaimed) return { status: "in_progress" };
    replyId = reclaimed.id;
  }

  try {
    const auth = await getGoogleAuth(db, input.organizationId);
    await withBackoff(() => google.replyToReview(auth, row.location.googleLocationId, row.review.googleReviewId, content));
    await db.update(replies).set({ publishStatus: "published", publishedAt: new Date(), errorMessage: null }).where(eq(replies.id, replyId));
    await db.update(reviews).set({ status: "replied" }).where(eq(reviews.id, input.reviewId));
    return { status: "published" };
  } catch (e) {
    const message = e instanceof AuthRevokedError ? "Google 授權已失效，請重新授權" : e instanceof Error ? e.message : String(e);
    if (e instanceof AuthRevokedError) await markCredentialRevoked(db, input.organizationId);
    await db.update(replies).set({ publishStatus: "failed", errorMessage: message }).where(eq(replies.id, replyId));
    await db.update(reviews).set({ status: "publish_failed" }).where(eq(reviews.id, input.reviewId));
    return { status: "failed", error: message };
  }
}

export async function setReviewSkipped(db: Db, organizationId: string, reviewId: string, skipped: boolean): Promise<boolean> {
  const [row] = await db
    .select({ id: reviews.id, status: reviews.status })
    .from(reviews)
    .innerJoin(locations, eq(reviews.locationId, locations.id))
    .where(and(eq(reviews.id, reviewId), eq(locations.organizationId, organizationId)));
  if (!row || row.status === "replied") return false;
  await db.update(reviews).set({ status: skipped ? "skipped" : "pending" }).where(eq(reviews.id, reviewId));
  return true;
}
