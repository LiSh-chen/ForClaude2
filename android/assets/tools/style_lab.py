#!/usr/bin/env python3
"""Five art directions for the home screen, drawn with the same scene so they can be compared.

    python3 tools/style_lab.py && node tools/render_svg.mjs style_a style_b style_c style_d style_e_small \
        && python3 tools/style_lab.py --finish

Writes svg/style_*.svg; --finish post-processes the pixel version and builds previews/styles_*.jpg.
"""
import base64
import io
import math
import random
import re
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import botanical as b
import make_travellers as t

ROOT = Path(__file__).resolve().parent.parent
SVG, GEN, PREV = ROOT / "svg", ROOT / "generated", ROOT / "previews"
W, H = 540, 1080
STEPS, GOAL = 6248, 9000
PROGRESS = STEPS / GOAL


def data_uri(img: Image.Image, fmt="PNG", **kw) -> str:
    buf = io.BytesIO()
    img.save(buf, fmt, **kw)
    return f"data:image/{fmt.lower()};base64," + base64.b64encode(buf.getvalue()).decode()


def recolor(markup: str, palette, seed=0) -> str:
    rnd = random.Random(seed)
    return re.sub(r'fill="#[0-9A-Fa-f]{6}"', lambda m: f'fill="{rnd.choice(palette)}"', markup)


def arc(cx, cy, r, fraction):
    a = -math.pi / 2 + 2 * math.pi * min(fraction, 0.9999)
    x, y = cx + r * math.cos(a), cy + r * math.sin(a)
    return f"M{cx},{cy - r} A{r},{r} 0 {1 if fraction > .5 else 0} 1 {x:.1f},{y:.1f}"


def text(x, y, s, size, fill, anchor="middle", weight="normal", family="'WenQuanYi Zen Hei Mono','IPAGothic',serif", extra=""):
    return f'<text x="{x}" y="{y}" font-size="{size}" fill="{fill}" text-anchor="{anchor}" font-weight="{weight}" font-family="{family}" {extra}>{s}</text>'


STAT = [("距離", "4.41 公里"), ("消耗", "251 大卡"), ("離目標", "2,752 步")]
NAV = ["今日", "旅程", "統計", "設定"]
FOX = dict(t.KINDS["fox"])


def svg_doc(body: str, w=W, h=H, defs="") -> str:
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}">\n<!--DEFS-->\n<defs>{defs}</defs>\n{body}\n</svg>\n'


def fern_pair(x, y, height, lean, seed, leaflets=18):
    return b.fern(x, y, height, lean, seed=seed, leaflets=leaflets)


# ----------------------------------------------------------------------------------------------------
# A. 復古植物圖鑑 — pen and earth-tone wash on old paper (what the app has now)
def style_a() -> str:
    paper = Image.open(GEN / "paper_page.png").convert("RGB").resize((W, H), Image.LANCZOS)
    hero = Image.open(GEN / "hero_scene.png").convert("RGBA").resize((540, 280), Image.LANCZOS)
    face = Image.open(GEN / "compass_face.png").convert("RGBA").resize((300, 300), Image.LANCZOS)
    case = Image.open(GEN / "compass_case.png").convert("RGBA").resize((528, 577), Image.LANCZOS)
    compass = Image.new("RGBA", (528, 577), (0, 0, 0, 0))
    compass.paste(face, (264 - 150, int(577 * .57) - 150), face)
    compass.alpha_composite(case)
    compass = compass.resize((300, 328), Image.LANCZOS)
    cards = ""
    for i, (label, value) in enumerate(STAT):
        x = 15 + i * 170
        cards += f'<g filter="url(#inkFine)"><path d="M{x},760 l160,3 l-2,92 l-156,-2 z" fill="#E6D3A5" stroke="#3B2A1A" stroke-width="1.4" stroke-linejoin="round"/></g>'
        cards += text(x + 80, 795, label, 15, "#3B2A1A") + text(x + 80, 830, value, 17, "#3B2A1A", weight="bold")
    nav = '<rect x="0" y="985" width="540" height="95" fill="#E6D3A5" opacity=".94"/><path d="M0,985 H540" stroke="#3B2A1A" stroke-width="1.2" opacity=".6"/>'
    for i, n in enumerate(NAV):
        nav += text(68 + i * 135, 1040, n, 15, "#3B2A1A", weight="bold" if i == 0 else "normal")
    body = (f'<image href="{data_uri(paper, "JPEG", quality=88)}" x="0" y="0" width="{W}" height="{H}"/>'
            + text(270, 66, "星霧大陸", 32, "#3B2A1A", weight="bold", family="'WenQuanYi Zen Hei Mono','IPAGothic',serif")
            + f'<image href="{data_uri(hero)}" x="0" y="82" width="540" height="280"/>'
            + f'<image href="{data_uri(compass)}" x="120" y="372" width="300" height="328"/>'
            + f'<path d="{arc(270, 372 + 328 * .57, 108, PROGRESS)}" fill="none" stroke="#5F8A52" stroke-width="12" stroke-linecap="round" filter="url(#inkFine)" opacity=".95"/>'
            + text(270, 572, f"{STEPS:,}", 46, "#3B2A1A", weight="bold", family="'DejaVu Serif',serif")
            + text(270, 602, f"/ {GOAL:,} 步", 16, "#3B2A1A") + text(270, 626, f"{round(PROGRESS * 100)}%", 18, "#3B2A1A", weight="bold")
            + cards + nav)
    return svg_doc(body)


# ----------------------------------------------------------------------------------------------------
# B. 水彩繪本 — soft pastel washes, no outlines, wet edges and paper grain
WC = '''<filter id="wc" filterUnits="userSpaceOnUse" x="-100" y="-100" width="800" height="1300" color-interpolation-filters="sRGB">
  <feTurbulence type="fractalNoise" baseFrequency="0.016" numOctaves="3" seed="5" result="n"/>
  <feDisplacementMap in="SourceGraphic" in2="n" scale="9" xChannelSelector="R" yChannelSelector="G" result="d"/>
  <feGaussianBlur in="d" stdDeviation="1.1" result="b"/>
  <feMorphology in="b" operator="erode" radius="2.2" result="e"/>
  <feComposite in="b" in2="e" operator="out" result="rim"/>
  <feColorMatrix in="rim" type="matrix" values="0 0 0 0 .25  0 0 0 0 .2  0 0 0 0 .25  0 0 0 .34 0" result="rimDark"/>
  <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="2" seed="12" result="p"/>
  <feColorMatrix in="p" type="matrix" values="0.22 0 0 0 .82  0.22 0 0 0 .82  0.22 0 0 0 .82  0 0 0 0 1" result="shade"/>
  <feComposite in="shade" in2="b" operator="in" result="sIn"/>
  <feBlend in="sIn" in2="b" mode="multiply" result="body"/>
  <feMerge><feMergeNode in="body"/><feMergeNode in="rimDark"/></feMerge>
</filter>
<filter id="grain" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed="3"/>
  <feColorMatrix type="matrix" values="0 0 0 0 .4  0 0 0 0 .35  0 0 0 0 .3  0 0 0 .13 0"/></filter>'''


def style_b() -> str:
    greens = ["#8FBFA8", "#7FB09A", "#A5CDB3", "#B9D8A8"]
    wash_a, _ = fern_pair(70, 335, 190, 0.22, 3, 20)
    wash_b, _ = fern_pair(135, 335, 140, -0.25, 5, 16)
    wash_c, _ = fern_pair(470, 335, 130, -0.3, 8, 16)
    fl_w, _ = b.flower(405, 335, 150, -0.06, seed=21, petal="#F2A9A0")
    ferns = recolor(wash_a + wash_b + wash_c, greens, 2)
    flower = re.sub(r'fill="#C9A3A0"', 'fill="#F2A9A0"', fl_w)
    flower = re.sub(r'fill="#(4F6B45|5E7A4E|6F8A55|7C8A4E)"', lambda m: random.Random(m.start()).choice(greens), flower)
    colours = dict(coat="#F2B27A", trousers="#9B8A7A", scarf="#F2A9A0", face="#FFF3E2", accent="#F0A868", wash="wc", outline=False, fine=False)
    fox = t.traveller_fragment("fox", 215, 190, 150, **colours)
    sky = '<defs><linearGradient id="skyB" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FCE3CF"/><stop offset="1" stop-color="#E5F0E6"/></linearGradient></defs>'
    scene = (f'<g filter="url(#wc)"><rect x="-10" y="82" width="560" height="290" fill="url(#skyB)"/></g>'
             '<g filter="url(#wc)"><circle cx="455" cy="150" r="30" fill="#F8C98A"/></g>'
             '<g filter="url(#wc)" opacity=".9"><path d="M-10,300 Q100,230 220,290 T430,280 T560,300 V345 H-10 Z" fill="#CFE3CC"/></g>'
             '<g filter="url(#wc)"><path d="M-10,325 Q120,290 260,325 T560,320 V372 H-10 Z" fill="#B9D6B2"/></g>'
             f'<g filter="url(#wc)">{ferns}{flower}</g>'
             f'{fox}'
             '<g filter="url(#wc)"><ellipse cx="270" cy="336" rx="80" ry="7" fill="#9BB394" opacity=".6"/></g>')
    cards = ""
    for i, (label, value) in enumerate(STAT):
        x = 15 + i * 170
        cards += f'<g filter="url(#wc)"><rect x="{x}" y="760" width="160" height="92" rx="22" fill="{["#F9D8CB", "#D6EAD9", "#D7E3F3"][i]}"/></g>'
        cards += text(x + 80, 795, label, 15, "#6F5F5A") + text(x + 80, 830, value, 17, "#4B3F3B", weight="bold")
    nav = '<g filter="url(#wc)"><rect x="0" y="990" width="540" height="100" rx="26" fill="#F6E8D8"/></g>'
    for i, n in enumerate(NAV):
        nav += text(68 + i * 135, 1045, n, 15, "#4B3F3B", weight="bold" if i == 0 else "normal")
    dial = ('<g filter="url(#wc)"><circle cx="270" cy="565" r="132" fill="#FBEBD6"/></g>'
            '<g filter="url(#wc)"><circle cx="270" cy="565" r="112" fill="none" stroke="#E4DACB" stroke-width="22"/></g>'
            f'<g filter="url(#wc)"><path d="{arc(270, 565, 112, PROGRESS)}" fill="none" stroke="#F08A7A" stroke-width="22" stroke-linecap="round"/></g>'
            + text(270, 568, f"{STEPS:,}", 44, "#4B3F3B", weight="bold", family="'DejaVu Serif',serif")
            + text(270, 598, f"/ {GOAL:,} 步", 16, "#6F5F5A") + text(270, 624, f"{round(PROGRESS * 100)}%", 18, "#D5705F", weight="bold"))
    blobs = ('<g filter="url(#wc)" opacity=".5"><circle cx="40" cy="470" r="70" fill="#F9D8CB"/><circle cx="520" cy="700" r="90" fill="#D6EAD9"/>'
             '<circle cx="470" cy="930" r="70" fill="#D7E3F3"/></g>')
    body = ('<rect width="540" height="1080" fill="#FBF6EC"/>' + blobs + text(270, 62, "星霧大陸", 32, "#4B3F3B", weight="bold")
            + scene + dial + cards + text(270, 888, "距離與熱量為估算", 12, "#8C7F78") + nav
            + '<rect width="540" height="1080" filter="url(#grain)"/>')
    return svg_doc(body, defs=WC)


# ----------------------------------------------------------------------------------------------------
# C. 剪紙層疊 — flat coloured paper, stacked, with soft cast shadows
SH = '''<filter id="shadow" x="-20%" y="-20%" width="140%" height="150%"><feDropShadow dx="0" dy="3" stdDeviation="3.2" flood-color="#1E2B2A" flood-opacity=".35"/></filter>
<filter id="shadowS" x="-20%" y="-20%" width="140%" height="150%"><feDropShadow dx="0" dy="2" stdDeviation="1.6" flood-color="#1E2B2A" flood-opacity=".35"/></filter>'''


def style_c() -> str:
    greens = ["#2F6B57", "#3B7B63", "#2A5C4E", "#4A8C6B"]
    wash_a, _ = fern_pair(70, 335, 190, 0.22, 3, 14)
    wash_b, _ = fern_pair(135, 335, 140, -0.25, 5, 12)
    wash_c, _ = fern_pair(470, 335, 130, -0.3, 8, 12)
    fl_w, _ = b.flower(405, 335, 150, -0.06, seed=21, petal="#E8B44F")
    flower = re.sub(r'fill="#C9A3A0"', 'fill="#E8B44F"', fl_w)
    flower = re.sub(r'fill="#(4F6B45|5E7A4E|6F8A55|7C8A4E)"', lambda m: random.Random(m.start()).choice(greens), flower)
    colours = dict(coat="#D9744B", trousers="#4B3A52", scarf="#F6EBD7", face="#F6EBD7", accent="#E58A57", wash="", outline=False, fine=False)
    fox = t.traveller_fragment("fox", 215, 190, 150, **colours)
    cards = ""
    for i, (label, value) in enumerate(STAT):
        x = 15 + i * 170
        cards += f'<rect x="{x}" y="760" width="160" height="92" rx="12" fill="#F6EBD7" filter="url(#shadow)"/>'
        cards += text(x + 80, 795, label, 15, "#7A5C48") + text(x + 80, 830, value, 17, "#2E3E3B", weight="bold")
    nav = '<rect x="0" y="990" width="540" height="100" fill="#2F5D62" filter="url(#shadow)"/>'
    for i, n in enumerate(NAV):
        nav += text(68 + i * 135, 1045, n, 15, "#F6EBD7", weight="bold" if i == 0 else "normal")
    scene = ('<rect x="0" y="82" width="540" height="290" fill="#F6D9A8" filter="url(#shadow)"/>'
             '<circle cx="455" cy="150" r="32" fill="#E8B44F" filter="url(#shadowS)"/>'
             '<path d="M0,250 Q120,190 250,245 T540,230 V372 H0 Z" fill="#6E9C86" filter="url(#shadow)"/>'
             '<path d="M0,290 Q150,240 290,292 T540,282 V372 H0 Z" fill="#3F7A66" filter="url(#shadow)"/>'
             '<path d="M0,330 Q150,300 300,330 T540,325 V372 H0 Z" fill="#2F5D4E" filter="url(#shadow)"/>'
             f'<g filter="url(#shadowS)">{recolor(wash_a + wash_b + wash_c, greens, 4)}{flower}</g>{fox}')
    dial = ('<circle cx="270" cy="565" r="140" fill="#2F5D62" filter="url(#shadow)"/>'
            '<circle cx="270" cy="565" r="118" fill="#F6EBD7" filter="url(#shadowS)"/>'
            f'<path d="{arc(270, 565, 128, PROGRESS)}" fill="none" stroke="#D9744B" stroke-width="16" stroke-linecap="round" filter="url(#shadowS)"/>'
            '<circle cx="270" cy="565" r="92" fill="#E8D5B0" filter="url(#shadowS)"/>'
            + text(270, 570, f"{STEPS:,}", 42, "#2E3E3B", weight="bold", family="'DejaVu Sans',sans-serif")
            + text(270, 598, f"/ {GOAL:,} 步", 15, "#7A5C48") + text(270, 622, f"{round(PROGRESS * 100)}%", 17, "#D9744B", weight="bold"))
    body = ('<rect width="540" height="1080" fill="#2A4B4F"/><path d="M0,0 H540 V460 Q270,540 0,460 Z" fill="#335C60"/>'
            + text(270, 62, "星霧大陸", 32, "#F6EBD7", weight="bold") + scene + dial + cards + nav)
    return svg_doc(body, defs=SH)


# ----------------------------------------------------------------------------------------------------
# D. 木刻版畫 — carved outlines, four inks, hatching and grain
WOOD = '''<filter id="carve" filterUnits="userSpaceOnUse" x="-100" y="-100" width="800" height="1300"><feTurbulence type="fractalNoise" baseFrequency="0.05" numOctaves="2" seed="7" result="n"/>
  <feDisplacementMap in="SourceGraphic" in2="n" scale="4.5" xChannelSelector="R" yChannelSelector="G"/></filter>
<filter id="woodgrain" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency="0.012 0.5" numOctaves="2" seed="4"/>
  <feColorMatrix type="matrix" values="0 0 0 0 .1  0 0 0 0 .08  0 0 0 0 .06  0 0 0 .22 0"/></filter>
<pattern id="lines" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(30)"><line x1="0" y1="0" x2="0" y2="6" stroke="#1B1A17" stroke-width="1.6"/></pattern>'''


def style_d() -> str:
    ink = "#1B1A17"
    indigo = "#243B5A"
    wash_a, ink_a = fern_pair(70, 335, 190, 0.22, 3, 12)
    wash_b, ink_b = fern_pair(135, 335, 140, -0.25, 5, 10)
    wash_c, ink_c = fern_pair(470, 335, 130, -0.3, 8, 10)
    fl_w, fl_i = b.flower(405, 335, 150, -0.06, seed=21, petal="#C8452F")
    flower = re.sub(r'fill="#C9A3A0"', 'fill="#C8452F"', fl_w)
    flower = re.sub(r'fill="#(4F6B45|5E7A4E|6F8A55|7C8A4E)"', lambda m: indigo, flower)
    bold = lambda s: s.replace('stroke-width="1.1', 'stroke-width="2.4').replace(f'stroke="{b.INK}"', f'stroke="{ink}"')
    colours = dict(coat="#C8452F", trousers="#243B5A", scarf="#D9A13B", face="#EFE3C8", accent="#D9A13B", wash="", outline=True, fine=False, ink="carve", k=1.0)
    fox = t.traveller_fragment("fox", 215, 190, 150, **colours)
    cards = ""
    for i, (label, value) in enumerate(STAT):
        x = 15 + i * 170
        cards += (f'<g filter="url(#carve)"><rect x="{x}" y="760" width="160" height="92" fill="#EFE3C8" stroke="{ink}" stroke-width="3.5"/>'
                  f'<rect x="{x + 6}" y="766" width="148" height="80" fill="none" stroke="{ink}" stroke-width="1.3"/></g>')
        cards += text(x + 80, 795, label, 15, ink, weight="bold") + text(x + 80, 830, value, 17, "#C8452F", weight="bold")
    nav = f'<rect x="0" y="990" width="540" height="100" fill="{indigo}"/><path d="M0,990 H540" stroke="{ink}" stroke-width="4"/>'
    for i, n in enumerate(NAV):
        nav += text(68 + i * 135, 1045, n, 16, "#EFE3C8" if i else "#D9A13B", weight="bold")
    sun = "".join(f'<circle cx="455" cy="150" r="{r}" fill="none" stroke="{ink}" stroke-width="2" opacity=".8"/>' for r in (24, 34, 44))
    scene = (f'<g filter="url(#carve)"><rect x="0" y="82" width="540" height="290" fill="#EFE3C8" stroke="{ink}" stroke-width="4"/>'
             f'<rect x="0" y="82" width="540" height="130" fill="url(#lines)" opacity=".35"/>'
             f'<circle cx="455" cy="150" r="22" fill="#C8452F" stroke="{ink}" stroke-width="3"/>{sun}'
             f'<path d="M0,290 Q120,230 250,285 T540,272 V372 H0 Z" fill="{indigo}" stroke="{ink}" stroke-width="3.5"/>'
             f'<path d="M0,290 Q120,230 250,285 T540,272 V372 H0 Z" fill="url(#lines)" opacity=".45"/>'
             f'{wash_a}{wash_b}{wash_c}{flower}{bold(ink_a + ink_b + ink_c + fl_i)}</g>{fox}')
    ticks = "".join(
        f'<line x1="{270 + 118 * math.sin(i * math.pi / 30):.1f}" y1="{565 - 118 * math.cos(i * math.pi / 30):.1f}" x2="{270 + (130 if i % 5 == 0 else 124) * math.sin(i * math.pi / 30):.1f}" y2="{565 - (130 if i % 5 == 0 else 124) * math.cos(i * math.pi / 30):.1f}" stroke="{ink}" stroke-width="{3 if i % 5 == 0 else 1.6}"/>'
        for i in range(60))
    dial = (f'<g filter="url(#carve)"><circle cx="270" cy="565" r="138" fill="#EFE3C8" stroke="{ink}" stroke-width="5"/>{ticks}'
            f'<circle cx="270" cy="565" r="104" fill="none" stroke="{ink}" stroke-width="2"/>'
            f'<path d="{arc(270, 565, 112, PROGRESS)}" fill="none" stroke="#C8452F" stroke-width="16" stroke-linecap="butt"/>'
            f'<path d="{arc(270, 565, 112, PROGRESS)}" fill="none" stroke="{ink}" stroke-width="2" stroke-dasharray="3 5" opacity=".9"/></g>'
            + text(270, 570, f"{STEPS:,}", 44, ink, weight="bold", family="'DejaVu Serif',serif")
            + text(270, 598, f"/ {GOAL:,} 步", 15, ink) + text(270, 622, f"{round(PROGRESS * 100)}%", 17, "#C8452F", weight="bold"))
    body = ('<rect width="540" height="1080" fill="#E9DDC0"/>' + text(270, 62, "星霧大陸", 34, ink, weight="bold") + scene + dial + cards + nav
            + '<rect width="540" height="1080" filter="url(#woodgrain)"/>')
    return svg_doc(body, defs=WOOD)


# ----------------------------------------------------------------------------------------------------
# E. 像素冒險 — drawn small with hard edges, then reduced to a 16-colour palette and enlarged
PICO = ["#140c1c", "#442434", "#30346d", "#4e4a4e", "#854c30", "#346524", "#d04648", "#757161",
        "#597dce", "#d27d2c", "#8595a1", "#6daa2c", "#d2aa99", "#6dc2ca", "#dad45e", "#deeed6"]


def style_e_small() -> str:
    """180 x 360 canvas (everything is drawn at one third of the final size)."""
    s = 1 / 3
    greens = ["#346524", "#6daa2c", "#4e8a28"]
    wash_a, _ = fern_pair(70, 335, 190, 0.22, 3, 9)
    wash_b, _ = fern_pair(135, 335, 140, -0.25, 5, 8)
    wash_c, _ = fern_pair(470, 335, 130, -0.3, 8, 8)
    fl_w, _ = b.flower(405, 335, 150, -0.06, seed=21, petal="#d04648")
    flower = re.sub(r'fill="#C9A3A0"', 'fill="#d04648"', fl_w)
    flower = re.sub(r'fill="#(4F6B45|5E7A4E|6F8A55|7C8A4E)"', lambda m: random.Random(m.start()).choice(greens), flower)
    colours = dict(coat="#d27d2c", trousers="#442434", scarf="#d04648", face="#deeed6", accent="#d27d2c", wash="", outline=False, fine=False)
    fox = t.traveller_fragment("fox", 215, 190, 150, **colours)
    cards = ""
    for i, (label, value) in enumerate(STAT):
        x = 15 + i * 170
        cards += f'<rect x="{x}" y="760" width="160" height="92" fill="#30346d"/><rect x="{x + 4}" y="764" width="152" height="84" fill="none" stroke="#6dc2ca" stroke-width="3"/>'
        cards += text(x + 80, 797, label, 17, "#6dc2ca", weight="bold") + text(x + 80, 832, value, 19, "#deeed6", weight="bold")
    nav = '<rect x="0" y="990" width="540" height="100" fill="#442434"/><rect x="0" y="990" width="540" height="8" fill="#854c30"/>'
    for i, n in enumerate(NAV):
        nav += text(68 + i * 135, 1048, n, 18, "#dad45e" if i == 0 else "#d2aa99", weight="bold")
    stars = "".join(f'<rect x="{x}" y="{y}" width="6" height="6" fill="#deeed6"/>' for x, y in [(30, 110), (140, 100), (260, 126), (360, 108), (500, 170), (90, 190)])
    scene = ('<rect x="0" y="82" width="540" height="290" fill="#30346d"/><rect x="0" y="230" width="540" height="142" fill="#597dce"/>' + stars
             + '<circle cx="455" cy="150" r="30" fill="#dad45e"/>'
             '<path d="M0,282 L70,226 L130,270 L210,214 L290,274 L370,230 L440,270 L540,224 V372 H0 Z" fill="#4e4a4e"/>'
             '<path d="M0,330 Q150,296 300,330 T540,322 V372 H0 Z" fill="#346524"/>'
             f'{wash_a}{wash_b}{wash_c}{flower}{fox}'
             '<rect x="0" y="336" width="540" height="36" fill="#6daa2c"/><rect x="0" y="336" width="540" height="6" fill="#346524"/>')
    dial = ('<circle cx="270" cy="565" r="140" fill="#140c1c"/><circle cx="270" cy="565" r="130" fill="#854c30"/><circle cx="270" cy="565" r="116" fill="#30346d"/>'
            f'<path d="{arc(270, 565, 123, PROGRESS)}" fill="none" stroke="#6daa2c" stroke-width="14"/>'
            + text(270, 574, f"{STEPS:,}", 46, "#deeed6", weight="bold", family="'DejaVu Sans Mono',monospace")
            + text(270, 602, f"/ {GOAL:,} 步", 16, "#6dc2ca") + text(270, 628, f"{round(PROGRESS * 100)}%", 20, "#dad45e", weight="bold"))
    body = (f'<g transform="scale({s})" shape-rendering="crispEdges"><rect width="540" height="1080" fill="#140c1c"/>'
            + text(270, 64, "星霧大陸", 34, "#dad45e", weight="bold") + scene + dial + cards + nav + "</g>")
    return svg_doc(body, 180, 360)


def finish():
    PREV.mkdir(exist_ok=True)
    pal = Image.new("P", (1, 1))
    flat = []
    for c in PICO:
        flat += [int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16)]
    pal.putpalette(flat + [0] * (768 - len(flat)))
    small = Image.open(GEN / "style_e_small.png").convert("RGB").quantize(palette=pal, dither=Image.Dither.NONE)
    small.convert("RGB").resize((540, 1080), Image.NEAREST).save(GEN / "style_e.png")
    names = ["style_a", "style_b", "style_c", "style_d", "style_e"]
    sheet = Image.new("RGB", (540 * 5 + 40, 1080), (255, 255, 255))
    for i, n in enumerate(names):
        im = Image.open(GEN / f"{n}.png").convert("RGB")
        im.save(PREV / f"{n}.jpg", quality=90)
        sheet.paste(im, (i * 550, 0))
    sheet.resize((sheet.width // 2, sheet.height // 2), Image.LANCZOS).save(PREV / "styles_overview.jpg", quality=90)
    print("finished previews")


if __name__ == "__main__":
    if "--finish" in sys.argv:
        finish()
    else:
        for name, fn in [("style_a", style_a), ("style_b", style_b), ("style_c", style_c), ("style_d", style_d), ("style_e_small", style_e_small)]:
            (SVG / f"{name}.svg").write_text(fn(), encoding="utf-8")
        print("wrote style svgs")
