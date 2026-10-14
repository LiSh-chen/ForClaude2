import { and, eq, gte, isNull, ne } from "drizzle-orm";
import { getGoogleBusiness } from "@/lib/adapters";
import { AuthRevokedError } from "@/lib/adapters/errors";
import type { GoogleBusinessAdapter } from "@/lib/adapters/google-business/interface";
import type { Db } from "@/lib/db/client";
import { locations, replies, reviews } from "@/lib/db/schema";
import { getGoogleAuth, markCredentialRevoked } from "@/lib/google-auth";
import { enqueueReplyRejected } from "@/lib/line/notify";
import { withBackoff } from "@/lib/retry";

/** 只追蹤這段時間內發布的回覆 */
const WATCH_HOURS = 48;
/** 發布後超過這段時間仍沒在 Google 上顯示，視為失敗 */
const GRACE_HOURS = 24;

export interface CheckResult { checked: number; verified: number; rejected: number; errors: number }

/**
 * 發布後檢查回覆是否真的顯示在 Google 上（SPEC F5）：
 * live → 記錄已確認；被拒絕、或超過寬限時間仍沒顯示 → 標記失敗並以 LINE 通知店家，附修改入口。
 * 狀態轉換以條件式 UPDATE 完成，重複執行不會重複通知。
 */
export async function checkPublishedReplies(db: Db, deps: { google?: GoogleBusinessAdapter; now?: () => Date } = {}): Promise<CheckResult> {
  const google = deps.google ?? getGoogleBusiness();
  const now = deps.now?.() ?? new Date();
  const out: CheckResult = { checked: 0, verified: 0, rejected: 0, errors: 0 };
  const rows = await db
    .select({ reply: replies, review: reviews, loc: locations })
    .from(replies)
    .innerJoin(reviews, eq(reviews.id, replies.reviewId))
    .innerJoin(locations, eq(locations.id, reviews.locationId))
    .where(
      and(
        eq(replies.publishStatus, "published"),
        isNull(replies.verifiedAt),
        ne(replies.publishedBy, "external"),
        gte(replies.publishedAt, new Date(now.getTime() - WATCH_HOURS * 3_600_000)),
      ),
    );

  const reject = async (replyId: string, reviewId: string, reason: string) => {
    const moved = await db
      .update(replies)
      .set({ publishStatus: "rejected", errorMessage: reason })
      .where(and(eq(replies.id, replyId), eq(replies.publishStatus, "published")))
      .returning({ id: replies.id });
    if (!moved.length) return;
    await db.update(reviews).set({ status: "publish_failed" }).where(eq(reviews.id, reviewId));
    await enqueueReplyRejected(db, reviewId);
    out.rejected++;
  };

  for (const { reply, review, loc } of rows) {
    out.checked++;
    try {
      const auth = await getGoogleAuth(db, loc.organizationId);
      const remote = await withBackoff(() => google.getReview(auth, loc.googleLocationId, review.googleReviewId));
      const state = remote?.reply?.state;
      if (state === "live") {
        await db.update(replies).set({ verifiedAt: now }).where(eq(replies.id, reply.id));
        out.verified++;
      } else if (state === "rejected") {
        await reject(reply.id, review.id, "Google 審核沒有通過這則回覆");
      } else if (reply.publishedAt && now.getTime() - reply.publishedAt.getTime() > GRACE_HOURS * 3_600_000) {
        await reject(reply.id, review.id, `發布超過 ${GRACE_HOURS} 小時，這則回覆仍沒有顯示在 Google 上`);
      }
    } catch (e) {
      out.errors++;
      if (e instanceof AuthRevokedError) await markCredentialRevoked(db, loc.organizationId);
      else console.error(`[check-replies] review ${review.id}:`, e);
    }
  }
  return out;
}
