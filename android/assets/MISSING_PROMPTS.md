# 還缺的圖片提示詞（共 3 張）

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

### `card_shihsanhang` — 景點卡：十三行博物館

- 用在：景點卡「十三行博物館」：羅盤偏轉發現煉鐵遺址
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Shihsanhang Museum of Archaeology in Bali, New Taipei, Taiwan, drawn exactly like this real aerial view from above and slightly to the side: on the left a long low grey wing whose steel roof curves down at the end like the back of a whale, with a flat-faced grey curved wall along its edge and a roof of small grey metal-clad panels; in the centre-right a tall octagonal tower clad in dark brown brick-like panels with a pale grey flat cap, leaning visibly like a tilted chimney; to its right a long angular wing of raw grey concrete with a dark glass panel; between them a small triangular lawn with grey steps leading down to the entrance; a car park with painted stripes in front; behind the building a wide park of low green lawns, winding paths, scattered trees and a few small dark-red-roofed pavilions, and at the far right a round pale-yellow cylindrical tank; beyond the park a long raised grey coastal embankment road, a strip of dark sand and green marsh grass, and a flat grey-green sea with small white waves under a pale hazy sky; in the foreground a road crossing with striped crosswalks and a tall tan-coloured building in the lower right corner. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_dihua` — 景點卡：迪化街

- 用在：景點卡「迪化街」：中藥南北貨的年貨大街
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Dihua Street in Taipei, Taiwan, drawn exactly like this real night view of the New Year market, looking straight down the narrow street: overhead, rows and rows of round paper lanterns in red and warm yellow hung on lines across the street, receding into the distance, with long red horizontal banners hung across (leave the banner characters as abstract white brush marks, not readable); on both sides low two-storey shops and open market stalls with corrugated metal awnings, strings of bare warm light bulbs hanging under the awnings, glowing shop fronts stacked with dried goods and red decorations, a bright yellow wall at the far right, blue and red neon-like shop signs high on the walls (abstract marks only); the whole street packed with a dense crowd of shoppers in winter jackets and backpacks seen from behind and from a distance; deep dark-blue night sky above, everything lit warm orange-gold by the lanterns. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
