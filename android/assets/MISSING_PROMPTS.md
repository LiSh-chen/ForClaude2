# 還缺的圖片提示詞（共 33 張）

## 怎麼用

1. 每張開新對話（Gemini 等圖像 AI），**先上傳風格參考圖** `android/assets/previews/style_reference.jpg`（現有景點卡的風格），再貼下面那張的提示詞。
2. 比例、背景都已寫在提示詞裡。一律**不要有文字**；圖裡出現字就重生成。
3. 需要透明的（旅行者、收藏格外框、App 圖示）提示詞已要求純白底，之後由工具去背；景點卡是一般圖，不用去背。
4. 存成 `android/assets/generated/<id>.png`（檔名用下面標題裡的 id），傳回來給我，我匯出並接進 App。
5. 旅行者角色已用你的造型，不在這份裡；它的走路動畫圖另見 `WALK_CYCLE_PROMPT.md`。建議先做一張景點卡（例如 `card_lotuspond`）確認風格，滿意再做其餘。

## 另外：App 圖示要兩張

`icon_foreground` 要出**彩色版**和**單色版**（同構圖，單色版全部改成單一深色，給手機「主題圖示」用）；單色版請在提示詞最後加一句：`Single flat dark brown colour only, no gradients, no other colours.`

---

### `icon_foreground` — App 圖示前景

- 用在：自適應圖示（桌面圖示）
- 尺寸：1024×1024　需要透明（純白底，之後去背）

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cheerful hand-drawn simple Taiwan island silhouette in muted moss green with a dotted brass walking route looping around it and one small red map pin, simple bold shapes, centered and kept inside the middle 60% of the frame with generous empty margin around it. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
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
Create an image with aspect ratio 16:9. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A wide panoramic illustration of the Taiwan coastline drawn as an old travel map vignette: green mountains in the middle, a dotted walking route circling the island, small sketches of a lighthouse, a temple roof, a night-market lantern string and a steam-train station along the route, a small traveller with a backpack in the foreground. Soft irregular edges fading into aged parchment, 1024x500 landscape, calm and warm mood.
```

---

### `card_guanshan` — 景點卡：關山

- 用在：景點卡「關山」：水牛稻田間騎環鎮單車
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A riverside bicycle path looping around a small valley town, a few cyclists, water buffalo standing in green rice paddies, mountains on both sides. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_taimali` — 景點卡：太麻里

- 用在：景點卡「太麻里」：二〇〇〇年迎曙光之地
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A coastal railway platform at dawn, a golden sun disc just rising out of the Pacific, orange light on the water, a slope of orange daylilies. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_shouka` — 景點卡：壽卡

- 用在：景點卡「壽卡」：屏東臺東交界山口驛站
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cyclists' rest stop at a high mountain pass on a winding coastal-mountain road, a Paiwan-style carved stone statue of a warrior at the entrance, clouds in the valley, forest on both sides. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_checheng` — 景點卡：車城福安宮

- 用在：景點卡「車城福安宮」：福德正神穿龍袍戴官帽
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traditional Taiwanese Earth God temple with a curved swallowtail roof, a very tall four-storey golden-paper burner beside it, red lanterns, incense smoke, and inside the doorway an Earth God statue dressed in a dragon robe and an official's hat, tropical trees behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_hengchun` — 景點卡：恆春古城

- 用在：景點卡「恆春古城」：恆春古城四座城門仍在
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old stone city gate with a tower in a thick stone wall, a quiet street passing through the gate arch, a small bus waiting to pass, tropical trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_maobitou` — 景點卡：貓鼻頭

- 用在：景點卡「貓鼻頭」：狀似蹲貓的波浪狀礁岩
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A limestone reef coast with wave-shaped rocks and one rock that looks like a crouching cat, turquoise sea, blue sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_nmmba` — 景點卡：海生館

- 用在：景點卡「海生館」：海底隧道中看魚群環繞
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A curved glass tunnel under a huge aquarium tank, a whale shark, rays and schools of fish circling overhead, deep blue light, small visitors looking up. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_fangliao` — 景點卡：枋寮

- 用在：景點卡「枋寮」：月台邊近看火車轉車盤
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small railway station platform with a round railway turntable in the foreground and a locomotive being turned on it, a coast line and mountain line meeting behind, palm trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_donggang` — 景點卡：東港

- 用在：景點卡「東港」：東港迎王平安祭典主廟
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fishing harbour with fishing boats, a bluefin tuna on the dock, a colourful temple procession carrying a ceremonial boat. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_pier2` — 景點卡：高雄駁二

- 用在：景點卡「高雄駁二」：名字來自二號接駁碼頭
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Old harbour warehouses turned into art spaces, colourful sculptures, a rail track, cranes and the sea behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_lotuspond` — 景點卡：蓮池潭

- 用在：景點卡「蓮池潭」：龍虎塔要龍口進虎口出
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A lotus pond with a pair of ornate pagodas, a dragon-head entrance on one and a tiger-head exit on the other, red and green lacquer, a pavilion reflected in the water. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_qiaotou` — 景點卡：橋頭糖廠

- 用在：景點卡「橋頭糖廠」：糖廠鐵道還能搭五分車
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old sugar refinery with brick chimneys, a small narrow-gauge sugar train running through a long green tunnel of trees, sugar-cane fields. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_anping` — 景點卡：安平古堡

- 用在：景點卡「安平古堡」：鄭成功收復的荷蘭古堡
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A stone-and-brick fort with a red-brick watchtower on a hill, old banyan trees, harbour behind, 17th-century Dutch style. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_jingzijiao` — 景點卡：井仔腳鹽田

- 用在：景點卡「井仔腳鹽田」：瓦盤鹽田池底鋪碎瓦片
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Tile-paved salt fields: a grid of terracotta-tile pans reflecting the sky, a small salt worker's hut, a wide flat coastal plain. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_budai` — 景點卡：布袋

- 用在：景點卡「布袋」：沙洲圍出的布袋狀潟湖
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fishing port on a lagoon with fishing boats, old salt-pan remains and gulls, a quiet evening. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_peitian` — 景點卡：朴子配天宮

- 用在：景點卡「朴子配天宮」：相傳神像搬不動而建廟
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A grand Mazu temple, in the main hall a large dark wooden Mazu statue whose base still joins the roots of the tree it was carved from, drifting incense smoke and lanterns. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_shuishang` — 景點卡：水上北回歸線標

- 用在：景點卡「水上北回歸線標」：夏至正午立竿沒有影子
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A monument marking the Tropic of Cancer at noon: a tall stone pillar with an arch, the sun directly overhead so the pillar casts almost no shadow, a line on the ground, plain fields. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_hinoki` — 景點卡：檜意森活村

- 用在：景點卡「檜意森活村」：阿里山林業的檜町宿舍
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Rows of old Japanese-style cypress-wood dormitories with tiled roofs, trees and a garden path. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_xiluo` — 景點卡：西螺大橋

- 用在：景點卡「西螺大橋」：長近兩公里的鋼桁大橋
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A long steel truss bridge over a broad river with a few pedestrians and bicycles, farmland, evening light. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_lukang` — 景點卡：鹿港老街

- 用在：景點卡「鹿港老街」：摸乳巷窄處約六十公分
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A very narrow stone-paved alley between old brick houses, a half-moon well set into a wall, hanging lanterns. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_roundhouse` — 景點卡：彰化扇形車庫

- 用在：景點卡「彰化扇形車庫」：人稱火車頭旅館的車庫
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fan-shaped railway roundhouse with locomotives pointing out from a central turntable, steam, brick building. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_gaomei` — 景點卡：高美濕地

- 用在：景點卡「高美濕地」：萬歲大眼蟹舉螯的濕地
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Tidal wetlands at sunset with wind turbines in silhouette, a wooden boardwalk, and in the foreground small fiddler crabs each raising one big claw as if cheering. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_dajia` — 景點卡：大甲鎮瀾宮

- 用在：景點卡「大甲鎮瀾宮」：九天遶境三百四十公里
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A grand Mazu temple at dawn with a long procession carrying a palanquin, banners and firecracker smoke. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_tongxiao` — 景點卡：通霄精鹽廠

- 用在：景點卡「通霄精鹽廠」：巨大精鹽包造景的鹽廠
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A salt-works visitor factory with a giant salt-bag shaped sculpture, white pyramids of salt, the sea behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_hsinchu` — 景點卡：新竹城隍廟

- 用在：景點卡「新竹城隍廟」：城隍廟對面掛著大算盤
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old city-god temple with ornate red roofs, a giant abacus hanging on the facade opposite it, a stone city gate beside, racks of rice noodles drying in the wind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_taoyuan` — 景點卡：桃園神社

- 用在：景點卡「桃園神社」：石燈籠夾道的神社參道
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A Japanese-style shrine with stone lanterns and a torii gate, large trees, stone steps. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_shihsanhang` — 景點卡：十三行博物館

- 用在：景點卡「十三行博物館」：羅盤偏轉發現煉鐵遺址
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A modern museum building at a river mouth with ancient pottery and iron-smelting finds displayed, the sea behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_dihua` — 景點卡：迪化街

- 用在：景點卡「迪化街」：中藥南北貨的年貨大街
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old trading street with narrow baroque-style shop facades, hanging dried goods, red lanterns, cobbles. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

---

### `card_finish` — 景點卡：回到台北

- 用在：景點卡「回到台北」：大廳地板像愛麗絲棋盤
- 尺寸：1024×768　一般圖，不用去背

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The great hall of a big city railway station with a black-and-white checkerboard floor, a few travellers sitting on the black squares, a backpack set down, warm light from tall windows. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```
