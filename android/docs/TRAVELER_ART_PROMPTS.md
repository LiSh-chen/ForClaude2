# 旅人頭像：Gemini 圖片提示詞

給 Gemini（網頁或 App）用。目標：五位旅人各一張頭像，風格一致，放進 App 的旅人頁、相遇視窗和明信片角落。

## 怎麼做（建議順序）
1. **同一個對話串裡生成全部五張**，Gemini 才會記得風格。先貼「共同風格」，再依序貼各角色。
2. **先生成汐婆**，滿意後把那張圖當參考，在後面幾位的提示詞前加一句：「Match the art style, line weight, colour palette and framing of the previous image exactly.」
3. 每位生成 3～4 張，挑最好的一張。不滿意就用「改：……」逐項調整，不要整個重寫。
4. 存成 **PNG，正方形（1:1），至少 1024×1024**。檔名用旅人代號：`ahbo.png`、`xiaohe.png`、`xipo.png`、`atie.png`、`momo.png`。
5. 把五張圖放進對話或專案資料夾給我，我負責縮成 512×512、裁成圓形、檢查在深淺背景上的樣子，再接進 App。（現有的程式畫頭像會保留當備援。）

## 共同風格（先貼這段）
```
A series of five character portraits for a mobile walking-adventure game set in Taiwan.

Art style: a simple cartoon doodle drawn by hand in a travel notebook, like a quick pen sketch with a few colour washes added. NOT realistic, NOT detailed. Bold, slightly wobbly dark-brown pen outlines of even thickness. Big simple shapes, flat colour fills with only a touch of watercolour texture, almost no shading. Cute stylised proportions: a rounded head that is a bit large, small simple dot eyes, a tiny nose, a simple curved line for the mouth, only one or two lines for wrinkles or hair detail. Leave small gaps in the colouring and let the pen lines overshoot, like a real notebook doodle.

Palette: muted and warm. Parchment cream, dark brown ink (#3B2A1A), moss green (#5F8A52), brass gold (#B8892D), faded sea blue, dusty rose, brick red.

Composition for every image: SQUARE 1:1 aspect ratio, 1024 x 1024 pixels. One character, head and shoulders, slightly turned, centered, filling about 75% of the frame with a margin all around. Background: plain flat parchment colour (#E6D3A5), no paper texture, no scenery, no border, no frame, no text, no watermark.

The characters are ordinary Taiwanese people in everyday travel clothes. Keep all five consistent with each other in line weight, level of detail and head proportions. Confirm you understand and wait for the first character.
```

## 一、汐婆（先畫這位）
退休的船長，年長女性，閱歷豐富、說話直爽。
```
Character 1, "Xi Po": a retired ship captain, a cheerful elderly Taiwanese grandmother. Round face with a few simple wrinkle lines, short grey hair peeking out from a blue headscarf knotted at the side, a warm teasing smile. A simple navy sailor jacket with a white stripe on the collar, and a small brass compass hanging on a cord around her neck. Doodle style as described, very few details.
```

## 二、阿柏
攝影師，愛山和雲，安靜、有耐心。
```
Character 2, "Ah Bo": a mountain photographer in their early thirties (man or woman), wearing a moss-green baseball cap and a simple jacket. A chunky camera hangs on a strap on the chest, one hand resting on it. Calm, patient, slightly dreamy small smile. Doodle style as described, very few details.
```

## 三、小禾
騎單車的農學系學生，活潑、好奇。
```
Character 3, "Xiao He": a cheerful agriculture student around twenty-two travelling by bicycle, wearing a wide straw hat with a red band and a cream shirt with rolled sleeves, a small notebook in the pocket. A bicycle handlebar and bell peek in at the edge. Big happy smile. Doodle style as described, very few details.
```

## 四、阿鐵
鐵道迷，健談、愛分享。
```
Character 4, "Ah Tie": a railway enthusiast in their late twenties (man or woman), wearing a navy railway-style cap with a small brass badge. A silver rectangular lunch box (like a Taiwan railway bento box) is hugged against the chest. Excited, talkative expression, head tilted as if listening to a distant train. Doodle style as described, very few details.
```

## 五、墨墨
拿速寫本的畫家，敏感、愛觀察。
```
Character 5, "Mo Mo": a travelling sketch artist around twenty-five, wearing a tilted red beret and a dusty-rose scarf, a brown sketchbook held against the chest and a pencil behind the ear. Thoughtful look with a small smile. Doodle style as described, very few details.
```

## 常用的調整句（逐項改）
- 「改：臉再圓一點、年紀再小一點。」
- 「改：更簡單、更卡通，細節再少一半。」
- 「改：線條再粗一點，顏色用平塗，水彩質感再淡一點。」
- 「改：請輸出正方形 1:1，1024×1024。」（Gemini 常會輸出橫圖）
- 「改：背景只要純色，不要任何紋理或景物。」
- 「改：表情更開心／更安靜。」
- 「改：配件要更清楚，但不要變大。」

## 驗收清單（我收到圖後會檢查）
- 五張是同一種畫風和同一種構圖，放在一起不突兀。
- 背景是單純的羊皮紙色，可以乾淨地裁成圓形。
- 頭像縮到 52 像素時仍能分出誰是誰：看帽子和配件。
- 沒有文字、簽名、浮水印。
- 畫面是簡單的塗鴉風，不是寫實或精細的水彩。
- 長相與年齡符合角色。

## 待你決定
- 阿柏、小禾、阿鐵、墨墨的性別我留給 Gemini 決定（提示詞寫「man or woman」）。你想指定的話，直接改這幾行。汐婆設定為年長女性。
- 之後如果想要全身立繪（相遇視窗用）或表情差分，再另外出一份提示詞。
