import { beforeAll, describe, expect, it } from "vitest";
import { and, eq } from "drizzle-orm";
import type { Db } from "@/lib/db/client";
import { aiUsageLogs, lineBindings, lineMessages, notifications, replies, replyDrafts, reviewAnalyses, reviews } from "@/lib/db/schema";
import { BIND_CODE_TTL_MS, bindWithCode, createBindCode, setBindingLocations } from "@/lib/line/bind";
import { MERGE_WINDOW_MS, enqueueNewReview, flushNotifications } from "@/lib/line/notify";
import { decodePostback, encodePostback, MAX_BUBBLES, MESSAGES, newReviewsMessage } from "@/lib/line/flex";
import { periodStart } from "@/lib/line/handlers";
import { getQuota } from "@/lib/usage";
import { makeTestDb } from "./helpers";
import { addAndSync, bindUser, bubbleCount, fastBackoff, lastTo, makeEnv, messagesTo, newGoogleReview, postbacks, sendEvents, sendPostback, sendText, setupOrg, textsOf, uris, type Env } from "./line.helpers";

let db: Db;
let env: Env;
beforeAll(async () => { db = await makeTestDb(); env = makeEnv(db); }, 60_000);

describe("webhook 簽章與事件", () => {
  it("rejects a bad or missing signature, bad JSON, and accepts LINE's empty verification ping", async () => {
    expect((await sendEvents(env, [], "bogus")).status).toBe(401);
    expect((await sendEvents(env, [], "")).status).toBe(401);
    expect((await sendEvents(env, [])).status).toBe(200);
    const raw = "{not json";
    const { handleWebhook } = await import("@/lib/line/handlers");
    expect((await handleWebhook(env.ctx, raw, env.line.sign(raw))).status).toBe(400);
  });

  it("greets a new follower with binding instructions, and a bound user with a welcome back", async () => {
    await sendEvents(env, [{ type: "follow", webhookEventId: "f1", replyToken: env.line.issueReplyToken("U-new"), source: { type: "user", userId: "U-new" } }]);
    expect(textsOf(lastTo(env.line, "U-new"))[0]).toBe(MESSAGES.welcome);
    const { orgId } = await setupOrg(env, "follow@example.com");
    await bindUser(env, orgId, "U-follow");
    await sendEvents(env, [{ type: "follow", webhookEventId: "f2", replyToken: env.line.issueReplyToken("U-follow"), source: { type: "user", userId: "U-follow" } }]);
    expect(textsOf(lastTo(env.line, "U-follow"))[0]).toBe(MESSAGES.welcomeBack);
  });

  it("ignores redelivered events (same webhookEventId)", async () => {
    const ev = { type: "message", webhookEventId: "dup-1", replyToken: env.line.issueReplyToken("U-dup"), source: { type: "user", userId: "U-dup" }, message: { type: "text", text: "說明" } };
    const before = env.line.sent.length;
    await sendEvents(env, [ev]);
    await sendEvents(env, [{ ...ev, replyToken: env.line.issueReplyToken("U-dup") }]);
    expect(env.line.sent.length - before).toBe(1);
  });

  it("falls back to push when the reply token was already used", async () => {
    const token = env.line.issueReplyToken("U-tok");
    await env.line.replyMessage(token, [{ type: "text", text: "x" }]);
    const before = env.line.sent.length;
    await sendEvents(env, [{ type: "message", webhookEventId: "tok-1", replyToken: token, source: { type: "user", userId: "U-tok" }, message: { type: "text", text: "說明" } }]);
    expect(env.line.sent.slice(before).map((m) => m.kind)).toEqual(["push"]);
  });
});

describe("F2 綁定", () => {
  it("binds with a one-time code, defaults to all enabled locations, and rejects reuse", async () => {
    const { orgId, loc } = await setupOrg(env, "bind1@example.com");
    const b = await bindUser(env, orgId, "U-bind1");
    expect(textsOf(lastTo(env.line, "U-bind1"))[0]).toContain("綁定成功");
    const links = await db.query.lineBindingLocations.findMany({ where: (t, { eq }) => eq(t.lineBindingId, b.id) });
    expect(links.map((l) => l.locationId)).toEqual([loc.id]);
    // 同一個碼不能再用
    const { code } = await createBindCode(db, orgId);
    expect((await bindWithCode(db, { userId: "U-a", displayName: "A" }, code)).ok).toBe(true);
    expect(await bindWithCode(db, { userId: "U-b", displayName: "B" }, code)).toEqual({ ok: false, reason: "invalid" });
  });

  it("rejects wrong and expired codes", async () => {
    const { orgId } = await setupOrg(env, "bind2@example.com");
    await sendText(env, "U-bind2", "999999");
    expect(textsOf(lastTo(env.line, "U-bind2"))[0]).toBe(MESSAGES.bindInvalid);
    const t0 = new Date("2026-10-01T00:00:00Z");
    const { code } = await createBindCode(db, orgId, t0);
    const late = new Date(t0.getTime() + BIND_CODE_TTL_MS + 1000);
    expect(await bindWithCode(db, { userId: "U-late", displayName: "" }, code, late)).toEqual({ ok: false, reason: "expired" });
  });

  it("an unbound user gets guidance instead of any data", async () => {
    await sendText(env, "U-stranger", "待處理");
    expect(textsOf(lastTo(env.line, "U-stranger"))[0]).toBe(MESSAGES.notBound);
  });

  it("supports multiple LINE users per organization with per-location choices", async () => {
    const { orgId, locs } = await setupOrg(env, "multi@example.com");
    const boss = await bindUser(env, orgId, "U-boss");
    const manager = await bindUser(env, orgId, "U-manager");
    expect(await setBindingLocations(db, orgId, manager.id, [])).toBe(true);
    expect(await setBindingLocations(db, "00000000-0000-0000-0000-000000000000", manager.id, [locs[0]!.id])).toBe(false);
    const rows = await db.select().from(lineBindings).where(eq(lineBindings.organizationId, orgId));
    expect(rows.map((r) => r.lineUserId).sort()).toEqual(["U-boss", "U-manager"]);
  });
});

describe("F5 通知與一鍵發布", () => {
  it("pushes a Flex card with draft and the four buttons after a new review is synced", async () => {
    const { orgId, loc } = await setupOrg(env, "notify1@example.com");
    await bindUser(env, orgId, "U-n1");
    const [rev] = await addAndSync(env, loc, [newGoogleReview({ starRating: 2, comment: "等了四十分鐘才上菜，態度也不太好。" })]);
    const push = messagesTo(env.line, "U-n1").filter((m) => m.kind === "push").at(-1)!;
    const flex = push.messages[0]!;
    expect(flex.type).toBe("flex");
    const datas = postbacks(flex).map(decodePostback);
    expect(datas.map((d) => d?.a).sort()).toEqual(["publish", "regen", "skip"]);
    expect(uris(flex)[0]).toContain(`/liff/review/${rev!.id}`);
    expect(JSON.stringify(flex)).toContain("建議回覆");
    const [draft] = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id));
    expect(JSON.stringify(flex)).toContain(draft!.content.slice(0, 20));
  });

  it("does not notify for historical reviews on first enable", async () => {
    const env2 = makeEnv(db);
    const { orgId, loc } = await setupOrg(env2, "hist@example.com");
    await bindUser(env2, orgId, "U-hist");
    const hist = await db.select().from(reviews).where(eq(reviews.locationId, loc.id));
    expect(hist.length).toBe(50);
    expect(messagesTo(env2.line, "U-hist").filter((m) => m.kind === "push")).toHaveLength(0);
  });

  it("one tap publishes exactly once; a second tap is harmless", async () => {
    const { orgId, loc } = await setupOrg(env, "tap@example.com");
    await bindUser(env, orgId, "U-tap");
    const [rev] = await addAndSync(env, loc, [newGoogleReview({ starRating: 5 })]);
    const pub = postbacks(messagesTo(env.line, "U-tap").filter((m) => m.kind === "push").at(-1)!.messages[0]).find((d) => d.startsWith("a=publish"))!;
    let calls = 0;
    const real = env.google.replyToReview.bind(env.google);
    env.google.replyToReview = async (...a) => { calls++; return real(...a); };
    await Promise.all([sendPostback(env, "U-tap", pub), sendPostback(env, "U-tap", pub)]);
    await sendPostback(env, "U-tap", pub);
    expect(calls).toBe(1);
    env.google.replyToReview = real;
    const texts = messagesTo(env.line, "U-tap").filter((m) => m.kind === "reply").flatMap(textsOf);
    expect(texts.some((t) => t.includes("已發布到 Google"))).toBe(true);
    expect(texts.some((t) => t.includes("已經回覆過了") || t.includes("正在發布中"))).toBe(true);
    const [r] = await db.select().from(reviews).where(eq(reviews.id, rev!.id));
    expect(r!.status).toBe("replied");
    const [rep] = await db.select().from(replies).where(eq(replies.reviewId, rev!.id));
    expect(rep).toMatchObject({ publishedBy: "line_user", publishedByLineUserId: "U-tap", publishStatus: "published" });
  });

  it("publishes the draft the user SAW, even if a newer version exists", async () => {
    const { orgId, loc } = await setupOrg(env, "saw@example.com");
    await bindUser(env, orgId, "U-saw");
    const [rev] = await addAndSync(env, loc, [newGoogleReview({ starRating: 4 })]);
    const oldPub = postbacks(messagesTo(env.line, "U-saw").filter((m) => m.kind === "push").at(-1)!.messages[0]).find((d) => d.startsWith("a=publish"))!;
    const [v1] = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id));
    await sendPostback(env, "U-saw", `a=regen&r=${rev!.id}`); // 產生第 2 版
    expect(await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id))).toHaveLength(2);
    await sendPostback(env, "U-saw", oldPub);
    const [rep] = await db.select().from(replies).where(eq(replies.reviewId, rev!.id));
    expect(rep!.content).toBe(v1!.content);
  });

  it("skip and regenerate buttons work and regenerate counts against quota", async () => {
    const { orgId, loc } = await setupOrg(env, "skip@example.com");
    await bindUser(env, orgId, "U-skip");
    const [a, b] = await addAndSync(env, loc, [newGoogleReview({ starRating: 3, comment: "餐點普通。" }), newGoogleReview({ starRating: 3, comment: "還可以吧。" })]);
    const used = (await getQuota(db, orgId)).used;
    await sendPostback(env, "U-skip", `a=regen&r=${a!.id}`);
    expect((await getQuota(db, orgId)).used).toBe(used + 1);
    const card = lastTo(env.line, "U-skip")!;
    expect(card.messages[0]!.type).toBe("flex");
    await sendPostback(env, "U-skip", `a=skip&r=${b!.id}`);
    expect((await db.select().from(reviews).where(eq(reviews.id, b!.id)))[0]!.status).toBe("skipped");
  });
});

describe("授權：只有綁定且有權限的 LINE 使用者能操作", () => {
  it("denies other organizations, unsubscribed locations, and forged data", async () => {
    const a = await setupOrg(env, "authA@example.com");
    const b = await setupOrg(env, "authB@example.com");
    await bindUser(env, a.orgId, "U-A");
    await bindUser(env, b.orgId, "U-B");
    const [rev] = await addAndSync(env, a.loc, [newGoogleReview({ starRating: 5 })], { notify: false });
    const [draft] = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id));

    // 別家店的 LINE 使用者
    await sendPostback(env, "U-B", `a=publish&d=${draft!.id}`);
    expect(textsOf(lastTo(env.line, "U-B"))[0]).toBe(MESSAGES.denied);
    await sendPostback(env, "U-B", `a=skip&r=${rev!.id}`);
    expect((await db.select().from(reviews).where(eq(reviews.id, rev!.id)))[0]!.status).toBe("pending");

    // 同店家，但沒有勾選該店點
    const [bindingA] = await db.select().from(lineBindings).where(eq(lineBindings.lineUserId, "U-A"));
    await setBindingLocations(db, a.orgId, bindingA!.id, []);
    await sendPostback(env, "U-A", `a=publish&d=${draft!.id}`);
    expect(textsOf(lastTo(env.line, "U-A"))[0]).toBe(MESSAGES.denied);
    expect(await db.select().from(replies).where(eq(replies.reviewId, rev!.id))).toHaveLength(0);

    // 亂寫的資料、沒綁定的人
    await sendPostback(env, "U-A", "a=publish&d=not-a-uuid");
    expect(textsOf(lastTo(env.line, "U-A"))[0]).toContain("失效");
    await sendPostback(env, "U-nobody", `a=publish&d=${draft!.id}`);
    expect(textsOf(lastTo(env.line, "U-nobody"))[0]).toBe(MESSAGES.notBound);
  });

  it("postback data round-trips and rejects junk", () => {
    const id = "11111111-1111-1111-1111-111111111111";
    expect(decodePostback(encodePostback({ a: "publish", d: id }))).toEqual({ a: "publish", d: id });
    expect(decodePostback(`a=skip&r=${id}`)).toEqual({ a: "skip", r: id });
    expect(decodePostback("a=pubbatch&m=x")).toBeNull();
    expect(decodePostback("a=drop_table")).toBeNull();
    expect(encodePostback({ a: "publish", d: id }).length).toBeLessThan(300);
  });
});

describe("風險評論", () => {
  it("has no one-tap publish button and a forged publish is refused", async () => {
    const { orgId, loc } = await setupOrg(env, "risk@example.com");
    await bindUser(env, orgId, "U-risk");
    const [rev] = await addAndSync(env, loc, [newGoogleReview({ starRating: 1, comment: "吃完後上吐下瀉，疑似食物中毒，我會去向衛生局檢舉。" })]);
    const flex = messagesTo(env.line, "U-risk").filter((m) => m.kind === "push").at(-1)!.messages[0]!;
    expect(postbacks(flex).some((d) => d.startsWith("a=publish"))).toBe(false);
    expect(JSON.stringify(flex)).toContain("查看並確認");
    expect(JSON.stringify(flex)).toContain("涉及食安");
    const [draft] = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id));
    await sendPostback(env, "U-risk", `a=publish&d=${draft!.id}`);
    expect(textsOf(lastTo(env.line, "U-risk"))[0]).toBe(MESSAGES.riskNeedsConfirm);
    expect(await db.select().from(replies).where(eq(replies.reviewId, rev!.id))).toHaveLength(0);
    const [a] = await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, rev!.id));
    expect(a!.riskFlags).toContain("涉及食安");
  });
});

describe("推播合併（同一店點 10 分鐘內合併成一則）", () => {
  it("sends one carousel for several reviews found in the same sync", async () => {
    const { orgId, loc } = await setupOrg(env, "merge1@example.com");
    await bindUser(env, orgId, "U-m1");
    await addAndSync(env, loc, [newGoogleReview({ starRating: 5 }), newGoogleReview({ starRating: 3, comment: "普通。" }), newGoogleReview({ starRating: 2, comment: "等太久。" })]);
    const pushes = messagesTo(env.line, "U-m1").filter((m) => m.kind === "push");
    expect(pushes).toHaveLength(1);
    expect(pushes[0]!.messages).toHaveLength(1);
    expect(bubbleCount(pushes[0]!.messages[0] as never)).toBe(3);
  });

  it("holds later reviews for 10 minutes, then merges them into one message", async () => {
    const { orgId, loc } = await setupOrg(env, "merge2@example.com");
    await bindUser(env, orgId, "U-m2");
    const t0 = new Date();
    const pushCount = () => messagesTo(env.line, "U-m2").filter((m) => m.kind === "push").length;
    const first = await addAndSync(env, loc, [newGoogleReview({ starRating: 5 })]);
    expect(pushCount()).toBe(1);
    // 之後又來 2 則：先排佇列，不送
    const later = await addAndSync(env, loc, [newGoogleReview({ starRating: 4 }), newGoogleReview({ starRating: 1, comment: "很糟。" })], { notify: false });
    for (const r of later) await enqueueNewReview(db, r.id);
    const held = await flushNotifications(db, { line: env.line, now: () => new Date(t0.getTime() + 2 * 60_000), backoff: fastBackoff });
    expect(held).toMatchObject({ sent: 0, held: 1 });
    expect(pushCount()).toBe(1);
    // 超過 10 分鐘：合併成一則
    const sent = await flushNotifications(db, { line: env.line, now: () => new Date(t0.getTime() + MERGE_WINDOW_MS + 60_000), backoff: fastBackoff });
    expect(sent.sent).toBe(1);
    expect(pushCount()).toBe(2);
    const merged = messagesTo(env.line, "U-m2").filter((m) => m.kind === "push").at(-1)!.messages[0] as never;
    expect(bubbleCount(merged)).toBe(2);
    // 再 flush 一次不會重送
    await flushNotifications(db, { line: env.line, now: () => new Date(t0.getTime() + 2 * MERGE_WINDOW_MS), backoff: fastBackoff });
    expect(pushCount()).toBe(2);
    expect(first).toHaveLength(1);
  });

  it("caps a carousel and points to the dashboard for the rest", () => {
    const mk = (n: number) => ({ locationName: "店", messageId: "m", items: Array.from({ length: n }, (_, i) => ({ locationName: "店", review: { id: `${i}`.padStart(8, "0") + "-0000-0000-0000-000000000000", stars: 3, comment: "x", riskFlags: [] }, draft: null, aiStatus: "none" as const })) });
    const msg = newReviewsMessage(mk(25)) as unknown as { contents: { contents: unknown[] } };
    expect(msg.contents.contents.length).toBeLessThanOrEqual(MAX_BUBBLES);
    expect(JSON.stringify(msg.contents.contents.at(-1))).toContain("還有");
  });

  it("does not notify twice for the same review and survives a LINE outage by re-queuing", async () => {
    const e = makeEnv(db);
    const { orgId, loc } = await setupOrg(e, "outage@example.com");
    await bindUser(e, orgId, "U-out");
    e.line.failPush = true;
    const [rev] = await addAndSync(e, loc, [newGoogleReview({ starRating: 5 })]);
    expect(messagesTo(e.line, "U-out").filter((m) => m.kind === "push")).toHaveLength(0);
    const failed = await db.select().from(lineMessages).where(and(eq(lineMessages.status, "failed")));
    expect(failed.length).toBeGreaterThan(0);
    expect(await enqueueNewReview(db, rev!.id)).toBe(false); // 已在佇列／已處理，不重複排入
    const queued = await db.select().from(notifications).where(eq(notifications.reviewId, rev!.id));
    expect(queued).toHaveLength(1);
    expect(queued[0]!.sentAt).toBeNull(); // 放回佇列
    e.line.failPush = false;
    await flushNotifications(db, { line: e.line, backoff: fastBackoff });
    expect(messagesTo(e.line, "U-out").filter((m) => m.kind === "push")).toHaveLength(1);
  });

  it("stops notifying users who blocked the account (unfollow)", async () => {
    const e = makeEnv(db);
    const { orgId, loc } = await setupOrg(e, "unfollow@example.com");
    await bindUser(e, orgId, "U-un");
    await sendEvents(e, [{ type: "unfollow", webhookEventId: "un-1", source: { type: "user", userId: "U-un" } }]);
    await addAndSync(e, loc, [newGoogleReview()]);
    expect(messagesTo(e.line, "U-un").filter((m) => m.kind === "push")).toHaveLength(0);
  });

  it("includes quota-exhausted and failed-AI reviews with a 'reply yourself' card", async () => {
    const e = makeEnv(db);
    const { orgId, loc } = await setupOrg(e, "noquota@example.com");
    await bindUser(e, orgId, "U-nq");
    const { limit } = await getQuota(db, orgId);
    await db.insert((await import("@/lib/db/schema")).usageCounters).values({ organizationId: orgId, month: (await import("@/lib/usage")).monthKey(), draftsUsed: limit });
    await addAndSync(e, loc, [newGoogleReview({ starRating: 4 })]);
    const flex = JSON.stringify(messagesTo(e.line, "U-nq").filter((m) => m.kind === "push").at(-1)!.messages[0]);
    expect(flex).toContain("額度已用完");
    expect(flex).toContain("自己回覆");
    expect(flex).not.toContain("a=publish");
  });
});

describe("好評一次發布", () => {
  it("publishes only the eligible positive drafts that were on the card", async () => {
    const e = makeEnv(db);
    const { orgId, loc } = await setupOrg(e, "batch@example.com");
    await bindUser(e, orgId, "U-batch");
    const revs = await addAndSync(e, loc, [
      newGoogleReview({ starRating: 5 }), newGoogleReview({ starRating: 4, comment: "不錯吃。" }), newGoogleReview({ starRating: 5, comment: "很棒，推薦。" }),
      newGoogleReview({ starRating: 1, comment: "很糟，冷掉了。" }),
    ]);
    const msg = messagesTo(e.line, "U-batch").filter((m) => m.kind === "push").at(-1)!.messages[0]!;
    const batch = postbacks(msg).find((d) => d.startsWith("a=pubbatch"));
    expect(batch).toBeTruthy();
    expect(JSON.stringify(msg)).toContain("全部發布（3）");
    await sendPostback(e, "U-batch", batch!);
    expect(textsOf(lastTo(e.line, "U-batch"))[0]).toContain("已發布 3 則");
    const rows = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id)));
    const status = (id: string) => rows.find((r) => r.id === id)!.status;
    expect(revs.map((r) => status(r!.id))).toEqual(["replied", "replied", "replied", "pending"]);
    // 再按一次：全部略過，不會重複發布
    await sendPostback(e, "U-batch", batch!);
    expect(textsOf(lastTo(e.line, "U-batch"))[0]).toContain("已發布 0 則");
  });

  it("refuses a batch message that belongs to someone else", async () => {
    const e = makeEnv(db);
    const a = await setupOrg(e, "batchA@example.com");
    const b = await setupOrg(e, "batchB@example.com");
    await bindUser(e, a.orgId, "U-bA");
    await bindUser(e, b.orgId, "U-bB");
    await addAndSync(e, a.loc, [newGoogleReview({ starRating: 5 }), newGoogleReview({ starRating: 5, comment: "超讚！" })]);
    const batch = postbacks(messagesTo(e.line, "U-bA").filter((m) => m.kind === "push").at(-1)!.messages[0]).find((d) => d.startsWith("a=pubbatch"))!;
    await sendPostback(e, "U-bB", batch);
    expect(textsOf(lastTo(e.line, "U-bB"))[0]).toBe(MESSAGES.denied);
  });
});

describe("LINE 對話代理人", () => {
  it("answers help, pending and stats without spending AI quota", async () => {
    const e = makeEnv(db);
    const { orgId, loc } = await setupOrg(e, "agent1@example.com");
    await bindUser(e, orgId, "U-ag1");
    await addAndSync(e, loc, [newGoogleReview({ starRating: 5 }), newGoogleReview({ starRating: 1, comment: "太慢。" })]);
    const used = (await getQuota(db, orgId)).used;
    const llmCalls = e.llm.calls.length;
    await sendText(e, "U-ag1", "說明");
    expect(textsOf(lastTo(e.line, "U-ag1"))[0]).toBe(MESSAGES.help);
    await sendText(e, "U-ag1", "待處理");
    expect(lastTo(e.line, "U-ag1")!.messages.map((m) => m.type)).toEqual(["text", "flex"]);
    await sendText(e, "U-ag1", "本週統計");
    const since = periodStart("week", new Date());
    const inWeek = (await db.select().from(reviews).where(eq(reviews.locationId, loc.id))).filter((r) => r.createTime >= since);
    const stat = textsOf(lastTo(e.line, "U-ag1"))[0]!;
    // 歷史評論本來就算進統計；區間以台灣時間午夜起算
    expect(stat).toMatch(/^最近 7 天：\n新評論 \d+ 則，平均 \d\.\d 星\n負評（1–2 星）\d+ 則\n已回覆 \d+ 則，目前待處理 2 則$/);
    expect(Number(/新評論 (\d+) 則/.exec(stat)![1])).toBe(inWeek.length);
    expect(e.llm.calls.length).toBe(llmCalls); // 明確指令不呼叫 AI
    expect((await getQuota(db, orgId)).used).toBe(used);
  });

  it("revises the current draft when the owner asks in plain words (counts 1 quota)", async () => {
    const e = makeEnv(db);
    const { orgId, loc } = await setupOrg(e, "agent2@example.com");
    await bindUser(e, orgId, "U-ag2");
    const [rev] = await addAndSync(e, loc, [newGoogleReview({ starRating: 2, comment: "等了四十分鐘才上菜。" })]);
    const used = (await getQuota(db, orgId)).used;
    await sendText(e, "U-ag2", "語氣再溫和一點，短一點");
    const reply = lastTo(e.line, "U-ag2")!;
    expect(textsOf(reply)[0]).toContain("已經照您的意思改了");
    expect(reply.messages[1]!.type).toBe("flex");
    expect(await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id))).toHaveLength(2);
    expect((await getQuota(db, orgId)).used).toBe(used + 1);
    const logs = await db.select().from(aiUsageLogs).where(eq(aiUsageLogs.organizationId, orgId));
    expect(logs.map((l) => l.purpose)).toEqual(expect.arrayContaining(["line_intent", "write_revise"]));
    // 修改請求要連同原草稿一起交給模型
    const call = e.llm.calls.find((c) => c.purpose === "write_revise")!;
    expect(call.user).toContain("目前草稿");
    expect(call.user).toContain("<instruction>");
  });

  it("does not revise when there is no current review or for other users' reviews", async () => {
    const e = makeEnv(db);
    const a = await setupOrg(e, "agent3a@example.com");
    const b = await setupOrg(e, "agent3b@example.com");
    await bindUser(e, a.orgId, "U-ag3a");
    await bindUser(e, b.orgId, "U-ag3b");
    await sendText(e, "U-ag3a", "短一點");
    expect(textsOf(lastTo(e.line, "U-ag3a"))[0]).toBe(MESSAGES.noContext);
    await addAndSync(e, a.loc, [newGoogleReview({ starRating: 3, comment: "普通。" })]);
    // B 的使用者即使自己有 lastReviewId，也不能改 A 的評論
    const [bb] = await db.select().from(lineBindings).where(eq(lineBindings.lineUserId, "U-ag3b"));
    const [aRev] = await db.select().from(reviews).where(and(eq(reviews.locationId, a.loc.id), eq(reviews.status, "pending")));
    await db.update(lineBindings).set({ lastReviewId: aRev!.id }).where(eq(lineBindings.id, bb!.id));
    const drafts = (await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, aRev!.id))).length;
    await sendText(e, "U-ag3b", "短一點");
    expect(textsOf(lastTo(e.line, "U-ag3b"))[0]).toBe(MESSAGES.denied);
    expect((await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, aRev!.id))).length).toBe(drafts);
  });

  it("treats chit-chat as 'other' and shows help instead of spending quota", async () => {
    const e = makeEnv(db);
    const { orgId, loc } = await setupOrg(e, "agent4@example.com");
    await bindUser(e, orgId, "U-ag4");
    await addAndSync(e, loc, [newGoogleReview()]);
    const used = (await getQuota(db, orgId)).used;
    await sendText(e, "U-ag4", "今天天氣真好");
    expect(textsOf(lastTo(e.line, "U-ag4"))[0]).toBe(MESSAGES.help);
    expect((await getQuota(db, orgId)).used).toBe(used);
  });
});
