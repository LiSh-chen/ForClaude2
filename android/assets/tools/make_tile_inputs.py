#!/usr/bin/env python3
"""Cuts the island painting (with its baked-in objects) into 21 tiles of 80 x 53 km (3:2, 15% overlap) for Gemini to refine.
Writes mapguide/tiles21/tileNN_input.jpg (2000x1333), tiles21.json (positions, needed for merging), TILES21_PROMPT.md.
Run from android/assets:  python3 tools/make_tile_inputs.py"""
import json, re, pathlib, numpy as np
from PIL import Image, ImageDraw
ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / 'generated/real_map_base_v3.jpg'
OUT = ROOT / 'mapguide/tiles21'; OUT.mkdir(parents=True, exist_ok=True)
im = Image.open(SRC).convert('RGB'); W, H = im.size            # 2000 x 3584 for LON 119.93..122.12, LAT 21.8..25.4
PXKM = W / 222.5
txt = (ROOT.parent / 'core/src/main/kotlin/com/starmist/core/route/TaiwanOutline.kt').read_text()
ll = [(float(a), float(b)) for a, b in re.findall(r'GeoPoint\(([\d.]+), ([\d.]+)\)', txt)]
mk = Image.new('L', (W, H), 0); ImageDraw.Draw(mk).polygon([((lo - 119.93) / 2.19 * W, (25.4 - la) / 3.6 * H) for la, lo in ll], fill=255); mk = np.asarray(mk) > 0
tw, th = 80 * PXKM, 80 * PXKM * 2 / 3
sx, sy = tw * 0.85, th * 0.85
tiles = []; y = 0.0
while y < H:
    x = 0.0
    while x < W:
        if mk[int(y):int(min(y + th, H)), int(x):int(min(x + tw, W))].sum() > 0.03 * tw * th: tiles.append([x, y])
        x += sx
    y += sy
meta = []
thumb = im.resize((W // 4, H // 4), Image.LANCZOS)
for i, (x, y) in enumerate(tiles, 1):
    x = min(max(x, 0), W - tw); y = min(max(y, 0), H - th)                 # keep inside the picture
    box = (round(x), round(y), round(x) + round(tw), round(y) + round(th))
    inp = im.crop(box).resize((2000, 1333), Image.LANCZOS)
    inp.save(OUT / f'tile{i:02d}_input.jpg', quality=95)
    lat0 = 25.4 - box[3] / H * 3.6; lat1 = 25.4 - box[1] / H * 3.6; lon0 = 119.93 + box[0] / W * 2.19; lon1 = 119.93 + box[2] / W * 2.19
    meta.append(dict(tile=i, box=box, out_size=[2000, 1333], lat=[round(lat0, 3), round(lat1, 3)], lon=[round(lon0, 3), round(lon1, 3)]))
    loc = thumb.copy(); d = ImageDraw.Draw(loc, 'RGBA'); b4 = [v / 4 for v in box]
    d.rectangle(b4, outline=(255, 0, 100, 255), width=3, fill=(255, 0, 100, 40)); loc.save(OUT / f'tile{i:02d}_locator.jpg', quality=85)
(OUT / 'tiles21.json').write_text(json.dumps(dict(source=str(SRC.name), size=[W, H], tiles=meta), indent=1))
print(len(tiles), 'tiles; tile =', round(tw), 'x', round(th), 'source px; output scale', round(2000 / tw, 2))
