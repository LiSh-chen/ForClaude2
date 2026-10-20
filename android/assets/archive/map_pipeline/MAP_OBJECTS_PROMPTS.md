# 真實世界地圖：物件素材（給 Gemini 的提示詞）

物件（景點地標、路上小景點、當地特色）一共 **4 張**，每張 24 個，6 欄×4 列。
位置全部由我按真實座標放，所以每個小圖只要畫好「樣子」，不需要管它在哪裡。

## 通則

1. 每個對話上傳兩張**風格參考圖**：`previews/2_compass.jpg` 和 `generated/real_map_base.png`。
2. 比例選 **3:2 橫式**，解析度選最高。每個小圖最後在螢幕上只有約 100 像素大，所以一格 300 像素就夠。
3. **背景純白**，格子之間留足空白，不要地面陰影、邊框、格線、文字、編號。（我用「正片疊底」把白色去掉，不用去背。）
4. 格子由左到右、由上到下；每格大小一致，主體置中，互不重疊，主體佔格子約 70%。
5. 一次畫 24 個，Gemini 偶爾會漏畫或畫錯。收到後我會逐格檢查，錯的那幾個再補畫（我會告訴你是哪幾格）。

### 風格前綴（每個提示詞前面都貼）

> Antique atlas illustration in the same hand-drawn style as the two uploaded reference images: fine dark-brown pen-and-ink line work with a light watercolour wash in muted greens, ochres, browns and soft blues, slightly imperfect lines, gentle paper-like grain, no digital gloss, no neon colours, no 3D rendering. Seen from a slightly raised top-down view like an old pictorial map. Plain pure white background. No text, no letters, no numbers, no frames, no ground shadows.

## O1　景點地標（1–24）

```
A sheet of 24 separate pictorial map icons, 6 columns by 4 rows, in reading order, each centred in its own equal cell with generous white space around it. Each icon shows the subject as a small illustration with a tiny irregular patch of watercolour ground under it, like a drawing on an antique map, recognisable when printed about 2 cm wide. Same scale, same slightly raised viewing angle and same style for every icon. Do not draw any text, signs with writing, numbers or borders.
The icons, in order:
1. 台北101: the tall segmented tower with stacked pagoda-like sections, small trees at its base
2. 基隆廟口: a temple (Dianji Temple) gate with rows of small lantern-lit food stalls in front, a harbour behind
3. 九份: a steep hillside of old teahouses with red lanterns and stone steps down to the sea
4. 福隆: a wide sandy beach where a river meets the ocean, a long sandbar, small umbrellas
5. 三貂角: a white lighthouse standing on a green headland above the sea
6. 頭城老街: a row of old brick shophouses with arcaded fronts and baroque-style facades
7. 傳統藝術中心: a traditional Chinese courtyard complex with curved roofs beside a canal, a small bridge
8. 蘇澳冷泉: a round stone spring pool with bubbles rising and a few people soaking, cool blue water
9. 七星潭: a crescent-shaped bay with a pebble beach, a few kites in the air
10. 松園別館: a two-storey Japanese-era wooden villa with a tiled roof among old pine trees
11. 林田山: a wooden forestry village on a hillside with a small logging railway and stacked logs
12. 瑞穗北回歸線標: a small white stone monument with a ring-shaped top on a hill (Tropic of Cancer marker)
13. 玉里: a steaming bowl of noodles in front of a small railway station building
14. 池上: a green rice field with a small railway station and a lunch box (bento) in the foreground
15. 關山: a bicycle on a path circling a rice field, mountains behind
16. 鹿野高台: a hot-air balloon rising over green tea terraces, a lookout deck
17. 史前博物館: a modern museum building with a curved glass roof and a stone-age mural wall
18. 太麻里: a coast with the first golden light of sunrise over the Pacific, a small seaside shrine
19. 壽卡: a mountain pass road winding up between steep forested slopes, a small roadside shrine
20. 車城福安宮: a Taiwanese Earth God temple with a curved swallowtail roof and a very large golden statue of the Earth God beside it
21. 恆春古城: a stone city gate with a tower and thick curved walls, a quiet street
22. 鵝鑾鼻: a white lighthouse on a grassy coral-limestone cape, the sea beyond
23. 貓鼻頭: a rock on the shore shaped like a crouching cat, surrounded by waves
24. 海生館: a modern aquarium building with a large whale sculpture and waves
```

存檔：`generated/objects_o1.png`

## O2　景點地標（25–46）

```
A sheet of 22 separate pictorial map icons, 6 columns by 4 rows, in reading order, each centred in its own equal cell with generous white space around it. Each icon shows the subject as a small illustration with a tiny irregular patch of watercolour ground under it, like a drawing on an antique map, recognisable when printed about 2 cm wide. Same scale, same slightly raised viewing angle and same style for every icon. Do not draw any text, signs with writing, numbers or borders. The last 2 cell(s) are left empty (plain white).
The icons, in order:
1. 枋寮: a small railway station with a steam train passing, the sea in the background
2. 東港: a temple with a large decorated wooden ceremonial ship (Wangchuan) on a harbour shore
3. 高雄駁二: old brick warehouses turned into an art area with colourful art installations, a harbour crane
4. 蓮池潭: a lake with the Dragon and Tiger Pagodas on the shore, a zigzag bridge
5. 橋頭糖廠: an old sugar mill with a tall chimney, a small sugarcane train and cane fields
6. 安平古堡: a red-brick fort with a watchtower and thick walls, old banyan trees
7. 井仔腳鹽田: tile-floored salt pans in a grid, small white salt pyramids, the sea edge
8. 布袋: a fishing harbour with a ferry and fishing boats, a small lighthouse
9. 朴子配天宮: a large Mazu temple with an ornate multi-layered roof, red lanterns
10. 水上北回歸線標: a small white monument with a ring symbol on flat farmland (Tropic of Cancer marker)
11. 檜意森活村: Japanese-era wooden houses with tiled roofs among tall cypress trees
12. 西螺大橋: a very long steel truss bridge crossing a wide braided river
13. 鹿港老街: a very narrow alley between tall red-brick walls with a tiny strip of sky
14. 彰化扇形車庫: a semicircular steam-locomotive depot with a locomotive on a turntable
15. 高美濕地: a row of white wind turbines on a tidal flat at sunset, shallow pools reflecting the sky
16. 大甲鎮瀾宮: a large Mazu temple with a procession of small figures carrying a palanquin
17. 通霄精鹽廠: a salt factory building with big white salt piles and a small train
18. 新竹城隍廟: a large city-god temple with a colourful carved roof and a courtyard
19. 桃園神社: a Japanese shrine with a torii gate, stone lanterns and a stair path through trees
20. 十三行博物館: a modern dark-roofed museum with a long ribbon-shaped structure beside the Tamsui river mouth
21. 迪化街: an old merchant street with arched baroque facades, hanging dried goods and lanterns
22. 回到台北: a cosy small house with a lantern light and a front garden (the end of the journey)
```

存檔：`generated/objects_o2.png`

## O3　路上小景點（24 個）

```
A sheet of 24 separate pictorial map icons, 6 columns by 4 rows, in reading order, each centred in its own equal cell with generous white space around it. Each icon shows the subject as a small illustration with a tiny irregular patch of watercolour ground under it, like a drawing on an antique map, recognisable when printed about 2 cm wide. Same scale, same slightly raised viewing angle and same style for every icon. Do not draw any text, signs with writing, numbers or borders.
The icons, in order:
1. 和平島公園: a rocky coast with strange wave-eroded stone shapes and a small park
2. 蘭陽博物館: a modern building shaped like a cuesta rock slope, beside the sea
3. 宜蘭酒廠: an old factory building with a tall chimney and large wooden barrels
4. 羅東林業文化園區: a wooden log pond with stacked logs and a small steam train
5. 南方澳漁港: a bridge over a colourful fishing harbour full of boats
6. 羅山瀑布: a high narrow waterfall in a green valley with a small bridge
7. 卑南文化公園: stone slab ruins in a grass park under a big sky
8. 知本溫泉: a steaming outdoor hot spring pool among trees
9. 旗津燈塔: a white lighthouse on a sandy spit at the harbour mouth
10. 愛河: a calm river with small boats and willow trees, bridges
11. 屏東演藝廳: a modern curved building with a green roof
12. 岡山水塔: an old brick water tower among low houses
13. 赤崁樓: a red-brick tower with a curved roof among old trees
14. 奇美博物館: a neoclassical white museum with columns and a large fountain
15. 嘉義公園: a park with a tall shrine-like pavilion and old trees
16. 八卦山大佛: a very large seated Buddha statue on a hill above a city
17. 梧棲漁港: a harbour with many moored fishing boats and a small market
18. 國家歌劇院: a futuristic white curved building with cave-like openings
19. 南寮漁港: a harbour with a long seaside cycle path and small boats
20. 中壢藝術館: a small modern brick arts hall with a courtyard
21. 竹圍漁港: a small harbour with an old wharf and fishing nets
22. 板橋林家花園: a classical Chinese garden with a pavilion, a pond and a curved bridge
23. 淡水紅毛城: a red fort with a flag on a hill by the river mouth
24. 西門紅樓: an octagonal red-brick Japanese-era building with a small plaza
```

存檔：`generated/objects_o3.png`

## O4　沿路當地特色（24 個）

```
A sheet of 24 separate pictorial map icons, 6 columns by 4 rows, in reading order, each centred in its own equal cell with generous white space around it. Each icon shows the subject as a small illustration with a tiny irregular patch of watercolour ground under it, like a drawing on an antique map, recognisable when printed about 2 cm wide. Same scale, same slightly raised viewing angle and same style for every icon. Do not draw any text, signs with writing, numbers or borders.
The icons, in order:
1. Pinglin tea hills: terraced hillsides of tea plantations with neat curved rows and a small tea factory hut
2. Beitou hot springs: steam rising from a valley of hot springs with small bathhouses
3. Tamsui sunset ferry: a small passenger ferry crossing a wide river mouth at sunset
4. Datun-side duck pond area (Taoyuan ponds): many small round irrigation ponds scattered in farmland
5. Lanyang plain paddies: a patchwork of green rice paddies with a water buffalo and an egret
6. Ruisui ranch: a green hillside pasture with a group of cows
7. Chishang paddies: wide golden rice fields with a long straight farm road and a lone tree
8. Linbian wax-apple farms: orchards of wax-apple trees with netting and a roadside fruit stand
9. Kenting beach dunes: a white sand beach with dunes, windswept grass and a few palms
10. Qigu lagoon oyster racks: oyster-farm racks in a shallow lagoon with a few small boats
11. Dongshi oyster racks: oyster-farm racks on a tidal flat with fishermen
12. Mailiao wind turbines: a row of white wind turbines along a coastal dike
13. Wanggong oyster fields: tidal flats with oyster racks and a small fishing hamlet
14. Tianwei flower garden: rows of colourful flower fields and greenhouses
15. Yuanli grass-weaving fields: low rush-grass fields and a small weaving house
16. Miaoli coastal wind turbines: a row of wind turbines on a coastal plain
17. Hsinchu windy coast: a windswept coast with wind-bent pines and a cycle path
18. Water buffalo: a water buffalo standing in a flooded rice paddy with an egret
19. Pineapple field: a pineapple field with rows of spiky plants
20. Lotus pond: a lotus pond with pink flowers and large round leaves
21. Roadside Earth God shrine: a small roadside Earth God shrine with incense and a banyan tree
22. Railway bridge: a train crossing a small iron bridge over a river
23. Temple fair: a small roadside temple fair with colourful banners and lanterns
24. Betel-nut stand: a roadside betel-nut stand with a tiny shop and tall palms
```

存檔：`generated/objects_o4.png`

## 交給我之後

我會把每一格切開、白色當透明，依真實座標放到地圖上，先給你看 3 到 4 個地點的效果。
