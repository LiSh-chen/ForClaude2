#!/usr/bin/env python3
"""Cuts the island ground painting into three overlapping bands and, for each, draws a guide with numbered footprints.

Gemini is asked to paint the objects INTO the ground painting at those footprints (and upscale the band at the same time).
Run from android/assets:  python3 tools/make_band_guides.py
Writes mapguide/bands/band{1,2,3}_clean.png, band{n}_guide.png, bands.json and ISLAND_OBJECTS_PROMPT.md
"""
import json, pathlib, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import make_tiles as T
import make_object_prompts as P   # EXTRA descriptions

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / 'mapguide/bands'; OUT.mkdir(parents=True, exist_ok=True)
LON0, LON1, LAT0, LAT1 = 119.93, 122.12, 21.8, 25.4
base = Image.open(ROOT / 'generated/island_ground.png').convert('RGB')   # 1116 x 2000
W, H = base.size
def px(lat, lon): return ((lon - LON0) / (LON1 - LON0) * W, (LAT1 - lat) / (LAT1 - LAT0) * H)
KM = W / 222.5   # base pixels per km

line, cards, pois = T.parse_route()
objs = []   # id, name, desc, x, y, width_px, weight
for cid, (name, lat, lon) in cards.items():
    if cid in T.SIGHTS: x, y = px(lat, lon); objs.append(dict(id=cid, name=name, desc=T.SIGHTS[cid], x=x, y=y, w=10.5 * KM, weight=0.15, kind='landmark'))
for pid, (name, lat, lon) in pois.items():
    if pid in T.POIS: x, y = px(lat, lon); objs.append(dict(id=pid, name=name, desc=T.POIS[pid], x=x, y=y, w=7.5 * KM, weight=0.8, kind='sight'))
EXTRA_POS = [(23.28, 120.31), (23.55, 120.43), (23.31, 120.33), (24.70, 120.88), (24.34, 120.60), (23.57, 120.30), (22.40, 120.58)]
for i, (n, la, lo, d) in enumerate(T.LOCAL):
    x, y = px(la, lo); objs.append(dict(id=f'local_{i}', name=n, desc=d, x=x, y=y, w=10 * KM, weight=1.0, kind='local'))
for i, ((n, d), (la, lo)) in enumerate(zip(P.EXTRA, EXTRA_POS)):
    x, y = px(la, lo); objs.append(dict(id=f'extra_{i}', name=n, desc=d, x=x, y=y, w=9 * KM, weight=1.0, kind='local'))
for o in objs: o['h'] = o['w'] * 0.8; o['x0'], o['y0'] = o['x'], o['y']

# push overlapping footprints apart (landmarks move least), never far from the true spot
for _ in range(30):
    for i, a in enumerate(objs):
        for b in objs[i + 1:]:
            ox = (a['w'] + b['w']) / 2 * 1.05 - abs(a['x'] - b['x']); oy = (a['h'] + b['h']) / 2 * 1.05 - abs(a['y'] - b['y'])
            if ox <= 0 or oy <= 0: continue
            s = a['weight'] + b['weight']
            if ox < oy:
                d = -1 if a['x'] <= b['x'] else 1; a['x'] += d * ox * a['weight'] / s; b['x'] -= d * ox * b['weight'] / s
            else:
                d = -1 if a['y'] <= b['y'] else 1; a['y'] += d * oy * a['weight'] / s; b['y'] -= d * oy * b['weight'] / s
    for o in objs:
        o['x'] = min(max(o['x'], o['x0'] - o['w'] * 1.2), o['x0'] + o['w'] * 1.2); o['y'] = min(max(o['y'], o['y0'] - o['h'] * 1.2), o['y0'] + o['h'] * 1.2)

# three bands of 3:2 (1116 x 744) with 116 px overlap; seams sit where no footprint is
BH = round(W * 2 / 3)
starts = [0, (H - BH) // 2, H - BH]
def free_seam(lo, hi):
    best = None
    for y in range(int(lo), int(hi)):
        clear = min(abs(y - o['y']) - o['h'] / 2 for o in objs)
        if best is None or clear > best[0]: best = (clear, y)
    return best[1]
seams = [free_seam(starts[1] + 20, starts[0] + BH - 20), free_seam(starts[2] + 20, starts[1] + BH - 20)]
terr = [(0, seams[0]), (seams[0], seams[1]), (seams[1], H)]
SC = 2000 / W
font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 26)
meta = []; prompts = []
for n, (s, (t0, t1)) in enumerate(zip(starts, terr), 1):
    clean = base.crop((0, s, W, s + BH)).resize((2000, round(BH * SC)), Image.LANCZOS)
    clean.save(OUT / f'band{n}_clean.png')
    guide = clean.copy(); d = ImageDraw.Draw(guide, 'RGBA')
    mine = sorted([o for o in objs if t0 <= o['y'] < t1], key=lambda o: (o['y'], o['x']))
    lines = []
    for k, o in enumerate(mine, 1):
        cx, cy = o['x'] * SC, (o['y'] - s) * SC; rw, rh = o['w'] * SC / 2, o['h'] * SC / 2
        d.ellipse((cx - rw, cy - rh, cx + rw, cy + rh), outline=(255, 0, 170, 255), width=4, fill=(255, 0, 170, 40))
        tw = d.textlength(str(k), font=font); d.text((cx - tw / 2, cy - 15), str(k), fill=(255, 0, 170, 255), font=font, stroke_width=3, stroke_fill=(255, 255, 255, 255))
        lines.append(f'{k}. {o["name"]}: {o["desc"]}  (footprint about {round(o["w"] * SC)} px wide)')
        o.setdefault('band', n); o['num'] = k
    guide.save(OUT / f'band{n}_guide.png')
    meta.append(dict(band=n, start_y=s, height=BH, terr=[t0, t1], scale=SC, objects=[dict(id=o['id'], num=o['num'], x=o['x'], y=o['y'], w=o['w']) for o in mine]))
    prompts.append((n, len(mine), lines, s, t0, t1))
(OUT / 'bands.json').write_text(json.dumps(dict(base_size=[W, H], seams=seams, bands=meta), indent=1))

md = ['# 真實世界地圖：把物件直接畫進地圖（分三段、順便提高畫質）', '',
      '做法：把整張底圖切成上、中、下三段（各 3:2，彼此有重疊），每一段我已經標好「哪個位置要畫什麼」。',
      'Gemini 這一步**同時**做兩件事：① 把這一段放大、畫得更精細；② 把物件畫進地圖裡，跟地形融為一體（沒有框、沒有白底、沒有貼圖感）。',
      '位置、大小都由我先定好（粉紅圈），所以不會再有「物件跟地圖分離」或位置歪掉的問題。', '',
      '## 每一段都要上傳的圖', '',
      '- 圖 A：`mapguide/bands/bandN_clean.png`（乾淨的底圖，這是要被保留、放大、加料的那張）',
      '- 圖 B：`mapguide/bands/bandN_guide.png`（同一張，但有粉紅色編號圓圈，只用來看「位置與大小」）', '',
      '比例選 **3:2 橫式**，解析度選最高。每段一個新對話，三段的提示詞前半相同。', '',
      '## 通用提示詞（每段都貼，後面接該段的物件清單）', '', '```',
      'I upload two images of the same map section. Image A is an antique hand-drawn atlas painting of part of Taiwan (the ground only). Image B is the SAME picture with numbered pink circles that show where objects must be painted.',
      'Task: output a refined, higher-resolution version of image A at the same framing (3:2, the highest resolution you can), in exactly the same hand-drawn style (fine dark-brown pen line work, light watercolour wash, paper grain).',
      '1. Keep EXACTLY the same coastline, rivers, mountains, fields and colours, and the same position of every feature. Do not move, crop, rotate or re-compose anything. Add finer detail only.',
      '2. At each pink circle, paint the listed object as part of the map itself: drawn onto the terrain, sized to fill its circle (about 90% of its width), same slightly raised pictorial viewpoint, same line work and colour wash. Let its edges melt into the surrounding ground (grass, sand, water, hills) so that it looks painted in the same sitting as the map. NO frame, NO white background, NO separate sticker or vignette, NO drop shadow.',
      '3. The final image must contain NO pink circles, NO numbers and NO text of any kind. Do not add any object that is not listed. Leave the parts of the map without a circle as they are (plain terrain, no extra buildings).',
      '```', '']
for n, cnt, lines, s, t0, t1 in prompts:
    md += [f'## 第 {n} 段（{cnt} 個物件）', '', '```', 'The objects, by circle number:'] + lines + ['```', '',
           f'存檔：`generated/bands/band{n}.png`', '']
md += ['## 交給我之後', '',
       '我把三段按重疊區對齊、在沒有物件的接縫線上漸變合併，成為一張大圖；你可以先看合併圖，再看遊戲裡的畫面。',
       '如果某個物件畫歪、漏畫，我會指出編號，請你只對那一段再生成一次。', '']
(ROOT / 'ISLAND_OBJECTS_PROMPT.md').write_text('\n'.join(md))
print('seams', seams, [(p[0], p[1]) for p in prompts], 'objects', len(objs))
