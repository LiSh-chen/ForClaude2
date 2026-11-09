# 旅人頭像：Gemini 圖片提示詞

給 Gemini（網頁或 App）用。目標：五位旅人各一張頭像，風格一致，放進 App 的旅人頁、相遇視窗和明信片角落。

## 怎麼做（建議順序）
1. **同一個對話串裡生成全部五張**，Gemini 才會記得風格。
2. **先附上 App 的景點卡當風格參考圖**（2～3 張），再貼「共同風格」。參考圖在專案裡：
   `android/app/src/main/res/drawable-nodpi/art_card_toucheng.webp`、`art_card_sandiaojiao.webp`、`art_card_fulong.webp`（或直接截 App「收藏」頁的相簿畫面）。
3. **先生成汐婆**，滿意後把那張圖當參考，在後面幾位的提示詞前加一句：「Match the art style, line weight, colour palette and framing of the previous image exactly.」
4. 每位生成 3～4 張，挑最好的一張。不滿意就用「改：……」逐項調整，不要整個重寫。
5. 存成 **PNG，正方形（1:1），至少 1024×1024**。檔名用旅人代號：`ahbo.png`、`xiaohe.png`、`xipo.png`、`atie.png`、`momo.png`。
6. 把五張圖給我，我負責縮成 512×512、裁成圓形、把紙色去掉（混合到 App 的羊皮紙色上）、檢查在小尺寸上的樣子，再接進 App。（現有的程式畫頭像會保留當備援。）

## 為什麼這樣寫
App 的景點卡是**褪色的鉛筆線加淡水彩、有紙紋的老筆記本風**。不要畫成粗黑線、飽和平塗的卡通圖或向量圖，也不要畫成很細緻的寫實水彩。我們要的是兩者之間：**保留舊筆記本的質感，只把人物簡化**。

## 台灣味（重點：靠服裝和配件，不靠臉）
Gemini 畫「亞洲人」容易偏日系。要讓人一眼覺得是台灣，用這些台灣常見的元素：
- **花布**：台灣懷舊的大朵牡丹或玫瑰花布（客家花布、阿嬤的花襯衫、花布頭巾、花布袖套）。
- **毛巾**：掛在脖子上的白毛巾，是台灣漁港、田裡、工地的標誌。
- **斗笠**：竹編圓錐斗笠（農夫、漁民）。
- **平安符**：紅色小布包的護身符、紅線，掛在脖子或背包上（媽祖信仰）。
- **台鐵便當盒**：鋁製便當盒，圓角長方形，上面有蓋。
- **日常裝扮**：運動外套、拉鍊薄外套、夾腳拖、雨鞋、帆布側背包。
- **平安符、便當盒、袋子上不要有字**（AI 畫的中文字會亂碼）。
- **避免**：水手領（像日本制服）、和服、和風圖案、櫻花、鳥居。

另外加一句：`Everyday Taiwanese look: warm sun-tanned skin, practical clothes, retro Taiwanese floral cloth and a towel around the neck where it fits. Avoid anything that looks Japanese or Korean (no sailor collar, no kimono-like styling).`

## 共同風格（附參考圖後貼這段）
```
A series of five character portraits for a mobile walking-adventure game set in Taiwan, matching the attached reference images.

Style: a loose travel-sketchbook illustration in the same style as the attached reference images. Thin sepia-brown pencil and fine pen lines with varying thickness, a little hatching on the clothes, soft faded watercolour washes in muted, greyed colours (dusty blue, sage green, faded brass, brick red, dusty rose), the colour not quite filling the outlines. Simplified and gently stylised features: small dot eyes, a small nose, a simple mouth, only a few lines for wrinkles or hair, minimal detail. It must NOT look like clean vector art, NOT like flat saturated cartoon colour, and NOT like a detailed realistic painting.

Composition: SQUARE 1:1 aspect ratio, 1024 x 1024 pixels. One character, head and shoulders, slightly turned, centered, filling about 70% of the frame. Background: no flat colour block and no scenery. The painted area fades out irregularly into blank, slightly grainy old paper, like a sketch on a notebook page with a soft vignette edge. Visible paper texture. No border, no frame, no watermark. IMPORTANT: no text, letters or Chinese characters anywhere in the image, including on charms, lunch boxes, bags and clothing (AI-drawn characters come out garbled); leave such surfaces blank.

The characters are ordinary Taiwanese people in everyday travel clothes (see the Taiwan details per character). Keep all five consistent with each other in line weight, level of detail and head proportions. Confirm you understand and wait for the first character.
```

## 一、汐婆（先畫這位）
退休的船長，年長女性，閱歷豐富、說話直爽。
```
Character 1, "Xi Po": a retired fishing-boat captain from a Taiwanese fishing port, a cheerful elderly Taiwanese grandmother with warm sun-tanned skin and a few simple wrinkle lines. Grey hair tied back in a small low bun, covered by a retro Taiwanese floral-cloth headscarf in faded blue (large peony pattern). A white towel around her neck. A plain navy work shirt (no sailor collar). Around her neck, on the same red string, a small brass compass and a small plain red cloth amulet pouch with no writing on it. Warm, teasing smile. Sketchbook style as described, very few details.
```

## 二、阿柏
攝影師，愛山和雲，安靜、有耐心。
```
Character 2, "Ah Bo": a mountain photographer in their early thirties (man or woman), wearing a moss-green baseball cap, a practical zip-up hiking jacket and a backpack strap, a small red amulet charm tied to the strap. A chunky camera hangs on the chest, one hand resting on it. Calm, patient, slightly dreamy small smile. Everyday Taiwanese look. Sketchbook style as described, very few details.
```

## 三、小禾
騎單車的農學系學生，活潑、好奇。
```
Character 3, "Xiao He": a cheerful agriculture student around twenty-two travelling by bicycle, wearing a Taiwanese bamboo conical farmer's hat (dou li) with a red band, a cream shirt, and floral-cloth arm sleeves. A white towel around the neck. A small notebook in the pocket. A bicycle handlebar and bell peek in at the edge. Big happy smile. Sketchbook style as described, very few details.
```

## 四、阿鐵
鐵道迷，健談、愛分享。
```
Character 4, "Ah Tie": a railway enthusiast in their late twenties (man or woman), wearing a navy cap with a small brass badge and a zip-up casual jacket. A silver aluminium rectangular lunch box with a lid (like a classic Taiwan Railways bento box) is hugged against the chest. Excited, talkative expression, head tilted as if listening to a distant train. Everyday Taiwanese look. Sketchbook style as described, very few details.
```

## 五、墨墨
拿速寫本的畫家，敏感、愛觀察。
```
Character 5, "Mo Mo": a travelling sketch artist around twenty-five, wearing a tilted red beret and a dusty-rose scarf, a canvas shoulder bag with a small red amulet charm, a brown sketchbook held against the chest and a pencil behind the ear. Thoughtful look with a small smile. Everyday Taiwanese look. Sketchbook style as described, very few details.
```

## 常用的調整句（逐項改）
- 「改：臉再圓一點、年紀再小一點。」
- 「改：更簡單，細節再少一半，但維持筆記本的鉛筆線和褪色水彩質感。」
- 「改：線條改成細的棕色鉛筆，不要粗黑線。」
- 「改：顏色再褪一點、灰一點，像老水彩。」
- 「改：背景不要有色塊，只有紙紋，畫面邊緣自然暈開。」
- 「改：線條再粗一點，顏色用平塗，水彩質感再淡一點。」
- 「改：請輸出正方形 1:1，1024×1024。」（Gemini 常會輸出橫圖）
- 「改：背景只要純色，不要任何紋理或景物。」
- 「改：表情更開心／更安靜。」
- 「改：配件要更清楚，但不要變大。」

## 驗收清單（我收到圖後會檢查）
- 五張是同一種畫風和同一種構圖，放在一起不突兀。
- 頭像縮到 52 像素時仍能分出誰是誰：看帽子和配件。
- 沒有文字、簽名、浮水印。
- 畫風與 App 的景點卡搭配：細棕色鉛筆線、褪色水彩、有紙紋，不是粗黑線卡通，也不是精細寫實。
- 背景是暈開的空白紙，不是色塊。
- 長相與年齡符合角色。

## 待你決定
- 阿柏、小禾、阿鐵、墨墨的性別我留給 Gemini 決定（提示詞寫「man or woman」）。你想指定的話，直接改這幾行。汐婆設定為年長女性。
- 之後如果想要全身立繪（相遇視窗用）或表情差分，再另外出一份提示詞。
