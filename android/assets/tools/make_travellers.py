#!/usr/bin/env python3
"""Writes svg/traveller_{fox,elf,cat,cape}.svg: four small travellers sharing one body, differing in head,
colours and a few extras. Render them with `node tools/render_svg.mjs traveller_fox ...`.

Painter's order matters (back to front), so each part is drawn fully (wash fill, then ink outline) in turn.
"""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "svg"
INK = "#3B2A1A"


class Canvas:
    def __init__(self, k=1.0, ink="ink"):
        self.defs, self.body, self._n = [], [], 0
        self.k, self.ink = k, ink

    def part(self, d, fill, ink=True, width=5, extra_fill_opacity=0.95):
        width = width * self.k
        """One shape: a washed fill and (optionally) a pen outline on top."""
        self._n += 1
        pid = f"p{self._n}"
        self.defs.append(f'<path id="{pid}" d="{d}"/>')
        self.body.append(f'<g filter="url(#wash)"><use href="#{pid}" fill="{fill}" opacity="{extra_fill_opacity}"/></g>')
        if ink:
            self.body.append(f'<g filter="url(#{self.ink})"><use href="#{pid}" fill="none" stroke="{INK}" stroke-width="{width}" stroke-linejoin="round" stroke-linecap="round"/></g>')

    def line(self, d, color=INK, width=5, opacity=1.0):
        width = width * (self.k if color == INK else max(self.k, 0.8))
        self.body.append(f'<g filter="url(#{self.ink})"><path d="{d}" fill="none" stroke="{color}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round" opacity="{opacity}"/></g>')

    def dot(self, cx, cy, r, fill=INK):
        self.body.append(f'<g filter="url(#{self.ink})"><circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}"/></g>')

    def raw(self, text):
        self.body.append(text)

    def svg(self):
        return ('<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 512 768">\n<!--DEFS-->\n'
                '<defs>' + "".join(self.defs) + '</defs>\n' + "\n".join(self.body) + '\n</svg>\n')


def traveller(kind: str, coat: str, trousers: str, scarf: str, face: str, accent: str, fine: bool = False) -> str:
    c = Canvas(k=0.5 if fine else 1.0, ink="inkFine" if fine else "ink")
    # ground shadow
    c.raw('<ellipse cx="270" cy="738" rx="130" ry="12" fill="#3B2A1A" opacity=".16"/>')

    # tails and capes sit behind everything
    if kind == "fox":
        c.part("M205,505 C118,530 62,445 96,350 C108,310 150,300 172,332 C148,385 162,435 218,440 Z", accent)
        c.part("M96,350 C108,310 150,300 172,332 C145,332 120,340 96,350 Z", "#FFF1DC", ink=False)
        c.line("M96,350 C108,310 150,300 172,332", width=4)
    if kind == "cat":
        c.line("M205,505 C130,520 95,430 140,360 C165,325 190,350 170,385", color="#A9A5B8", width=26)
        c.line("M205,505 C130,520 95,430 140,360 C165,325 190,350 170,385", width=4, opacity=.0)
    if kind == "cape":
        c.part("M218,330 C140,380 112,490 140,590 L236,548 Z", accent)

    # back leg, then front leg, then boots
    c.part("M214,520 L258,520 L234,708 L194,708 Z", trousers)
    c.part("M190,702 L242,702 L248,734 L172,734 Q172,716 190,702 Z", "#5E4128")
    c.part("M262,520 L306,520 L342,700 L300,710 Z", trousers)
    c.part("M298,700 L346,692 Q376,712 382,734 L294,736 Z", "#5E4128")

    # walking staff behind the front arm
    c.line("M392,346 L338,736", color="#7A5230", width=14)
    c.line("M392,346 L338,736", width=4, opacity=.55)
    c.part("M392,332 m-16,0 a16,16 0 1,0 32,0 a16,16 0 1,0 -32,0", "#D3A94A", width=4)

    # satchel on the back, then the coat over its strap
    c.part("M148,395 Q148,385 160,385 L232,385 Q244,385 244,397 L244,486 Q244,498 232,498 L160,498 Q148,498 148,486 Z", "#A97B4A")
    c.line("M152,420 L240,420", width=4, opacity=.7)
    c.dot(196, 428, 7, "#D3A94A")
    if kind == "cape":
        # a rolled map strapped on top of the pack
        c.part("M146,375 L250,375 L250,395 L146,395 Z", "#F1E3BE", width=4)
        c.part("M146,375 m-10,10 a10,10 0 1,1 20,0 a10,10 0 1,1 -20,0", "#E3CF9B", width=4)

    c.part("M206,338 C206,318 322,318 326,340 L350,548 C300,570 226,570 178,552 Z", coat)
    c.line("M190,520 C250,540 310,540 342,522", width=4, opacity=.7)  # belt line
    for y in (400, 450):
        c.dot(262, y, 5, "#7C5A1B")
    # scarf / collar
    c.part("M208,336 C230,358 304,358 326,336 C326,318 208,318 208,336 Z", scarf)
    c.part("M300,345 C316,370 320,400 322,420 L296,424 C294,398 288,372 276,356 Z", scarf, width=4)

    # front arm, hand on the staff
    c.part("M292,350 C330,362 354,410 362,456 L336,474 C328,430 306,398 280,376 Z", coat)
    c.part("M371,470 m-19,0 a19,19 0 1,0 38,0 a19,19 0 1,0 -38,0", face, width=4)

    # --- heads
    hx, hy = 275, 232
    if kind == "fox":
        c.part("M215,190 L206,92 L268,150 Z", accent)           # ears behind the head
        c.part("M286,148 L338,86 L346,176 Z", accent)
        c.part("M222,150 L218,118 L248,146 Z", "#D3A6BC", ink=False)
        c.part("M300,140 L330,112 L334,150 Z", "#D3A6BC", ink=False)
        c.part("M196,222 C190,170 250,142 306,152 C346,160 372,210 404,262 C384,282 350,296 330,318 C282,336 220,310 196,222 Z", "#FFF1DC")
        c.part("M196,222 C190,170 250,142 306,152 C300,190 262,214 248,262 C232,280 210,262 196,222 Z", accent, ink=False)
        c.dot(398, 262, 12)                                      # nose
        c.dot(332, 232, 8)                                       # eye
        c.dot(335, 229, 2.5, "#FFF1DC")
        c.part("M300,276 m-16,0 a16,10 0 1,0 32,0 a16,10 0 1,0 -32,0", "#D3A6BC", ink=False, extra_fill_opacity=.7)
        c.line("M350,285 Q372,280 388,284", width=3)
    elif kind == "cat":
        c.part("M208,190 L200,96 L262,150 Z", coat)
        c.part("M290,148 L348,92 L350,182 Z", coat)
        c.part("M214,152 L214,124 L242,146 Z", "#D3A6BC", ink=False)
        c.part("M304,140 L336,112 L338,152 Z", "#D3A6BC", ink=False)
        c.part(f"M{hx},{hy} m-82,0 a82,76 0 1,0 164,0 a82,76 0 1,0 -164,0", face)
        c.dot(322, 236, 8)
        c.dot(325, 233, 2.5, "#FFFFFF")
        c.dot(352, 262, 6, "#C27A8C")
        for dy, dx in ((-8, 0), (4, 6), (16, 0)):
            c.line(f"M{356},{266 + dy} L{416 + dx},{258 + dy * 1.8}", width=2.5, opacity=.8)
    elif kind == "elf":
        c.part("M200,248 L132,214 L204,222 Z", face)           # pointed ear behind
        c.part(f"M{hx},{hy} m-76,0 a76,74 0 1,0 152,0 a76,74 0 1,0 -152,0", face)
        c.dot(322, 238, 8)
        c.dot(325, 235, 2.5, "#FFFFFF")
        c.line("M338,268 Q350,272 360,266", width=3)
        c.part("M196,206 C206,118 320,102 352,184 C306,152 250,160 196,206 Z", accent)   # leaf cap
        c.part("M330,158 C360,118 396,128 402,150 C376,150 352,150 330,158 Z", accent)    # little leaf tip
        c.line("M236,168 C270,150 310,152 336,172", width=3, opacity=.7)
    elif kind == "cape":
        c.part(f"M{hx},{hy} m-74,0 a74,72 0 1,0 148,0 a74,72 0 1,0 -148,0", face)
        c.part("M201,214 C205,170 270,150 330,176 C300,188 270,196 244,230 C226,248 206,246 201,214 Z", "#7A5230", ink=False)
        c.dot(322, 240, 8)
        c.dot(325, 237, 2.5, "#FFFFFF")
        c.line("M330,270 Q342,274 352,268", width=3)
        # wide-brimmed explorer's hat
        c.part("M275,196 m-132,0 a132,28 0 1,0 264,0 a132,28 0 1,0 -264,0", "#D9C79A")
        c.part("M205,194 C208,120 342,116 346,192 C300,206 250,206 205,194 Z", "#D9C79A")
        c.part("M207,184 C260,196 296,196 344,182 L346,198 C296,210 252,210 205,198 Z", accent, ink=False)
    return c.svg()


def traveller_fragment(kind: str, x: float, y: float, width: float, **colours) -> str:
    """The traveller as a nested <svg>, ready to place inside a larger scene (fine pen lines)."""
    doc = traveller(kind, fine=True, **colours)
    inner = doc.split("<!--DEFS-->", 1)[1].rsplit("</svg>", 1)[0]
    height = width * 768 / 512
    return f'<svg x="{x}" y="{y}" width="{width}" height="{height}" viewBox="0 0 512 768">{inner}</svg>'


KINDS = {
    "fox": dict(coat="#E0A064", trousers="#8A6B4A", scarf="#D3A6BC", face="#FFF1DC", accent="#E0A064"),
    "elf": dict(coat="#8DB584", trousers="#7A6A48", scarf="#E3F0C8", face="#F4E1C2", accent="#6FA06A"),
    "cat": dict(coat="#A9A5B8", trousers="#7D7480", scarf="#D3A6BC", face="#F2E6E0", accent="#A9A5B8"),
    "cape": dict(coat="#8DA9CF", trousers="#7A6A48", scarf="#E3CF9B", face="#F4E1C2", accent="#C86B5E"),
}

if __name__ == "__main__":
    for kind, colours in KINDS.items():
        (OUT / f"traveller_{kind}.svg").write_text(traveller(kind, **colours), encoding="utf-8")
        print("wrote", f"traveller_{kind}.svg")
