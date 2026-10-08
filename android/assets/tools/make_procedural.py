#!/usr/bin/env python3
"""Makes the assets that need no illustrator: paper texture and fog. Pure numpy + Pillow.

    python3 tools/make_procedural.py

Writes PNGs into generated/. Deterministic (fixed seeds), so reruns give identical files.
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "generated"


def periodic_blur(noise: np.ndarray, sigma: float) -> np.ndarray:
    """Gaussian blur through the FFT, so the result tiles seamlessly."""
    h, w = noise.shape
    fy = np.fft.fftfreq(h)[:, None]
    fx = np.fft.fftfreq(w)[None, :]
    kernel = np.exp(-2 * (np.pi ** 2) * (sigma ** 2) * (fx ** 2 + fy ** 2))
    out = np.real(np.fft.ifft2(np.fft.fft2(noise) * kernel))
    out -= out.min()
    return out / max(out.max(), 1e-9)


def parchment(size: int = 768, seed: int = 11) -> Image.Image:
    rng = np.random.default_rng(seed)
    base = np.array([241, 227, 190], dtype=np.float64)

    # Cloudy brightness variation at several scales, all periodic.
    cloud = np.zeros((size, size))
    for sigma, weight in [(3, 0.18), (9, 0.28), (28, 0.34), (90, 0.20)]:
        cloud += weight * periodic_blur(rng.standard_normal((size, size)), sigma)
    cloud = (cloud - cloud.mean()) / cloud.std()

    # Warm stains where a second, very soft field is high.
    stain = periodic_blur(rng.standard_normal((size, size)), 55)
    stain = np.clip((stain - 0.62) / 0.25, 0, 1)

    img = np.empty((size, size, 3))
    for c in range(3):
        img[..., c] = base[c] + cloud * (7.5 if c < 2 else 9.5)
    stain_color = np.array([196, 150, 82], dtype=np.float64)
    for c in range(3):
        img[..., c] = img[..., c] * (1 - 0.32 * stain) + stain_color[c] * 0.32 * stain

    # Fine paper grain.
    img += rng.standard_normal((size, size, 1)) * 2.2

    pil = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")

    # Fibres and specks, drawn nine times so they wrap around the edges.
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for _ in range(420):
        x, y = rng.uniform(0, size, 2)
        length = rng.uniform(6, 26)
        angle = rng.uniform(0, 2 * np.pi)
        dx, dy = np.cos(angle) * length, np.sin(angle) * length
        alpha = int(rng.uniform(10, 34))
        for ox in (-size, 0, size):
            for oy in (-size, 0, size):
                draw.line([(x + ox, y + oy), (x + ox + dx, y + oy + dy)], fill=(92, 66, 38, alpha), width=1)
    for _ in range(520):
        x, y = rng.uniform(0, size, 2)
        r = rng.uniform(0.6, 1.9)
        alpha = int(rng.uniform(18, 70))
        for ox in (-size, 0, size):
            for oy in (-size, 0, size):
                draw.ellipse([x + ox - r, y + oy - r, x + ox + r, y + oy + r], fill=(70, 48, 28, alpha))
    return Image.alpha_composite(pil, layer).convert("RGB")


def fog_puff(size: int = 256, seed: int = 5) -> Image.Image:
    rng = np.random.default_rng(seed)
    ys, xs = np.mgrid[0:size, 0:size]
    r = np.hypot(xs - size / 2 + 0.5, ys - size / 2 + 0.5) / (size / 2)
    falloff = np.clip(1 - r, 0, 1) ** 1.6

    # Wispy edge: modulate with a soft noise field.
    wisps = np.zeros((size, size))
    for sigma, weight in [(5, 0.6), (14, 0.4)]:
        wisps += weight * periodic_blur(rng.standard_normal((size, size)), sigma)
    alpha = np.clip(falloff * (0.55 + 0.7 * wisps), 0, 1) * 0.96

    rgb = np.empty((size, size, 3))
    shade = 0.94 + 0.1 * wisps
    for c, v in enumerate((217, 208, 185)):
        rgb[..., c] = np.clip(v * shade, 0, 255)
    rgba = np.dstack([rgb, alpha * 255]).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    parchment().save(OUT / "parchment_tile.png", optimize=True)
    fog_puff().save(OUT / "fog_puff.png", optimize=True)
    # Launcher icon background: the same paper, a little larger and darkened towards the corners.
    paper = Image.open(OUT / "parchment_tile.png").convert("RGB").resize((1024, 1024), Image.LANCZOS)
    ys, xs = np.mgrid[0:1024, 0:1024]
    edge = np.clip((np.hypot(xs - 512, ys - 512) / 724) ** 2.2, 0, 1)[..., None]
    shaded = np.asarray(paper, dtype=np.float64) * (1 - 0.28 * edge) + np.array([185, 143, 82]) * 0.28 * edge
    Image.fromarray(np.clip(shaded, 0, 255).astype(np.uint8), "RGB").save(OUT / "icon_background.png", optimize=True)
    print("wrote parchment_tile.png, fog_puff.png, icon_background.png")


if __name__ == "__main__":
    main()
