#!/usr/bin/env python3
"""Writes MAP_OBJECTS_PROMPTS.md and objects_manifest.json: the object sheets Gemini should draw (24 icons per sheet)."""
import json, pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import make_tiles as T

ROOT = pathlib.Path(__file__).resolve().parent.parent
line, cards, pois = T.parse_route()
EXTRA = [
 ('Water buffalo', 'a water buffalo standing in a flooded rice paddy with an egret'),
 ('Pineapple field', 'a pineapple field with rows of spiky plants'),
 ('Lotus pond', 'a lotus pond with pink flowers and large round leaves'),
 ('Roadside Earth God shrine', 'a small roadside Earth God shrine with incense and a banyan tree'),
 ('Railway bridge', 'a train crossing a small iron bridge over a river'),
 ('Temple fair', 'a small roadside temple fair with colourful banners and lanterns'),
 ('Betel-nut stand', 'a roadside betel-nut stand with a tiny shop and tall palms'),
]
sheets = []
land = [(cid, name, SIGHT) for cid, (name, lat, lon) in cards.items() for SIGHT in [T.SIGHTS.get(cid)] if SIGHT]
sheets.append(('O1', '景點地標（1–24）', [(c, n, d, 'landmark') for c, n, d in land[:24]]))
sheets.append(('O2', '景點地標（25–46）', [(c, n, d, 'landmark') for c, n, d in land[24:]]))
minor = [(pid, name, T.POIS[pid], 'minor') for pid, (name, lat, lon) in pois.items() if pid in T.POIS]
sheets.append(('O3', '路上小景點（24 個）', minor))
local = [(f'local_{i}', n, d, 'local') for i, (n, la, lo, d) in enumerate(T.LOCAL)] + [(f'extra_{i}', n, d, 'local') for i, (n, d) in enumerate(EXTRA)]
sheets.append(('O4', '沿路當地特色（24 個）', local))

PREFIX = ("Antique atlas illustration in the same hand-drawn style as the two uploaded reference images: fine dark-brown pen-and-ink line work with a light watercolour wash in muted greens, ochres, browns and soft blues, slightly imperfect lines, gentle paper-like grain, no digital gloss, no neon colours, no 3D rendering. Seen from a slightly raised top-down view like an old pictorial map. Plain pure white background. No text, no letters, no numbers, no frames, no ground shadows.")
md = ['# 真實世界地圖：物件素材（給 Gemini 的提示詞）', '',
      '物件（景點地標、路上小景點、當地特色）一共 **4 張**，每張 24 個，6 欄×4 列。',
      '位置全部由我按真實座標放，所以每個小圖只要畫好「樣子」，不需要管它在哪裡。', '',
      '## 通則', '',
      '1. 每個對話上傳兩張**風格參考圖**：`previews/2_compass.jpg` 和 `generated/real_map_base.png`。',
      '2. 比例選 **3:2 橫式**，解析度選最高。每個小圖最後在螢幕上只有約 100 像素大，所以一格 300 像素就夠。',
      '3. **背景純白**，格子之間留足空白，不要地面陰影、邊框、格線、文字、編號。（我用「正片疊底」把白色去掉，不用去背。）',
      '4. 格子由左到右、由上到下；每格大小一致，主體置中，互不重疊，主體佔格子約 70%。',
      '5. 一次畫 24 個，Gemini 偶爾會漏畫或畫錯。收到後我會逐格檢查，錯的那幾個再補畫（我會告訴你是哪幾格）。', '',
      '### 風格前綴（每個提示詞前面都貼）', '', '> ' + PREFIX, '']
manifest = []
for sid, title, items in sheets:
    lines = []
    for i, (iid, name, desc, kind) in enumerate(items, 1):
        lines.append(f'{i}. {name}: {desc}')
        manifest.append({'sheet': sid, 'cell': i, 'id': iid, 'name': name, 'kind': kind})
    empty = 24 - len(items)
    tail = f' The last {empty} cell(s) are left empty (plain white).' if empty else ''
    md += [f'## {sid}　{title}', '',
           '```',
           f'A sheet of {len(items)} separate pictorial map icons, 6 columns by 4 rows, in reading order, each centred in its own equal cell with generous white space around it. Each icon shows the subject as a small illustration with a tiny irregular patch of watercolour ground under it, like a drawing on an antique map, recognisable when printed about 2 cm wide. Same scale, same slightly raised viewing angle and same style for every icon. Do not draw any text, signs with writing, numbers or borders.{tail}',
           'The icons, in order:'] + lines + ['```', '', f'存檔：`generated/objects_{sid.lower()}.png`', '']
md += ['## 交給我之後', '', '我會把每一格切開、白色當透明，依真實座標放到地圖上，先給你看 3 到 4 個地點的效果。', '']
(ROOT / 'MAP_OBJECTS_PROMPTS.md').write_text('\n'.join(md))
(ROOT / 'objects_manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=1))
print({s[0]: len(s[2]) for s in sheets})
