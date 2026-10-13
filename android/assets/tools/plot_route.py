#!/usr/bin/env python3
"""Draws previews/taiwan_route_map.png from core/build/review/taiwan_route.json (written by RouteTest).
Needs matplotlib + basemap (pip install matplotlib basemap basemap-data)."""
import json
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib import font_manager
from mpl_toolkits.basemap import Basemap

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT.parent / "core" / "build" / "review" / "taiwan_route.json"
OUT = ROOT / "previews" / "taiwan_route_map.png"
font_manager.fontManager.addfont("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc")
plt.rcParams["font.family"] = "WenQuanYi Zen Hei"

stops = json.loads(DATA.read_text(encoding="utf-8"))
numbered = [s for s in stops if s["kind"] in ("card", "supply")]
for i, s in enumerate(numbered, 1):
    s["no"] = i

fig = plt.figure(figsize=(24, 22), dpi=100, facecolor="#f4ead2")
ax = fig.add_axes([0.01, 0.02, 0.42, 0.96])
m = Basemap(projection="merc", llcrnrlon=119.9, llcrnrlat=21.75, urcrnrlon=122.25, urcrnrlat=25.4, resolution="i", ax=ax)
m.drawmapboundary(fill_color="#cfe0e0")
m.fillcontinents(color="#e9dfbf", lake_color="#cfe0e0")
m.drawcoastlines(linewidth=0.8, color="#6b5a48")
m.drawparallels(range(22, 26), labels=[1, 0, 0, 0], linewidth=0.3, color="#9aa", fontsize=11)
m.drawmeridians([120, 121, 122], labels=[0, 0, 0, 1], linewidth=0.3, color="#9aa", fontsize=11)

pts = [m(s["lng"], s["lat"]) for s in stops]
for i in range(1, len(stops)):
    transit = stops[i]["kind"] == "transit"
    ax.plot([pts[i - 1][0], pts[i][0]], [pts[i - 1][1], pts[i][1]],
            color="#c0392b" if transit else "#2f5d62", lw=2.2 if not transit else 2.4,
            ls=(0, (3, 3)) if transit else "-", alpha=0.9, zorder=3)
    if transit:
        mx, my = (pts[i - 1][0] + pts[i][0]) / 2, (pts[i - 1][1] + pts[i][1]) / 2
        ax.text(mx + 6000, my, stops[i]["text"] + "\n（不計步數）", fontsize=12, color="#c0392b", va="center", zorder=6,
                bbox=dict(boxstyle="round,pad=0.2", fc="#fff6", ec="none"))
for s, (x, y) in zip(stops, pts):
    if s["kind"] == "card":
        ax.scatter([x], [y], s=330, c="#2f5d62", ec="white", lw=1.5, zorder=5)
        ax.text(x, y, str(s["no"]), color="white", ha="center", va="center", fontsize=9.5, fontweight="bold", zorder=6)
    elif s["kind"] == "supply":
        ax.scatter([x], [y], s=300, c="#e58a57", ec="white", lw=1.5, marker="s", zorder=5)
        ax.text(x, y, str(s["no"]), color="white", ha="center", va="center", fontsize=9.5, fontweight="bold", zorder=6)
    elif s["kind"] == "transit":
        ax.scatter([x], [y], s=60, c="#c0392b", zorder=5)
        ax.text(x + 4000, y - 9000, s["name"], fontsize=11, color="#c0392b", zorder=6)
ax.set_title("星霧大陸・台灣環島路線（順時針）  約 %.0f 公里" % stops[-1]["km"], fontsize=22, color="#3b2a1a", pad=14)

# Legend, two columns
lx = fig.add_axes([0.45, 0.02, 0.54, 0.96]); lx.axis("off"); lx.set_xlim(0, 2); lx.set_ylim(0, 1)
rows = len(numbered)
per_col = (rows + 1) // 2
lx.text(0.0, 0.995, "●卡片　■補給站（台鐵車站紀念章：站名＋開站年份）　┅搭車／搭渡輪（不計步數）", fontsize=14, color="#3b2a1a", va="top")
for idx, s in enumerate(numbered):
    col, row = divmod(idx, per_col)
    x = col * 1.0
    y = 0.955 - row * (0.93 / per_col)
    mark = "●" if s["kind"] == "card" else "■"
    colr = "#2f5d62" if s["kind"] == "card" else "#e58a57"
    lx.text(x, y, f'{s["no"]:>2}', fontsize=13, color="white", fontweight="bold", ha="right", va="center",
            bbox=dict(boxstyle="circle,pad=0.25" if s["kind"] == "card" else "square,pad=0.25", fc=colr, ec="none"))
    detail = s["text"] if s["kind"] == "card" else f'{s["text"]} 年開站'
    lx.text(x + 0.03, y, f'{s["name"]}', fontsize=14, color="#3b2a1a", va="center", fontweight="bold")
    lx.text(x + 0.03, y - 0.011, f'{detail}　{s["km"]:.0f} km', fontsize=11, color="#6b5a48", va="top")
fig.savefig(OUT, facecolor=fig.get_facecolor())
print("wrote", OUT)
