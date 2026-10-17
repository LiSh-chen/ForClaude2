#!/usr/bin/env python3
"""Merges the 21 refined tiles into one big map (5560 x 9970): registers each result on its input, matches colours in the overlaps,
blends with feathered weights (border-distance), and falls back to the upscaled base where a tile is unusable.
Run from android/assets:  python3 tools/merge_tiles21.py  -> generated/real_map_hi.jpg (+ preview)"""
import sys, json, pathlib, cv2, numpy as np
from scipy.ndimage import distance_transform_edt
ROOT = pathlib.Path(__file__).resolve().parent.parent
meta = json.loads((ROOT / 'mapguide/tiles21/tiles21.json').read_text()); W0, H0 = meta['size']
S = 2000 / (meta['tiles'][0]['box'][2] - meta['tiles'][0]['box'][0]); W, H = round(W0 * S), round(H0 * S)
print('canvas', W, H, 'scale', round(S, 3))
sift = cv2.SIFT_create(8000)
base = cv2.imread(str(ROOT / 'generated/real_map_base_v3.jpg')); base = cv2.resize(base, (W, H), interpolation=cv2.INTER_CUBIC)
# bad rows / margins per tile (Gemini artefacts): (tile, y0, y1) in tile pixels set to zero weight
BAD = {1: [(0, 70)], 2: [(0, 70)], 16: [(585, 615)]}
tiles = []
for t in meta['tiles']:
    n = t['tile']; inp = cv2.imread(str(ROOT / f'mapguide/tiles21/tile{n:02d}_input.jpg')); res = cv2.imread(str(ROOT / f'generated/tiles21/tile{n:02d}.jpg'))
    res = cv2.resize(res, (2000, 1333), interpolation=cv2.INTER_AREA if res.shape[1] > 2000 else cv2.INTER_CUBIC)
    k1, d1 = sift.detectAndCompute(cv2.cvtColor(inp, cv2.COLOR_BGR2GRAY), None); k2, d2 = sift.detectAndCompute(cv2.cvtColor(res, cv2.COLOR_BGR2GRAY), None)
    m = cv2.BFMatcher().knnMatch(d2, d1, k=2); good = [a for a, b in m if a.distance < 0.75 * b.distance]
    A, _ = cv2.estimateAffinePartial2D(np.float32([k2[a.queryIdx].pt for a in good]), np.float32([k1[a.trainIdx].pt for a in good]), ransacReprojThreshold=6)
    reg = cv2.warpAffine(res, A, (2000, 1333), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
    valid = np.ones((1333, 2000), np.float32)
    # the affine may leave border-replicated margins: fade them out
    valid[:8] = valid[-8:] = 0; valid[:, :8] = valid[:, -8:] = 0
    for (a, b) in BAD.get(n, []): valid[a:b] = 0
    w = distance_transform_edt(valid > 0).astype(np.float32); w = np.minimum(w / 150.0, 1.0) ** 1.5 * (valid > 0)
    x0, y0 = round(t['box'][0] * S), round(t['box'][1] * S)
    tiles.append(dict(n=n, img=reg.astype(np.float32), w=w, x0=x0, y0=y0))
# per-tile gain (per channel) so neighbours agree in overlaps: a few Gauss-Seidel sweeps on log gains against the neighbours
gains = {t['n']: np.ones(3, np.float32) for t in tiles}
for _ in range(6):
    for t in tiles:
        num = np.zeros(3); den = 0
        for u in tiles:
            if u is t: continue
            ax0, ay0 = max(t['x0'], u['x0']), max(t['y0'], u['y0']); ax1, ay1 = min(t['x0'] + 2000, u['x0'] + 2000), min(t['y0'] + 1333, u['y0'] + 1333)
            if ax1 - ax0 < 60 or ay1 - ay0 < 60: continue
            a = t['img'][ay0 - t['y0']:ay1 - t['y0'], ax0 - t['x0']:ax1 - t['x0']]; b = u['img'][ay0 - u['y0']:ay1 - u['y0'], ax0 - u['x0']:ax1 - u['x0']]
            wm = (t['w'][ay0 - t['y0']:ay1 - t['y0'], ax0 - t['x0']:ax1 - t['x0']] * u['w'][ay0 - u['y0']:ay1 - u['y0'], ax0 - u['x0']:ax1 - u['x0']])
            if wm.sum() < 1000: continue
            ma = (a * wm[..., None]).sum((0, 1)) / wm.sum(); mb = (b * wm[..., None]).sum((0, 1)) / wm.sum()
            num += np.log((mb * gains[u['n']] + 1) / (ma * gains[t['n']] + 1)) * wm.sum() ; den += wm.sum()
        if den: gains[t['n']] = gains[t['n']] * np.exp(np.clip(num / den, -0.2, 0.2) * 0.8).astype(np.float32)
mean = np.mean([g for g in gains.values()], axis=0)
for k in gains: gains[k] = gains[k] / mean
print('gain range', min(g.min() for g in gains.values()), max(g.max() for g in gains.values()))
out = np.zeros((H, W, 3), np.float32); wsum = np.zeros((H, W), np.float32)
for t in tiles:
    x0, y0 = t['x0'], t['y0']; x1, y1 = min(x0 + 2000, W), min(y0 + 1333, H)
    img = np.clip(t['img'][:y1 - y0, :x1 - x0] * gains[t['n']], 0, 255); w = t['w'][:y1 - y0, :x1 - x0]
    out[y0:y1, x0:x1] += img * w[..., None]; wsum[y0:y1, x0:x1] += w
eps = 1e-3; tw = np.clip(wsum, 0, 1)[..., None]
res = (out / np.maximum(wsum, eps)[..., None]) * tw + base.astype(np.float32) * (1 - tw)
res = np.clip(res, 0, 255).astype(np.uint8)
cv2.imwrite(str(ROOT / 'generated/real_map_hi.jpg'), res, [cv2.IMWRITE_JPEG_QUALITY, 92])
cv2.imwrite(str(ROOT / 'generated/real_map_hi_preview.jpg'), cv2.resize(res, (W // 6, H // 6), interpolation=cv2.INTER_AREA), [cv2.IMWRITE_JPEG_QUALITY, 90])
print('uncovered fraction', float((wsum < 0.05).mean()))
