import { beforeAll, describe, expect, it } from "vitest";
import { and, eq } from "drizzle-orm";
import { MockGoogleBusinessAdapter } from "@/lib/adapters/google-business/mock";
import { MockLlmAdapter } from "@/lib/adapters/llm/mock";
import { AuthRevokedError, RateLimitError } from "@/lib/adapters/errors";
import type { Db } from "@/lib/db/client";
import { aiUsageLogs, googleCredentials, locations, replies, replyDrafts, reviewAnalyses, reviews, usageCounters } from "@/lib/db/schema";
import { enableLocation, refreshLocationList } from "@/lib/locations";
import { syncAllLocations, syncLocation, INITIAL_IMPORT_COUNT } from "@/lib/jobs/sync-reviews";
import { publishReply, setReviewSkipped } from "@/lib/publish";
import { generateDraft } from "@/lib/ai/pipeline";
import { getQuota, monthKey } from "@/lib/usage";
import { makeOrg, makeTestDb } from "./helpers";

let db: Db;
beforeAll(async () => { db = await makeTestDb(); }, 60_000);

const fastBackoff = { baseMs: 0, sleep: async () => {} };

async function setup(email: string) {
  const google = new MockGoogleBusinessAdapter();
  const llm = new MockLlmAdapter();
  const { orgId, userId } = await makeOrg(db, email);
  await refreshLocationList(db, orgId, { google });
  const locs = await db.select().from(locations).where(eq(locations.organizationId, orgId));
  return { google, llm, orgId, userId, locs };
}

describe("F1: account & location setup", () => {
  it("stores the refresh token encrypted and lists locations", async () => {
    const { userId, locs } = await setup("f1@example.com");
    expect(locs.length).toBe(3);
    const [cred] = await db.select().from(googleCredentials).where(eq(googleCredentials.userId, userId));
    expect(cred!.refreshTokenEncrypted).not.toContain("mock-refresh-token");
    expect(cred!.refreshTokenEncrypted.startsWith("v1:")).toBe(true);
  });

  it("enforces the plan's location limit (trial = 3)", async () => {
    const { google, llm, orgId } = await setup("limit@example.com");
    await google.listLocations({ refreshToken: "x" });
    const extra = [{ googleLocationId: "mock-loc-4", name: "第四間" }];
    const g2 = new MockGoogleBusinessAdapter([...(await google.listLocations({ refreshToken: "x" })), ...extra]);
    await refreshLocationList(db, orgId, { google: g2 });
    const all = await db.select().from(locations).where(eq(locations.organizationId, orgId));
    const results = [];
    for (const l of all) results.push(await enableLocation(db, orgId, l.id, { google: g2, llm, draft: false }));
    expect(results.filter((r) => r.ok)).toHaveLength(3);
    expect(results.find((r) => !r.ok)).toMatchObject({ ok: false, error: "limit_reached", limit: 3 });
  });

  it("detects revoked authorization and marks the credential", async () => {
    const { google, orgId, userId } = await setup("revoke@example.com");
    google.revoked = true;
    await expect(refreshLocationList(db, orgId, { google })).rejects.toBeInstanceOf(AuthRevokedError);
    const [cred] = await db.select().from(googleCredentials).where(eq(googleCredentials.userId, userId));
    expect(cred!.status).toBe("revoked");
  });
});

describe("F3: review sync", () => {
  it("first enable imports history without drafts; new reviews get drafts; no duplicates", async () => {
    const { google, llm, orgId, locs } = await setup("sync@example.com");
    const loc = locs[0]!;
    const first = await enableLocation(db, orgId, loc.id, { google, llm });
    expect(first).toEqual({ ok: true, imported: INITIAL_IMPORT_COUNT });
    const hist = await db.select().from(reviews).where(eq(reviews.locationId, loc.id));
    expect(hist).toHaveLength(INITIAL_IMPORT_COUNT);
    expect(hist.every((r) => r.isHistorical)).toBe(true);
    expect(llm.calls).toHaveLength(0); // 歷史評論不呼叫 AI
    expect(await db.select().from(replyDrafts)).toHaveLength(0);

    // 新評論出現
    const now = new Date();
    google.addReview(loc.googleLocationId, { googleReviewId: "new-1", starRating: 1, comment: "等了四十分鐘才上菜，態度也不太好。", reviewerDisplayName: "王小明", createTime: now, updateTime: now, reply: null });
    const r1 = await syncLocation(db, loc.id, { google, llm, backoff: fastBackoff });
    expect(r1).toMatchObject({ created: 1, drafted: 1 });
    // 再同步一次：不會重複建立、也不會再叫 AI
    const calls = llm.calls.length;
    const r2 = await syncLocation(db, loc.id, { google, llm, backoff: fastBackoff });
    expect(r2).toMatchObject({ created: 0, updated: 0, drafted: 0 });
    expect(llm.calls.length).toBe(calls);

    const [rev] = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id), eq(reviews.googleReviewId, "new-1")));
    expect(rev).toMatchObject({ isHistorical: false, status: "pending", aiStatus: "done" });
    const [a] = await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, rev!.id));
    expect(a!.sentiment).toBe("negative");
    expect(await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id))).toHaveLength(1);
  });

  it("marks edited reviews and keeps the original create time", async () => {
    const { google, llm, orgId, locs } = await setup("edit@example.com");
    const loc = locs[0]!;
    await enableLocation(db, orgId, loc.id, { google, llm });
    const { reviews: list } = await google.listReviews({ refreshToken: "x" }, loc.googleLocationId, { pageSize: 1 });
    const target = list[0]!;
    const [before] = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id), eq(reviews.googleReviewId, target.googleReviewId)));
    target.comment = "我改變心意了：其實很棒";
    target.updateTime = new Date();
    const res = await syncLocation(db, loc.id, { google, llm, backoff: fastBackoff, draft: false });
    expect(res.updated).toBe(1);
    const [after] = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id), eq(reviews.googleReviewId, target.googleReviewId)));
    expect(after).toMatchObject({ isEdited: true, comment: "我改變心意了：其實很棒" });
    expect(after!.createTime).toEqual(before!.createTime);
  });

  it("imports existing Google replies as already replied", async () => {
    const { google, llm, orgId, locs } = await setup("extreply@example.com");
    const loc = locs[1]!;
    const { reviews: list } = await google.listReviews({ refreshToken: "x" }, loc.googleLocationId, { pageSize: 1 });
    list[0]!.reply = { comment: "感謝", updateTime: new Date(), state: "live" };
    await enableLocation(db, orgId, loc.id, { google, llm });
    const [rev] = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id), eq(reviews.googleReviewId, list[0]!.googleReviewId)));
    expect(rev!.status).toBe("replied");
    const [rep] = await db.select().from(replies).where(eq(replies.reviewId, rev!.id));
    expect(rep).toMatchObject({ publishedBy: "external", publishStatus: "published" });
  });

  it("retries rate limits with exponential backoff", async () => {
    const { google, llm, orgId, locs } = await setup("rate@example.com");
    const loc = locs[0]!;
    await enableLocation(db, orgId, loc.id, { google, llm });
    const real = google.listReviews.bind(google);
    let fails = 2;
    google.listReviews = async (...args) => { if (fails-- > 0) throw new RateLimitError(); return real(...args); };
    const waits: number[] = [];
    await syncLocation(db, loc.id, { google, llm, draft: false, backoff: { sleep: async (ms) => { waits.push(ms); }, baseMs: 1000 } });
    expect(waits).toEqual([1000, 2000]);
  });

  it("a failing location does not affect the others", async () => {
    const { google, llm, orgId, locs } = await setup("isolate@example.com");
    for (const l of locs.slice(0, 2)) await enableLocation(db, orgId, l.id, { google, llm });
    const real = google.listReviews.bind(google);
    google.listReviews = async (a, locId, p) => { if (locId === locs[0]!.googleLocationId) throw new Error("boom"); return real(a, locId, p); };
    const out = (await syncAllLocations(db, { google, llm, draft: false, backoff: fastBackoff, now: () => new Date(Date.now() + 31 * 60_000) })).filter((r) => locs.slice(0, 2).some((l) => l.id === r.locationId));
    expect(out.find((r) => r.locationId === locs[0]!.id)?.error).toBe("boom");
    expect(out.find((r) => r.locationId === locs[1]!.id)?.error).toBeUndefined();
  });
});

describe("F4: drafts, quota & cost logging", () => {
  it("logs token usage for every AI call and counts quota", async () => {
    const { google, llm, orgId, locs } = await setup("cost@example.com");
    const loc = locs[0]!;
    await enableLocation(db, orgId, loc.id, { google, llm });
    const [rev] = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id), eq(reviews.starRating, 5)));
    const target = rev ?? (await db.select().from(reviews).where(eq(reviews.locationId, loc.id)))[0]!;
    const res = await generateDraft(db, target.id, { llm });
    expect(res.status).toBe("ok");
    const logs = await db.select().from(aiUsageLogs).where(eq(aiUsageLogs.organizationId, orgId));
    expect(logs.length).toBeGreaterThan(0);
    expect(logs.every((l) => l.tokensIn > 0 && l.model.startsWith("mock-"))).toBe(true);
    expect((await getQuota(db, orgId)).used).toBe(1);
    // 換一個版本：version +1、再扣一次額度
    const again = await generateDraft(db, target.id, { llm });
    expect(again).toMatchObject({ status: "ok", version: 2 });
    expect((await getQuota(db, orgId)).used).toBe(2);
  });

  it("stops drafting when the monthly quota is used up but still syncs reviews", async () => {
    const { google, llm, orgId, locs } = await setup("quota@example.com");
    const loc = locs[0]!;
    await enableLocation(db, orgId, loc.id, { google, llm });
    const { limit } = await getQuota(db, orgId);
    await db.insert(usageCounters).values({ organizationId: orgId, month: monthKey(), draftsUsed: limit }).onConflictDoUpdate({ target: [usageCounters.organizationId, usageCounters.month], set: { draftsUsed: limit } });
    const now = new Date();
    google.addReview(loc.googleLocationId, { googleReviewId: "q-1", starRating: 4, comment: "很好吃，下次還會再來", reviewerDisplayName: "A", createTime: now, updateTime: now, reply: null });
    const res = await syncLocation(db, loc.id, { google, llm, backoff: fastBackoff });
    expect(res).toMatchObject({ created: 1, drafted: 0 });
    const [rev] = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id), eq(reviews.googleReviewId, "q-1")));
    expect(rev).toMatchObject({ aiStatus: "quota_exceeded", status: "pending" });
    expect((await getQuota(db, orgId)).used).toBe(limit);
  });

  it("never exceeds quota under concurrent drafts", async () => {
    const { google, llm, orgId, locs } = await setup("race@example.com");
    const loc = locs[0]!;
    await enableLocation(db, orgId, loc.id, { google, llm });
    const { limit } = await getQuota(db, orgId);
    await db.insert(usageCounters).values({ organizationId: orgId, month: monthKey(), draftsUsed: limit - 2 }).onConflictDoUpdate({ target: [usageCounters.organizationId, usageCounters.month], set: { draftsUsed: limit - 2 } });
    const ids = (await db.select().from(reviews).where(eq(reviews.locationId, loc.id)).limit(6)).map((r) => r.id);
    const results = await Promise.all(ids.map((id) => generateDraft(db, id, { llm })));
    expect(results.filter((r) => r.status === "ok")).toHaveLength(2);
    expect(results.filter((r) => r.status === "quota_exceeded")).toHaveLength(4);
    expect((await getQuota(db, orgId)).used).toBe(limit);
  });

  it("fails after retries and releases the quota when the model keeps violating rules", async () => {
    const { google, llm, orgId, locs } = await setup("bad@example.com");
    const loc = locs[0]!;
    await enableLocation(db, orgId, loc.id, { google, llm });
    const bad = { async generate(req: Parameters<MockLlmAdapter["generate"]>[0]) {
      if (req.purpose === "classify") return llm.generate(req);
      return { text: "歡迎使用優惠碼 SAVE10，詳見 https://example.com 立即享折扣。", model: "bad", tokensIn: 1, tokensOut: 1, costUsd: 0 };
    } };
    const [rev] = await db.select().from(reviews).where(eq(reviews.locationId, loc.id)).limit(1);
    const before = (await getQuota(db, orgId)).used;
    const res = await generateDraft(db, rev!.id, { llm: bad });
    expect(res.status).toBe("failed");
    expect(await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, rev!.id))).toHaveLength(0);
    expect((await getQuota(db, orgId)).used).toBe(before);
    const [after] = await db.select().from(reviews).where(eq(reviews.id, rev!.id));
    expect(after!.aiStatus).toBe("failed");
  });
});

describe("F5/F7: publishing", () => {
  async function pendingReview(email: string) {
    const ctx = await setup(email);
    const loc = ctx.locs[0]!;
    await enableLocation(db, ctx.orgId, loc.id, { google: ctx.google, llm: ctx.llm });
    const [rev] = await db.select().from(reviews).where(and(eq(reviews.locationId, loc.id), eq(reviews.status, "pending")));
    return { ...ctx, loc, rev: rev! };
  }

  it("publishes once; repeated or concurrent clicks do not publish twice", async () => {
    const { google, orgId, rev, loc } = await pendingReview("pub@example.com");
    let calls = 0;
    const real = google.replyToReview.bind(google);
    google.replyToReview = async (...a) => { calls++; await new Promise((r) => setTimeout(r, 20)); return real(...a); };
    const input = { reviewId: rev.id, organizationId: orgId, content: "謝謝您的支持", publishedBy: "user" as const, confirmRisk: true };
    const results = await Promise.all([publishReply(db, input, { google }), publishReply(db, input, { google }), publishReply(db, input, { google })]);
    expect(calls).toBe(1);
    expect(results.filter((r) => r.status === "published")).toHaveLength(1);
    expect(results.every((r) => ["published", "in_progress", "already_published"].includes(r.status))).toBe(true);
    expect((await publishReply(db, input, { google })).status).toBe("already_published");
    const [after] = await db.select().from(reviews).where(eq(reviews.id, rev.id));
    expect(after!.status).toBe("replied");
    const g = await google.getReview({ refreshToken: "x" }, loc.googleLocationId, rev.googleReviewId);
    expect(g!.reply!.comment).toBe("謝謝您的支持");
  });

  it("records failures and allows a retry", async () => {
    const { google, orgId, rev } = await pendingReview("fail@example.com");
    const real = google.replyToReview.bind(google);
    google.replyToReview = async () => { throw new Error("Google 暫時無法使用"); };
    const input = { reviewId: rev.id, organizationId: orgId, content: "回覆內容", publishedBy: "user" as const, confirmRisk: true };
    expect(await publishReply(db, input, { google })).toEqual({ status: "failed", error: "Google 暫時無法使用" });
    const [failed] = await db.select().from(reviews).where(eq(reviews.id, rev.id));
    expect(failed!.status).toBe("publish_failed");
    google.replyToReview = real;
    expect((await publishReply(db, input, { google })).status).toBe("published");
  });

  it("refuses other organizations and requires confirmation for risky reviews", async () => {
    const { google, rev, orgId } = await pendingReview("owner2@example.com");
    const other = await makeOrg(db, "intruder@example.com");
    expect((await publishReply(db, { reviewId: rev.id, organizationId: other.orgId, content: "x", publishedBy: "user" }, { google })).status).toBe("not_found");
    await db.insert(reviewAnalyses).values({ reviewId: rev.id, sentiment: "negative", language: "zh-TW", riskFlags: ["涉及食安"], needsHuman: true });
    expect((await publishReply(db, { reviewId: rev.id, organizationId: orgId, content: "x", publishedBy: "user" }, { google })).status).toBe("risk_confirmation_required");
  });

  it("skip / unskip only touches the owner's unreplied reviews", async () => {
    const { rev, orgId } = await pendingReview("skip@example.com");
    expect(await setReviewSkipped(db, orgId, rev.id, true)).toBe(true);
    expect((await db.select().from(reviews).where(eq(reviews.id, rev.id)))[0]!.status).toBe("skipped");
    const other = await makeOrg(db, "other-skip@example.com");
    expect(await setReviewSkipped(db, other.orgId, rev.id, false)).toBe(false);
  });
});
