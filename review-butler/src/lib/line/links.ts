/** 站內網址與 LIFF 連結。 */
export function appBaseUrl(): string {
  return (process.env.APP_BASE_URL ?? "http://localhost:3000").replace(/\/$/, "");
}

/**
 * 在 LINE 內開啟的編輯頁。有設定 LIFF_ID 時用 liff.line.me（在 LINE 裡以 LIFF 開啟並取得身分），
 * 否則（本機模擬）直接指向站內頁面。LIFF Endpoint URL 請設為 `${APP_BASE_URL}/liff`。
 */
export function liffReviewUrl(reviewId: string): string {
  const id = process.env.LIFF_ID;
  return id ? `https://liff.line.me/${id}/review/${reviewId}` : `${appBaseUrl()}/liff/review/${reviewId}`;
}

export const dashboardUrl = () => `${appBaseUrl()}/dashboard`;
export const lineCallbackUrl = () => `${appBaseUrl()}/api/auth/line/callback`;
