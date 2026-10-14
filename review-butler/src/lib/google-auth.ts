import { and, eq } from "drizzle-orm";
import { AuthRevokedError } from "@/lib/adapters/errors";
import type { GoogleAuth } from "@/lib/adapters/google-business/interface";
import { decrypt } from "./crypto";
import type { Db } from "./db/client";
import { googleCredentials, users } from "./db/schema";

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

/** 偵測到授權失效：標記後台提示（LINE 通知在階段 2 加入）。 */
export async function markCredentialRevoked(db: Db, organizationId: string): Promise<void> {
  const owners = await db.select({ id: users.id }).from(users).where(eq(users.organizationId, organizationId));
  for (const u of owners) {
    await db
      .update(googleCredentials)
      .set({ status: "revoked", updatedAt: new Date() })
      .where(and(eq(googleCredentials.userId, u.id), eq(googleCredentials.status, "active")));
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
