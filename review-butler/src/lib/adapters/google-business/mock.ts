import { AuthRevokedError } from "../errors";
import { generateReviews } from "./fake-reviews";
import type {
  GoogleAuth,
  GoogleBusinessAdapter,
  GoogleLocation,
  GoogleReview,
  GoogleReviewReply,
  ListReviewsParams,
  ListReviewsResult,
} from "./interface";

const DEFAULT_LOCATIONS: GoogleLocation[] = [
  { googleLocationId: "mock-loc-1", name: "阿財牛肉麵（大安店）" },
  { googleLocationId: "mock-loc-2", name: "小森咖啡" },
  { googleLocationId: "mock-loc-3", name: "光之美甲工作室" },
];

export class MockGoogleBusinessAdapter implements GoogleBusinessAdapter {
  private store = new Map<string, GoogleReview[]>();
  /** 測試用：設為 true 模擬授權被撤銷 */
  revoked = false;

  constructor(private locations: GoogleLocation[] = DEFAULT_LOCATIONS, private seed = 42) {}

  private guard(auth: GoogleAuth) {
    if (this.revoked || !auth.refreshToken) throw new AuthRevokedError();
  }

  private reviewsOf(locationId: string): GoogleReview[] {
    let list = this.store.get(locationId);
    if (!list) {
      list = generateReviews({ seed: this.seed, count: 50, idPrefix: locationId });
      this.store.set(locationId, list);
    }
    return list;
  }

  /** 測試用：新增一則新評論（排在最前） */
  addReview(locationId: string, review: GoogleReview) {
    this.reviewsOf(locationId).unshift(review);
  }

  async listLocations(auth: GoogleAuth) {
    this.guard(auth);
    return this.locations;
  }

  async listReviews(auth: GoogleAuth, locationId: string, params: ListReviewsParams = {}): Promise<ListReviewsResult> {
    this.guard(auth);
    const all = this.reviewsOf(locationId);
    const size = params.pageSize ?? 50;
    const start = params.pageToken ? Number(params.pageToken) : 0;
    const reviews = all.slice(start, start + size);
    const next = start + size < all.length ? String(start + size) : undefined;
    return { reviews, nextPageToken: next };
  }

  async replyToReview(auth: GoogleAuth, locationId: string, reviewId: string, comment: string): Promise<GoogleReviewReply> {
    this.guard(auth);
    const review = this.reviewsOf(locationId).find((r) => r.googleReviewId === reviewId);
    if (!review) throw new Error(`Review not found: ${reviewId}`);
    review.reply = { comment, updateTime: new Date(), state: "live" };
    return review.reply;
  }

  async getReview(auth: GoogleAuth, locationId: string, reviewId: string) {
    this.guard(auth);
    return this.reviewsOf(locationId).find((r) => r.googleReviewId === reviewId) ?? null;
  }
}
