# 還缺的圖片提示詞（共 14 張）

## 怎麼用

1. 每張開新對話（Gemini 等圖像 AI）。景點卡先上傳風格參考圖 `previews/style_reference.jpg`；**App 圖示與商店主視覺要改上傳角色設定圖** `generated/traveller_sheet.jpg`，因為畫面裡有你的旅行者。
2. 比例、背景都已寫在提示詞裡。一律**不要有文字**；圖裡出現字就重生成。
3. 需要透明的（收藏格外框、App 圖示）提示詞已要求純白底，之後由工具去背；景點卡是一般圖，不用去背。
4. 存成 `generated/<id>.png`（檔名用標題裡的 id），傳回來給我，我匯出並接進 App。
5. 建議先做一張景點卡（例如 `card_lotuspond`）確認風格，滿意再做其餘。旅行者走路圖的提示詞另見 `WALK_CYCLE_PROMPT.md`（已完成）。

## App 圖示要兩張

`icon_foreground` 要出**彩色版**和**單色版**（同構圖，全部改成單一深色，給手機「主題圖示」用）；單色版請在提示詞最後加一句：`Single flat dark brown colour only, no gradients, no other colours.`

---

### `icon_foreground` — App 圖示前景

- 用在：自適應圖示（桌面圖示）
- 尺寸：1024×1024　需要透明（純白底，之後去背）

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Use the uploaded character sheet as the exact reference for the character (the small round glossy white spirit with big black eyes, wide-brim cream hat, patched green cloak, backpack, crooked staff with a glowing lantern). Draw this character cheerfully standing in front view on a small simple moss-green Taiwan island silhouette, a dotted brass walking route curling around the island, simple bold shapes, everything centred and kept inside the middle 60% of the frame with generous empty margin around it. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```
---

### `collection_slot_locked` — 收藏格：尚未收集

- 用在：收藏頁尚未收集景點的格子（取代目前的問號徽章）
- 尺寸：512×512　需要透明（純白底，之後去背）

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A blank collectible card slot drawn like an empty pressed-specimen mount in an old naturalist's album: a faint pencil outline of a small landmark silhouette inside a rounded rectangle of aged paper with soft hatched shading, a tiny brass corner mount at each corner, muted and quiet. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```
---

### `collection_slot_frame` — 收藏格：已收集外框

- 用在：收藏頁已收集景點卡的外框（透明中央，疊在景點插圖上）
- 尺寸：512×512　需要透明（純白底，之後去背）

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A thin ornamental picture frame for a postcard-like collectible card: aged cream paper border with fine sepia pen lines, small brass corner pieces, subtle watercolor stains at the edges, empty transparent centre. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```
---

### `store_feature` — 商店主視覺（1024x500）

- 用在：Google Play 商店頁頂端橫幅
- 尺寸：1024×500　一般圖，不用去背

```
Create an image with aspect ratio 16:9. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A wide panoramic illustration of the Taiwan coastline drawn as an old travel map vignette: green mountains in the middle, a dotted walking route circling the island, small sketches of a lighthouse, a temple roof, a night-market lantern string and a steam-train station along the route, the small round white spirit traveller from the uploaded character sheet (hat, patched green cloak, backpack, staff with a glowing lantern) walking along the route in the foreground, matching the reference exactly. Soft irregular edges fading into aged parchment, 1024x500 landscape, calm and warm mood.
```
---

### `card_budai` — 景點卡：布袋

- 用在：景點卡「布袋」：沙洲圍出的布袋狀潟湖
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A low flat sandbar beside a quiet lagoon, tall casuarina trees forming a windbreak forest behind it, clam ponds and flocks of water birds, rows of oyster racks standing in the shallow water, evening light. A flat sandy shore, not rocky cliffs, no palm trees, no salt pyramids, no church. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_hinoki` — 景點卡：檜意森活村

- 用在：景點卡「檜意森活村」：阿里山林業的檜町宿舍
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Rows of low single-storey Japanese-style wooden bungalow dormitories built of cypress on raised foundations with small under-floor ventilation openings, red cement-tile roofs, a quiet garden street between them with big shady trees, warm cypress-wood colours. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_lukang` — 景點卡：鹿港老街

- 用在：景點卡「鹿港老街」：摸乳巷窄處約六十公分
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old town lane of red-brick Minnan-style long narrow shophouses along a winding red-brick paved street, and between two houses a very narrow gap only one person wide (about 60 to 70 centimetres), where a person has to turn sideways, with a few hanging lanterns. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_gaomei` — 景點卡：高美濕地

- 用在：景點卡「高美濕地」：萬歲大眼蟹舉螯的濕地
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A coastal tidal wetland: eighteen very tall slender white wind turbines standing in a long line along the coast, a disused octagonal lighthouse with red and white horizontal bands, a long curving wooden boardwalk running out across the tidal flat, patches of marsh grass, and small fiddler crabs each raising one big claw in the foreground. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_dajia` — 景點卡：大甲鎮瀾宮

- 用在：景點卡「大甲鎮瀾宮」：九天遶境三百四十公里
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A grand traditional Minnan temple with a two-hall layout and a three-door front porch, a double-eaved hip-and-gable roof covered with colourful cut-and-paste ceramic decorations, the three star gods and a dragon-riding general on the front roof ridge, a wide stone courtyard in front, incense smoke and dawn light. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_tongxiao` — 景點卡：通霄精鹽廠

- 用在：景點卡「通霄精鹽廠」：巨大精鹽包造景的鹽廠
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A modern salt-works visitor park by the sea (not a salt pan, not a salt mountain): at the entrance an oversized salt-bag installation taller than a person, white salt-themed sculptures, a sundial-like pattern on the paved ground, clean low buildings and the sea behind. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_hsinchu` — 景點卡：新竹城隍廟

- 用在：景點卡「新竹城隍廟」：城隍廟對面掛著大算盤
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traditional Minnan three-hall city-god temple with undulating gable walls and cut-and-paste dragons and swallowtail roof ridges, a pair of stone lions at the central door, a lively open-air food-stall square in front of the temple, and a big wooden abacus hanging in the scene. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_taoyuan` — 景點卡：桃園神社

- 用在：景點卡「桃園神社」：石燈籠夾道的神社參道
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A Japanese Shinto shrine complex built of wood (not a Taiwanese temple): a torii gate, a stone-paved approach with rows of stone lanterns, a purification water pavilion, a worship hall and main hall with copper-plated roofs, a pair of stone guardian lions that look like Taiwanese stone lions, big old trees. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_shihsanhang` — 景點卡：十三行博物館

- 用在：景點卡「十三行博物館」：羅盤偏轉發現煉鐵遺址
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A modern museum in grey exposed concrete: a three-storey block under a long low-sloping steel whale-back roof, and beside it an octagonal tower leaning about 17 degrees like a tilted chimney; seen from the north plaza the whole building looks like a big ship; keep it in grey tones, not white or colourful. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_dihua` — 景點卡：迪化街

- 用在：景點卡「迪化街」：中藥南北貨的年貨大街
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old trading street: a row of two-storey red-brick Baroque-style shophouses with a covered arcade walkway on the ground floor, low Minnan-style brick houses and a few western-style facades standing side by side, dried goods and bags of produce displayed at the shop fronts, a busy crowd of shoppers seen from a distance. If an attached reference photo disagrees with this description, follow the photo. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
