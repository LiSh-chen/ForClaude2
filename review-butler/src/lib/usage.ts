import { and, eq, sql } from "drizzle-orm";
import { getPlan, type PlanCode } from "@/config/plans";
import type { Db } from "./db/client";
import { subscriptions, usageCounters } from "./db/schema";

/** 以 Asia/Taipei 計算的月份鍵（YYYY-MM），額度以台灣時間月份重置。 */
export function monthKey(date: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Taipei", year: "numeric", month: "2-digit" }).formatToParts(date);
  const get = (t: string) => parts.find((p) => p.type === t)!.value;
  return `${get("year")}-${get("month")}`;
}

export interface Quota {
  plan: PlanCode;
  /** 方案基本額度（不含管理員加給） */
  base: number;
  /** 本月可用總額度 = base + 加給 */
  limit: number;
  used: number;
  remaining: number;
}

export async function getQuota(db: Db, organizationId: string, now: Date = new Date()): Promise<Quota> {
  const [sub] = await db.select().from(subscriptions).where(eq(subscriptions.organizationId, organizationId));
  const plan = (sub?.plan ?? "trial") as PlanCode;
  const [row] = await db
    .select()
    .from(usageCounters)
    .where(and(eq(usageCounters.organizationId, organizationId), eq(usageCounters.month, monthKey(now))));
  const base = getPlan(plan).monthlyDrafts;
  const limit = base + (row?.draftsBonus ?? 0);
  const used = row?.draftsUsed ?? 0;
  return { plan, base, limit, used, remaining: Math.max(0, limit - used) };
}

/**
 * 預先扣 1 則額度。以單一 SQL（upsert + 條件）保證併發下不會超額。
 * 失敗（額度已滿）回傳 false；AI 呼叫最終失敗時請呼叫 releaseDraft 歸還。
 */
export async function reserveDraft(db: Db, organizationId: string, now: Date = new Date()): Promise<boolean> {
  const { base, limit } = await getQuota(db, organizationId, now);
  if (limit <= 0) return false;
  const rows = await db
    .insert(usageCounters)
    .values({ organizationId, month: monthKey(now), draftsUsed: 1 })
    .onConflictDoUpdate({
      target: [usageCounters.organizationId, usageCounters.month],
      set: { draftsUsed: sql`${usageCounters.draftsUsed} + 1` },
      setWhere: sql`${usageCounters.draftsUsed} < ${base} + ${usageCounters.draftsBonus}`,
    })
    .returning({ used: usageCounters.draftsUsed });
  return rows.length > 0;
}

export async function releaseDraft(db: Db, organizationId: string, now: Date = new Date()): Promise<void> {
  await db
    .update(usageCounters)
    .set({ draftsUsed: sql`greatest(${usageCounters.draftsUsed} - 1, 0)` })
    .where(and(eq(usageCounters.organizationId, organizationId), eq(usageCounters.month, monthKey(now))));
}
