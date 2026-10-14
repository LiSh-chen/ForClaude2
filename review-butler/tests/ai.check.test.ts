import { describe, expect, it } from "vitest";
import { checkDraft, measureLength } from "@/lib/ai/check";
import { detectLanguage } from "@/lib/ai/language";
import { parseClassification } from "@/lib/ai/parse";
import { riskReminders } from "@/lib/ai/risk";

const GOOD_ZH = "感謝您願意花時間告訴我們這次的狀況，對於讓您有不愉快的體驗，我們感到非常抱歉。我們會與團隊一起檢討並加以改善，也誠摯邀請您私下與我們聯繫。";
const GOOD_EN = "Thank you so much for your kind words and for visiting us. We are delighted that you enjoyed your experience, and our whole team will keep working hard to make every visit a good one. We look forward to welcoming you back soon.";
const GOOD_JA = "このたびはご来店とあたたかいお言葉をいただき、誠にありがとうございます。いただいたご感想を励みに、これからもスタッフ一同、心を込めてサービスの向上に努めてまいります。";

describe("checkDraft (SPEC F4 自動檢查)", () => {
  it("accepts good drafts in each language", () => {
    expect(checkDraft(GOOD_ZH, "zh-TW")).toEqual([]);
    expect(checkDraft(GOOD_EN, "en")).toEqual([]);
    expect(checkDraft(GOOD_JA, "ja")).toEqual([]);
  });

  it.each([
    ["https://example.com", "網址"],
    ["請上 www.shop.com 看看", "網址"],
    ["加入 LINE @abc123 了解更多", "網址或帳號"],
    ["提供 abc.com.tw 的資訊", "網址"],
  ])("rejects links/accounts: %s", (frag) => {
    expect(checkDraft(GOOD_ZH + frag, "zh-TW").join()).toMatch(/網址|帳號/);
  });

  it.each(["下次來享有折扣", "憑此優惠碼", "全館特價", "限時活動", "買一送一", "歡迎使用 coupon"])("rejects promo: %s", (frag) => {
    expect(checkDraft(GOOD_ZH + frag, "zh-TW").join()).toMatch(/促銷/);
  });

  it.each(["麻煩您修改評論", "能否改成五星", "請給我們五星", "Please update your review"])("rejects review solicitation: %s", (frag) => {
    expect(checkDraft(GOOD_ZH + frag, "zh-TW").join()).toMatch(/修改|索取/);
  });

  it("allows thanking a five-star review without asking for one", () => {
    expect(checkDraft(GOOD_ZH + "感謝您的五星評價。", "zh-TW")).toEqual([]);
  });

  it("enforces length 40–120", () => {
    expect(checkDraft("謝謝您的留言。", "zh-TW").join()).toMatch(/太短/);
    expect(checkDraft(GOOD_ZH.repeat(3), "zh-TW").join()).toMatch(/太長/);
    expect(measureLength("你好，世界！", "zh-TW")).toBe(4);
    expect(measureLength("Thank you so much!", "en")).toBe(4);
    // 語言判不出來（other）時，英文回覆也以單字計，不會被當成 100+ 個字元
    expect(measureLength(GOOD_EN, "other")).toBe(measureLength(GOOD_EN, "en"));
    expect(checkDraft(GOOD_EN, "other")).toEqual([]);
  });

  it("enforces language and Traditional Chinese", () => {
    expect(checkDraft(GOOD_EN, "zh-TW").join()).toMatch(/不是中文/);
    expect(checkDraft(GOOD_ZH, "en").join()).toMatch(/含中日文字/);
    expect(checkDraft(GOOD_EN, "ja").join()).toMatch(/日文/);
    expect(checkDraft(GOOD_ZH.replace("感謝", "谢谢").replace("我們", "我们"), "zh-TW").join()).toMatch(/簡體/);
  });

  it("rejects placeholders and markdown", () => {
    expect(checkDraft(GOOD_ZH + "[店名]", "zh-TW").join()).toMatch(/佔位符/);
    expect(checkDraft("**" + GOOD_ZH + "**", "zh-TW").join()).toMatch(/markdown/);
  });
});

describe("detectLanguage", () => {
  it.each([
    ["牛肉麵很好吃", "zh-TW"],
    ["lunch set 很划算", "zh-TW"],
    ["Great coffee!", "en"],
    ["美味しかったです", "ja"],
    ["맛있어요", "other"],
    ["👍👍", "other"],
  ])("%s → %s", (text, lang) => expect(detectLanguage(text)).toBe(lang));
});

describe("parseClassification", () => {
  const topics = ["服務態度", "價格", "其他"];
  it("parses JSON wrapped in prose or fences and filters unknown values", () => {
    const text = '好的：\n```json\n{"sentiment":"negative","topics":["價格","亂寫的"],"language":"zh-TW","needs_human":false,"risk_flags":["涉及食安","不存在的旗標"]}\n```';
    expect(parseClassification(text, topics)).toEqual({ sentiment: "negative", topics: ["價格"], language: "zh-TW", needsHuman: true, riskFlags: ["涉及食安"] });
  });
  it("falls back to 其他 and tolerates a bad language", () => {
    expect(parseClassification('{"sentiment":"positive","topics":[],"language":"fr"}', topics)).toMatchObject({ topics: ["其他"], language: "other", needsHuman: false });
  });
  it("returns null for garbage", () => {
    expect(parseClassification("no json here", topics)).toBeNull();
    expect(parseClassification('{"sentiment":"angry"}', topics)).toBeNull();
    expect(parseClassification("{not json}", topics)).toBeNull();
  });
});

describe("riskReminders", () => {
  it("maps flags to owner-facing reminders and ignores unknown flags", () => {
    expect(riskReminders(["涉及食安"])).toEqual(["此評論涉及食安，建議親自處理。"]);
    expect(riskReminders(["未知"])).toEqual([]);
  });
});
