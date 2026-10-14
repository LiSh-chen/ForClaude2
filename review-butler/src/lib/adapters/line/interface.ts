/** LINE message object；Flex Message 等結構以官方文件為準，這裡不重複定義型別。 */
export type LineMessage = { type: string; [key: string]: unknown };

export interface LineProfile {
  userId: string;
  displayName: string;
}

export interface PushOptions {
  /** X-Line-Retry-Key（UUID）。同一則訊息重試時用同一個 key，LINE 會擋掉重複發送（409 視為已送達）。 */
  retryKey?: string;
}

export interface LineAdapter {
  pushMessage(to: string, messages: LineMessage[], opts?: PushOptions): Promise<void>;
  /** 免費：優先使用 reply token（SPEC 6.2）。token 僅一次有效、約 1 分鐘內有效。 */
  replyMessage(replyToken: string, messages: LineMessage[]): Promise<void>;
  getProfile(userId: string): Promise<LineProfile | null>;
  /** 驗證 webhook 的 x-line-signature；body 必須是未解析的原始字串 */
  verifySignature(rawBody: string, signature: string): boolean;

  // ---- LINE Login（綁定與 LIFF 身分）----
  /** 產生導向 LINE Login 授權頁的網址（會一併要求加官方帳號好友） */
  buildLoginUrl(params: { state: string; redirectUri: string }): string;
  /** 以授權碼換取使用者身分 */
  exchangeLoginCode(params: { code: string; redirectUri: string }): Promise<LineProfile>;
  /** 驗證 LIFF 取得的 ID token；無效回傳 null */
  verifyIdToken(idToken: string): Promise<LineProfile | null>;
}

export class LineApiError extends Error {
  constructor(message: string, public status?: number) {
    super(message);
    this.name = "LineApiError";
  }
}
