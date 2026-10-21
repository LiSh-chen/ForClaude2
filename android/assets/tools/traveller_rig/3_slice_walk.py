#!/usr/bin/env python3
"""Cuts a walk-cycle sheet (a grid of frames of the same traveller) into app frames.

    python3 3_slice_walk.py <sheet.png|jpg> <out_dir> [--rows 2] [--cols 3] [--flip]

Each cell is cut out of the white background, scaled with ONE common factor (so the traveller does
not change size between frames), put on a 411 x 400 canvas with the feet on the same line and the
body centred on the same column as the standing pictures, and saved as art_traveller_walk_<n>.webp.
A contact sheet (contact.png, frames side by side on a checkerboard) is written too, for a look.
--flip mirrors the frames: the app wants the traveller facing right.
"""
import argparse, pathlib, sys
import cv2, numpy as np
from PIL import Image

CANVAS = (411, 400)           # same as art_traveller_body.webp
FEET_Y = 0.975
RES = pathlib.Path(__file__).resolve().parents[3] / "app/src/main/res/drawable-nodpi"
POCKET_MIN_PIXELS = 3000       # at the sheet's own resolution (cells of about 650 px)

def cut_out(cell: np.ndarray) -> np.ndarray:
    """RGBA of one cell: white connected to the border becomes transparent."""
    white = (cell.min(axis=2) > 232).astype(np.uint8)
    n, lab = cv2.connectedComponents(white, connectivity=4)
    border = set(lab[0, :]) | set(lab[-1, :]) | set(lab[:, 0]) | set(lab[:, -1])
    bg = np.isin(lab, [b for b in border if b != 0]) & (white > 0)
    # Pockets of background fully enclosed by the drawing (between the hat brim and the staff, say) are
    # not reached from the border. They are big and almost pure white; the body's highlights are small.
    pure = (cell.min(axis=2) > 245).astype(np.uint8)
    n2, lab2, stats2, _ = cv2.connectedComponentsWithStats(pure, connectivity=4)
    for i in range(1, n2):
        if stats2[i, 4] > POCKET_MIN_PIXELS:
            bg |= cv2.dilate((lab2 == i).astype(np.uint8), np.ones((3, 3), np.uint8)) > 0
    alpha = cv2.GaussianBlur(np.where(bg, 0, 255).astype(np.uint8), (3, 3), 0)
    return np.dstack([cell, alpha])

def bbox(rgba: np.ndarray):
    ys, xs = np.where(rgba[..., 3] > 40)
    return xs.min(), ys.min(), xs.max() + 1, ys.max() + 1

def standing_reference():
    """Height of the standing picture (body + staff + hand) and where its body sits, so frames match it."""
    layers = [Image.open(RES / f"art_traveller_{n}.webp").convert("RGBA") for n in ("body", "staff", "hand")]
    im = layers[0]
    for l in layers[1:]:
        im = Image.alpha_composite(im, l)
    a = np.array(im)
    ys, xs = np.where(a[..., 3] > 40)
    top, bottom = ys.min(), ys.max() + 1
    low = a[(top + bottom) // 2:, :, 3] > 40
    cx = np.where(low.any(axis=0))[0]
    return bottom - top, (cx.min() + cx.max()) / 2

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("sheet"); ap.add_argument("out")
    ap.add_argument("--rows", type=int, default=2); ap.add_argument("--cols", type=int, default=3)
    ap.add_argument("--flip", action="store_true")
    a = ap.parse_args()
    rgb = np.array(Image.open(a.sheet).convert("RGB"))
    H, W, _ = rgb.shape
    ch, cw = H // a.rows, W // a.cols
    frames = []
    for r in range(a.rows):
        for c in range(a.cols):
            cell = cut_out(rgb[r * ch:(r + 1) * ch, c * cw:(c + 1) * cw])
            x0, y0, x1, y1 = bbox(cell)
            frames.append(cell[y0:y1, x0:x1])
    target_h, target_cx = standing_reference()
    # one common scale: the average frame is as tall as the standing picture, so nothing jumps when it starts to walk
    scale = target_h / (sum(f.shape[0] for f in frames) / len(frames))
    scale = min(scale, 0.96 * CANVAS[0] / max(f.shape[1] for f in frames))
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    tiles = []
    for i, f in enumerate(frames):
        fw, fh = max(1, round(f.shape[1] * scale)), max(1, round(f.shape[0] * scale))
        g = cv2.resize(f, (fw, fh), interpolation=cv2.INTER_AREA)
        if a.flip: g = g[:, ::-1]
        # centre on the body: the middle of the lower half is the body, the hat and staff reach out sideways
        low = g[fh // 2:, :, 3] > 40
        cx = np.where(low.any(axis=0))[0]
        body_c = (cx.min() + cx.max()) / 2 if len(cx) else fw / 2
        canvas = np.zeros((CANVAS[1], CANVAS[0], 4), np.uint8)
        ox = int(round(target_cx - body_c)); oy = int(round(CANVAS[1] * FEET_Y - fh))
        xs0, ys0 = max(ox, 0), max(oy, 0)
        xs1, ys1 = min(ox + fw, CANVAS[0]), min(oy + fh, CANVAS[1])
        canvas[ys0:ys1, xs0:xs1] = g[ys0 - oy:ys1 - oy, xs0 - ox:xs1 - ox]
        Image.fromarray(canvas).save(out / f"art_traveller_walk_{i}.webp", quality=90)
        tiles.append(canvas)
    sheet = np.full((CANVAS[1], CANVAS[0] * len(tiles), 3), 235, np.uint8)
    for i, t in enumerate(tiles):
        chk = ((np.indices(t.shape[:2]).sum(axis=0) // 20) % 2 * 25 + 215).astype(np.uint8)
        bgc = np.dstack([chk] * 3).astype(float); al = t[..., 3:4] / 255.0
        sheet[:, i * CANVAS[0]:(i + 1) * CANVAS[0]] = (t[..., :3] * al + bgc * (1 - al)).astype(np.uint8)
        sheet[CANVAS[1] - int(CANVAS[1] * (1 - FEET_Y)), i * CANVAS[0]:(i + 1) * CANVAS[0]] = (255, 0, 0)  # feet line
    Image.fromarray(sheet).save(out / "contact.png")
    print(f"{len(tiles)} frames, scale {scale:.3f} -> {out}")

if __name__ == "__main__":
    sys.exit(main())
