# 旅行者休息動作：請 Gemini 畫逐格動作

## 怎麼用
每個動作各開一個新對話：先上傳兩張參考圖（角色參考圖_traveller_cute.jpg 與 站姿參考圖_側面.png），比例選 3:2、解析度選最高，貼上「共同提示詞」再接上該動作的段落。
出圖後各存成 動作名稱.png 傳給我（例如 1_伸懶腰.png）。五個動作不用一次全做，先做「打瞌睡」和「伸懶腰」也行。
若六格排版或頭部不一致，在同一對話裡說：「keep the head and face exactly the same in all six frames, only the body pose changes」。

## 共同提示詞
Create an image with aspect ratio 3:2. Use the uploaded pictures as the exact reference for the character: the first is the character design (a small round, glossy, milk-white spirit-like body with two tiny rounded stubby feet and short stubby arms ending in small white mitten hands; large glossy dark-brown eyes with white highlights, a small open smile and soft pink blush marks; a wide-brim cream hat with a dark-green band and a pointed, slightly bent tip; an olive-green patched cloak tied at the neck with a little bow, a tan patch and a dusty-rose patch; a brown leather backpack with a rolled dusty-rose bedroll on top; a crooked wooden staff with a knobbly hook from which hangs a small glass lantern with a warm glowing flame), the second is the exact side-view STANDING pose that every sequence must start from and end on. Same soft watercolor washes with fine dark-brown ink outlines, same cute and gentle mood.

Draw an ACTION SEQUENCE of this one character as a sprite sheet: a grid of exactly 3 columns by 2 rows, six frames of the same size, reading left to right then top to bottom. Every frame shows the whole character (hat, staff and lantern included) in SIDE VIEW facing RIGHT. Frame 1 and frame 6 are exactly the standing pose of the second reference picture, so the action begins and ends in the same pose. IMPORTANT consistency rules: keep the hat, the head, the face and the character's size and design identical in all six frames (the head always in the same three-quarter view as the reference, the same eyes and mouth style, only the expression may change as the action needs); only the body pose and the action change. Keep the feet (or the part resting on the ground) on the same ground line in every cell and the character horizontally centred in its cell. Leave clear empty space between the cells so nothing touches or overlaps the cell edges. Completely flat, plain, pure white background (#FFFFFF) in the whole image; no ground, no shadow, no grid lines, no frame numbers, no text, no letters, no symbols, no floating marks (no hearts, music notes, Zzz or sparkles - those are added later), no watermarks.


### 1_伸懶腰 ###
（接在共同提示詞後面）
THE ACTION: a big happy stretch. The six frames, in order: 1 standing (the reference); 2 rises onto tiptoes and starts lifting both stubby arms; 3 stretched up as tall as possible, both arms straight up above the hat brim, body elongated, eyes squeezed shut with a content smile; 4 holds the stretch, trembling slightly, leaning back a little; 5 lets the breath out, body relaxes and squashes down slightly, arms drop, a relieved face; 6 standing (the reference).

### 2_坐下休息 ###
（接在共同提示詞後面）
THE ACTION: sitting down for a rest. The six frames, in order: 1 standing (the reference); 2 wipes the forehead with one stubby hand, a small sweat drop on the head, tired but smiling; 3 bends its knees and lowers the body, the staff leaning against the shoulder; 4 sitting on the ground with the legs stretched forward, the backpack resting behind it, the staff leaning beside it, a relaxed happy sigh; 5 starts to get up, pushing off the ground with both hands; 6 standing (the reference). Keep the sitting frames inside the same cell size as the standing ones, with the ground line unchanged.

### 3_打瞌睡 ###
（接在共同提示詞後面）
THE ACTION: a yawn and a doze, then waking up with a start. The six frames, in order: 1 standing (the reference); 2 a big yawn, mouth wide open, eyes squeezed shut, one stubby hand rising toward the mouth; 3 eyelids heavy, half-closed eyes, the body sagging forward a little and the hat tilting down; 4 asleep standing up: eyes closed with a peaceful smile, the head and hat nodding forward, leaning on the staff; 5 jolts awake: eyes wide open, surprised mouth, a small hop with the feet just off the ground; 6 standing (the reference).

### 4_好奇張望 ###
（接在共同提示詞後面）
THE ACTION: curious looking around, then noticing something. The six frames, in order: 1 standing (the reference); 2 tilts the whole upper body and the face upward, looking at the sky, eyes wide and shiny; 3 stands on tiptoe shading its eyes with one stubby hand, looking far ahead; 4 turns toward the viewer in three-quarter view, one stubby hand raised, delighted open mouth as if it found something; 5 gives a small excited hop with both feet off the ground; 6 standing (the reference).

### 5_開心哼歌 ###
（接在共同提示詞後面）
THE ACTION: humming happily and swaying to its own tune. The six frames, in order: 1 standing (the reference); 2 sways its body to the left (toward the back), the staff swinging slightly, eyes closed in a contented smile; 3 hops with both feet off the ground, arms out to the sides; 4 lands and sways to the right (toward the front), the lantern swinging forward; 5 a second small hop, cheerful open mouth as if singing; 6 standing (the reference).


## 收到圖之後（已完成）
五張圖存在 assets/generated/idle_sheets/（idle_stretch / look / hum / nap / sit.jpg）。切圖用
`python3 assets/tools/traveller_rig/5_slice_idle.py <動作> <圖> <輸出資料夾> --fit 0.97`，會輸出 art_idle_<動作>_0..20.webp（六個畫好的姿勢加上用光流算出的中間畫面，半尺寸；各格會依燈籠大小統一人物尺寸）
（放進 app/src/main/res/drawable-nodpi）與 contact_<動作>.png（目視檢查）。動作名稱對應 WalkerIdle.kt 的 Idle.NAMES。
圖中第二列的角色畫得比第一列大，腳本以每列的站姿格為準各自縮放；整張圖一起去背，所以越過格線的手杖與燈籠不會被切斷。
