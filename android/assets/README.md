# 素材庫（asset library）

星霧大陸需要的所有插圖與貼圖都列在 [`manifest.json`](manifest.json)：共 33 項，每項有用途、尺寸、是否透明、完整的 AI 提示詞。

## 目前狀態
- **程式產生（已完成）**：羊皮紙底紋、迷霧團、圖示底。由 `tools/make_procedural.py` 產生，已匯出進 App。
- **需要 AI 繪圖（待生成）**：其餘 30 項（羅盤、場景前景、旅人、地圖與地標、背包素材、圖示前景）。
  開發環境裡沒有圖像生成模型可用，所以這部分要用外部的圖像 AI（ChatGPT／Gemini／Midjourney／Stable Diffusion 等）生成，再放進來。
  App 找不到圖時會自動用內建的程式繪圖，所以可以一張一張補。

## 流程
```bash
cd android/assets
python3 tools/build_assets.py prompts   # 產生 PROMPTS.md：每個素材一段可直接貼上的提示詞
# 用圖像 AI 生成後，存成 generated/<id>.png（.jpg / .webp 也可以）
python3 tools/build_assets.py check     # 看哪些到了、尺寸／比例有沒有問題
python3 tools/build_assets.py export    # 去背、縮到規格、轉 WebP，寫進 app/src/main/res/drawable-nodpi/art_<id>.webp
python3 tools/build_assets.py sheet     # 產生 preview.png 總覽
```
需要 Python 3、Pillow、numpy。

## 生成時的重點
1. **風格一致**：每個提示詞都帶同一段「共同風格」。同一批素材盡量用同一個工具、同一組設定生成。
2. **透明背景素材**：請用**純白底**生成（提示詞已寫明）。匯出時會把與邊界相連的純色背景去掉；圖裡面的白色高光會保留。
   羅盤外殼（`compass_case`）中央開口也要透明，所以它會把內部的白一起去掉。
3. **可橫向平鋪的長條**（遠景樹林、海浪、地面小徑）：左右邊緣要能接上。若 AI 做不到，把圖左右各切一半對調後修接縫即可。
4. **文字**：所有素材都不要有文字。地名、數字都由 App 用字型畫，AI 畫中文很容易出錯。
5. **尺寸**：匯出只會縮小、不會放大；比例不對時從中心裁切。`check` 會提醒。

## 加素材 / 改規格
改 `manifest.json`，再跑 `prompts`。App 端用 `rememberArt("<id>")` 取圖（見 `ui/Art.kt`）；取不到就回到程式繪圖。
目前 App 已經接上的：`parchment_tile`（全 App 背景）、`fog_puff`（世界地圖迷霧）、圖示（`icon_foreground`／`icon_background`）。
其他素材的版面接線，等圖到了再配合實際構圖調整（例如羅盤表盤的位置、旅人的腳底線）。
