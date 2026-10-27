#!/usr/bin/env python3
"""Cuts the idle-action sheets (six frames each, 3 x 2, the traveller facing right) into app frames art_idle_<act>_<n>.webp.

    python3 5_slice_idle.py <act> <sheet.png|jpg> <out_dir>      e.g.  python3 5_slice_idle.py sit 2_sit.png /tmp/idle

Like 3_slice_walk.py (same cut-out, same canvas), but a frame keeps its height above the ground: each row gets the scale that makes its
standing frame (1 or 6) as tall as the standing picture; a frame is centred on the standing body column, and moved
down by the same amount that moves the standing frame of its row onto the feet line, so a hop or a sit stays a hop or a sit.
"""
import argparse, importlib.util, pathlib, sys
import cv2, numpy as np
from PIL import Image

here = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("slice_walk", here / "3_slice_walk.py")
sw = importlib.util.module_from_spec(spec); spec.loader.exec_module(sw)

def body_center(rgba: np.ndarray) -> float:
    """Middle of the lower half (the hat and staff reach out sideways), in the array's own x."""
    h = rgba.shape[0]
    xs = np.where((rgba[h // 2:, :, 3] > 40).any(axis=0))[0]
    return (xs.min() + xs.max()) / 2

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("act"); ap.add_argument("sheet"); ap.add_argument("out")
    ap.add_argument("--fit", type=float, default=1.0, help="extra shrink, the same for all five sheets, so that the widest frame (hat in front, lantern behind) stays inside the canvas")
    a = ap.parse_args()
    rgb = np.array(Image.open(a.sheet).convert("RGB"))
    H, W, _ = rgb.shape
    ch, cw = H // 2, W // 3
    # Cut the whole sheet out at once and take the six figures as connected pieces: a staff or lantern that reaches across
    # a cell border would otherwise be cut in two and leave a sliver in the next cell.
    full = sw.cut_out(rgb)
    solid = (full[..., 3] > 40).astype(np.uint8)
    n, lab, stats, cen = cv2.connectedComponentsWithStats(cv2.dilate(solid, np.ones((13, 13), np.uint8)), connectivity=8)
    seeds = sorted(range(1, n), key=lambda i: -stats[i, 4])[:6]          # the six figures; a lantern that came loose is a smaller piece
    if len(seeds) < 6 or stats[seeds[-1], 4] < 20000:
        sys.exit("expected 6 figures: the frames touch each other or one is missing")
    near = {i: cv2.distanceTransform((lab != i).astype(np.uint8), cv2.DIST_L2, 3) for i in seeds}
    owner = {i: i for i in seeds}
    for i in range(1, n):
        if i not in owner and stats[i, 4] > 100:
            ys, xs = np.where(lab == i)
            owner[i] = min(seeds, key=lambda k: near[k][ys, xs].min())
    cells = [None] * 6
    for k in seeds:
        slot = int(cen[k][1] // ch) * 3 + int(cen[k][0] // cw)
        pieces = np.isin(lab, [i for i, o in owner.items() if o == k])
        mine = full.copy(); mine[..., 3] = np.where(pieces, full[..., 3], 0)
        x0, y0, x1, y1 = sw.bbox(mine)
        cells[slot] = (mine[y0:y1, x0:x1], x0, y0, y1)
    target_h, target_cx = sw.standing_reference()
    # the generated rows differ in size (the second row is drawn larger), so each row gets its own scale from its standing frame
    scales = {0: a.fit * target_h / cells[0][0].shape[0], 1: a.fit * target_h / cells[5][0].shape[0]}
    widest = max(f[0].shape[1] * scales[i // 3] for i, f in enumerate(cells))
    print(a.act, "fit that makes the widest frame take 96%% of the canvas: %.3f" % (a.fit * 0.96 * sw.CANVAS[0] / widest))
    ref_bottom = {0: cells[0][3], 1: cells[5][3]}              # feet line of each row, in cell pixels
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    tiles = []
    for i, (f, x0, y0, y1) in enumerate(cells):
        row = i // 3
        scale = scales[row]
        fw, fh = max(1, round(f.shape[1] * scale)), max(1, round(f.shape[0] * scale))
        g = cv2.resize(f, (fw, fh), interpolation=cv2.INTER_AREA)
        ox = int(round(target_cx - body_center(g)))                   # the body column, frame by frame: the generated cells drift sideways
        ox = max(min(ox, sw.CANVAS[0] - fw), 0)                       # never push the staff or lantern off the canvas
        oy = int(round(sw.CANVAS[1] * sw.FEET_Y - fh + (y1 - ref_bottom[row]) * scale))
        canvas = np.zeros((sw.CANVAS[1], sw.CANVAS[0], 4), np.uint8)
        xs0, ys0 = max(ox, 0), max(oy, 0)
        xs1, ys1 = min(ox + fw, sw.CANVAS[0]), min(oy + fh, sw.CANVAS[1])
        canvas[ys0:ys1, xs0:xs1] = g[ys0 - oy:ys1 - oy, xs0 - ox:xs1 - ox]
        Image.fromarray(canvas).save(out / f"art_idle_{a.act}_{i}.webp", quality=90)
        tiles.append(canvas)
    sheet = np.full((sw.CANVAS[1], sw.CANVAS[0] * 6, 3), 235, np.uint8)
    for i, t in enumerate(tiles):
        chk = ((np.indices(t.shape[:2]).sum(axis=0) // 20) % 2 * 25 + 215).astype(np.uint8)
        bgc = np.dstack([chk] * 3).astype(float); al = t[..., 3:4] / 255.0
        sheet[:, i * sw.CANVAS[0]:(i + 1) * sw.CANVAS[0]] = (t[..., :3] * al + bgc * (1 - al)).astype(np.uint8)
        sheet[int(sw.CANVAS[1] * sw.FEET_Y), i * sw.CANVAS[0]:(i + 1) * sw.CANVAS[0]] = (255, 0, 0)
    Image.fromarray(sheet).save(out / f"contact_{a.act}.png")
    print(a.act, "scales", {k: round(v, 3) for k, v in scales.items()})

if __name__ == "__main__":
    sys.exit(main())
