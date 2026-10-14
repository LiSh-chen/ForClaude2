import { randomUUID } from "node:crypto";
import { asMockLine } from "@/lib/adapters";
import type { MockLineAdapter } from "@/lib/adapters/line/mock";
import type { Db } from "@/lib/db/client";
import { handleWebhook } from "./handlers";

/** 開發用 LINE 模擬器的後端：把「使用者在 LINE 做的事」包成與真實 LINE 一模一樣的、已簽章的 webhook 事件。 */
export const mockLine = (): MockLineAdapter | null => asMockLine();

export interface UserLine { userId: string; text: string; at: Date }
const g = globalThis as unknown as { __lineSimLog?: UserLine[] };
export const userLog = (): UserLine[] => (g.__lineSimLog ??= []);

export async function sendSimEvent(db: Db, userId: string, event: Record<string, unknown>) {
  const line = mockLine();
  if (!line) throw new Error("LINE 模擬器只能在 mock 模式使用");
  const full = { webhookEventId: randomUUID(), replyToken: line.issueReplyToken(userId), source: { type: "user", userId }, timestamp: Date.now(), ...event };
  const raw = JSON.stringify({ destination: "Usimulator", events: [full] });
  return handleWebhook({ db }, raw, line.sign(raw));
}

export function newSimUser(name: string): string {
  const line = mockLine();
  const userId = `U-sim-${randomUUID().slice(0, 8)}`;
  line?.ensureUser(userId, name || "模擬使用者");
  return userId;
}
