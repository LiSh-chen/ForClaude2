#!/usr/bin/env python3
"""Draws the layout guide for the real-world (Taiwan) map and writes the Gemini prompt that goes with it.

The picture covers lon 119.95-122.10, lat 21.80-25.40 (the same box the app uses), so a fraction of the
width or height in the prompt is a real place. Run from android/assets:  python3 tools/make_real_map_guide.py
"""
import re
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

LON0, LON1, LAT0, LAT1 = 119.95, 122.10, 21.80, 25.40
W = 1200
H = round(W * (LAT1 - LAT0) / ((LON1 - LON0) * 0.9171))  # cos(23.5 deg)
root = Path(__file__).resolve().parent.parent
src = (root.parent / "core/src/main/kotlin/com/starmist/core/route/TaiwanOutline.kt").read_text()
outline = [(float(a), float(b)) for a, b in re.findall(r"GeoPoint\(([\d.]+), ([\d.]+)\)", src)]


def px(lat, lon):
    return ((lon - LON0) / (LON1 - LON0) * W, (LAT1 - lat) / (LAT1 - LAT0) * H)


def pct(lat, lon):
    x, y = px(lat, lon)
    return round(x / W * 100), round(y / H * 100)


# (number, what, lat, lon).  Kept to things that are real and easy to place.
OBJECTS = [
    ("A. Mountains", [
        (1, "Yushan: the highest peak, one tall snow-capped peak with a few lower summits around it", 23.47, 120.96),
        (2, "Xueshan: a large rugged peak with snow patches", 24.38, 121.23),
        (3, "Hehuanshan: a broad grassy high ridge with snow dusting", 24.14, 121.28),
        (4, "Alishan: rounded forested mountains of tall cedars, with a hint of a mountain railway line", 23.51, 120.80),
        (5, "Beidawu mountain: a lone pyramid-shaped peak in the far south", 22.62, 120.75),
        (6, "Central Mountain Range: a continuous spine of high ridges from the north-east tip, down the middle, to the southern tip (follows 1-2-3-5), ridges drawn as pen-hatched slopes with green wash on the lower flanks, snow only on the highest peaks", 24.0, 121.1),
        (7, "Coastal Range: a lower, greener, narrower ridge along the east side of the long valley, from about 23.9N to 22.8N", 23.4, 121.4),
    ]),
    ("B. Valleys, plains and lakes", [
        (8, "Huadong rift valley: a long narrow flat valley between the Central and Coastal Ranges, with strips of green and yellow farmland and a river running down its length", 23.4, 121.3),
        (9, "Chianan plain: a wide flat patchwork of rice fields, fish ponds and small villages", 23.3, 120.35),
        (10, "Lanyang plain: a small triangular plain of rice paddies opening to the sea", 24.7, 121.7),
        (11, "Taipei basin: a flat bowl among low hills, dense with tiny roofs", 25.03, 121.5),
        (12, "Pingtung plain: flat farmland with scattered orchards", 22.6, 120.55),
        (13, "Sun Moon Lake: a small lake ringed by hills", 23.86, 120.91),
        (14, "Taroko gorge: a deep marble gorge cut through the mountains to the east coast", 24.17, 121.5),
    ]),
    ("C. Rivers (thin blue pen lines, wiggling, widening toward the sea)", [
        (15, "Tamsui River with its branches, ending at the north-west tip of the Taipei basin", 25.12, 121.43),
        (16, "Dajia River from the mountains to the west coast", 24.27, 120.9),
        (17, "Zhuoshui River, the longest, from the central mountains to the west coast", 23.85, 120.7),
        (18, "Zengwen River into the south-west plain", 23.2, 120.4),
        (19, "Gaoping River into the south-west", 22.9, 120.65),
        (20, "Hualien and Xiuguluan Rivers along the Huadong valley, each reaching the east coast through a gap in the Coastal Range", 23.6, 121.4),
        (21, "Lanyang River into the Lanyang plain", 24.65, 121.72),
        (22, "Beinan River into the south-east coast", 22.85, 121.15),
    ]),
    ("D. Towns and a few landmarks (tiny old-style roofs, no text)", [
        (23, "Taipei: a dense cluster of small roofs with one tall slender tower (Taipei 101)", 25.04, 121.55),
        (24, "Keelung: a small harbour town with a breakwater", 25.13, 121.74),
        (25, "Hsinchu: a small walled town", 24.80, 120.97),
        (26, "Taichung: a medium town of roofs", 24.15, 120.68),
        (27, "Changhua: a small town with a hilltop big Buddha statue", 24.08, 120.54),
        (28, "Chiayi: a small town with a few roofs", 23.48, 120.45),
        (29, "Tainan: a town with a small fort by the coast (Anping)", 23.0, 120.2),
        (30, "Kaohsiung: a larger harbour town with cranes and ships", 22.63, 120.28),
        (31, "Yilan: a small town on the plain", 24.76, 121.75),
        (32, "Hualien: a small harbour town on the east coast", 23.98, 121.6),
        (33, "Taitung: a small town on the south-east coast", 22.76, 121.14),
        (34, "Eluanbi lighthouse on the southern tip, a white tower on a rocky cape", 21.90, 120.85),
        (35, "Salt pans near Jingzijiao: neat white rectangles on the south-west coast", 23.26, 120.11),
        (36, "Gaomei wetland with a row of white wind turbines", 24.31, 120.55),
        (37, "Sandiaojiao lighthouse on the north-east cape", 25.0, 122.0),
    ]),
    ("E. Offshore islands (draw as small islands in the sea, inside the picture)", [
        (38, "Guishan Island: a small turtle-shaped volcanic island", 24.84, 121.95),
        (39, "Green Island: a small round island with a lighthouse", 22.66, 121.49),
        (40, "Orchid Island: a medium island with hills and a few villages", 22.05, 121.55),
        (41, "Xiaoliuqiu: a small flat coral island", 22.34, 120.37),
    ]),
    ("F. Sea life and ships (a few, spread out; in the sea only)", [
        (42, "Two old sailing ships in the Taiwan Strait", 24.0, 120.1),
        (43, "A small fishing boat with nets near the north coast", 25.32, 121.4),
        (44, "A whale spouting, in the Pacific off the east coast", 24.0, 122.0),
        (45, "A pod of dolphins jumping", 23.0, 121.8),
        (46, "A small sailing boat off the south-east coast", 22.2, 121.3),
        (47, "A sea serpent, subtle and old-engraving style, in the far south-east sea", 21.95, 121.9),
    ]),
]


def write_guide():
    im = Image.new("RGB", (W, H), (214, 228, 228))
    d = ImageDraw.Draw(im)
    for i in range(1, 10):
        x, y = W * i / 10, H * i / 10
        d.line([(x, 0), (x, H)], fill=(190, 205, 205), width=1)
        d.line([(0, y), (W, y)], fill=(190, 205, 205), width=1)
    pts = [px(a, b) for a, b in outline]
    d.polygon(pts, fill=(236, 226, 192), outline=(60, 50, 40))
    d.line(pts + [pts[0]], fill=(60, 50, 40), width=3)
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 22)
    for _, items in OBJECTS:
        for n, _, lat, lon in items:
            x, y = px(lat, lon)
            d.ellipse([x - 16, y - 16, x + 16, y + 16], fill=(200, 60, 50), outline="white", width=2)
            d.text((x, y), str(n), fill="white", font=font, anchor="mm")
    (root / "mapguide").mkdir(exist_ok=True)
    im.save(root / "mapguide/real_map_guide.png")
    print("guide", im.size)


def write_prompt():
    rows = []
    for title, items in OBJECTS:
        rows.append(f"\n**{title}**\n")
        for n, what, lat, lon in items:
            x, y = pct(lat, lon)
            rows.append(f"{n}. {what} — about {x}% from the left, {y}% from the top.")
    body = "\n".join(rows)
    text = f"""# 真實世界（台灣）地圖：給 Gemini 的提示詞

用法：新開對話，貼「風格前綴」（同 `MAP_PROMPTS.md`），上傳 `previews/2_compass.jpg` 當風格參考，再上傳 `mapguide/real_map_guide.png` 當版面草圖，最後貼下面的提示詞。
比例選**直式**（草圖是 {W}×{H}，約 0.55，最接近 9:16；Gemini 給不出就選 9:16，再告訴我實際尺寸）。解析度選最高。
草圖上的紅色數字圓圈只是定位用，**不要畫進成品**；陸地輪廓（黑線）要照草圖畫，我的路線是按這個輪廓算的。

> Use the uploaded guide image ONLY for layout: draw Taiwan with exactly the same coastline, position and proportions as the beige land shape; do not copy its flat colours, grid lines, black outline or red numbered circles. Draw a richly detailed antique atlas map of the island in the style described above, portrait, seen from a slightly raised top-down view. The land fills the central part of the picture; the sea fills ALL four edges of the picture to the very edge (no border, no frame, no margin, no vignette, no corner ornaments, no compass rose), shown with fine horizontal pen hatching and small wave marks, thicker hatching near the coast.
>
> Fill the whole land with detail: green wash forests of tiny round and pointed trees, grass ticks, scattered tiny farmsteads, faint dirt roads linking the towns (the west plain road from north to south, the east valley road, and the coastal roads, all thin dashed ochre lines), and hand-drawn hills and mountains that blend smoothly into the plains with shaded slopes (no abrupt isolated blobs).
>
> Place these objects. Positions are given as a percentage from the left edge and from the top edge of the picture, matching the numbered circles on the guide. Draw every one of them, once, and do NOT add any text, labels, numbers or legend:
{body}
>
> Keep the area immediately around each town and landmark reasonably clear so that a route line and small icons can be added on top later. Absolutely NO text, NO place names, NO numbers, NO red markers, NO route line, NO border or frame.

**驗收：** 海岸線跟草圖一致（尤其北部、恆春半島、花東海岸）；沒有文字、沒有邊框、海一直延伸到圖的四邊；五座山、縱谷、四個離島都在。位置偏很多就在同一個對話說 "move object N toward ..."，或整張重畫。

存檔：`generated/real_map_base.png`（有多張就挑一張，再告訴我尺寸）。
"""
    (root / "REAL_MAP_PROMPT.md").write_text(text)
    print("prompt written")


if __name__ == "__main__":
    write_guide()
    write_prompt()
