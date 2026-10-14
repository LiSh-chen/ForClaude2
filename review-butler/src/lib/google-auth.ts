import { and, eq } from "drizzle-orm";
import { AuthRevokedError } from "@/lib/adapters/errors";
import type { GoogleAuth } from "@/lib/adapters/google-business/interface";
import { decrypt } from "./crypto";
import type { Db } from "./db/client";
import { googleCredentials, users } from "./db/schema";
import { enqueueAuthRevoked } from "./line/notify";

/** 取得該店家帳號的 Google 授權（解密 refresh token）。授權已失效時拋 AuthRevokedError。 */
export async function getGoogleAuth(db: Db, organizationId: string): Promise<GoogleAuth> {
  const [row] = await db
    .select({ enc: googleCredentials.refreshTokenEncrypted, status: googleCredentials.status })
    .from(googleCredentials)
    .innerJoin(users, eq(users.id, googleCredentials.userId))
    .where(eq(users.organizationId, organizationId));
  if (!row) throw new AuthRevokedError("尚未連結 Google 帳號");
  if (row.status !== "active") throw new AuthRevokedError();
  return { refreshToken: decrypt(row.enc) };
}

/** 偵測到授權失效：標記（後台顯示提示），並在「首次」失效時排入 LINE 通知（F1 驗收）。 */
export async function markCredentialRevoked(db: Db, organizationId: string): Promise<void> {
  const owners = await db.select({ id: users.id }).from(users).where(eq(users.organizationId, organizationId));
  let transitioned = false;
  for (const u of owners) {
    const rows = await db
      .update(googleCredentials)
      .set({ status: "revoked", updatedAt: new Date() })
      .where(and(eq(googleCredentials.userId, u.id), eq(googleCredentials.status, "active")))
      .returning({ id: googleCredentials.id });
    transitioned ||= rows.length > 0;
  }
  if (!transitioned) return; // 已經是失效狀態，不重複通知
  try {
    // 只排入佇列；由排程（runTick）統一送出，這樣送出時走的是同一套 adapter 與重試邏輯
    await enqueueAuthRevoked(db, organizationId);
  } catch (e) {
    console.error("[google-auth] revoke notification failed:", e);
  }
}

export async function getCredentialStatus(db: Db, organizationId: string): Promise<"active" | "revoked" | "expired" | "missing"> {
  const [row] = await db
    .select({ status: googleCredentials.status })
    .from(googleCredentials)
    .innerJoin(users, eq(users.id, googleCredentials.userId))
    .where(eq(users.organizationId, organizationId));
  return row?.status ?? "missing";
}
