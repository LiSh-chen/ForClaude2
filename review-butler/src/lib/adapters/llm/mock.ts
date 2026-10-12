import type { LlmAdapter, LlmRequest, LlmResponse } from "./interface";

const approxTokens = (s: string) => Math.ceil(s.length / 2);

/**
 * 決定性的假 LLM，只為了讓流程能跑通，不代表真實品質。
 * - purpose === "classify"：依關鍵字輸出分類 JSON
 * - 其他：輸出固定樣板的繁中回覆
 */
export class MockLlmAdapter implements LlmAdapter {
  readonly calls: LlmRequest[] = [];

  async generate(req: LlmRequest): Promise<LlmResponse> {
    this.calls.push(req);
    const text = req.purpose === "classify" ? this.classify(req.user) : this.write(req.user);
    const tokensIn = approxTokens(req.system + req.user);
    const tokensOut = approxTokens(text);
    return { text, model: `mock-${req.tier}`, tokensIn, tokensOut, costUsd: 0 };
  }

  private classify(user: string): string {
    const stars = Number(/星等[:：]\s*(\d)/.exec(user)?.[1] ?? 3);
    const sentiment = stars >= 4 ? "positive" : stars <= 2 ? "negative" : "neutral";
    const riskFlags: string[] = [];
    if (/食物中毒|上吐下瀉|拉肚子/.test(user)) riskFlags.push("涉及食安");
    if (/律師|告|檢舉|法律/.test(user)) riskFlags.push("提到法律行動");
    const language = /[ぁ-んァ-ン]/.test(user) ? "ja" : /[一-鿿]/.test(user) ? "zh-TW" : "en";
    return JSON.stringify({
      sentiment,
      topics: ["其他"],
      language,
      needs_human: riskFlags.length > 0,
      risk_flags: riskFlags,
    });
  }

  private write(user: string): string {
    const stars = Number(/星等[:：]\s*(\d)/.exec(user)?.[1] ?? 3);
    return stars <= 2
      ? "感謝您願意告訴我們這次的狀況，我們很抱歉讓您有不愉快的體驗，會檢討並改善。歡迎私下與我們聯繫，讓我們有機會為您處理。"
      : "感謝您的留言與支持，您的肯定是我們持續進步的動力，期待下次再為您服務。";
  }
}
