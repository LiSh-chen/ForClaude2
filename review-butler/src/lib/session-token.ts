import { createHmac, timingSafeEqual } from "node:crypto";
import { getSessionSecret } from "./env";

export const SESSION_COOKIE = "rb_session";
const MAX_AGE_SEC = 60 * 60 * 24 * 30;

interface Payload { uid: string; exp: number }

const sign = (body: string, secret: string) => createHmac("sha256", secret).update(body).digest("base64url");

export function createSessionToken(userId: string, now = Date.now(), secret = getSessionSecret()): string {
  const body = Buffer.from(JSON.stringify({ uid: userId, exp: Math.floor(now / 1000) + MAX_AGE_SEC } satisfies Payload)).toString("base64url");
  return `${body}.${sign(body, secret)}`;
}

/** 驗證簽章與效期；成功回傳 userId。 */
export function verifySessionToken(token: string | undefined, now = Date.now(), secret = getSessionSecret()): string | null {
  if (!token) return null;
  const [body, sig] = token.split(".");
  if (!body || !sig) return null;
  const expected = Buffer.from(sign(body, secret));
  const actual = Buffer.from(sig);
  if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) return null;
  try {
    const p = JSON.parse(Buffer.from(body, "base64url").toString()) as Payload;
    return p.exp * 1000 > now ? p.uid : null;
  } catch {
    return null;
  }
}

export const SESSION_MAX_AGE_SEC = MAX_AGE_SEC;
