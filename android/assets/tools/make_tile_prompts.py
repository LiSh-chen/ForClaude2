#!/usr/bin/env python3
"""Writes TILES21_PROMPT.md: the general prompt plus, for every tile, the objects that sit in it. Run after make_tile_inputs.py."""
import json, pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import make_band_guides as G
ROOT = G.ROOT
SAVE = lambda n: f'After generating, save the final image to my Google Drive in the folder "AI cooperate" (My Drive / AI cooperate), with the exact file name tile{n:02d}.png (no extra words in the name). If a file with that name already exists, replace it.'
meta = json.loads((ROOT / 'mapguide/tiles21/tiles21.json').read_text()); SC = meta['size'][0] / G.W
md = ['# 真實世界地圖：21 張切圖細畫（每張 80×53 公里）', '',
 '目的：把目前的全島圖（含景點物件）放大、細畫，畫質約提高 2.8 倍，並解決「放大後地貌/物件糊掉」的問題。',
 '位置不能動，所以每張都給你**我切好的原圖**，Gemini 只做「同一張圖的精修放大」。', '',
 '## 每一張的做法', '',
 '1. 開新對話（每張一個），比例 **3:2 橫式**，解析度選最高。',
 '2. 上傳兩張圖：`tileNN_input.jpg`（要精修的那張）和 `tileNN_locator.jpg`（小圖，粉紅框＝這一張在全島的位置，只給你參考，**不要求 Gemini 畫它**；想省事可以不傳）。',
 '3. 貼下面的「通用提示詞」，後面接該張的「物件清單」。',
 '4. 每張提示詞最後一行（After generating, save…）會要求 Gemini 把成品直接存到你的雲端硬碟 **我的雲端硬碟／AI cooperate**，檔名固定為 `tileNN.png`（NN 是兩位數編號，和輸入檔一致）；我之後從那裡取檔。若 Gemini 沒存成功或檔名不對，請「下載原尺寸」後手動存進同一資料夾並改成這個檔名。', '',
 '## 通用提示詞（每張都貼）', '', '```',
 'The uploaded image is a section of an antique hand-drawn atlas map of Taiwan (about 80 x 53 km of ground). It is slightly blurry because it was enlarged. Small pictorial illustrations (buildings, harbours, temples, fields, lighthouses, etc.) are already placed on the map, and they still look a little like pasted stickers.',
 'Task: return the SAME image, same framing, same 3:2 ratio, at the highest resolution you can, as a crisp, finely detailed repaint in exactly the same hand-drawn style (fine dark-brown pen line work, light watercolour wash, paper grain).',
 '1. Do NOT change the composition. Every coastline, river, road, field outline, mountain ridge, forest patch, colour area and every illustration must stay in exactly the same position, with the same size and shape. Do not move, crop, rotate, mirror, add or remove anything. Nothing may be re-composed.',
 '2. Add only finer detail: sharper pen lines, finer hatching on slopes, more refined tree and field texture, cleaner coast lines and sea hatching, crisp paper grain.',
 '3. Redraw each illustration with more detail but the same subject (listed below) and let its edges melt into the surrounding ground (grass into grass, sand into sand, water into sea or river), so that it no longer looks like a sticker: no light patch around it, no frame, a very soft contact shadow only.',
 '4. Do not add any text, numbers, labels, frames, scale bars, compasses or watermarks. Do not add buildings, houses or objects that are not already in the image.',
 '5. If something is cut by the image border, keep it cut at the border exactly as it is.',
 '6. Keep every illustration at the SAME size and footprint it has in the uploaded image: do not enlarge, shrink or extend any illustration, and do not let it spread over fields, coast or sea around it (for example a harbour must stay inside its current outline and must not turn land into water).',
 '```', '']
for t in meta['tiles']:
    x0, y0, x1, y1 = [v / SC for v in t['box']]        # in base px (1116 x 2000)
    inside = [o for o in G.objs if x0 <= o['x0'] < x1 and y0 <= o['y0'] < y1]
    inside.sort(key=lambda o: (round((o['y0'] - y0) / (y1 - y0) * 3), o['x0']))
    lines = []
    for o in inside:
        fx, fy = (o['x0'] - x0) / (x1 - x0), (o['y0'] - y0) / (y1 - y0)
        col = '左' if fx < 1/3 else ('中' if fx < 2/3 else '右'); row = '上' if fy < 1/3 else ('中' if fy < 2/3 else '下')
        lines.append(f'- ({row}{col}) {o["name"]}: {o["desc"]}')
    md += [f'## 第 {t["tile"]:02d} 張　緯度 {t["lat"][0]}–{t["lat"][1]}°N，經度 {t["lon"][0]}–{t["lon"][1]}°E（{len(inside)} 個物件）', '',
           '```', 'The illustrations in this section (position in the picture, subject):' if inside else 'There are no illustrations in this section; it is plain terrain and sea.'] + lines + ['', SAVE(t['tile']), '```', '']
md += ['## 交給我之後', '',
 '我會逐張自動檢查：對位（縮放、位移、殘差）、整塊有沒有被重畫、每個物件有沒有被搬動，列成一張表給你；通過的我再合併成一張大圖（接縫用多頻段混合，兩張重疊的色調也會對齊）。不通過的只需重做那一張。', '']
(ROOT / 'TILES21_PROMPT.md').write_text('\n'.join(md))
print('ok', sum(l.startswith('- (') for l in md), 'object lines')
