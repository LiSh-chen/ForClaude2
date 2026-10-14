"use server";

import { redirect } from "next/navigation";
import { z } from "zod";
import { INDUSTRIES, type Industry } from "@/config/industries";
import { getGoogleBusiness, isMockGoogle } from "@/lib/adapters";
import { MockGoogleBusinessAdapter } from "@/lib/adapters/google-business/mock";
import { generateDraft } from "@/lib/ai/pipeline";
import { requireUser } from "@/lib/auth";
import { brandSettings, locations } from "@/lib/db/schema";
import { syncLocation } from "@/lib/jobs/sync-reviews";
import { disableLocation, enableLocation, refreshLocationList, setLocationIndustry } from "@/lib/locations";
import { publishReply, setReviewSkipped } from "@/lib/publish";
import { getReviewDetail } from "@/lib/queries";
import { and, eq } from "drizzle-orm";

type Flash = { notice?: string; error?: string };
function go(path: string, flash: Flash = {}): never {
  const q = new URLSearchParams();
  if (flash.notice) q.set("notice", flash.notice);
  if (flash.error) q.set("error", flash.error);
  redirect(q.size ? `${path}${path.includes("?") ? "&" : "?"}${q}` : path);
}
const reviewPath = (id: string) => `/dashboard/reviews/${id}`;
const errMsg = (e: unknown) => (e instanceof Error ? e.message : String(e));

// ---------- 評論 ----------

/** 立即檢查所有啟用店點的新評論（排程之外的手動觸發）。 */
export async function syncNowAction() {
  const { db, org } = await requireUser();
  const locs = await db.select().from(locations).where(and(eq(locations.organizationId, org.id), eq(locations.enabled, true)));
  if (!locs.length) go("/dashboard/locations", { error: "請先選擇要啟用的店點" });
  let created = 0, drafted = 0;
  const failed: string[] = [];
  for (const l of locs) {
    try {
      const r = await syncLocation(db, l.id);
      created += r.created; drafted += r.drafted;
    } catch (e) {
      failed.push(`${l.name}：${errMsg(e)}`);
    }
  }
  go("/dashboard", failed.length ? { error: `部分店點更新失敗（${failed.join("；")}）` } : { notice: created ? `找到 ${created} 則新評論，已準備 ${drafted} 則回覆草稿` : "目前沒有新評論" });
}

const SAMPLES: Record<string, string> = {
  "5": "牛肉麵湯頭很濃郁，麵條Q彈，店員也很親切，下次會再來！",
  "4": "整體不錯，環境乾淨，只是假日人多要等一下。",
  "3": "餐點普通，沒有特別驚艷，價格偏高。",
  "2": "等了四十分鐘才上菜，詢問後店員態度也不太好。",
  "1": "湯是冷的，而且吃到一根頭髮，非常噁心！",
  food: "吃完後上吐下瀉，疑似食物中毒，我會去向衛生局檢舉。",
  en: "Amazing ramen! The broth was rich and the staff were super friendly.",
};

/** 開發工具：在 mock Google 加一則新評論，然後同步（僅 mock 模式可用）。 */
export async function simulateReviewAction(formData: FormData) {
  const { db, org } = await requireUser();
  const google = getGoogleBusiness();
  if (!isMockGoogle() || !(google instanceof MockGoogleBusinessAdapter)) go("/dashboard", { error: "只有模擬模式可以產生假評論" });
  const kind = String(formData.get("kind") ?? "5");
  const locationId = String(formData.get("locationId") ?? "");
  const [loc] = await db.select().from(locations).where(and(eq(locations.id, locationId), eq(locations.organizationId, org.id)));
  if (!loc?.enabled) go("/dashboard", { error: "請先選擇一個已啟用的店點" });
  const stars = kind === "food" ? 1 : kind === "en" ? 5 : Number(kind);
  const now = new Date();
  (google as MockGoogleBusinessAdapter).addReview(loc.googleLocationId, {
    googleReviewId: `sim-${now.getTime()}`,
    starRating: Math.min(5, Math.max(1, stars)) as 1 | 2 | 3 | 4 | 5,
    comment: SAMPLES[kind] ?? SAMPLES["5"]!,
    reviewerDisplayName: "測試顧客",
    createTime: now,
    updateTime: now,
    reply: null,
  });
  try {
    const r = await syncLocation(db, loc.id);
    go("/dashboard", { notice: `已新增 1 則模擬評論，${r.drafted ? "並產生草稿" : "但沒有產生草稿（額度用完或 AI 失敗）"}` });
  } catch (e) {
    if (isRedirect(e)) throw e;
    go("/dashboard", { error: errMsg(e) });
  }
}

function isRedirect(e: unknown) {
  return typeof e === "object" && e !== null && "digest" in e && String((e as { digest: unknown }).digest).startsWith("NEXT_REDIRECT");
}

const publishSchema = z.object({ reviewId: z.string().uuid(), content: z.string() });

export async function publishAction(formData: FormData) {
  const { db, org } = await requireUser();
  const input = publishSchema.parse({ reviewId: formData.get("reviewId"), content: formData.get("content") });
  const res = await publishReply(db, {
    reviewId: input.reviewId,
    organizationId: org.id,
    content: input.content,
    publishedBy: "user",
    confirmRisk: formData.get("confirmRisk") === "on",
  });
  const path = reviewPath(input.reviewId);
  switch (res.status) {
    case "published": go(path, { notice: "已發布到 Google" });
    case "already_published": go(path, { notice: "這則已經回覆過了" });
    case "in_progress": go(path, { notice: "正在發布中，請稍候再重新整理" });
    case "risk_confirmation_required": go(path, { error: "這則評論有風險提醒，請先勾選「我已了解」再發布" });
    case "invalid": go(path, { error: res.reason });
    case "not_found": go("/dashboard", { error: "找不到這則評論" });
    case "failed": go(path, { error: `發布失敗：${res.error}` });
  }
}

/** 產生草稿或「換一個版本」（兩者都計入額度）。 */
export async function regenerateAction(formData: FormData) {
  const { db, org } = await requireUser();
  const reviewId = z.string().uuid().parse(formData.get("reviewId"));
  if (!(await getReviewDetail(db, org.id, reviewId))) go("/dashboard", { error: "找不到這則評論" });
  const res = await generateDraft(db, reviewId);
  const path = reviewPath(reviewId);
  if (res.status === "ok") go(path, { notice: "已產生新的草稿" });
  if (res.status === "quota_exceeded") go(path, { error: "本月草稿額度已用完，請升級方案或等下個月" });
  go(path, { error: "AI 這次沒有寫出合格的草稿，請再試一次，或直接自己寫回覆" });
}

export async function skipAction(formData: FormData) {
  const { db, org } = await requireUser();
  const reviewId = z.string().uuid().parse(formData.get("reviewId"));
  const skip = formData.get("skip") === "1";
  await setReviewSkipped(db, org.id, reviewId, skip);
  go(skip ? "/dashboard" : reviewPath(reviewId), { notice: skip ? "已略過這則評論" : "已改回待處理" });
}

// ---------- 店點 ----------

export async function refreshLocationsAction() {
  const { db, org } = await requireUser();
  try {
    await refreshLocationList(db, org.id);
  } catch (e) {
    go("/dashboard/locations", { error: `讀取店點失敗：${errMsg(e)}` });
  }
  go("/dashboard/locations", { notice: "已更新店點清單" });
}

export async function enableLocationAction(formData: FormData) {
  const { db, org } = await requireUser();
  const id = z.string().uuid().parse(formData.get("locationId"));
  const res = await enableLocation(db, org.id, id).catch((e: unknown) => ({ ok: false as const, error: "failed" as const, message: errMsg(e) }));
  if (res.ok) go("/dashboard/locations", { notice: res.imported ? `已啟用，並匯入最近 ${res.imported} 則歷史評論（不會通知，您可以在「歷史評論」挑選要處理的）` : "已啟用" });
  if (res.error === "limit_reached") go("/dashboard/locations", { error: `目前方案最多啟用 ${"limit" in res ? res.limit : ""} 個店點` });
  go("/dashboard/locations", { error: "啟用失敗" + ("message" in res ? `：${res.message}` : "") });
}

export async function disableLocationAction(formData: FormData) {
  const { db, org } = await requireUser();
  await disableLocation(db, org.id, z.string().uuid().parse(formData.get("locationId")));
  go("/dashboard/locations", { notice: "已停用，不再抓取這間店的新評論" });
}

export async function setIndustryAction(formData: FormData) {
  const { db, org } = await requireUser();
  const industry = z.enum(INDUSTRIES).parse(formData.get("industry")) as Industry;
  await setLocationIndustry(db, org.id, z.string().uuid().parse(formData.get("locationId")), industry);
  go("/dashboard/locations", { notice: "已更新產業別" });
}

// ---------- 設定 ----------

const settingsSchema = z.object({
  tone: z.enum(["親切", "專業", "活潑"]),
  salutation: z.enum(["我們", "本店", "店名"]),
  signature: z.string().max(60).optional(),
  storeInfo: z.string().max(1500).optional(),
});

export async function saveSettingsAction(formData: FormData) {
  const { db, org } = await requireUser();
  const parsed = settingsSchema.safeParse({
    tone: formData.get("tone"),
    salutation: formData.get("salutation"),
    signature: formData.get("signature") || undefined,
    storeInfo: formData.get("storeInfo") || undefined,
  });
  if (!parsed.success) go("/dashboard/settings", { error: "設定格式不正確，簽名最多 60 字、店家資訊最多 1500 字" });
  const v = parsed.data;
  await db
    .insert(brandSettings)
    .values({ organizationId: org.id, tone: v.tone, salutation: v.salutation, signature: v.signature ?? null, storeInfo: v.storeInfo ?? null })
    .onConflictDoUpdate({
      target: brandSettings.organizationId,
      set: { tone: v.tone, salutation: v.salutation, signature: v.signature ?? null, storeInfo: v.storeInfo ?? null, updatedAt: new Date() },
    });
  go("/dashboard/settings", { notice: "已儲存。之後的草稿會依這些設定撰寫" });
}
