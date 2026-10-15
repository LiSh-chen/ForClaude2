# 真實世界地圖：素材圖（給 Gemini 的提示詞）

做法：地形、河流、道路、海岸線、森林和農田的**位置**由真實資料決定（衛星高程、OpenStreetMap），
Gemini 只負責畫**素材**（山、樹、農田、地標小圖……），我把素材按真實位置貼成整張地圖。
所以風格會和你選的 A 風格（羊皮紙＋鋼筆＋淡水彩）一致，放大也夠清楚，景點位置也一定對。

## 通則（每個對話都一樣）

1. 每個對話都上傳兩張**風格參考圖**：`previews/2_compass.jpg` 和 `generated/real_map_base.png`。
2. 先貼「風格前綴」，再貼該張的提示詞。
3. **背景一律純白**（#FFFFFF），素材之間留足空白，不要地面陰影、不要邊框、不要格線、不要文字、不要編號。
   （我會用「正片疊底」把白色去掉，所以素材的線條與水彩會自然融進羊皮紙，不用去背。）
4. 格子數量與順序以提示詞為準，由左到右、由上到下。每格大小一致，素材置中，互不重疊。
5. 解析度選最高；比例選 4:3 橫式（4 欄×3 列）或 3:4 直式（3 欄×4 列），提示詞會寫。
6. 存檔名依各張的「存檔」欄，放在 `assets/generated/`，一次傳給我一組即可。

### 風格前綴（每個提示詞前面都貼）

> Antique atlas illustration in the same hand-drawn style as the two uploaded reference images: fine dark-brown pen-and-ink line work with a light watercolour wash in muted greens, ochres, browns and soft blues, slightly imperfect lines, gentle paper-like grain, no digital gloss, no neon colours, no 3D rendering. Seen from a slightly raised top-down view like an old pictorial map. Plain pure white background. No text, no letters, no numbers, no frames, no ground shadows.

---

## A 組：地形素材（3 張，每張 36 個以內）

### A1　山與丘陵（4 欄×3 列，共 12 個）

> A sheet of 12 separate hill and mountain sprites for an old pictorial map, 4 columns by 3 rows, each centred in its own equal cell. Row 1: (1) a low rounded green hill with a few tiny trees; (2) a group of three gentle green hills; (3) a long soft ridge with pen hatching on the shaded side; (4) a steep forested ridge seen from the side. Row 2: (5) a tall rugged mountain with dark green lower slopes and grey rock above; (6) a pair of rugged peaks sharing a saddle; (7) a snow-capped peak with white snow patches and bare rock; (8) a large snow-capped mountain massif with several summits. Row 3: (9) a steep sea cliff with layered rock striations; (10) a small volcanic cone with a flat crater top; (11) a terraced hillside with tea-plantation rows; (12) a grassy high ridge with scattered rocks. Every sprite is drawn like the mountains in the reference map: pen hatching for shaded slopes, soft watercolour wash for the lit side, slightly wider than tall.

存檔：`generated/sprite_hills.png`

### A2　樹與植物（4 欄×3 列，共 12 個）

> A sheet of 12 separate vegetation sprites for an old pictorial map, 4 columns by 3 rows, each centred in its own equal cell, each sprite a small cluster that can be tiled in a forest. Row 1: (1) a small cluster of three round broadleaf trees; (2) a larger cluster of five dense broadleaf trees; (3) a cluster of tall pointed conifers (cypress / hinoki); (4) a mixed cluster of conifers and broadleaf trees. Row 2: (5) a bamboo grove with slim curved stalks; (6) betel-nut palm trees, tall thin trunks with feathery crowns, a group of four; (7) coconut-like tropical palms, a group of three; (8) mangrove trees with roots rising from shallow water. Row 3: (9) a single large banyan tree with aerial roots; (10) a group of banana plants; (11) a reed marsh with cattails and two egrets; (12) a small group of flowering camphor trees with pale blossoms. Same pen-and-wash style as the reference map.

存檔：`generated/sprite_plants.png`

### A3　農田、海岸、水上（4 欄×3 列，共 12 個）

> A sheet of 12 separate landscape sprites for an old pictorial map, 4 columns by 3 rows, each centred in its own equal cell. Row 1: (1) a patchwork of green rice paddies with thin dike lines, about 4 small fields; (2) a patchwork of golden ripe rice fields with a winding farm path; (3) a group of fish ponds with raised banks, pale blue water; (4) salt pans: neat white rectangular tiled pans with small cone-shaped salt heaps. Row 2: (5) oyster-farm racks in shallow water seen from above, rows of poles; (6) a sandy beach strip with small dunes and drift-wood; (7) a rocky shore with dark rocks and white foam; (8) a small fishing harbour with a short breakwater and three moored boats. Row 3: (9) a small wooden fishing boat with a sail; (10) a larger old sailing ship; (11) a patch of wave marks: fine horizontal pen waves in groups of three; (12) a school of jumping dolphins with small splashes. Same pen-and-wash style as the reference map.

存檔：`generated/sprite_land.png`

---

## B 組：景點地標小圖（46 個，4 張，每張 12 個，最後一張 10 個）

每個景點在地圖上都會有一張對應的小圖，位置就是該景點的真實座標。
請把每個小圖畫成**像古地圖上的圖示**：一個建築或景物為主體，周圍一小塊淡水彩地面（不要塊狀底色），
大小一致，主體約佔格子的 70%。

統一提示詞（每張都用，只換編號清單）：

> A sheet of 12 separate pictorial map icons of famous places in Taiwan, 4 columns by 3 rows, in reading order, each centred in its own equal cell. Each icon shows the place's most recognisable building or scene as a small illustration with a tiny irregular patch of watercolour ground under it, like a landmark drawn on an antique map, with enough detail to be recognised when printed about 2 cm wide. The 12 icons, in order: [list below]. Keep the same scale, viewing angle (slightly raised) and style for all icons. Do not draw any text or signs with writing.

### B1（景點 1–12）

1. Taipei 101: the tall segmented tower with stacked pagoda-like sections, small trees at its base
2. Keelung Miaokou night market: a temple (Dianji Temple) gate with rows of small lantern-lit food stalls in front
3. Jiufen: a steep hillside of old teahouses with red lanterns and stone steps down to the sea
4. Fulong: a wide sandy beach where a river meets the ocean, a long sandbar, small umbrellas
5. Sandiaojiao: a white lighthouse standing on a green headland above the sea
6. Toucheng Old Street: a row of old brick shophouses with arcaded fronts and baroque-style decorated facades
7. National Center for Traditional Arts: a traditional Chinese courtyard complex with curved roofs beside a canal, a small bridge
8. Su'ao cold spring: a round stone spring pool with bubbles rising and a few people soaking, steam-free, cool blue water
9. Qixingtan: a crescent-shaped bay with a pebble beach and calm water, a few kites in the air
10. Pine Garden (Hualien): a two-storey Japanese-era wooden villa with a tiled roof among old pine trees
11. Lintienshan: a wooden forestry village on a hillside with a small logging railway line and stacked logs
12. Ruisui Tropic of Cancer marker: a small white stone monument on a hill with a ring-shaped top, mountains behind

存檔：`generated/icons_b1.png`

### B2（景點 13–24）

13. Yuli: a steaming bowl of noodles in front of a small railway station building
14. Chishang: a green rice field with a small railway station and a lunch box (bento) in the foreground
15. Guanshan: a bicycle on a path circling a rice field, mountains behind
16. Luye highland: a hot-air balloon rising over green tea terraces, a lookout deck
17. National Museum of Prehistory (Taitung): a modern museum building with a curved glass roof and a stone-age mural wall
18. Taimali: a coast with the first golden light of sunrise over the Pacific, a small seaside shrine, sun rays
19. Shouka: a mountain pass road winding up between steep forested slopes, a small roadside shrine
20. Checheng Fu'an Temple: a Taiwanese Earth God temple with a curved swallowtail roof and a very large golden statue of the Earth God beside it
21. Hengchun old town: a stone city gate with a tower and thick curved walls, a quiet street
22. Eluanbi: a white lighthouse on a grassy coral-limestone cape, the sea beyond
23. Maobitou: a rock on the shore shaped like a crouching cat, surrounded by waves
24. National Museum of Marine Biology and Aquarium: a modern aquarium building with a large whale sculpture and waves

存檔：`generated/icons_b2.png`

### B3（景點 25–36）

25. Fangliao: a small railway station with a steam train passing, sea in the background
26. Donggang: a temple with a large decorated wooden ceremonial ship (Wangchuan) on a harbour shore
27. Pier-2 Art Center (Kaohsiung): old brick warehouses turned into an art area, with a few colourful art installations, a harbour crane
28. Lotus Pond: a lake with the Dragon and Tiger Pagodas on the shore, a zigzag bridge
29. Qiaotou Sugar Refinery: an old sugar mill with a tall chimney, a small sugarcane train and cane fields
30. Anping Fort (Zeelandia): a red-brick fort with a watchtower and thick walls, old banyan trees
31. Jingzijiao salt pans: tile-floored salt pans in a grid, small white salt pyramids, a sea edge
32. Budai: a fishing harbour with a ferry and fishing boats, a small lighthouse
33. Peitian Temple (Puzi): a large Mazu temple with an ornate multi-layered roof, red lanterns
34. Shuishang Tropic of Cancer marker: a small white monument with a ring symbol on flat farmland
35. Hinoki Village (Chiayi): Japanese-era wooden houses with tiled roofs among tall cypress trees
36. Xiluo Bridge: a very long steel truss bridge crossing a wide braided river

存檔：`generated/icons_b3.png`

### B4（景點 37–46，最後兩格留空白）

37. Lukang Old Street: a very narrow alley between tall red-brick walls with a tiny strip of sky
38. Changhua Roundhouse: a semicircular steam-locomotive depot with a locomotive on a turntable
39. Gaomei Wetlands: a row of white wind turbines on a tidal flat at sunset, shallow pools reflecting the sky
40. Dajia Zhenlan Temple: a large Mazu temple with a procession of small figures carrying a palanquin
41. Tongxiao Salt Works: a salt factory building with big white salt piles and a small train
42. Hsinchu City God Temple: a large city-god temple with a colourful carved roof and a courtyard
43. Taoyuan Shrine: a Japanese shrine with a torii gate, stone lanterns and a stair path through trees
44. Shihsanhang Museum: a modern dark-roofed museum with a long ribbon-shaped structure beside the Tamsui river mouth
45. Dihua Street: an old merchant street with arched baroque facades and hanging dried goods, lanterns
46. Home in Taipei: a cosy small house with a lantern light and a front garden (the finish of the journey)
(cells 11 and 12 left empty)

存檔：`generated/icons_b4.png`

---

## C 組：路上小景點（24 個，2 張，每張 12 個）

這些是「不是景點卡、但沿路看得到」的地方，畫得比 B 組小一點、簡單一點。
統一提示詞與 B 組相同，只是改成「Each icon is simpler and smaller than a landmark, like a minor sight on the way」。

### C1（1–12）

1. Heping Island Park: a rocky coast with strange wave-eroded stone shapes and a small park
2. Lanyang Museum: a modern building shaped like a cuesta rock slope, beside the sea
3. Yilan distillery: an old factory building with a tall chimney and large wooden barrels
4. Luodong Forestry Culture Park: a wooden log pond with stacked logs and a small steam train
5. Nanfang'ao fishing harbour: a bridge over a colourful fishing harbour full of boats
6. Luoshan waterfall: a high narrow waterfall in a green valley with a small bridge
7. Beinan Cultural Park: stone slab ruins in a grass park under a big sky
8. Zhiben hot spring: a steaming outdoor spring pool among trees
9. Cijin lighthouse: a white lighthouse on a sandy spit at the harbour mouth
10. Love River: a calm river with small boats and willow trees, bridges
11. Pingtung Performing Arts Center: a modern curved building with a green roof
12. Gangshan water tower: an old brick water tower among low houses

存檔：`generated/icons_c1.png`

### C2（13–24）

13. Chihkan Tower: a red-brick tower with a curved roof among old trees
14. Chimei Museum: a neoclassical white museum with columns and a large fountain
15. Chiayi Park: a park with a tall shrine-like pavilion and old trees
16. Baguashan Buddha: a very large seated Buddha statue on a hill above a city
17. Wuqi fishing harbour: a harbour with many moored fishing boats and a small market
18. National Taichung Theater: a futuristic white curved building with cave-like openings
19. Nanliao fishing harbour: a harbour with a long seaside cycle path and small boats
20. Zhongli Arts Center: a small modern brick arts hall with a courtyard
21. Zhuwei fishing harbour: a small harbour with an old wharf and fishing nets
22. Lin Family Garden: a classical Chinese garden with a pavilion, a pond and a curved bridge
23. Hongmaocheng (Fort San Domingo): a red fort with a flag on a hill by the river mouth
24. Red House Theater (Ximen): an octagonal red-brick Japanese-era building with a small plaza

存檔：`generated/icons_c2.png`

---

## D 組：沿路的當地特色（不是景點，但畫在地圖上更有味道）

我會按位置把這些貼在對應地方（例如檳榔樹在南部、蚵架在彰化外海、茶園在北部丘陵、風車在苗栗海邊……）。

### D1（4 欄×3 列，共 12 個）

> A sheet of 12 separate small scenes of everyday Taiwan countryside for an old pictorial map, 4 columns by 3 rows, each centred in its own equal cell. (1) a tea plantation on a hillside with neat curved rows and a small tea factory hut; (2) a water buffalo standing in a flooded rice paddy with an egret; (3) a roadside betel-nut stand with a tiny shop and tall palms; (4) a farmer harvesting golden rice with a straw hat; (5) a small row of wind turbines along a seaside dike; (6) a pineapple field with rows of spiky plants; (7) a lotus pond with pink flowers and large round leaves; (8) a small roadside Earth God shrine with incense and a banyan tree; (9) a train crossing a small iron bridge over a river; (10) an old fishing boat on a beach pulled up on sand with nets; (11) a group of cows on a green hillside pasture; (12) a small roadside temple fair with colourful banners and lanterns. Same pen-and-wash style as the reference map.

存檔：`generated/sprite_local.png`

---

## 交給我之後

我會：
1. 把每張素材切開（白色當作透明）。
2. 按真實的地形與位置排出整張地圖（山排在山脊上、樹排在森林裡、農田排在平原上、地標排在景點座標、地方特色排在合適位置）。
3. 先拿 3 到 4 個地點的畫面給你確認風格，再做成整條路線的地圖。

建議的傳送順序：A1 → A2 → A3 → B1 → B2 → B3 → B4 → D1 → C1 → C2，
先傳 A1、A2、A3 就可以先做出一個地點的畫面讓你確認風格，不必等全部完成。
