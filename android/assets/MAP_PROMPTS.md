# 世界地圖與事件插圖：給 Gemini 的提示詞

風格統一為「泛黃羊皮紙＋鋼筆細緻素描＋淡水彩（暗綠、褐色、枯黃、大地色）」。
**每次對話都先貼「風格前綴」，並上傳 `previews/2_compass.jpg` 當風格參考圖。**

## 風格前綴（每個提示詞前面都貼）

> Antique botanical field-guide illustration on aged, yellowed parchment with fine fibre texture and faint creases. Fine dark-brown pen-and-ink line work with a light watercolour wash in muted dark green, brown, ochre and earth tones. Hand-drawn, slightly imperfect lines, gentle paper grain, no digital gloss, no neon colours, no 3D rendering.

---

## 1. 主地圖（最重要的一張）

- 上傳 `mapguide/layout_guide.png`（版面草圖）作為「構圖參考」。
- 比例選 **3:4 直式**，解析度選最高（至少 2K）。
- 草圖上的數字圓圈與紅色三角形只是定位標記，**不要畫進成品**。

> Use the uploaded image ONLY as a layout guide for the position of the land, the sea and the seven regions; do not copy its flat colours, its numbered circles or its red triangle. Draw a richly detailed fantasy world map, portrait 3:4, in the style described above. One large island continent with a winding, finely drawn coastline, surrounded by sea shown with fine horizontal pen hatching, small wave marks, and a few tiny sea creatures. The map is seen from a slightly raised top-down view, like an old atlas.
>
> Regions, matching the guide (numbers are for you only):
> 1. Top-left: a dense whispering forest — layered round-crowned trees, ferns, glowing mushroom rings, a stream, a winding dirt path entering from the top-left corner coast where a tiny wooden jetty stands.
> 2. Top-right: a moonlit coast — pale sand beaches, dunes with reeds, tide pools, a small lighthouse ruin, shells, a calm bay.
> 3. Right side, in the sea: a cluster of four small coral islets with turquoise shallows, ring reefs and tiny palm-like plants.
> 4. Left-middle: a misty swamp — still dark water, mangrove roots, boardwalk fragments, reeds, drifting mist drawn as soft pale wisps.
> 5. Lower-left: star dunes — rolling ochre sand ridges, a few dry shrubs, a dry riverbed, tiny star-shaped flowers.
> 6. Lower-right: sky ruins — rocky highlands with broken white stone arches and pillars, and a few fragments floating just above the ground with small drop shadows.
> 7. Bottom-centre: the World Tree — one enormous ancient tree with a wide crown and roots spreading over a green meadow, the largest and most detailed object on the map.
>
> The land between the regions is gentle hills, meadows and small forests connecting them into one continuous continent (a hand-drawn dirt path may faintly link regions in order 1→2→3→4→5→6→7, but keep it subtle). Add a small ornamental compass rose in the lower-left sea corner and a thin ornate hand-drawn border with vine corners. Absolutely NO text, NO place names, NO labels, NO numbers, NO legend, NO route line, NO red markers.

**驗收：** 七個區域的相對位置要和草圖一致；沒有任何文字；陸地輪廓離邊框至少 3%。位置偏太多就重新生成，或補一句 "move region X further toward the top-right".

存檔：`generated/world_map_master.png`（若比例不是 3:4，告訴我，App 會依圖片比例調整）。

## 2. 區域特寫精修（可選，讓放大看更精緻）

地圖在手機上會被放大查看，主圖解析度不夠時，用這個流程：把主圖裁出一個區域（我可以幫你裁），上傳後貼：

> Redraw the uploaded map crop at much higher detail and larger scale, keeping exactly the same layout, coastline, colours and style: same positions of trees, water, paths and buildings. Add more fine pen detail, tiny plants, texture and small curious details, but add no text and no new large objects. Output a clean crop with the same aspect ratio.

存檔：`generated/region_<編號>.png`。

## 3. 地標小圖示（7 個，白底去背）

沿用 `PROMPTS.md` 裡的 `landmark_forest / coast / coral / swamp / dunes / ruins / tree` 提示詞。
若地圖主圖已經畫得夠精緻，地標圖示可以省略，只在「已抵達」處顯示發光標記。

## 4. 事件插圖（10 張，故事跳出時顯示）

比例 **4:3 橫式**。每張的構圖：有淡淡手繪邊框，邊緣漸淡融入紙張，中央主角清楚，**不要文字**。

通用句型（把 `<SCENE>` 換成下表場景）：

> <SCENE>. Vignette illustration with soft irregular edges that fade into the parchment, 4:3 landscape, a single clear focal subject in the centre, a gentle magical glow, whimsical but calm and healing mood. No text, no letters, no frame numbers.

| 檔名 | SCENE |
|---|---|
| ev_forest_fawn | A small fawn spirit with tiny glowing antlers sitting by a stream, one foreleg tangled in green vines, soft forest light |
| ev_forest_riddle | A round fluffy moss spirit with two tiny eyes blocking a forest path, a question-mark-shaped curl of vine floating above it |
| ev_forest_mushrooms | A perfect ring of softly glowing mushrooms in a dark forest clearing, faint motes of light rising |
| ev_forest_firefly | A single lost firefly circling in the air in front of a traveller's shoulder, trail of light looping |
| ev_forest_oldtree | A huge fallen ancient tree across a path, bark carved with faint glowing runes, moss and tiny flowers growing on it |
| ev_coast_whale | A giant gentle tide-whale spirit surfacing far away under a large moon, ripples of silver light on the water, a small figure listening on the shore |
| ev_coast_crab | A hermit crab merchant with an enormous spiral shell stall on a beach, tiny jars and trinkets laid out on a cloth |
| ev_coast_jellyfish | Several translucent glowing moon-jellyfish drifting in shallow water at dusk like little lanterns |
| ev_coast_bottle | An old glass bottle sealed with wax washed up on wet sand, a faint glow inside, foam around it |
| ev_coast_lowtide | A very wide low-tide beach with glistening wet sand, shells and a sunset-coloured sky mirrored on the sand |

## 5. 道具得到時的插圖（可選）

沿用 `PROMPTS.md` 的 `item_*` 6 個白底圖示即可。

## 6. 命名與交付

全部放進 `android/assets/generated/`，檔名照上面寫。放好後跟我說一聲，我會匯出到 App 並接上。
