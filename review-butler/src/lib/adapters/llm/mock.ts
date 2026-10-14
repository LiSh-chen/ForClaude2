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
    const text = req.purpose === "classify" ? this.classify(req.user) : req.purpose === "line_intent" ? this.intent(req.user) : this.write(req.user, this.calls.length);
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

  private intent(text: string): string {
    const revise = /改|短|長|語氣|溫和|不要|道歉|親切|專業|活潑|簡單|正式|口語|加上|拿掉|再.{0,3}一點/.test(text);
    const period = /今天/.test(text) ? "today" : /本月|這個月/.test(text) ? "month" : "week";
    return JSON.stringify({ intent: revise ? "revise" : /統計|幾則|多少|平均/.test(text) ? "stats" : /待|還沒|未回/.test(text) ? "pending" : "other", period });
  }

  private write(user: string, n = 0): string {
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
    const alt = n % 2 === 1;
    if (user.includes("<instruction>")) {
      return stars <= 2
        ? "謝謝您的反映，對這次的狀況我們很抱歉，會與同仁一起檢討改進。歡迎您私下與我們聯繫，讓我們有機會向您說明並好好處理。"
        : "謝謝您的肯定與支持，聽到您滿意我們非常開心，會繼續用心把服務做好，期待您再次光臨，祝您順心愉快。";
    }
    return stars <= 2
      ? alt
        ? "謝謝您特地留下意見，這次讓您不開心，我們真的很抱歉。我們會立刻和團隊討論改善的方法，也想邀請您私下與我們聯繫，讓我們有機會了解詳細情形並盡力處理。"
        : "感謝您願意花時間告訴我們這次的狀況，對於讓您有不愉快的體驗，我們感到非常抱歉。我們會與團隊一起檢討並加以改善，也誠摯邀請您私下與我們聯繫，讓我們有機會好好了解並為您處理。"
      : alt
        ? "真的很謝謝您留下這麼溫暖的評價，能被您肯定是對我們最大的鼓勵。我們會繼續用心經營，期待下次再為您服務，也祝您天天都有好心情，歡迎常來坐坐。"
        : "非常感謝您的留言與肯定，能讓您有愉快的體驗是我們最開心的事。我們會繼續把服務與品質顧好，也歡迎您下次再來，期待再次為您服務，祝您一切順心，天天開心。";
  }
}
