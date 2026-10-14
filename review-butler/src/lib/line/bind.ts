import { randomInt } from "node:crypto";
import { and, desc, eq, inArray, isNotNull, isNull, lt, or } from "drizzle-orm";
import type { LineAdapter, LineProfile } from "@/lib/adapters/line/interface";
import type { Db } from "@/lib/db/client";
import { lineBindCodes, lineBindingLocations, lineBindings, locations } from "@/lib/db/schema";

export const BIND_CODE_TTL_MS = 10 * 60 * 1000;

/** 產生 6 位數綁定碼（10 分鐘內、限用一次）。 */
export async function createBindCode(db: Db, organizationId: string, now: Date = new Date()): Promise<{ code: string; expiresAt: Date }> {
  await db.delete(lineBindCodes).where(or(lt(lineBindCodes.expiresAt, now), isNotNull(lineBindCodes.usedAt)));
  const expiresAt = new Date(now.getTime() + BIND_CODE_TTL_MS);
  for (let i = 0; i < 10; i++) {
    const code = String(randomInt(0, 1_000_000)).padStart(6, "0");
    const rows = await db.insert(lineBindCodes).values({ code, organizationId, expiresAt }).onConflictDoNothing().returning();
    if (rows.length) return { code, expiresAt };
  }
  throw new Error("無法產生綁定碼，請再試一次");
}

export type BindResult =
  | { ok: true; bindingId: string; organizationId: string; created: boolean }
  | { ok: false; reason: "invalid" | "expired" };

/** 建立／更新綁定。新綁定預設接收該店家「所有已啟用店點」的通知。 */
export async function upsertBinding(db: Db, organizationId: string, profile: LineProfile): Promise<{ bindingId: string; created: boolean }> {
  const [existing] = await db
    .select()
    .from(lineBindings)
    .where(and(eq(lineBindings.organizationId, organizationId), eq(lineBindings.lineUserId, profile.userId)));
  if (existing) {
    await db.update(lineBindings).set({ displayName: profile.displayName, following: true }).where(eq(lineBindings.id, existing.id));
    return { bindingId: existing.id, created: false };
  }
  const [row] = await db
    .insert(lineBindings)
    .values({ organizationId, lineUserId: profile.userId, displayName: profile.displayName })
    .returning();
  const enabled = await db.select({ id: locations.id }).from(locations).where(and(eq(locations.organizationId, organizationId), eq(locations.enabled, true)));
  if (enabled.length) {
    await db.insert(lineBindingLocations).values(enabled.map((l) => ({ lineBindingId: row!.id, locationId: l.id }))).onConflictDoNothing();
  }
  return { bindingId: row!.id, created: true };
}

/** 以綁定碼綁定（使用者在 LINE 對話框輸入 6 位數字）。碼只能用一次，以單一 UPDATE 原子地「用掉」它。 */
export async function bindWithCode(db: Db, profile: LineProfile, code: string, now: Date = new Date()): Promise<BindResult> {
  const [row] = await db.select().from(lineBindCodes).where(eq(lineBindCodes.code, code));
  if (!row || row.usedAt) return { ok: false, reason: "invalid" };
  if (row.expiresAt <= now) return { ok: false, reason: "expired" };
  const claimed = await db
    .update(lineBindCodes)
    .set({ usedAt: now })
    .where(and(eq(lineBindCodes.code, code), isNull(lineBindCodes.usedAt)))
    .returning();
  if (!claimed.length) return { ok: false, reason: "invalid" };
  const { bindingId, created } = await upsertBinding(db, row.organizationId, profile);
  return { ok: true, bindingId, organizationId: row.organizationId, created };
}

export async function listBindings(db: Db, organizationId: string) {
  const bindings = await db.select().from(lineBindings).where(eq(lineBindings.organizationId, organizationId)).orderBy(lineBindings.createdAt);
  if (!bindings.length) return [];
  const links = await db.select().from(lineBindingLocations).where(inArray(lineBindingLocations.lineBindingId, bindings.map((b) => b.id)));
  return bindings.map((b) => ({ ...b, locationIds: links.filter((l) => l.lineBindingId === b.id).map((l) => l.locationId) }));
}

/** 設定這位使用者接收哪些店點的通知（只接受該店家自己的店點）。 */
export async function setBindingLocations(db: Db, organizationId: string, bindingId: string, locationIds: string[]): Promise<boolean> {
  const [binding] = await db.select().from(lineBindings).where(and(eq(lineBindings.id, bindingId), eq(lineBindings.organizationId, organizationId)));
  if (!binding) return false;
  const own = locationIds.length
    ? await db.select({ id: locations.id }).from(locations).where(and(eq(locations.organizationId, organizationId), inArray(locations.id, locationIds)))
    : [];
  await db.delete(lineBindingLocations).where(eq(lineBindingLocations.lineBindingId, bindingId));
  if (own.length) await db.insert(lineBindingLocations).values(own.map((l) => ({ lineBindingId: bindingId, locationId: l.id })));
  return true;
}

export async function removeBinding(db: Db, organizationId: string, bindingId: string): Promise<boolean> {
  const rows = await db.delete(lineBindings).where(and(eq(lineBindings.id, bindingId), eq(lineBindings.organizationId, organizationId))).returning();
  return rows.length > 0;
}

/** 這個 LINE 使用者在哪些店家有綁定（同一人可能同時是兩家店的老闆）。 */
export async function bindingsForLineUser(db: Db, lineUserId: string) {
  return db.select().from(lineBindings).where(eq(lineBindings.lineUserId, lineUserId));
}

/** 該綁定是否有權操作某店點（綁定存在、屬於同一店家、且有勾選接收該店點）。 */
export async function canAccessLocation(db: Db, bindingId: string, locationId: string): Promise<boolean> {
  const rows = await db
    .select({ id: lineBindingLocations.lineBindingId })
    .from(lineBindingLocations)
    .innerJoin(lineBindings, eq(lineBindings.id, lineBindingLocations.lineBindingId))
    .innerJoin(locations, eq(locations.id, lineBindingLocations.locationId))
    .where(and(eq(lineBindingLocations.lineBindingId, bindingId), eq(lineBindingLocations.locationId, locationId), eq(locations.organizationId, lineBindings.organizationId)));
  return rows.length > 0;
}

/** 傳一則測試訊息給某位綁定者，確認真的收得到。 */
export async function sendTestMessage(db: Db, line: LineAdapter, organizationId: string, bindingId: string): Promise<{ ok: true } | { ok: false; error: string }> {
  const [b] = await db.select().from(lineBindings).where(and(eq(lineBindings.id, bindingId), eq(lineBindings.organizationId, organizationId)));
  if (!b) return { ok: false, error: "找不到這個綁定" };
  try {
    await line.pushMessage(b.lineUserId, [{ type: "text", text: "✅ 這是好評管家的測試訊息。收到這則，代表之後的新評論通知也收得到。" }]);
    return { ok: true };
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) };
  }
}

/** 目前還有效的綁定碼（沒用過、沒過期）。 */
export async function activeBindCode(db: Db, organizationId: string, now: Date = new Date()) {
  const rows = await db.select().from(lineBindCodes).where(and(eq(lineBindCodes.organizationId, organizationId), isNull(lineBindCodes.usedAt))).orderBy(desc(lineBindCodes.createdAt));
  return rows.find((r) => r.expiresAt > now) ?? null;
}
