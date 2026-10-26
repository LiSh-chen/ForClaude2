# 旅行者拆層

走路動畫要把角色拆成三層（身體、手杖加提燈、握杖的手）。來源是使用者做的角色設定圖 `generated/traveller_sheet.jpg`（正面、側面、背面三視圖）。

1. `1_cut_sheet.py`：裁出中間的側面圖、去掉白底，輸出 `full.png`。
2. `2_split_layers.py`：用顏色與連通區域把手杖（含提燈）分出來，手杖被手擋住的那一段補上，帽簷尖端被手杖蓋住的地方用修補填回，輸出 `L_body.png`、`L_staff.png`、`L_hand.png`；最後會印出「三層疊起來與原圖不同的像素數」（目前約 460，約佔全圖 0.1%）。
3. 兩邊各加 70 px 空白（手杖擺動不被切掉）、左右翻轉（角色要朝右）、縮成一半，存成 `drawable-nodpi/art_traveller_{body,staff,hand}.webp`（411×400）。

程式裡的支點座標（`RealMap.kt` 的 `HAND_*`、`FEET_*`、`LANTERN_*`）是依這個尺寸算出的比例。換角色圖就要重新量。
腳本裡的路徑指向暫存資料夾，重做時請改成實際路徑。

## 現行角色（2026-10 起）

地圖上的旅行者改用使用者提供的可愛版角色 `generated/traveller_cute.jpg`（角色站在島上的正面圖）。`tools/cute_traveller/make_cute_traveller.py` 用墨線輪廓把角色從島上摳出來，輸出 `art_traveller_body.webp`，手杖與手兩層留空（整隻角色是一張圖，程式照舊讓它彈跳、搖擺、提燈發光），同時產生 App 圖示前景 `art_icon_foreground.webp`。舊的三視圖版本與逐格走路圖備份在 `assets/archive/traveller_sheet_version/`。以下是舊版（三視圖）的做法，換回時才用得到。

## 逐格走路動畫（可愛版，2026-10 起）

使用者用 Gemini 畫的走路連續動作圖 `generated/traveller_walk_sheet_cute.jpg`（3 欄 × 2 列、側面朝右）用同一支 `3_slice_walk.py` 處理（`--rows 2 --cols 3`，不用 `--flip`），輸出的 6 格放進 `drawable-nodpi/art_traveller_walk_0..5.webp`；站立圖 `art_traveller_body.webp` 用第 0 格，這樣站著和走路是同一個側面造型。畫布大小跟著 `art_traveller_body.webp`（681×708）。去背時提燈外圍的淡黃光暈也會去掉（程式在走路與站立時另外畫光暈）。

## 逐格走路動畫（舊版，選用）

使用者另外做的連續動作圖 `generated/traveller_walk_sheet.jpg`（3 欄 × 2 列、6 個走路姿勢，角色朝左）用 `3_slice_walk.py` 處理：

    python3 tools/traveller_rig/3_slice_walk.py generated/traveller_walk_sheet.jpg <輸出資料夾> --flip

它會切成 6 格、去掉白底（連帽簷與手杖之間被圍住的白色也去掉）、用同一個縮放比例，並對齊成與站立圖相同的高度與身體位置，輸出 `art_traveller_walk_0..5.webp` 加一張 `contact.png` 預覽。把 6 張放進 `drawable-nodpi/`，走路時 App 就逐格播放；少一張就退回程式做的彈跳與搖擺。
