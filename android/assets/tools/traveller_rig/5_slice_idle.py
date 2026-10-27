#!/usr/bin/env python3
"""Cuts the idle-action sheets (six frames each, 3 x 2, the traveller facing right) into app frames art_idle_<act>_<n>.webp.

    python3 5_slice_idle.py <act> <sheet.png|jpg> <out_dir>      e.g.  python3 5_slice_idle.py sit 2_sit.png /tmp/idle

Besides the six drawn frames it writes in-between pictures (optical-flow tweens, --tween per gap, 3 by default: 21 frames),
all at half size, and evens out the size of the frames by their standing frame.  Like 3_slice_walk.py (same cut-out, same canvas), but a frame keeps its height above the ground: each row gets the scale that makes its
standing frame (1 or 6) as tall as the standing picture; a frame is centred on the standing body column, and moved
down by the same amount that moves the standing frame of its row onto the feet line, so a hop or a sit stays a hop or a sit.
"""
import argparse, importlib.util, pathlib, sys
import cv2, numpy as np
from PIL import Image

here = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("slice_walk", here / "3_slice_walk.py")
sw = importlib.util.module_from_spec(spec); spec.loader.exec_module(sw)

PAD = 60                         # empty room added left and right: a wide pose (hat in front, lantern behind) needs more than the walk pictures have
CW = sw.CANVAS[0] + 2 * PAD + 1  # the app draws these pictures centred on the standing picture, at the same height

def body_center(rgba: np.ndarray) -> float:
    """Middle of the lower half (the hat and staff reach out sideways), in the array's own x."""
    h = rgba.shape[0]
    xs = np.where((rgba[h // 2:, :, 3] > 40).any(axis=0))[0]
    return (xs.min() + xs.max()) / 2

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("act"); ap.add_argument("sheet"); ap.add_argument("out")
    ap.add_argument("--tween", type=int, default=3, help="in-between pictures made between two drawn frames")
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
    target_cx += PAD
    # the generated rows differ in size (the second row is drawn larger), so each row gets its own scale from its standing frame
    scales = {0: a.fit * target_h / cells[0][0].shape[0], 1: a.fit * target_h / cells[5][0].shape[0]}
    ref_bottom = {0: cells[0][3], 1: cells[5][3]}              # feet line of each row, in cell pixels
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    tiles = []
    for i, (f, x0, y0, y1) in enumerate(cells):
        row = i // 3
        scale = scales[row]
        fw, fh = max(1, round(f.shape[1] * scale)), max(1, round(f.shape[0] * scale))
        g = cv2.resize(f, (fw, fh), interpolation=cv2.INTER_AREA)
        ox = int(round(target_cx - body_center(g)))                   # the body column, frame by frame: the generated cells drift sideways
        ox = max(min(ox, CW - fw), 0)                       # never push the staff or lantern off the canvas
        oy = int(round(sw.CANVAS[1] * sw.FEET_Y - fh + (y1 - ref_bottom[row]) * scale))
        canvas = np.zeros((sw.CANVAS[1], CW, 4), np.uint8)
        xs0, ys0 = max(ox, 0), max(oy, 0)
        xs1, ys1 = min(ox + fw, CW), min(oy + fh, sw.CANVAS[1])
        canvas[ys0:ys1, xs0:xs1] = g[ys0 - oy:ys1 - oy, xs0 - ox:xs1 - ox]
        tiles.append(canvas)
    tiles = even_out_size(tiles, target_cx)
    assert tiles[0].shape[1] == CW
    frames = [tiles[0]]
    for i in range(5):                                             # in-between pictures: the app steps through these, not just the six drawn ones
        frames += tween(tiles[i], tiles[i + 1], a.tween) + [tiles[i + 1]]
    half = (CW // 2, sw.CANVAS[1] // 2)                  # drawn at most about 180 px tall on screen, so half size is plenty
    small = [cv2.resize(t, half, interpolation=cv2.INTER_AREA) for t in frames]
    for i, t in enumerate(small):
        Image.fromarray(t).save(out / f"art_idle_{a.act}_{i}.webp", quality=88)
    sheet = np.full((half[1] * 3, half[0] * 7, 3), 235, np.uint8)
    for i, t in enumerate(small[:21]):
        r, c = divmod(i, 7)
        chk = ((np.indices(t.shape[:2]).sum(axis=0) // 12) % 2 * 25 + 215).astype(np.uint8)
        bgc = np.dstack([chk] * 3).astype(float); al = t[..., 3:4] / 255.0
        sheet[r * half[1]:(r + 1) * half[1], c * half[0]:(c + 1) * half[0]] = (t[..., :3] * al + bgc * (1 - al)).astype(np.uint8)
    Image.fromarray(sheet).save(out / f"contact_{a.act}.png")
    print(a.act, len(small), "frames; scales", {k: round(v, 3) for k, v in scales.items()})

def silhouette(rgba: np.ndarray) -> float:
    return float((rgba[..., 3] > 128).sum())

def even_out_size(tiles, pivot_x):
    """The generated rows are drawn at slightly different sizes, so each row is scaled about the feet until its standing frame
    (1 in the first row, 6 in the second) has the silhouette of the walk's standing picture (art_traveller_walk_2). Both
    standing frames are then the same size, and so are the frames that follow each of them."""
    ref = silhouette(np.array(Image.open(sw.RES / "art_traveller_walk_2.webp").convert("RGBA")))
    out = []
    for i, t in enumerate(tiles):
        r = float(np.clip(np.sqrt(ref / silhouette(tiles[0 if i < 3 else 5])), 0.9, 1.1))
        M = np.float32([[r, 0, pivot_x * (1 - r)], [0, r, sw.CANVAS[1] * sw.FEET_Y * (1 - r)]])
        out.append(cv2.warpAffine(t, M, (CW, sw.CANVAS[1]), flags=cv2.INTER_AREA if r < 1 else cv2.INTER_CUBIC))
    for i, t in enumerate(out):
        edge = int((t[:, :3, 3] > 40).sum() + (t[:, -3:, 3] > 40).sum())
        if edge: print("  frame", i, "touches the canvas edge:", edge, "px")
    return out

def tween(A: np.ndarray, B: np.ndarray, n: int):
    """n in-between pictures of A -> B: both are bent along the optical flow between them and melted, so an arm rises
    instead of one picture fading into the other."""
    white = lambda t: (t[..., :3] * (t[..., 3:] / 255.0) + 255 * (1 - t[..., 3:] / 255.0)).astype(np.uint8)
    ga, gb = cv2.cvtColor(white(A), cv2.COLOR_RGB2GRAY), cv2.cvtColor(white(B), cv2.COLOR_RGB2GRAY)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    dis.setFinestScale(1); dis.setPatchSize(12); dis.setPatchStride(4); dis.setVariationalRefinementIterations(8)
    flow = cv2.GaussianBlur(dis.calc(ga, gb, None), (0, 0), 2.5)
    h, w = ga.shape
    gx, gy = np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))
    pa = np.dstack([A[..., :3] * (A[..., 3:] / 255.0), A[..., 3:]]).astype(np.float32)   # premultiplied, so edges melt cleanly
    pb = np.dstack([B[..., :3] * (B[..., 3:] / 255.0), B[..., 3:]]).astype(np.float32)
    out = []
    for k in range(1, n + 1):
        t = k / (n + 1)
        wa = cv2.remap(pa, gx - t * flow[..., 0], gy - t * flow[..., 1], cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT)
        wb = cv2.remap(pb, gx + (1 - t) * flow[..., 0], gy + (1 - t) * flow[..., 1], cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT)
        m = (1 - t) * wa + t * wb
        al = m[..., 3:]
        rgb = np.where(al > 1, m[..., :3] / np.maximum(al, 1) * 255.0, 0)
        out.append(np.dstack([np.clip(rgb, 0, 255), np.clip(al, 0, 255)]).astype(np.uint8))
    return out

if __name__ == "__main__":
    sys.exit(main())
