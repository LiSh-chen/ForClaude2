#!/usr/bin/env python3
"""One whole-island guide picture for Gemini: coast, offshore islands, relief and land cover, nothing else.

The picture is 9:16 and covers lon 119.93-122.12, lat 21.80-25.40, so the same numbers turn any pixel back into a place.
python3 tools/make_island_guide.py [width]    (default 2304 px wide, 4096 tall)
"""
import math, sys, pathlib
import numpy as np
from PIL import Image, ImageDraw
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import mock_map as M
from shapely import wkb
from scipy.ndimage import gaussian_filter

LON0, LON1, LAT0, LAT1 = 119.93, 122.12, 21.80, 25.40
ROOT = pathlib.Path(__file__).resolve().parent.parent

def land_polygons():
    from mpl_toolkits.basemap import Basemap
    m = Basemap(llcrnrlon=LON0 - .2, llcrnrlat=LAT0 - .2, urcrnrlon=LON1 + .2, urcrnrlat=LAT1 + .2, resolution='f')
    polys = []
    for seg in m.coastsegs:
        a = np.array(seg)
        if len(a) < 12: continue
        if a[:, 0].max() < LON0 or a[:, 0].min() > LON1 or a[:, 1].max() < LAT0 or a[:, 1].min() > LAT1: continue
        # drop lakes and other land outside our box (Penghu is west of the box, China is far)
        polys.append(a)
    return polys

def main():
    W = int(sys.argv[1]) if len(sys.argv) > 1 else 2304
    H = int(round(W * 16 / 9))
    def px(lon, lat): return ((lon - LON0) / (LON1 - LON0) * W, (LAT1 - lat) / (LAT1 - LAT0) * H)
    land_img = Image.new('L', (W, H), 0); ld = ImageDraw.Draw(land_img)
    polys = land_polygons()
    for a in polys: ld.polygon([px(x, y) for x, y in a], fill=255)
    land = np.asarray(land_img) > 0
    print('land polygons', len(polys), flush=True)
    ys, xs = np.mgrid[0:H:2, 0:W:2]
    lons = LON0 + (xs + .5) / W * (LON1 - LON0); lats = LAT1 - (ys + .5) / H * (LAT1 - LAT0)
    el = M.elevation(lats, lons)
    el = np.asarray(Image.fromarray(el.astype(np.float32)).resize((W, H), Image.BILINEAR))
    print('elevation done', flush=True)
    mpp = (LON1 - LON0) * 111.32 * math.cos(math.radians(23.6)) * 1000 / W
    gy, gx = np.gradient(gaussian_filter(el, 1.5), mpp)
    slope = np.arctan(3.0 * np.hypot(gx, gy)); asp = np.arctan2(-gx, gy)
    shade = np.clip(math.sin(math.radians(42)) * np.cos(slope) + math.cos(math.radians(42)) * np.sin(slope) * np.cos(math.radians(315) - asp), 0, 1)
    e = np.clip(el, 0, 3952)
    tint = [(0, (226, 214, 178)), (60, (214, 214, 160)), (250, (170, 190, 130)), (700, (130, 160, 100)), (1400, (120, 130, 92)), (2300, (150, 130, 100)), (3200, (200, 195, 185)), (3952, (245, 245, 245))]
    col = np.zeros((H, W, 3), np.float32)
    for c in range(3): col[..., c] = np.interp(e, [t[0] for t in tint], [t[1][c] for t in tint])
    col *= (0.50 + 0.78 * shade)[..., None]
    sea = np.array([190, 214, 226], np.float32)
    im = Image.fromarray(np.clip(np.where(land[..., None], col, sea), 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(im, 'RGBA')
    bbox = (LON0, LAT0, LON1, LAT1)
    n = 0
    for r in M.fetch('base', 'land_cover', bbox, ['subtype', 'geometry']):
        c = {'forest': (70, 120, 60, 70), 'crop': (235, 210, 90, 120), 'urban': (205, 120, 120, 130), 'wetland': (90, 160, 170, 130)}.get(r['subtype'])
        if not c: continue
        for p in M.polys(wkb.loads(r['geometry'])):
            d.polygon([px(x, y) for x, y in p.exterior.coords], fill=c); n += 1
    print('cover polygons', n, flush=True)
    for r in M.fetch('base', 'water', bbox, ['class', 'geometry']):
        g = wkb.loads(r['geometry'])
        if g.geom_type not in ('Polygon', 'MultiPolygon') or r['class'] not in ('lake', 'reservoir'): continue
        for p in M.polys(g):
            if p.area * 111 * 111 * 0.9 < 1.0: continue
            d.polygon([px(x, y) for x, y in p.exterior.coords], fill=(60, 130, 210, 255))
    seaimg = Image.new('RGB', (W, H), tuple(int(v) for v in sea))
    im = Image.composite(im, seaimg, land_img)
    dd = ImageDraw.Draw(im)
    for a in polys: dd.line([px(x, y) for x, y in a] + [px(*a[0])], fill=(70, 60, 50), width=3)
    out = ROOT / 'mapguide/island_guide.jpg'
    im.save(out, quality=93)
    im.resize((W // 4, H // 4), Image.LANCZOS).save(ROOT / 'mapguide/island_guide_small.jpg', quality=90)
    print('saved', out, im.size)

if __name__ == '__main__':
    main()
