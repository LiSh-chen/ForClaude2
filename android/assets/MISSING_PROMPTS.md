# 還缺的圖片提示詞（共 16 張）

## 怎麼用

1. 每張開新對話（Gemini 等圖像 AI），**先上傳風格參考圖** `previews/style_reference.jpg`（現有景點卡的畫風）。**不附實景照片**，景點的外觀細節已寫在提示詞文字裡。App 圖示與商店主視覺例外：請改上傳角色設定圖 `generated/traveller_sheet.jpg`。
2. 比例、背景都已寫在提示詞裡。一律不要有文字；圖裡出現字就重生成。
3. 需要透明的（收藏格外框、App 圖示）提示詞已要求純白底，之後由工具去背；景點卡是一般圖，不用去背。
4. 存成 `generated/<id>.png`（檔名用標題裡的 id），傳回來給我，我匯出並接進 App。
5. 景點卡提示詞只寫「查證確認或單一來源」的外觀細節；查不到的顏色、尺寸沒有指定，圖像工具會自己決定，所以**這幾張可能仍與實景有差**：通霄精鹽廠、布袋、新竹城隍廟、大甲鎮瀾宮、桃園神社。

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
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Haomeiliao sandbar near Budai, Chiayi, Taiwan, drawn only from these facts: a low flat sandbar of soft pale sand beside a quiet lagoon, a windbreak forest of casuarina trees behind it with a few coconut palms and beach morning-glory on the sand, clam ponds, rows of oyster racks standing in the shallow water around the harbour, calm light. A flat sandy shore, not rocky cliffs, no salt pyramids, no church. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_hinoki` — 景點卡：檜意森活村

- 用在：景點卡「檜意森活村」：阿里山林業的檜町宿舍
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Hinoki Village in Chiayi, Taiwan, drawn only from these facts: rows of single-storey Japanese-style wooden houses raised about 75 centimetres off the ground on posts with ventilation gaps, walls of overlapping horizontal wooden boards, Japanese-style tiled roofs, quiet paths between the houses under big shady trees, warm cypress-wood tones. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_lukang` — 景點卡：鹿港老街

- 用在：景點卡「鹿港老街」：摸乳巷窄處約六十公分
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The old street of Lukang, Taiwan, drawn only from these facts: red-brick Minnan-style shophouses and red-brick paved lanes, wooden doors and wooden lattice windows, a winding lane with orange-red brick walls, and between two buildings a very narrow gap only one person wide (about 60 to 70 centimetres) where a person must turn sideways. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_gaomei` — 景點卡：高美濕地

- 用在：景點卡「高美濕地」：萬歲大眼蟹舉螯的濕地
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Gaomei wetland in Taichung, Taiwan, drawn only from these facts: a dozen or so pure white wind turbines standing in a row along the seawall road, a disused octagonal lighthouse with red and white parallel stripes, a long curving wooden boardwalk out over the tidal flat, patches of green marsh grass turning golden, shallow water that mirrors the sky at low tide, the sea turning gold and orange-red at sunset, and small fiddler crabs each raising one big claw in the foreground. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_dajia` — 景點卡：大甲鎮瀾宮

- 用在：景點卡「大甲鎮瀾宮」：九天遶境三百四十公里
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Zhenlan Temple in Dajia, Taichung, Taiwan, drawn only from these facts: a traditional Minnan temple with a front hall and a main hall around a courtyard, a big open forecourt, a roof ridge covered with colourful cut-and-paste ceramic figures with gold leaf (the three star gods and a dragon-riding general on the front ridge), and a large plaque above the entrance with no readable writing. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_tongxiao` — 景點卡：通霄精鹽廠

- 用在：景點卡「通霄精鹽廠」：巨大精鹽包造景的鹽廠
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Tongxiao salt-works visitor park in Miaoli, Taiwan, drawn only from these facts: a giant salt-sack landmark taller than a person near the entrance, a sundial-shaped pattern on the paved ground, a fresh blue-and-white colour theme, clean modern low buildings, the sea in the background. It is a visitor park, not a salt pan and not a salt mountain. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_hsinchu` — 景點卡：新竹城隍廟

- 用在：景點卡「新竹城隍廟」：城隍廟對面掛著大算盤
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The City God Temple of Hsinchu, Taiwan, drawn only from these facts: a traditional Minnan temple whose gable walls rise and fall in continuous waves, roof ridges with swallowtail ends and ceramic dragons, three bays with a pair of blue-stone lions at the central door, and in front of the temple a lively open plaza of food stalls. Do not draw an abacus. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_taoyuan` — 景點卡：桃園神社

- 用在：景點卡「桃園神社」：石燈籠夾道的神社參道
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Taoyuan Shinto shrine in Taiwan, drawn only from these facts: Japanese shrine buildings of unpainted cypress and cedar wood (not a Taiwanese temple), roofs of copper tiles weathered to green, a long stone-paved approach with stone lanterns on both sides, a purification water pavilion, and one single surviving torii gate whose top crossbeams have been removed. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_shihsanhang` — 景點卡：十三行博物館

- 用在：景點卡「十三行博物館」：羅盤偏轉發現煉鐵遺址
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Shihsanhang Museum of Archaeology in Bali, New Taipei, Taiwan, drawn only from these facts: raw grey concrete, sandstone and weathered metal panels, a three-storey block under a long steel roof shaped like a whale's back, an octagonal tower that visibly leans (about 17 degrees) like a tilted chimney, long grey stairs outside, and seen from the north plaza the whole building looks like a big ship. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_dihua` — 景點卡：迪化街

- 用在：景點卡「迪化街」：中藥南北貨的年貨大街
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Dihua Street in Taipei, Taiwan, drawn only from these facts: narrow deep two-storey shophouses in a continuous row with a covered arcade walkway, a mix of red-brick facades and Baroque-style facades in red brick and grey concrete with moulded Baroque gable decoration, a regular pattern of four columns and three windows on the front, dried goods in open sacks displayed under the arcade, a crowd of shoppers seen from a distance. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_checheng` — 景點卡：車城福安宮

- 用在：景點卡「車城福安宮」：福德正神穿龍袍戴官帽
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Fu-An Temple, an Earth God temple in Checheng, Pingtung, Taiwan, drawn only from these facts: a grand palace-style temple of three halls with an unusually tall main hall about five to six storeys high; in the forecourt a large octagonal paper-burning furnace with three-tiered eaves and four storeys; inside the main doorway the Earth God statue has a black face, a prime minister's hat and a dragon robe, with many small gold medals hanging all over its body. Red lanterns and drifting incense smoke. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
---

### `card_shuishang` — 景點卡：水上北回歸線標

- 用在：景點卡「水上北回歸線標」：夏至正午立竿沒有影子
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Tropic of Cancer Marker park in Shuishang, Chiayi, Taiwan, drawn only from these facts: a tall monument tower with a triangular (three-sided) cross-section and a three-ring instrument on its top; beside it a five-storey observatory-style building with a rounded dome; in the plaza planet sculptures, a ringed Saturn model and a rocket on display; a main road beside the park. It is noon and the sun is overhead, so the tower casts almost no shadow. The tower is NOT a white round pillar or a square obelisk (that is the Hualien marker). Leave the exact colours natural and muted, not pure white. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```
