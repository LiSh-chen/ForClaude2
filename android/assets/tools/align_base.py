#!/usr/bin/env python3
"""Nudges the painted island base onto the real coast (GSHHG) and real relief (SRTM hillshade) with a smooth warp.
Run from android/assets:  python3 tools/align_base.py <in.png> <out.png>   (default generated/island_ground.png -> generated/island_ground_aligned.png)"""
import sys, re, pathlib, numpy as np, cv2
from scipy.interpolate import RBFInterpolator
from scipy.ndimage import gaussian_filter, distance_transform_edt
from PIL import Image, ImageDraw
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import mock_map as M
ROOT = pathlib.Path(__file__).resolve().parent.parent
SCR = '/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/'
src_path = sys.argv[1] if len(sys.argv) > 1 else str(ROOT / 'generated/island_ground.png')
out_path = sys.argv[2] if len(sys.argv) > 2 else str(ROOT / 'generated/island_ground_aligned.png')
base = cv2.imread(src_path); H, W = base.shape[:2]; KM = W / 222.5
g = cv2.cvtColor(base, cv2.COLOR_BGR2GRAY).astype(np.float32)
txt = (ROOT.parent / 'core/src/main/kotlin/com/starmist/core/route/TaiwanOutline.kt').read_text()
ll = [(float(a), float(b)) for a, b in re.findall(r'GeoPoint\(([\d.]+), ([\d.]+)\)', txt)]
pts = np.array([((lo - 119.93) / 2.19 * W, (25.4 - la) / 3.6 * H) for la, lo in ll])

def metrics(img):
    gg = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY).astype(np.float32)
    m = Image.new('L', (W, H), 0); ImageDraw.Draw(m).polygon([tuple(p) for p in pts], fill=255); m = np.asarray(m) > 0
    edge = cv2.morphologyEx(m.astype(np.uint8), cv2.MORPH_GRADIENT, np.ones((3, 3), np.uint8)) > 0
    v = distance_transform_edt(~(gg < 90))[edge] / KM
    return np.median(v), np.percentile(v, 90)

# 1. coast: along the outward normal, where is the darkest (stroke) pixel?
gs = cv2.GaussianBlur(g, (0, 0), 1.2)
n = len(pts); cv_pts, cv_vec = [], []
for i in range(n):
    t = pts[(i + 3) % n] - pts[(i - 3) % n]; t /= np.hypot(*t) or 1; nrm = np.array([-t[1], t[0]])
    best = None
    for s in np.arange(-9, 9.01, 0.5):
        x, y = pts[i] + nrm * s
        if not (1 <= x < W - 1 and 1 <= y < H - 1): continue
        v = gs[int(round(y)), int(round(x))]
        if best is None or v < best[0]: best = (v, s)
    if best and best[0] < 110: cv_pts.append(pts[i]); cv_vec.append(-nrm * best[1])     # move the painting by this much
cv_pts = np.array(cv_pts); cv_vec = np.array(cv_vec)
# smooth the vectors along the coast (median of neighbours) to ignore stray dark lines
k = 7; sm = np.array([np.median(np.roll(cv_vec, j, 0)[:, c] if False else cv_vec[max(0, i - k):i + k + 1, c]) for i in range(len(cv_vec)) for c in (0, 1)]).reshape(-1, 2)
# 2. relief: block shifts of the painted shading against the SRTM hillshade
ys, xs = np.mgrid[0:H, 0:W]
dem = M.elevation((25.4 - (ys + .5) / H * 3.6).astype(np.float64), (119.93 + (xs + .5) / W * 2.19).astype(np.float64))
dz = gaussian_filter(dem, 1.0); gy, gx = np.gradient(dz, 1000 / KM)
slope = np.arctan(np.hypot(gx, gy)); asp = np.arctan2(-gx, gy); az, alt = np.radians(315), np.radians(40)
hs = np.sin(alt) * np.cos(slope) + np.cos(alt) * np.sin(slope) * np.cos(az - asp); hs = ((hs - hs.mean()) / hs.std()).astype(np.float32)
hp = lambda a: a - cv2.GaussianBlur(a, (0, 0), 12)
gl, hl = hp(g), hp(hs); B = 160; rp, rv = [], []
win = cv2.createHanningWindow((B, B), cv2.CV_32F)
for y in range(0, H - B, 80):
    for x in range(0, W - B, 80):
        if (dem[y:y + B, x:x + B] > 300).mean() < 0.7: continue
        (dx, dy), r = cv2.phaseCorrelate(hl[y:y + B, x:x + B], gl[y:y + B, x:x + B], win)
        if r > 0.1 and np.hypot(dx, dy) < 6: rp.append((x + B / 2, y + B / 2)); rv.append((-dx, -dy))
print('coast vectors', len(cv_pts), 'relief vectors', len(rp))
def warp_with(sign):
    P = np.vstack([cv_pts, np.array(rp)]); V = np.vstack([sm, sign * np.array(rv)]) if len(rp) else sm
    rbf = RBFInterpolator(P, V, kernel='thin_plate_spline', smoothing=60.0, neighbors=40)
    gx_, gy_ = np.meshgrid(np.arange(0, W, 12), np.arange(0, H, 12)); q = np.c_[gx_.ravel(), gy_.ravel()]
    d = rbf(q).reshape(gx_.shape + (2,)); d = np.clip(d, -9, 9)
    d = cv2.resize(d, (W, H), interpolation=cv2.INTER_CUBIC)
    # output pixel (x,y) takes the painting from (x - d): moves painted content by +d
    mapx = (xs - d[..., 0]).astype(np.float32); mapy = (ys - d[..., 1]).astype(np.float32)
    return cv2.remap(base, mapx, mapy, cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE), d
def relief_resid(img):
    gg = hp(cv2.cvtColor(img, cv2.COLOR_BGR2GRAY).astype(np.float32)); out = []
    for y in range(0, H - B, 80):
        for x in range(0, W - B, 80):
            if (dem[y:y + B, x:x + B] > 300).mean() < 0.7: continue
            (dx, dy), r = cv2.phaseCorrelate(hl[y:y + B, x:x + B], gg[y:y + B, x:x + B], win)
            if r > 0.1: out.append(np.hypot(dx, dy) / KM)
    return np.median(out), np.percentile(out, 90)
print('before: relief median/p90 km %.3f %.3f' % relief_resid(base))
print('before: coast median/p90 km %.3f %.3f' % metrics(base))
best = None
for sign in (1, -1):
    out, d = warp_with(sign); m = metrics(out); print('relief sign', sign, 'coast median/p90 km %.3f %.3f' % m, 'relief %.3f %.3f' % relief_resid(out), 'max shift px', np.abs(d).max())
    score = relief_resid(out)[1] + m[1]
    if best is None or score < best[4]: best = (m, out, d, sign, score)
cv2.imwrite(out_path, best[1]); print('kept sign', best[3], 'after', best[0])
np.save(SCR + 'align_disp.npy', best[2])
