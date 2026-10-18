#!/usr/bin/env python3
"""Township (鄉鎮市區) borders from Overture division areas of the villages: villages are grouped by the township in their hierarchy,
dissolved, and the shared outlines written as polylines to app/src/main/res/raw/town_borders.bin (gzip; int32 lat/lon * 1e5, delta coded)."""
import sys, re, gzip, struct, pickle, pathlib, collections
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import mock_map as M
import shapely, shapely.geometry
from shapely import wkb
from shapely.geometry import Polygon, MultiLineString, LineString
from shapely.ops import unary_union, linemerge
ROOT = pathlib.Path(__file__).resolve().parent.parent
bb = (119.93, 21.8, 122.12, 25.4)
rows = M.fetch('divisions', 'division_area', bb, ['subtype', 'names', 'country', 'geometry', 'division_id'])
div = pickle.load(open('/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/div.pkl', 'rb'))
hier = {d['id']: d['hierarchies'] for d in div if d['subtype'] == 'locality'}
groups = collections.defaultdict(list)
for r in rows:
    if r['subtype'] != 'locality' or r['country'] != 'TW': continue
    h = hier.get(r['division_id'])
    if not h: continue
    chain = h[0]
    town = next((x for x in chain[1:] if x['subtype'] == 'locality' and x['name'][-1:] in '鄉鎮市區' and x['name'] != r['names']['primary']), None)
    if town is None and r['names']['primary'][-1:] in '鄉鎮市區': town = dict(division_id=r['division_id'], name=r['names']['primary'])
    if town is None: continue
    groups[town['division_id']].append(wkb.loads(r['geometry']))
print('townships with area', len(groups))
import numpy as np, rasterio.features, rasterio.transform
from scipy.ndimage import distance_transform_edt
src = (ROOT.parent / 'core/src/main/kotlin/com/starmist/core/route/TaiwanOutline.kt').read_text()
outline = Polygon([(float(b_), float(a_)) for a_, b_ in re.findall(r'GeoPoint\(([\d.]+), ([\d.]+)\)', src)])
RES = 0.001                                          # about 100 m per pixel
W, H = round((bb[2] - bb[0]) / RES), round((bb[3] - bb[1]) / RES)
tf = rasterio.transform.from_origin(bb[0], bb[3], RES, RES)
ids = {k: i + 1 for i, k in enumerate(groups)}
lab = np.zeros((H, W), np.int32)
for k, gs in groups.items():
    lab = np.maximum(lab, rasterio.features.rasterize([(g.buffer(0), ids[k]) for g in gs], out_shape=(H, W), transform=tf, dtype='int32'))
land = rasterio.features.rasterize([(outline.buffer(0.002), 1)], out_shape=(H, W), transform=tf, dtype='uint8') > 0
# fill gaps between villages (and small unlisted bits of land) with the nearest township
_, (iy, ix) = distance_transform_edt(lab == 0, return_indices=True)
filled = np.where(land, lab[iy, ix], 0)
filled[(lab == 0) & land & (distance_transform_edt(lab == 0) > 25)] = 0          # keep far-off land (islets) out
polys = [shapely.geometry.shape(g) for g, v in rasterio.features.shapes(filled, mask=filled > 0, transform=tf)]
lines = unary_union([p.boundary for p in polys])
coast = outline.boundary.buffer(0.004)
inner = lines.difference(coast)
merged = linemerge(inner) if inner.geom_type == 'MultiLineString' else inner
segs = [s_.simplify(0.0012) for s_ in getattr(merged, 'geoms', [merged]) if s_.length > 0.02]
print('polylines', len(segs), 'vertices', sum(len(s_.coords) for s_ in segs))
out = bytearray(struct.pack('>I', len(segs)))
for s in segs:
    c = [(round(y * 1e5), round(x * 1e5)) for x, y in s.coords]; out += struct.pack('>H', len(c)); out += struct.pack('>ii', *c[0])
    for a, b in zip(c, c[1:]): out += struct.pack('>hh', max(-32768, min(32767, b[0] - a[0])), max(-32768, min(32767, b[1] - a[1])))
raw = ROOT.parent / 'app/src/main/res/raw'; raw.mkdir(parents=True, exist_ok=True)
with gzip.open(raw / 'town_borders.bin', 'wb') as f: f.write(out)
print('size', (raw / 'town_borders.bin').stat().st_size, 'bytes')
