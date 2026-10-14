import type { Db } from "@/lib/db/client";
import { checkPublishedReplies, type CheckResult } from "./check-replies";
import { syncAllLocations, type SyncDeps, type SyncResult } from "./sync-reviews";
import { flushNotifications, type FlushResult, type NotifyDeps } from "@/lib/line/notify";

export interface TickResult { sync: SyncResult[]; notifications: FlushResult; replies: CheckResult }

/**
 * 一次排程：抓新評論 → 送出到期的通知（含 10 分鐘合併窗口到期的）→ 檢查已發布回覆的狀態。
 * 由 /api/jobs/tick 呼叫（建議每分鐘一次；Vercel Cron 或之後的 Inngest 都可以）。
 * 各步驟互相隔離：前一步失敗不會讓後面的步驟不跑。
 */
export async function runTick(db: Db, deps: SyncDeps & NotifyDeps = {}): Promise<TickResult> {
  const safe = async <T>(label: string, fn: () => Promise<T>, fallback: T): Promise<T> => {
    try { return await fn(); } catch (e) { console.error(`[tick] ${label} failed:`, e); return fallback; }
  };
  const sync = await safe("sync", () => syncAllLocations(db, deps), [] as SyncResult[]);
  const notifications = await safe("notifications", () => flushNotifications(db, deps), { sent: 0, held: 0, failed: 0 });
  const replies = await safe("replies", () => checkPublishedReplies(db, { google: deps.google, now: deps.now }), { checked: 0, verified: 0, rejected: 0, errors: 0 });
  return { sync, notifications, replies };
}
