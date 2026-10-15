#!/usr/bin/env python3
"""Pastes the cut-out object icons onto the three ground bands at their exact footprints (positions can not drift this way).
Gemini is then only asked to harmonise the edges (see ISLAND_HARMONIZE_PROMPT.md).  Run from android/assets."""
import pathlib, sys
from PIL import Image, ImageFilter
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import make_band_guides as G

RES = G.ROOT.parent / 'app/src/main/res/drawable-nodpi'
for n, (s, (t0, t1)) in enumerate(zip(G.starts, G.terr), 1):
    img = Image.open(G.OUT / f'band{n}_clean.png').convert('RGBA')
    mine = sorted([o for o in G.objs if t0 <= o['y'] < t1], key=lambda o: o['y'])
    for o in mine:
        ic = Image.open(RES / f"art_obj_{o['id']}.webp").convert('RGBA')
        iw = round(o['w'] * G.SC * 1.15); ih = round(iw * ic.height / ic.width)
        ic = ic.resize((iw, ih), Image.LANCZOS)
        x, y = round(o['x'] * G.SC - iw / 2), round((o['y'] - s) * G.SC - ih / 2)
        shadow = Image.new('RGBA', ic.size, (60, 45, 30, 0)); shadow.putalpha(ic.getchannel('A').point(lambda v: int(v * 0.35)).filter(ImageFilter.GaussianBlur(5)))
        img.alpha_composite(shadow, (max(x + 4, 0), max(y + 6, 0))) if x + 4 >= 0 and y + 6 >= 0 else None
        img.alpha_composite(ic, (x, y)) if x >= 0 and y >= 0 and x + iw <= img.width and y + ih <= img.height else img.paste(ic, (x, y), ic)
    img.convert('RGB').save(G.OUT / f'band{n}_composite.png')
    print(n, len(mine))
