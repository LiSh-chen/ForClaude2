import { and, desc, eq } from "drizzle-orm";
import { DEFAULT_TOPICS, type Industry } from "@/config/industries";
import { getLlm } from "@/lib/adapters";
import { LlmFatalError, LlmRefusalError, type LlmAdapter, type LlmRequest, type LlmResponse } from "@/lib/adapters/llm/interface";
import type { Db } from "@/lib/db/client";
import { aiUsageLogs, brandSettings, locations, replyDrafts, reviewAnalyses, reviews } from "@/lib/db/schema";
import { releaseDraft, reserveDraft } from "@/lib/usage";
import { checkDraft, type ReviewLanguage } from "./check";
import { detectLanguage } from "./language";
import { parseClassification } from "./parse";
import { CLASSIFY_SYSTEM, classifyUserPrompt, writeSystemPrompt, writeUserPrompt } from "./prompts";

/** 失敗後的重試次數（SPEC F4：AI 呼叫失敗重試 2 次，共 3 次嘗試）。 */
export const MAX_RETRIES = 2;

const INDUSTRY_LABEL: Record<Industry, string> = { food: "餐飲", beauty: "美業", medical: "醫療", education: "教育補習", other: "其他" };

export interface PipelineDeps {
  llm?: LlmAdapter;
  /** 店主用說的修改要求（LINE 對話）。有值時以最新草稿為基礎修改，而不是重寫。 */
  instruction?: string;
}

export type DraftResult =
  | { status: "ok"; draftId: string; version: number; content: string }
  | { status: "quota_exceeded" }
  | { status: "failed"; reason: string };

interface Ctx {
  review: typeof reviews.$inferSelect;
  location: typeof locations.$inferSelect;
  brand: { tone: string; salutation: string; signature: string | null; storeInfo: string | null };
}

async function loadContext(db: Db, reviewId: string): Promise<Ctx> {
  const [row] = await db
    .select({ review: reviews, location: locations })
    .from(reviews)
    .innerJoin(locations, eq(reviews.locationId, locations.id))
    .where(eq(reviews.id, reviewId));
  if (!row) throw new Error(`Review not found: ${reviewId}`);
  const [b] = await db.select().from(brandSettings).where(eq(brandSettings.organizationId, row.location.organizationId));
  return {
    ...row,
    brand: { tone: b?.tone ?? "親切", salutation: b?.salutation ?? "我們", signature: b?.signature ?? null, storeInfo: b?.storeInfo ?? null },
  };
}

/** 呼叫 LLM 並記錄 token 與成本（SPEC 6.2：每次 AI 呼叫都要記錄）。 */
export async function callLlm(db: Db, organizationId: string, llm: LlmAdapter, req: LlmRequest): Promise<LlmResponse> {
  const res = await llm.generate(req);
  await db.insert(aiUsageLogs).values({
    organizationId,
    purpose: req.purpose,
    model: res.model,
    tokensIn: res.tokensIn,
    tokensOut: res.tokensOut,
    cost: res.costUsd.toFixed(6),
  });
  return res;
}

function sentimentFromStars(stars: number) {
  return stars >= 4 ? "positive" : stars === 3 ? "neutral" : "negative";
}

/** 分類（小模型）。沒有文字的評論不呼叫 AI；分類失敗時退回以星等判斷並標記需人工。 */
export async function analyzeReview(db: Db, reviewId: string, deps: PipelineDeps = {}) {
  const existing = await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, reviewId));
  if (existing[0]) return existing[0];

  const ctx = await loadContext(db, reviewId);
  const { review, location } = ctx;
  const stars = review.starRating;
  let analysis: { sentiment: "positive" | "neutral" | "negative"; topics: string[]; language: ReviewLanguage; needsHuman: boolean; riskFlags: string[] };

  if (!review.comment?.trim()) {
    analysis = { sentiment: sentimentFromStars(stars), topics: [], language: "zh-TW", needsHuman: false, riskFlags: [] };
  } else {
    const llm = deps.llm ?? getLlm();
    const topics = DEFAULT_TOPICS[location.industry];
    const prompt = classifyUserPrompt({ industryLabel: INDUSTRY_LABEL[location.industry], topics, stars, comment: review.comment });
    let parsed = null;
    for (let attempt = 0; attempt <= MAX_RETRIES && !parsed; attempt++) {
      try {
        const res = await callLlm(db, location.organizationId, llm, { tier: "classify", purpose: "classify", system: CLASSIFY_SYSTEM, user: prompt });
        parsed = parseClassification(res.text, topics);
      } catch (e) {
        if (e instanceof LlmFatalError || e instanceof LlmRefusalError) break;
      }
    }
    const detected = detectLanguage(review.comment);
    analysis = parsed
      ? { ...parsed, language: detected !== "other" ? detected : parsed.language }
      : { sentiment: sentimentFromStars(stars), topics: ["其他"], language: detected, needsHuman: true, riskFlags: [] };
  }

  const [row] = await db
    .insert(reviewAnalyses)
    .values({ reviewId, ...analysis })
    .onConflictDoNothing()
    .returning();
  return row ?? (await db.select().from(reviewAnalyses).where(eq(reviewAnalyses.reviewId, reviewId)))[0]!;
}

function cleanDraft(text: string): string {
  return text
    .replace(/^```[a-z]*\n?|```$/gim, "")
    .trim()
    .replace(/^["“「『]\s*|\s*["”」』]$/g, "")
    .trim();
}

/**
 * 產生一則草稿（大模型）。每次成功都消耗 1 則月額度（含「換一個版本」）。
 * 違反規則（網址、促銷、長度、語言…）會帶著違規原因重寫，最多重試 MAX_RETRIES 次；
 * 仍失敗就不存草稿，回報 failed，由店家手動回覆。
 */
export async function generateDraft(db: Db, reviewId: string, deps: PipelineDeps = {}): Promise<DraftResult> {
  const llm = deps.llm ?? getLlm();
  const analysis = await analyzeReview(db, reviewId, deps);
  const ctx = await loadContext(db, reviewId);
  const { review, location, brand } = ctx;
  const orgId = location.organizationId;

  const previous = await db.select().from(replyDrafts).where(eq(replyDrafts.reviewId, reviewId)).orderBy(desc(replyDrafts.version));

  if (!(await reserveDraft(db, orgId))) {
    if (!previous.length) await db.update(reviews).set({ aiStatus: "quota_exceeded" }).where(eq(reviews.id, reviewId));
    return { status: "quota_exceeded" };
  }

  const system = writeSystemPrompt({ ...brand, storeName: location.name }, analysis.language);
  let violations: string[] = [];
  let reason = "";

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    try {
      const res = await callLlm(db, orgId, llm, {
        tier: "write",
        purpose: deps.instruction ? "write_revise" : previous.length ? "write_regenerate" : "write",
        system,
        user: writeUserPrompt({
          stars: review.starRating,
          comment: review.comment,
          sentiment: analysis.sentiment,
          topics: analysis.topics,
          riskFlags: analysis.riskFlags,
          previousDrafts: previous.slice(0, 3).map((d) => d.content),
          violations,
          revision: deps.instruction && previous[0] ? { draft: previous[0].content, instruction: deps.instruction.slice(0, 300) } : undefined,
        }),
      });
      const body = cleanDraft(res.text);
      violations = checkDraft(body, analysis.language);
      if (violations.length === 0) {
        const content = brand.signature?.trim() ? `${body}\n\n${brand.signature.trim()}` : body;
        const version = (previous[0]?.version ?? 0) + 1;
        const [draft] = await db
          .insert(replyDrafts)
          .values({ reviewId, content, version, model: res.model, tokensIn: res.tokensIn, tokensOut: res.tokensOut, cost: res.costUsd.toFixed(6) })
          .returning();
        await db.update(reviews).set({ aiStatus: "done" }).where(eq(reviews.id, reviewId));
        return { status: "ok", draftId: draft!.id, version, content };
      }
      reason = `草稿未通過檢查：${violations.join("；")}`;
    } catch (e) {
      reason = e instanceof Error ? e.message : String(e);
      if (e instanceof LlmFatalError || e instanceof LlmRefusalError) break;
    }
  }

  await releaseDraft(db, orgId);
  if (!previous.length) await db.update(reviews).set({ aiStatus: "failed" }).where(eq(reviews.id, reviewId));
  return { status: "failed", reason };
}

/** 新評論的完整處理：分類 → 草稿。歷史評論與已回覆的評論不會自動呼叫（歷史評論由店家挑選後再產生）。 */
export async function processNewReview(db: Db, reviewId: string, deps: PipelineDeps = {}): Promise<DraftResult | null> {
  const [review] = await db.select().from(reviews).where(eq(reviews.id, reviewId));
  if (!review || review.isHistorical || review.status !== "pending") return null;
  const drafts = await db.select().from(replyDrafts).where(and(eq(replyDrafts.reviewId, reviewId)));
  if (drafts.length) return null;
  return generateDraft(db, reviewId, deps);
}
