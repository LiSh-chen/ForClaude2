import { and, desc, eq, sql, type SQL } from "drizzle-orm";
import type { Db } from "./db/client";
import { locations, replies, replyDrafts, reviewAnalyses, reviews } from "./db/schema";

export const PAGE_SIZE = 20;
export type ReviewStatus = "pending" | "replied" | "skipped" | "publish_failed";
export const STATUS_LABEL: Record<ReviewStatus, string> = { pending: "待處理", replied: "已回覆", skipped: "已略過", publish_failed: "發布失敗" };

export interface ReviewFilters {
  locationId?: string;
  stars?: number;
  status?: ReviewStatus;
  /** true：只看歷史評論；false：排除歷史評論 */
  historical?: boolean;
  page?: number;
}

function conditions(orgId: string, f: ReviewFilters): SQL[] {
  const c: SQL[] = [eq(locations.organizationId, orgId)];
  if (f.locationId) c.push(eq(reviews.locationId, f.locationId));
  if (f.stars) c.push(eq(reviews.starRating, f.stars));
  if (f.status) c.push(eq(reviews.status, f.status));
  if (f.historical !== undefined) c.push(eq(reviews.isHistorical, f.historical));
  return c;
}

export async function listReviews(db: Db, orgId: string, f: ReviewFilters) {
  const page = Math.max(1, f.page ?? 1);
  const rows = await db
    .select({
      review: reviews,
      locationName: locations.name,
      sentiment: reviewAnalyses.sentiment,
      riskFlags: reviewAnalyses.riskFlags,
      hasDraft: sql<boolean>`exists (select 1 from ${replyDrafts} where ${replyDrafts.reviewId} = ${reviews.id})`,
    })
    .from(reviews)
    .innerJoin(locations, eq(reviews.locationId, locations.id))
    .leftJoin(reviewAnalyses, eq(reviewAnalyses.reviewId, reviews.id))
    .where(and(...conditions(orgId, f)))
    .orderBy(desc(reviews.createTime))
    .limit(PAGE_SIZE + 1)
    .offset((page - 1) * PAGE_SIZE);
  return { items: rows.slice(0, PAGE_SIZE), hasMore: rows.length > PAGE_SIZE, page };
}

/** 各狀態的數量（不含歷史評論），以及尚未處理的歷史評論數。 */
export async function reviewCounts(db: Db, orgId: string, f: Pick<ReviewFilters, "locationId" | "stars">) {
  const base = conditions(orgId, { ...f });
  const rows = await db
    .select({ status: reviews.status, historical: reviews.isHistorical, n: sql<number>`count(*)::int` })
    .from(reviews)
    .innerJoin(locations, eq(reviews.locationId, locations.id))
    .where(and(...base))
    .groupBy(reviews.status, reviews.isHistorical);
  const counts: Record<ReviewStatus, number> = { pending: 0, replied: 0, skipped: 0, publish_failed: 0 };
  let historyPending = 0;
  for (const r of rows) {
    if (r.historical) { if (r.status === "pending") historyPending += r.n; }
    else counts[r.status] += r.n;
  }
  return { counts, historyPending };
}

export async function getReviewDetail(db: Db, orgId: string, reviewId: string) {
  const [row] = await db
    .select({ review: reviews, location: locations, analysis: reviewAnalyses })
    .from(reviews)
    .innerJoin(locations, eq(reviews.locationId, locations.id))
    .leftJoin(reviewAnalyses, eq(reviewAnalyses.reviewId, reviews.id))
    .where(and(eq(reviews.id, reviewId), eq(locations.organizationId, orgId)));
  if (!row) return null;
  const drafts = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, reviewId)).orderBy(desc(replyDrafts.version));
  const [reply] = await db.select().from(replies).where(eq(replies.reviewId, reviewId));
  return { ...row, drafts, reply: reply ?? null };
}
