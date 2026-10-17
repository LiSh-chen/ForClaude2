# 真實世界地圖：21 張切圖細畫（每張 80×53 公里）

目的：把目前的全島圖（含景點物件）放大、細畫，畫質約提高 2.8 倍，並解決「放大後地貌/物件糊掉」的問題。
位置不能動，所以每張都給你**我切好的原圖**，Gemini 只做「同一張圖的精修放大」。

## 每一張的做法

1. 開新對話（每張一個），比例 **3:2 橫式**，解析度選最高。
2. 上傳兩張圖：`tileNN_input.jpg`（要精修的那張）和 `tileNN_locator.jpg`（小圖，粉紅框＝這一張在全島的位置，只給你參考，**不要求 Gemini 畫它**；想省事可以不傳）。
3. 貼下面的「通用提示詞」，後面接該張的「物件清單」。
4. 每張提示詞最後一行（After generating, save…）會要求 Gemini 把成品直接存到你的雲端硬碟 **我的雲端硬碟／AI cooperate**，檔名固定為 `tileNN.png`（NN 是兩位數編號，和輸入檔一致）；我之後從那裡取檔。若 Gemini 沒存成功或檔名不對，請「下載原尺寸」後手動存進同一資料夾並改成這個檔名。

## 通用提示詞（每張都貼）

```
The uploaded image is a section of an antique hand-drawn atlas map of Taiwan (about 80 x 53 km of ground). It is slightly blurry because it was enlarged. Small pictorial illustrations (buildings, harbours, temples, fields, lighthouses, etc.) are already placed on the map, and they still look a little like pasted stickers.
Task: return the SAME image, same framing, same 3:2 ratio, at the highest resolution you can, as a crisp, finely detailed repaint in exactly the same hand-drawn style (fine dark-brown pen line work, light watercolour wash, paper grain).
1. Do NOT change the composition. Every coastline, river, road, field outline, mountain ridge, forest patch, colour area and every illustration must stay in exactly the same position, with the same size and shape. Do not move, crop, rotate, mirror, add or remove anything. Nothing may be re-composed.
2. Add only finer detail: sharper pen lines, finer hatching on slopes, more refined tree and field texture, cleaner coast lines and sea hatching, crisp paper grain.
3. Redraw each illustration with more detail but the same subject (listed below) and let its edges melt into the surrounding ground (grass into grass, sand into sand, water into sea or river), so that it no longer looks like a sticker: no light patch around it, no frame, a very soft contact shadow only.
4. Do not add any text, numbers, labels, frames, scale bars, compasses or watermarks. Do not add buildings, houses or objects that are not already in the image.
5. If something is cut by the image border, keep it cut at the border exactly as it is.
```

## 第 01 張　緯度 24.919–25.4°N，經度 120.599–121.386°E（4 個物件）

```
The illustrations in this section (position in the picture, subject):
- (下右) Datun-side duck pond area (Taoyuan ponds): many small round irrigation ponds scattered in farmland
- (中右) 竹圍漁港: a small harbour with an old wharf and fishing nets
- (下右) 桃園神社: a Japanese shrine with a torii gate, stone lanterns and a stair path through trees
- (下右) 中壢藝術館: a small modern brick arts hall with a courtyard

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile01.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 02 張　緯度 24.919–25.4°N，經度 121.268–122.055°E（16 個物件）

```
The illustrations in this section (position in the picture, subject):
- (中左) 淡水紅毛城: a red fort with a flag on a hill by the river mouth
- (中左) Tamsui sunset ferry: a small passenger ferry crossing a wide river mouth at sunset
- (中中) 和平島公園: a rocky coast with strange wave-eroded stone shapes and a small park
- (下左) 桃園神社: a Japanese shrine with a torii gate, stone lanterns and a stair path through trees
- (中左) 十三行博物館: a modern dark-roofed museum with a long ribbon-shaped structure beside the Tamsui river mouth
- (下左) 板橋林家花園: a classical Chinese garden with a pavilion, a pond and a curved bridge
- (中左) Beitou hot springs: steam rising from a valley of hot springs with small bathhouses
- (下左) 西門紅樓: an octagonal red-brick Japanese-era building with a small plaza
- (下左) 迪化街: an old merchant street with arched baroque facades, hanging dried goods and lanterns
- (下左) 回到台北: a cosy small house with a lantern light and a front garden (the end of the journey)
- (下中) 台北101: the tall segmented tower with stacked pagoda-like sections, small trees at its base
- (中中) 基隆廟口: a temple (Dianji Temple) gate with rows of small lantern-lit food stalls in front, a harbour behind
- (中右) 九份: a steep hillside of old teahouses with red lanterns and stone steps down to the sea
- (下右) 福隆: a wide sandy beach where a river meets the ocean, a long sandbar, small umbrellas
- (下右) 三貂角: a white lighthouse standing on a green headland above the sea
- (下中) Pinglin tea hills: terraced hillsides of tea plantations with neat curved rows and a small tea factory hut

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile02.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 03 張　緯度 24.51–24.991°N，經度 120.599–121.386°E（7 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上右) 中壢藝術館: a small modern brick arts hall with a courtyard
- (中中) Hsinchu windy coast: a windswept coast with wind-bent pines and a cycle path
- (上中) 南寮漁港: a harbour with a long seaside cycle path and small boats
- (中中) 新竹城隍廟: a large city-god temple with a colourful carved roof and a courtyard
- (下左) Miaoli coastal wind turbines: a row of wind turbines on a coastal plain
- (中中) Roadside Earth God shrine: a small roadside Earth God shrine with incense and a banyan tree
- (下左) 通霄精鹽廠: a salt factory building with big white salt piles and a small train

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile03.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 04 張　緯度 24.51–24.991°N，經度 121.268–122.055°E（9 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上中) Pinglin tea hills: terraced hillsides of tea plantations with neat curved rows and a small tea factory hut
- (中中) 宜蘭酒廠: an old factory building with a tall chimney and large wooden barrels
- (上右) 頭城老街: a row of old brick shophouses with arcaded fronts and baroque-style facades
- (上右) 蘭陽博物館: a modern building shaped like a cuesta rock slope, beside the sea
- (中中) 羅東林業文化園區: a wooden log pond with stacked logs and a small steam train
- (中中) Lanyang plain paddies: a patchwork of green rice paddies with a water buffalo and an egret
- (中右) 傳統藝術中心: a traditional Chinese courtyard complex with curved roofs beside a canal, a small bridge
- (下右) 蘇澳冷泉: a round stone spring pool with bubbles rising and a few people soaking, cool blue water
- (下右) 南方澳漁港: a bridge over a colourful fishing harbour full of boats

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile04.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 05 張　緯度 24.1–24.581°N，經度 119.93–120.717°E（7 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上右) 通霄精鹽廠: a salt factory building with big white salt piles and a small train
- (中右) 大甲鎮瀾宮: a large Mazu temple with a procession of small figures carrying a palanquin
- (上右) Yuanli grass-weaving fields: low rush-grass fields and a small weaving house
- (中右) 梧棲漁港: a harbour with many moored fishing boats and a small market
- (中右) 高美濕地: a row of white wind turbines on a tidal flat at sunset, shallow pools reflecting the sky
- (中右) Railway bridge: a train crossing a small iron bridge over a river
- (下右) 國家歌劇院: a futuristic white curved building with cave-like openings

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile05.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 06 張　緯度 24.1–24.581°N，經度 120.599–121.386°E（5 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上左) 通霄精鹽廠: a salt factory building with big white salt piles and a small train
- (中左) 大甲鎮瀾宮: a large Mazu temple with a procession of small figures carrying a palanquin
- (上左) Yuanli grass-weaving fields: low rush-grass fields and a small weaving house
- (中左) Railway bridge: a train crossing a small iron bridge over a river
- (下左) 國家歌劇院: a futuristic white curved building with cave-like openings

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile06.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 07 張　緯度 24.1–24.581°N，經度 121.268–122.055°E（0 個物件）

```
There are no illustrations in this section; it is plain terrain and sea.

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile07.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 08 張　緯度 23.691–24.173°N，經度 119.93–120.717°E（8 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上右) 國家歌劇院: a futuristic white curved building with cave-like openings
- (中中) Wanggong oyster fields: tidal flats with oyster racks and a small fishing hamlet
- (上中) 鹿港老街: a very narrow alley between tall red-brick walls with a tiny strip of sky
- (上右) 彰化扇形車庫: a semicircular steam-locomotive depot with a locomotive on a turntable
- (上右) 八卦山大佛: a very large seated Buddha statue on a hill above a city
- (下右) 西螺大橋: a very long steel truss bridge crossing a wide braided river
- (中右) Tianwei flower garden: rows of colourful flower fields and greenhouses
- (下中) Mailiao wind turbines: a row of white wind turbines along a coastal dike

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile08.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 09 張　緯度 23.691–24.173°N，經度 120.599–121.386°E（1 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上左) 國家歌劇院: a futuristic white curved building with cave-like openings

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile09.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 10 張　緯度 23.691–24.173°N，經度 121.268–122.055°E（3 個物件）

```
The illustrations in this section (position in the picture, subject):
- (中中) 松園別館: a two-storey Japanese-era wooden villa with a tiled roof among old pine trees
- (上中) 七星潭: a crescent-shaped bay with a pebble beach, a few kites in the air
- (下左) 林田山: a wooden forestry village on a hillside with a small logging railway and stacked logs

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile10.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 11 張　緯度 23.282–23.763°N，經度 119.93–120.717°E（10 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上中) Mailiao wind turbines: a row of white wind turbines along a coastal dike
- (中中) Temple fair: a small roadside temple fair with colourful banners and lanterns
- (中中) Pineapple field: a pineapple field with rows of spiky plants
- (中左) Dongshi oyster racks: oyster-farm racks on a tidal flat with fishermen
- (中中) 朴子配天宮: a large Mazu temple with an ornate multi-layered roof, red lanterns
- (中中) 水上北回歸線標: a small white monument with a ring symbol on flat farmland (Tropic of Cancer marker)
- (中中) 檜意森活村: Japanese-era wooden houses with tiled roofs among tall cypress trees
- (中右) 嘉義公園: a park with a tall shrine-like pavilion and old trees
- (下左) 布袋: a fishing harbour with a ferry and fishing boats, a small lighthouse
- (下中) Lotus pond: a lotus pond with pink flowers and large round leaves

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile11.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 12 張　緯度 23.282–23.763°N，經度 120.599–121.386°E（3 個物件）

```
The illustrations in this section (position in the picture, subject):
- (中右) 瑞穗北回歸線標: a small white stone monument with a ring-shaped top on a hill (Tropic of Cancer marker)
- (中右) Ruisui ranch: a green hillside pasture with a group of cows
- (下右) 玉里: a steaming bowl of noodles in front of a small railway station building

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile12.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 13 張　緯度 23.282–23.763°N，經度 121.268–122.055°E（4 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上左) 林田山: a wooden forestry village on a hillside with a small logging railway and stacked logs
- (中左) 瑞穗北回歸線標: a small white stone monument with a ring-shaped top on a hill (Tropic of Cancer marker)
- (中左) Ruisui ranch: a green hillside pasture with a group of cows
- (下左) 玉里: a steaming bowl of noodles in front of a small railway station building

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile13.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 14 張　緯度 22.873–23.354°N，經度 119.93–120.717°E（7 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上中) Water buffalo: a water buffalo standing in a flooded rice paddy with an egret
- (上中) Lotus pond: a lotus pond with pink flowers and large round leaves
- (中左) Qigu lagoon oyster racks: oyster-farm racks in a shallow lagoon with a few small boats
- (上左) 井仔腳鹽田: tile-floored salt pans in a grid, small white salt pyramids, the sea edge
- (下左) 安平古堡: a red-brick fort with a watchtower and thick walls, old banyan trees
- (下中) 赤崁樓: a red-brick tower with a curved roof among old trees
- (下中) 奇美博物館: a neoclassical white museum with columns and a large fountain

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile14.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 15 張　緯度 22.873–23.354°N，經度 120.599–121.386°E（6 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上右) 玉里: a steaming bowl of noodles in front of a small railway station building
- (中右) 池上: a green rice field with a small railway station and a lunch box (bento) in the foreground
- (中右) Chishang paddies: wide golden rice fields with a long straight farm road and a lone tree
- (中右) 羅山瀑布: a high narrow waterfall in a green valley with a small bridge
- (中右) 關山: a bicycle on a path circling a rice field, mountains behind
- (下中) 鹿野高台: a hot-air balloon rising over green tea terraces, a lookout deck

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile15.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 16 張　緯度 22.873–23.354°N，經度 121.268–122.055°E（2 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上左) 玉里: a steaming bowl of noodles in front of a small railway station building
- (中左) 羅山瀑布: a high narrow waterfall in a green valley with a small bridge

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile16.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 17 張　緯度 22.463–22.944°N，經度 119.93–120.717°E（8 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上中) 奇美博物館: a neoclassical white museum with columns and a large fountain
- (上中) 岡山水塔: an old brick water tower among low houses
- (中中) 橋頭糖廠: an old sugar mill with a tall chimney, a small sugarcane train and cane fields
- (下中) 旗津燈塔: a white lighthouse on a sandy spit at the harbour mouth
- (下中) 高雄駁二: old brick warehouses turned into an art area with colourful art installations, a harbour crane
- (中中) 愛河: a calm river with small boats and willow trees, bridges
- (中中) 蓮池潭: a lake with the Dragon and Tiger Pagodas on the shore, a zigzag bridge
- (中右) 屏東演藝廳: a modern curved building with a green roof

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile17.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 18 張　緯度 22.463–22.944°N，經度 120.599–121.386°E（5 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上中) 鹿野高台: a hot-air balloon rising over green tea terraces, a lookout deck
- (中中) 史前博物館: a modern museum building with a curved glass roof and a stone-age mural wall
- (上中) 卑南文化公園: stone slab ruins in a grass park under a big sky
- (下中) 太麻里: a coast with the first golden light of sunrise over the Pacific, a small seaside shrine
- (中中) 知本溫泉: a steaming outdoor hot spring pool among trees

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile18.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 19 張　緯度 22.054–22.535°N，經度 119.93–120.717°E（5 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上中) 東港: a temple with a large decorated wooden ceremonial ship (Wangchuan) on a harbour shore
- (上右) Linbian wax-apple farms: orchards of wax-apple trees with netting and a roadside fruit stand
- (上右) Betel-nut stand: a roadside betel-nut stand with a tiny shop and tall palms
- (中右) 枋寮: a small railway station with a steam train passing, the sea in the background
- (下右) 車城福安宮: a Taiwanese Earth God temple with a curved swallowtail roof and a very large golden statue of the Earth God beside it

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile19.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 20 張　緯度 22.054–22.535°N，經度 120.599–121.386°E（2 個物件）

```
The illustrations in this section (position in the picture, subject):
- (中左) 壽卡: a mountain pass road winding up between steep forested slopes, a small roadside shrine
- (下左) 車城福安宮: a Taiwanese Earth God temple with a curved swallowtail roof and a very large golden statue of the Earth God beside it

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile20.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 第 21 張　緯度 21.8–22.281°N，經度 120.599–121.386°E（7 個物件）

```
The illustrations in this section (position in the picture, subject):
- (上左) 壽卡: a mountain pass road winding up between steep forested slopes, a small roadside shrine
- (中左) 海生館: a modern aquarium building with a large whale sculpture and waves
- (中左) 車城福安宮: a Taiwanese Earth God temple with a curved swallowtail roof and a very large golden statue of the Earth God beside it
- (下左) 貓鼻頭: a rock on the shore shaped like a crouching cat, surrounded by waves
- (中左) 恆春古城: a stone city gate with a tower and thick curved walls, a quiet street
- (下左) Kenting beach dunes: a white sand beach with dunes, windswept grass and a few palms
- (下左) 鵝鑾鼻: a white lighthouse on a grassy coral-limestone cape, the sea beyond

After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile21.png (no extra words in the name). If a file with that name already exists, replace it.
```

## 交給我之後

我會逐張自動檢查：對位（縮放、位移、殘差）、整塊有沒有被重畫、每個物件有沒有被搬動，列成一張表給你；通過的我再合併成一張大圖（接縫用多頻段混合，兩張重疊的色調也會對齊）。不通過的只需重做那一張。
