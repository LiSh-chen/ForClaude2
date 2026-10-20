# 真實世界地圖：整張地面底圖（給 Gemini 的提示詞）

**一次畫整座台灣的地面**，不含任何物件。景點小圖、河流、道路、路線都由我之後按真實座標放上去，所以位置不會歪。

## 怎麼做

1. 新開 Gemini 對話，上傳**三張**圖：
   - 風格參考：`previews/2_compass.jpg`
   - 風格參考：`generated/real_map_base.png`（你最早那張地圖）
   - 草圖：`mapguide/island_guide.jpg`（2304×4096，已附上）
2. 比例選 **9:16 直式**，解析度選**最高（4K）**。如果有「重新生成較高畫質」或放大功能也請用。
3. 貼下面的提示詞。
4. 存成 `generated/island_ground.png` 傳給我。可以多生成幾張，挑山形最像草圖、海岸線最貼合的那張。

## 提示詞

```
Repaint the uploaded guide picture as a large, richly detailed antique hand-drawn pictorial map of the GROUND ONLY, in exactly the style of the two style reference images (fine dark-brown pen and ink, light watercolour wash, aged parchment feeling, gentle grain). Highest possible resolution, crisp fine linework.

The guide is a flat data map of the island of Taiwan: pale blue = sea, thin dark line = coastline, shaded relief = mountains and valleys (white-grey on the highest peaks), light green = forest, yellow = farmland, pink = towns, blue patches = lakes. The small separate islands in the sea must stay.

Keep the coastline and the position, shape and direction of every mountain range, valley, plain, lake and island EXACTLY where they are in the guide: same framing, the whole 9:16 portrait picture is the map and the sea reaches all four edges (no border, no frame, no margin, no compass, no title, no scale bar).
Seen from a slightly raised top-down view, like the reference map.

Paint:
- mountains and hills in pen-hatched relief that follows the shaded relief of the guide, with a soft green or brown wash; bare grey rock and a little snow only on the very highest central peaks;
- forests as tiny trees, denser on mountain slopes;
- farmland as a calm patchwork of paddies and fields in soft yellow-green and gold, flat and quiet;
- towns only as a pale warm wash with a very few tiny roofs, never dense;
- the sea with fine horizontal pen hatching and a few small wave marks close to the coast, becoming calm and pale far from land.

DO NOT draw any rivers or streams, roads, bridges, railways, buildings or landmarks, boats or ships, animals, sea monsters, labels, numbers or text. The ground must be quiet and free of such things, because all of those will be placed on top later.
```

## 交給我之後

- 我會先用海岸線疊圖檢查有沒有對上，必要時自動微調，讓畫面每一點都對上真實座標。
- 再把河流、道路、路線和景點小圖放上去，給你看幾個地點的效果。

## 第二步（第一步滿意後再做）：放大並加細節，分三段

不要請 Gemini 一開始就畫三段再要求它們完美接在一起：三張是各自獨立生成的，邊緣的山形、顏色、筆觸不會剛好一樣，沒辦法保證接縫完美。
比較穩的做法是**第一步整張畫好之後，再用「放大加細節」把它拆成三段**，因為三段都來自同一張圖，內容本來就一致，接縫由我處理。

1. 第一步傳回 `island_ground.png` 後，我會把它切成**北、中、南三段，每段和相鄰的那段重疊約 17%**，並附上對應的草圖，傳給你三張圖（`ground_north.png`、`ground_middle.png`、`ground_south.png` 的素材）。
2. 每一段各開一個對話，上傳風格參考兩張和這一段的圖，比例選 **3:2 橫式**、解析度選最高（4K），貼這段提示詞：

```
Upscale and refine the uploaded picture to the highest possible resolution, as the same antique hand-drawn pictorial map in the same style. Keep EVERYTHING exactly where it is: the coastline, every mountain, valley, plain and island keep their exact position, size and shape, and the picture is not cropped, shifted, rotated, mirrored or re-composed; the content at the very edges must stay the same as in the uploaded picture, because it will be joined to the neighbouring pictures.
Only add finer detail: crisper pen hatching on the mountains, more tiny trees in the forests, finer paddy and field patterns, finer wave hatching in the sea, finer paper grain. Keep the same colours and the same brush feeling.
Do NOT add any rivers, roads, buildings, landmarks, boats, animals, labels or text. No border, no frame.
```

3. 三段傳回後，我會用重疊部分做柔和的漸變接合，並檢查接縫與位置有沒有對上，再合成一張超高解析度的整張地圖。
   （三段原本就是同一張圖來的，接合處不會突然變樣；如果有哪一段跑掉，我會請你重畫那一段。）

這樣整座島南北約有 7000 多像素，每個像素約 55 公尺：
- 一個畫面看 **40 公里寬**：不用放大，很清楚。
- 一個畫面看 **15 公里寬**：約放大 3 倍，手繪水彩風格仍然偏軟，但比單張好很多。

## 關於清晰度（先講清楚）

只做第一步時：一張圖最高約 4K，整座島南北約 4096 像素，每個像素約 100 公尺。
- 一個畫面看 **40 公里寬**（約走 7 天的路）：大約 2 倍放大，手繪水彩風格看起來會略軟但可以接受。
- 一個畫面看 **15 公里寬**：約 6 倍放大，會明顯變糊。
做完第二步就會好很多，見上面。
