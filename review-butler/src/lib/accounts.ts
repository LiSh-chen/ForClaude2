import { eq } from "drizzle-orm";
import { getPlan } from "@/config/plans";
import { encrypt } from "./crypto";
import type { Db } from "./db/client";
import { brandSettings, googleCredentials, organizations, subscriptions, users } from "./db/schema";

export interface GoogleAccountInfo {
  googleSub: string;
  email: string;
  name?: string | null;
  refreshToken: string;
  scopes?: string;
}

/**
 * 登入／註冊的共用入口（mock 與正式 Google OAuth 都走這裡）。
 * 新用戶會建立 organization、品牌設定預設值、14 天試用訂閱；refresh token 以 AES-GCM 加密後存入。
 */
export async function upsertGoogleAccount(db: Db, info: GoogleAccountInfo, now: Date = new Date()): Promise<string> {
  return db.transaction(async (tx) => {
    const [existing] = await tx.select().from(users).where(eq(users.googleSub, info.googleSub));
    let userId: string;
    if (existing) {
      userId = existing.id;
    } else {
      const [org] = await tx.insert(organizations).values({ name: `${info.email.split("@")[0]} 的店家` }).returning();
      const [user] = await tx
        .insert(users)
        .values({ googleSub: info.googleSub, email: info.email, name: info.name ?? null, organizationId: org!.id })
        .returning();
      userId = user!.id;
      await tx.insert(brandSettings).values({ organizationId: org!.id });
      const trial = getPlan("trial");
      await tx.insert(subscriptions).values({
        organizationId: org!.id,
        plan: "trial",
        status: "trialing",
        trialEndsAt: new Date(now.getTime() + (trial.trialDays ?? 14) * 86_400_000),
      });
    }
    const values = { refreshTokenEncrypted: encrypt(info.refreshToken), scopes: info.scopes ?? null, status: "active" as const, updatedAt: now };
    await tx
      .insert(googleCredentials)
      .values({ userId, ...values })
      .onConflictDoUpdate({ target: googleCredentials.userId, set: values });
    return userId;
  });
}

/** 開發用：不經 Google，直接以 email 建立／登入一個模擬帳號。 */
export function signInMock(db: Db, email: string): Promise<string> {
  const e = email.trim().toLowerCase();
  return upsertGoogleAccount(db, { googleSub: `mock:${e}`, email: e, name: e.split("@")[0], refreshToken: "mock-refresh-token" });
}
