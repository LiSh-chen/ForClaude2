# 真實世界地圖：把物件直接畫進地圖（分三段、順便提高畫質）

做法：把整張底圖切成上、中、下三段（各 3:2，彼此有重疊），每一段我已經標好「哪個位置要畫什麼」。
Gemini 這一步**同時**做兩件事：① 把這一段放大、畫得更精細；② 把物件畫進地圖裡，跟地形融為一體（沒有框、沒有白底、沒有貼圖感）。
位置、大小都由我先定好（粉紅圈），所以不會再有「物件跟地圖分離」或位置歪掉的問題。

## 每一段都要上傳的圖

- 圖 A：`mapguide/bands/bandN_clean.png`（乾淨的底圖，這是要被保留、放大、加料的那張）
- 圖 B：`mapguide/bands/bandN_guide.png`（同一張，但有粉紅色編號圓圈，只用來看「位置與大小」）

比例選 **3:2 橫式**，解析度選最高。每段一個新對話，三段的提示詞前半相同。

## 通用提示詞（每段都貼，後面接該段的物件清單）

```
I upload two images of the same map section. Image A is an antique hand-drawn atlas painting of part of Taiwan (the ground only). Image B is the SAME picture with numbered pink circles that show where objects must be painted.
Task: output a refined, higher-resolution version of image A at the same framing (3:2, the highest resolution you can), in exactly the same hand-drawn style (fine dark-brown pen line work, light watercolour wash, paper grain).
1. Keep EXACTLY the same coastline, rivers, mountains, fields and colours, and the same position of every feature. Do not move, crop, rotate or re-compose anything. Add finer detail only.
2. At each pink circle, paint the listed object as part of the map itself: drawn onto the terrain, sized to fill its circle (about 90% of its width), same slightly raised pictorial viewpoint, same line work and colour wash. Let its edges melt into the surrounding ground (grass, sand, water, hills) so that it looks painted in the same sitting as the map. NO frame, NO white background, NO separate sticker or vignette, NO drop shadow.
3. The final image must contain NO pink circles, NO numbers and NO text of any kind. Do not add any object that is not listed. Leave the parts of the map without a circle as they are (plain terrain, no extra buildings).
```

## 第 1 段（38 個物件）

```
The objects, by circle number:
1. Tamsui sunset ferry: a small passenger ferry crossing a wide river mouth at sunset  (footprint about 63 px wide)
2. 淡水紅毛城: a red fort with a flag on a hill by the river mouth  (footprint about 54 px wide)
3. 和平島公園: a rocky coast with strange wave-eroded stone shapes and a small park  (footprint about 54 px wide)
4. Beitou hot springs: steam rising from a valley of hot springs with small bathhouses  (footprint about 63 px wide)
5. 十三行博物館: a modern dark-roofed museum with a long ribbon-shaped structure beside the Tamsui river mouth  (footprint about 72 px wide)
6. 基隆廟口: a temple (Dianji Temple) gate with rows of small lantern-lit food stalls in front, a harbour behind  (footprint about 72 px wide)
7. 竹圍漁港: a small harbour with an old wharf and fishing nets  (footprint about 54 px wide)
8. 九份: a steep hillside of old teahouses with red lanterns and stone steps down to the sea  (footprint about 72 px wide)
9. 迪化街: an old merchant street with arched baroque facades, hanging dried goods and lanterns  (footprint about 72 px wide)
10. 西門紅樓: an octagonal red-brick Japanese-era building with a small plaza  (footprint about 54 px wide)
11. Datun-side duck pond area (Taoyuan ponds): many small round irrigation ponds scattered in farmland  (footprint about 63 px wide)
12. 回到台北: a cosy small house with a lantern light and a front garden (the end of the journey)  (footprint about 72 px wide)
13. 台北101: the tall segmented tower with stacked pagoda-like sections, small trees at its base  (footprint about 72 px wide)
14. 福隆: a wide sandy beach where a river meets the ocean, a long sandbar, small umbrellas  (footprint about 72 px wide)
15. 板橋林家花園: a classical Chinese garden with a pavilion, a pond and a curved bridge  (footprint about 54 px wide)
16. 三貂角: a white lighthouse standing on a green headland above the sea  (footprint about 72 px wide)
17. 桃園神社: a Japanese shrine with a torii gate, stone lanterns and a stair path through trees  (footprint about 72 px wide)
18. 中壢藝術館: a small modern brick arts hall with a courtyard  (footprint about 54 px wide)
19. Pinglin tea hills: terraced hillsides of tea plantations with neat curved rows and a small tea factory hut  (footprint about 63 px wide)
20. 蘭陽博物館: a modern building shaped like a cuesta rock slope, beside the sea  (footprint about 54 px wide)
21. 南寮漁港: a harbour with a long seaside cycle path and small boats  (footprint about 54 px wide)
22. 頭城老街: a row of old brick shophouses with arcaded fronts and baroque-style facades  (footprint about 72 px wide)
23. Hsinchu windy coast: a windswept coast with wind-bent pines and a cycle path  (footprint about 63 px wide)
24. 新竹城隍廟: a large city-god temple with a colourful carved roof and a courtyard  (footprint about 72 px wide)
25. 宜蘭酒廠: an old factory building with a tall chimney and large wooden barrels  (footprint about 54 px wide)
26. Lanyang plain paddies: a patchwork of green rice paddies with a water buffalo and an egret  (footprint about 63 px wide)
27. 傳統藝術中心: a traditional Chinese courtyard complex with curved roofs beside a canal, a small bridge  (footprint about 72 px wide)
28. Roadside Earth God shrine: a small roadside Earth God shrine with incense and a banyan tree  (footprint about 45 px wide)
29. 羅東林業文化園區: a wooden log pond with stacked logs and a small steam train  (footprint about 54 px wide)
30. Miaoli coastal wind turbines: a row of wind turbines on a coastal plain  (footprint about 63 px wide)
31. 蘇澳冷泉: a round stone spring pool with bubbles rising and a few people soaking, cool blue water  (footprint about 72 px wide)
32. 南方澳漁港: a bridge over a colourful fishing harbour full of boats  (footprint about 54 px wide)
33. 通霄精鹽廠: a salt factory building with big white salt piles and a small train  (footprint about 72 px wide)
34. Yuanli grass-weaving fields: low rush-grass fields and a small weaving house  (footprint about 63 px wide)
35. 大甲鎮瀾宮: a large Mazu temple with a procession of small figures carrying a palanquin  (footprint about 72 px wide)
36. 高美濕地: a row of white wind turbines on a tidal flat at sunset, shallow pools reflecting the sky  (footprint about 72 px wide)
37. Railway bridge: a train crossing a small iron bridge over a river  (footprint about 45 px wide)
38. 梧棲漁港: a harbour with many moored fishing boats and a small market  (footprint about 54 px wide)
```

存檔：`generated/bands/band1.png`

## 第 2 段（29 個物件）

```
The objects, by circle number:
1. 國家歌劇院: a futuristic white curved building with cave-like openings  (footprint about 54 px wide)
2. 彰化扇形車庫: a semicircular steam-locomotive depot with a locomotive on a turntable  (footprint about 72 px wide)
3. 八卦山大佛: a very large seated Buddha statue on a hill above a city  (footprint about 54 px wide)
4. 鹿港老街: a very narrow alley between tall red-brick walls with a tiny strip of sky  (footprint about 72 px wide)
5. 七星潭: a crescent-shaped bay with a pebble beach, a few kites in the air  (footprint about 72 px wide)
6. 松園別館: a two-storey Japanese-era wooden villa with a tiled roof among old pine trees  (footprint about 72 px wide)
7. Wanggong oyster fields: tidal flats with oyster racks and a small fishing hamlet  (footprint about 63 px wide)
8. Tianwei flower garden: rows of colourful flower fields and greenhouses  (footprint about 63 px wide)
9. 西螺大橋: a very long steel truss bridge crossing a wide braided river  (footprint about 72 px wide)
10. Mailiao wind turbines: a row of white wind turbines along a coastal dike  (footprint about 63 px wide)
11. 林田山: a wooden forestry village on a hillside with a small logging railway and stacked logs  (footprint about 72 px wide)
12. Temple fair: a small roadside temple fair with colourful banners and lanterns  (footprint about 45 px wide)
13. Pineapple field: a pineapple field with rows of spiky plants  (footprint about 45 px wide)
14. Ruisui ranch: a green hillside pasture with a group of cows  (footprint about 63 px wide)
15. 檜意森活村: Japanese-era wooden houses with tiled roofs among tall cypress trees  (footprint about 72 px wide)
16. 嘉義公園: a park with a tall shrine-like pavilion and old trees  (footprint about 54 px wide)
17. 朴子配天宮: a large Mazu temple with an ornate multi-layered roof, red lanterns  (footprint about 72 px wide)
18. 瑞穗北回歸線標: a small white stone monument with a ring-shaped top on a hill (Tropic of Cancer marker)  (footprint about 72 px wide)
19. Dongshi oyster racks: oyster-farm racks on a tidal flat with fishermen  (footprint about 63 px wide)
20. 水上北回歸線標: a small white monument with a ring symbol on flat farmland (Tropic of Cancer marker)  (footprint about 72 px wide)
21. 布袋: a fishing harbour with a ferry and fishing boats, a small lighthouse  (footprint about 72 px wide)
22. 玉里: a steaming bowl of noodles in front of a small railway station building  (footprint about 72 px wide)
23. Lotus pond: a lotus pond with pink flowers and large round leaves  (footprint about 45 px wide)
24. Water buffalo: a water buffalo standing in a flooded rice paddy with an egret  (footprint about 45 px wide)
25. 井仔腳鹽田: tile-floored salt pans in a grid, small white salt pyramids, the sea edge  (footprint about 72 px wide)
26. 羅山瀑布: a high narrow waterfall in a green valley with a small bridge  (footprint about 54 px wide)
27. Chishang paddies: wide golden rice fields with a long straight farm road and a lone tree  (footprint about 63 px wide)
28. Qigu lagoon oyster racks: oyster-farm racks in a shallow lagoon with a few small boats  (footprint about 63 px wide)
29. 池上: a green rice field with a small railway station and a lunch box (bento) in the foreground  (footprint about 72 px wide)
```

存檔：`generated/bands/band2.png`

## 第 3 段（27 個物件）

```
The objects, by circle number:
1. 關山: a bicycle on a path circling a rice field, mountains behind  (footprint about 72 px wide)
2. 安平古堡: a red-brick fort with a watchtower and thick walls, old banyan trees  (footprint about 72 px wide)
3. 赤崁樓: a red-brick tower with a curved roof among old trees  (footprint about 54 px wide)
4. 奇美博物館: a neoclassical white museum with columns and a large fountain  (footprint about 54 px wide)
5. 鹿野高台: a hot-air balloon rising over green tea terraces, a lookout deck  (footprint about 72 px wide)
6. 岡山水塔: an old brick water tower among low houses  (footprint about 54 px wide)
7. 卑南文化公園: stone slab ruins in a grass park under a big sky  (footprint about 54 px wide)
8. 橋頭糖廠: an old sugar mill with a tall chimney, a small sugarcane train and cane fields  (footprint about 72 px wide)
9. 史前博物館: a modern museum building with a curved glass roof and a stone-age mural wall  (footprint about 72 px wide)
10. 蓮池潭: a lake with the Dragon and Tiger Pagodas on the shore, a zigzag bridge  (footprint about 72 px wide)
11. 知本溫泉: a steaming outdoor hot spring pool among trees  (footprint about 54 px wide)
12. 屏東演藝廳: a modern curved building with a green roof  (footprint about 54 px wide)
13. 高雄駁二: old brick warehouses turned into an art area with colourful art installations, a harbour crane  (footprint about 72 px wide)
14. 太麻里: a coast with the first golden light of sunrise over the Pacific, a small seaside shrine  (footprint about 72 px wide)
15. 愛河: a calm river with small boats and willow trees, bridges  (footprint about 54 px wide)
16. 旗津燈塔: a white lighthouse on a sandy spit at the harbour mouth  (footprint about 54 px wide)
17. 東港: a temple with a large decorated wooden ceremonial ship (Wangchuan) on a harbour shore  (footprint about 72 px wide)
18. Linbian wax-apple farms: orchards of wax-apple trees with netting and a roadside fruit stand  (footprint about 63 px wide)
19. Betel-nut stand: a roadside betel-nut stand with a tiny shop and tall palms  (footprint about 45 px wide)
20. 枋寮: a small railway station with a steam train passing, the sea in the background  (footprint about 72 px wide)
21. 壽卡: a mountain pass road winding up between steep forested slopes, a small roadside shrine  (footprint about 72 px wide)
22. 車城福安宮: a Taiwanese Earth God temple with a curved swallowtail roof and a very large golden statue of the Earth God beside it  (footprint about 72 px wide)
23. 海生館: a modern aquarium building with a large whale sculpture and waves  (footprint about 72 px wide)
24. 恆春古城: a stone city gate with a tower and thick curved walls, a quiet street  (footprint about 72 px wide)
25. Kenting beach dunes: a white sand beach with dunes, windswept grass and a few palms  (footprint about 63 px wide)
26. 貓鼻頭: a rock on the shore shaped like a crouching cat, surrounded by waves  (footprint about 72 px wide)
27. 鵝鑾鼻: a white lighthouse on a grassy coral-limestone cape, the sea beyond  (footprint about 72 px wide)
```

存檔：`generated/bands/band3.png`

## 交給我之後

我把三段按重疊區對齊、在沒有物件的接縫線上漸變合併，成為一張大圖；你可以先看合併圖，再看遊戲裡的畫面。
如果某個物件畫歪、漏畫，我會指出編號，請你只對那一段再生成一次。
