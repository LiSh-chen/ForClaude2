# 旅行者拆層

走路動畫要把角色拆成三層（身體、手杖加提燈、握杖的手）。來源是使用者做的角色設定圖 `generated/traveller_sheet.jpg`（正面、側面、背面三視圖）。

1. `1_cut_sheet.py`：裁出中間的側面圖、去掉白底，輸出 `full.png`。
2. `2_split_layers.py`：用顏色與連通區域把手杖（含提燈）分出來，手杖被手擋住的那一段補上，帽簷尖端被手杖蓋住的地方用修補填回，輸出 `L_body.png`、`L_staff.png`、`L_hand.png`；最後會印出「三層疊起來與原圖不同的像素數」（目前約 460，約佔全圖 0.1%）。
3. 兩邊各加 70 px 空白（手杖擺動不被切掉）、左右翻轉（角色要朝右）、縮成一半，存成 `drawable-nodpi/art_traveller_{body,staff,hand}.webp`（411×400）。

程式裡的支點座標（`RealMap.kt` 的 `HAND_*`、`FEET_*`、`LANTERN_*`）是依這個尺寸算出的比例。換角色圖就要重新量。
腳本裡的路徑指向暫存資料夾，重做時請改成實際路徑。

## 逐格走路動畫（選用）

使用者另外做的連續動作圖 `generated/traveller_walk_sheet.jpg`（3 欄 × 2 列、6 個走路姿勢，角色朝左）用 `3_slice_walk.py` 處理：

    python3 tools/traveller_rig/3_slice_walk.py generated/traveller_walk_sheet.jpg <輸出資料夾> --flip

它會切成 6 格、去掉白底（連帽簷與手杖之間被圍住的白色也去掉）、用同一個縮放比例，並對齊成與站立圖相同的高度與身體位置，輸出 `art_traveller_walk_0..5.webp` 加一張 `contact.png` 預覽。把 6 張放進 `drawable-nodpi/`，走路時 App 就逐格播放；少一張就退回程式做的彈跳與搖擺。
