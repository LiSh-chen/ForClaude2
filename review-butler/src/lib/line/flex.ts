import type { LineMessage } from "@/lib/adapters/line/interface";
import { riskReminders } from "@/lib/ai/risk";
import { dashboardUrl, liffReviewUrl } from "./links";

// ---------- postback 資料 ----------
// LINE 限制 postback data 長度（官方文件為準），這裡只放 UUID 與動作代碼，遠低於上限。
export type PostbackAction =
  | { a: "publish"; d: string } // d = 草稿 id：發布「使用者當時看到的那一版」
  | { a: "regen"; r: string }
  | { a: "skip"; r: string }
  | { a: "pubbatch"; m: string }; // m = line_messages.id

export const encodePostback = (p: PostbackAction): string => new URLSearchParams(p as Record<string, string>).toString();

export function decodePostback(data: string): PostbackAction | null {
  const q = new URLSearchParams(data);
  const a = q.get("a");
  const uuid = /^[0-9a-f-]{36}$/i;
  const pick = (k: string) => { const v = q.get(k); return v && uuid.test(v) ? v : null; };
  if (a === "publish" && pick("d")) return { a, d: pick("d")! };
  if ((a === "regen" || a === "skip") && pick("r")) return { a, r: pick("r")! };
  if (a === "pubbatch" && pick("m")) return { a, m: pick("m")! };
  return null;
}

// ---------- 版面元件 ----------
const COLOR = { muted: "#888888", star: "#F5A623", risk: "#C62828", text: "#222222" };
const stars = (n: number) => "★".repeat(n) + "☆".repeat(5 - n);
const clip = (s: string, n: number) => (s.length > n ? `${s.slice(0, n)}…` : s);

type Json = Record<string, unknown>;
const text = (t: string, extra: Json = {}): Json => ({ type: "text", text: t, wrap: true, ...extra });
const button = (label: string, action: Json, style: "primary" | "secondary" | "link" = "secondary"): Json => ({
  type: "button", style, height: "sm", action: { ...action, label: clip(label, 20) },
});
const postback = (label: string, data: PostbackAction, displayText?: string): Json =>
  button(label, { type: "postback", data: encodePostback(data), ...(displayText ? { displayText } : {}) }, "secondary");
const uri = (label: string, url: string, style: "primary" | "secondary" | "link" = "secondary"): Json => button(label, { type: "uri", uri: url }, style);

export interface BubbleInput {
  locationName: string;
  review: { id: string; stars: number; comment: string | null; riskFlags: string[] };
  draft: { id: string; content: string } | null;
  aiStatus: "none" | "done" | "failed" | "quota_exceeded";
}

/** 單則評論的卡片：評論＋建議回覆＋四個按鈕。有風險的評論不給一鍵發布，要進編輯頁確認。 */
export function reviewBubble(i: BubbleInput): Json {
  const risky = i.review.riskFlags.length > 0;
  const body: Json[] = [
    text("顧客評論", { size: "xs", color: COLOR.muted }),
    text(i.review.comment ? clip(i.review.comment, 200) : "（顧客只留下星等，沒有文字）", { color: COLOR.text }),
    { type: "separator", margin: "md" },
  ];
  if (risky) body.push(text(`⚠ ${i.review.riskFlags.join("、")}`, { size: "sm", weight: "bold", color: COLOR.risk, margin: "md" }), ...riskReminders(i.review.riskFlags).map((r) => text(r, { size: "xs", color: COLOR.risk })));
  if (i.draft) {
    body.push(text("建議回覆", { size: "xs", color: COLOR.muted, margin: "md" }), text(clip(i.draft.content, 300)));
  } else {
    const why = i.aiStatus === "quota_exceeded" ? "本月 AI 草稿額度已用完，這則需要您自己回覆（或到網頁升級方案）。" : i.aiStatus === "failed" ? "AI 這次沒有寫出合格的草稿，請自己回覆，或讓 AI 再試一次。" : "還沒有草稿。";
    body.push(text(why, { size: "sm", color: COLOR.muted, margin: "md" }));
  }

  const edit = uri(i.draft ? (risky ? "✏️ 查看並確認" : "✏️ 修改") : "✏️ 自己回覆", liffReviewUrl(i.review.id), risky || !i.draft ? "primary" : "secondary");
  const regen = i.aiStatus === "quota_exceeded" ? null : postback(i.draft ? "🔄 換一個" : "✨ 讓 AI 寫", { a: "regen", r: i.review.id });
  const skip = postback("⏭️ 略過", { a: "skip", r: i.review.id }, "略過這則");
  const footer: Json[] = [];
  if (i.draft && !risky) footer.push(button("✅ 發布", { type: "postback", data: encodePostback({ a: "publish", d: i.draft.id }), displayText: "發布" }, "primary"));
  footer.push({ type: "box", layout: "horizontal", spacing: "sm", contents: [edit, ...(regen ? [regen] : []), skip] });

  return {
    type: "bubble",
    size: "mega",
    header: { type: "box", layout: "vertical", spacing: "xs", contents: [text(i.locationName, { size: "sm", color: COLOR.muted }), text(stars(i.review.stars), { size: "xl", color: COLOR.star })] },
    body: { type: "box", layout: "vertical", spacing: "sm", contents: body },
    footer: { type: "box", layout: "vertical", spacing: "sm", contents: footer },
  };
}

/** 「好評一次發布」卡片（≥2 則符合條件時放在輪播最前面）。 */
function batchBubble(count: number, messageId: string): Json {
  return {
    type: "bubble",
    size: "mega",
    body: { type: "box", layout: "vertical", spacing: "md", contents: [text("好評一次發布", { weight: "bold", size: "lg" }), text(`有 ${count} 則 4 星以上、沒有風險提醒的好評，草稿都準備好了。`, { size: "sm" }), text("負評與有風險提醒的評論不會被一起發布。", { size: "xs", color: COLOR.muted })] },
    footer: { type: "box", layout: "vertical", contents: [button(`✅ 全部發布（${count}）`, { type: "postback", data: encodePostback({ a: "pubbatch", m: messageId }), displayText: "好評一次發布" }, "primary")] },
  };
}

/** 輪播最多 10 個 bubble（各來源說法不一，取保守值）；超過時最後一張導向網頁後台。 */
export const MAX_BUBBLES = 10;

export interface NewReviewsInput {
  /** 用於 altText；多店點混合時給通用名稱 */
  locationName: string;
  items: BubbleInput[];
  /** 預先產生的 line_messages.id，供「好評一次發布」按鈕使用；不給就不顯示批次發布卡片 */
  messageId?: string;
}

/** 是否可被「好評一次發布」處理：4 星以上、沒有風險、已有草稿。 */
export const isBatchEligible = (i: BubbleInput): boolean => i.review.stars >= 4 && i.review.riskFlags.length === 0 && i.draft !== null;

/** 合併推播：同一店點的多則新評論 → 一則 Flex 訊息（單則為 bubble、多則為 carousel）。 */
export function newReviewsMessage(i: NewReviewsInput): LineMessage {
  const n = i.items.length;
  const altText = clip(n === 1 ? `${i.locationName}：${i.items[0]!.review.stars} 星新評論，已準備好回覆草稿` : `${i.locationName} 有 ${n} 則新評論，已準備好回覆草稿`, 380);
  if (n === 1) return { type: "flex", altText, contents: reviewBubble(i.items[0]!) };

  const eligible = i.items.filter(isBatchEligible).length;
  const lead: Json[] = eligible >= 2 && i.messageId ? [batchBubble(eligible, i.messageId)] : [];
  const room = MAX_BUBBLES - lead.length;
  const shown = i.items.length > room ? i.items.slice(0, room - 1) : i.items;
  const bubbles = [...lead, ...shown.map(reviewBubble)];
  if (shown.length < i.items.length) {
    bubbles.push({
      type: "bubble",
      body: { type: "box", layout: "vertical", spacing: "md", contents: [text(`還有 ${i.items.length - shown.length} 則`, { weight: "bold" }), text("請到網頁後台查看全部評論。", { size: "sm" })] },
      footer: { type: "box", layout: "vertical", contents: [uri("開啟後台", dashboardUrl(), "primary")] },
    });
  }
  return { type: "flex", altText, contents: { type: "carousel", contents: bubbles } };
}

/** 發布後 Google 拒絕／未顯示：提醒並提供修改入口（F5）。 */
export function replyRejectedMessage(input: { locationName: string; reviewId: string; stars: number; reason: string }): LineMessage {
  return {
    type: "flex",
    altText: `${input.locationName}：有一則回覆沒有成功顯示在 Google`,
    contents: {
      type: "bubble",
      body: { type: "box", layout: "vertical", spacing: "sm", contents: [
        text("⚠ 回覆沒有成功顯示", { weight: "bold", color: COLOR.risk }),
        text(`${input.locationName}（${stars(input.stars)}）`, { size: "sm", color: COLOR.muted }),
        text(input.reason, { size: "sm" }),
        text("可能是內容被 Google 審核擋下。建議修改後重新發布。", { size: "xs", color: COLOR.muted }),
      ] },
      footer: { type: "box", layout: "vertical", contents: [uri("✏️ 修改並重新發布", liffReviewUrl(input.reviewId), "primary")] },
    },
  };
}

export const textMessage = (t: string): LineMessage => ({ type: "text", text: t });

export const MESSAGES = {
  welcome: "歡迎使用好評管家 👋\n有新的 Google 評論時，我會把寫好的回覆草稿傳給您，您按一下就能發布。\n\n請到網頁後台的「LINE」頁取得 6 位數綁定碼，直接在這裡輸入就完成綁定了。",
  welcomeBack: "歡迎回來！有新的評論我會第一時間通知您。\n輸入「說明」可以看我能幫您做什麼。",
  bindOk: (n: number) => `✅ 綁定成功！${n > 0 ? `目前會通知您 ${n} 間店的新評論。` : "還沒有啟用的店點，請先到網頁後台啟用。"}\n之後有新評論我會傳給您，輸入「說明」可以看我能做什麼。`,
  bindInvalid: "綁定碼不正確或已經用過了，請回到網頁後台重新取得。",
  bindExpired: "綁定碼已經過期（10 分鐘內有效），請回到網頁後台重新取得。",
  notBound: "您還沒有綁定喔。請到網頁後台的「LINE」頁取得 6 位數綁定碼，直接輸入在這裡即可。",
  help: "我可以幫您：\n• 新評論來了會傳卡片，按「發布」一鍵回覆\n• 回「待處理」看還沒回的評論\n• 回「統計」看本週評論概況\n• 看完草稿想調整，直接說，例如「語氣再溫和一點」「短一點」「不要道歉」，我會改好再給您看\n• 回「說明」再看一次這段",
  denied: "您沒有這間店的操作權限。",
  stale: "這則評論已經處理過了，不用再操作。",
  noContext: "請先對某一則評論的卡片操作（或等新評論通知），我才知道要修改哪一則。",
  riskNeedsConfirm: "⚠ 這則評論有風險提醒，需要您親自確認內容後才能發布。請點卡片上的「查看並確認」。",
  authRevoked: "⚠ 您的 Google 授權已失效，暫時無法抓取新評論或發布回覆。請到網頁後台重新登入 Google 授權。",
};
