# 旅行者走路動畫：畫一張「走路連續動作」圖

App 已經會用程式讓旅行者彈跳、搖擺、擺動手杖（見 `tools/traveller_rig/`）。要升級成**真正逐格的走路動畫**，需要同一個角色的 6 個走路姿勢。做法：一次畫成一張 3 欄 × 2 列的格子圖，我再切開、去背、對齊腳底、接進 App。

## 怎麼做

1. 新開對話，**先上傳角色設定圖** `android/assets/generated/traveller_sheet.jpg`（你做的三視圖），當角色參考。
2. 貼下面的提示詞，比例選 **3:2**，解析度選最高。
3. 檢查結果（見下面「檢查清單」）；不合格就在同一對話裡說明要改什麼，或重生成。
4. 存成 `android/assets/generated/traveller_walk_sheet.png` 傳給我。我會執行 `python3 tools/traveller_rig/3_slice_walk.py generated/traveller_walk_sheet.png <輸出資料夾> --rows 2 --cols 3`；角色若是朝左，加 `--flip`（App 要朝右）。

## 提示詞

```
Create an image with aspect ratio 3:2. Use the uploaded character sheet as the exact reference for the character: the same small round glossy white spirit-like body with large black eyes, the same wide-brim cream hat with a green band, the same patched green cloak with small gold stars and orange lining, the same large leather backpack with rolled bedroll, the same crooked wooden staff with a hanging glowing lantern, the same tiny stubby feet. Same pen-and-ink linework and soft watercolor style as the reference.

Draw a WALK CYCLE of this one character as a sprite sheet: a grid of exactly 3 columns by 2 rows, six frames, all the same size cell, reading left to right then top to bottom. Every frame shows the character in SIDE VIEW facing RIGHT, whole body including the hat, staff and lantern, walking at a relaxed steady pace. The six poses of one full walking cycle:
1. contact: the front foot just touches the ground, body at its lowest
2. down: body settles onto the front foot, cloak swings forward
3. passing: the back foot passes under the body, body at its highest, lantern swings backward
4. contact with the other foot
5. down on the other foot, cloak swings forward
6. passing with the first foot, body at its highest, lantern swings back

Keep the character exactly the same size and design in all six frames, with the feet on the same imaginary ground line in every cell and the body centred horizontally in its cell. Only the pose changes: a small body bob, the cloak and hat brim swaying a little, the staff held in the same hand but swinging slightly, the lantern swinging, feet stepping. Leave clear empty space between cells so nothing touches or overlaps the cell edges. Completely flat plain pure white background (#FFFFFF) in the whole image, no ground, no shadow, no grid lines, no frame numbers, no text, no letters, no numbers, no watermarks.
```

## 檢查清單（出圖後先看這幾點）

- 恰好 6 格、3 欄 2 列，每格一個角色，沒有疊在一起或被切到邊。
- 六格的角色**大小一樣**、造型一樣（帽子、披風星星與補丁、背包、手杖、提燈位置）；若某格變大變小，請重生成那一格的對話。
- 全部朝同一個方向（朝右最好；朝左我會翻轉）。
- 腳在每格大約同一高度；背景是純白、沒有地面與陰影。
- 動作有明顯差別但不誇張：身體高低、披風擺動、提燈前後擺。若六格幾乎一模一樣，請在對話裡說「make the leg and body motion more distinct between frames」。

## 限制（先知道）

AI 圖像工具很難讓六格完全一致，細節（星星、補丁、花紋）可能每格不同，縮到地圖上的小尺寸時通常看不出來。我的切圖工具只能校正大小和腳的位置，不能修正造型不一致。如果實在不一致，就維持目前程式做的動態（彈跳、搖擺、手杖擺動），那個效果已經能用。
