import type { ReviewLanguage } from "./check";

/** 以字元判斷語言（中英混雜算中文）；無法判斷回傳 "other"。這比請模型判斷更便宜也更穩。 */
export function detectLanguage(text: string): ReviewLanguage {
  const kana = (text.match(/[぀-ヿ]/g) ?? []).length;
  const cjk = (text.match(/[一-鿿]/g) ?? []).length;
  const latin = (text.match(/[A-Za-z]/g) ?? []).length;
  if (kana > 0) return "ja";
  if (cjk > 0) return "zh-TW";
  if (latin > 0) return "en";
  return "other";
}
