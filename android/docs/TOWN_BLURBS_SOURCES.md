# 區域介紹：來源與查證狀態（2026-10-19 更新）

`app/src/main/res/raw/town_blurbs.txt` 目前有 104 個鄉鎮區（1–3 個標籤），另有約 15 區尚無可用標籤，之後補。

來源與流程：
1. 第一輪（`docs/research/towns_NN.txt` + `towns_NN_sources.md`）：背景查證，每個事實要有兩個獨立來源網頁。
2. 第二輪：Gemini 給出候選（`gemini_reply_1.txt`），其附的來源多為網站首頁、不可當證據；再逐條用網頁搜尋核對（`verify_out_N.txt`），判定 OK／FIX（改寫成只含有證據支持的部分）／BAD／?（額度用完未查）。BAD 與 ? 不收入。

**限制（請知悉）**：本環境連不上中文維基百科與多數網頁，所有核對都只看到搜尋摘要，沒有開過原文；每個事實的兩個網址與句子的對應是依摘要推定。上線前需要人工抽查，尤其是 `verify_out_*.txt` 的 OK／FIX 項目與標示「待複核」的來源表。

舊稿（106 區一句話）在 `town_blurbs_old_unverified.txt`，**不可使用**。
