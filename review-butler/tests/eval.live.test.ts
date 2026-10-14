/**
 * 真實模型評測（預設跳過）。用法：
 *   ANTHROPIC_API_KEY=sk-ant-... EVAL_LIVE=1 npm run eval:drafts
 * 會把 tests/fixtures/reviews.json 的所有評論送進真實的「分類（小模型）→ 撰寫（大模型）」流程，
 * 印出每則的草稿、違規原因、token 與成本，並檢查：失敗率、風險評論召回率、單則平均成本。
 * 這會花真實的錢（約數美分）。
 */
import { eq } from "drizzle-orm";
import { beforeAll, describe, expect, it } from "vitest";
import { ClaudeLlmAdapter } from "@/lib/adapters/llm/claude";
import { checkDraft } from "@/lib/ai/check";
import { generateDraft } from "@/lib/ai/pipeline";
import type { Db } from "@/lib/db/client";
import { aiUsageLogs, locations, replyDrafts, reviewAnalyses, reviews } from "@/lib/db/schema";
import { getModel } from "@/lib/env";
import { makeOrg, makeTestDb } from "./helpers";
import { REVIEW_FIXTURES } from "./fixtures/load";

describe.skipIf(!process.env.EVAL_LIVE)("live eval (真實 Claude API)", () => {
  let db: Db;
  const rows: { id: string; reviewId: string }[] = [];
  let orgId: string;

  beforeAll(async () => {
    db = await makeTestDb();
    ({ orgId } = await makeOrg(db, "eval@example.com"));
    const locs = new Map<string, string>();
    for (const r of REVIEW_FIXTURES) {
      if (!locs.has(r.industry)) {
        const [l] = await db.insert(locations).values({ organizationId: orgId, googleLocationId: `ev-${r.industry}`, name: "測試小吃店", industry: r.industry, enabled: true }).returning();
        locs.set(r.industry, l!.id);
      }
      const [row] = await db.insert(reviews).values({ googleReviewId: r.id, locationId: locs.get(r.industry)!, starRating: r.stars, comment: r.comment, createTime: new Date(), updateTime: new Date() }).returning();
      rows.push({ id: r.id, reviewId: row!.id });
    }
  }, 60_000);

  it(`runs all fixtures (classify=${getModel("classify")}, write=${getModel("write")})`, async () => {
    const llm = new ClaudeLlmAdapter();
    const failures: string[] = [];
    const queue = [...rows];
    // 3 條並行，避免打爆速率限制
    await Promise.all(Array.from({ length: 3 }, async () => {
      for (let item = queue.shift(); item; item = queue.shift()) {
        const res = await generateDraft(db, item.reviewId, { llm });
        if (res.status !== "ok") failures.push(`${item.id}: ${JSON.stringify(res)}`);
      }
    }));

    const logs = await db.select().from(aiUsageLogs).where(eq(aiUsageLogs.organizationId, orgId));
    const cost = logs.reduce((s, l) => s + Number(l.cost), 0);
    const tokensIn = logs.reduce((s, l) => s + l.tokensIn, 0);
    const tokensOut = logs.reduce((s, l) => s + l.tokensOut, 0);

    let riskExpected = 0, riskCaught = 0;
    for (const f of REVIEW_FIXTURES) {
      const reviewId = rows.find((r) => r.id === f.id)!.reviewId;
      const [a] = await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, reviewId));
      const [d] = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, reviewId));
      if (f.expectRisk.length) { riskExpected++; if (a?.riskFlags.length) riskCaught++; }
      console.log(`\n[${f.id}] ${f.stars}★ ${f.language} ${a?.sentiment ?? "?"} risk=${JSON.stringify(a?.riskFlags ?? [])}${f.note ? `（${f.note}）` : ""}\n  評論：${(f.comment ?? "（無文字）").slice(0, 80)}\n  草稿：${d?.content ?? "（無）"}`);
      if (d) expect(checkDraft(d.content.split("\n\n")[0]!, a!.language), f.id).toEqual([]);
    }
    console.log(`\n=== 總結 ===\n評論數 ${REVIEW_FIXTURES.length}，失敗 ${failures.length}\n風險評論召回 ${riskCaught}/${riskExpected}\nAI 呼叫 ${logs.length} 次，token 進 ${tokensIn} / 出 ${tokensOut}\n總成本 US$${cost.toFixed(4)}，每則評論平均 US$${(cost / REVIEW_FIXTURES.length).toFixed(4)}`);
    if (failures.length) console.log("失敗清單：\n" + failures.join("\n"));

    expect(failures.length / REVIEW_FIXTURES.length).toBeLessThanOrEqual(0.1);
    expect(riskCaught / Math.max(1, riskExpected)).toBeGreaterThanOrEqual(0.8);
  }, 600_000);
});
