#!/usr/bin/env python3
"""Plans the painted map tiles along the real route and draws a guide picture for each.

Each tile is a square piece of Taiwan (36 km a side).  The guide picture is a plain data map (coast, hills, rivers, roads,
land cover) with numbered red circles where landmarks must be painted; Gemini repaints it in the hand-drawn atlas style.
Run from android/assets:  python3 tools/make_tiles.py            (plan + prompts only)
                          python3 tools/make_tiles.py guides      (also draws every guide picture; slow)
"""
import math, re, sys, json, pathlib
import numpy as np
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import mock_map as M
from shapely import wkb
import pyarrow.compute as pc
from scipy.ndimage import gaussian_filter

ROOT = pathlib.Path(__file__).resolve().parent.parent
CORE = ROOT.parent / 'core/src/main/kotlin/com/starmist/core/route/TaiwanLoop.kt'
OUT = ROOT / 'mapguide/tiles'
TILE_KM = 40.0
COVER_KM = 25.0   # route points must fall inside this square, so the 15 km wide view (7.5 km each way) never leaves the tile

# (id, name, lat, lon, kind, what to paint) ---------------------------------------------------------------------------
SIGHTS = {
 'taipei101': 'the tall segmented tower with stacked pagoda-like sections, small trees at its base',
 'keelung': 'a temple (Dianji Temple) gate with rows of small lantern-lit food stalls in front, a harbour behind',
 'jiufen': 'a steep hillside of old teahouses with red lanterns and stone steps down to the sea',
 'fulong': 'a wide sandy beach where a river meets the ocean, a long sandbar, small umbrellas',
 'sandiaojiao': 'a white lighthouse standing on a green headland above the sea',
 'toucheng': 'a row of old brick shophouses with arcaded fronts and baroque-style facades',
 'ncta': 'a traditional Chinese courtyard complex with curved roofs beside a canal, a small bridge',
 'suao': 'a round stone spring pool with bubbles rising and a few people soaking, cool blue water',
 'qixingtan': 'a crescent-shaped bay with a pebble beach, a few kites in the air',
 'pinegarden': 'a two-storey Japanese-era wooden villa with a tiled roof among old pine trees',
 'lintienshan': 'a wooden forestry village on a hillside with a small logging railway and stacked logs',
 'ruisui': 'a small white stone monument with a ring-shaped top on a hill (Tropic of Cancer marker)',
 'yuli': 'a steaming bowl of noodles in front of a small railway station building',
 'chishang': 'a green rice field with a small railway station and a lunch box (bento) in the foreground',
 'guanshan': 'a bicycle on a path circling a rice field, mountains behind',
 'luye': 'a hot-air balloon rising over green tea terraces, a lookout deck',
 'prehistory': 'a modern museum building with a curved glass roof and a stone-age mural wall',
 'taimali': 'a coast with the first golden light of sunrise over the Pacific, a small seaside shrine',
 'shouka': 'a mountain pass road winding up between steep forested slopes, a small roadside shrine',
 'checheng': 'a Taiwanese Earth God temple with a curved swallowtail roof and a very large golden statue of the Earth God beside it',
 'hengchun': 'a stone city gate with a tower and thick curved walls, a quiet street',
 'eluanbi': 'a white lighthouse on a grassy coral-limestone cape, the sea beyond',
 'maobitou': 'a rock on the shore shaped like a crouching cat, surrounded by waves',
 'nmmba': 'a modern aquarium building with a large whale sculpture and waves',
 'fangliao': 'a small railway station with a steam train passing, the sea in the background',
 'donggang': 'a temple with a large decorated wooden ceremonial ship (Wangchuan) on a harbour shore',
 'pier2': 'old brick warehouses turned into an art area with colourful art installations, a harbour crane',
 'lotuspond': 'a lake with the Dragon and Tiger Pagodas on the shore, a zigzag bridge',
 'qiaotou': 'an old sugar mill with a tall chimney, a small sugarcane train and cane fields',
 'anping': 'a red-brick fort with a watchtower and thick walls, old banyan trees',
 'jingzijiao': 'tile-floored salt pans in a grid, small white salt pyramids, the sea edge',
 'budai': 'a fishing harbour with a ferry and fishing boats, a small lighthouse',
 'peitian': 'a large Mazu temple with an ornate multi-layered roof, red lanterns',
 'shuishang': 'a small white monument with a ring symbol on flat farmland (Tropic of Cancer marker)',
 'hinoki': 'Japanese-era wooden houses with tiled roofs among tall cypress trees',
 'xiluo': 'a very long steel truss bridge crossing a wide braided river',
 'lukang': 'a very narrow alley between tall red-brick walls with a tiny strip of sky',
 'roundhouse': 'a semicircular steam-locomotive depot with a locomotive on a turntable',
 'gaomei': 'a row of white wind turbines on a tidal flat at sunset, shallow pools reflecting the sky',
 'dajia': 'a large Mazu temple with a procession of small figures carrying a palanquin',
 'tongxiao': 'a salt factory building with big white salt piles and a small train',
 'hsinchu': 'a large city-god temple with a colourful carved roof and a courtyard',
 'taoyuan': 'a Japanese shrine with a torii gate, stone lanterns and a stair path through trees',
 'shihsanhang': 'a modern dark-roofed museum with a long ribbon-shaped structure beside the Tamsui river mouth',
 'dihua': 'an old merchant street with arched baroque facades, hanging dried goods and lanterns',
 'finish': 'a cosy small house with a lantern light and a front garden (the end of the journey)',
}
POIS = {
 'heping': 'a rocky coast with strange wave-eroded stone shapes and a small park',
 'lanyang': 'a modern building shaped like a cuesta rock slope, beside the sea',
 'yilan_distillery': 'an old factory building with a tall chimney and large wooden barrels',
 'luodong': 'a wooden log pond with stacked logs and a small steam train',
 'nanfangao': 'a bridge over a colourful fishing harbour full of boats',
 'luoshan': 'a high narrow waterfall in a green valley with a small bridge',
 'beinan': 'stone slab ruins in a grass park under a big sky',
 'zhiben': 'a steaming outdoor hot spring pool among trees',
 'cijin': 'a white lighthouse on a sandy spit at the harbour mouth',
 'loveriver': 'a calm river with small boats and willow trees, bridges',
 'pingtung_hall': 'a modern curved building with a green roof',
 'gangshan': 'an old brick water tower among low houses',
 'chihkan': 'a red-brick tower with a curved roof among old trees',
 'chimei': 'a neoclassical white museum with columns and a large fountain',
 'chiayi_park': 'a park with a tall shrine-like pavilion and old trees',
 'baguashan': 'a very large seated Buddha statue on a hill above a city',
 'wuqi': 'a harbour with many moored fishing boats and a small market',
 'taichung_theater': 'a futuristic white curved building with cave-like openings',
 'nanliao': 'a harbour with a long seaside cycle path and small boats',
 'zhongli_hall': 'a small modern brick arts hall with a courtyard',
 'zhuwei': 'a small harbour with an old wharf and fishing nets',
 'lin_garden': 'a classical Chinese garden with a pavilion, a pond and a curved bridge',
 'hongmaocheng': 'a red fort with a flag on a hill by the river mouth',
 'red_house': 'an octagonal red-brick Japanese-era building with a small plaza',
}
# local colour that is not a sight: (name, lat, lon, what to paint)  -- illustrative positions
LOCAL = [
 ('Pinglin tea hills', 24.935, 121.715, 'terraced hillsides of tea plantations with neat curved rows and a small tea factory hut'),
 ('Beitou hot springs', 25.137, 121.505, 'steam rising from a valley of hot springs with small bathhouses'),
 ('Tamsui sunset ferry', 25.168, 121.435, 'a small passenger ferry crossing a wide river mouth at sunset'),
 ('Datun-side duck pond area (Taoyuan ponds)', 25.05, 121.20, 'many small round irrigation ponds scattered in farmland'),
 ('Lanyang plain paddies', 24.70, 121.78, 'a patchwork of green rice paddies with a water buffalo and an egret'),
 ('Ruisui ranch', 23.50, 121.37, 'a green hillside pasture with a group of cows'),
 ('Chishang paddies', 23.15, 121.23, 'wide golden rice fields with a long straight farm road and a lone tree'),
 ('Linbian wax-apple farms', 22.45, 120.52, 'orchards of wax-apple trees with netting and a roadside fruit stand'),
 ('Kenting beach dunes', 21.96, 120.80, 'a white sand beach with dunes, windswept grass and a few palms'),
 ('Qigu lagoon oyster racks', 23.15, 120.08, 'oyster-farm racks in a shallow lagoon with a few small boats'),
 ('Dongshi oyster racks', 23.45, 120.14, 'oyster-farm racks on a tidal flat with fishermen'),
 ('Mailiao wind turbines', 23.75, 120.20, 'a row of white wind turbines along a coastal dike'),
 ('Wanggong oyster fields', 23.97, 120.35, 'tidal flats with oyster racks and a small fishing hamlet'),
 ('Tianwei flower garden', 23.89, 120.52, 'rows of colourful flower fields and greenhouses'),
 ('Yuanli grass-weaving fields', 24.44, 120.65, 'low rush-grass fields and a small weaving house'),
 ('Miaoli coastal wind turbines', 24.62, 120.76, 'a row of wind turbines on a coastal plain'),
 ('Hsinchu windy coast', 24.83, 120.92, 'a windswept coast with wind-bent pines and a cycle path'),
]
NAMES = {}  # filled from TaiwanLoop.kt

def parse_route():
    src = CORE.read_text().split('val route')[-1]
    cards, pois, line = {}, {}, []
    for m in re.finditer(r'(card|supply|stop|via)\(((?:"[^"]*",\s*){0,3}?)(\d+\.\d+),\s*(\d+\.\d+)', src):
        kind, strs, lat, lon = m.group(1), re.findall(r'"([^"]*)"', m.group(2)), float(m.group(3)), float(m.group(4))
        line.append((lat, lon))
        if kind == 'card': cards[strs[0]] = (strs[1], lat, lon)
    for m in re.finditer(r'poi\("([a-z_0-9]+)", "([^"]+)", ([\d.]+), ([\d.]+)', CORE.read_text()):
        pois[m.group(1)] = (m.group(2), float(m.group(3)), float(m.group(4)))
    return line, cards, pois

def to_km(lat, lon, lat0=24.0):
    return ((lon - 120.0) * 111.32 * math.cos(math.radians(lat0)), (lat - 21.5) * 110.57)

def plan(line):
    pts = [to_km(*p) for p in line]
    # densify so a long straight leg cannot jump over a tile
    dense = []
    for a, b in zip(pts, pts[1:]):
        n = max(1, int(math.hypot(b[0]-a[0], b[1]-a[1]) / 2))
        dense += [(a[0] + (b[0]-a[0]) * k / n, a[1] + (b[1]-a[1]) * k / n) for k in range(n)]
    dense.append(pts[-1])
    tiles = []; i = 0
    while i < len(dense):
        xs, ys = [dense[i][0]], [dense[i][1]]
        j = i
        while j + 1 < len(dense):
            nx, ny = dense[j+1]
            if max(max(xs + [nx]) - min(xs + [nx]), max(ys + [ny]) - min(ys + [ny])) > COVER_KM: break
            xs.append(nx); ys.append(ny); j += 1
        cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
        tiles.append((cx, cy)); i = j + 1
    out = []
    for cx, cy in tiles:
        lat = 21.5 + cy / 110.57
        lon = 120.0 + cx / (111.32 * math.cos(math.radians(24.0)))
        out.append((round(lat, 4), round(lon, 4)))
    return out

def inside(tile, lat, lon):
    la, lo = tile
    kx = 111.32 * math.cos(math.radians(la)); ky = 110.57
    dx, dy = (lon - lo) * kx, (lat - la) * ky
    return abs(dx) <= TILE_KM / 2 - 1.0 and abs(dy) <= TILE_KM / 2 - 1.0, 50 + 100 * (dx / TILE_KM + .5) , 100 * (0.5 - dy / TILE_KM)

def items_for(tile, cards, pois):
    items = []
    for cid, (name, lat, lon) in cards.items():
        ok, x, y = inside(tile, lat, lon)
        if ok and cid in SIGHTS: items.append((name, lat, lon, SIGHTS[cid], 'landmark'))
    for pid, (name, lat, lon) in pois.items():
        ok, x, y = inside(tile, lat, lon)
        if ok and pid in POIS: items.append((name, lat, lon, POIS[pid], 'minor'))
    for name, lat, lon, what in LOCAL:
        ok, x, y = inside(tile, lat, lon)
        if ok: items.append((name, lat, lon, what, 'local'))
    return items

def pct(tile, lat, lon):
    la, lo = tile
    kx = 111.32 * math.cos(math.radians(la)); ky = 110.57
    dx, dy = (lon - lo) * kx, (lat - la) * ky
    return round(100 * (0.5 + dx / TILE_KM)), round(100 * (0.5 - dy / TILE_KM))

def draw_guide(tile, items, out, W=2048):
    lat0, lon0 = tile
    kx = 111.32 * math.cos(math.radians(lat0)); ky = 110.57
    dlon, dlat = TILE_KM / kx, TILE_KM / ky
    west, east, south, north = lon0 - dlon/2, lon0 + dlon/2, lat0 - dlat/2, lat0 + dlat/2
    bbox = (west, south, east, north)
    h = w = W
    def px(lon, lat): return ((lon - west) / (east - west) * w, (north - lat) / (north - south) * h)
    ys, xs = np.mgrid[0:h:2, 0:w:2]  # half-res elevation, enlarged later
    lons = west + (xs + .5) / w * (east - west); lats = north - (ys + .5) / h * (north - south)
    el = M.elevation(lats, lons)
    el = np.asarray(Image.fromarray(gaussian_filter(el, 1.0).astype(np.float32)).resize((w, h), Image.BILINEAR))
    full = np.load(M.SCR + 'full.npy')
    land_img = Image.new('L', (w, h), 0); ImageDraw.Draw(land_img).polygon([px(x, y) for x, y in full], fill=255)
    land = np.asarray(land_img) > 0
    mpp = TILE_KM * 1000 / w
    gy, gx = np.gradient(gaussian_filter(el, 2), mpp)
    slope = np.arctan(2.0 * np.hypot(gx, gy)); asp = np.arctan2(-gx, gy)
    shade = np.clip(np.sin(math.radians(42)) * np.cos(slope) + np.cos(math.radians(42)) * np.sin(slope) * np.cos(math.radians(315) - asp), 0, 1)
    base = np.array([226, 214, 178], np.float32)
    col = np.zeros((h, w, 3), np.float32)
    col[:] = base
    e = np.clip(el, 0, 3200)
    hi = np.clip((e - 200) / 1500, 0, 1)[..., None]
    col = col * (1 - hi * .45) + np.array([120, 130, 96], np.float32) * hi * .45
    col *= (0.55 + 0.7 * shade)[..., None]
    sea = np.array([190, 214, 226], np.float32)
    im = Image.fromarray(np.clip(np.where(land[..., None], col, sea), 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(im, 'RGBA')
    d.line([px(x, y) for x, y in full] + [px(*full[0])], fill=(70, 60, 50, 255), width=3)
    for r in M.fetch('base', 'land_cover', bbox, ['subtype', 'geometry']):
        c = {'forest': (70, 120, 60, 80), 'crop': (235, 210, 90, 120), 'urban': (205, 120, 120, 130), 'wetland': (90, 160, 170, 130), 'barren': (210, 200, 180, 90)}.get(r['subtype'])
        if not c: continue
        for p in M.polys(wkb.loads(r['geometry'])): d.polygon([px(x, y) for x, y in p.exterior.coords], fill=c)
    for r in M.fetch('base', 'water', bbox, ['class', 'geometry']):
        g = wkb.loads(r['geometry']); cl = r['class']
        if cl in ('drain', 'ditch', 'swimming_pool', 'basin', 'canal', 'pond'): continue
        if g.geom_type in ('LineString', 'MultiLineString'):
            wd = {'river': 5, 'stream': 2}.get(cl, 2)
            for pts in M.geom_pts(g): d.line([px(x, y) for x, y in pts], fill=(40, 110, 200, 255), width=wd)
        else:
            for p in M.polys(g):
                if p.area * kx * ky * 1e6 < 20000: continue
                d.polygon([px(x, y) for x, y in p.exterior.coords], fill=(60, 130, 210, 255))
    roads = M.fetch('transportation', 'segment', bbox, ['class', 'geometry'], pc.field('subtype') == 'road')
    for r in roads:
        cl = r['class']
        if cl not in ('primary', 'trunk', 'secondary'): continue
        g = wkb.loads(r['geometry'])
        if g.geom_type == 'LineString': d.line([px(x, y) for x, y in g.coords], fill=(120, 70, 30, 255), width=4 if cl != 'secondary' else 3)
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 34)
    for n, (name, lat, lon, what, kind) in enumerate(items, 1):
        x, y = px(lon, lat)
        R = 30 if kind == 'landmark' else 24
        d.ellipse([x - R, y - R, x + R, y + R], fill=(205, 40, 40, 255), outline=(255, 255, 255, 255), width=3)
        d.text((x, y), str(n), fill='white', font=font, anchor='mm')
    im.convert('RGB').save(out, quality=92)

def main():
    guides = len(sys.argv) > 1 and sys.argv[1] == 'guides'
    only = set(int(x) for x in sys.argv[2:]) if guides and len(sys.argv) > 2 else None
    line, cards, pois = parse_route()
    tiles = plan(line)
    OUT.mkdir(parents=True, exist_ok=True)
    meta = []
    prompt_lines = []
    for k, tile in enumerate(tiles, 1):
        items = items_for(tile, cards, pois)
        meta.append({'id': k, 'lat': tile[0], 'lon': tile[1], 'items': [i[0] for i in items]})
        if guides and (only is None or k in only) and not (OUT / f'tile_{k:02d}_guide.jpg').exists():
            draw_guide(tile, items, OUT / f'tile_{k:02d}_guide.jpg')
            print('guide', k, len(items), flush=True)
        lst = '\n'.join(f'{n}. ({kind}) {name} — {what} — about {pct(tile, lat, lon)[0]}% from the left, {pct(tile, lat, lon)[1]}% from the top.' for n, (name, lat, lon, what, kind) in enumerate(items, 1)) or '(no landmarks in this tile: paint only the landscape)'
        prompt_lines.append((k, tile, items, lst))
    (OUT / 'tiles.json').write_text(json.dumps(meta, ensure_ascii=False, indent=1))
    # prompts document
    md = ['# 真實世界地圖：分區重畫提示詞（自動產生，請勿手改）', '',
          f'整條路線分成 {len(tiles)} 塊，每塊約 {TILE_KM:.0f} 公里見方，從台北出發順時針編號。',
          '',
          '## 每一塊怎麼做',
          '1. 新開對話，上傳**三張**圖：風格參考 `previews/2_compass.jpg`、風格參考 `generated/real_map_base.png`、這一塊的**草圖**。',
          '2. 第 2 塊起，再多上傳**上一塊你滿意的成品**，並在提示詞最後加一句：',
          '   「The second uploaded painted picture is the neighbouring tile: continue the same landscape style, palette and brush work so the two join seamlessly; do not copy its content.」',
          '3. 貼該塊的提示詞。比例選 **1:1 正方形**，解析度選最高（越高越好，至少 2048 像素）。',
          '4. 存成 `generated/tile_XX.png` 傳給我（可一次傳幾塊）。我會用海岸線疊圖檢查有沒有對上，歪掉的會請你重畫。',
          '5. 草圖上的紅圈數字只是定位用，不能畫進成品；成品要鋪滿整張正方形，沒有邊框、沒有指北針、沒有文字。',
          '']
    for k, tile, items, lst in prompt_lines:
        md += [f'## 第 {k:02d} 塊（中心 {tile[0]:.3f}°N, {tile[1]:.3f}°E）', f'草圖：`mapguide/tiles/tile_{k:02d}_guide.jpg`　存檔：`generated/tile_{k:02d}.png`', '', '```', PROMPT.format(items=lst), '```', '']
    (ROOT / 'MAP_TILES_PROMPTS.md').write_text('\n'.join(md))
    print(len(tiles), 'tiles')

PROMPT = """Repaint the uploaded guide picture as a richly detailed antique hand-drawn pictorial map, in exactly the style of the two style reference images (fine dark-brown pen and ink, light watercolour wash, aged parchment, gentle grain). The guide is a flat data map: pale blue = sea, thick dark line = coastline, deep blue lines and patches = rivers and lakes, brown lines = roads, shaded relief = hills and mountains, light green patches = forest, yellow patches = farmland, pink patches = towns, red numbered circles = landmarks to paint (the circles and numbers are NOT part of the picture).
Keep the coastline, rivers, roads and the position of every hill EXACTLY where they are in the guide, same framing, square, full bleed (the picture fills the whole square, no border, no frame, no margin, no compass, no title). Seen from a slightly raised top-down view.
Paint the terrain: pen-hatched hills and mountains following the shaded relief (snow patches only on the very highest peaks), tiny trees in the forest patches, a patchwork of paddies and fields in the yellow farmland, small clusters of old roofs only in the pink town patches (few, not dense), thin pen roads, sea with fine horizontal hatching and a few wave marks near the coast.
Paint each numbered landmark as a small illustrated vignette standing on the ground at its circle, about 6% of the picture width, recognisable but integrated into the landscape (no text on signs, no labels):
{items}
No text, no letters, no numbers anywhere in the picture."""

if __name__ == '__main__':
    main()
