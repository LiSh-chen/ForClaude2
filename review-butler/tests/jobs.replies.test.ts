import { beforeAll, describe, expect, it } from "vitest";
import { and, eq } from "drizzle-orm";
import type { Db } from "@/lib/db/client";
import { googleCredentials, lineMessages, notifications, replies, reviews, users } from "@/lib/db/schema";
import { checkPublishedReplies } from "@/lib/jobs/check-replies";
import { runTick } from "@/lib/jobs/tick";
import { flushNotifications } from "@/lib/line/notify";
import { publishReply } from "@/lib/publish";
import { syncLocation } from "@/lib/jobs/sync-reviews";
import { makeTestDb } from "./helpers";
import { addAndSync, bindUser, fastBackoff, makeEnv, messagesTo, newGoogleReview, postbacks, setupOrg, uris, type Env } from "./line.helpers";

let db: Db;
beforeAll(async () => { db = await makeTestDb(); }, 60_000);

async function published(env: Env, email: string, lineUser = "U-" + email.split("@")[0]) {
  const org = await setupOrg(env, email);
  await bindUser(env, org.orgId, lineUser);
  const [rev] = await addAndSync(env, org.loc, [newGoogleReview({ starRating: 5 })]);
  const res = await publishReply(db, { reviewId: rev!.id, organizationId: org.orgId, content: "感謝您的支持，歡迎再來。", publishedBy: "line_user", lineUserId: lineUser }, { google: env.google });
  expect(res.status).toBe("published");
  const gr = (await env.google.listReviews({ refreshToken: "x" }, org.loc.googleLocationId, { pageSize: 5 })).reviews.find((r) => r.googleReviewId === rev!.googleReviewId)!;
  return { ...org, rev: rev!, lineUser, gr };
}

describe("發布後檢查回覆是否真的顯示在 Google（F5）", () => {
  it("marks a live reply as verified and stays quiet", async () => {
    const env = makeEnv(db);
    const p = await published(env, "live@example.com");
    const before = env.line.sent.length;
    const r = await checkPublishedReplies(db, { google: env.google });
    expect(r).toMatchObject({ verified: 1, rejected: 0 });
    const [rep] = await db.select().from(replies).where(eq(replies.reviewId, p.rev.id));
    expect(rep!.verifiedAt).not.toBeNull();
    await flushNotifications(db, { line: env.line, backoff: fastBackoff });
    expect(env.line.sent.length).toBe(before);
    // 已確認的不會再檢查
    expect((await checkPublishedReplies(db, { google: env.google })).checked).toBe(0);
  });

  it("notifies once with an edit link when Google rejects the reply, and allows re-publishing", async () => {
    const env = makeEnv(db);
    const p = await published(env, "rejected@example.com");
    p.gr.reply!.state = "rejected";
    const r = await checkPublishedReplies(db, { google: env.google });
    expect(r.rejected).toBe(1);
    const [rev] = await db.select().from(reviews).where(eq(reviews.id, p.rev.id));
    expect(rev!.status).toBe("publish_failed");
    const [rep] = await db.select().from(replies).where(eq(replies.reviewId, p.rev.id));
    expect(rep).toMatchObject({ publishStatus: "rejected" });

    await flushNotifications(db, { line: env.line, backoff: fastBackoff });
    const msgs = messagesTo(env.line, p.lineUser).filter((m) => m.kind === "push");
    const card = msgs.at(-1)!.messages[0]!;
    expect(JSON.stringify(card)).toContain("回覆沒有成功顯示");
    expect(uris(card)[0]).toContain(`/liff/review/${p.rev.id}`);
    const count = msgs.length;

    // 再檢查、再 flush：不會重複通知
    await checkPublishedReplies(db, { google: env.google });
    await flushNotifications(db, { line: env.line, backoff: fastBackoff });
    expect(messagesTo(env.line, p.lineUser).filter((m) => m.kind === "push").length).toBe(count);

    // 修改後重新發布
    p.gr.reply = null;
    const again = await publishReply(db, { reviewId: p.rev.id, organizationId: p.orgId, content: "修改後的回覆內容，謝謝您。", publishedBy: "user" }, { google: env.google });
    expect(again.status).toBe("published");
    const [after] = await db.select().from(replies).where(eq(replies.reviewId, p.rev.id));
    expect(after).toMatchObject({ publishStatus: "published", content: "修改後的回覆內容，謝謝您。", verifiedAt: null });
  });

  it("treats a reply that never appears after 24h as failed, but not before", async () => {
    const env = makeEnv(db);
    const p = await published(env, "never@example.com");
    p.gr.reply = null; // Google 上看不到
    const [rep] = await db.select().from(replies).where(eq(replies.reviewId, p.rev.id));
    const soon = () => new Date(rep!.publishedAt!.getTime() + 3 * 3_600_000);
    const late = () => new Date(rep!.publishedAt!.getTime() + 25 * 3_600_000);
    // 資料庫是整個檔案共用的，所以只檢查「這一則」回覆的狀態
    const status = async () => (await db.select().from(replies).where(eq(replies.reviewId, p.rev.id)))[0]!;
    await checkPublishedReplies(db, { google: env.google, now: soon });
    expect((await status()).publishStatus).toBe("published");
    await checkPublishedReplies(db, { google: env.google, now: late });
    const after = await status();
    expect(after.publishStatus).toBe("rejected");
    expect(after.errorMessage).toContain("24 小時");
  });

  it("ignores replies that already existed on Google", async () => {
    const env = makeEnv(db);
    const org = await setupOrg(env, "external@example.com", 1);
    env.google.addReview(org.loc.googleLocationId, newGoogleReview({ reply: { comment: "原本就有", updateTime: new Date(), state: "live" } }));
    await syncLocation(db, org.loc.id, { google: env.google, llm: env.llm, line: env.line, backoff: fastBackoff });
    expect((await checkPublishedReplies(db, { google: env.google })).checked).toBe(0);
  });
});

describe("授權失效 → LINE 通知（F1 驗收）", () => {
  it("notifies the owner exactly once when authorization is revoked", async () => {
    const env = makeEnv(db);
    const org = await setupOrg(env, "revoke-line@example.com");
    await bindUser(env, org.orgId, "U-rev");
    env.google.revoked = true;
    const later = () => new Date(Date.now() + 31 * 60_000); // 同步最少相隔 30 分鐘
    const out = await runTick(db, { google: env.google, llm: env.llm, line: env.line, backoff: fastBackoff, now: later });
    expect(out.sync.find((s) => s.locationId === org.loc.id)?.error).toBeTruthy();
    const pushes = () => messagesTo(env.line, "U-rev").filter((m) => m.kind === "push");
    expect(pushes()).toHaveLength(1);
    expect(JSON.stringify(pushes()[0]!.messages[0])).toContain("授權已失效");
    const [cred] = await db.select().from(googleCredentials).innerJoin(users, eq(users.id, googleCredentials.userId)).where(eq(users.organizationId, org.orgId));
    expect(cred!.google_credentials.status).toBe("revoked");
    // 之後每次排程都失敗，但不會一直重複通知
    await runTick(db, { google: env.google, llm: env.llm, line: env.line, backoff: fastBackoff, now: later });
    await runTick(db, { google: env.google, llm: env.llm, line: env.line, backoff: fastBackoff, now: later });
    expect(pushes()).toHaveLength(1);
  });
});

describe("runTick", () => {
  it("isolates steps and returns a summary", async () => {
    const env = makeEnv(db);
    const org = await setupOrg(env, "tick@example.com");
    await bindUser(env, org.orgId, "U-tick");
    env.google.addReview(org.loc.googleLocationId, newGoogleReview({ starRating: 4 }));
    const out = await runTick(db, { google: env.google, llm: env.llm, line: env.line, backoff: fastBackoff, now: () => new Date(Date.now() + 31 * 60_000) });
    const mine = out.sync.find((s) => s.locationId === org.loc.id)!;
    expect(mine).toMatchObject({ created: 1, drafted: 1 });
    expect(messagesTo(env.line, "U-tick").filter((m) => m.kind === "push")).toHaveLength(1);
    // 資料庫裡沒有殘留未送出的通知
    const pending = await db.select().from(notifications).where(and(eq(notifications.organizationId, org.orgId)));
    expect(pending.every((n) => n.sentAt !== null)).toBe(true);
    expect((await db.select().from(lineMessages).where(eq(lineMessages.organizationId, org.orgId))).length).toBeGreaterThan(0);
    expect(postbacks(messagesTo(env.line, "U-tick").filter((m) => m.kind === "push")[0]!.messages[0]).length).toBeGreaterThan(0);
  });
});

describe("同步頻率（SPEC F3：每 30 分鐘）", () => {
  it("skips locations synced less than 30 minutes ago, so a per-minute cron does not hammer Google", async () => {
    const env = makeEnv(db);
    const org = await setupOrg(env, "interval@example.com");
    const t0 = Date.now();
    const run = (offsetMin: number) => runTick(db, { google: env.google, llm: env.llm, line: env.line, backoff: fastBackoff, now: () => new Date(t0 + offsetMin * 60_000) });
    const ran = (r: Awaited<ReturnType<typeof run>>) => r.sync.some((s) => s.locationId === org.loc.id);
    expect(ran(await run(5))).toBe(false); // 剛匯入完，5 分鐘後還不用再抓
    expect(ran(await run(31))).toBe(true);
    // 31 分鐘那次同步完成後，lastSyncedAt 是「現在」；再過 1 分鐘（以 t0+32 分鐘看）不會再抓
    const [loc] = await db.select().from((await import("@/lib/db/schema")).locations).where(eq((await import("@/lib/db/schema")).locations.id, org.loc.id));
    expect(Date.now() - loc!.lastSyncedAt!.getTime()).toBeLessThan(60_000);
  });
});
