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

### `icon_background` — App 圖示底（紙張）

- 用在：自適應圖示背景
- 最終尺寸：1024×1024　Gemini 比例：1:1　背景：一般圖，不用去背
- 注意：由 tools/make_procedural.py 產生。

_由程式產生，不需要 AI 圖。_

### `paper_page` — 整頁羊皮紙（摺痕、纖維、污漬、手寫筆記）

- 用在：全 App 背景（取代平鋪底紋）
- 最終尺寸：1080×2160　Gemini 比例：9:16　背景：一般圖，不用去背
- 注意：由 tools/make_paper_page.py（紙）＋svg/paper_notes.svg（手寫筆記與素描）合成；tools/compose_pages.py 輸出 generated/paper_page.png。

_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_

```
Create an image with aspect ratio 9:16. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. 
```

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

### `collection_slot_locked` — 收藏格：尚未收集

- 用在：收藏頁尚未收集景點的格子（取代目前的問號徽章）
- 最終尺寸：512×512　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：中央留空讓 App 疊上景點序號；不要文字與數字。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A blank collectible card slot drawn like an empty pressed-specimen mount in an old naturalist's album: a faint pencil outline of a small landmark silhouette inside a rounded rectangle of aged paper with soft hatched shading, a tiny brass corner mount at each corner, muted and quiet. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `collection_slot_frame` — 收藏格：已收集外框

- 用在：收藏頁已收集景點卡的外框（透明中央，疊在景點插圖上）
- 最終尺寸：512×512　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：中央必須完全空白（之後去背）；邊框寬度約整張的 6%；不要文字。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A thin ornamental picture frame for a postcard-like collectible card: aged cream paper border with fine sepia pen lines, small brass corner pieces, subtle watercolor stains at the edges, empty transparent centre. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## character

### `traveller_fox` — 旅人：小狐

- 用在：首頁／回顧視窗的角色
- 最終尺寸：512×768　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：面向右；腳貼底邊；全身。 已由 SVG 手繪完成（svg/traveller_fox.svg，用 tools/render_svg.mjs 渲染）；若之後有 AI 圖，直接覆蓋 generated/traveller_fox.png。

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small traveller walking, side view facing right, whole body from head to feet with the feet at the bottom edge, with fox ears, a wide-brimmed hat tucked behind them, a warm orange wash coat, and a small leather satchel on the back. Friendly, simple, storybook proportions. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `traveller_walker` — 旅人：背包行者（新角色）

- 用在：環島地圖上的目前位置標記、收卡視窗（取代舊的小狐）
- 最終尺寸：512×768　Gemini 比例：3:4　背景：純白（匯出去背）
- 注意：面向右；腳貼底邊；全身；不要奇幻元素（沒有動物耳朵、披風）。完成後把 generated/traveller_walker.png 匯出，再把 RouteVisuals.kt 的 rememberArt("traveller_fox") 改成 traveller_walker，並刪除 traveller_fox。

```
Create an image with aspect ratio 3:4. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A friendly Taiwanese traveller walking, side view facing right, whole body from head to feet with the feet at the bottom edge: a bucket sun hat, a light linen shirt with rolled sleeves, a medium backpack with a rolled sleeping mat, comfortable walking shoes and a small water bottle clipped to the strap. Gender-neutral, simple storybook proportions, a relaxed cheerful stride. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## icon

### `icon_foreground` — App 圖示前景

- 用在：自適應圖示（桌面圖示）
- 最終尺寸：1024×1024　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：重要內容限制在中央 60% 以內（安全區）；需要另外一張單色版（同構圖，單一深色）給主題圖示用。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cheerful hand-drawn simple Taiwan island silhouette in muted moss green with a dotted brass walking route looping around it and one small red map pin, simple bold shapes, centered and kept inside the middle 60% of the frame with generous empty margin around it. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## card

### `card_taipei101` — 景點卡：台北101

- 用在：路線頁與解鎖視窗的景點卡插圖（高五百零八公尺的高塔）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Taipei 101, a tall skyscraper whose tiers look like stacked bamboo segments, rising above a basin city of low rooftops with green mountains behind, soft morning mist. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_keelung` — 景點卡：基隆廟口

- 用在：路線頁與解鎖視窗的景點卡插圖（奠濟宮四周擠滿小吃攤）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A bustling night-market lane beside a small temple, red lanterns, steaming food stalls and paper lanterns in a harbour town. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_jiufen` — 景點卡：九份

- 用在：路線頁與解鎖視窗的景點卡插圖（淘金小山城因電影重生）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A hillside mountain town of stone stairways and glowing red lanterns above a calm sea at dusk, old tea houses with tiled roofs. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_fulong` — 景點卡：福隆

- 用在：路線頁與解鎖視窗的景點卡插圖（雙溪在這裡流入太平洋）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A long sandy beach where a small river meets the Pacific, a railway station and track in the background, a paper lunchbox on a bench. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_sandiaojiao` — 景點卡：三貂角

- 用在：路線頁與解鎖視窗的景點卡插圖（位於本島最東端的燈塔）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A white lighthouse on a green headland at the eastern tip of an island, waves breaking on dark rocks, wide Pacific horizon. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_toucheng` — 景點卡：頭城老街

- 用在：路線頁與解鎖視窗的景點卡插圖（開蘭第一街約六百公尺）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old street of narrow red-brick shophouses with arcaded fronts and hanging signs, a gate at the end of the street, quiet morning. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_ncta` — 景點卡：傳統藝術中心

- 用在：路線頁與解鎖視窗的景點卡插圖（傳藝中心坐落冬山河畔）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traditional arts village beside a river: old-style buildings with curved roofs, a small stage, craft stalls with lanterns, willows by the water. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_suao` — 景點卡：蘇澳冷泉

- 用在：路線頁與解鎖視窗的景點卡插圖（約二十一度的碳酸冷泉）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cold spring pool with small bubbles rising in clear water in a small port town, fishing boats and green hills in the background. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_qixingtan` — 景點卡：七星潭

- 用在：路線頁與解鎖視窗的景點卡插圖（新月形海灣鋪滿礫石灘）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A crescent-shaped bay with a pebble beach, waves rolling in, distant mountains and a wide Pacific sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_pinegarden` — 景點卡：松園別館

- 用在：路線頁與解鎖視窗的景點卡插圖（日治時期軍方的辦公處）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A two-storey Japanese colonial-style wooden building among tall pine trees, a quiet garden path, the sea visible beyond. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_lintienshan` — 景點卡：林田山

- 用在：路線頁與解鎖視窗的景點卡插圖（至今仍保存的林業聚落）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A former forestry village on a mountainside: wooden houses with tin roofs, an old log-hauling railway track, tall cedar trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_ruisui` — 景點卡：瑞穗北回歸線標

- 用在：路線頁與解鎖視窗的景點卡插圖（回歸線標一九三三年建）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A white monument marking the Tropic of Cancer on a terrace with a round plaza, rice fields and mountains behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_yuli` — 景點卡：玉里

- 用在：路線頁與解鎖視窗的景點卡插圖（當地招牌小吃是玉里麵）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A bowl of steaming noodles on a wooden table in a small-town eatery, a valley railway station visible through the window. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_chishang` — 景點卡：池上

- 用在：路線頁與解鎖視窗的景點卡插圖（池上便當起源於火車站）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traditional railway lunchbox of rice with side dishes beside a small train-station platform, golden rice paddies behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_guanshan` — 景點卡：關山

- 用在：路線頁與解鎖視窗的景點卡插圖（環鎮單車道長十二公里）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A flat riverside bicycle path looping around a small valley town, cyclists, rice fields and mountains on both sides. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_luye` — 景點卡：鹿野高台

- 用在：路線頁與解鎖視窗的景點卡插圖（夏季舉辦熱氣球嘉年華）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Hot-air balloons rising over a green highland plateau in summer, people on the grass, a wide valley below. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_prehistory` — 景點卡：史前博物館

- 用在：路線頁與解鎖視窗的景點卡插圖（專門展示臺灣史前文化）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A museum hall with ancient stone tools, pottery and carved stone slabs displayed in soft light, a standing stone column in front. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_taimali` — 景點卡：太麻里

- 用在：路線頁與解鎖視窗的景點卡插圖（號稱台灣最早的曙光處）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Sunrise over the Pacific seen from a coastal railway platform, golden light on the water, a slope of orange daylilies. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_shouka` — 景點卡：壽卡

- 用在：路線頁與解鎖視窗的景點卡插圖（南迴公路上最高的山口）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A winding mountain road reaching a high pass marked by a small signpost, clouds in the valley, forest on both sides. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_checheng` — 景點卡：車城福安宮

- 用在：路線頁與解鎖視窗的景點卡插圖（全台最大的土地公廟宇）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traditional Taiwanese Earth God temple with a curved swallowtail roof, red lanterns, incense smoke and a small stone lion at the entrance, tropical trees behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_hengchun` — 景點卡：恆春古城

- 用在：路線頁與解鎖視窗的景點卡插圖（四座城門全都完整保留）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old stone city gate with a tower and curving stone walls, a quiet street, tropical trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_eluanbi` — 景點卡：鵝鑾鼻

- 用在：路線頁與解鎖視窗的景點卡插圖（台灣本島最南端的燈塔）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A white lighthouse standing on a green cape at the southern tip of an island, ocean on both sides, tropical vegetation. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_maobitou` — 景點卡：貓鼻頭

- 用在：路線頁與解鎖視窗的景點卡插圖（狀似蹲貓的波浪狀礁岩）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A limestone reef coast with wave-shaped rocks and one rock that looks like a crouching cat, turquoise sea, blue sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_nmmba` — 景點卡：海生館

- 用在：路線頁與解鎖視窗的景點卡插圖（海生館二〇〇〇年成立）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A marine aquarium building by the sea, a large glass tank with a whale shark and coral fish, blue light. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_fangliao` — 景點卡：枋寮

- 用在：路線頁與解鎖視窗的景點卡插圖（環島鐵路由此全線貫通）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small railway station where a coastal line meets a mountain line, a train on the track, palm trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_donggang` — 景點卡：東港

- 用在：路線頁與解鎖視窗的景點卡插圖（東港迎王平安祭典主廟）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fishing harbour with fishing boats, a bluefin tuna on the dock, a colourful temple procession carrying a ceremonial boat. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_pier2` — 景點卡：高雄駁二

- 用在：路線頁與解鎖視窗的景點卡插圖（港邊舊倉庫變身藝術區）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Old harbour warehouses turned into art spaces, colourful sculptures, a rail track, cranes and the sea behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_lotuspond` — 景點卡：蓮池潭

- 用在：路線頁與解鎖視窗的景點卡插圖（龍虎塔一九七六年落成）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A lotus pond with two ornate pagodas shaped like a dragon and a tiger, red and green lacquer, a pavilion reflected in the water. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_qiaotou` — 景點卡：橋頭糖廠

- 用在：路線頁與解鎖視窗的景點卡插圖（一九〇一年興建的糖廠）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old sugar refinery with brick chimneys and a small narrow-gauge sugar train, sugar-cane fields. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_anping` — 景點卡：安平古堡

- 用在：路線頁與解鎖視窗的景點卡插圖（荷蘭人一六二四年起建）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A stone-and-brick fort with a red-brick watchtower on a hill, old banyan trees, harbour behind, 17th-century Dutch style. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_jingzijiao` — 景點卡：井仔腳鹽田

- 用在：路線頁與解鎖視窗的景點卡插圖（現存最古老的瓦盤鹽田）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Tile-paved salt fields: a grid of terracotta-tile pans reflecting the sky, a small salt worker's hut, a wide flat coastal plain. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_budai` — 景點卡：布袋

- 用在：路線頁與解鎖視窗的景點卡插圖（一八九五年日軍登陸處）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fishing port on a lagoon with fishing boats, old salt-pan remains and gulls, a quiet evening. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_peitian` — 景點卡：朴子配天宮

- 用在：路線頁與解鎖視窗的景點卡插圖（朴子供奉媽祖的配天宮）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A grand Mazu temple with ornate roof carvings, drifting incense smoke, lanterns and a courtyard in a small town. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_shuishang` — 景點卡：水上北回歸線標

- 用在：路線頁與解鎖視窗的景點卡插圖（一九〇八年起建的標誌）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A monument marking the Tropic of Cancer: a tall stone pillar with an arch, a line on the ground, plain fields around. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_hinoki` — 景點卡：檜意森活村

- 用在：路線頁與解鎖視窗的景點卡插圖（林務局宿舍改造的園區）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Rows of old Japanese-style cypress-wood dormitories with tiled roofs, trees and a garden path. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_xiluo` — 景點卡：西螺大橋

- 用在：路線頁與解鎖視窗的景點卡插圖（長近兩公里的鋼桁大橋）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A long steel truss bridge over a broad river with a few pedestrians and bicycles, farmland, evening light. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_lukang` — 景點卡：鹿港老街

- 用在：路線頁與解鎖視窗的景點卡插圖（寬約七十公分的摸乳巷）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A very narrow stone-paved alley between old brick houses, a half-moon well set into a wall, hanging lanterns. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_roundhouse` — 景點卡：彰化扇形車庫

- 用在：路線頁與解鎖視窗的景點卡插圖（一九二二年建成仍運作）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fan-shaped railway roundhouse with locomotives pointing out from a central turntable, steam, brick building. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_gaomei` — 景點卡：高美濕地

- 用在：路線頁與解鎖視窗的景點卡插圖（風車剪影襯著濕地夕陽）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Wind turbines in silhouette on tidal wetlands at sunset, reflections in shallow water, a wooden boardwalk. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_dajia` — 景點卡：大甲鎮瀾宮

- 用在：路線頁與解鎖視窗的景點卡插圖（九天遶境三百四十公里）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A grand Mazu temple at dawn with a long procession carrying a palanquin, banners and firecracker smoke. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_tongxiao` — 景點卡：通霄精鹽廠

- 用在：路線頁與解鎖視窗的景點卡插圖（台鹽通霄的觀光精鹽廠）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A salt-works visitor factory with white pyramids of salt and salt sculptures, the sea behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_hsinchu` — 景點卡：新竹城隍廟

- 用在：路線頁與解鎖視窗的景點卡插圖（一七四八年創建城隍廟）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old city-god temple with ornate red roofs and a stone city gate beside it, racks of rice noodles drying in the wind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_taoyuan` — 景點卡：桃園神社

- 用在：路線頁與解鎖視窗的景點卡插圖（一九三八年建成的神社）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A Japanese-style shrine with stone lanterns and a torii gate, large trees, stone steps. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_shihsanhang` — 景點卡：十三行博物館

- 用在：路線頁與解鎖視窗的景點卡插圖（展示十三行遺址的文物）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A modern museum building at a river mouth with ancient pottery and iron-smelting finds displayed, the sea behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_dihua` — 景點卡：迪化街

- 用在：路線頁與解鎖視窗的景點卡插圖（號稱台北最古老的街道）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old trading street with narrow baroque-style shop facades, hanging dried goods, red lanterns, cobbles. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_finish` — 景點卡：回到台北

- 用在：路線頁與解鎖視窗的景點卡插圖（繞完一圈回到熟悉的家）
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traveller coming home: a city skyline with a tall tower at sunrise, a railway station, a welcome banner, a backpack set down. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

## map

### `real_map_base` — 真實世界（台灣）地圖底圖

- 用在：首頁真實世界地圖
- 最終尺寸：1116×2000　Gemini 比例：9:16　背景：一般圖，不用去背
- 注意：海岸線要照 mapguide/real_map_guide.png；沒有文字、沒有邊框。

```
Create an image with aspect ratio 9:16. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. 見 REAL_MAP_PROMPT.md
```

## store

### `store_feature` — 商店主視覺（1024x500）

- 用在：Google Play 商店頁頂端橫幅
- 最終尺寸：1024×500　Gemini 比例：16:9　背景：一般圖，不用去背
- 注意：左右留白約 10%（商店會裁切）；不要文字，App 名稱「來台灣走走」由後製加上。

```
Create an image with aspect ratio 16:9. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm and slightly whimsical fantasy mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A wide panoramic illustration of the Taiwan coastline drawn as an old travel map vignette: green mountains in the middle, a dotted walking route circling the island, small sketches of a lighthouse, a temple roof, a night-market lantern string and a steam-train station along the route, a small traveller with a backpack in the foreground. Soft irregular edges fading into aged parchment, 1024x500 landscape, calm and warm mood.
```
