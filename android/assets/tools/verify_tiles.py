#!/usr/bin/env python3
"""Identifies which tile each Gemini result is, registers it on the tile input, and checks it.
Usage (from android/assets):  python3 tools/verify_tiles.py result1.jpg result2.jpg ...
Saves generated/tiles21/tileNN.jpg (as returned, resized to 2000x1333) and writes the registered copy to $VERIFY_OUT (default /tmp) as tileNN_reg.png."""
import sys, os, json, pathlib, cv2, numpy as np
from PIL import Image
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import make_band_guides as G
ROOT = G.ROOT; OUTD = pathlib.Path(os.environ.get('VERIFY_OUT', '/tmp')); OUTD.mkdir(parents=True, exist_ok=True)
meta = json.loads((ROOT / 'mapguide/tiles21/tiles21.json').read_text()); SC = meta['size'][0] / G.W
sift = cv2.SIFT_create(8000)
inp = {}
for t in range(1, len(meta['tiles']) + 1):
    c = cv2.imread(str(ROOT / f'mapguide/tiles21/tile{t:02d}_input.jpg')); inp[t] = (c, sift.detectAndCompute(cv2.cvtColor(c, cv2.COLOR_BGR2GRAY), None))
rows = []
for path in sys.argv[1:]:
    im = Image.open(path).convert('RGB'); w0 = im.size[0]
    r = cv2.cvtColor(np.asarray(im), cv2.COLOR_RGB2BGR); r2 = cv2.resize(r, (2000, 1333), interpolation=cv2.INTER_AREA if r.shape[1] > 2000 else cv2.INTER_CUBIC)
    k2, d2 = sift.detectAndCompute(cv2.cvtColor(r2, cv2.COLOR_BGR2GRAY), None); best = None
    for t, (c, (k1, d1)) in inp.items():
        m = cv2.BFMatcher().knnMatch(d2, d1, k=2); good = [a for a, b in m if a.distance < 0.75 * b.distance]
        if len(good) < 12: continue
        src = np.float32([k2[a.queryIdx].pt for a in good]); dst = np.float32([k1[a.trainIdx].pt for a in good])
        A, inl = cv2.estimateAffinePartial2D(src, dst, ransacReprojThreshold=8)
        if A is not None and (best is None or inl.sum() > best[1]): best = (t, int(inl.sum()), A)
    if not best: rows.append((path, '?', 'no match')); continue
    t, ninl, A = best; reg = cv2.warpAffine(r2, A, (2000, 1333), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
    gi = cv2.cvtColor(inp[t][0], cv2.COLOR_BGR2GRAY).astype(np.float32); gr = cv2.cvtColor(reg, cv2.COLOR_BGR2GRAY).astype(np.float32)
    lo = lambda a: cv2.GaussianBlur(a, (0, 0), 10); d = np.abs(lo(gi) - lo(gr))
    box = meta['tiles'][t - 1]['box']; s = 2000 / (box[2] - box[0]); disp = []
    for o in G.objs:
        X, Y = o['x0'] * SC, o['y0'] * SC
        if not (box[0] <= X < box[2] and box[1] <= Y < box[3]): continue
        cx, cy = (X - box[0]) * s, (Y - box[1]) * s; h = int(o['w'] * SC * s * 0.8)
        if cx - h < 0 or cy - h < 0 or cx + h > 2000 or cy + h > 1333: continue
        a = gi[int(cy - h):int(cy + h), int(cx - h):int(cx + h)]; b = gr[int(cy - h):int(cy + h), int(cx - h):int(cx + h)]
        (dx, dy), _ = cv2.phaseCorrelate(a, b, cv2.createHanningWindow((2 * h, 2 * h), cv2.CV_32F)); disp.append((np.hypot(dx, dy) / (o['w'] * SC * s), o['id']))
    disp.sort(reverse=True)
    ok = ninl >= 60 and abs(np.hypot(A[0, 0], A[0, 1]) - 1) < 0.02 and (not disp or disp[0][0] < 0.15) and d.mean() < 8
    Image.fromarray(cv2.cvtColor(r2, cv2.COLOR_BGR2RGB)).save(ROOT / f'generated/tiles21/tile{t:02d}.jpg', quality=96)
    cv2.imwrite(str(OUTD / f'tile{t:02d}_reg.png'), reg)
    rows.append((t, w0, ninl, round(float(np.hypot(A[0, 0], A[0, 1])), 4), np.round(A[:, 2], 1).tolist(), round(float(d.mean()), 1), len(disp), (round(disp[0][0], 2), disp[0][1]) if disp else '-', 'OK' if ok else 'CHECK'))
print('tile | src width | inliers | scale | shift px | lowfreq diff | objects | worst object move (x width) | verdict')
for r in sorted(rows, key=lambda r: (str(r[0]))): print(*r, sep=' | ')
