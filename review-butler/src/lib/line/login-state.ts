import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { getSessionSecret } from "@/lib/env";

export const LINE_NONCE_COOKIE = "rb_line_nonce";
const TTL_MS = 10 * 60 * 1000;

const sign = (body: string) => createHmac("sha256", getSessionSecret()).update(`line-login:${body}`).digest("base64url");

/**
 * LINE Login 的 state：綁定到「哪一家店」，並以 cookie 內的 nonce 防 CSRF／登入固定攻擊。
 * 回呼時必須同時滿足：簽章正確、未過期、nonce 與 cookie 相同。
 */
export function createLoginState(orgId: string, now = Date.now()): { state: string; nonce: string } {
  const nonce = randomBytes(16).toString("base64url");
  const body = Buffer.from(JSON.stringify({ o: orgId, n: nonce, e: now + TTL_MS })).toString("base64url");
  return { state: `${body}.${sign(body)}`, nonce };
}

export function verifyLoginState(state: string | null, cookieNonce: string | undefined, now = Date.now()): { orgId: string } | null {
  if (!state || !cookieNonce) return null;
  const [body, sig] = state.split(".");
  if (!body || !sig) return null;
  const expected = Buffer.from(sign(body));
  const actual = Buffer.from(sig);
  if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) return null;
  try {
    const p = JSON.parse(Buffer.from(body, "base64url").toString()) as { o: string; n: string; e: number };
    if (p.e < now) return null;
    const a = Buffer.from(p.n), b = Buffer.from(cookieNonce);
    if (a.length !== b.length || !timingSafeEqual(a, b)) return null;
    return { orgId: p.o };
  } catch {
    return null;
  }
}
