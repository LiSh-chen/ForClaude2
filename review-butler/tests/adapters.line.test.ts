import { createHmac } from "node:crypto";
import { describe, expect, it, vi } from "vitest";
import { RateLimitError } from "@/lib/adapters/errors";
import { LineApiError } from "@/lib/adapters/line/interface";
import { MockLineAdapter } from "@/lib/adapters/line/mock";
import { RealLineAdapter, type LineConfig } from "@/lib/adapters/line/real";
import { createLoginState, verifyLoginState } from "@/lib/line/login-state";

describe("MockLineAdapter", () => {
  it("records push and reply messages with their recipient", async () => {
    const line = new MockLineAdapter();
    line.ensureUser("U1", "老闆");
    const tok = line.issueReplyToken("U1");
    await line.pushMessage("U1", [{ type: "text", text: "hi" }]);
    await line.replyMessage(tok, [{ type: "text", text: "yo" }]);
    expect(line.sent.map((m) => [m.kind, m.userId])).toEqual([["push", "U1"], ["reply", "U1"]]);
  });
  it("verifies webhook signatures", () => {
    const line = new MockLineAdapter("secret");
    const body = JSON.stringify({ events: [] });
    expect(line.verifySignature(body, line.sign(body))).toBe(true);
    expect(line.verifySignature(body, "bogus")).toBe(false);
    expect(line.verifySignature(body + " ", line.sign(body))).toBe(false);
  });
  it("accepts a reply token only once, like real LINE", async () => {
    const line = new MockLineAdapter();
    const tok = line.issueReplyToken("U1");
    await line.replyMessage(tok, [{ type: "text", text: "a" }]);
    await expect(line.replyMessage(tok, [{ type: "text", text: "b" }])).rejects.toThrow(/already used/);
  });
  it("de-duplicates pushes that share a retry key", async () => {
    const line = new MockLineAdapter();
    await line.pushMessage("U1", [{ type: "text", text: "a" }], { retryKey: "k1" });
    await line.pushMessage("U1", [{ type: "text", text: "a" }], { retryKey: "k1" });
    expect(line.sent).toHaveLength(1);
  });
  it("simulates login code exchange and id token verification", async () => {
    const line = new MockLineAdapter();
    line.ensureUser("U9", "店長");
    expect(await line.exchangeLoginCode({ code: "mock-code:U9", redirectUri: "x" })).toEqual({ userId: "U9", displayName: "店長" });
    expect(await line.verifyIdToken("mock-id:U9")).toEqual({ userId: "U9", displayName: "店長" });
    expect(await line.verifyIdToken("garbage")).toBeNull();
    await expect(line.exchangeLoginCode({ code: "nope", redirectUri: "x" })).rejects.toThrow();
  });
});

const cfg: LineConfig = { channelSecret: "sekret", channelAccessToken: "TOKEN", loginChannelId: "1234", loginChannelSecret: "LSEC" };

function fakeFetch(responder: (url: string, init: RequestInit) => Response) {
  const calls: { url: string; init: RequestInit }[] = [];
  const fn = vi.fn(async (url: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(url), init: init ?? {} });
    return responder(String(url), init ?? {});
  });
  return { fetch: fn as unknown as typeof fetch, calls };
}
const json = (body: unknown, status = 200, headers: Record<string, string> = {}) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", ...headers } });

describe("RealLineAdapter（以假的 fetch 驗證請求格式；尚未對真實 LINE 實測）", () => {
  it("push sends bearer token, body and the retry key", async () => {
    const f = fakeFetch(() => json({}));
    await new RealLineAdapter(f.fetch, cfg).pushMessage("U1", [{ type: "text", text: "hi" }], { retryKey: "11111111-1111-1111-1111-111111111111" });
    const c = f.calls[0]!;
    expect(c.url).toBe("https://api.line.me/v2/bot/message/push");
    expect(c.init.method).toBe("POST");
    const h = c.init.headers as Record<string, string>;
    expect(h.Authorization).toBe("Bearer TOKEN");
    expect(h["X-Line-Retry-Key"]).toBe("11111111-1111-1111-1111-111111111111");
    expect(JSON.parse(String(c.init.body))).toEqual({ to: "U1", messages: [{ type: "text", text: "hi" }] });
  });
  it("treats 409 (same retry key already accepted) as success, 429 as RateLimitError, others as LineApiError", async () => {
    const mk = (status: number, headers: Record<string, string> = {}) => new RealLineAdapter(fakeFetch(() => json({ message: "x" }, status, headers)).fetch, cfg);
    await expect(mk(409).pushMessage("U", [{ type: "text", text: "a" }], { retryKey: "k" })).resolves.toBeUndefined();
    const rl = await mk(429, { "retry-after": "3" }).pushMessage("U", []).catch((e) => e);
    expect(rl).toBeInstanceOf(RateLimitError);
    expect((rl as RateLimitError).retryAfterMs).toBe(3000);
    const bad = await mk(400).replyMessage("tok", []).catch((e) => e);
    expect(bad).toBeInstanceOf(LineApiError);
    expect((bad as LineApiError).status).toBe(400);
  });
  it("reply and profile use the right endpoints; unknown profile returns null", async () => {
    const f = fakeFetch((url) => (url.includes("/profile/Unone") ? json({}, 404) : json({ userId: "U1", displayName: "王老闆" })));
    const line = new RealLineAdapter(f.fetch, cfg);
    await line.replyMessage("rtok", [{ type: "text", text: "a" }]);
    expect(f.calls[0]!.url).toBe("https://api.line.me/v2/bot/message/reply");
    expect(JSON.parse(String(f.calls[0]!.init.body)).replyToken).toBe("rtok");
    expect(await line.getProfile("U1")).toEqual({ userId: "U1", displayName: "王老闆" });
    expect(f.calls[1]!.url).toBe("https://api.line.me/v2/bot/profile/U1");
    expect(await line.getProfile("Unone")).toBeNull();
  });
  it("verifies webhook signatures with the channel secret (HMAC-SHA256, base64)", () => {
    const line = new RealLineAdapter(fakeFetch(() => json({})).fetch, cfg);
    const body = '{"events":[]}';
    const sig = createHmac("sha256", "sekret").update(body).digest("base64");
    expect(line.verifySignature(body, sig)).toBe(true);
    expect(line.verifySignature(body, createHmac("sha256", "other").update(body).digest("base64"))).toBe(false);
    expect(line.verifySignature(body + "x", sig)).toBe(false);
    expect(line.verifySignature(body, "")).toBe(false);
  });
  it("builds the LINE Login URL (asks to add the bot as a friend) and exchanges the code", async () => {
    const f = fakeFetch((url) => (url.endsWith("/oauth2/v2.1/token") ? json({ access_token: "AT", id_token: "IT" }) : json({ userId: "U7", displayName: "店長" })));
    const line = new RealLineAdapter(f.fetch, cfg);
    const u = new URL(line.buildLoginUrl({ state: "S", redirectUri: "https://app.test/cb" }));
    expect(u.origin + u.pathname).toBe("https://access.line.me/oauth2/v2.1/authorize");
    expect(Object.fromEntries(u.searchParams)).toMatchObject({ response_type: "code", client_id: "1234", redirect_uri: "https://app.test/cb", state: "S", scope: "profile openid", bot_prompt: "aggressive" });
    expect(await line.exchangeLoginCode({ code: "C", redirectUri: "https://app.test/cb" })).toEqual({ userId: "U7", displayName: "店長" });
    const form = new URLSearchParams(String(f.calls[0]!.init.body));
    expect(Object.fromEntries(form)).toMatchObject({ grant_type: "authorization_code", code: "C", client_id: "1234", client_secret: "LSEC" });
    expect((f.calls[1]!.init.headers as Record<string, string>).Authorization).toBe("Bearer AT");
  });
  it("verifies LIFF id tokens against the Login channel; invalid → null", async () => {
    const ok = new RealLineAdapter(fakeFetch(() => json({ sub: "U5", name: "老闆", aud: "1234" })).fetch, cfg);
    expect(await ok.verifyIdToken("tok")).toEqual({ userId: "U5", displayName: "老闆" });
    const bad = fakeFetch(() => json({ error: "invalid_request" }, 400));
    expect(await new RealLineAdapter(bad.fetch, cfg).verifyIdToken("tok")).toBeNull();
    expect(Object.fromEntries(new URLSearchParams(String(bad.calls[0]!.init.body)))).toEqual({ id_token: "tok", client_id: "1234" });
  });
});

describe("LINE Login state（防 CSRF）", () => {
  const now = 1_800_000_000_000;
  it("round-trips for the same cookie nonce", () => {
    const { state, nonce } = createLoginState("org-1", now);
    expect(verifyLoginState(state, nonce, now + 1000)).toEqual({ orgId: "org-1" });
  });
  it("rejects wrong nonce, missing cookie, expiry, tampering and forgery", () => {
    const { state, nonce } = createLoginState("org-1", now);
    expect(verifyLoginState(state, "other-nonce", now)).toBeNull();
    expect(verifyLoginState(state, undefined, now)).toBeNull();
    expect(verifyLoginState(state, nonce, now + 11 * 60_000)).toBeNull();
    const [body, sig] = state.split(".");
    const forged = Buffer.from(JSON.stringify({ o: "org-evil", n: nonce, e: now + 1e9 })).toString("base64url");
    expect(verifyLoginState(`${forged}.${sig}`, nonce, now)).toBeNull();
    expect(verifyLoginState(`${body}.`, nonce, now)).toBeNull();
    expect(verifyLoginState(null, nonce, now)).toBeNull();
  });
});
