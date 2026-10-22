# 用實景照片重畫：安平古堡、朴子配天宮、水上北回歸線標

這三張現在的圖風格對、構圖也好看，但建築樣子和實景不像（原因：我寫提示詞時只有文字資料，沒有照片，圖像工具只能憑想像畫）。想畫得像，要**給它實景照片當參考**。

## 做法（每張一個新對話）

1. 先上傳三樣：
   - 風格參考圖 `previews/style_reference.jpg`（現有卡圖的畫風）
   - **一到兩張該景點的實景照片**（請用你自己拍的，或可自由使用的照片；不要直接拿有版權的商業照片）
   - 要重畫的那張現有插圖（可選，讓它保留構圖與好的細節）
2. 貼下面的提示詞（已加上「照片對照」的要求）。
3. 結果存成 `generated/card_<id>.png` 傳回來，我替換掉舊圖。

## 提示詞

### `card_anping` — 安平古堡
```
Create an image with aspect ratio 4:3. Redraw the place in the attached PHOTO as a vintage field-guide watercolor and pen-and-ink illustration, in exactly the style of the attached style-reference picture. Keep the real architecture, layout, proportions, colours and viewpoint of the photo: the fort's old brick wall remains and the watchtower must look the same as in the photo (same shape, same colours, same roof). Keep the old banyan roots creeping over the brick wall. Soft irregular vignette edges fading into aged parchment, 4:3 landscape. No text, no letters, no numbers, no watermarks.
```

### `card_peitian` — 朴子配天宮
```
Create an image with aspect ratio 4:3. Redraw the place in the attached PHOTO as a vintage field-guide watercolor and pen-and-ink illustration, in exactly the style of the attached style-reference picture. Keep the real temple architecture, layout, pose of the Mazu statue (seated or standing exactly as in the photo), colours and viewpoint. KEEP this memorable detail from the earlier version: the dark wooden statue's base is joined to the roots of the tree it was carved from, spreading across the floor. Lanterns and drifting incense smoke. Soft irregular vignette edges fading into aged parchment, 4:3 landscape. No text, no letters, no numbers, no watermarks.
```

### `card_shuishang` — 水上北回歸線標
```
Create an image with aspect ratio 4:3. Redraw the monument in the attached PHOTO as a vintage field-guide watercolor and pen-and-ink illustration, in exactly the style of the attached style-reference picture. Keep the real shape, proportions, materials, colours and viewpoint of the monument and its surroundings exactly as in the photo. It is noon and the sun is overhead, so the monument casts almost no shadow. Soft irregular vignette edges fading into aged parchment, 4:3 landscape. No text, no letters, no numbers, no watermarks.
```

## 另一個辦法：照原樣收下

如果覺得「示意插畫」已經夠用，也可以不重畫。卡片上的標語和補充句才是主要資訊，圖是氣氛。但這個 App 是帶人去真實地點的，使用者到了現場可能會對照，所以樣子差太多還是建議重畫。
