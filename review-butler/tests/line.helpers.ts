import { and, eq } from "drizzle-orm";
import { MockGoogleBusinessAdapter } from "@/lib/adapters/google-business/mock";
import type { GoogleReview } from "@/lib/adapters/google-business/interface";
import { MockLineAdapter, type SentLineMessage } from "@/lib/adapters/line/mock";
import { MockLlmAdapter } from "@/lib/adapters/llm/mock";
import type { Db } from "@/lib/db/client";
import { lineBindings, locations, reviews } from "@/lib/db/schema";
import { enableLocation, refreshLocationList } from "@/lib/locations";
import { syncLocation } from "@/lib/jobs/sync-reviews";
import { createBindCode } from "@/lib/line/bind";
import { handleWebhook, type LineCtx } from "@/lib/line/handlers";
import { makeOrg } from "./helpers";

export const fastBackoff = { baseMs: 0, sleep: async () => {} };

export interface Env {
  db: Db;
  google: MockGoogleBusinessAdapter;
  llm: MockLlmAdapter;
  line: MockLineAdapter;
  ctx: LineCtx;
}

export const makeEnv = (db: Db): Env => {
  const google = new MockGoogleBusinessAdapter();
  const llm = new MockLlmAdapter();
  const line = new MockLineAdapter();
  return { db, google, llm, line, ctx: { db, google, llm, line } };
};

/** 建立一家有一個已啟用店點的店（歷史評論已匯入，不會通知）。 */
export async function setupOrg(env: Env, email: string, locIndex = 0) {
  const { orgId, userId } = await makeOrg(env.db, email);
  await refreshLocationList(env.db, orgId, { google: env.google });
  const locs = await env.db.select().from(locations).where(eq(locations.organizationId, orgId));
  const loc = locs[locIndex]!;
  await enableLocation(env.db, orgId, loc.id, { google: env.google, llm: env.llm, notify: false, line: env.line });
  return { orgId, userId, loc, locs };
}

let seq = 0;
export function newGoogleReview(over: Partial<GoogleReview> = {}): GoogleReview {
  const now = new Date();
  return { googleReviewId: `t-${Date.now()}-${seq++}`, starRating: 5, comment: "牛肉麵湯頭很濃郁，店員也很親切！", reviewerDisplayName: "顧客", createTime: now, updateTime: now, reply: null, ...over };
}

/** 加入評論並同步（預設同步後會產生草稿、並推播給已綁定者）。 */
export async function addAndSync(env: Env, loc: typeof locations.$inferSelect, reviewsToAdd: GoogleReview[], opts: { notify?: boolean } = {}) {
  for (const r of reviewsToAdd) env.google.addReview(loc.googleLocationId, r);
  await syncLocation(env.db, loc.id, { google: env.google, llm: env.llm, line: env.line, notify: opts.notify, backoff: fastBackoff });
  const rows = await env.db.select().from(reviews).where(and(eq(reviews.locationId, loc.id)));
  return reviewsToAdd.map((r) => rows.find((x) => x.googleReviewId === r.googleReviewId)!);
}

/** 以綁定碼綁定一位 LINE 使用者（走完整的 webhook 流程）。 */
export async function bindUser(env: Env, orgId: string, lineUserId: string, name = "老闆") {
  env.line.ensureUser(lineUserId, name);
  const { code } = await createBindCode(env.db, orgId);
  await sendText(env, lineUserId, code);
  const [b] = await env.db.select().from(lineBindings).where(and(eq(lineBindings.organizationId, orgId), eq(lineBindings.lineUserId, lineUserId)));
  return b!;
}

let evSeq = 0;
export async function sendEvents(env: Env, events: object[], signature?: string) {
  const raw = JSON.stringify({ destination: "Uxxx", events });
  return handleWebhook(env.ctx, raw, signature ?? env.line.sign(raw));
}
export const sendText = (env: Env, userId: string, text: string) =>
  sendEvents(env, [{ type: "message", webhookEventId: `ev-${Date.now()}-${evSeq++}`, replyToken: env.line.issueReplyToken(userId), source: { type: "user", userId }, message: { type: "text", id: "1", text } }]);
export const sendPostback = (env: Env, userId: string, data: string) =>
  sendEvents(env, [{ type: "postback", webhookEventId: `ev-${Date.now()}-${evSeq++}`, replyToken: env.line.issueReplyToken(userId), source: { type: "user", userId }, postback: { data } }]);

// ---- 訊息檢視 ----
export const messagesTo = (line: MockLineAdapter, userId: string): SentLineMessage[] => line.sent.filter((m) => m.userId === userId);
export const lastTo = (line: MockLineAdapter, userId: string) => messagesTo(line, userId).at(-1);
export const textsOf = (m: SentLineMessage | undefined) => (m?.messages ?? []).map((x) => (x.type === "text" ? String(x.text) : x.type === "flex" ? `[flex] ${String(x.altText)}` : x.type));

/** 找出訊息裡所有 postback 的 data（深度走訪）。 */
export function postbacks(msg: unknown): string[] {
  const out: string[] = [];
  const walk = (n: unknown) => {
    if (Array.isArray(n)) n.forEach(walk);
    else if (n && typeof n === "object") {
      const o = n as Record<string, unknown>;
      if (o.type === "postback" && typeof o.data === "string") out.push(o.data);
      Object.values(o).forEach(walk);
    }
  };
  walk(msg);
  return out;
}
export const uris = (msg: unknown): string[] => {
  const out: string[] = [];
  const walk = (n: unknown) => {
    if (Array.isArray(n)) n.forEach(walk);
    else if (n && typeof n === "object") {
      const o = n as Record<string, unknown>;
      if (o.type === "uri" && typeof o.uri === "string") out.push(o.uri);
      Object.values(o).forEach(walk);
    }
  };
  walk(msg);
  return out;
};
export const bubbleCount = (msg: { type: string; contents?: unknown }) => {
  const c = msg.contents as { type: string; contents?: unknown[] };
  return c.type === "carousel" ? c.contents!.length : 1;
};
