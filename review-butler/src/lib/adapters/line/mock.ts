import { createHmac, randomUUID, timingSafeEqual } from "node:crypto";
import type { LineAdapter, LineMessage, LineProfile, PushOptions } from "./interface";

export interface SentLineMessage {
  kind: "push" | "reply";
  /** push：收件者 userId；reply：reply token */
  target: string;
  /** 這則訊息屬於哪個 LINE 使用者（reply 由 token 對應回來）；模擬器據此畫出對話 */
  userId?: string;
  messages: LineMessage[];
  at: Date;
}

/**
 * 假的 LINE：記錄所有送出的訊息，並提供簽章、Login 與 reply token 的模擬，
 * 讓 webhook、綁定、通知流程在沒有真實 channel 時也能完整跑通（見 /dev/line 模擬器）。
 */
export class MockLineAdapter implements LineAdapter {
  readonly sent: SentLineMessage[] = [];
  readonly users = new Map<string, string>(); // userId -> displayName
  private tokens = new Map<string, { userId: string; used: boolean }>();
  private retryKeys = new Set<string>();
  /** 測試用：設為 true 時，push 會丟錯（模擬 LINE 暫時故障） */
  failPush = false;

  constructor(private channelSecret = "mock-line-secret") {}

  ensureUser(userId: string, displayName: string) {
    this.users.set(userId, displayName);
  }

  /** 模擬器：發一個 reply token 給某位使用者（真實 LINE 會隨 webhook 事件附上） */
  issueReplyToken(userId: string): string {
    const token = `rt-${randomUUID()}`;
    this.tokens.set(token, { userId, used: false });
    return token;
  }

  async pushMessage(to: string, messages: LineMessage[], opts: PushOptions = {}) {
    if (this.failPush) throw new Error("LINE push failed (mock)");
    if (opts.retryKey) {
      if (this.retryKeys.has(opts.retryKey)) return; // 與真實 LINE 一致：相同 retry key 不重送
      this.retryKeys.add(opts.retryKey);
    }
    this.sent.push({ kind: "push", target: to, userId: to, messages, at: new Date() });
  }

  async replyMessage(replyToken: string, messages: LineMessage[]) {
    const t = this.tokens.get(replyToken);
    // 真實 LINE：reply token 只能用一次；未知或已用過的 token 會被拒絕
    if (t?.used) throw new Error("Invalid reply token (already used)");
    if (t) t.used = true;
    this.sent.push({ kind: "reply", target: replyToken, userId: t?.userId, messages, at: new Date() });
  }

  async getProfile(userId: string): Promise<LineProfile | null> {
    return { userId, displayName: this.users.get(userId) ?? `Mock User ${userId.slice(-4)}` };
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

  buildLoginUrl({ state, redirectUri }: { state: string; redirectUri: string }): string {
    return `/dev/line/login?${new URLSearchParams({ state, redirect_uri: redirectUri })}`;
  }

  async exchangeLoginCode({ code }: { code: string; redirectUri?: string }): Promise<LineProfile> {
    const userId = code.replace(/^mock-code:/, "");
    if (!code.startsWith("mock-code:") || !userId) throw new Error("Invalid mock login code");
    return { userId, displayName: this.users.get(userId) ?? userId };
  }

  async verifyIdToken(idToken: string): Promise<LineProfile | null> {
    if (!idToken.startsWith("mock-id:")) return null;
    const userId = idToken.slice("mock-id:".length);
    return userId ? { userId, displayName: this.users.get(userId) ?? userId } : null;
  }
}
