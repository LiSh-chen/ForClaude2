import { describe, expect, it } from "vitest";
import { AuthRevokedError } from "@/lib/adapters/errors";
import { generateReviews } from "@/lib/adapters/google-business/fake-reviews";
import { MockGoogleBusinessAdapter } from "@/lib/adapters/google-business/mock";

const auth = { refreshToken: "t" };

describe("fake reviews", () => {
  const reviews = generateReviews({ seed: 1, count: 300 });
  it("is deterministic for the same seed", () => {
    const now = new Date("2026-10-01T00:00:00Z");
    expect(generateReviews({ seed: 1, count: 20, now })).toEqual(generateReviews({ seed: 1, count: 20, now }));
  });
  it("covers all star ratings, empty comments, and zh/en/ja text", () => {
    expect(new Set(reviews.map((r) => r.starRating))).toEqual(new Set([1, 2, 3, 4, 5]));
    expect(reviews.some((r) => r.comment === null)).toBe(true);
    expect(reviews.some((r) => r.comment && /[一-鿿]/.test(r.comment))).toBe(true);
    expect(reviews.some((r) => r.comment && /^[\x00-\x7f]+$/.test(r.comment))).toBe(true);
    expect(reviews.some((r) => r.comment && /[ぁ-んァ-ン]/.test(r.comment))).toBe(true);
  });
  it("has unique ids", () => {
    expect(new Set(reviews.map((r) => r.googleReviewId)).size).toBe(reviews.length);
  });
});

describe("MockGoogleBusinessAdapter", () => {
  it("lists locations and paginates reviews", async () => {
    const g = new MockGoogleBusinessAdapter();
    const [loc] = await g.listLocations(auth);
    const p1 = await g.listReviews(auth, loc!.googleLocationId, { pageSize: 20 });
    expect(p1.reviews).toHaveLength(20);
    const p2 = await g.listReviews(auth, loc!.googleLocationId, { pageSize: 20, pageToken: p1.nextPageToken });
    expect(p2.reviews[0]!.googleReviewId).not.toBe(p1.reviews[0]!.googleReviewId);
  });
  it("replies to a review and reflects it on getReview", async () => {
    const g = new MockGoogleBusinessAdapter();
    const { reviews } = await g.listReviews(auth, "loc", { pageSize: 1 });
    await g.replyToReview(auth, "loc", reviews[0]!.googleReviewId, "謝謝");
    expect((await g.getReview(auth, "loc", reviews[0]!.googleReviewId))?.reply?.comment).toBe("謝謝");
  });
  it("throws AuthRevokedError when revoked", async () => {
    const g = new MockGoogleBusinessAdapter();
    g.revoked = true;
    await expect(g.listLocations(auth)).rejects.toBeInstanceOf(AuthRevokedError);
  });
});
