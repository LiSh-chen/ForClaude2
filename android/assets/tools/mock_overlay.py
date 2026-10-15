#!/usr/bin/env python3
"""Trial: the original painted island map as the ground, with real rivers, roads, route and landmark pictures drawn on top."""
import math, sys, pathlib, re, random
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageChops, ImageFilter
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import mock_map as M
from shapely import wkb
import pyarrow.compute as pc

ASSETS = pathlib.Path(__file__).resolve().parent.parent
BASE = Image.open(ASSETS / 'generated/real_map_base.png').convert('RGB')
FONT = ImageFont.truetype('/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc', 26)
LON0, LON1, LAT0, LAT1 = 119.95, 122.10, 21.80, 25.40

def route_pts():
    src = (ASSETS.parent / 'core/src/main/kotlin/com/starmist/core/route/TaiwanLoop.kt').read_text().split('val route')[-1]
    return [(float(m.group(4)), float(m.group(3))) for m in re.finditer(r'(card|supply|stop|via)\(((?:"[^"]*",\s*){0,3}?)(\d+\.\d+),\s*(\d+\.\d+)', src)]

def wobble(P, amp, seed):
    rnd = random.Random(seed); out = []
    for i, (x, y) in enumerate(P):
        out.append((x + rnd.uniform(-amp, amp), y + rnd.uniform(-amp, amp)))
    return out

def render(center, width_km, out, art=(), W=920, aspect=0.92, rivers_min='river'):
    lat0, lon0 = center
    kx = 111.32 * math.cos(math.radians(lat0)); ky = 110.57
    wkm = width_km; hkm = wkm / aspect
    dlon, dlat = wkm / kx, hkm / ky
    west, east, south, north = lon0 - dlon/2, lon0 + dlon/2, lat0 - dlat/2, lat0 + dlat/2
    H = int(W / aspect)
    # ground: crop of the painted map, enlarged (it is only 1116 px for the whole island)
    bw, bh = BASE.size
    box = ((west - LON0) / (LON1 - LON0) * bw, (LAT1 - north) / (LAT1 - LAT0) * bh, (east - LON0) / (LON1 - LON0) * bw, (LAT1 - south) / (LAT1 - LAT0) * bh)
    im = BASE.crop(tuple(int(round(v)) for v in box)).resize((W, H), Image.BICUBIC)
    S = 2
    ov = Image.new('RGBA', (W * S, H * S), (0, 0, 0, 0)); d = ImageDraw.Draw(ov)
    def px(lon, lat): return ((lon - west) / (east - west) * W * S, (north - lat) / (north - south) * H * S)
    bbox = (west, south, east, north)
    k = 0
    for r in M.fetch('base', 'water', bbox, ['class', 'geometry']):
        g = wkb.loads(r['geometry']); cl = r['class']
        if g.geom_type not in ('LineString', 'MultiLineString'): continue
        if rivers_min == 'river' and cl != 'river': continue
        if cl in ('drain', 'ditch', 'canal'): continue
        wd = {'river': 3.6, 'stream': 1.6}.get(cl, 1.4)
        for pts in M.geom_pts(g):
            P = wobble([px(x, y) for x, y in pts], 0.9 * S, k); k += 1
            d.line(P, fill=(70, 98, 118, 235), width=int((wd + 1.1) * S), joint='curve')
            d.line(P, fill=(176, 204, 210, 255), width=int(wd * S), joint='curve')
    for r in M.fetch('transportation', 'segment', bbox, ['class', 'geometry'], pc.field('subtype') == 'road'):
        cl = r['class']
        if cl not in ('primary', 'secondary', 'trunk') and not (cl == 'tertiary' and width_km <= 20): continue
        g = wkb.loads(r['geometry'])
        if g.geom_type != 'LineString': continue
        P = [px(x, y) for x, y in g.coords]
        col = (120, 78, 46, 235) if cl != 'tertiary' else (120, 78, 46, 150)
        wd = 1.8 if cl != 'tertiary' else 1.0
        # dotted like an old map road
        seglen = 7 * S; pos = 0
        for a, b in zip(P, P[1:]):
            L = math.hypot(b[0] - a[0], b[1] - a[1])
            if L < 1e-6: continue
            t = 0
            while t < L:
                t2 = min(L, t + 4 * S)
                pa = (a[0] + (b[0] - a[0]) * t / L, a[1] + (b[1] - a[1]) * t / L); pb = (a[0] + (b[0] - a[0]) * t2 / L, a[1] + (b[1] - a[1]) * t2 / L)
                d.line([pa, pb], fill=col, width=int(wd * S)); t += seglen
    rp = route_pts()
    P = [px(x, y) for x, y in rp]
    d.line(P, fill=(246, 236, 205, 235), width=int(8 * S), joint='curve')
    d.line(P, fill=(168, 52, 36, 255), width=int(3.4 * S), joint='curve')
    ov = ov.resize((W, H), Image.LANCZOS)
    im = im.convert('RGBA'); im.alpha_composite(ov); im = im.convert('RGB')
    # landmark pictures: the card art fades into parchment, so "multiply" lets it sit on the ground without a frame
    for name, lat, lon, path, size_km in art:
        x = (lon - west) / (east - west) * W; y = (north - lat) / (north - south) * H
        a = Image.open(path).convert('RGB'); sz = int(size_km / wkm * W)
        a = a.resize((sz, int(sz * a.height / a.width)), Image.LANCZOS)
        # feather the edge so the picture melts into the ground
        m = Image.new('L', a.size, 0); ImageDraw.Draw(m).ellipse([a.width * .06, a.height * .06, a.width * .94, a.height * .94], fill=255)
        m = m.filter(ImageFilter.GaussianBlur(a.width * .07))
        region = im.crop((int(x - a.width / 2), int(y - a.height / 2), int(x - a.width / 2) + a.width, int(y - a.height / 2) + a.height))
        mult = ImageChops.multiply(region, a)
        im.paste(Image.composite(mult, region, m), (int(x - a.width / 2), int(y - a.height / 2)))
    d2 = ImageDraw.Draw(im, 'RGBA')
    for name, lat, lon, path, size_km in art:
        x = (lon - west) / (east - west) * W; y = (north - lat) / (north - south) * H
        d2.ellipse([x - 7, y - 7, x + 7, y + 7], fill=(247, 235, 203, 255), outline=(168, 52, 36, 255), width=3)
        sz = int(size_km / wkm * W); ty = y + sz * 0.38 + 6
        tw = d2.textlength(name, font=FONT)
        d2.rounded_rectangle([x - tw / 2 - 7, ty - 3, x + tw / 2 + 7, ty + 30], radius=6, fill=(247, 235, 203, 235), outline=(185, 143, 82, 255))
        d2.text((x - tw / 2, ty), name, font=FONT, fill=(59, 42, 26, 255))
    im.save(out); print('saved', out)

if __name__ == '__main__':
    G = ASSETS / 'generated'
    SC = '/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/'
    which = sys.argv[1]
    if which == 'north60':
        render((25.0, 121.7), 60, SC + 'trial_north60.png', art=[('台北101', 25.0336, 121.5644, G / 'card_taipei101.webp', 11), ('九份', 25.107, 121.843, G / 'card_jiufen.webp', 11), ('基隆廟口', 25.1286, 121.7432, G / 'card_keelung.jpg', 11), ('福隆', 25.0208, 121.9453, G / 'card_fulong.jpg', 11)])
    if which == 'taipei15':
        render((25.045, 121.535), 15, SC + 'trial_taipei15.png', art=[('台北101', 25.0336, 121.5644, G / 'card_taipei101.webp', 7)], rivers_min='all')
    if which == 'east40':
        render((23.1, 121.2), 40, SC + 'trial_east40.png', art=[('池上', 23.1243, 121.2210, G / 'card_chishang.webp', 8), ('關山', 23.0457, 121.1644, None, 0)] if False else [('池上', 23.1243, 121.2210, G / 'card_chishang.webp', 8), ('鹿野高台', 22.9134, 121.1196, G / 'card_luye.webp', 8)])
