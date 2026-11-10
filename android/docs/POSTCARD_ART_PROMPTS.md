# 明信片風景圖：Gemini 提示詞

每張明信片上方是一幅「該地的風景」，畫風要和 App 的景點卡一致（細棕色鉛筆線、褪色淡水彩、有紙紋、邊緣暈開成空白紙）。
一張明信片對應一幅圖，檔名是 `postcard_<代號>.png`，代號寫在 `app/src/main/res/raw/postcard_facts.txt` 的第 4 欄。

## 做法
1. 在 Gemini 同一個對話裡，**附上 2～3 張 App 的景點卡當風格參考**（`app/src/main/res/drawable-nodpi/art_card_toucheng.webp`、`art_card_sandiaojiao.webp`、`art_card_fulong.webp` 等），再貼「共同風格」。
2. 依序貼各張的提示詞。每張生成 3～4 個，挑最好的一張。
3. 存成 PNG，**橫式 3:2（1536×1024）**，檔名照下表。
4. 把圖給我，我縮成 1024 寬的 WebP、檢查在明信片上的樣子後接進 App。（圖還沒到之前，明信片先顯示該區的輪廓當佔位。）

## 共同風格（附參考圖後貼）
```
A series of landscape illustrations for postcards in a mobile walking-adventure game set in Taiwan, matching the attached reference images exactly in style.

Style: a travel-sketchbook illustration like the reference images: thin sepia-brown pencil and fine pen lines with a little hatching, soft faded watercolour washes in muted, greyed colours, visible paper grain. The painted scene fades out irregularly into blank old paper at the edges (soft vignette), like a sketch on a notebook page. Detailed enough to recognise the place, but loose and hand-drawn, NOT a photograph, NOT clean vector art, NOT glossy digital painting.

Composition: landscape 3:2 aspect ratio, 1536 x 1024 pixels. One clear view of the place, with a foreground, a middle ground and a background. No people unless stated. IMPORTANT: no text, letters, signs or Chinese characters anywhere in the image; leave signboards and banners blank.

Confirm you understand and wait for the first scene.
```

## 各張的場景（只畫有資料依據的東西）

### `pingxi`：新北市平溪區（阿鐵）
事實：平溪線原本是煤礦公司為運煤而修的鐵路，沿線留有礦場遺跡。
```
Scene "pingxi": a small old branch-line railway running through a narrow mountain valley in Taiwan. A short two-carriage diesel train (blue and white, rounded old design) passes slowly along a single track between old low brick and tin-roofed houses. In the background, forested steep hills with mist, and the weathered concrete and timber framework of an old coal-mine inclined shaft entrance half covered by plants. A few rusty mine-cart rails in the foreground. No text, no people.
```

### `sanxia`：新北市三峽區（墨墨）
事實：三峽祖師廟由畫家李梅樹主持重建，石雕木雕刻得極細。
```
Scene "sanxia": the front of a large traditional Taiwanese temple seen from its courtyard: a multi-tiered curved tile roof with ornate ridge decorations, richly carved stone dragon pillars, and finely carved stone panels and wooden eaves, in warm stone and faded red. A few carved stone lions at the entrance steps. Soft light, a feeling of great craftsmanship; draw some of the carving detail lightly with fine lines. Blank signboard above the door. No people, no text.
```

### `yehliu`：新北市萬里區（汐婆）
事實：野柳的蕈狀岩由海浪、風和風化磨成；女王頭因側面像婦人的頭而得名。
```
Scene "yehliu": a rocky headland on the northern coast of Taiwan with several mushroom-shaped rocks (thin neck, wider cap) standing on a flat wave-cut rock shelf with pools and ridges. In the centre, one rock whose top looks, in side view, like the head of a woman with a long neck and an elegant upswept hairstyle. Calm sea and a pale sky behind, small waves breaking on the shelf. No people, no text.
```

### `kouhu`：雲林縣口湖鄉（小禾）
事實：牽水狀祭典，轉動紙糊的水狀，追思水災亡者；題材沉重，畫面莊重安靜。
```
Scene "kouhu": a quiet coastal village field near the sea in Yunlin, Taiwan, at dusk. In the middle stands a tall, hollow, three-tiered structure made of thin bamboo strips covered in grey-white patterned paper, about one metre tall (a ritual paper structure called a "shuizhuang"), with a few small paper lanterns glowing beside it. Flat wetland and fishponds with reflections, a long low horizon, and the sea beyond. Respectful, still and gentle mood, soft muted colours. No people, no skulls, no text.
```

### `qingjing`：南投縣仁愛鄉（阿柏）
事實：清境一帶住著從滇緬邊境來的雲南籍居民和他們的後代，雲南料理是當地特色。
```
Scene "qingjing": a high mountain landscape in central Taiwan with green rolling grass slopes and a ridge line emerging from drifting white clouds, a few small houses with red-tile roofs on the hillside below. In the foreground on a wooden table: a steaming bowl of Yunnan-style rice noodles in broth with a little greens and herbs, and a small plate of fried dough, drawn with simple warm colours. Quiet misty morning light. No people, no text.
```

## 驗收清單
- 風格和景點卡一致：細棕色鉛筆線、褪色水彩、紙紋、邊緣暈開。
- 3:2 橫式，沒有文字、招牌字、浮水印。
- 場景只含上面寫的內容，沒有加奇怪的建築或人物。
- 口湖那張氣氛莊重，沒有驚悚或不敬的元素。
