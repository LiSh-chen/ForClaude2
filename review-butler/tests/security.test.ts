import { randomBytes } from "node:crypto";
import { describe, expect, it } from "vitest";
import { decrypt, encrypt } from "@/lib/crypto";
import { createSessionToken, verifySessionToken } from "@/lib/session-token";

const key = randomBytes(32);

describe("encrypt / decrypt (refresh token 加密)", () => {
  it("round-trips and never stores plaintext", () => {
    const token = "1//0g-secret-refresh-token";
    const enc = encrypt(token, key);
    expect(enc).not.toContain(token);
    expect(decrypt(enc, key)).toBe(token);
  });
  it("uses a fresh IV every time", () => {
    expect(encrypt("same", key)).not.toBe(encrypt("same", key));
  });
  it("rejects tampering and the wrong key", () => {
    const enc = encrypt("hello", key);
    const parts = enc.split(":");
    parts[3] = Buffer.from("tampered").toString("base64");
    expect(() => decrypt(parts.join(":"), key)).toThrow();
    expect(() => decrypt(enc, randomBytes(32))).toThrow();
    expect(() => decrypt("garbage", key)).toThrow();
  });
});

describe("session token", () => {
  const secret = "test-secret";
  const now = 1_800_000_000_000;
  it("verifies a valid token", () => {
    expect(verifySessionToken(createSessionToken("user-1", now, secret), now + 1000, secret)).toBe("user-1");
  });
  it("rejects expired, tampered, forged and missing tokens", () => {
    const t = createSessionToken("user-1", now, secret);
    expect(verifySessionToken(t, now + 31 * 86_400_000, secret)).toBeNull();
    expect(verifySessionToken(t, now, "other-secret")).toBeNull();
    const [body, sig] = t.split(".");
    const forged = Buffer.from(JSON.stringify({ uid: "admin", exp: 9_999_999_999 })).toString("base64url");
    expect(verifySessionToken(`${forged}.${sig}`, now, secret)).toBeNull();
    expect(verifySessionToken(`${body}.`, now, secret)).toBeNull();
    expect(verifySessionToken(undefined, now, secret)).toBeNull();
    expect(verifySessionToken("a.b.c", now, secret)).toBeNull();
  });
});
