"""Pen-and-wash drawing helpers that emit SVG path strings: ferns, flowers, grasses, handwriting.

Everything is deterministic (seeded). Colours follow the field-notebook palette: dark green, brown,
withered yellow and earth tones.
"""
import math
import random

INK = "#3B2A1A"
GREENS = ["#4F6B45", "#5E7A4E", "#6F8A55", "#7C8A4E"]
YELLOWS = ["#C9A85C", "#B99A4E", "#D2B46C"]
EARTH = ["#A68A64", "#8C6B45", "#7A5A3A"]


def _quad(a, c, b, t):
    u = 1 - t
    return (u * u * a[0] + 2 * u * t * c[0] + t * t * b[0], u * u * a[1] + 2 * u * t * c[1] + t * t * b[1])


def _tangent(a, c, b, t):
    u = 1 - t
    dx, dy = 2 * u * (c[0] - a[0]) + 2 * t * (b[0] - c[0]), 2 * u * (c[1] - a[1]) + 2 * t * (b[1] - c[1])
    n = math.hypot(dx, dy) or 1
    return dx / n, dy / n


def fern(x, y, height, lean, seed=0, leaflets=24, wash_layer=True, ink_width=1.1):
    """A pinnate fern frond. Returns (wash_svg, ink_svg): fills and pen lines, to be drawn with filters."""
    rnd = random.Random(seed)
    base = (x, y)
    tip = (x + lean * height, y - height)
    ctrl = (x - lean * height * 0.2, y - height * 0.62)
    wash, ink = [], []
    ink.append(f'<path d="M{x:.1f},{y:.1f} Q{ctrl[0]:.1f},{ctrl[1]:.1f} {tip[0]:.1f},{tip[1]:.1f}" fill="none" stroke="{INK}" stroke-width="{ink_width * 1.5:.2f}" stroke-linecap="round"/>')
    for i in range(1, leaflets + 1):
        t = 0.06 + 0.9 * i / (leaflets + 1)
        px, py = _quad(base, ctrl, tip, t)
        tx, ty = _tangent(base, ctrl, tip, t)
        nx, ny = -ty, tx
        envelope = math.sin(math.pi * min(1.0, t * 0.92 + 0.1)) ** 0.8
        length = height * 0.26 * envelope + height * 0.02
        width = length * 0.2
        for side in (-1, 1):
            j = rnd.uniform(0.92, 1.08)
            dirx, diry = tx * 0.62 + nx * side, ty * 0.62 + ny * side
            dn = math.hypot(dirx, diry)
            dirx, diry = dirx / dn, diry / dn
            ex, ey = px + dirx * length * j, py + diry * length * j
            mx, my = px + dirx * length * j * 0.5, py + diry * length * j * 0.5
            c1 = (mx - diry * width, my + dirx * width)
            c2 = (mx + diry * width, my - dirx * width)
            d = f"M{px:.1f},{py:.1f} Q{c1[0]:.1f},{c1[1]:.1f} {ex:.1f},{ey:.1f} Q{c2[0]:.1f},{c2[1]:.1f} {px:.1f},{py:.1f} Z"
            fill = rnd.choice(GREENS if t < 0.75 else GREENS + YELLOWS)
            wash.append(f'<path d="{d}" fill="{fill}" opacity="0.82"/>')
            ink.append(f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="{ink_width:.2f}" stroke-linejoin="round"/>')
            ink.append(f'<path d="M{px:.1f},{py:.1f} L{ex:.1f},{ey:.1f}" stroke="{INK}" stroke-width="{ink_width * 0.55:.2f}" opacity=".6" fill="none"/>')
    return "".join(wash), "".join(ink)


def flower(x, y, height, lean, seed=0, petal="#C9A3A0", ink_width=1.2):
    """A lily-like flower on a stem with two long basal leaves, buds and stamens."""
    rnd = random.Random(seed)
    tip = (x + lean * height, y - height)
    ctrl = (x + lean * height * 0.05, y - height * 0.55)
    wash, ink = [], []
    stem = f"M{x:.1f},{y:.1f} Q{ctrl[0]:.1f},{ctrl[1]:.1f} {tip[0]:.1f},{tip[1]:.1f}"
    ink.append(f'<path d="{stem}" fill="none" stroke="{INK}" stroke-width="{ink_width * 1.5:.2f}" stroke-linecap="round"/>')
    for side in (-1, 1):  # basal leaves
        d = (f"M{x:.1f},{y:.1f} C{x + side * height * 0.25:.1f},{y - height * 0.35:.1f} {x + side * height * 0.5:.1f},{y - height * 0.3:.1f} "
             f"{x + side * height * 0.55:.1f},{y - height * 0.1:.1f} C{x + side * height * 0.3:.1f},{y - height * 0.2:.1f} {x + side * height * 0.1:.1f},{y - height * 0.12:.1f} {x:.1f},{y:.1f} Z")
        wash.append(f'<path d="{d}" fill="{rnd.choice(GREENS)}" opacity=".8"/>')
        ink.append(f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="{ink_width:.2f}"/>')
        ink.append(f'<path d="M{x:.1f},{y:.1f} Q{x + side * height * 0.3:.1f},{y - height * 0.22:.1f} {x + side * height * 0.52:.1f},{y - height * 0.11:.1f}" stroke="{INK}" stroke-width="{ink_width * .5:.2f}" fill="none" opacity=".6"/>')
    # petals
    plen = height * 0.3
    for k in range(-2, 4):
        ang = math.radians(-90 + k * 30 - 15 + rnd.uniform(-5, 5))
        dx, dy = math.cos(ang), math.sin(ang)
        px, py = -dy, dx
        ex, ey = tip[0] + dx * plen, tip[1] + dy * plen
        c1 = (tip[0] + dx * plen * 0.55 + px * plen * 0.3, tip[1] + dy * plen * 0.55 + py * plen * 0.3)
        c2 = (tip[0] + dx * plen * 0.55 - px * plen * 0.3, tip[1] + dy * plen * 0.55 - py * plen * 0.3)
        d = f"M{tip[0]:.1f},{tip[1]:.1f} Q{c1[0]:.1f},{c1[1]:.1f} {ex:.1f},{ey:.1f} Q{c2[0]:.1f},{c2[1]:.1f} {tip[0]:.1f},{tip[1]:.1f} Z"
        wash.append(f'<path d="{d}" fill="{petal}" opacity=".78"/>')
        ink.append(f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="{ink_width:.2f}"/>')
        ink.append(f'<path d="M{tip[0]:.1f},{tip[1]:.1f} L{tip[0] + dx * plen * .8:.1f},{tip[1] + dy * plen * .8:.1f}" stroke="{INK}" stroke-width="{ink_width * .5:.2f}" opacity=".55"/>')
    for k in range(-2, 3):  # stamens
        ex, ey = tip[0] + k * plen * 0.12, tip[1] - plen * 0.55
        ink.append(f'<path d="M{tip[0]:.1f},{tip[1]:.1f} L{ex:.1f},{ey:.1f}" stroke="{INK}" stroke-width="{ink_width * .6:.2f}"/>')
        ink.append(f'<circle cx="{ex:.1f}" cy="{ey:.1f}" r="{ink_width * 1.4:.1f}" fill="#A8782C"/>')
    # a bud on a side stem
    bx, by = _quad((x, y), ctrl, tip, 0.5)
    ex, ey = bx + height * 0.22 * (-1 if lean >= 0 else 1), by - height * 0.12
    ink.append(f'<path d="M{bx:.1f},{by:.1f} Q{(bx + ex) / 2:.1f},{by - 6:.1f} {ex:.1f},{ey:.1f}" fill="none" stroke="{INK}" stroke-width="{ink_width:.2f}"/>')
    bud = f"M{ex:.1f},{ey:.1f} q-5,-14 4,-26 q10,10 2,26 Z"
    wash.append(f'<path d="{bud}" fill="{petal}" opacity=".8"/>')
    ink.append(f'<path d="{bud}" fill="none" stroke="{INK}" stroke-width="{ink_width:.2f}"/>')
    return "".join(wash), "".join(ink)


def grass(x, y, height, count=7, seed=0, ink_width=1.0):
    rnd = random.Random(seed)
    parts = []
    for _ in range(count):
        gx = x + rnd.uniform(-height * .25, height * .25)
        lean = rnd.uniform(-.35, .35)
        d = f"M{gx:.1f},{y:.1f} Q{gx + lean * height * .3:.1f},{y - height * .6:.1f} {gx + lean * height:.1f},{y - height * rnd.uniform(.6, 1):.1f}"
        parts.append(f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="{ink_width:.2f}" stroke-linecap="round" opacity=".85"/>')
    return "".join(parts)


def handwriting(x, y, length, size, seed=0, slant=12):
    """Loopy cursive-looking scribble (not real words), as one SVG path string."""
    rnd = random.Random(seed)
    h = size
    d = []
    cx = 0.0
    while cx < length:
        word_len = rnd.randint(3, 8)
        d.append(f"M{cx:.1f},0")
        for _ in range(word_len):
            w = h * rnd.uniform(0.38, 0.62)
            kind = rnd.random()
            if kind < 0.34:      # humps, like n / m
                d.append(f"Q{w * .25:.1f},{-h * .75:.1f} {w * .5:.1f},{-h * .45:.1f} T{w:.1f},0".replace("Q", f"q").replace("T", "t") if False else
                         f"q{w * .22:.1f},{-h * .8:.1f} {w * .5:.1f},{-h * .5:.1f} t{w * .5:.1f},{h * .5:.1f}")
            elif kind < 0.55:    # round, like o / a
                d.append(f"c{-w * .1:.1f},{-h * .8:.1f} {w * .8:.1f},{-h * .8:.1f} {w * .6:.1f},0 c{-w * .1:.1f},{h * .2:.1f} {-w * .3:.1f},{h * .1:.1f} {w * .45:.1f},{-h * .05:.1f}")
            elif kind < 0.75:    # tall loop, like l / h / b
                d.append(f"c{w * .1:.1f},{-h * 2.0:.1f} {w * 1.0:.1f},{-h * 1.8:.1f} {w * .3:.1f},{-h * .15:.1f} q{w * .3:.1f},{h * .1:.1f} {w * .6:.1f},{h * .15:.1f}")
            elif kind < 0.9:     # descender, like g / y
                d.append(f"c{w * .3:.1f},{-h * .5:.1f} {w * .5:.1f},{-h * .3:.1f} {w * .45:.1f},{h * .1:.1f} c{-w * .1:.1f},{h * .9:.1f} {-w * .7:.1f},{h * .8:.1f} {-w * .2:.1f},{h * .1:.1f} q{w * .5:.1f},{-h * .2:.1f} {w * .9:.1f},{-h * .2:.1f}")
            else:                # t / i tick
                d.append(f"l{w * .15:.1f},{-h * .9:.1f} m{w * -.2:.1f},{h * .4:.1f} l{w * .7:.1f},{-h * .05:.1f} l{w * .3:.1f},{h * .55:.1f}")
            cx += w
        cx += h * rnd.uniform(0.6, 1.0)
    return f'<g transform="translate({x:.1f} {y:.1f}) skewX({-slant})"><path d="{"".join(d)}" fill="none" stroke="#4A3524" stroke-width="{max(1.0, size / 14):.2f}" stroke-linecap="round" stroke-linejoin="round"/></g>'
