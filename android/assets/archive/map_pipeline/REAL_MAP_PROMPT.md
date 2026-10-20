# 真實世界（台灣）地圖：給 Gemini 的提示詞

用法：新開對話，貼「風格前綴」（同 `MAP_PROMPTS.md`），上傳 `previews/2_compass.jpg` 當風格參考，再上傳 `mapguide/real_map_guide.png` 當版面草圖，最後貼下面的提示詞。
比例選**直式**（草圖是 1200×2191，約 0.55，最接近 9:16；Gemini 給不出就選 9:16，再告訴我實際尺寸）。解析度選最高。
草圖上的紅色數字圓圈只是定位用，**不要畫進成品**；陸地輪廓（黑線）要照草圖畫，我的路線是按這個輪廓算的。

> Use the uploaded guide image ONLY for layout: draw Taiwan with exactly the same coastline, position and proportions as the beige land shape; do not copy its flat colours, grid lines, black outline or red numbered circles. Draw a richly detailed antique atlas map of the island in the style described above, portrait, seen from a slightly raised top-down view. The land fills the central part of the picture; the sea fills ALL four edges of the picture to the very edge (no border, no frame, no margin, no vignette, no corner ornaments, no compass rose), shown with fine horizontal pen hatching and small wave marks, thicker hatching near the coast.
>
> Fill the whole land with detail: green wash forests of tiny round and pointed trees, grass ticks, scattered tiny farmsteads, faint dirt roads linking the towns (the west plain road from north to south, the east valley road, and the coastal roads, all thin dashed ochre lines), and hand-drawn hills and mountains that blend smoothly into the plains with shaded slopes (no abrupt isolated blobs).
>
> Place these objects. Positions are given as a percentage from the left edge and from the top edge of the picture, matching the numbered circles on the guide. Draw every one of them, once, and do NOT add any text, labels, numbers or legend:

**A. Mountains**

1. Yushan: the highest peak, one tall snow-capped peak with a few lower summits around it — about 47% from the left, 54% from the top.
2. Xueshan: a large rugged peak with snow patches — about 60% from the left, 28% from the top.
3. Hehuanshan: a broad grassy high ridge with snow dusting — about 62% from the left, 35% from the top.
4. Alishan: rounded forested mountains of tall cedars, with a hint of a mountain railway line — about 40% from the left, 52% from the top.
5. Beidawu mountain: a lone pyramid-shaped peak in the far south — about 37% from the left, 77% from the top.
6. Central Mountain Range: a continuous spine of high ridges from the north-east tip, down the middle, to the southern tip (follows 1-2-3-5), ridges drawn as pen-hatched slopes with green wash on the lower flanks, snow only on the highest peaks — about 53% from the left, 39% from the top.
7. Coastal Range: a lower, greener, narrower ridge along the east side of the long valley, from about 23.9N to 22.8N — about 67% from the left, 56% from the top.

**B. Valleys, plains and lakes**

8. Huadong rift valley: a long narrow flat valley between the Central and Coastal Ranges, with strips of green and yellow farmland and a river running down its length — about 63% from the left, 56% from the top.
9. Chianan plain: a wide flat patchwork of rice fields, fish ponds and small villages — about 19% from the left, 58% from the top.
10. Lanyang plain: a small triangular plain of rice paddies opening to the sea — about 81% from the left, 19% from the top.
11. Taipei basin: a flat bowl among low hills, dense with tiny roofs — about 72% from the left, 10% from the top.
12. Pingtung plain: flat farmland with scattered orchards — about 28% from the left, 78% from the top.
13. Sun Moon Lake: a small lake ringed by hills — about 45% from the left, 43% from the top.
14. Taroko gorge: a deep marble gorge cut through the mountains to the east coast — about 72% from the left, 34% from the top.

**C. Rivers (thin blue pen lines, wiggling, widening toward the sea)**

15. Tamsui River with its branches, ending at the north-west tip of the Taipei basin — about 69% from the left, 8% from the top.
16. Dajia River from the mountains to the west coast — about 44% from the left, 31% from the top.
17. Zhuoshui River, the longest, from the central mountains to the west coast — about 35% from the left, 43% from the top.
18. Zengwen River into the south-west plain — about 21% from the left, 61% from the top.
19. Gaoping River into the south-west — about 33% from the left, 69% from the top.
20. Hualien and Xiuguluan Rivers along the Huadong valley, each reaching the east coast through a gap in the Coastal Range — about 67% from the left, 50% from the top.
21. Lanyang River into the Lanyang plain — about 82% from the left, 21% from the top.
22. Beinan River into the south-east coast — about 56% from the left, 71% from the top.

**D. Towns and a few landmarks (tiny old-style roofs, no text)**

23. Taipei: a dense cluster of small roofs with one tall slender tower (Taipei 101) — about 74% from the left, 10% from the top.
24. Keelung: a small harbour town with a breakwater — about 83% from the left, 7% from the top.
25. Hsinchu: a small walled town — about 47% from the left, 17% from the top.
26. Taichung: a medium town of roofs — about 34% from the left, 35% from the top.
27. Changhua: a small town with a hilltop big Buddha statue — about 27% from the left, 37% from the top.
28. Chiayi: a small town with a few roofs — about 23% from the left, 53% from the top.
29. Tainan: a town with a small fort by the coast (Anping) — about 12% from the left, 67% from the top.
30. Kaohsiung: a larger harbour town with cranes and ships — about 15% from the left, 77% from the top.
31. Yilan: a small town on the plain — about 84% from the left, 18% from the top.
32. Hualien: a small harbour town on the east coast — about 77% from the left, 39% from the top.
33. Taitung: a small town on the south-east coast — about 55% from the left, 73% from the top.
34. Eluanbi lighthouse on the southern tip, a white tower on a rocky cape — about 42% from the left, 97% from the top.
35. Salt pans near Jingzijiao: neat white rectangles on the south-west coast — about 7% from the left, 59% from the top.
36. Gaomei wetland with a row of white wind turbines — about 28% from the left, 30% from the top.
37. Sandiaojiao lighthouse on the north-east cape — about 95% from the left, 11% from the top.

**E. Offshore islands (draw as small islands in the sea, inside the picture)**

38. Guishan Island: a small turtle-shaped volcanic island — about 93% from the left, 16% from the top.
39. Green Island: a small round island with a lighthouse — about 72% from the left, 76% from the top.
40. Orchid Island: a medium island with hills and a few villages — about 74% from the left, 93% from the top.
41. Xiaoliuqiu: a small flat coral island — about 20% from the left, 85% from the top.

**F. Sea life and ships (a few, spread out; in the sea only)**

42. Two old sailing ships in the Taiwan Strait — about 7% from the left, 39% from the top.
43. A small fishing boat with nets near the north coast — about 67% from the left, 2% from the top.
44. A whale spouting, in the Pacific off the east coast — about 95% from the left, 39% from the top.
45. A pod of dolphins jumping — about 86% from the left, 67% from the top.
46. A small sailing boat off the south-east coast — about 63% from the left, 89% from the top.
47. A sea serpent, subtle and old-engraving style, in the far south-east sea — about 91% from the left, 96% from the top.
>
> Keep the area immediately around each town and landmark reasonably clear so that a route line and small icons can be added on top later. Absolutely NO text, NO place names, NO numbers, NO red markers, NO route line, NO border or frame.

**驗收：** 海岸線跟草圖一致（尤其北部、恆春半島、花東海岸）；沒有文字、沒有邊框、海一直延伸到圖的四邊；五座山、縱谷、四個離島都在。位置偏很多就在同一個對話說 "move object N toward ..."，或整張重畫。

存檔：`generated/real_map_base.png`（有多張就挑一張，再告訴我尺寸）。

## 修圖：把畫錯位置的湖與風車移到高美濕地

第一張圖裡，高美濕地（風車＋濕地）被畫在雲林、嘉義外海邊（約距左 12–26%、距上 44–55%），變成一大片淡藍色湖。高美應該在台中清水海邊（約距左 28%、距上 30%，海岸線上）。

用法：同一個對話，上傳目前的 `generated/real_map_base.png`，貼下面的文字。

> Edit the uploaded map, keeping everything else exactly the same (coastline, style, colours, all other objects and positions). Two changes only: (1) In the area about 12–26% from the left and 44–55% from the top, there is a large pale-blue lagoon with white wind turbines. Remove it completely and fill that area with the same flat patchwork of rice fields, fish ponds and small villages as the surrounding plain, so it blends in with no visible edit. (2) At the coast about 28% from the left and 30% from the top (central west coast, a little north of the middle of the island), draw a small tidal-flat wetland with a few shallow pools and a row of five small white wind turbines standing on the shore. Keep it small, no bigger than a quarter of the removed lagoon. No text, no frame.

存檔後覆蓋 `generated/real_map_base.png`，我再重新匯出並用截圖確認。
