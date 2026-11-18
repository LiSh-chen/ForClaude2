# 授權與資料來源

（與 App 內「設定 → 關於 → 授權與資料來源」相同；上架時貼到商店頁。）

## 行政區與底圖
鄉鎮市區與村里的範圍、歸屬使用 Overture Maps Foundation 的行政區資料（https://overturemaps.org）與 © OpenStreetMap contributors（https://www.openstreetmap.org/copyright），依 Open Database License (ODbL) 1.0 授權。底圖的繪製參考 Overture Maps 與 OpenStreetMap 的道路、水域與土地覆蓋資料。

## 地形
各區的海拔來自 NASA SRTM 地形資料。

## 海岸線
台灣本島的輪廓簡化自 GSHHG 海岸線資料（Wessel & Smith），依 GNU LGPL 授權。

## 字體
內文使用霞鶩文楷 繁體（LXGW WenKai TC），SIL Open Font License 1.1，© The LXGW WenKai Project Authors（https://github.com/lxgw/LxgwWenKai）。字型檔的細節見 docs/FONTS.md。

## 插圖
景點卡、地圖和旅人的插圖由 Google Gemini 生成圖像，再經人工挑選與整理。

## 文字內容
鄉鎮、景點與明信片中的事實，整理自維基百科（CC BY-SA 4.0）、政府機關與新聞媒體的公開資料，並各自查證；來源見 docs/research。旅人說話的口吻與經歷是虛構的。

## 開源程式庫
本 App 使用 Kotlin、Jetpack Compose、Room、DataStore、Kotlin 協程與 kotlinx-datetime 等開源程式庫，皆採 Apache License 2.0。

## 上架前待確認
- 地圖畫面是否需要在畫面角落附上簡短的「© OpenStreetMap contributors」；目前只放在設定的授權頁與商店頁。
- 插圖的 AI 生成說明是否保留（商店頁的 AI 內容揭露欄位也要照實填寫）。
