import { createHmac, timingSafeEqual } from "node:crypto";
import { RateLimitError } from "../errors";
import { LineApiError, type LineAdapter, type LineMessage, type LineProfile, type PushOptions } from "./interface";

const API = "https://api.line.me";

export interface LineConfig {
  channelSecret: string;
  channelAccessToken: string;
  loginChannelId: string;
  loginChannelSecret: string;
}

export function lineConfigFromEnv(env: NodeJS.ProcessEnv = process.env): LineConfig {
  const need = (k: string) => {
    const v = env[k];
    if (!v) throw new Error(`${k} is not set`);
    return v;
  };
  return {
    channelSecret: need("LINE_CHANNEL_SECRET"),
    channelAccessToken: need("LINE_CHANNEL_ACCESS_TOKEN"),
    loginChannelId: need("LINE_LOGIN_CHANNEL_ID"),
    loginChannelSecret: need("LINE_LOGIN_CHANNEL_SECRET"),
  };
}

/**
 * 真實 LINE（Messaging API + LINE Login v2.1）。
 * 端點與欄位依 LINE 官方文件撰寫；尚未以真實 channel 實測，上線前請用測試帳號走一遍（README 有清單）。
 */
export class RealLineAdapter implements LineAdapter {
  private _config?: LineConfig;

  constructor(private fetchImpl: typeof fetch = fetch, config?: LineConfig) {
    this._config = config;
  }

  private get config(): LineConfig {
    return (this._config ??= lineConfigFromEnv());
  }

  private async call(path: string, init: RequestInit & { json?: unknown; form?: Record<string, string>; auth?: string }): Promise<Response> {
    const headers: Record<string, string> = { ...(init.headers as Record<string, string> | undefined) };
    if (init.auth) headers.Authorization = `Bearer ${init.auth}`;
    let body: BodyInit | undefined;
    if (init.json !== undefined) { headers["Content-Type"] = "application/json"; body = JSON.stringify(init.json); }
    if (init.form) { headers["Content-Type"] = "application/x-www-form-urlencoded"; body = new URLSearchParams(init.form).toString(); }
    return this.fetchImpl(`${API}${path}`, { method: init.method ?? "GET", headers, body });
  }

  private async fail(res: Response, what: string): Promise<never> {
    if (res.status === 429) {
      const retryAfter = Number(res.headers.get("retry-after"));
      throw new RateLimitError(Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter * 1000 : undefined);
    }
    const detail = await res.text().catch(() => "");
    throw new LineApiError(`LINE ${what} failed (${res.status}) ${detail.slice(0, 300)}`, res.status);
  }

  async pushMessage(to: string, messages: LineMessage[], opts: PushOptions = {}): Promise<void> {
    const headers: Record<string, string> = {};
    if (opts.retryKey) headers["X-Line-Retry-Key"] = opts.retryKey;
    const res = await this.call("/v2/bot/message/push", { method: "POST", auth: this.config.channelAccessToken, headers, json: { to, messages } });
    // 409：相同 retry key 的請求已被接受，視為成功，不可再重送
    if (res.ok || res.status === 409) return;
    await this.fail(res, "push");
  }

  async replyMessage(replyToken: string, messages: LineMessage[]): Promise<void> {
    const res = await this.call("/v2/bot/message/reply", { method: "POST", auth: this.config.channelAccessToken, json: { replyToken, messages } });
    if (!res.ok) await this.fail(res, "reply");
  }

  async getProfile(userId: string): Promise<LineProfile | null> {
    const res = await this.call(`/v2/bot/profile/${encodeURIComponent(userId)}`, { auth: this.config.channelAccessToken });
    if (res.status === 404) return null;
    if (!res.ok) await this.fail(res, "profile");
    const p = (await res.json()) as { userId: string; displayName: string };
    return { userId: p.userId, displayName: p.displayName };
  }

  verifySignature(rawBody: string, signature: string): boolean {
    const expected = createHmac("sha256", this.config.channelSecret).update(rawBody).digest();
    let actual: Buffer;
    try { actual = Buffer.from(signature, "base64"); } catch { return false; }
    return actual.length === expected.length && timingSafeEqual(actual, expected);
  }

  buildLoginUrl({ state, redirectUri }: { state: string; redirectUri: string }): string {
    const q = new URLSearchParams({
      response_type: "code",
      client_id: this.config.loginChannelId,
      redirect_uri: redirectUri,
      state,
      scope: "profile openid",
      // 授權時一併提示加入官方帳號好友（需在 LINE Developers Console 將官方帳號連結到 Login channel）
      bot_prompt: "aggressive",
    });
    return `https://access.line.me/oauth2/v2.1/authorize?${q}`;
  }

  async exchangeLoginCode({ code, redirectUri }: { code: string; redirectUri: string }): Promise<LineProfile> {
    const tokenRes = await this.call("/oauth2/v2.1/token", {
      method: "POST",
      form: { grant_type: "authorization_code", code, redirect_uri: redirectUri, client_id: this.config.loginChannelId, client_secret: this.config.loginChannelSecret },
    });
    if (!tokenRes.ok) await this.fail(tokenRes, "token exchange");
    const { access_token } = (await tokenRes.json()) as { access_token: string };
    const res = await this.call("/v2/profile", { auth: access_token });
    if (!res.ok) await this.fail(res, "login profile");
    const p = (await res.json()) as { userId: string; displayName: string };
    return { userId: p.userId, displayName: p.displayName };
  }

  async verifyIdToken(idToken: string): Promise<LineProfile | null> {
    const res = await this.call("/oauth2/v2.1/verify", { method: "POST", form: { id_token: idToken, client_id: this.config.loginChannelId } });
    if (res.status === 400) return null; // 無效或過期
    if (!res.ok) await this.fail(res, "verify id token");
    const p = (await res.json()) as { sub: string; name?: string };
    return { userId: p.sub, displayName: p.name ?? "" };
  }
}
