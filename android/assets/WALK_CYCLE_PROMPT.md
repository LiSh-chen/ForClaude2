# 旅行者走路動畫：畫一張「走路連續動作」圖（可愛版角色）

地圖上的旅行者目前是**一張靜態圖**，程式讓它上下彈跳、左右搖擺（見 `tools/cute_traveller/`）。要升級成**真正逐格的邁步**，需要同一個角色的 6 個走路姿勢。做法：一次畫成 3 欄 × 2 列的格子圖，我再切開、去背、對齊腳底、接進 App。

## 怎麼做

1. 新開對話，**先上傳角色參考圖** `android/assets/generated/traveller_cute.jpg`（就是你提供的那張：角色站在島上）。
2. 貼下面的提示詞，比例選 **3:2**，解析度選最高。
3. 檢查結果（見「檢查清單」）；不合格就在同一對話裡說明要改什麼，或重生成。
4. 存成 `traveller_walk_sheet.png` 傳給我。我會執行  
   `python3 tools/traveller_rig/3_slice_walk.py <圖> <輸出資料夾> --rows 2 --cols 3`（角色若朝左，加 `--flip`；App 要朝右）。

## 提示詞

```
Create an image with aspect ratio 3:2. Use the uploaded picture as the exact reference for the character. Keep this exact character design: a small round, glossy, milk-white spirit-like body with two tiny rounded stubby feet and short stubby arms ending in small white mitten hands; large glossy dark-brown eyes with white highlights and a small open smile with soft pink blush marks; a wide-brim cream hat with a dark-green band and a pointed, slightly bent tip at the top; an olive-green patched cloak tied at the neck with a little bow knot, with a tan patch and a dusty-rose patch stitched on; a brown leather backpack with straps and a rolled dusty-rose bedroll with a spiral end on top; a crooked wooden staff with a knobbly hook at the top, from which hangs a small glass lantern with a warm glowing yellow flame. Same soft watercolor washes with fine dark-brown ink outlines as the reference, same cute and gentle mood.

Draw a WALK CYCLE of this one character as a sprite sheet: a grid of exactly 3 columns by 2 rows, six frames, all cells the same size, reading left to right then top to bottom. Every frame shows the whole character (hat, staff and lantern included) in SIDE VIEW facing RIGHT, walking at a relaxed steady pace, with the staff held in the hand nearest to the viewer and planted slightly ahead of the body, the lantern hanging from it and swinging. The six poses of one full walking cycle:
1. contact: the front foot has just touched the ground, the body is at its lowest
2. down: the body settles onto the front foot, the cloak swings forward
3. passing: the back foot passes under the body, the body is at its highest, the lantern swings backward
4. contact with the other foot
5. down on the other foot, the cloak swings forward
6. passing with the first foot, the body at its highest, the lantern swinging back

Keep the character exactly the same size and design in all six frames, with the feet on the same imaginary ground line in every cell and the body centred horizontally in its cell. Only the pose changes: a small body bob, the cloak and the hat tip swaying a little, the staff swinging slightly, the lantern swinging, the feet stepping. Leave clear empty space between the cells so that nothing touches or overlaps the cell edges. Completely flat, plain, pure white background (#FFFFFF) in the whole image; no ground, no map, no island, no dotted route, no shadow under the feet, no grid lines, no frame numbers, no text, no letters, no numbers, no watermarks.
```

## 檢查清單（出圖後先看這幾點）

- 恰好 6 格、3 欄 2 列，每格一個角色，沒有疊在一起或被切到邊。
- 六格的角色**大小一樣**、造型一樣（帽子尖角與綠帶、披風補丁、背包與粉色鋪蓋捲、手杖、提燈位置）。
- 全部朝同一個方向（朝右最好；朝左我會翻轉）。
- 腳在每格大約同一高度；背景是純白，沒有島、虛線、地面或陰影。
- 動作有明顯差別但不誇張：身體高低、披風擺動、提燈前後擺、雙腳交替。若六格幾乎一模一樣，請說「make the leg and body motion more distinct between frames」。

## 如果六格排版一直不穩：改成一格一格畫

先在同一對話裡讓它畫好**第 1 格**，滿意後用下面的句子逐格要求（{N}、{POSE} 換成對應的姿勢），每格各存一張，我再拼起來：

```
Same character, same style and same side view facing right as the six-frame sheet above. Redraw ONLY frame {N} of the walk cycle ({POSE}) on its own, as a single image with aspect ratio 1:1, the whole character centred, feet on the lower third line, same size as in the sheet, plain pure white background (#FFFFFF), no ground, no shadow, no text.
```

姿勢對照：1 contact（前腳剛著地、身體最低）、2 down（重心落在前腳、披風前擺）、3 passing（後腳經過身體下方、身體最高、提燈向後擺）、4 contact（換另一隻腳）、5 down（換腳）、6 passing（換腳、身體最高、提燈向後擺）。

## 限制（先知道）

AI 圖像工具很難讓六格完全一致，細節（補丁位置、星星、花紋）可能每格不同，縮到地圖上的小尺寸時通常看不出來。我的切圖工具只能校正大小和腳的位置，不能修正造型不一致。如果實在不一致，就維持目前程式做的動態（彈跳、搖擺、提燈發光），效果已經能用。
