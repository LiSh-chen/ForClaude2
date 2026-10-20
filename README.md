# news-curator

可複製的「人工把關型」電子報引擎:從大量 RSS 來源挑出少數真正有價值的內容,
**每天備好候選 → 你花約 30 分鐘審稿 → 才寄出**。沒有核准的內容就不寄,寧缺勿濫。

引擎不含任何題材。每份電子報是 `newsletters/<名稱>/` 一個目錄(評分標準 `rubric.md`、參數 `newsletter.json`、來源 `sources.json`)。
內建兩份:`gems`(生活冷知識:實用、反直覺、少人知道)、`insight`(大議題精選)。

```bash
python -m curator newsletters                              # 列出電子報
python -m curator init pets --brief "給養貓新手的飼主,要科學證據、不要業配"   # 由 Claude 產生定義草稿 (需 ANTHROPIC_API_KEY)
python -m curator init pets --copy-from gems               # 或複製現有的再手改
python -m curator check pets --prune                       # 連線驗證來源,移除壞掉的
python -m curator collect --profile pets                   # 抓取+評分+寫短文 → 待審佇列
python -m curator queue list --profile pets                # 審稿 (或看 data/queue/pets.md)
python -m curator queue approve 1 3 --profile pets
python -m curator send --profile pets --dry-run --out p.html
python -m pytest tests
```

完整說明(自己用、給別人用的模板流程、部署、風險)見 [docs/newsletter-engine.md](docs/newsletter-engine.md)。
`digest.yml` 為舊的全自動 insight 流程,新流程用 `newsletter.yml`。

---

# 投資研究日誌系統

另含一套每日財經新聞 → 產業 → 個股 → 目標價 → 研究日誌的系統，並定期檢討績效、回饋參數。
說明見 [docs/invest-journal.md](docs/invest-journal.md)，方法論見 [docs/methodology.md](docs/methodology.md)。
程式在 `ijournal/`，設定在 `config/`，workflow 為 `invest_journal_daily.yml` / `invest_journal_review.yml`。
