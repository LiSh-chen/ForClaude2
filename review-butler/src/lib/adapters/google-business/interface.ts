export interface GoogleAuth {
  /** 解密後的 refresh token；實作負責換發 access token */
  refreshToken: string;
}

export interface GoogleLocation {
  googleLocationId: string;
  name: string;
}

export interface GoogleReviewReply {
  comment: string;
  updateTime: Date;
  /** Google 審核後是否正常顯示；用於發布後的狀態檢查（F5） */
  state: "pending" | "live" | "rejected";
}

export interface GoogleReview {
  googleReviewId: string;
  starRating: 1 | 2 | 3 | 4 | 5;
  /** 只有星等、沒有文字時為 null */
  comment: string | null;
  reviewerDisplayName: string;
  createTime: Date;
  updateTime: Date;
  reply: GoogleReviewReply | null;
}

export interface ListReviewsParams {
  pageSize?: number;
  pageToken?: string;
}

export interface ListReviewsResult {
  reviews: GoogleReview[];
  nextPageToken?: string;
}

/**
 * Google Business Profile 抽象介面。錯誤約定：
 * - 授權失效 → 拋 AuthRevokedError
 * - 速率限制 → 拋 RateLimitError
 */
export interface GoogleBusinessAdapter {
  listLocations(auth: GoogleAuth): Promise<GoogleLocation[]>;
  listReviews(auth: GoogleAuth, googleLocationId: string, params?: ListReviewsParams): Promise<ListReviewsResult>;
  /** 發布（或覆寫）店家回覆 */
  replyToReview(auth: GoogleAuth, googleLocationId: string, googleReviewId: string, comment: string): Promise<GoogleReviewReply>;
  getReview(auth: GoogleAuth, googleLocationId: string, googleReviewId: string): Promise<GoogleReview | null>;
}
