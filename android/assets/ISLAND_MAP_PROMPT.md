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

## 關於清晰度（先講清楚）

一張圖的解析度有上限：Gemini 最高約 4K，也就是整座島南北約 4096 像素，每個像素約 100 公尺。
- 一個畫面看 **40 公里寬**（約走 7 天的路）：大約是 2 倍放大，水彩手繪風格看起來會略軟但可以接受。
- 一個畫面看 **15 公里寬**：約 6 倍放大，會明顯變糊。

所以預設會建議用 40 公里左右的畫面。如果之後你覺得不夠清楚，我可以再做第二步：把這張圖切成北、中、南三段，
請 Gemini 照這張圖的樣子重畫成更精細的版本（這一步有這張當位置依據，不容易歪）。先不用做。
