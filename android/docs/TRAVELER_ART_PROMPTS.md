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
A series of five character portraits for a mobile walking-adventure game set in Taiwan. Art style: hand-drawn ink and watercolour illustration, like a page from an old botanist's field notebook. Soft sepia ink outlines with a slightly uneven hand-drawn line, gentle watercolour washes, subtle paper grain. Muted warm palette: parchment cream (#F1E3BE), dark brown ink (#3B2A1A), moss green (#5F8A52), brass gold (#B8892D), faded sea blue, dusty rose. Framing for every portrait: head and shoulders, slightly turned three-quarter view, friendly relaxed expression, centered, filling about 80% of the frame. Background: plain flat parchment colour (#E6D3A5) with no scenery, no border, no frame, no text, no watermark. The characters are ordinary Taiwanese people in everyday travel clothes. Keep all five consistent with each other. Confirm you understand and wait for the first character.
```

## 一、汐婆（先畫這位）
退休的船長，年長女性，閱歷豐富、說話直爽。
```
Character 1, "Xi Po": a retired ship captain, an elderly Taiwanese woman in her late sixties, weathered tanned skin, kind sharp eyes, short grey-streaked hair mostly covered by a faded blue headscarf knotted at the side. She wears a worn navy sailor-style jacket over a light shirt. A small brass compass hangs on a cord around her neck. Warm, wise, slightly teasing smile.
```

## 二、阿柏
攝影師，愛山和雲，安靜、有耐心。
```
Character 2, "Ah Bo": a mountain and landscape photographer, a Taiwanese man or woman in their early thirties (choose the look that fits), wearing a moss-green baseball cap and a simple outdoor jacket. A camera hangs on a strap on the chest, one hand resting on it. Calm, patient, slightly dreamy expression, as if waiting for the right light.
```

## 三、小禾
騎單車的農學系學生，活潑、好奇。
```
Character 3, "Xiao He": a cheerful agriculture student travelling by bicycle, a Taiwanese young person around twenty-two, wearing a wide straw hat with a red band, a cream-coloured shirt with rolled sleeves, a small notebook tucked in the pocket. A bicycle handlebar and a bell are partly visible at the edge of the frame. Bright curious smile, sun-kissed cheeks.
```

## 四、阿鐵
鐵道迷，健談、愛分享。
```
Character 4, "Ah Tie": a railway enthusiast, a Taiwanese man or woman in their late twenties, wearing a navy railway-style cap with a small brass badge and a plain shirt. A silver rectangular lunch box (like a Taiwan railway bento box) is held against the chest or hangs from a strap. Enthusiastic, talkative expression, head slightly tilted as if listening to a distant train.
```

## 五、墨墨
拿速寫本的畫家，敏感、愛觀察。
```
Character 5, "Mo Mo": a travelling sketch artist, a Taiwanese young person around twenty-five, wearing a tilted red beret and a dusty-rose scarf, a brown sketchbook held against the chest and a pencil tucked behind the ear. Slightly ink-stained fingers. Thoughtful, observant expression with a small smile.
```

## 常用的調整句（逐項改）
- 「改：臉再圓一點、年紀再小一點。」
- 「改：線條再粗一點，水彩暈染再淡一點。」
- 「改：背景只要純色，不要任何紋理或景物。」
- 「改：表情更開心／更安靜。」
- 「改：配件要更清楚，但不要變大。」

## 驗收清單（我收到圖後會檢查）
- 五張是同一種畫風和同一種構圖，放在一起不突兀。
- 背景是單純的羊皮紙色，可以乾淨地裁成圓形。
- 頭像縮到 52 像素時仍能分出誰是誰：看帽子和配件。
- 沒有文字、簽名、浮水印。
- 長相與年齡符合角色。

## 待你決定
- 阿柏、小禾、阿鐵、墨墨的性別我留給 Gemini 決定（提示詞寫「man or woman」）。你想指定的話，直接改這幾行。汐婆設定為年長女性。
- 之後如果想要全身立繪（相遇視窗用）或表情差分，再另外出一份提示詞。
