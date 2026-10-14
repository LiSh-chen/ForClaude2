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

  private classify(prompt: string): string {
    const stars = Number(/星等[:：]\s*(\d)/.exec(prompt)?.[1] ?? 3);
    // 只分析 <review> 內的評論文字，不被外框的中文說明干擾
    const user = /<review>([\s\S]*?)<\/review>/.exec(prompt)?.[1] ?? prompt;
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
    const review = /<review>([\s\S]*?)<\/review>/.exec(user)?.[1] ?? "";
    const ja = /[ぁ-んァ-ン]/.test(review);
    const zh = /[\u4e00-\u9fff]/.test(review);
    if (ja) {
      return "このたびはご来店とあたたかいお言葉をいただき、誠にありがとうございます。いただいたご感想を励みに、これからもスタッフ一同、心を込めてサービスの向上に努めてまいります。またのお越しを心よりお待ちしております。";
    }
    if (!zh && review.trim()) {
      return stars <= 2
        ? "Thank you for taking the time to share this with us, and we are truly sorry that your visit did not meet expectations. We will review what happened with our team and work to improve. Please contact us directly so we can listen and make things right."
        : "Thank you so much for your kind words and for visiting us. We are delighted that you enjoyed your experience, and our whole team will keep working hard to make every visit a good one. We look forward to welcoming you back soon.";
    }
    return stars <= 2
      ? "感謝您願意花時間告訴我們這次的狀況，對於讓您有不愉快的體驗，我們感到非常抱歉。我們會與團隊一起檢討並加以改善，也誠摯邀請您私下與我們聯繫，讓我們有機會好好了解並為您處理。"
      : "非常感謝您的留言與肯定，能讓您有愉快的體驗是我們最開心的事。我們會繼續把服務與品質顧好，也歡迎您下次再來，期待再次為您服務，祝您一切順心，天天開心。";
  }
}
