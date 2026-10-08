#!/usr/bin/env python3
"""A full-screen sheet of old parchment: yellowed and uneven, folded, fibrous, with coffee rings and ink blots.

    python3 tools/make_paper_page.py          # -> generated/paper_base.png (1080 x 2160)

Handwritten notes and sketches are added on top by svg/paper_notes.svg (see make_botanical.py).
Deterministic; pure numpy + Pillow.
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "generated"
W, H = 1080, 2160


def blur(noise: np.ndarray, sigma: float) -> np.ndarray:
    fy = np.fft.fftfreq(noise.shape[0])[:, None]
    fx = np.fft.fftfreq(noise.shape[1])[None, :]
    out = np.real(np.fft.ifft2(np.fft.fft2(noise) * np.exp(-2 * (np.pi ** 2) * sigma ** 2 * (fx ** 2 + fy ** 2))))
    out -= out.min()
    return out / max(out.max(), 1e-9)


def main() -> None:
    rng = np.random.default_rng(2026)
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float64)

    # --- tone: yellowed base, cloudy, with brown blotches
    cloud = sum(w * blur(rng.standard_normal((H, W)), s) for s, w in [(5, .12), (18, .24), (60, .34), (200, .30)])
    cloud = (cloud - cloud.mean()) / cloud.std()
    img = np.empty((H, W, 3))
    base = np.array([234, 212, 160.0])
    for c, k in enumerate((9, 10, 13)):
        img[..., c] = base[c] + cloud * k
    blotch = np.clip((blur(rng.standard_normal((H, W)), 110) - 0.58) / 0.25, 0, 1)
    img = img * (1 - 0.36 * blotch[..., None]) + np.array([186, 142, 84.0]) * 0.36 * blotch[..., None]

    # --- edges: darkened and slightly burnt, ragged
    edge_dist = np.minimum.reduce([xs, W - 1 - xs, ys, H - 1 - ys])
    ragged = edge_dist + (blur(rng.standard_normal((H, W)), 14) - 0.5) * 120
    edge = np.clip(1 - ragged / 150, 0, 1) ** 2
    img = img * (1 - 0.6 * edge[..., None]) + np.array([128, 90, 48.0]) * 0.6 * edge[..., None]

    # --- folds: one vertical, two horizontal, each a dark line with a pale lip and a soft shadow
    def crease(position: float, vertical: bool, length: int, strength: float) -> np.ndarray:
        wobble = (blur(rng.standard_normal((1, length)), 40)[0] - 0.5) * 14
        along = np.arange(length)
        fade = 0.55 + 0.45 * blur(rng.standard_normal((1, length)), 25)[0]
        if vertical:
            d = xs - (position + wobble[None, :][:, :]).repeat(H, 0) if False else xs - (position + wobble[ys.astype(int)[:, 0] % length][:, None])
            f = fade[ys.astype(int)[:, 0] % length][:, None]
        else:
            d = ys - (position + wobble[xs.astype(int)[0] % length][None, :])
            f = fade[xs.astype(int)[0] % length][None, :]
        line = -0.20 * np.exp(-(d / 1.8) ** 2) + 0.10 * np.exp(-((d - 4.5) / 3.5) ** 2) - 0.045 * np.exp(-(d / 22) ** 2)
        return line * f * strength

    shade = crease(W * 0.5, True, H, 1.0) + crease(H * 0.34, False, W, 0.9) + crease(H * 0.67, False, W, 0.9)
    # small crumples
    for _ in range(16):
        x0, y0 = rng.uniform(0, W), rng.uniform(0, H)
        ang, length = rng.uniform(0, np.pi), rng.uniform(160, 460)
        t = (xs - x0) * np.cos(ang) + (ys - y0) * np.sin(ang)
        d = -(xs - x0) * np.sin(ang) + (ys - y0) * np.cos(ang)
        along = np.exp(-((t / length) ** 2) * 2)
        shade += along * (-0.11 * np.exp(-(d / 1.5) ** 2) + 0.05 * np.exp(-((d - 3) / 3) ** 2)) * rng.uniform(.4, .9)
    img *= (1 + shade)[..., None]

    # --- coffee rings and a ink blot, as pigment darkening the paper
    def ring(cx, cy, r, strength):
        dx, dy = xs - cx, ys - cy
        rr, th = np.hypot(dx, dy), np.arctan2(dy, dx)
        wobble = 3.2 * np.sin(2 * th + 1.3) + 1.8 * np.sin(5 * th + 0.4) + 1.0 * np.sin(11 * th)
        rim = np.exp(-((rr - r - wobble) / 4.5) ** 2)
        halo = np.exp(-((rr - r - wobble - 10) / 16) ** 2) * 0.35
        inner = (rr < r + wobble) * (0.10 + 0.05 * blur(rng.standard_normal((H, W)), 8))
        return (rim * 0.55 + halo + inner) * strength

    stain = ring(760, 430, 150, 1.0) + ring(250, 1640, 105, 0.8) + ring(870, 1830, 60, 0.6)
    stain = np.clip(stain, 0, 0.8)
    img = img * (1 - stain[..., None]) + np.array([112, 74, 38.0]) * stain[..., None]

    pil = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)

    # ink blots with soft edges, then splatter
    for cx, cy, r in [(172, 612, 15), (905, 1236, 11), (460, 1950, 18)]:
        pts = []
        for k in range(18):
            a = k / 18 * 2 * np.pi
            rr = r * rng.uniform(0.7, 1.25)
            pts.append((cx + np.cos(a) * rr, cy + np.sin(a) * rr))
        draw.polygon(pts, fill=(38, 28, 22, 150))
        for _ in range(26):
            a, d = rng.uniform(0, 2 * np.pi), r * rng.uniform(1.4, 4.0)
            rr = rng.uniform(0.8, 2.6)
            draw.ellipse([cx + np.cos(a) * d - rr, cy + np.sin(a) * d - rr, cx + np.cos(a) * d + rr, cy + np.sin(a) * d + rr], fill=(38, 28, 22, int(rng.uniform(70, 150))))

    # fibres: fine curved hairs, dark and light
    for _ in range(3800):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        a, step = rng.uniform(0, 2 * np.pi), rng.uniform(3, 8)
        points = [(x, y)]
        for _ in range(int(rng.integers(3, 7))):
            a += rng.normal(0, 0.45)
            x, y = x + np.cos(a) * step, y + np.sin(a) * step
            points.append((x, y))
        if rng.random() < 0.72:
            draw.line(points, fill=(98, 70, 42, int(rng.uniform(16, 52))), width=1)
        else:
            draw.line(points, fill=(255, 246, 224, int(rng.uniform(34, 80))), width=1)
    for _ in range(900):  # specks
        x, y, r = rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.6, 1.8)
        draw.ellipse([x - r, y - r, x + r, y + r], fill=(70, 48, 28, int(rng.uniform(30, 100))))

    out = Image.alpha_composite(pil, layer).convert("RGB")
    grain = rng.standard_normal((H, W, 1)) * 2.0
    out = Image.fromarray(np.clip(np.asarray(out, dtype=np.float64) + grain, 0, 255).astype(np.uint8), "RGB")
    OUT.mkdir(parents=True, exist_ok=True)
    out.save(OUT / "paper_base.png", optimize=True)
    print("wrote paper_base.png", out.size)


if __name__ == "__main__":
    main()
