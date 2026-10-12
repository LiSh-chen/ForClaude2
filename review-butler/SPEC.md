# 好評管家（暫名）— 開發規格書

> 給 Claude Code 的說明：這份文件是整個專案的需求來源。請依「第 9 節 開發階段」的順序實作，每完成一個階段就停下來讓我確認。遇到規格沒寫清楚的地方，先問我，不要自行假設商業邏輯。
>
> 外部 API（Google Business Profile、LINE、綠界）的端點與欄位，實作前請以官方最新文件為準；本文件中的端點僅供參考。

---

## 1. 產品概述

### 1.1 一句話說明
幫台灣在地店家用 AI 回覆 Google 評論：每天自動抓新評論，AI 依評論內容產生個人化的繁體中文回覆草稿，透過 LINE 推給店家，一鍵確認即發布到 Google；每月產出顧客抱怨主題報告。

### 1.2 目標用戶
- 台灣的餐廳、咖啡廳、美業工作室、診所、補習班等有 Google 商家檔案的中小店家
- 店主通常沒時間、不擅長寫回覆，主要用手機和 LINE 處理事情

### 1.3 要解決的痛點
1. 店家通常只回好評，最需要處理的負評反而沒人回
2. 消費者會看店家怎麼回負評再決定是否消費
3. Google 會審核店家回覆，罐頭範本或推銷語氣可能被拒絕，因此需要針對每則評論個人化撰寫

### 1.4 商業目標
- 月營收 NT$25,000（約 70 家付費店家）
- 引流工具：免費「AI 能見度健檢」頁面，收集潛在客戶 email／LINE

---

## 2. 方案與定價

| 方案 | 月費 | 內容 |
|---|---|---|
| 免費試用 | 14 天 | 進階方案全部功能，不需綁卡 |
| 基本 | NT$299 | 1 個店點、每月 100 則 AI 回覆草稿、LINE 通知 |
| 進階 | NT$490 | 最多 3 個店點、每月 400 則草稿、月度抱怨主題報告、好評自動回覆 |

- 方案額度要寫在設定檔或資料庫，不要寫死在程式碼中
- 超過額度時：停止產生 AI 草稿，但仍照常通知新評論，並提示升級

---

## 3. 技術架構

### 3.1 建議技術組合（可討論調整）
| 層級 | 選擇 |
|---|---|
| 前端與後端 | Next.js（App Router）＋ TypeScript |
| UI | Tailwind CSS＋shadcn/ui，**以手機版為優先設計** |
| 資料庫 | PostgreSQL（Supabase 或 Neon） |
| ORM | Drizzle ORM |
| 背景排程 | Inngest（或 BullMQ＋Redis，擇一） |
| 驗證 | Google OAuth（同時取得 Business Profile 授權）＋ LINE Login 綁定 |
| AI | Claude API（模型名稱寫在環境變數，分類用小模型、寫回覆用大模型） |
| 通知 | LINE Messaging API（Flex Message＋LIFF） |
| 金流 | 綠界 ECPay 定期定額＋電子發票 API |
| 部署 | Vercel（Web）＋ Inngest Cloud（排程） |
| 錯誤監控 | Sentry |

### 3.2 外部服務抽象化（重要）
所有外部服務都要包成 adapter 介面，並提供 **mock 實作**：

```
src/lib/adapters/
  google-business/   interface.ts, real.ts, mock.ts
  line/              interface.ts, real.ts, mock.ts
  llm/               interface.ts, claude.ts, mock.ts
  payment/           interface.ts, ecpay.ts, mock.ts
```

原因：Google Business Profile API 需要另外申請存取權限，審核期間無法呼叫正式 API。開發與測試階段一律使用 mock，以環境變數 `USE_MOCK_ADAPTERS=true` 切換。mock 要能產生各種星等、有無文字、中英文混雜的假評論。

### 3.3 建議目錄結構
```
src/
  app/
    (marketing)/          首頁、定價、健檢工具頁
    (app)/dashboard/      店家後台
    liff/                 LINE 內開啟的頁面（編輯回覆）
    api/
      auth/               OAuth callback
      line/webhook/       LINE webhook
      payment/ecpay/      綠界回傳通知
      inngest/            排程進入點
  lib/
    adapters/             見 3.2
    db/                   schema 與查詢
    ai/                   prompt 與 AI 呼叫邏輯
    jobs/                 背景任務
  components/
tests/
```

---

## 4. 功能需求

### F1. 註冊與連結 Google 商家
- 店家用 Google 帳號登入，授權範圍需包含 Business Profile 管理權限（`https://www.googleapis.com/auth/business.manage`）
- 登入後列出該帳號管理的所有店點，讓店家勾選要啟用的店點（受方案上限限制）
- 安全儲存 refresh token（加密後存入資料庫），存取權杖過期時自動更新
- 授權失效時，後台顯示提示並發 LINE 通知請店家重新授權

**驗收條件**
- [ ] 可以完成 Google 登入並看到店點清單
- [ ] refresh token 以加密形式儲存，資料庫中看不到明文
- [ ] 撤銷授權後，系統能偵測並通知

### F2. 綁定 LINE
- 後台顯示「綁定 LINE」按鈕，透過 LINE Login 取得店家的 LINE userId，並引導加入本服務的 LINE 官方帳號好友
- 一個店家帳號可綁定多個 LINE 用戶（例如老闆和店長），每人可設定接收哪些店點的通知

**驗收條件**
- [ ] 綁定後，系統可以推播測試訊息到該 LINE 帳號
- [ ] 可解除綁定

### F3. 抓取新評論
- 排程每 30 分鐘輪詢每個啟用店點的評論（之後可改用 Google 的新評論推播通知機制）
- 新評論存入資料庫；已存在的評論若內容有修改，更新並標記為「已編輯」
- 首次啟用店點時，匯入最近 50 則評論，但**不要**對歷史評論發通知，只在後台列出「尚未回覆的歷史評論」讓店家選擇處理
- 要處理 API 速率限制與錯誤重試（指數退避）

**驗收條件**
- [ ] 新評論在 30 分鐘內出現在後台
- [ ] 不會重複建立同一則評論
- [ ] 單一店點 API 失敗不影響其他店點

### F4. AI 產生回覆草稿
流程分兩步：

**步驟一：分類（小模型）**
輸出 JSON：
```json
{
  "sentiment": "positive | neutral | negative",
  "topics": ["服務態度", "等候時間", "價格", "餐點品質", "環境衛生", "其他"],
  "language": "zh-TW | en | ja | other",
  "needs_human": true,
  "risk_flags": ["疑似假評論", "涉及食安", "涉及人身攻擊", "提到法律行動"]
}
```
- 主題清單要能依產業別設定（餐飲、美業、醫療等各有一組預設主題）

**步驟二：撰寫回覆（大模型）**
回覆規則（寫進 system prompt）：
1. 使用繁體中文與台灣用語；評論若是英文或日文，用相同語言回覆
2. 必須具體回應評論提到的內容，不可空泛
3. 一星、二星：先感謝、具體承認問題、說明會如何改善、邀請私下聯繫；**不可**爭辯、指責顧客或透露顧客個資
4. 四星、五星：感謝並提及評論中的具體細節，不要每則都一樣
5. **不可**包含促銷、折扣碼、網址、要求修改評論等內容（避免被 Google 審核拒絕）
6. 長度約 40–120 字
7. 遵守店家設定的語氣（親切、專業、活潑）、稱呼（我們／本店／店名）與簽名
8. 店家可提供「店家資訊」（營業時間、特色、常見問題）作為參考，但不可捏造店家沒提供的資訊
9. 若 `risk_flags` 不為空，草稿開頭加上給店家的提醒，例如「此評論涉及食安，建議親自處理」，且該則一律不可自動發布

- 每則評論產生 1 則草稿，店家可要求「換一個版本」（計入額度）
- AI 呼叫失敗時重試 2 次，仍失敗則通知店家「請手動回覆」

**驗收條件**
- [ ] 準備至少 30 則測試評論（各星等、中英文、含風險內容），草稿皆符合上述規則
- [ ] 草稿中不出現網址、折扣、促銷字樣（寫自動化檢查）
- [ ] 每次 AI 呼叫記錄 token 用量與成本

### F5. LINE 通知與一鍵發布
- 新評論加草稿完成後，推播 Flex Message 給綁定的 LINE 用戶，內容包含：店點名稱、星等、評論內容、AI 草稿、按鈕
- 按鈕：
  - 「✅ 發布」：postback 到 webhook，系統呼叫 Google API 發布回覆
  - 「✏️ 修改」：開啟 LIFF 頁面，可編輯草稿後發布
  - 「🔄 換一個」：重新產生草稿並推播
  - 「⏭️ 略過」：標記為不回覆
- 發布後回傳結果訊息（成功／失敗原因）
- 發布後定期檢查該回覆在 Google 上的狀態，若被拒絕或未成功顯示，通知店家並提供修改入口
- 推播要合併：同一店點 10 分鐘內多則評論，合併成一則訊息，以節省 LINE 推播額度

**驗收條件**
- [ ] 從收到 LINE 通知到發布完成，店家只需點一下
- [ ] 重複點擊「發布」不會重複發布（冪等處理）
- [ ] 只有綁定且有權限的 LINE 用戶能操作該店點

### F6. 好評自動回覆（進階方案）
- 店家可開啟「四星以上、無風險標記、有文字內容的評論自動發布」
- 一星到三星**永遠不自動發布**
- 自動發布後仍發 LINE 通知（內容為「已自動回覆」），並可在 24 小時內修改

### F7. 店家後台（網頁）
- 評論列表：可依店點、星等、狀態（待處理／已回覆／已略過／發布失敗）篩選
- 評論詳情：檢視、編輯、發布
- 設定頁：語氣、稱呼、簽名、產業別、店家資訊、通知對象、自動回覆開關
- 用量頁：本月已用草稿數與剩餘額度
- 帳務頁：目前方案、付款紀錄、發票、升降級、取消訂閱
- 全部頁面須支援手機瀏覽

### F8. 月度抱怨主題報告（進階方案）
- 每月 1 日產生上個月報告，以 LINE 推送摘要並附後台完整報告連結
- 內容：
  - 評論數、平均星等，與前一個月比較
  - 各主題出現次數（正面與負面分開統計）
  - 負面主題前 3 名，各附 2–3 則代表性評論摘錄
  - AI 產生的 3 點具體改善建議
  - 回覆率（含負評回覆率）
- 報告以網頁呈現，可匯出 PDF

### F9. 訂閱與付款
- 串接綠界定期定額（信用卡），每月自動扣款
- 付款成功後自動開立電子發票（綠界電子發票 API），可輸入統編
- 扣款失敗：通知店家，寬限 7 天；逾期後停止產生 AI 草稿，但保留資料
- 試用到期前 3 天與 1 天發 LINE 提醒
- 支援升級（立即生效、按比例計費或下期生效，請先問我）、降級（下期生效）、取消（本期結束後停止）
- 所有綠界回傳通知要驗證檢查碼，並以冪等方式處理

### F10. 免費「AI 能見度健檢」引流頁
- 公開頁面，不需登入
- 使用者輸入：店名、城市與行政區、產業類別
- 系統用 3–5 個與該產業相關的查詢詞（例如「大安區推薦的拉麵店」），透過 AI 模型 API 詢問，檢查：
  - 店名是否被提到
  - 被提到時的描述是否正確
  - 同區被提到的其他店家
- 結果頁顯示分數與建議，並在頁面中說明：**結果為透過 API 查詢的估計值，可能與消費者實際在 AI 助理看到的不同**
- 完整報告需留下 email 或加 LINE 好友才能查看（收集潛在客戶）
- 結果頁加上「Google 評論是 AI 判斷店家的重要依據，免費試用好評管家」的導流按鈕
- 防濫用：Cloudflare Turnstile 驗證、每 IP 每日 3 次、同店家結果快取 7 天
- 每次健檢的 AI 成本要記錄，並設定每日總預算上限，超過時暫停服務

---

## 5. 資料模型（初稿）

```
users                 店家帳號（Google 登入）
organizations         店家（一個 user 可屬於一個 organization）
locations             Google 店點（google_location_id, name, industry, enabled）
google_credentials    加密的 refresh token、授權狀態
line_bindings         LINE userId、可接收通知的店點
reviews               google_review_id, location_id, star_rating, comment,
                      reviewer_display_name, create_time, update_time, is_edited
review_analyses       sentiment, topics(json), language, risk_flags(json)
reply_drafts          review_id, content, version, model, tokens_in, tokens_out, cost
replies               review_id, content, published_at, publish_status,
                      published_by (user / line_user / auto), error_message
brand_settings        tone, salutation, signature, store_info, auto_reply_enabled
subscriptions         plan, status, trial_ends_at, current_period_end, ecpay_ids
invoices              發票號碼、金額、統編
usage_counters        organization_id, month, drafts_used
monthly_reports       location_id, month, data(json)
geo_checks            store_name, area, industry, results(json), cost, ip_hash
leads                 email / line_user_id, source, geo_check_id
ai_usage_logs         用途、模型、token、成本、時間
```

- 所有時間以 UTC 儲存，顯示時轉為 Asia/Taipei
- 評論者姓名屬於個人資料，只用於顯示，不用於分析或對外輸出

---

## 6. 非功能需求

### 6.1 安全與個資
- OAuth token、LINE channel secret 等敏感資料一律加密儲存或放在環境變數
- 遵守台灣《個人資料保護法》：提供隱私權政策、服務條款頁面；店家取消並要求刪除時，30 天內刪除所有資料
- 傳給 AI 模型的資料只包含必要內容（評論內容、星等、店家設定），不傳 email、token 等
- 所有 webhook（LINE、綠界）都要驗證簽章

### 6.2 成本控制
- 每次 AI 呼叫記錄 token 與成本，後台（管理員）可看每個店家的月成本
- 目標：單一店家每月 AI 成本低於月費的 20%
- 分類用小模型、撰寫用大模型，模型名稱可用環境變數切換
- LINE 推播合併發送，盡量使用免費的回覆訊息（reply token）

### 6.3 可靠性
- 背景任務要可重試、冪等
- 單一店家或店點的錯誤不可影響其他店家
- 重要錯誤送到 Sentry，並通知管理員

### 6.4 管理員後台（簡易版）
- 店家列表、訂閱狀態、用量與 AI 成本
- 手動延長試用、手動調整額度
- 健檢工具的使用量與成本

---

## 7. 環境變數

```
DATABASE_URL=
USE_MOCK_ADAPTERS=true
ENCRYPTION_KEY=

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

LINE_CHANNEL_ID=
LINE_CHANNEL_SECRET=
LINE_CHANNEL_ACCESS_TOKEN=
LINE_LOGIN_CHANNEL_ID=
LINE_LOGIN_CHANNEL_SECRET=
LIFF_ID=

ANTHROPIC_API_KEY=
LLM_MODEL_CLASSIFY=
LLM_MODEL_WRITE=

ECPAY_MERCHANT_ID=
ECPAY_HASH_KEY=
ECPAY_HASH_IV=
ECPAY_INVOICE_MERCHANT_ID=
ECPAY_INVOICE_HASH_KEY=
ECPAY_INVOICE_HASH_IV=

TURNSTILE_SITE_KEY=
TURNSTILE_SECRET_KEY=
GEO_CHECK_DAILY_BUDGET_USD=

INNGEST_EVENT_KEY=
INNGEST_SIGNING_KEY=
SENTRY_DSN=
APP_BASE_URL=
```

建立 `.env.example`，不要提交任何真實金鑰。

---

## 8. 測試要求
- AI 回覆品質：建立 `tests/fixtures/reviews.json`（至少 30 則多樣化評論），寫測試檢查草稿是否違反規則（含網址、促銷字詞、長度超出範圍、語言不符）
- adapter：每個外部服務的 mock 都要有對應的單元測試
- 關鍵流程整合測試：新評論 → 分類 → 草稿 → LINE 通知 → 發布 → 狀態更新
- 金流：綠界回傳通知的檢查碼驗證與冪等處理
- 額度：超過方案額度時的行為

---

## 9. 開發階段

### 階段 0：專案骨架
- 初始化 Next.js、TypeScript、Tailwind、shadcn/ui、Drizzle、測試框架
- 建立資料庫 schema 與 migration
- 建立所有 adapter 介面與 mock 實作
- 建立 `.env.example` 與 README（含本機啟動步驟）

### 階段 1：核心流程 MVP（全部使用 mock）
- F1 Google 登入（mock 店點）
- F3 抓取評論（mock 評論）
- F4 AI 分類與草稿（使用真實 Claude API）
- F7 店家後台：評論列表、編輯、發布、設定頁
- 完成後，我要能在本機看到假評論、產生真實的 AI 草稿並「發布」到 mock

### 階段 2：LINE 整合
- F2 LINE 綁定
- F5 LINE 通知、Flex Message、LIFF 編輯頁、一鍵發布
- 推播合併邏輯

### 階段 3：串接真實 Google API
- 取得 Google Business Profile API 存取權限後，實作 real adapter
- 處理授權失效、速率限制、回覆狀態檢查
- 用我自己的店點實測

### 階段 4：付款與方案
- F9 綠界定期定額、電子發票
- 方案額度、試用期、寬限期邏輯
- F7 帳務頁、用量頁

### 階段 5：進階功能
- F6 好評自動回覆
- F8 月度報告
- 管理員後台

### 階段 6：引流工具
- F10 AI 能見度健檢頁
- 首頁、定價頁、隱私權政策、服務條款頁

---

## 10. 開發以外需要我處理的事（不需要 Claude Code 實作）
這些項目審核時間較長，請在開發初期就開始申請：
1. **申請 Google Business Profile API 存取權限**：需在 Google Cloud 專案中提出申請，核准前 API 配額為 0
2. **Google OAuth 應用程式驗證**：使用 `business.manage` 權限的正式上線應用通常需要通過 Google 驗證
3. **LINE 官方帳號**：建立 Messaging API channel、LINE Login channel、LIFF app；確認方案的推播額度與費用
4. **綠界**：申請特店、開通定期定額與電子發票（開立發票需要辦理商業登記）
5. **隱私權政策與服務條款**：上線前準備好正式內容
6. **網域與品牌名稱**：確認商標與網域可用
7. **注意**：LINE Notify 已停止服務，通知一律使用 LINE Messaging API

---

## 11. 未來可能的擴充（目前不要實作）
- 多語系介面
- 支援其他評論平台
- 邀請滿意顧客留下評論的 QR Code 工具
- 連鎖品牌多店管理
- 英文版，進軍海外市場
