import { and, eq, sql } from "drizzle-orm";
import { getPlan, type PlanCode } from "@/config/plans";
import { getGoogleBusiness } from "@/lib/adapters";
import { AuthRevokedError } from "@/lib/adapters/errors";
import type { GoogleBusinessAdapter } from "@/lib/adapters/google-business/interface";
import type { Industry } from "@/config/industries";
import type { Db } from "./db/client";
import { locations, subscriptions } from "./db/schema";
import { getGoogleAuth, markCredentialRevoked } from "./google-auth";
import { syncLocation, type SyncDeps } from "./jobs/sync-reviews";
import { withBackoff } from "./retry";

/** 從 Google 取得該帳號管理的店點，寫入／更新資料庫（新店點預設為未啟用）。 */
export async function refreshLocationList(db: Db, organizationId: string, deps: { google?: GoogleBusinessAdapter } = {}) {
  const google = deps.google ?? getGoogleBusiness();
  try {
    const auth = await getGoogleAuth(db, organizationId);
    const remote = await withBackoff(() => google.listLocations(auth));
    for (const l of remote) {
      await db
        .insert(locations)
        .values({ organizationId, googleLocationId: l.googleLocationId, name: l.name })
        .onConflictDoUpdate({ target: [locations.organizationId, locations.googleLocationId], set: { name: l.name } });
    }
  } catch (e) {
    if (e instanceof AuthRevokedError) await markCredentialRevoked(db, organizationId);
    throw e;
  }
}

export type EnableResult = { ok: true; imported: number } | { ok: false; error: "not_found" | "limit_reached"; limit?: number };

/** 啟用店點（受方案店點數上限限制）。首次啟用會匯入歷史評論，不發通知、不產生草稿。 */
export async function enableLocation(db: Db, organizationId: string, locationId: string, deps: SyncDeps = {}): Promise<EnableResult> {
  const [loc] = await db.select().from(locations).where(and(eq(locations.id, locationId), eq(locations.organizationId, organizationId)));
  if (!loc) return { ok: false, error: "not_found" };
  if (loc.enabled) return { ok: true, imported: 0 };

  const [sub] = await db.select().from(subscriptions).where(eq(subscriptions.organizationId, organizationId));
  const limit = getPlan((sub?.plan ?? "trial") as PlanCode).maxLocations;
  const [countRow] = await db
    .select({ n: sql<number>`count(*)::int` })
    .from(locations)
    .where(and(eq(locations.organizationId, organizationId), eq(locations.enabled, true)));
  if ((countRow?.n ?? 0) >= limit) return { ok: false, error: "limit_reached", limit };

  await db.update(locations).set({ enabled: true }).where(eq(locations.id, locationId));
  const res = await syncLocation(db, locationId, deps);
  return { ok: true, imported: res.imported };
}

export async function disableLocation(db: Db, organizationId: string, locationId: string) {
  await db.update(locations).set({ enabled: false }).where(and(eq(locations.id, locationId), eq(locations.organizationId, organizationId)));
}

export async function setLocationIndustry(db: Db, organizationId: string, locationId: string, industry: Industry) {
  await db.update(locations).set({ industry }).where(and(eq(locations.id, locationId), eq(locations.organizationId, organizationId)));
}
