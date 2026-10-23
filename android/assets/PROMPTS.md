# 素材提示詞（給 Gemini 等圖像 AI 使用）

## 怎麼用

1. **先做 1 張試風格**：`traveller_walker`（再加一張景點卡）。風格滿意後，把它與 `previews/style_reference.jpg`（現有景點卡的風格）一起當「風格參考圖」上傳，生成其他素材，才會一致。
2. **每張單獨生成**：把該素材的整段提示詞貼進去。提示詞開頭已經寫了比例；Gemini 支援的比例是 1:1、3:4、4:3、9:16、16:9，**匯出時會自動裁成規格尺寸**。
3. **一律要純白底**（提示詞已寫）。Gemini 不會輸出透明背景，匯出工具會把與邊緣相連的白底去掉。
4. **不要有文字**：地名、數字都由 App 畫。圖裡如果出現字，請重生成。
5. **存檔**：存成 `android/assets/generated/<id>.png`（檔名就是下面標題裡的 id）。完成後執行 `python3 tools/build_assets.py check` 檢查、`export` 匯出；或把圖直接傳給 Claude 處理。
6. 不滿意就用同一個提示詞再生成，或在對話裡說明要改什麼（例如「線條再細一點」「顏色更淡」）。

## 共同風格（已經包含在每個提示詞裡，不用另外貼）

Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures.

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
Create an image with aspect ratio 9:16. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. 
```

## icon

### `icon_foreground` — App 圖示前景

- 用在：自適應圖示（桌面圖示）
- 最終尺寸：1024×1024　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：先上傳 generated/traveller_sheet.jpg 當角色參考。重要內容限制在中央 60% 以內（安全區）；需要另外一張單色版（同構圖，單一深色）給主題圖示用。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Use the uploaded character sheet as the exact reference for the character (the small round glossy white spirit with big black eyes, wide-brim cream hat, patched green cloak, backpack, crooked staff with a glowing lantern). Draw this character cheerfully standing in front view on a small simple moss-green Taiwan island silhouette, a dotted brass walking route curling around the island, simple bold shapes, everything centred and kept inside the middle 60% of the frame with generous empty margin around it. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## card

### `card_taipei101` — 景點卡：台北101

- 用在：景點卡「台北101」：六百噸阻尼球穩住高塔
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Taipei 101, a tall skyscraper whose tiers look like stacked bamboo segments, rising above a basin city of low rooftops with green mountains behind, soft morning mist. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_keelung` — 景點卡：基隆廟口

- 用在：景點卡「基隆廟口」：油炸麵包夾料的三明治
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A bustling night-market lane beside a small temple, red lanterns, steaming food stalls and paper lanterns in a harbour town. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_jiufen` — 景點卡：九份

- 用在：景點卡「九份」：悲情城市捧紅山城芋圓
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A hillside mountain town of stone stairways and glowing red lanterns above a calm sea at dusk, old tea houses with tiled roofs. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_fulong` — 景點卡：福隆

- 用在：景點卡「福隆」：雙溪在這裡流入太平洋
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A long sandy beach where a small river meets the Pacific, a railway station and track in the background, a paper lunchbox on a bench. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_sandiaojiao` — 景點卡：三貂角

- 用在：景點卡「三貂角」：西班牙人取的聖地牙哥
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A white lighthouse on a green headland at the eastern tip of an island, waves breaking on dark rocks, wide Pacific horizon. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_toucheng` — 景點卡：頭城老街

- 用在：景點卡「頭城老街」：開蘭第一街約六百公尺
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old street of narrow red-brick shophouses with arcaded fronts and hanging signs, a gate at the end of the street, quiet morning. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_ncta` — 景點卡：傳統藝術中心

- 用在：景點卡「傳統藝術中心」：園內三條街仿各地老街
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traditional arts village beside a river: old-style buildings with curved roofs, a small stage, craft stalls with lanterns, willows by the water. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_suao` — 景點卡：蘇澳冷泉

- 用在：景點卡「蘇澳冷泉」：冷泉曾做彈珠汽水前身
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cold spring pool with small bubbles rising in clear water in a small port town, fishing boats and green hills in the background. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_qixingtan` — 景點卡：七星潭

- 用在：景點卡「七星潭」：七星潭名為潭實為海灣
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A crescent-shaped bay with a pebble beach, waves rolling in, distant mountains and a wide Pacific sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_pinegarden` — 景點卡：松園別館

- 用在：景點卡「松園別館」：能走進防爆門的防空洞
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A two-storey Japanese colonial-style wooden building among tall pine trees, a quiet garden path, the sea visible beyond. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_lintienshan` — 景點卡：林田山

- 用在：景點卡「林田山」：大火後倖存的林業木屋
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A former forestry village on a mountainside: wooden houses with tin roofs, an old log-hauling railway track, tall cedar trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_ruisui` — 景點卡：瑞穗北回歸線標

- 用在：景點卡「瑞穗北回歸線標」：北回歸線每年緩緩北移
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A white monument marking the Tropic of Cancer on a terrace with a round plaza, rice fields and mountains behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_yuli` — 景點卡：玉里

- 用在：景點卡「玉里」：當地招牌小吃是玉里麵
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A bowl of steaming noodles on a wooden table in a small-town eatery, a valley railway station visible through the window. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_chishang` — 景點卡：池上

- 用在：景點卡「池上」：月台上賣起的池上便當
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A traditional railway lunchbox of rice with side dishes beside a small train-station platform, golden rice paddies behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_guanshan` — 景點卡：關山

- 用在：景點卡「關山」：水牛稻田間騎環鎮單車
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A riverside bicycle path looping around a small valley town, a few cyclists, water buffalo standing in green rice paddies, mountains on both sides. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_luye` — 景點卡：鹿野高台

- 用在：景點卡「鹿野高台」：夏季舉辦熱氣球嘉年華
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Hot-air balloons rising over a green highland plateau in summer, people on the grass, a wide valley below. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_prehistory` — 景點卡：史前博物館

- 用在：景點卡「史前博物館」：興建車站挖出史前遺址
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A museum hall with ancient stone tools, pottery and carved stone slabs displayed in soft light, a standing stone column in front. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_taimali` — 景點卡：太麻里

- 用在：景點卡「太麻里」：二〇〇〇年迎曙光之地
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A coastal railway platform at dawn, a golden sun disc just rising out of the Pacific, orange light on the water, a slope of orange daylilies. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_shouka` — 景點卡：壽卡

- 用在：景點卡「壽卡」：屏東臺東交界山口驛站
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A cyclists' rest stop at a high mountain pass on a winding coastal-mountain road, a Paiwan-style carved stone statue of a warrior at the entrance, clouds in the valley, forest on both sides. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_checheng` — 景點卡：車城福安宮

- 用在：景點卡「車城福安宮」：福德正神穿龍袍戴官帽
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Fu-An Temple, an Earth God temple in Checheng, Pingtung, Taiwan, drawn only from these facts: a grand palace-style temple of three halls with an unusually tall main hall about five to six storeys high; in the forecourt a large octagonal paper-burning furnace with three-tiered eaves and four storeys; inside the main doorway the Earth God statue has a black face, a prime minister's hat and a dragon robe, with many small gold medals hanging all over its body. Red lanterns and drifting incense smoke. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_hengchun` — 景點卡：恆春古城

- 用在：景點卡「恆春古城」：恆春古城四座城門仍在
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old stone city gate with a tower in a thick stone wall, a quiet street passing through the gate arch, a small bus waiting to pass, tropical trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_eluanbi` — 景點卡：鵝鑾鼻

- 用在：景點卡「鵝鑾鼻」：圍牆環繞的小要塞燈塔
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A white lighthouse standing on a green cape at the southern tip of an island, ocean on both sides, tropical vegetation. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_maobitou` — 景點卡：貓鼻頭

- 用在：景點卡「貓鼻頭」：狀似蹲貓的波浪狀礁岩
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A limestone reef coast with wave-shaped rocks and one rock that looks like a crouching cat, turquoise sea, blue sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_nmmba` — 景點卡：海生館

- 用在：景點卡「海生館」：海底隧道中看魚群環繞
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A curved glass tunnel under a huge aquarium tank, a whale shark, rays and schools of fish circling overhead, deep blue light, small visitors looking up. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_fangliao` — 景點卡：枋寮

- 用在：景點卡「枋寮」：月台邊近看火車轉車盤
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A small railway station platform with a round railway turntable in the foreground and a locomotive being turned on it, a coast line and mountain line meeting behind, palm trees. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_donggang` — 景點卡：東港

- 用在：景點卡「東港」：東港迎王平安祭典主廟
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fishing harbour with fishing boats, a bluefin tuna on the dock, a colourful temple procession carrying a ceremonial boat. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_pier2` — 景點卡：高雄駁二

- 用在：景點卡「高雄駁二」：名字來自二號接駁碼頭
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Old harbour warehouses turned into art spaces, colourful sculptures, a rail track, cranes and the sea behind. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_lotuspond` — 景點卡：蓮池潭

- 用在：景點卡「蓮池潭」：龍虎塔要龍口進虎口出
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A lotus pond with a pair of ornate pagodas, a dragon-head entrance on one and a tiger-head exit on the other, red and green lacquer, a pavilion reflected in the water. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_qiaotou` — 景點卡：橋頭糖廠

- 用在：景點卡「橋頭糖廠」：糖廠鐵道還能搭五分車
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. An old sugar refinery with brick chimneys, a small narrow-gauge sugar train running through a long green tunnel of trees, sugar-cane fields. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_anping` — 景點卡：安平古堡

- 用在：景點卡「安平古堡」：鄭成功收復的荷蘭古堡
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：依使用者提供的實景照片逐項描述（2026-10-22）；不需再附參考照片。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Anping Fort (Fort Zeelandia remains) in Tainan, Taiwan, drawn exactly like this real view: in the middle a tall white lookout tower with a square tapering shaft, a wider square cabin at its top with large slanted glass windows on every side (like an air-traffic-control cab), and a steep pyramid roof of orange-red clay tile with a tiny spike on top; two small narrow windows low on the shaft. Below it a long wall of old red-brick with thin lighter horizontal bands and rows of small square holes, and a wide straight red-brick staircase of about twenty steps with a thin black metal handrail climbing up to a terrace; on the terrace behind the wall a low Japanese-style building with a grey-tiled hipped roof and an arched verandah with cream columns, and a flagpole with a small red pennant. In front a courtyard paved with red brick, and big old banyan trees with neatly pruned umbrella-shaped crowns and aerial roots wrapped around their trunks, plus two small clipped pine-like shrubs. Soft cloudy sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_jingzijiao` — 景點卡：井仔腳鹽田

- 用在：景點卡「井仔腳鹽田」：瓦盤鹽田池底鋪碎瓦片
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Tile-paved salt fields: a grid of terracotta-tile pans reflecting the sky, a small salt worker's hut, a wide flat coastal plain. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_budai` — 景點卡：布袋

- 用在：景點卡「布袋」：沙洲圍出的布袋狀潟湖
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Haomeiliao sandbar near Budai, Chiayi, Taiwan, drawn only from these facts: a low flat sandbar of soft pale sand beside a quiet lagoon, a windbreak forest of casuarina trees behind it with a few coconut palms and beach morning-glory on the sand, clam ponds, rows of oyster racks standing in the shallow water around the harbour, calm light. A flat sandy shore, not rocky cliffs, no salt pyramids, no church. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_peitian` — 景點卡：朴子配天宮

- 用在：景點卡「朴子配天宮」：相傳神像搬不動而建廟
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：依使用者提供的實景照片逐項描述（2026-10-22）；不需再附參考照片。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Peitian Temple (Mazu temple) in Puzi, Chiayi, Taiwan, drawn exactly like this real front view: a wide traditional Minnan temple front hall seen straight on, with a swallowtail roof whose middle ridge is highest; orange-red clay-tile roofs in layered curves; along the ridges rich colourful cut-and-paste ceramic decoration, with a pair of green dragons facing each other on the central ridge and a small group of figures between them, phoenixes and flames on the side ridges, blue and green trim on the eaves; a horizontal plaque with gold characters across the front roof edge; a row of eleven round red lanterns hanging under the eaves; two carved grey stone dragon pillars at the central doors, stone pillars with blue-and-white ceramic panels on the side bays; in the middle of the forecourt a large golden incense burner with two dragon handles tied with a red ribbon; on both sides festive blossom trees decorated with red and cream flowers; a wide stone-paved forecourt and a clear deep-blue sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_shuishang` — 景點卡：水上北回歸線標

- 用在：景點卡「水上北回歸線標」：夏至正午立竿沒有影子
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：依使用者提供的實景照片逐項描述（2026-10-22）；不需再附參考照片。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Tropic of Cancer Marker in Shuishang, Chiayi, Taiwan, drawn exactly like this real view: in the foreground a tall narrow white monument tower with a flat tapering shaft, a column of orange-red vertical characters running down its face (draw them as abstract orange-red brush marks, not readable), a thick white base block set on a black polished granite plinth, and on top a cage of three silver metal rings forming an armillary sphere. Behind it to the right a big white saucer-shaped dome building (the sun hall) standing on a pillared round base, its dome ribbed with curved arches and a thin mast on top, a railing around its ring-shaped platform; low green lawn between them; a few small white stone lanterns on the right; a brick-paved plaza with red and grey stripes; big white clouds in a blue sky. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_hinoki` — 景點卡：檜意森活村

- 用在：景點卡「檜意森活村」：阿里山林業的檜町宿舍
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Hinoki Village in Chiayi, Taiwan, drawn only from these facts: rows of single-storey Japanese-style wooden houses raised about 75 centimetres off the ground on posts with ventilation gaps, walls of overlapping horizontal wooden boards, Japanese-style tiled roofs, quiet paths between the houses under big shady trees, warm cypress-wood tones. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_xiluo` — 景點卡：西螺大橋

- 用在：景點卡「西螺大橋」：長近兩公里的鋼桁大橋
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A long steel truss bridge over a broad river with a few pedestrians and bicycles, farmland, evening light. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_lukang` — 景點卡：鹿港老街

- 用在：景點卡「鹿港老街」：摸乳巷窄處約六十公分
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The old street of Lukang, Taiwan, drawn only from these facts: red-brick Minnan-style shophouses and red-brick paved lanes, wooden doors and wooden lattice windows, a winding lane with orange-red brick walls, and between two buildings a very narrow gap only one person wide (about 60 to 70 centimetres) where a person must turn sideways. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_roundhouse` — 景點卡：彰化扇形車庫

- 用在：景點卡「彰化扇形車庫」：人稱火車頭旅館的車庫
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：4:3 橫式；不要任何文字；邊緣淡出到羊皮紙色即可，不必去背。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A fan-shaped railway roundhouse with locomotives pointing out from a central turntable, steam, brick building. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no people's faces in close-up.
```

### `card_gaomei` — 景點卡：高美濕地

- 用在：景點卡「高美濕地」：萬歲大眼蟹舉螯的濕地
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Gaomei wetland in Taichung, Taiwan, drawn only from these facts: a dozen or so pure white wind turbines standing in a row along the seawall road, a disused octagonal lighthouse with red and white parallel stripes, a long curving wooden boardwalk out over the tidal flat, patches of green marsh grass turning golden, shallow water that mirrors the sky at low tide, the sea turning gold and orange-red at sunset, and small fiddler crabs each raising one big claw in the foreground. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_dajia` — 景點卡：大甲鎮瀾宮

- 用在：景點卡「大甲鎮瀾宮」：九天遶境三百四十公里
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Zhenlan Temple in Dajia, Taichung, Taiwan, drawn only from these facts: a traditional Minnan temple with a front hall and a main hall around a courtyard, a big open forecourt, a roof ridge covered with colourful cut-and-paste ceramic figures with gold leaf (the three star gods and a dragon-riding general on the front ridge), and a large plaque above the entrance with no readable writing. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_tongxiao` — 景點卡：通霄精鹽廠

- 用在：景點卡「通霄精鹽廠」：巨大精鹽包造景的鹽廠
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Tongxiao salt-works visitor park in Miaoli, Taiwan, drawn only from these facts: a giant salt-sack landmark taller than a person near the entrance, a sundial-shaped pattern on the paved ground, a fresh blue-and-white colour theme, clean modern low buildings, the sea in the background. It is a visitor park, not a salt pan and not a salt mountain. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_hsinchu` — 景點卡：新竹城隍廟

- 用在：景點卡「新竹城隍廟」：城隍廟對面掛著大算盤
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The City God Temple of Hsinchu, Taiwan, drawn only from these facts: a traditional Minnan temple whose gable walls rise and fall in continuous waves, roof ridges with swallowtail ends and ceramic dragons, three bays with a pair of blue-stone lions at the central door, and in front of the temple a lively open plaza of food stalls. Do not draw an abacus. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_taoyuan` — 景點卡：桃園神社

- 用在：景點卡「桃園神社」：石燈籠夾道的神社參道
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Taoyuan Shinto shrine in Taiwan, drawn only from these facts: Japanese shrine buildings of unpainted cypress and cedar wood (not a Taiwanese temple), roofs of copper tiles weathered to green, a long stone-paved approach with stone lanterns on both sides, a purification water pavilion, and one single surviving torii gate whose top crossbeams have been removed. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_shihsanhang` — 景點卡：十三行博物館

- 用在：景點卡「十三行博物館」：羅盤偏轉發現煉鐵遺址
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. The Shihsanhang Museum of Archaeology in Bali, New Taipei, Taiwan, drawn only from these facts: raw grey concrete, sandstone and weathered metal panels, a three-storey block under a long steel roof shaped like a whale's back, an octagonal tower that visibly leans (about 17 degrees) like a tilted chimney, long grey stairs outside, and seen from the north plaza the whole building looks like a big ship. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

### `card_dihua` — 景點卡：迪化街

- 用在：景點卡「迪化街」：中藥南北貨的年貨大街
- 最終尺寸：1024×768　Gemini 比例：4:3　背景：一般圖，不用去背
- 注意：外觀只用查證確認或單一來源的細節（docs/research/visual_out_*.md、visual2_out_*.md），不附參考照片；未查到的細節不指定。

```
Create an image with aspect ratio 4:3. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. Dihua Street in Taipei, Taiwan, drawn only from these facts: narrow deep two-storey shophouses in a continuous row with a covered arcade walkway, a mix of red-brick facades and Baroque-style facades in red brick and grey concrete with moulded Baroque gable decoration, a regular pattern of four columns and three windows on the front, dried goods in open sacks displayed under the arcade, a crowd of shoppers seen from a distance. A vignette illustration with soft irregular edges fading into aged parchment, 4:3 landscape, one clear focal subject in the centre, calm and healing mood. No text, no letters, no numbers, no signboards, no people's faces in close-up.
```

## map

### `real_map_base` — 真實世界（台灣）地圖底圖

- 用在：首頁真實世界地圖
- 最終尺寸：1116×2000　Gemini 比例：9:16　背景：一般圖，不用去背
- 注意：海岸線要照 mapguide/real_map_guide.png；沒有文字、沒有邊框。 已完成並接進 App，不需要再做；製作紀錄在 archive/map_pipeline/。

```
Create an image with aspect ratio 9:16. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. 已完成，見 archive/map_pipeline/
```

## ui

### `collection_slot_locked` — 收藏格：尚未收集

- 用在：收藏頁尚未收集景點的格子（取代目前的問號徽章）
- 最終尺寸：512×512　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：中央留空讓 App 疊上景點序號；不要文字與數字。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A blank collectible card slot drawn like an empty pressed-specimen mount in an old naturalist's album: a faint pencil outline of a small landmark silhouette inside a rounded rectangle of aged paper with soft hatched shading, a tiny brass corner mount at each corner, muted and quiet. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

### `collection_slot_frame` — 收藏格：已收集外框

- 用在：收藏頁已收集景點卡的外框（透明中央，疊在景點插圖上）
- 最終尺寸：512×512　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：中央必須完全空白（之後去背）；邊框寬度約整張的 6%；不要文字。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A thin ornamental picture frame for a postcard-like collectible card: aged cream paper border with fine sepia pen lines, small brass corner pieces, subtle watercolor stains at the edges, empty transparent centre. Isolated on a completely flat, plain, pure white background (#FFFFFF) with nothing else in the frame, no shadow on the background, no paper texture, clean edges so it can be cut out.
```

## store

### `store_feature` — 商店主視覺（1024x500）

- 用在：Google Play 商店頁頂端橫幅
- 最終尺寸：1024×500　Gemini 比例：16:9　背景：一般圖，不用去背
- 注意：左右留白約 10%（商店會裁切）；不要文字，App 名稱「來台灣走走」由後製加上。 先上傳 generated/traveller_sheet.jpg 當角色參考。

```
Create an image with aspect ratio 16:9. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. A wide panoramic illustration of the Taiwan coastline drawn as an old travel map vignette: green mountains in the middle, a dotted walking route circling the island, small sketches of a lighthouse, a temple roof, a night-market lantern string and a steam-train station along the route, the small round white spirit traveller from the uploaded character sheet (hat, patched green cloak, backpack, staff with a glowing lantern) walking along the route in the foreground, matching the reference exactly. Soft irregular edges fading into aged parchment, 1024x500 landscape, calm and warm mood.
```

## character

### `traveller_body` — 旅人：身體（含帽子、披風、背包）

- 用在：環島地圖上的目前位置（走路動畫的三層）
- 最終尺寸：411×400　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：三張同尺寸、同座標，疊在一起就是完整角色，面向右。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. （由使用者的角色設定圖 generated/traveller_sheet.jpg 拆層而成，不需要再用 AI 出圖；做法見 tools/traveller_rig/）
```

### `traveller_staff` — 旅人：手杖與提燈

- 用在：環島地圖上的目前位置（走路動畫的三層）
- 最終尺寸：411×400　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：三張同尺寸、同座標，疊在一起就是完整角色，面向右。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. （由使用者的角色設定圖 generated/traveller_sheet.jpg 拆層而成，不需要再用 AI 出圖；做法見 tools/traveller_rig/）
```

### `traveller_hand` — 旅人：握杖的手

- 用在：環島地圖上的目前位置（走路動畫的三層）
- 最終尺寸：411×400　Gemini 比例：1:1　背景：純白（匯出去背）
- 注意：三張同尺寸、同座標，疊在一起就是完整角色，面向右。

```
Create an image with aspect ratio 1:1. Vintage botanical field-guide illustration: fine pen-and-ink linework in dark sepia brown, delicate watercolor washes in muted moss green, dusty rose and old brass, drawn by hand in a naturalist's notebook, gentle cross-hatching, calm, warm and slightly nostalgic travel-journal mood. Absolutely no text, letters, numbers, logos, watermarks or signatures. （由使用者的角色設定圖 generated/traveller_sheet.jpg 拆層而成，不需要再用 AI 出圖；做法見 tools/traveller_rig/）
```
