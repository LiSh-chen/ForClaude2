import { sql } from "drizzle-orm";
import {
  boolean,
  check,
  index,
  integer,
  jsonb,
  numeric,
  pgEnum,
  pgTable,
  primaryKey,
  text,
  timestamp,
  uniqueIndex,
  uuid,
} from "drizzle-orm/pg-core";

// 所有時間以 UTC 儲存（timestamptz），顯示時再轉 Asia/Taipei。
const ts = (name: string) => timestamp(name, { withTimezone: true, mode: "date" });
const createdAt = () => ts("created_at").notNull().defaultNow();
const id = () => uuid("id").primaryKey().defaultRandom();
/** 費用以美元計，6 位小數。 */
const usd = (name: string) => numeric(name, { precision: 12, scale: 6 });

// ---------- enums ----------
export const industryEnum = pgEnum("industry", ["food", "beauty", "medical", "education", "other"]);
export const credentialStatusEnum = pgEnum("credential_status", ["active", "revoked", "expired"]);
export const reviewStatusEnum = pgEnum("review_status", ["pending", "replied", "skipped", "publish_failed"]);
export const sentimentEnum = pgEnum("sentiment", ["positive", "neutral", "negative"]);
export const languageEnum = pgEnum("review_language", ["zh-TW", "en", "ja", "other"]);
export const publishStatusEnum = pgEnum("publish_status", ["pending", "published", "failed", "rejected"]);
/** external = 在 Google 上原本就存在的回覆（非本系統發布） */
export const publishedByEnum = pgEnum("published_by", ["user", "line_user", "auto", "external"]);
/** AI 草稿狀態：none=尚未處理、done=已有草稿、failed=重試後仍失敗（請手動回覆）、quota_exceeded=額度用完 */
export const aiStatusEnum = pgEnum("ai_status", ["none", "done", "failed", "quota_exceeded"]);
export const subscriptionStatusEnum = pgEnum("subscription_status", [
  "trialing",
  "active",
  "past_due",
  "canceled",
  "expired",
]);

// ---------- 帳號與店家 ----------
export const organizations = pgTable("organizations", {
  id: id(),
  name: text("name").notNull(),
  createdAt: createdAt(),
});

export const users = pgTable("users", {
  id: id(),
  /** 一個 user 屬於一個 organization */
  organizationId: uuid("organization_id").references(() => organizations.id, { onDelete: "set null" }),
  googleSub: text("google_sub").notNull().unique(),
  email: text("email").notNull(),
  name: text("name"),
  createdAt: createdAt(),
});

export const googleCredentials = pgTable("google_credentials", {
  id: id(),
  userId: uuid("user_id").notNull().references(() => users.id, { onDelete: "cascade" }).unique(),
  /** AES-GCM 加密後的 refresh token，資料庫中不得出現明文 */
  refreshTokenEncrypted: text("refresh_token_encrypted").notNull(),
  scopes: text("scopes"),
  status: credentialStatusEnum("status").notNull().default("active"),
  updatedAt: ts("updated_at").notNull().defaultNow(),
  createdAt: createdAt(),
});

export const locations = pgTable(
  "locations",
  {
    id: id(),
    organizationId: uuid("organization_id").notNull().references(() => organizations.id, { onDelete: "cascade" }),
    googleLocationId: text("google_location_id").notNull(),
    name: text("name").notNull(),
    industry: industryEnum("industry").notNull().default("other"),
    enabled: boolean("enabled").notNull().default(false),
    /** 首次啟用時的歷史評論匯入是否完成（歷史評論不發通知，見 F3） */
    initialImportDone: boolean("initial_import_done").notNull().default(false),
    lastSyncedAt: ts("last_synced_at"),
    createdAt: createdAt(),
  },
  (t) => [
    uniqueIndex("locations_org_google_idx").on(t.organizationId, t.googleLocationId),
    index("locations_enabled_idx").on(t.enabled),
  ],
);

export const brandSettings = pgTable("brand_settings", {
  id: id(),
  organizationId: uuid("organization_id").notNull().references(() => organizations.id, { onDelete: "cascade" }).unique(),
  tone: text("tone").notNull().default("親切"),
  salutation: text("salutation").notNull().default("我們"),
  signature: text("signature"),
  storeInfo: text("store_info"),
  autoReplyEnabled: boolean("auto_reply_enabled").notNull().default(false),
  updatedAt: ts("updated_at").notNull().defaultNow(),
});

// ---------- LINE ----------
export const lineBindings = pgTable(
  "line_bindings",
  {
    id: id(),
    organizationId: uuid("organization_id").notNull().references(() => organizations.id, { onDelete: "cascade" }),
    lineUserId: text("line_user_id").notNull(),
    displayName: text("display_name"),
    createdAt: createdAt(),
  },
  (t) => [uniqueIndex("line_bindings_org_user_idx").on(t.organizationId, t.lineUserId)],
);

/** 每個 LINE 用戶可接收哪些店點的通知 */
export const lineBindingLocations = pgTable(
  "line_binding_locations",
  {
    lineBindingId: uuid("line_binding_id").notNull().references(() => lineBindings.id, { onDelete: "cascade" }),
    locationId: uuid("location_id").notNull().references(() => locations.id, { onDelete: "cascade" }),
  },
  (t) => [primaryKey({ columns: [t.lineBindingId, t.locationId] })],
);

// ---------- 評論與 AI ----------
export const reviews = pgTable(
  "reviews",
  {
    id: id(),
    googleReviewId: text("google_review_id").notNull(),
    locationId: uuid("location_id").notNull().references(() => locations.id, { onDelete: "cascade" }),
    starRating: integer("star_rating").notNull(),
    comment: text("comment"),
    /** 個資：只用於顯示，不用於分析或對外輸出 */
    reviewerDisplayName: text("reviewer_display_name"),
    createTime: ts("create_time").notNull(),
    updateTime: ts("update_time").notNull(),
    isEdited: boolean("is_edited").notNull().default(false),
    /** 首次匯入的歷史評論：不發通知，列在「尚未回覆的歷史評論」 */
    isHistorical: boolean("is_historical").notNull().default(false),
    status: reviewStatusEnum("status").notNull().default("pending"),
    aiStatus: aiStatusEnum("ai_status").notNull().default("none"),
    createdAt: createdAt(),
  },
  (t) => [
    uniqueIndex("reviews_google_review_idx").on(t.locationId, t.googleReviewId),
    index("reviews_location_status_idx").on(t.locationId, t.status),
    check("reviews_star_rating_range", sql`${t.starRating} between 1 and 5`),
  ],
);

export const reviewAnalyses = pgTable("review_analyses", {
  id: id(),
  reviewId: uuid("review_id").notNull().references(() => reviews.id, { onDelete: "cascade" }).unique(),
  sentiment: sentimentEnum("sentiment").notNull(),
  topics: jsonb("topics").$type<string[]>().notNull().default([]),
  language: languageEnum("language").notNull(),
  needsHuman: boolean("needs_human").notNull().default(false),
  riskFlags: jsonb("risk_flags").$type<string[]>().notNull().default([]),
  createdAt: createdAt(),
});

export const replyDrafts = pgTable(
  "reply_drafts",
  {
    id: id(),
    reviewId: uuid("review_id").notNull().references(() => reviews.id, { onDelete: "cascade" }),
    content: text("content").notNull(),
    version: integer("version").notNull().default(1),
    model: text("model").notNull(),
    tokensIn: integer("tokens_in").notNull().default(0),
    tokensOut: integer("tokens_out").notNull().default(0),
    cost: usd("cost").notNull().default("0"),
    createdAt: createdAt(),
  },
  (t) => [uniqueIndex("reply_drafts_review_version_idx").on(t.reviewId, t.version)],
);

export const replies = pgTable("replies", {
  id: id(),
  /** 一則評論在 Google 上只有一則店家回覆 → 同時作為發布冪等的保護 */
  reviewId: uuid("review_id").notNull().references(() => reviews.id, { onDelete: "cascade" }).unique(),
  content: text("content").notNull(),
  publishedAt: ts("published_at"),
  publishStatus: publishStatusEnum("publish_status").notNull().default("pending"),
  publishedBy: publishedByEnum("published_by").notNull(),
  publishedByLineUserId: text("published_by_line_user_id"),
  errorMessage: text("error_message"),
  createdAt: createdAt(),
});

// ---------- 訂閱與帳務 ----------
export const subscriptions = pgTable("subscriptions", {
  id: id(),
  organizationId: uuid("organization_id").notNull().references(() => organizations.id, { onDelete: "cascade" }).unique(),
  /** 對應 src/config/plans.ts 的 PlanCode */
  plan: text("plan").notNull().default("trial"),
  status: subscriptionStatusEnum("status").notNull().default("trialing"),
  trialEndsAt: ts("trial_ends_at"),
  currentPeriodEnd: ts("current_period_end"),
  cancelAtPeriodEnd: boolean("cancel_at_period_end").notNull().default(false),
  /** 降級於下期生效時暫存 */
  pendingPlan: text("pending_plan"),
  ecpayMerchantTradeNo: text("ecpay_merchant_trade_no"),
  ecpayGwsr: text("ecpay_gwsr"),
  createdAt: createdAt(),
  updatedAt: ts("updated_at").notNull().defaultNow(),
});

export const invoices = pgTable("invoices", {
  id: id(),
  organizationId: uuid("organization_id").notNull().references(() => organizations.id, { onDelete: "cascade" }),
  subscriptionId: uuid("subscription_id").references(() => subscriptions.id, { onDelete: "set null" }),
  /** 綠界回傳通知的交易編號，用於冪等處理 */
  ecpayTradeNo: text("ecpay_trade_no").unique(),
  invoiceNumber: text("invoice_number"),
  amountTwd: integer("amount_twd").notNull(),
  /** 統一編號（選填） */
  taxId: text("tax_id"),
  createdAt: createdAt(),
});

export const usageCounters = pgTable(
  "usage_counters",
  {
    organizationId: uuid("organization_id").notNull().references(() => organizations.id, { onDelete: "cascade" }),
    /** 格式 YYYY-MM（Asia/Taipei 月份） */
    month: text("month").notNull(),
    draftsUsed: integer("drafts_used").notNull().default(0),
    /** 管理員手動調整額度 */
    draftsBonus: integer("drafts_bonus").notNull().default(0),
  },
  (t) => [primaryKey({ columns: [t.organizationId, t.month] })],
);

export const monthlyReports = pgTable(
  "monthly_reports",
  {
    id: id(),
    locationId: uuid("location_id").notNull().references(() => locations.id, { onDelete: "cascade" }),
    month: text("month").notNull(),
    data: jsonb("data").notNull(),
    createdAt: createdAt(),
  },
  (t) => [uniqueIndex("monthly_reports_location_month_idx").on(t.locationId, t.month)],
);

// ---------- 引流工具 ----------
export const geoChecks = pgTable(
  "geo_checks",
  {
    id: id(),
    storeName: text("store_name").notNull(),
    area: text("area").notNull(),
    industry: text("industry").notNull(),
    results: jsonb("results"),
    cost: usd("cost").notNull().default("0"),
    /** 只存雜湊，不存原始 IP */
    ipHash: text("ip_hash").notNull(),
    createdAt: createdAt(),
  },
  (t) => [index("geo_checks_lookup_idx").on(t.storeName, t.area, t.industry), index("geo_checks_ip_idx").on(t.ipHash, t.createdAt)],
);

export const leads = pgTable("leads", {
  id: id(),
  email: text("email"),
  lineUserId: text("line_user_id"),
  source: text("source").notNull(),
  geoCheckId: uuid("geo_check_id").references(() => geoChecks.id, { onDelete: "set null" }),
  createdAt: createdAt(),
});

// ---------- 成本紀錄 ----------
export const aiUsageLogs = pgTable(
  "ai_usage_logs",
  {
    id: id(),
    /** 健檢工具的呼叫沒有 organization */
    organizationId: uuid("organization_id").references(() => organizations.id, { onDelete: "set null" }),
    purpose: text("purpose").notNull(), // classify | write | report | geo_check ...
    model: text("model").notNull(),
    tokensIn: integer("tokens_in").notNull().default(0),
    tokensOut: integer("tokens_out").notNull().default(0),
    cost: usd("cost").notNull().default("0"),
    createdAt: createdAt(),
  },
  (t) => [index("ai_usage_logs_org_time_idx").on(t.organizationId, t.createdAt)],
);
