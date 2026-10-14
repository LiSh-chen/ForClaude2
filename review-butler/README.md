# 好評管家（暫名）

用 AI 幫台灣在地店家回覆 Google 評論：抓新評論 → AI 產生個人化繁中草稿 → LINE 推播 → 一鍵發布。
完整需求見 [SPEC.md](./SPEC.md)（開發順序依第 9 節分階段進行）。

> 目前進度：**階段 1（核心流程 MVP）完成**：模擬 Google 登入 → 抓評論 → AI 分類與草稿 → 後台編輯、發布（到 mock）。LINE（階段 2）、真實 Google（階段 3）、付款（階段 4）尚未實作。

## 技術組合
Next.js 16（App Router）· TypeScript · Tailwind CSS v4 · shadcn/ui · Drizzle ORM（PostgreSQL）· Vitest

## 本機啟動
需求：Node.js 20+。**不需要先裝資料庫**——沒設定 `DATABASE_URL` 時，本機會用內嵌的 PGlite（資料放在 `.data/pglite`，自動建表）。

```bash
cd review-butler
npm install
npm run dev                    # http://localhost:3000
```
打開 http://localhost:3000 → 用任一 Email「模擬 Google 登入」（換 Email = 另一家店）→ 選擇店點並啟用 → 在評論頁用「開發工具：模擬一則新評論」就能看到草稿與發布流程。

### 用真實的 Claude 產生草稿
預設全部走 mock（草稿是固定樣板，只用來驗證流程）。要看真實文案：
```bash
cp .env.example .env.local
# 填入 ANTHROPIC_API_KEY，並設定：
#   USE_MOCK_ADAPTERS=true   # Google / LINE / 金流仍是 mock
#   USE_MOCK_LLM=false       # 只有 AI 用真的
# （選填）LLM_MODEL_CLASSIFY / LLM_MODEL_WRITE，預設 claude-haiku-5-5 / claude-sonnet-5-5
npm run dev
```
每次 AI 呼叫的 token 與成本會寫入 `ai_usage_logs`。

### 使用自己的 PostgreSQL（Supabase / Neon / 本機）
在 `.env.local` 設定 `DATABASE_URL`，執行 `npm run db:migrate`。正式環境**必須**設定 `DATABASE_URL`、`ENCRYPTION_KEY`、`SESSION_SECRET`（缺少時會直接報錯，不會用開發預設值）。

## 常用指令
| 指令 | 說明 |
|---|---|
| `npm test` | 執行測試（用記憶體內的 PGlite，不需外部資料庫、不需金鑰） |
| `npm run eval:drafts` | **真實模型評測**：把 `tests/fixtures/reviews.json` 的 38 則評論送進真實的分類＋撰寫流程，印出草稿、違規、風險召回與成本。需 `ANTHROPIC_API_KEY`，會花少量費用 |
| `npm run typecheck` | TypeScript 型別檢查 |
| `npm run db:generate` | 修改 `src/lib/db/schema.ts` 後產生新的 migration |
| `npm run build` | 正式建置 |

## 目錄
```
src/
  app/                 Next.js 路由
  components/ui/       shadcn/ui 元件（可用 `npx shadcn add <name>` 新增）
  config/              方案額度（plans.ts）、產業別與預設主題（industries.ts）
  lib/
    adapters/          外部服務抽象：google-business / line / llm / payment
                       每個都有 interface.ts + mock，real 實作留待對應階段
    db/                Drizzle schema、連線、migrations
    ai/ jobs/          之後階段使用
tests/
```

## Adapter 約定
- 一律透過 `src/lib/adapters/index.ts` 的 `getGoogleBusiness()` / `getLine()` / `getLlm()` / `getPayment()` 取得實例，不直接 new。
- `USE_MOCK_ADAPTERS` 未設定時視為 `true`，避免誤打正式 API。
- Real adapter 目前都只是會拋 `NotImplementedError` 的占位。
- Mock 提供測試鉤子：`MockLineAdapter.sent`、`MockGoogleBusinessAdapter.revoked / addReview()`、`MockLlmAdapter.calls` 等。

## 階段 1 的行為與設計決定
- **草稿流程**：分類（小模型，無文字的評論不呼叫 AI）→ 撰寫（大模型）→ 程式檢查（網址、促銷、要求改評論、長度 40–120、語言、繁體、佔位符）→ 不合格就帶著原因重寫，最多重試 2 次，仍失敗就標示「請手動回覆」，不存壞草稿。
- **語言**用字元判斷，不靠模型；**情緒**與**風險標記**由模型判斷。評論者姓名不會送進 AI。評論內容以 `<review>` 包住並聲明為資料，防提示注入。
- **風險提醒**（食安、法律、人身攻擊、疑似假評論）顯示在介面上，**不寫進草稿本文**（否則一鍵發布時會被一起發到 Google）；這類評論必須勾選確認才能發布。
- **額度**：每次產生草稿（含「換一個版本」）以單一 SQL 原子扣 1 則，併發下不會超額；AI 最終失敗會歸還。超額時仍同步評論，只是不產生草稿。月份以台灣時間計算。
- **發布冪等**：以 `replies.review_id` 唯一索引搶佔，重複點擊或併發請求只會呼叫 Google 一次；失敗後可重試。
- **首次啟用店點**匯入最近 50 則歷史評論：不產生草稿、不通知，列在「歷史評論」讓店家挑選。
- **排程**：階段 1 尚無 Inngest，用「立即檢查新評論」按鈕觸發；同步邏輯在 `src/lib/jobs/`，之後直接由排程呼叫。
- Real adapter（Google／LINE／綠界）仍為 `NotImplementedError` 占位。Claude 的 refusal 目前視為失敗（請手動回覆），尚未啟用 server-side fallback。
