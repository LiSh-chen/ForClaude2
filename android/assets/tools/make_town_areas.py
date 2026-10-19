#!/usr/bin/env python3
"""Township (鄉鎮市區) areas: villages grouped by the township in their Overture hierarchy, gaps filled from the nearest township on a ~100 m grid,
outlines simplified. Writes app/src/main/res/raw/town_areas.bin (gzip): n, then per township: name, label point (inside the area), rings
(int32 lat/lon * 1e5 for the first vertex, int16 deltas after).  Run from android/assets (needs the division pickle from make_town_borders)."""
import sys, re, gzip, struct, pickle, pathlib, collections
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import mock_map as M
import numpy as np, rasterio.features, rasterio.transform, shapely
from shapely import wkb
from shapely.geometry import Polygon, shape
from shapely.ops import unary_union
from scipy.ndimage import distance_transform_edt
ROOT = pathlib.Path(__file__).resolve().parent.parent
bb = (119.93, 21.8, 122.12, 25.4)
rows = M.fetch('divisions', 'division_area', bb, ['subtype', 'names', 'country', 'geometry', 'division_id'])
div = pickle.load(open('/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/div.pkl', 'rb'))
hier = {d['id']: d['hierarchies'] for d in div if d['subtype'] == 'locality'}
groups = collections.defaultdict(list); names = {}; counties = {}
for r in rows:
    if r['subtype'] != 'locality' or r['country'] != 'TW': continue
    h = hier.get(r['division_id'])
    if not h: continue
    town = next((x for x in h[0][1:] if x['subtype'] == 'locality' and x['name'][-1:] in '鄉鎮市區' and x['name'] != r['names']['primary']), None)
    if town is None and r['names']['primary'][-1:] in '鄉鎮市區': town = dict(division_id=r['division_id'], name=r['names']['primary'])
    if town is None: continue
    groups[town['division_id']].append(wkb.loads(r['geometry'])); names[town['division_id']] = town['name'].split(' ')[0]
    counties[town['division_id']] = next((x['name'] for x in h[0] if x['subtype'] == 'region'), '')
src = (ROOT.parent / 'core/src/main/kotlin/com/starmist/core/route/TaiwanOutline.kt').read_text()
outline = Polygon([(float(b_), float(a_)) for a_, b_ in re.findall(r'GeoPoint\(([\d.]+), ([\d.]+)\)', src)])
RES = 0.001
W, H = round((bb[2] - bb[0]) / RES), round((bb[3] - bb[1]) / RES)
tf = rasterio.transform.from_origin(bb[0], bb[3], RES, RES)
keys = list(groups); lab = np.zeros((H, W), np.int32)
for i, k in enumerate(keys, 1):
    lab = np.maximum(lab, rasterio.features.rasterize([(g.buffer(0), i) for g in groups[k]], out_shape=(H, W), transform=tf, dtype='int32'))
land = rasterio.features.rasterize([(outline.buffer(0.002), 1)], out_shape=(H, W), transform=tf, dtype='uint8') > 0
dist, (iy, ix) = distance_transform_edt(lab == 0, return_indices=True)
filled = np.where(land & (dist < 25), lab[iy, ix], 0)
polys = collections.defaultdict(list)
for g, v in rasterio.features.shapes(filled, mask=filled > 0, transform=tf): polys[int(v)].append(shape(g))
import math
from shapely.ops import transform as _tf
dem_cache = {}
def dem_stats(poly):
    minx, miny, maxx, maxy = poly.bounds
    xs = np.arange(minx, maxx, 0.003); ys = np.arange(miny, maxy, 0.003)
    pts = [(x, y) for x in xs for y in ys if poly.contains(shapely.geometry.Point(x, y))]
    if not pts: pts = [(poly.representative_point().x, poly.representative_point().y)]
    e = M.elevation(np.array([p[1] for p in pts], dtype=np.float64), np.array([p[0] for p in pts], dtype=np.float64))
    return int(np.median(e)), int(np.percentile(e, 98))
facts = {}
out = bytearray(struct.pack('>H', len(polys)))
total = 0
for v, ps in polys.items():
    u = unary_union(ps).simplify(0.0009)
    parts = [p for p in getattr(u, 'geoms', [u]) if p.geom_type == 'Polygon' and p.area > 2e-5]
    if not parts: continue
    big = max(parts, key=lambda p: p.area)
    from shapely.ops import polylabel
    lp = polylabel(big, tolerance=0.0005)      # the point farthest from every edge, so the name sits in the middle of the area
    nm = names[keys[v - 1]].encode('utf8'); cy = counties[keys[v - 1]].encode('utf8')
    union = unary_union(parts); area = union.area * 111.0 * 101.5
    med, hi = dem_stats(union)
    out += struct.pack('>B', len(nm)) + nm + struct.pack('>B', len(cy)) + cy + struct.pack('>HhhH', min(65535, round(area * 10)), med, hi, len(groups[keys[v - 1]])) + struct.pack('>ii', round(lp.y * 1e5), round(lp.x * 1e5)) + struct.pack('>H', len(parts))
    facts[(counties[keys[v - 1]], names[keys[v - 1]])] = dict(area=area, med=med, hi=hi, villages=len(groups[keys[v - 1]]), poly=union)
    for p in parts:
        c = [(round(y * 1e5), round(x * 1e5)) for x, y in p.exterior.coords]; total += len(c)
        out += struct.pack('>H', len(c)) + struct.pack('>ii', *c[0])
        for a, b in zip(c, c[1:]): out += struct.pack('>hh', max(-32768, min(32767, b[0] - a[0])), max(-32768, min(32767, b[1] - a[1])))
raw = ROOT.parent / 'app/src/main/res/raw'; raw.mkdir(parents=True, exist_ok=True)
with gzip.open(raw / 'town_areas.bin', 'wb') as f: f.write(out)
print('townships', len(polys), 'vertices', total, 'bytes', (raw / 'town_areas.bin').stat().st_size)

# which townships the default route passes, in order (for writing the introductions)
import make_tiles as T, shapely.geometry as sg
line, _, _ = T.parse_route()
seen = []
for (la0, lo0), (la1, lo1) in zip(line, line[1:]):
    n = max(1, int(math.hypot((la1 - la0) * 111, (lo1 - lo0) * 101) / 0.5))
    for k in range(n + 1):
        pt = sg.Point(lo0 + (lo1 - lo0) * k / n, la0 + (la1 - la0) * k / n)
        for key, f in facts.items():
            if f['poly'].contains(pt):
                if key not in seen: seen.append(key)
                break
open('/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/route_towns.txt', 'w').write('\n'.join(f'{c}|{n}|{facts[(c, n)]["area"]:.0f}|{facts[(c, n)]["med"]}|{facts[(c, n)]["hi"]}|{facts[(c, n)]["villages"]}' for c, n in seen))
print('route passes', len(seen), 'townships')
