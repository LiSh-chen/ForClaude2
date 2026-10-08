#!/usr/bin/env python3
"""Writes svg/hero_scene.svg (ferns, flowers and a traveller on a path) and svg/paper_notes.svg
(handwritten botanical notes and small sketches for the margins of the paper page).
Render with `node tools/render_svg.mjs hero_scene paper_notes`."""
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import botanical as b
import make_travellers as t

OUT = Path(__file__).resolve().parent.parent / "svg"
INK = b.INK


def group(parts, filt):
    return f'<g filter="url(#{filt})">{"".join(parts)}</g>'


def plant(kind, *args, **kwargs):
    fn = {"fern": b.fern, "flower": b.flower}[kind]
    wash, ink = fn(*args, **kwargs)
    return f'<g filter="url(#wash)">{wash}</g><g filter="url(#inkFine)">{ink}</g>'


def hero() -> str:
    w, h, ground = 1080, 560, 470
    parts = []
    # far left to right, back to front
    parts.append(plant("fern", 70, ground, 250, 0.35, seed=11, leaflets=20))
    parts.append(plant("fern", 215, ground + 4, 390, 0.22, seed=3, leaflets=28))
    parts.append(plant("fern", 120, ground + 6, 300, -0.28, seed=5, leaflets=24))
    parts.append(plant("fern", 300, ground + 2, 220, 0.4, seed=8, leaflets=18))
    parts.append(f'<g filter="url(#inkFine)">{b.grass(40, ground + 2, 70, 9, seed=2)}{b.grass(330, ground + 3, 60, 8, seed=4)}</g>')
    parts.append(plant("flower", 800, ground + 2, 300, -0.07, seed=21, petal="#C9A3A0"))
    parts.append(plant("flower", 930, ground + 4, 240, 0.12, seed=22, petal="#D2B46C"))
    parts.append(plant("fern", 700, ground + 6, 230, -0.32, seed=14, leaflets=18))
    parts.append(plant("fern", 1010, ground + 4, 280, -0.25, seed=17, leaflets=22))
    parts.append(f'<g filter="url(#inkFine)">{b.grass(870, ground + 3, 60, 10, seed=6)}{b.grass(1050, ground + 2, 70, 8, seed=7)}</g>')

    # the road: a dotted line, stones and footprints
    rnd = random.Random(5)
    road = [f'<path d="M0,{ground + 2} L{w},{ground + 2}" stroke="{INK}" stroke-width="1.6" stroke-dasharray="2 9" stroke-linecap="round" fill="none"/>']
    for i in range(16):
        x = 20 + i * 68 + rnd.uniform(-10, 10)
        y = ground + 14 + rnd.uniform(0, 14)
        road.append(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{rnd.uniform(4, 9):.1f}" ry="{rnd.uniform(2, 4):.1f}" fill="none" stroke="{INK}" stroke-width="1" opacity=".6"/>')
    parts.append(group(road, "inkFine"))

    # the traveller, walking right between the plants
    colours = dict(t.KINDS["fox"])
    colours.update(coat="#7C8A5C", trousers="#7A6648", scarf="#A9674E", accent="#A9784A")
    parts.append(t.traveller_fragment("fox", 395, ground - 322 + 6, 215, **colours))

    # little annotations beside the plants
    notes = [b.handwriting(850, 118, 130, 11, seed=31), b.handwriting(842, 136, 110, 11, seed=32),
             b.handwriting(340, 120, 120, 11, seed=33), b.handwriting(330, 138, 90, 11, seed=34)]
    leaders = (f'<path d="M846,142 Q820,170 800,200" fill="none" stroke="{INK}" stroke-width="1" opacity=".6"/>'
               f'<path d="M335,142 Q300,170 296,215" fill="none" stroke="{INK}" stroke-width="1" opacity=".6"/>')
    parts.append(f'<g filter="url(#inkFine)" opacity=".6">{"".join(notes)}{leaders}</g>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}">\n<!--DEFS-->\n' + "\n".join(parts) + "\n</svg>\n"


def notes_page() -> str:
    w, h = 1080, 2160
    parts = []
    rnd = random.Random(77)

    def paragraph(x, y, lines, size, length, seed, opacity=.5):
        out = []
        for i in range(lines):
            ln = length * rnd.uniform(0.7, 1.0) if i == lines - 1 else length * rnd.uniform(0.9, 1.0)
            out.append(b.handwriting(x + rnd.uniform(0, 6), y + i * size * 1.9, ln, size, seed=seed + i))
        return f'<g filter="url(#inkFine)" opacity="{opacity}">{"".join(out)}</g>'

    # headings and text blocks around the edges, kept faint so the app stays readable on top
    parts.append(paragraph(70, 110, 3, 17, 330, 100, .5))
    parts.append(paragraph(640, 1010, 4, 15, 350, 200, .42))
    parts.append(paragraph(80, 1330, 7, 15, 420, 300, .4))
    parts.append(paragraph(610, 1520, 5, 14, 380, 400, .38))
    parts.append(paragraph(90, 1900, 4, 15, 360, 500, .42))
    parts.append(paragraph(600, 2010, 3, 13, 340, 600, .36))

    # small sketches with leader lines
    parts.append(plant("fern", 130, 960, 280, 0.18, seed=41, leaflets=22))
    parts.append(plant("fern", 220, 960, 200, -0.3, seed=42, leaflets=16))
    parts.append(plant("flower", 880, 900, 300, -0.1, seed=43, petal="#C9A3A0"))
    parts.append(plant("fern", 780, 1850, 220, 0.22, seed=44, leaflets=16))
    parts.append(plant("flower", 160, 1790, 240, 0.08, seed=45, petal="#D2B46C"))
    parts.append(f'<g filter="url(#inkFine)">{b.grass(300, 960, 60, 7, seed=9)}{b.grass(960, 902, 50, 6, seed=10)}</g>')
    # leaf cross-section and seed pods in the margin
    pods = []
    for i in range(4):
        cx, cy = 90 + i * 55, 1190
        pods.append(f'<ellipse cx="{cx}" cy="{cy}" rx="16" ry="30" fill="none" stroke="{INK}" stroke-width="1.2"/><path d="M{cx},{cy - 30} L{cx},{cy + 30}" stroke="{INK}" stroke-width=".8" opacity=".6"/>')
    parts.append(f'<g filter="url(#inkFine)" opacity=".6">{"".join(pods)}</g>')
    arrows = (f'<path d="M300,990 Q360,1040 420,1020" fill="none" stroke="{INK}" stroke-width="1.1"/><path d="M410,1008 L422,1020 L406,1026" fill="none" stroke="{INK}" stroke-width="1.1"/>'
              f'<path d="M820,930 Q760,980 700,980" fill="none" stroke="{INK}" stroke-width="1.1"/>')
    parts.append(f'<g filter="url(#inkFine)" opacity=".5">{arrows}</g>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}">\n<!--DEFS-->\n' + "\n".join(parts) + "\n</svg>\n"


if __name__ == "__main__":
    (OUT / "hero_scene.svg").write_text(hero(), encoding="utf-8")
    (OUT / "paper_notes.svg").write_text(notes_page(), encoding="utf-8")
    print("wrote hero_scene.svg, paper_notes.svg")
