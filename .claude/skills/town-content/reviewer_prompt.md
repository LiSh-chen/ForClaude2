請依 /home/user/ForClaude2/android/docs/SOP_TOWN_CONTENT.md（v3.4）與 REVIEW_HANDBOOK.md 審查下列區域的候選池與擬定條目（只審不改）。檔案在 /home/user/ForClaude2/android/docs/research/candidate_pools/<縣市>_<區>.md，末尾 ```proposal 區塊是擬定條目。
每區要做：
1. 逐條擬定句判 PASS/FAIL：Q1–Q7、類型模板必備資訊、證據分級 E1–E4（稱號需兩個各自發布來源；傳說用相傳；變動數字）、時效詞、特定店名、行政區（D9：官方地址或可靠來源；存疑要查）、字數 14–90、禁用字。每個 FAIL 要標決議編號（D1–D17 或 S1 無適用規則）與建議改寫句（比現有規則更嚴者可自動套用）。
2. 查證：用 Bash curl（-A "ForClaude2-research/1.0"，zh.wikipedia.org 可讀，429 就等 retry-after）與 WebSearch 核對研究員標記「只見摘要／需核對」的事實；你的搜尋額度若用完，如實標示「無法查證」，不得當作通過。
3. 漏選檢查（D17）：不看研究員結論，自己判斷維基整頁與其他來源中該區的前 3 個特色，與擬定條目比對。
4. 研究員標示的「建議放回」項目（使用者曾移除者）：判斷是否為核心特色、是否值得放回，給結論與建議句；使用者同意前不寫入。
5. 條目數量是否符合 D10/D11。
輸出：每區一節，附「最終建議條目（完整句子）」與狀態（PASS／需第2輪改寫／刪除），寫到 /home/user/ForClaude2/android/docs/research/{OUT}，回報精簡摘要（每區 PASS/FAIL 數與需決策項）。
