import { beforeAll, describe, expect, it } from "vitest";
import { eq } from "drizzle-orm";
import { MockGoogleBusinessAdapter } from "@/lib/adapters/google-business/mock";
import { MockLlmAdapter } from "@/lib/adapters/llm/mock";
import type { Db } from "@/lib/db/client";
import { locations } from "@/lib/db/schema";
import { enableLocation, refreshLocationList } from "@/lib/locations";
import { syncLocation } from "@/lib/jobs/sync-reviews";
import { getReviewDetail, listReviews, PAGE_SIZE, reviewCounts } from "@/lib/queries";
import { makeOrg, makeTestDb } from "./helpers";

let db: Db;
let orgId: string;
let otherOrgId: string;
let locId: string;
let reviewId: string;
const google = new MockGoogleBusinessAdapter();
const llm = new MockLlmAdapter();

beforeAll(async () => {
  db = await makeTestDb();
  ({ orgId } = await makeOrg(db, "q1@example.com"));
  ({ orgId: otherOrgId } = await makeOrg(db, "q2@example.com"));
  await refreshLocationList(db, orgId, { google });
  const [loc] = await db.select().from(locations).where(eq(locations.organizationId, orgId));
  locId = loc!.id;
  await enableLocation(db, orgId, locId, { google, llm });
  const now = new Date();
  google.addReview(loc!.googleLocationId, { googleReviewId: "fresh", starRating: 1, comment: "湯是冷的", reviewerDisplayName: "A", createTime: now, updateTime: now, reply: null });
  await syncLocation(db, locId, { google, llm, backoff: { sleep: async () => {} } });
}, 60_000);

describe("review queries", () => {
  it("separates historical from new reviews and paginates", async () => {
    const hist = await listReviews(db, orgId, { historical: true });
    expect(hist.items).toHaveLength(PAGE_SIZE);
    expect(hist.hasMore).toBe(true);
    const page3 = await listReviews(db, orgId, { historical: true, page: 3 });
    expect(page3.items.length).toBe(50 - 2 * PAGE_SIZE - 0 >= 0 ? Math.min(PAGE_SIZE, 50 - 2 * PAGE_SIZE) : 0);
    const fresh = await listReviews(db, orgId, { historical: false });
    expect(fresh.items).toHaveLength(1);
    expect(fresh.items[0]!.hasDraft).toBe(true);
    reviewId = fresh.items[0]!.review.id;
  });

  it("filters by stars, status and location, and counts per status", async () => {
    expect((await listReviews(db, orgId, { stars: 1, historical: false })).items).toHaveLength(1);
    expect((await listReviews(db, orgId, { stars: 5, historical: false })).items).toHaveLength(0);
    expect((await listReviews(db, orgId, { status: "replied" })).items.every((i) => i.review.status === "replied")).toBe(true);
    const { counts, historyPending } = await reviewCounts(db, orgId, {});
    expect(counts.pending).toBe(1);
    expect(historyPending).toBeGreaterThan(0);
  });

  it("never leaks another organization's reviews", async () => {
    expect((await listReviews(db, otherOrgId, {})).items).toHaveLength(0);
    expect(await getReviewDetail(db, otherOrgId, reviewId)).toBeNull();
    const own = await getReviewDetail(db, orgId, reviewId);
    expect(own?.drafts).toHaveLength(1);
    expect(own?.analysis?.sentiment).toBe("negative");
  });
});
