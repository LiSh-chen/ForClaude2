#!/usr/bin/env python3
"""Cuts the object sheets from Gemini into single icons (white background kept: the app draws them with Multiply)."""
import json, pathlib
import numpy as np
from PIL import Image
ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT.parent / 'app/src/main/res/drawable-nodpi'
man = json.loads((ROOT / 'objects_manifest.json').read_text())
CAPTIONS = {'O3', 'O4'}
cells = {}

def remove_captions(cell):
    """Gemini printed captions around icons: keep only the vertical run of ink rows with the most ink (gaps > 8 rows split runs)."""
    arr = np.asarray(cell).copy()
    ink = (arr.min(axis=2) < 236).sum(axis=1)
    ys = np.where(ink > 2)[0]
    if len(ys) == 0: return cell
    runs, start, prev = [], ys[0], ys[0]
    for y in ys[1:]:
        if y - prev > 8: runs.append((start, prev)); start = y
        prev = y
    runs.append((start, prev))
    best = max(runs, key=lambda r: ink[r[0]:r[1] + 1].sum())
    arr[:best[0]] = 255; arr[best[1] + 1:] = 255
    return Image.fromarray(arr)
for sheet in ('O1', 'O2', 'O3', 'O4'):
    im = Image.open(ROOT / f'generated/objsheets/{sheet.lower()}.jpg').convert('RGB')
    W, H = im.size; cw, ch = W / 6, H / 4
    items = [m for m in man if m['sheet'] == sheet]
    for it in items:
        k = it['cell'] - 1; r, c = divmod(k, 6)
        x0, y0 = int(c * cw), int(r * ch)
        h = int(ch * 1.0)
        cell = im.crop((x0, y0, int(x0 + cw), y0 + h))
        if sheet in CAPTIONS:
            cell = remove_captions(cell)
        a = np.asarray(cell).min(axis=2)
        ys, xs = np.where(a < 236)
        if len(xs) < 50: print('EMPTY', it); continue
        bx0, bx1, by0, by1 = max(xs.min() - 6, 0), min(xs.max() + 6, cell.width), max(ys.min() - 6, 0), min(ys.max() + 6, cell.height)
        ic = cell.crop((bx0, by0, bx1, by1))
        s = 360 / max(ic.size)
        if s < 1: ic = ic.resize((round(ic.width * s), round(ic.height * s)), Image.LANCZOS)
        out = RES / f"art_obj_{it['id']}.webp"
        ic.save(out, quality=90)
        cells[it['id']] = ic
print(len(cells), 'icons')
# contact sheet for checking
ids = list(cells)
cols = 10; rows = (len(ids) + cols - 1) // cols
sheet = Image.new('RGB', (cols * 200, rows * 170), 'white')
for i, k in enumerate(ids):
    ic = cells[k].copy(); ic.thumbnail((190, 160)); sheet.paste(ic, ((i % cols) * 200 + 5, (i // cols) * 170 + 5))
sheet.save('/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/obj_contact.png')
