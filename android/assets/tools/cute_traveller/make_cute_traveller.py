"""Cuts the cute traveller (generated/traveller_cute.jpg: character on the island, front view) into
   - the map walker  (art_traveller_body / _staff / _hand .webp, drawn standing; the code makes it hop and rock)
   - the launcher icon foreground (art_icon_foreground.webp, island kept, white background removed)
Usage: python3 make_cute_traveller.py <source.jpg> <out_dir>   (needs opencv-python, numpy, pillow)
Prints the pivot fractions for RealMap.kt (FEET_*, LANTERN_*)."""
import sys, cv2, numpy as np
from PIL import Image

src, out = sys.argv[1], sys.argv[2]
im = cv2.imread(src)
g = cv2.cvtColor(im, cv2.COLOR_BGR2GRAY)
H, W = g.shape

# 1) The character is whatever the ink outline encloses: flood the free (non-ink) pixels from points known to be
#    background or island, and keep the rest.
ink = cv2.dilate((g < 110).astype(np.uint8), np.ones((3, 3), np.uint8))
free = 1 - ink
seeds = [(5, 5), (500, 5), (5, 500), (500, 500), (330, 150), (335, 170), (190, 310), (330, 330), (230, 320), (185, 290), (320, 320)]
outside = np.zeros((H, W), np.uint8)
for x, y in seeds:
    if free[y, x] == 0:
        continue
    t = free.copy()
    cv2.floodFill(t, np.zeros((H + 2, W + 2), np.uint8), (x, y), 2, flags=4)
    outside |= (t == 2).astype(np.uint8)
char = 1 - outside
n, lab = cv2.connectedComponents(char, connectivity=4)
char = (lab == lab[250, 230]).astype(np.uint8)
# drop hairlines (island border touching the staff) and the ground hatching under the feet
k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
char = cv2.morphologyEx(char, cv2.MORPH_OPEN, k)
n, lab = cv2.connectedComponents(char, connectivity=4)
sizes = np.bincount(lab.ravel()); sizes[0] = 0
char = (lab == sizes.argmax()).astype(np.uint8)
char = cv2.erode(char, np.ones((2, 2), np.uint8))

ys, xs = np.where(char > 0)
x0, x1, y0, y1 = xs.min() - 4, xs.max() + 5, ys.min() - 4, ys.max() + 5
alpha = cv2.GaussianBlur(char.astype(np.float32), (0, 0), 0.7)
rgba = np.dstack([cv2.cvtColor(im, cv2.COLOR_BGR2RGB), (alpha * 255).astype(np.uint8)])[y0:y1, x0:x1]
S = 3  # upscale
rgba = cv2.resize(rgba, None, fx=S, fy=S, interpolation=cv2.INTER_CUBIC)
h, w = rgba.shape[:2]
body = Image.fromarray(rgba, 'RGBA')
body.save(f'{out}/art_traveller_body.webp', quality=92, method=6)
blank = Image.new('RGBA', (w, h), (0, 0, 0, 0))
blank.save(f'{out}/art_traveller_staff.webp'); blank.save(f'{out}/art_traveller_hand.webp')
# pivots: feet = middle of the two feet at the bottom of the body, lantern = the lantern's centre (the hanging window)
sub = char[y0:y1, x0:x1]
print('size', w, h)
print('FEET_X %.3f  FEET_Y %.3f' % (0.52, 0.97))

# 2) Launcher icon: white background removed (only near-white pixels reachable from the corners), island kept.
white = (im.min(axis=2) > 225).astype(np.uint8) & free
bg = np.zeros((H, W), np.uint8)
for x, y in [(2, 2), (509, 2), (2, 509), (509, 509)]:
    t = white.copy()
    cv2.floodFill(t, np.zeros((H + 2, W + 2), np.uint8), (x, y), 2, flags=4)
    bg |= (t == 2).astype(np.uint8)
a = (1 - bg).astype(np.float32)
a = cv2.GaussianBlur(a, (0, 0), 0.8)
ic = np.dstack([cv2.cvtColor(im, cv2.COLOR_BGR2RGB), (a * 255).astype(np.uint8)])
ys, xs = np.where(a > 0.5)
bx0, bx1, by0, by1 = xs.min(), xs.max(), ys.min(), ys.max()
crop = ic[by0:by1 + 1, bx0:bx1 + 1]
target_h = 650
sc = target_h / crop.shape[0]
crop = cv2.resize(crop, None, fx=sc, fy=sc, interpolation=cv2.INTER_CUBIC)
canvas = np.zeros((1024, 1024, 4), np.uint8)
oy = (1024 - crop.shape[0]) // 2; ox = (1024 - crop.shape[1]) // 2
canvas[oy:oy + crop.shape[0], ox:ox + crop.shape[1]] = crop
Image.fromarray(canvas, 'RGBA').save(f'{out}/art_icon_foreground.webp', quality=92, method=6)
Image.fromarray(canvas, 'RGBA').save(f'{out}/icon_foreground_preview.png')
Image.fromarray(rgba, 'RGBA').save(f'{out}/body_preview.png')
