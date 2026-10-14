# 好評管家（暫名）

用 AI 幫台灣在地店家回覆 Google 評論：抓新評論 → AI 產生個人化繁中草稿 → LINE 推播 → 一鍵發布。
完整需求見 [SPEC.md](./SPEC.md)（開發順序依第 9 節分階段進行）。

> 目前進度：**階段 2（LINE 整合）完成**：模擬 Google 登入 → 抓評論 → AI 草稿 → **LINE 推播卡片、一鍵發布、LIFF 編輯、對話代理人** → 發布到 mock。真實 Google（階段 3）、付款（階段 4）尚未實作。LINE 部分已用「本機 LINE 模擬器」與假的 HTTP 層完整測試，但**尚未對真實 LINE channel 實測**（見下方清單）。

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

### LINE 模擬器（沒有真實 LINE 也能玩完整流程）
`npm run dev` 後：
1. 後台「LINE」頁 → 產生 6 位數綁定碼（或點「用 LINE 登入並綁定」，會進到模擬的 LINE Login 頁）。
2. 開 http://localhost:3000/dev/line → 新增一位 LINE 使用者 → 在對話框輸入綁定碼。
3. 回評論頁，用「開發工具：模擬一則新評論」→ 模擬器的聊天室會收到 Flex 卡片，按鈕都可以按（發布、修改→LIFF 編輯頁、換一個、略過），也可以打字：「待處理」「統計」「短一點」。
4. 同一店點 10 分鐘內只推播一次（其餘合併）；模擬器的「⏱ 執行排程一次」等同於排程呼叫 `/api/jobs/tick`。

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
| `npm run db:generate` | 修改 `src/lib/db/schema.ts` 後產生新的 migration（**只新增，不要改已提交的舊 migration**） |
| `npm run build` | 正式建置 |

## 階段 2：LINE 怎麼運作
| 項目 | 做法 |
|---|---|
| 綁定（F2） | 兩種：**LINE Login**（本人綁定，會要求加官方帳號好友），或**綁定碼**（後台產生 6 位數字、10 分鐘有效、只能用一次，貼到 LINE 對話框即可，適合請店長綁定）。可多人綁定，每人可勾選要接收哪些店點；可傳測試訊息、可解除綁定。 |
| 通知（F5） | 新評論 → AI 草稿 → Flex 卡片（店點、星等、評論、建議回覆、✅發布／✏️修改／🔄換一個／⏭️略過）。歷史評論不通知；額度用完或 AI 失敗也會通知，卡片改為「請自己回覆」。 |
| 推播合併 | 同一店點 10 分鐘內只推播一次，期間累積的合併成一則輪播（第一則不延遲）；每個輪播最多 10 張卡，其餘導向後台。 |
| 好評一次發布 | 輪播中有 ≥2 則「4 星以上、無風險、已有草稿」時多一張「全部發布」卡；只發布當時卡片上看到的那一版，且仍符合條件的項目。 |
| 一鍵發布與冪等 | 按鈕帶的是「草稿 id」→ 發布的就是使用者當時看到的那一版。重複點擊、LINE 重送 webhook（以 `webhookEventId` 去重）、併發請求都只會發布一次。 |
| 權限 | 每個動作都檢查：此 LINE 使用者已綁定、屬於該店家、且有勾選接收該店點。其他人的卡片、偽造的按鈕資料一律拒絕。 |
| 風險評論 | 卡片沒有一鍵發布，只有「查看並確認」→ LIFF 頁勾選確認後才能發布；伺服器端也強制檢查。 |
| 對話代理人 | 明確指令（說明／待處理／統計／略過）不呼叫 AI；其他訊息由小模型判斷意圖，目前只有「用說的修改草稿」會寫新版本（**計入月額度**，與「換一個」相同）。統計由程式查資料庫，不經 AI，不會被評論內容影響。 |
| 發布後檢查 | 排程檢查回覆是否真的顯示在 Google：被拒絕或 24 小時仍未顯示 → 標為失敗，LINE 通知並附修改入口。 |
| 授權失效 | 偵測到 Google 授權失效 → 後台顯示提示，並以 LINE 通知一次。 |
| 排程 | `GET/POST /api/jobs/tick`（需 `Authorization: Bearer $CRON_SECRET`）。建議**每分鐘**呼叫一次：同步 Google 自己限制為每店點 30 分鐘一次，其餘（送出到期通知、檢查回覆狀態）每次都跑。Vercel Cron 或之後的 Inngest 都可以。 |

### 上線前，用真實 LINE channel 走一遍的清單（尚未實測）
Messaging API／LINE Login 的端點與欄位依官方文件撰寫，並以假的 HTTP 層驗證了請求格式；請用**測試用的 channel** 確認：
1. Messaging API channel：Webhook URL 設為 `${APP_BASE_URL}/api/line/webhook` 並按 Verify；填入 `LINE_CHANNEL_SECRET`、`LINE_CHANNEL_ACCESS_TOKEN`。關閉「自動回應訊息」。
2. LINE Login channel：Callback URL 設為 `${APP_BASE_URL}/api/auth/line/callback`；填入 `LINE_LOGIN_CHANNEL_ID`、`LINE_LOGIN_CHANNEL_SECRET`；在 Console 把官方帳號連結到這個 Login channel（授權時才會提示加好友）。
3. LIFF app：Endpoint URL 設為 `${APP_BASE_URL}/liff`，scope 勾 `openid`、`profile`；填入 `LIFF_ID`。
4. 設 `USE_MOCK_LINE=false`（或 `USE_MOCK_ADAPTERS=false`）、`CRON_SECRET`，並讓排程每分鐘呼叫 `/api/jobs/tick`。
5. 實測重點：綁定（兩種）→ 新評論卡片 → 一鍵發布 → 修改（LIFF）→ 換一個 → 封鎖後解除；卡片在手機 LINE 上的版面（輪播張數上限各來源說法不一，程式保守取 10）。
6. 已知待確認：官方文件中 postback `data` 長度上限（目前每筆約 50 字元，遠低於常見的 300 上限）。

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

### 階段 2 已知限制與取捨
- **10 分鐘合併窗口**：同一店點剛推播過，之後 10 分鐘內的新評論（包括負評、風險評論）要等窗口結束才會通知。這是照 SPEC 的設計；若希望負評／風險評論不受限，可以在 `flushNotifications` 加例外。
- 「立即檢查新評論」按鈕直接同步，通知由下一次排程送出（最多約 1 分鐘）；授權失效的 LINE 通知同樣由排程送出。
- 沒有為 LINE 做額度外的用量限制；「用說的修改」每次都扣 1 則月額度，也會呼叫一次小模型判斷意圖。
- 尚未實作：好評自動回覆（階段 5）、月報（階段 5）、付款與方案變更（階段 4）。
