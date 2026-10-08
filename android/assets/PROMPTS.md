# 素材提示詞（給 Gemini 等圖像 AI 使用）

## 怎麼用

1. **先做 3 張試風格**：`compass_case`、`compass_face`、`traveller_fox`。風格滿意後，再把這張當「風格參考圖」一起上傳，生成其他素材，才會一致。
2. **每張單獨生成**：把該素材的整段提示詞貼進去。提示詞開頭已經寫了比例；Gemini 支援的比例是 1:1、3:4、4:3、9:16、16:9，**匯出時會自動裁成規格尺寸**。
3. **一律要純白底**（提示詞已寫）。Gemini 不會輸出透明背景，匯出工具會把與邊緣相連的白底去掉。
4. **不要有文字**：地名、數字都由 App 畫。圖裡如果出現字，請重生成。
5. **存檔**：存成 `android/assets/generated/<id>.png`（檔名就是下面標題裡的 id）。完成後執行 `python3 tools/build_assets.py check` 檢查、`export` 匯出；或把圖直接傳給 Claude 處理。
6. 不滿意就用同一個提示詞再生成，或在對話裡說明要改什麼（例如「線條再細一點」「顏色更淡」）。

## 共同風格（已經包含在每個提示詞裡，不用另外貼）

Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures.

## 負面提示詞

Gemini 沒有獨立的負面提示詞欄位；提示詞裡已用「no text、no watermark…」表達。其他工具（Midjourney `--no`、Stable Diffusion）可用：

text, letters, numbers, watermark, signature, logo, photo-realistic, 3d render, glossy plastic, neon colors, harsh black outlines, cartoon outline, blurry, low resolution, cropped, frame, border (unless asked), multiple variations in one image (unless asked)

## texture

### `parchment_tile` — 羊皮紙底紋（可無縫平鋪）

- 用在：全 App 背景
- 最終尺寸：768×768　Gemini 比例：1:1　背景：一般圖，不用去背
- 注意：由 tools/make_procedural.py 產生；若想換成 AI 圖，必須是可無縫平鋪的紙張紋理。

_由程式產生，不需要 AI 圖。_

### `fog_puff` — 迷霧團

- 用在：世界地圖上蓋住未探索處
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：由 tools/make_procedural.py 產生。中心不透明、邊緣漸透明的柔和霧團。

_由程式產生，不需要 AI 圖。_

### `icon_background` — App 圖示底（紙張）

- 用在：自適應圖示背景
- 最終尺寸：1024×1024　Gemini 比例：1:1　背景：一般圖，不用去背
- 注意：由 tools/make_procedural.py 產生。

_由程式產生，不需要 AI 圖。_

## ui

### `compass_case` — 黃銅羅盤外殼（中空）

- 用在：首頁步數羅盤的外框
- 最終尺寸：1024×1120　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：表盤開口直徑約為寬度的 56%，圓心在 (50%, 57%)；吊環在最上方。 中央開口需要透明，所以去背時連內部的純白也一起去掉（keyInterior）。 已由 SVG 手繪完成（svg/compass_case.svg，用 tools/render_svg.mjs 渲染）；若之後有 AI 圖，直接覆蓋 generated/compass_case.png。

_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Top-down view of an antique brass pocket compass case with a small hanging ring at the top, a thick riveted brass rim with worn patina, and green vines with small leaves winding around the outer edge on both sides. The big circular opening in the middle must be EMPTY and plain white so a dial can be placed behind it. Perfectly symmetrical, centered. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `compass_face` — 羅盤表盤

- 用在：放在外殼後面，進度弧線畫在上面
- 最終尺寸：1024×1024　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：圓形填滿整張圖；中心保持淡，因為會疊上步數。 已由 SVG 手繪完成（svg/compass_face.svg，用 tools/render_svg.mjs 渲染）；若之後有 AI 圖，直接覆蓋 generated/compass_face.png。

_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A flat circular antique compass dial face seen from above, aged parchment yellow with a faint engraved compass rose, a fine tick-mark ring near the edge and only the four letters N, E, S, W. The circle fills the whole frame edge to edge. Leave the centre fairly empty and light so a number can be written over it. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## scene

### `fern_a` — 蕨類叢 A

- 用在：首頁場景左側前景
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：純白（匯出去背）
- 注意：底部置中對齊地面。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cluster of three graceful fern fronds curving to the right, fine ink veins, pale green wash, a few small grass blades at the base. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `fern_b` — 蕨類叢 B

- 用在：首頁場景前景變化
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：純白（匯出去背）
- 注意：底部置中對齊地面。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cluster of two large fern fronds and one small unfurling fiddlehead, curving to the left, fine ink veins, pale green wash. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `lily_a` — 百合花叢 A

- 用在：首頁場景右側前景
- 最終尺寸：768×1024　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：底部置中對齊地面。

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tall lily-like flower with a dusty rose wash on a slender curved stem, two long basal leaves, stamens with tiny brass dots, plus a small bud on a second stem. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `lily_b` — 百合花叢 B

- 用在：首頁場景前景變化
- 最終尺寸：768×1024　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：底部置中對齊地面。

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Two bell-shaped wildflowers in pale green-white with a touch of rose, on thin curved stems with narrow leaves, a few round seed pods. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `reed_a` — 蘆葦與貝殼叢

- 用在：月光海岸場景前景
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：純白（匯出去背）
- 注意：底部置中對齊地面。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A clump of reeds with brown cattail heads, a few narrow leaves, and two small spiral shells lying at the base, pale blue-green wash. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `shells` — 貝殼組

- 用在：海岸場景地面點綴（4 個排成一列）
- 最終尺寸：1024×256　Gemini 比例：16:9　背景：純白（匯出去背）
- 注意：四個等寬格子。

```
Create an image with aspect ratio 16:9. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Four different small seashells in a horizontal row with space between them: a spiral snail shell, a scallop, a conch, a tiny cone shell. Rose and cream wash. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `far_trees` — 遠景樹林帶（可橫向平鋪）

- 用在：首頁場景遠景
- 最終尺寸：2048×512　Gemini 比例：16:9　背景：純白（匯出去背）
- 注意：左右邊緣需可無縫接合；整體低對比。 需要左右可無縫平鋪，Gemini 沒有 4:1 比例也很難接縫，建議不要用 AI 做，由 Claude 以 SVG／程式繪製。

_建議跳過：由 Claude 以 SVG／程式繪製（可無縫平鋪）。_

### `waves` — 海浪帶（可橫向平鋪）

- 用在：海岸場景遠景
- 最終尺寸：2048×256　Gemini 比例：16:9　背景：純白（匯出去背）
- 注意：左右邊緣需可無縫接合。 需要左右可無縫平鋪，Gemini 沒有 4:1 比例也很難接縫，建議不要用 AI 做，由 Claude 以 SVG／程式繪製。

_建議跳過：由 Claude 以 SVG／程式繪製（可無縫平鋪）。_

### `ground_path` — 地面小徑（可橫向平鋪）

- 用在：場景地面
- 最終尺寸：2048×128　Gemini 比例：16:9　背景：純白（匯出去背）
- 注意：左右邊緣需可無縫接合。 需要左右可無縫平鋪，Gemini 沒有 4:1 比例也很難接縫，建議不要用 AI 做，由 Claude 以 SVG／程式繪製。

_建議跳過：由 Claude 以 SVG／程式繪製（可無縫平鋪）。_

## character

### `traveller_fox` — 旅人：小狐

- 用在：首頁／回顧視窗的角色
- 最終尺寸：512×768　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：面向右；腳貼底邊；全身。 已由 SVG 手繪完成（svg/traveller_fox.svg，用 tools/render_svg.mjs 渲染）；若之後有 AI 圖，直接覆蓋 generated/traveller_fox.png。

_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small traveller walking, side view facing right, whole body from head to feet with the feet at the bottom edge, with fox ears, a wide-brimmed hat tucked behind them, a warm orange wash coat, and a small leather satchel on the back. Friendly, simple, storybook proportions. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `traveller_elf` — 旅人：森林精靈

- 用在：首頁／回顧視窗的角色
- 最終尺寸：512×768　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：面向右；腳貼底邊；全身。 已由 SVG 手繪完成（svg/traveller_elf.svg，用 tools/render_svg.mjs 渲染）；若之後有 AI 圖，直接覆蓋 generated/traveller_elf.png。

_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small traveller walking, side view facing right, whole body from head to feet with the feet at the bottom edge, a forest sprite with pointed leaf-shaped ears and a green leaf cap, a sage green wash cloak, and a small satchel. Friendly, simple, storybook proportions. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `traveller_cat` — 旅人：貓耳

- 用在：首頁／回顧視窗的角色
- 最終尺寸：512×768　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：面向右；腳貼底邊；全身。 已由 SVG 手繪完成（svg/traveller_cat.svg，用 tools/render_svg.mjs 渲染）；若之後有 AI 圖，直接覆蓋 generated/traveller_cat.png。

_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small traveller walking, side view facing right, whole body from head to feet with the feet at the bottom edge, with soft grey cat ears and a long tail, a dusty rose scarf, a pale grey-lavender wash jacket, and a small satchel. Friendly, simple, storybook proportions. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `traveller_cape` — 旅人：披風旅人

- 用在：首頁／回顧視窗的角色
- 最終尺寸：512×768　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：面向右；腳貼底邊；全身。 已由 SVG 手繪完成（svg/traveller_cape.svg，用 tools/render_svg.mjs 渲染）；若之後有 AI 圖，直接覆蓋 generated/traveller_cape.png。

_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small traveller walking, side view facing right, whole body from head to feet with the feet at the bottom edge, wearing a wide-brimmed explorer's hat, a flowing muted red cape, a pale blue wash tunic, and a rolled map on the backpack. Friendly, simple, storybook proportions. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## map

### `world_map_base` — 世界地圖底圖（無地名、無地標）

- 用在：旅程頁世界地圖
- 最終尺寸：1024×1312　Gemini 比例：3:4　背景：一般圖，不用去背
- 注意：陸地輪廓請大致落在整張圖的 4%–97% 範圍內，以便地標座標對得上；地標與路線由 App 疊上。

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fantasy island map on aged cream parchment drawn in ink and watercolor, portrait orientation: one large irregular landmass with a winding coastline, pale green-beige wash on the land, the sea around it shown with fine horizontal hatching and a few small wave marks, a small ornamental compass rose in the lower-left corner, and a thin ornate hand-drawn border. The land itself is left mostly plain, with only a few faint hills and tiny grass marks. NO place names, NO labels, NO icons for forests, towns or mountains, NO route lines — those are added separately.
```

### `landmark_forest` — 地標：低語森林

- 用在：世界地圖上的地點圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tiny sketch of three round-crowned trees close together with mushrooms at their feet, green wash. Centered, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `landmark_coast` — 地標：月光海岸

- 用在：世界地圖上的地點圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tiny sketch of a curved sandy bay with three wave lines and a small spiral shell, blue wash and a faint crescent moon. Centered, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `landmark_coral` — 地標：珊瑚群島

- 用在：世界地圖上的地點圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tiny sketch of a small island with branching pink coral and two palm-like plants, turquoise wash. Centered, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `landmark_swamp` — 地標：霧中沼澤

- 用在：世界地圖上的地點圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tiny sketch of reeds with cattails over a pool of still water with ripples, with a firefly or two, grey-green wash. Centered, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `landmark_dunes` — 地標：星砂沙丘

- 用在：世界地圖上的地點圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tiny sketch of three rolling sand dunes with a small star above, warm sand wash. Centered, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `landmark_ruins` — 地標：浮空遺跡

- 用在：世界地圖上的地點圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tiny sketch of a floating chunk of rock with a broken stone arch standing on it and a small cloud beside it, cream and grey wash. Centered, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `landmark_tree` — 地標：世界之樹

- 用在：世界地圖上的地點圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A tiny sketch of one enormous ancient tree with a wide crown and spiralling roots, faint gold light in the leaves, green wash. Centered, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## item

### `item_life_seed` — 素材：生命之種

- 用在：背包與回顧視窗的圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A single glowing seed with a tiny green sprout and two leaves, resting in a small curled leaf, soft golden glow. Centered icon, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `item_moon_dew` — 素材：月光露

- 用在：背包與回顧視窗的圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A single round dewdrop on a leaf tip, silvery-blue with a tiny crescent moon reflected in it, soft glow. Centered icon, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `item_tide_pearl` — 素材：潮汐珠

- 用在：背包與回顧視窗的圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A round pearl with a faint swirling wave pattern inside, resting in an open scallop shell, soft blue-white glow. Centered icon, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `item_rainbow_coral` — 素材：彩虹珊瑚芽

- 用在：背包與回顧視窗的圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small branching coral sprout with tips fading from pink to orange to teal. Centered icon, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `item_wind_feather` — 素材：風之羽

- 用在：背包與回顧視窗的圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A single long pale-blue feather with swirling wind lines around it. Centered icon, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `item_rune_shard` — 素材：符文碎片

- 用在：背包與回顧視窗的圖示
- 最終尺寸：256×256　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：置中；小尺寸也要看得清。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small broken fragment of a stone tablet carved with a glowing rune, warm amber light in the grooves. Centered icon, simple, readable at a small size. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## icon

### `icon_foreground` — App 圖示前景

- 用在：自適應圖示（桌面圖示）
- 最終尺寸：1024×1024　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：重要內容限制在中央 60% 以內（安全區）。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An antique brass pocket compass seen from above with a small green sprout growing from the top ring, simple bold shapes, centered and kept inside the middle 60% of the frame with generous empty margin around it. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```
