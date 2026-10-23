# 還缺的圖片提示詞（共 1 張）

## 怎麼用

1. 每張開新對話（Gemini 等圖像 AI），**先上傳風格參考圖** `previews/style_reference.jpg`（現有景點卡的畫風）。**不附實景照片**，景點的外觀細節已寫在提示詞文字裡。App 圖示與商店主視覺例外：請改上傳角色設定圖 `generated/traveller_sheet.jpg`。
2. 比例、背景都已寫在提示詞裡。一律不要有文字；圖裡出現字就重生成。
3. 需要透明的（收藏格外框、App 圖示）提示詞已要求純白底，之後由工具去背；景點卡是一般圖，不用去背。
4. 存成 `generated/<id>.png`（檔名用標題裡的 id），傳回來給我，我匯出並接進 App。
5. 景點卡提示詞只寫「查證確認或單一來源」的外觀細節；查不到的顏色、尺寸沒有指定，圖像工具會自己決定，所以**這幾張可能仍與實景有差**：通霄精鹽廠、布袋、新竹城隍廟、大甲鎮瀾宮、桃園神社。


---

### `card_taoyuan` — 景點卡：桃園神社

- 用在：景點卡「桃園神社」：石燈籠夾道的神社參道
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Taoyuan Shinto shrine in Taoyuan, Taiwan, drawn exactly like this real view, looking straight up the central axis: in the foreground a single surviving grey stone torii gate whose upper curved crossbeam has been removed: only two round grey stone pillars and one straight horizontal beam between them remain, with the pillar tops left bare and flat where the curved beam used to sit, a small vertical plaque hanging in the middle of that beam (leave any characters as abstract marks), each pillar wrapped from mid-height to the base in a deep-blue banner with white brush marks (not readable); a plain grey concrete path leading to three wide low steps, then a long straight stone staircase climbing between two stone-rubble retaining walls to a raised terrace; at the top a dark-timber shrine hall with a low grey-tiled hip roof half hidden among trees; on each side of the staircase a small stone guardian lion on a pedestal, and in the foreground left and right two tall grey stone lanterns on stepped stone bases; a low wooden picket fence along the terrace, a small open-sided wooden water pavilion with a dark tiled roof at the far left, tall slim cypress trees and a few small palms in front of the hall, and behind everything a steep hill covered in dense dark-green forest under a clear blue sky. Unpainted weathered wood and grey stone, not a red Taiwanese temple. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

