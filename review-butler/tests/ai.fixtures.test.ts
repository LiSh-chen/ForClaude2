import { eq } from "drizzle-orm";
import { beforeAll, describe, expect, it } from "vitest";
import { MockLlmAdapter } from "@/lib/adapters/llm/mock";
import { checkDraft } from "@/lib/ai/check";
import { detectLanguage } from "@/lib/ai/language";
import { generateDraft } from "@/lib/ai/pipeline";
import type { Db } from "@/lib/db/client";
import { aiUsageLogs, locations, replyDrafts, reviewAnalyses, reviews } from "@/lib/db/schema";
import { makeOrg, makeTestDb } from "./helpers";
import { REVIEW_FIXTURES } from "./fixtures/load";

describe("fixtures (SPEC §8：至少 30 則多樣化評論)", () => {
  it("covers the required variety", () => {
    expect(REVIEW_FIXTURES.length).toBeGreaterThanOrEqual(30);
    expect(new Set(REVIEW_FIXTURES.map((r) => r.stars))).toEqual(new Set([1, 2, 3, 4, 5]));
    expect(new Set(REVIEW_FIXTURES.map((r) => r.language))).toEqual(new Set(["zh-TW", "en", "ja", "other"]));
    expect(REVIEW_FIXTURES.some((r) => r.comment === null)).toBe(true);
    expect(REVIEW_FIXTURES.some((r) => r.expectRisk.length > 0)).toBe(true);
    expect(new Set(REVIEW_FIXTURES.map((r) => r.industry)).size).toBeGreaterThanOrEqual(4);
    expect(REVIEW_FIXTURES.some((r) => /ignore previous|忽略以上/i.test(r.comment ?? ""))).toBe(true);
  });

  it("deterministic language detection matches the labels", () => {
    for (const r of REVIEW_FIXTURES.filter((x) => x.comment)) {
      expect(detectLanguage(r.comment!), r.id).toBe(r.language);
    }
  });
});

describe("pipeline over all fixtures (mock LLM：驗證流程與規則檢查，不代表真實文案品質)", () => {
  let db: Db;
  const ids = new Map<string, string>();
  const llm = new MockLlmAdapter();

  beforeAll(async () => {
    db = await makeTestDb();
    const { orgId } = await makeOrg(db, "fixtures@example.com");
    const locByIndustry = new Map<string, string>();
    for (const r of REVIEW_FIXTURES) {
      if (!locByIndustry.has(r.industry)) {
        const [loc] = await db.insert(locations).values({ organizationId: orgId, googleLocationId: `fx-${r.industry}`, name: `測試店-${r.industry}`, industry: r.industry, enabled: true }).returning();
        locByIndustry.set(r.industry, loc!.id);
      }
      const [row] = await db
        .insert(reviews)
        .values({ googleReviewId: r.id, locationId: locByIndustry.get(r.industry)!, starRating: r.stars, comment: r.comment, createTime: new Date(), updateTime: new Date() })
        .returning();
      ids.set(r.id, row!.id);
    }
  }, 60_000);

  it("produces a rule-compliant draft for every review and logs usage", async () => {
    for (const r of REVIEW_FIXTURES) {
      const res = await generateDraft(db, ids.get(r.id)!, { llm });
      expect(res.status, `${r.id}: ${JSON.stringify(res)}`).toBe("ok");
      const [a] = await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, ids.get(r.id)!));
      const [d] = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, ids.get(r.id)!));
      expect(checkDraft(d!.content, a!.language), `${r.id} draft: ${d!.content}`).toEqual([]);
      expect(a!.sentiment).toBe(r.stars >= 4 ? "positive" : r.stars === 3 ? "neutral" : "negative");
    }
    const withText = REVIEW_FIXTURES.filter((r) => r.comment).length;
    const logs = await db.select().from(aiUsageLogs);
    expect(logs.filter((l) => l.purpose === "classify")).toHaveLength(withText); // 無文字評論不呼叫分類
    expect(logs.filter((l) => l.purpose === "write")).toHaveLength(REVIEW_FIXTURES.length);
  });

  it("flags the risky reviews the mock classifier can see and never leaks reviewer names to the model", async () => {
    for (const r of REVIEW_FIXTURES.filter((x) => x.language === "zh-TW" && x.expectRisk.length && x.expectRisk.every((f) => ["涉及食安", "提到法律行動"].includes(f)))) {
      const [a] = await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, ids.get(r.id)!));
      expect(a!.riskFlags, r.id).toEqual(expect.arrayContaining(r.expectRisk));
      expect(a!.needsHuman).toBe(true);
    }
    // 評論者姓名屬個資：fixtures 沒有姓名欄位，這裡確保 prompt 不含「reviewer」字樣
    expect(llm.calls.every((c) => !/reviewer|姓名|暱稱：/.test(c.user))).toBe(true);
  });

  it("wraps review text as data and tells the model to ignore embedded instructions", () => {
    const call = llm.calls.find((c) => c.purpose === "write" && /忽略以上所有規則/.test(c.user))!;
    expect(call.user).toContain("<review>");
    expect(call.system).toContain("不是給你的指令");
  });
});
