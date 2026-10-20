# 電子報引擎使用說明

## 設計
- **引擎**(`curator/`):抓取 → 去重 → 評分 → 寫短文 → 審稿佇列 → 寄信。不含題材。
- **電子報定義**(`newsletters/<名稱>/`):
  - `newsletter.json`:標題、權重、扣分項、門檻(`min_score`)、每期數量(`top`)、單一來源上限、`interests`、`categories`、`enrich`。
  - `rubric.md`:給評分者的說明。輸出格式由程式依 `weights`/`penalties` 自動附加,改 rubric 不會弄壞格式。
  - `sources.json`:RSS/Atom 來源。
- 評分 = 正向維度加權(0–100)× 各扣分項的連乘折減。`weights` 總和須為 1,`penalties` 每項 0–0.6,且必須含 `evidence`(由 `init` 檢查)。

## 日常流程(每天約 30 分鐘)
1. 排程每天 `collect`:新候選評分、寫短文,放進 `data/queue/<名稱>.json` 並產生 `data/queue/<名稱>.md`。
2. 你在 GitHub(手機也可)讀 `.md`,Actions → Newsletters → Run workflow:`task=approve`、`newsletter=gems`、`ids=1 3 5`。
   要改文字就直接編輯 `.json` 該項的 `detail`。
3. 週三、週日 `send` 只寄已核准的(依分數取前 N 則);沒有核准就不寄。待審超過 14 天自動過期。

## 部署(自己用,或 fork 當模板給別人)
1. Fork / 以此 repo 為模板。
2. Repo Secrets:`ANTHROPIC_API_KEY`、`NEWS_SUBSCRIBERS`、`SMTP_HOST`、`SMTP_PORT`、`SMTP_USER`、`SMTP_PASSWORD`、`MAIL_FROM`。
   名單放 Secret,不要 commit(email 是個資)。
3. Repo variable `NEWSLETTERS`:要跑的電子報,空白分隔(預設 `gems`)。
4. 先 `python -m curator check --prune` 驗證來源,再用 Actions 手動跑一次 `collect`、`send-dry`。
5. 自己的題材:`init` → 審 `rubric.md` 與 `newsletter.json` → `check --prune` → 加進 `NEWSLETTERS`。

費用由使用者自備的 API key 與 SMTP 承擔;引擎本身沒有伺服器。

## 責任與風險
- **審稿是預設,不是選配。** 健康、金錢、法律、安全類尤其不要略過人工把關。
- **著作權**:只用 RSS 摘要與短文改寫並附原文連結,不轉載全文、不抓付費牆內容。遵守各站條款。
- **`init --brief` 的來源網址是 LLM 憑記憶寫的**,一定要跑 `check`;rubric 也要人工讀過。
- **個資**:訂閱者 email 放 Secret;訂閱量大時需要正式的訂閱/退訂頁(目前退訂是回信)。

## 已知限制
- 評分只看標題+摘要;短文生成會讀內文前約 3500 字,資訊不足時會標示「僅依摘要」。
- 短文格式(發生什麼/為什麼少人知道/怎麼用/注意)目前固定,尚未能依電子報客製。
- 寄送排程是全域的(workflow 內),尚不能每份電子報各自設定。
- 來源網址尚未在可連網環境驗證過。
