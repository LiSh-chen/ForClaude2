import { createHmac, timingSafeEqual } from "node:crypto";
import type { LineAdapter, LineMessage, LineProfile } from "./interface";

export interface SentLineMessage {
  kind: "push" | "reply";
  target: string;
  messages: LineMessage[];
}

export class MockLineAdapter implements LineAdapter {
  /** 測試／demo 用：記錄所有送出的訊息 */
  readonly sent: SentLineMessage[] = [];

  constructor(private channelSecret = "mock-line-secret") {}

  async pushMessage(to: string, messages: LineMessage[]) {
    this.sent.push({ kind: "push", target: to, messages });
  }

  async replyMessage(replyToken: string, messages: LineMessage[]) {
    this.sent.push({ kind: "reply", target: replyToken, messages });
  }

  async getProfile(userId: string): Promise<LineProfile | null> {
    return { userId, displayName: `Mock User ${userId.slice(-4)}` };
  }

  /** 與 LINE 官方演算法相同：HMAC-SHA256(channelSecret, body) 的 base64 */
  sign(rawBody: string): string {
    return createHmac("sha256", this.channelSecret).update(rawBody).digest("base64");
  }

  verifySignature(rawBody: string, signature: string): boolean {
    const expected = Buffer.from(this.sign(rawBody));
    const actual = Buffer.from(signature);
    return expected.length === actual.length && timingSafeEqual(expected, actual);
  }
}
