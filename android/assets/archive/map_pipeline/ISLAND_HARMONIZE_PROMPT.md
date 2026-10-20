# 真實世界地圖：把已放好位置的物件「融入」地圖（只修邊、不動位置）

上一版讓 Gemini 自己決定物件的位置，結果位置偏了。這一版反過來：
**物件已經由我按真實座標貼在地圖上**（`mapguide/bands/bandN_composite.png`），Gemini 只負責「讓它看起來是同一幅畫」，
不准搬動、縮放、增減任何東西。（我試過：Gemini 對「地形」的對位其實很準，偏的只有它自己決定的物件位置；所以只修邊最安全。）

每段（共三段）各開一個新對話，比例 3:2、解析度選最高，只上傳一張：`bandN_composite.png`，貼下面的提示詞。

```
This image is an antique hand-drawn atlas map of part of Taiwan. Small pictorial illustrations (buildings, harbours, fields, temples, lighthouses, etc.) have been pasted onto the map and they currently look like separate stickers with light paper-coloured edges.

Task: return the SAME image, same framing, same size ratio (3:2), highest resolution, with these illustrations repainted so that they blend into the map as if drawn in the same sitting:
- Keep every illustration exactly where it is, at the same size, with the same subject and the same composition. Do NOT move, resize, rotate, add, remove or replace any illustration.
- Keep the whole map (coastline, sea, mountains, rivers, fields, colours, paper texture) exactly as it is. Do not redraw or re-compose any area that is not an illustration or its immediate edge.
- Only change the edge of each illustration: remove the light sticker-like border/patch, let its ground melt into the surrounding terrain (grass into grass, sand into sand, water into sea or river, hills into hills), match the colour wash and the pen line weight of the map, and add a very soft contact shadow on the ground.
- Where an illustration stands on the sea, make its water continue into the surrounding sea with the same hatching lines.
- No text, no numbers, no frames, no new objects, no pink marks.
```

存檔：`generated/bands/band1.png`、`band2.png`、`band3.png`

## 我收到之後

1. 先檢查每個物件有沒有被搬動（把你的結果與合成圖逐物件比對位移，超過幾個像素的我會挑出來）。
2. 位移很小的整段採用；有問題的物件，我回退成原本貼的版本，或請你只對那一段再生成一次。
3. 三段在沒有物件的接縫線上漸變合併成一張大圖放進 App。
