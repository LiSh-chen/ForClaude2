#!/usr/bin/env python3
"""Writes mapguide/layout_guide.png: a flat-colour layout sketch of the world (3:4) that is uploaded to
Gemini together with the master-map prompt, so coastlines and regions land where the app expects them.
Positions are fractions of width/height."""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1536, 2048
OUT = Path(__file__).resolve().parent.parent / "mapguide"
NODES = {  # id: (x, y, colour, label)
    "forest": (0.26, 0.12, (60, 120, 70), "1"),
    "coast": (0.72, 0.24, (225, 205, 150), "2"),
    "coral": (0.80, 0.46, (90, 170, 180), "3"),
    "swamp": (0.28, 0.46, (105, 120, 85), "4"),
    "dunes": (0.24, 0.68, (232, 190, 110), "5"),
    "ruins": (0.70, 0.72, (150, 140, 170), "6"),
    "tree": (0.50, 0.92, (90, 150, 60), "7"),
}
BLOBS = [  # land blobs: x, y, radius (fractions of width)
    (0.26, 0.13, 0.20), (0.50, 0.20, 0.16), (0.66, 0.24, 0.17), (0.44, 0.34, 0.15),
    (0.28, 0.46, 0.18), (0.52, 0.50, 0.14), (0.27, 0.57, 0.15), (0.41, 0.58, 0.14), (0.58, 0.60, 0.14), (0.26, 0.68, 0.18), (0.48, 0.66, 0.14),
    (0.68, 0.70, 0.19), (0.50, 0.86, 0.17), (0.38, 0.78, 0.12), (0.62, 0.84, 0.12),
]
ISLETS = [(0.82, 0.43, 0.045), (0.88, 0.50, 0.035), (0.78, 0.50, 0.03), (0.86, 0.38, 0.025)]


def blob_mask(items) -> Image.Image:
    yy, xx = np.mgrid[0:H, 0:W]
    field = np.zeros((H, W), np.float32)
    for x, y, r in items:
        d = np.hypot(xx - x * W, yy - y * H) / (r * W)
        field += np.clip(1 - d, 0, 1) ** 1.2
    return Image.fromarray(((field > 0.42) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))


def main() -> None:
    OUT.mkdir(exist_ok=True)
    img = Image.new("RGB", (W, H), (150, 195, 205))
    land = blob_mask(BLOBS + [(x, y, r) for x, y, r in ISLETS])
    img.paste(Image.new("RGB", (W, H), (200, 190, 140)), mask=land)
    d = ImageDraw.Draw(img)
    zone = Image.new("RGB", (W, H))
    for key, (x, y, col, _) in NODES.items():
        if key == "coral":
            continue
        tile = Image.new("RGB", (W, H), col)
        m = Image.new("L", (W, H), 0)
        ImageDraw.Draw(m).ellipse([x * W - 230, y * H - 230, x * W + 230, y * H + 230], fill=170)
        m = m.filter(ImageFilter.GaussianBlur(70))
        m = Image.fromarray(np.minimum(np.array(m), np.array(land)))
        img.paste(tile, mask=m)
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 56)
    except OSError:
        font = ImageFont.load_default()
    for key, (x, y, col, label) in NODES.items():
        cx, cy = x * W, y * H
        d.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], fill=(255, 255, 255), outline=(30, 30, 30), width=5)
        d.text((cx, cy), label, fill=(30, 30, 30), font=font, anchor="mm")
    sx, sy = 0.10 * W, 0.05 * H
    d.polygon([(sx, sy - 40), (sx + 36, sy + 30), (sx - 36, sy + 30)], fill=(220, 40, 40), outline=(30, 30, 30))
    img.save(OUT / "layout_guide.png")
    img.resize((384, 512)).save(OUT / "layout_guide_small.png")
    print("wrote layout_guide.png")


if __name__ == "__main__":
    main()
