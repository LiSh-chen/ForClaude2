"""把日誌/檢討/績效渲染成靜態網站（相對路徑，可放在任意子路徑，如 GitHub Pages 的 /journal/）。"""
from __future__ import annotations
import html
import re
from pathlib import Path

import markdown

from . import config as C
from .utils import isnum, pct

CSS = """
:root{--bg:#f7f7f4;--fg:#1c2421;--card:#fff;--muted:#68726d;--line:#dcdfda;--accent:#1f6f4a;--up:#c0392b;--down:#1a8f4c}
@media (prefers-color-scheme:dark){:root{--bg:#121614;--fg:#e6eae7;--card:#1a1f1c;--muted:#9aa59f;--line:#2c342f;--accent:#5bbd8a}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.7 system-ui,-apple-system,"Noto Sans TC",sans-serif}
header{background:var(--card);border-bottom:1px solid var(--line);position:sticky;top:0;z-index:5}
nav{max-width:1000px;margin:0 auto;padding:10px 16px;display:flex;gap:14px;flex-wrap:wrap;align-items:center}
nav b{margin-right:auto}nav a{color:var(--accent);text-decoration:none;font-weight:600}
main{max-width:1000px;margin:0 auto;padding:12px 16px 60px}
h1{font-size:1.5rem}h2{margin-top:2rem;border-bottom:1px solid var(--line);padding-bottom:4px}h3{margin-top:1.6rem}
a{color:var(--accent)}blockquote{margin:10px 0;padding:6px 14px;border-left:4px solid var(--accent);background:var(--card);color:var(--muted)}
.tw{overflow-x:auto}table{border-collapse:collapse;font-size:13.5px;margin:10px 0;min-width:100%}
th,td{border:1px solid var(--line);padding:5px 8px;text-align:left;vertical-align:top}th{background:var(--card)}
code{background:var(--card);padding:1px 4px;border-radius:4px}.muted{color:var(--muted)}
ul.arch{list-style:none;padding:0}ul.arch li{padding:6px 0;border-bottom:1px solid var(--line)}
svg text{fill:var(--fg);font-size:11px}
footer{max-width:1000px;margin:0 auto;padding:0 16px 40px;color:var(--muted);font-size:12.5px}
"""
NAV = [("index.html", "儀表板"), ("archive.html", "日誌"), ("reviews.html", "檢討"), ("methodology.html", "方法論")]


def md2html(text: str) -> str:
    h = markdown.markdown(text, extensions=["tables", "sane_lists"])
    return re.sub(r"<table>", '<div class="tw"><table>', h).replace("</table>", "</table></div>")


def page(title: str, body: str, root: str = "") -> str:
    nav = "".join(f'<a href="{root}{u}">{t}</a>' for u, t in NAV)
    return (f'<!doctype html><html lang="zh-Hant"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
            f"<title>{html.escape(title)}｜投資研究日誌</title><style>{CSS}</style></head><body>"
            f'<header><nav><b>📒 投資研究日誌</b>{nav}</nav></header><main>{body}</main>'
            f'<footer>本站內容由系統自動產生，僅供研究與教育用途，不構成投資建議；目標價為模型估算，投資有風險。</footer></body></html>')


def _bar_svg(items: list[tuple[str, float]], w=900, h=220) -> str:
    if not items:
        return ""
    m = max(abs(v) for _, v in items) or 0.01
    bw = max(8, min(40, (w - 60) // len(items) - 4))
    mid = h / 2 - 10
    out = [f'<svg viewBox="0 0 {w} {h}" width="100%" role="img" aria-label="各期推薦批次平均超額報酬"><line x1="30" x2="{w}" y1="{mid}" y2="{mid}" stroke="#999"/>']
    for i, (lab, v) in enumerate(items):
        x = 70 + i * (bw + 4)
        bh = abs(v) / m * (mid - 20)
        y = mid - bh if v >= 0 else mid
        out.append(f'<rect x="{x}" y="{y:.1f}" width="{bw}" height="{max(bh, 1):.1f}" fill="{"#1f6f4a" if v >= 0 else "#c0392b"}"><title>{lab}: {v * 100:+.1f}%</title></rect>')
        if len(items) <= 24:
            out.append(f'<text x="{x + bw / 2}" y="{h - 6}" text-anchor="middle" transform="rotate(-60 {x + bw / 2} {h - 6})">{lab[5:]}</text>')
    out.append(f'<text x="2" y="12">+{m * 100:.0f}%</text></svg>')
    return "".join(out)


def write_dashboard(out: Path, data: Path) -> None:
    """複製前端檔案，並輸出儀表板用的資料（每日快照、日期索引、績效）。"""
    import shutil
    web = Path(__file__).parent / "web"
    for f in ("index.html", "app.js", "style.css"):
        shutil.copyfile(web / f, out / f)
    (out / "data" / "snap").mkdir(parents=True, exist_ok=True)
    dates = []
    for f in sorted((data / "snapshots").glob("*.json"), reverse=True) if (data / "snapshots").exists() else []:
        sn = C.load_json(f)
        shutil.copyfile(f, out / "data" / "snap" / f.name)
        dates.append({"date": f.stem, "tw": len(sn["picks"]["TW"]), "us": len(sn["picks"]["US"]),
                      "em": sum(len(v) for v in (sn.get("emerging") or {"picks": {}})["picks"].values())})
    C.save_json(out / "data" / "index.json", {"dates": dates})
    st = C.load_json(data / "pipeline_status.json")
    C.save_json(out / "data" / "status.json", st if st else {})
    perf = C.load_json(data / "performance.json")
    C.save_json(out / "data" / "performance.json", perf if perf else {"positions": [], "cohorts": [], "summary": {}})


def build_site() -> None:
    out = C.path("site")
    jdir, rdir, data = C.path("journal"), C.path("reviews"), C.path("data")
    (out / "journal").mkdir(exist_ok=True)
    (out / "reviews").mkdir(exist_ok=True)
    edir = C.path("emerging")
    (out / "emerging").mkdir(exist_ok=True)
    journals = sorted(jdir.glob("*.md"), reverse=True)
    emerging = sorted(edir.glob("*.md"), reverse=True)
    reviews = sorted(rdir.glob("*.md"), reverse=True)
    # 日誌頁
    for f in journals:
        body = md2html(f.read_text(encoding="utf-8"))
        (out / "journal" / f"{f.stem}.html").write_text(page(f.stem, body, "../"), encoding="utf-8")
    for f in reviews:
        (out / "reviews" / f"{f.stem}.html").write_text(page(f.stem, md2html(f.read_text(encoding="utf-8")), "../"), encoding="utf-8")
    for f in emerging:
        (out / "emerging" / f"{f.stem}.html").write_text(page("前瞻專區 " + f.stem, md2html(f.read_text(encoding="utf-8")), "../"), encoding="utf-8")
    write_dashboard(out, data)
    # 列表
    rows = []
    for f in journals:
        pj = C.load_json(data / "picks" / f"{f.stem}.json") or {"picks": []}
        tk = "、".join(p["ticker"] for p in pj["picks"])
        rows.append(f'<li><a href="journal/{f.stem}.html"><b>{f.stem}</b></a> <span class="muted">{html.escape(tk)}</span></li>')
    (out / "archive.html").write_text(page("日誌列表", f'<h1>日誌列表</h1><ul class="arch">{"".join(rows) or "<li>尚無</li>"}</ul>'), encoding="utf-8")
    rrows = [f'<li><a href="reviews/{f.stem}.html"><b>{f.stem}</b></a></li>' for f in reviews]
    (out / "reviews.html").write_text(page("檢討報告", f'<h1>檢討報告</h1><p class="muted">每週追蹤績效、每月檢討並在樣本足夠時回饋參數。</p><ul class="arch">{"".join(rrows) or "<li>尚無</li>"}</ul>'), encoding="utf-8")
    # 舊網址導向儀表板分頁
    for old, tab in (("performance.html", "perf"), ("emerging.html", "emerging")):
        (out / old).write_text(f'<!doctype html><meta charset="utf-8"><meta http-equiv="refresh" content="0;url=index.html#{tab}"><a href="index.html#{tab}">前往儀表板</a>', encoding="utf-8")
    # 方法論
    mp = C.ROOT / "docs" / "methodology.md"
    (out / "methodology.html").write_text(page("方法論", md2html(mp.read_text(encoding="utf-8")) if mp.exists() else "<p>—</p>"), encoding="utf-8")
    print(f"[site] 已建置 {len(journals)} 篇日誌、{len(reviews)} 篇檢討 → {out}")
