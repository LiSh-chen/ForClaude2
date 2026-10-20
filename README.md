# news-curator

從大量新聞來源中,挑出「新穎、有洞見、影響深遠、有證據」的少數文章,定期寄給訂閱者。寧缺勿濫:沒有達標內容就不寄。

流程:抓 RSS → 去重/排除已寄 → 評分 → 門檻+多樣性篩選 → 寄信 → 記錄已寄。

## 評分
Claude 對每篇文章給 novelty / depth / impact / evidence (0–10) 與 trivia(瑣事程度,最多扣 40%),
總分 0–100,預設 ≥55 才入選,單一來源最多 2 篇。沒有 `ANTHROPIC_API_KEY` 時退回關鍵字啟發式(品質明顯較差)。

## 使用
```bash
python -m curator run --dry-run --out preview.html   # 預覽,不寄信
python -m curator subscribe you@example.com           # 本機名單 subscribers.txt (已 gitignore)
python -m pytest tests
```
可調參數:`--days --top --min-score --per-source --model --sources`。來源在 `sources.json`。

## 部署 (GitHub Actions)
Repo Secrets:`ANTHROPIC_API_KEY`、`NEWS_SUBSCRIBERS`(逗號或換行分隔)、`SMTP_HOST`、`SMTP_PORT`、`SMTP_USER`、`SMTP_PASSWORD`、`MAIL_FROM`。
`.github/workflows/digest.yml` 預設週一、週四寄出。名單放 Secret 而非檔案,因為 email 是個資。

## 生活冷知識模式 (`--profile gems`)
針對「主流新聞太瑣碎,想要貼近生活、實用/有趣、但少人知道」的需求。同一條流程,換一套選稿哲學:
- 評分維度:practical(用得上)、surprise(反直覺)、life(貼近生活)、evidence(可查證);
  扣分:mainstream(已廣為人知,最多 −40%)、trivia(雜訊/業配,最多 −40%)。未經證實的養生/理財偏方會被壓低。
- 來源 `sources_gems.json`:偏小眾與一手來源(研究新聞稿、科普、冷門專欄)。
- 入選後 Claude 再讀內文,寫成「發生什麼 / 為什麼少人知道 / 怎麼用 / 注意事項 + 信心程度」,只根據原文不外加事實。
- `--interests "育兒, 理財"` 可依讀者關注微調;預設 ≥50 分才入選、每期最多 5 則,沒達標就不寄。
- 已寄紀錄 `data/seen_gems.json`;排程 `.github/workflows/gems.yml`(週三、週日)。可用 repo variable `GEMS_INTERESTS` 設主題。
```bash
python -m curator run --profile gems --dry-run --out gems.html
```

### 審稿流程 (每天約 30 分鐘)
`collect`(每天自動)→ 你審 `data/queue/gems.md` → `approve` → `send`(週三、週日自動,只寄已核准的;沒有核准就不寄)。
```bash
python -m curator collect                 # 抓取+評分+寫短文,放進待審佇列
python -m curator queue list              # 看待審項目 (或直接開 data/queue/gems.md)
python -m curator queue approve 1 3 5     # 核准 / reject / unapprove
python -m curator send --dry-run --out p.html
```
在 GitHub 手機版也能操作:Actions → Gems digest → Run workflow → 選 task、填 ids。
要改短文內容,直接編輯 `data/queue/gems.json` 中該項的 `detail` 再核准。待審超過 14 天自動過期。

## 已知限制
- 退訂目前是「回信說 unsubscribe」,由你手動改 Secret;訂閱量大時需要另做訂閱/退訂網頁。
- `sources.json` 的 feed 網址未經實際連線驗證(開發環境網路受限),首次執行看日誌中的 ✓/✗ 並修正。
- 評分只看標題+摘要,未讀全文。

---

# 投資研究日誌系統

另含一套每日財經新聞 → 產業 → 個股 → 目標價 → 研究日誌的系統，並定期檢討績效、回饋參數。
說明見 [docs/invest-journal.md](docs/invest-journal.md)，方法論見 [docs/methodology.md](docs/methodology.md)。
程式在 `ijournal/`，設定在 `config/`，workflow 為 `invest_journal_daily.yml` / `invest_journal_review.yml`。
