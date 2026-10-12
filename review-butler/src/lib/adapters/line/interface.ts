/** LINE message object；Flex Message 等結構以官方文件為準，這裡不重複定義型別。 */
export type LineMessage = { type: string; [key: string]: unknown };

export interface LineProfile {
  userId: string;
  displayName: string;
}

export interface LineAdapter {
  pushMessage(to: string, messages: LineMessage[]): Promise<void>;
  /** 免費：優先使用 reply token（SPEC 6.2） */
  replyMessage(replyToken: string, messages: LineMessage[]): Promise<void>;
  getProfile(userId: string): Promise<LineProfile | null>;
  /** 驗證 webhook 的 x-line-signature；body 必須是未解析的原始字串 */
  verifySignature(rawBody: string, signature: string): boolean;
}
