"""每日流程編排。"""
from __future__ import annotations
import datetime as dt
import json

import pandas as pd

from . import config as C
from . import llm
from . import emerging as E
from . import sessions, snapshot
from .features import norm_fundamentals, price_features
from .journal import candidate_record, pick_record, write_journal
from .news import scan
from .selection import eligible, pick, score_sectors, score_stocks
from .utils import market_of


def _index_stats(prices, uni):
    out = []
    for ix in uni["indices"]:
        df = prices.get(ix["t"])
        if df is None or len(df) < 70:
            continue
        c = df["Close"]
        out.append({"name": ix["name"], "series": [round(float(v), 2) for v in c.tail(60)], "last": float(c.iloc[-1]), "ret_1d": float(c.iloc[-1] / c.iloc[-2] - 1),
                    "ret_1m": float(c.iloc[-1] / c.iloc[-22] - 1), "ret_3m": float(c.iloc[-1] / c.iloc[-64] - 1)})
    return out


def run_daily(provider, force: bool = False, asof: str | None = None) -> str | None:
    uni, params, src = C.load_universe(), C.load_params(), C.load_sources()
    tickers = list(uni["stocks"]) + [i["t"] for i in uni["indices"]] + list(uni["benchmarks"].values()) + list(uni["reference"].values())
    tickers = list(dict.fromkeys(tickers))
    print(f"[daily] 抓取 {len(tickers)} 檔價格…")
    prices = provider.prices(tickers)
    if provider.name == "live" and not asof:
        prices, n_part = sessions.strip_partial(prices)
        if n_part:
            print(f"[daily] 交易所尚在盤中：已剔除 {n_part} 檔『未完成的當日 K 線』，只使用已收盤資料。")
    if asof:
        cut = pd.Timestamp(asof)
        prices = {t: d[d.index <= cut] for t, d in prices.items()}
    # 行情基準日：各市場以「參考標的」的最新收盤日判斷（台股用 2330.TW，因 Yahoo 的 ^TWII 常慢一天）
    basis = {m: str(prices[r].index[-1].date()) for m, r in uni["reference"].items() if r in prices and len(prices[r])}
    if not basis:
        raise SystemExit("取不到參考標的價格，無法決定資料基準日（請檢查網路/資料來源）。")
    date = max(basis.values())
    bdates = [pd.Timestamp(date)]
    jpath = C.path("journal") / f"{date}.md"
    if jpath.exists() and not force:
        # 同一基準日會被更新兩次：台股收盤後（美股仍是前一日）→ 美股收盤後（兩邊都最新）。
        # 只有『某市場的收盤日比上次更新』才覆蓋；否則略過（排程重複觸發時不浪費時間）。
        old = (C.load_json(C.path("data") / "snapshots" / f"{date}.json") or {}).get("basis") or {}
        if not any((basis.get(m) or "") > (old.get(m) or "") for m in basis):
            print(f"[daily] {date} 的日誌已存在且沒有更新的行情（台股 {basis.get('TW')}、美股 {basis.get('US')}），略過。")
            return None
        print(f"[daily] {date} 的日誌已存在，但有更新的行情（原 {old}，現 {basis}）→ 覆蓋更新。")

    feats = {}
    for t, st in uni["stocks"].items():
        b = prices.get(uni["benchmarks"][st["market"]])
        f = price_features(prices.get(t), b)
        if f and f["date"] >= str((max(bdates) - pd.Timedelta(days=7)).date()):  # 排除停牌/下市（資料落後超過 7 天）
            feats[t] = f
    print(f"[daily] 有效價格 {len(feats)}/{len(uni['stocks'])} 檔；抓取基本面…")
    funds = {}
    for t in feats:
        funds[t] = norm_fundamentals(provider.fundamentals(t), feats[t]["last"], "TWD" if market_of(t) == "TW" else "USD")
    print("[daily] 掃描新聞…")
    items = provider.news(src)
    scan_res = scan(items, uni, src)

    data = C.path("data")
    nh_path = data / "news_history.json"
    nh = C.load_json(nh_path, {})
    sec_scores = score_sectors(uni, scan_res, feats, funds, {d: h for d, h in nh.items() if d < date}, params)
    rows = score_stocks(uni, feats, funds, sec_scores, scan_res, params)
    sel = pick(rows, sec_scores, params)
    picks, notes, stats = sel["picks"], sel["notes"], sel["stats"]
    if not any(picks.values()):
        print("[daily] 今日沒有任何標的通過篩選；仍會寫入日誌說明。")

    # 前期名單、績效摘要、參數歷史
    prev = None
    for f in sorted((data / "picks").glob("*.json") if (data / "picks").exists() else [], reverse=True):
        if f.stem < date:
            pj = C.load_json(f)
            prev = {"date": f.stem, "picks": pj["picks"]}
            break
    perf = C.load_json(data / "performance.json")
    ctx = {
        "params": params, "uni": uni, "sec_scores": sec_scores, "picks": picks, "notes": notes, "stats": stats, "rows": rows, "scan": scan_res,
        "fetch_log": provider.fetch_log, "provider": provider.name, "indices": _index_stats(prices, uni), "prev": prev,
        "perf_summary": perf["summary"] if perf and perf.get("summary") else None,
        "param_history": C.load_json(C.history_path(), []), "elig": lambda r: eligible(r, params), "basis": basis,
    }
    # 選用 LLM 評論
    if llm.available():
        ctx["llm"] = llm.commentary({
            "date": date, "indices": ctx["indices"],
            "top_sectors": {m: [{"name": v["name"], "score": v["score"], "tags": v["tags"], "headlines": [h["title"] for h in scan_res["sector"][k][m]["heads"][:3]]}
                                for k, v in sorted(sec_scores[m].items(), key=lambda kv: kv[1]["rank"])[:5]] for m in sec_scores},
            "picks": [{"ticker": r["ticker"], "name": r["zh_name"], "sector": r["sector"], "upside": r["val"]["upside"], "scores": r["scores"],
                       "roe": r["f"].get("roe"), "rev_growth": r["f"].get("rev_growth"), "pe_fwd": r["f"].get("pe_fwd")} for m in picks for r in picks[m]],
        })

    # 前瞻專區（失敗不得影響主日誌）
    emg = None
    try:
        themes = E.score_themes(uni, items, scan_res, src, rows, params)
        e_picked = E.pick_emerging(themes, rows, {r["ticker"] for m in picks for r in picks[m]}, params)
        clues = E.discover(items, uni)
        (C.path("emerging") / f"{date}.md").write_text(E.write_report(date, themes, e_picked, rows, params, clues, provider.name, scan_res["n_used"]), encoding="utf-8")
        edir = C.path("data") / "emerging"
        edir.mkdir(exist_ok=True)
        C.save_json(edir / f"{date}.json", E.record(e_picked, themes, date, params, clues))
        ctx["emerging_top"] = [(t["name"], t["status"]) for t in themes[:3]]
        emg = {"themes": themes, "picked": e_picked, "clues": clues}
        print(f"[daily] 前瞻專區：趨勢 {len(themes)} 個，推薦 {sum(len(v) for v in e_picked.values())} 檔")
    except Exception as ex:  # noqa: BLE001
        print(f"[daily] 前瞻專區產生失敗（略過）：{ex!r}")

    md = write_journal(date, ctx)
    jpath.write_text(md, encoding="utf-8")
    try:
        sdir = C.path("data") / "snapshots"
        sdir.mkdir(exist_ok=True)
        C.save_json(sdir / f"{date}.json", snapshot.build(date, ctx, emg, prices))
    except Exception as ex:  # noqa: BLE001
        print(f"[daily] 儀表板快照產生失敗（略過）：{ex!r}")
    pdir = data / "picks"
    pdir.mkdir(exist_ok=True)
    C.save_json(pdir / f"{date}.json", {
        "date": date, "params_version": params["version"], "provider": provider.name, "llm": bool(ctx.get("llm")),
        "picks": [pick_record(r) for m in ("TW", "US") for r in picks[m]],
        "focus_sectors": {m: [k for k, v in sorted(sec_scores[m].items(), key=lambda kv: kv[1]["rank"])[: params["selection"]["focus_sectors_per_market"]]] for m in sec_scores},
    })
    cdir = data / "candidates"
    cdir.mkdir(exist_ok=True)
    picked = {r["ticker"] for m in picks for r in picks[m]}
    focus = {m: set(k for k, v in sec_scores[m].items() if v["rank"] <= params["selection"]["focus_sectors_per_market"]) for m in sec_scores}
    C.save_json(cdir / f"{date}.json", {"date": date, "rows": [candidate_record(r, picked, focus) for r in rows.values() if r["composite"] is not None]})
    nh[date] = {m: {k: scan_res["sector"][k][m]["heat"] + 0.5 * scan_res["sector"][k]["TW" if m == "US" else "US"]["heat"] for k in scan_res["sector"]} for m in ("US", "TW")}
    C.save_json(nh_path, dict(sorted(nh.items())[-120:]))
    print(f"[daily] 完成：{jpath}（台股 {len(picks['TW'])} 檔、美股 {len(picks['US'])} 檔）")
    return date
