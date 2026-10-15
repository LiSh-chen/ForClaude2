# 旅人角色：給 Gemini 的提示詞

目標：一位有我們自己特色的旅人（不是動物），風格和羊皮紙地圖一致。
步驟：先挑**造型**，再畫**動作圖**。每一步都先給我確認再做下一步。

## 設定（這是我建議的方向，你可以改）

**星霧旅人**：一位年輕的徒步旅人，性別不明確，戴寬簷旅行帽，披一件縫著星形補丁的舊斗篷，
背著鼓鼓的帆布背包，手持一根長手杖，杖頂掛一盞會發出淡淡星光的小油燈（呼應「星霧大陸」）。
穿皮靴、綁腿。臉小小的、表情溫和好奇，頭身比約 3 頭身，像古地圖上的小人物。
配色：斗篷用褪色的暗綠和赭紅，帽子奶油色，星形補丁是黃銅色，燈光是淡黃。

## 第 1 步：造型三視圖（要你確認）

上傳 `previews/2_compass.jpg` 和 `generated/real_map_base.png` 當風格參考，貼：

> Design a new original traveller character for an antique pictorial map game, in the same hand-drawn pen-and-ink and watercolour style as the uploaded references. The character is a young walking traveller, gender-neutral, human, about 3 heads tall (a small charming proportion like a figure drawn on an old map), wearing a wide-brimmed cream travel hat, a worn muted-green cloak patched with a few brass star-shaped patches, leather boots with leg wraps, a bulging canvas backpack and a long wooden walking staff with a small glowing star-light lantern hanging from the top. Gentle curious face. NOT an animal, no animal ears or tails. Show three views side by side on a plain white background: front, side (facing right) and back, same scale, standing still. No text, no ground shadow.

存檔：`generated/traveller_design.png`

請給我看你選的那張，如果想改（例如不要背包、改帽子、改色），在同一個對話裡跟 Gemini 說就好。

## 第 2 步：動作圖（造型確認後）

上傳確認好的 `traveller_design.png`，貼下面三段（各一張）：

### 2a　行走（4 欄×2 列，共 8 個）

> Using exactly the same character as the uploaded design, draw a side-view walking cycle facing right: 8 frames in 4 columns by 2 rows, reading order, forming a smooth loop (contact, down, passing, up for the right leg, then the same for the left leg). Same scale and same ground line in every cell, the character centred, the walking staff swinging naturally, the lantern swaying slightly. Plain pure white background, no ground shadow, no text, no frames.

存檔：`generated/traveller_walk.png`

### 2b　爬坡、下坡、停下來（4 欄×3 列，共 12 個）

> Using exactly the same character as the uploaded design, side view facing right, 12 frames in 4 columns by 3 rows, reading order, same scale and ground line in every cell. Row 1: climbing a steep slope, leaning forward, using the staff to push up, 4 frames of one cycle. Row 2: walking downhill carefully, leaning back slightly, staff planted ahead, 4 frames of one cycle. Row 3: standing still and breathing (2 frames: slight rise and fall), looking around with a hand shading the eyes (1 frame), sitting on the ground resting with the lantern beside (1 frame). Plain pure white background, no ground shadow, no text.

存檔：`generated/traveller_slope.png`

### 2c　特殊動作（4 欄×2 列，共 8 個）

> Using exactly the same character as the uploaded design, side view facing right, 8 frames in 4 columns by 2 rows, reading order, same scale and ground line. Row 1: wading through shallow water with trousers rolled up, lifting feet (4 frames of one cycle). Row 2: (5) picking up a small glowing item from the ground, bending down; (6) holding the item up in delight; (7) waving a hand to say hello; (8) taking a bow with the hat in hand. Plain pure white background, no ground shadow, no text.

存檔：`generated/traveller_special.png`

## 交給我之後

- 把動作切成一格一格，白色去掉。
- 在地圖上依地形選動作：平地用「行走」、爬坡用「爬坡」、下坡用「下坡」、過河用「涉水」，
  走得快慢決定播放速度；停下來時用「站著呼吸」，每次拿到物品或蓋章用「撿東西」。
- 左右方向用鏡像，不用另外畫。
