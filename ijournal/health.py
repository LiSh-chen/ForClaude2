"""績效健檢：把追蹤結果整理成『一頁看懂』的狀態——現在表現如何、為什麼、樣本夠不夠下結論、檢討機制什麼時候會調整系統。
每次追蹤（每日）都會重算並寫入 data/health.json，儀表板績效頁頂端與週度檢討報告共用同一份結果。
重點是避免兩種錯誤：①樣本太少就過度反應 ②每天重複推薦同一檔股票，被當成許多獨立樣本。"""
from __future__ import annotations
import datetime as dt

from . import config as C
from .utils import isnum


def _avg(xs):
    xs = [x for x in xs if isnum(x)]
    return sum(xs) / len(xs) if xs else None


def _next_saturday(today: dt.date, include_today: bool) -> dt.date:
    d = today + dt.timedelta(days=(5 - today.weekday()) % 7)
    return d if (d > today or include_today) else d + dt.timedelta(days=7)


def _first_saturday(y: int, m: int) -> dt.date:
    d = dt.date(y, m, 1)
    return d + dt.timedelta(days=(5 - d.weekday()) % 7)


def schedule(today: dt.date, reviews: list[str], history: list[dict], params: dict) -> dict:
    last = max(reviews) if reviews else None                       # 檔名 YYYY-MM-DD-weekly/monthly
    last_date = dt.date.fromisoformat(last[:10]) if last else None
    nxt = _next_saturday(today, include_today=(last_date != today))
    fs = _first_saturday(today.year, today.month)
    if fs < today or (fs == today and last_date == today):
        y, m = (today.year + 1, 1) if today.month == 12 else (today.year, today.month + 1)
        fs = _first_saturday(y, m)
    ch = history[-1] if history else None
    return {"last_review": str(last_date) if last_date else None, "last_period": last[11:] if last else None, "next_weekly": str(nxt), "next_monthly": str(fs),
            "params_version": params["version"], "last_change": {"date": ch["date"], "version": ch["version"], "changes": ch["changes"]} if ch else None}


def build_health(perf: dict, params: dict, history: list[dict], reviews: list[str], sector_names: dict, today: dt.date | None = None) -> dict:
    today = today or dt.date.today()
    t = params["tuning"]
    pos = [p for p in perf.get("positions", []) if p.get("book", "main") == "main"]
    H = t["horizon_days"]
    out = {"asof": str(today), "n_picks": len(pos), "findings": [], "schedule": schedule(today, reviews, history, params)}
    if not pos:
        out.update(verdict={"level": "sample", "label": "還沒有推薦紀錄可檢討"}, progress={}, n_unique=0)
        return out
    first = {}
    for p in sorted(pos, key=lambda p: p["date"]):
        first.setdefault(p["ticker"], p)
    n_u = len(first)
    bench = [p["rlast"] - p["alast"] for p in pos if isnum(p.get("alast")) and isnum(p.get("rlast"))]
    a_all, r_all, b_all = _avg([p.get("alast") for p in pos]), _avg([p.get("rlast") for p in pos]), _avg(bench)
    a_first = _avg([p.get("alast") for p in first.values()])
    m5 = [p for p in pos if isnum(p.get("r5"))]
    by_m = {}
    for m in ("TW", "US"):
        ps = [p for p in pos if p["market"] == m]
        if ps:
            by_m[m] = {"n": len(ps), "alpha": _avg([p.get("alast") for p in ps]), "ret": _avg([p.get("rlast") for p in ps]), "bench": _avg([p["rlast"] - p["alast"] for p in ps if isnum(p.get("alast"))])}
    sec = {}
    for p in pos:
        sec[p["sector"]] = sec.get(p["sector"], 0) + 1
    top = sorted(sec.items(), key=lambda kv: -kv[1])[:3]
    stop = sum(1 for p in pos if p.get("stop_hit")) / len(pos)
    ranked = sorted((p for p in first.values() if isnum(p.get("alast"))), key=lambda p: p["alast"])
    matured = [p for p in pos if isnum(p.get(f"r{H}"))]
    n_mat_u = len({p["ticker"] for p in matured})
    prog = {"matured": len(matured), "need": t["min_matured_picks"], "matured_unique": n_mat_u, "need_unique": t.get("min_unique_tickers", 20),
            "horizon": H, "days_tracked": max(p.get("n_days", 0) for p in pos)}
    out.update(n_unique=n_u, first_date=min(p["date"] for p in pos), last_date=max(p["date"] for p in pos),
               alpha=a_all, ret=r_all, bench=b_all, alpha_first=a_first, by_market=by_m, stop_rate=stop, matured5=len(m5),
               top_sectors=[{"id": k, "name": sector_names.get(k, k), "share": v / len(pos)} for k, v in top],
               worst=[{"ticker": p["ticker"], "name": p["name"], "alpha": p["alast"]} for p in ranked[:3]], best=[{"ticker": p["ticker"], "name": p["name"], "alpha": p["alast"]} for p in ranked[-3:][::-1]],
               progress=prog)
    # --- 一句話結論：先講事實，再講『能不能下結論』
    enough = prog["matured"] >= prog["need"] and n_mat_u >= prog["need_unique"]
    pc = lambda x: f"{x * 100:+.1f}%"
    if not enough:
        out["verdict"] = {"level": "sample", "label": f"推薦後平均超額 {pc(a_all)}，但樣本太少（{n_u} 檔不同股票、到期 {len(matured)} 筆），還不能判斷選股好壞"}
    elif a_all < -0.01:
        out["verdict"] = {"level": "weak", "label": f"推薦後平均超額 {pc(a_all)}，落後大盤，需要檢討"}
    else:
        out["verdict"] = {"level": "ok", "label": f"推薦後平均超額 {pc(a_all)}，表現不輸大盤"}
    F = out["findings"]
    F.append({"level": "info", "text": f"{len(pos)} 筆推薦其實只有 {n_u} 檔不同股票（同一檔每天重複計入）；去重後自首次推薦起平均超額 {pc(a_first)}。"})
    if isnum(b_all) and isnum(r_all):
        F.append({"level": "info", "text": f"同期大盤平均 {pc(b_all)}、推薦平均 {pc(r_all)}：" + ("大盤本身在下跌，絕對報酬為負不全是選股問題。" if b_all < -0.01 else "大盤沒有明顯下跌。")})
    if len(by_m) == 2 and isnum(by_m["TW"]["alpha"]) and isnum(by_m["US"]["alpha"]) and abs(by_m["TW"]["alpha"] - by_m["US"]["alpha"]) >= 0.02:
        w, b = sorted(by_m, key=lambda k: by_m[k]["alpha"])
        F.append({"level": "warn", "text": f"{'台股' if w == 'TW' else '美股'}拖累整體：超額 {pc(by_m[w]['alpha'])}（{'台股' if b == 'TW' else '美股'} {pc(by_m[b]['alpha'])}）。"})
    if top and top[0][1] / len(pos) >= 0.25:
        F.append({"level": "warn", "text": "推薦集中在 " + "、".join(f"{sector_names.get(k, k)} {v / len(pos):.0%}" for k, v in top[:2]) + "：績效主要反映這些題材的行情，而不是選股能力。"})
    if stop >= 0.3:
        F.append({"level": "warn", "text": f"{stop:.0%} 的推薦已碰到參考停損；若大盤沒有同步重挫，代表停損設太近或選到高波動股。"})
    if ranked and ranked[0]["alast"] < -0.05:
        F.append({"level": "warn", "text": "拖累最大：" + "、".join(f"{p['ticker']}（{pc(p['alast'])}）" for p in ranked[:3]) + "。"})
    if not enough:
        F.append({"level": "info", "text": f"調整系統所需的樣本還不夠：到期 {len(matured)}／{prog['need']} 筆、不同股票 {n_mat_u}／{prog['need_unique']} 檔。在此之前不自動調參，避免追著雜訊跑。"})
    F.append({"level": "info", "text": f"檢討機制：每週六產生檢討報告；每月第一個週六在樣本足夠時調整參數（上次檢討 {out['schedule']['last_review'] or '尚無'}、下次 {out['schedule']['next_weekly']}）。"})
    # --- 結構性建議（讓檢討不只是描述）
    acts = []
    if top and top[0][1] / len(pos) >= 0.25:
        acts.append("評估加入『同題材跨市場上限』：目前只限同市場同產業 2 檔，美股與台股的同一題材（如 AI 晶片）仍會同時入選，放大單一題材的風險。")
    if stop >= 0.3:
        acts.append("檢討停損規則（目前約 2.5 倍 ATR、6%–18%）是否貼合持有期；並區分『大盤急跌』與『個股論點被推翻』兩種情境。")
    out["actions"] = acts
    return out


def write_health(perf: dict) -> dict:
    data = C.path("data")
    reviews = sorted(f.stem for f in C.path("reviews").glob("*.md"))
    h = build_health(perf, C.load_params(), C.load_json(C.history_path(), []), reviews, {s["id"]: s["name"] for s in C.load_universe()["sectors"]})
    C.save_json(data / "health.json", h)
    return h
