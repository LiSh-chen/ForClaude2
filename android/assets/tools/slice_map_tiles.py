#!/usr/bin/env python3
"""Cuts generated/real_map_hi.jpg into 1024 px squares: app/src/main/res/drawable-nodpi/art_maptile_<row>_<col>.webp, and
writes the low-res fallback art_real_map_base.webp (the whole map at 2000 px wide). Run from android/assets."""
import pathlib, cv2
ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT.parent / 'app/src/main/res/drawable-nodpi'; T = 1024
for old in RES.glob('art_maptile_*.webp'): old.unlink()
im = cv2.imread(str(ROOT / 'generated/real_map_hi.jpg')); H, W = im.shape[:2]; print('full', W, H)
rows, cols = -(-H // T), -(-W // T); n = 0; tot = 0
for r in range(rows):
    for c in range(cols):
        t = im[r * T:(r + 1) * T, c * T:(c + 1) * T]
        p = RES / f'art_maptile_{r}_{c}.webp'; cv2.imwrite(str(p), t, [cv2.IMWRITE_WEBP_QUALITY, 82]); n += 1; tot += p.stat().st_size
cv2.imwrite(str(RES / 'art_real_map_base.webp'), cv2.resize(im, (2000, round(H * 2000 / W)), interpolation=cv2.INTER_AREA), [cv2.IMWRITE_WEBP_QUALITY, 88])
print(rows, 'x', cols, '=', n, 'tiles', round(tot / 1e6, 1), 'MB')
