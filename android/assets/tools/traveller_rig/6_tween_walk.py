#!/usr/bin/env python3
"""Makes the smooth walk cycle: between each two of the six drawn walk frames (art_traveller_walk_0..5) it adds in-between pictures
along the optical flow (the same method as the idle acts, see 5_slice_idle.py), and closes the loop from frame 5 back to 0.
Writes art_traveller_walkx_0..23 (half size: the traveller is never drawn more than about 180 px tall).

    python3 6_tween_walk.py <out_dir> [--tween 3]
"""
import argparse, importlib.util, pathlib, sys
import cv2, numpy as np
from PIL import Image

here = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("slice_idle", here / "5_slice_idle.py")
si = importlib.util.module_from_spec(spec); spec.loader.exec_module(si)
RES = si.sw.RES

def main() -> None:
    ap = argparse.ArgumentParser(); ap.add_argument("out"); ap.add_argument("--tween", type=int, default=3)
    a = ap.parse_args()
    keys = [np.array(Image.open(RES / f"art_traveller_walk_{i}.webp").convert("RGBA")) for i in range(6)]
    frames = []
    for i in range(6):
        frames.append(keys[i])
        frames += si.tween(keys[i], keys[(i + 1) % 6], a.tween)
    half = (keys[0].shape[1] // 2, keys[0].shape[0] // 2)
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    small = [cv2.resize(f, half, interpolation=cv2.INTER_AREA) for f in frames]
    for k, f in enumerate(small): Image.fromarray(f).save(out / f"art_traveller_walkx_{k}.webp", quality=88)
    cols = 8
    sheet = np.full((half[1] * ((len(small) + cols - 1) // cols), half[0] * cols, 3), 235, np.uint8)
    for k, t in enumerate(small):
        r, c = divmod(k, cols)
        chk = ((np.indices(t.shape[:2]).sum(axis=0) // 12) % 2 * 25 + 215).astype(np.uint8)
        bgc = np.dstack([chk] * 3).astype(float); al = t[..., 3:4] / 255.0
        sheet[r * half[1]:(r + 1) * half[1], c * half[0]:(c + 1) * half[0]] = (t[..., :3] * al + bgc * (1 - al)).astype(np.uint8)
    Image.fromarray(sheet).save(out / "contact_walkx.png")
    print(len(small), "frames")

if __name__ == "__main__":
    sys.exit(main())
