import { NotImplementedError } from "../errors";
import type { GoogleBusinessAdapter } from "./interface";

/** 階段 3：待取得 Business Profile API 權限後實作。端點以官方最新文件為準。 */
export class RealGoogleBusinessAdapter implements GoogleBusinessAdapter {
  listLocations(): never { throw new NotImplementedError("GoogleBusiness.listLocations"); }
  listReviews(): never { throw new NotImplementedError("GoogleBusiness.listReviews"); }
  replyToReview(): never { throw new NotImplementedError("GoogleBusiness.replyToReview"); }
  getReview(): never { throw new NotImplementedError("GoogleBusiness.getReview"); }
}
