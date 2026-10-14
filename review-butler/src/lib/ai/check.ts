/**
 * 草稿的自動規則檢查（SPEC F4 規則 3/5/6 與語言規則）。
 * 這些是程式判斷，不靠 AI 檢查 AI；違規時由流程要求重寫。
 */
export type ReviewLanguage = "zh-TW" | "en" | "ja" | "other";

const URL_RE = /https?:\/\/|www\.|\b[a-z0-9-]+\.(?:com|tw|net|org|io|co|me|app|shop)\b|line\.me|@[a-z0-9_.]{3,}/i;

const PROMO_WORDS = [
  "折扣", "優惠", "折價", "打折", "特價", "促銷", "優惠碼", "折扣碼", "折價券", "優待", "買一送一", "限時", "抽獎", "集點", "加碼送",
  "coupon", "discount", "promo", "% off", "free gift", "voucher", "special offer",
];

/** 要求顧客修改／刪除評論、或索取好評 */
const REVIEW_SOLICIT = [
  "修改評論", "更改評論", "修改評價", "更改評價", "刪除評論", "刪除評價", "撤回評論", "撤回評價", "重新評分", "重新評價",
  "改成五星", "改為五星", "給我們五星", "給五星", "追加好評", "補一個好評",
  "update your review", "change your review", "change your rating", "revise your rating", "remove your review", "delete your review", "edit your review",
];

/** 簡體字專用字（台灣繁體不會出現）。只用於 zh-TW。 */
const SIMPLIFIED_RE = /[这们说请谢么为务应样时体验欢还让没吗给对们过关于开门间问题见饭觉]/;

const cjkCount = (s: string) => (s.match(/[一-鿿]/g) ?? []).length;
const kanaCount = (s: string) => (s.match(/[぀-ヿ]/g) ?? []).length;
const latinWords = (s: string) => (s.match(/[A-Za-z]+(?:['’-][A-Za-z]+)*/g) ?? []).length;

const latinLetters = (s: string) => (s.match(/[A-Za-z]/g) ?? []).length;

/** CJK／韓文等以字數計（不含空白與標點）；英文（含判不出語言但以拉丁字母為主的回覆）以單字數計。 */
export function measureLength(text: string, language: ReviewLanguage): number {
  const compact = text.replace(/[\s\p{P}\p{S}]/gu, "");
  const byWords = language === "en" || (language === "other" && latinLetters(text) > compact.length / 2);
  return byWords ? latinWords(text) : compact.length;
}

export const LENGTH_LIMITS = { min: 40, max: 120 } as const;

export function checkDraft(text: string, language: ReviewLanguage): string[] {
  const issues: string[] = [];
  const lower = text.toLowerCase();

  if (URL_RE.test(text)) issues.push("含有網址或帳號連結");
  for (const w of PROMO_WORDS) if (lower.includes(w.toLowerCase())) { issues.push(`含有促銷用語「${w}」`); break; }
  for (const w of REVIEW_SOLICIT) if (lower.includes(w.toLowerCase())) { issues.push(`含有要求修改／刪除評論或索取好評的字眼「${w}」`); break; }
  if (/\[[^\]]{1,20}\]|\{[^}]{1,20}\}/.test(text)) issues.push("含有未填入的佔位符");
  if (/```|^#{1,6}\s|\*\*/m.test(text)) issues.push("含有 markdown 格式");

  const len = measureLength(text, language);
  if (len < LENGTH_LIMITS.min) issues.push(`太短（${len}，需 ${LENGTH_LIMITS.min}–${LENGTH_LIMITS.max}）`);
  if (len > LENGTH_LIMITS.max) issues.push(`太長（${len}，需 ${LENGTH_LIMITS.min}–${LENGTH_LIMITS.max}）`);

  if (language === "zh-TW") {
    if (cjkCount(text) < latinWords(text)) issues.push("評論是中文，回覆卻不是中文");
    if (kanaCount(text) > 0) issues.push("中文回覆中混入日文假名");
    if (SIMPLIFIED_RE.test(text)) issues.push("含簡體字，需使用繁體中文");
  } else if (language === "en") {
    if (cjkCount(text) + kanaCount(text) > 0) issues.push("評論是英文，回覆卻含中日文字");
  } else if (language === "ja") {
    if (kanaCount(text) === 0) issues.push("評論是日文，回覆卻不是日文");
  }
  return issues;
}
