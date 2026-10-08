"""把當日分析結果整理成給儀表板用的精簡快照（JSON）。詳細內容由前端點擊後才展開。"""
from __future__ import annotations

import datetime as dt

from . import emerging as E
from .journal import analyze, thesis
from .selection import eligible
from .utils import isnum

FIN_KEYS = ("market_cap", "pe_fwd", "pe_ttm", "peg", "roe", "gross_margin", "op_margin", "net_margin", "rev_growth", "eps_growth", "de", "fcf_margin", "beta", "target_mean", "n_analysts")


def _r(x, d=4):
    return round(x, d) if isnum(x) else None


def series(prices: dict, t: str, n: int = 120) -> dict:
    df = prices.get(t)
    if df is None:
        return {"d": [], "c": []}
    c = df["Close"].dropna().tail(n)
    return {"d": [x.strftime("%y%m%d") for x in c.index], "c": [round(float(v), 2) for v in c]}


def stock_obj(r: dict, sec: dict | None, risk_ctx: dict, prices: dict, names: dict, extra: dict | None = None, sec_heads: list | None = None) -> dict:
    v, f, pf = r["val"], r["f"], r["pf"]
    th, risks = thesis(r, sec, risk_ctx)
    an = analyze(r, sec, risk_ctx, sec_heads)
    stop = pf["last"] - max(min(2.5 * pf["atr14"], 0.18 * pf["last"]), 0.06 * pf["last"])
    o = {
        "ticker": r["ticker"], "name": r["zh_name"], "market": r["market"], "sector": r["sector"], "sector_name": names.get(r["sector"], r["sector"]),
        "rank": r.get("pick_rank"), "price": _r(pf["last"], 2), "ccy": f.get("currency"), "asof": pf["date"],
        "composite": _r(r["composite"], 1), "scores": {k: _r(x, 1) for k, x in r["scores"].items()},
        "target": {"base": _r(v["base"], 2), "bull": _r(v["bull"], 2), "bear": _r(v["bear"], 2), "ev": _r(v["ev"], 2), "upside": _r(v["upside"]), "ev_upside": _r(v["ev_upside"]),
                   "confidence": v["confidence"], "capped": bool(v["capped"]), "raw_upside": _r(v["raw_blend"] / pf["last"] - 1), "rr": _r(v["rr"], 2), "stop": _r(stop, 2), "spread": _r(v["spread"], 2),
                   "peer_pe": _r(v["peer_pe"], 1)},
        "methods": [{"k": k, "label": m["label"], "value": _r(m["value"], 2), "weight": _r(m["weight"], 3), "upside": _r(m["upside"]), "detail": m["detail"]} for k, m in v["methods"].items()],
        "dropped": [m["label"] for m in v["dropped"].values()],
        "fin": {k: _r(f.get(k), 4) for k in FIN_KEYS},
        "tech": {k: _r(pf.get(k), 4) for k in ("ret_1m", "ret_3m", "rel_3m", "rel_6m", "dist200", "from_high", "vol_ann")},
        "thesis": th, "risks": risks, "headline": an["headline"], "highlights": an["highlights"], "why": an["why"], "risks_now": an["risks_now"], "risks_watch": an["risks_watch"],
        "news": [{"title": h["title"], "link": h["link"], "source": h["source"], "sent": _r(h.get("sent"), 2)} for h in ((r.get("news") or {}).get("heads") or [])[:3]],
        "series": series(prices, r["ticker"]),
    }
    if extra:
        o.update(extra)
    return o


def build(date: str, ctx: dict, emg: dict | None, prices: dict) -> dict:
    p, uni, sec_scores, picks, rows = ctx["params"], ctx["uni"], ctx["sec_scores"], ctx["picks"], ctx["rows"]
    names = {s["id"]: s["name"] for s in uni["sectors"]} | {t["id"]: t["name"] for t in uni["themes"]}
    sectors_cfg = {s["id"]: s for s in uni["sectors"]}
    picked = {r["ticker"] for m in picks for r in picks[m]}
    N = p["selection"]["picks_per_market"]

    last = lambda tk: str(prices[tk].index[-1].date()) if tk in prices and len(prices[tk]) else None
    snap = {"date": date, "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
            "basis": {"TW": last("^TWII"), "US": last("^GSPC")}, "params_version": p["version"], "provider": ctx["provider"], "llm": bool(ctx.get("llm")),
            "summary": (ctx.get("llm") or {}).get("summary"),
            "news": {"ok": sum(1 for x in ctx["fetch_log"] if x["ok"]), "total": len(ctx["fetch_log"]), "items": ctx["scan"]["n_items"], "used": ctx["scan"]["n_used"],
                     "failed": [x["source"] for x in ctx["fetch_log"] if not x["ok"]]},
            "indices": ctx["indices"]}
    # --- 主榜
    snap["picks"] = {m: [stock_obj(r, sec_scores[m].get(r["sector"]), {"traits": sectors_cfg[r["sector"]].get("risk_traits"), "watch": sectors_cfg[r["sector"]].get("risk_watch")}, prices, names,
                                   sec_heads=ctx["scan"]["sector"][r["sector"]][m]["heads"]) for r in picks[m]] for m in ("TW", "US")}
    snap["shortage"] = {m: {"n": len(picks[m]), "N": N, "candidates": ctx["stats"][m]["candidates"], "reasons": ctx["stats"][m]["reasons"]} for m in ("TW", "US")}
    focus_n = p["selection"]["focus_sectors_per_market"]
    snap["sectors"] = {}
    for m in ("TW", "US"):
        lst = []
        for k, s in sorted(sec_scores[m].items(), key=lambda kv: kv[1]["rank"]):
            members = []
            for r in sorted((x for x in rows.values() if x["market"] == m and x["sector"] == k and x["composite"] is not None), key=lambda x: -x["composite"]):
                ok, why = eligible(r, p)
                members.append({"t": r["ticker"], "n": r["zh_name"], "c": _r(r["composite"], 1), "s": "入選" if r["ticker"] in picked else ("合格未入選" if ok else why)})
            heads = ctx["scan"]["sector"][k][m]["heads"][:3] or ctx["scan"]["sector"][k]["US" if m == "TW" else "TW"]["heads"][:2]
            lst.append({"id": k, "name": s["name"], "rank": s["rank"], "focus": s["rank"] <= focus_n, "score": _r(s["score"], 1), "parts": {"news": _r(s["heat_rank_score"], 0), "mom": _r(s["mom_rank_score"], 0), "fund": _r(s["fund_rank_score"], 0)},
                        "ret_1m": _r(s["ret_1m"]), "n_news": s["news"]["n"], "sent": _r(s["news"]["sent"], 2), "tags": s["tags"], "risk": sectors_cfg[k]["risk"],
                        "heads": [{"title": h["title"], "link": h["link"], "source": h["source"]} for h in heads], "members": members[:12]})
        snap["sectors"][m] = lst
    # --- 前瞻專區
    if emg:
        themes, ep = emg["themes"], emg["picked"]
        e = p["emerging"]
        got = {r["ticker"] for m in ep for r in ep[m]}
        th_cfg = {t["id"]: t for t in uni["themes"]}
        eth = []
        for t in themes:
            mem = []
            for tk in t["members"]:
                r = rows.get(tk)
                if not r:
                    continue
                ok, why = E.eligible(r, p)
                mem.append({"t": tk, "n": r["zh_name"], "c": _r(r["composite"], 1), "s": "入選" if tk in got else ("合格未入選" if ok else why)})
            eth.append({"id": t["id"], "name": t["name"], "short": th_cfg[t["id"]].get("short"), "tier": t["tier"], "rank": t["rank"], "score": _r(t["score"], 1), "parts": {k: _r(x, 0) for k, x in t["components"].items()},
                        "coverage": _r(t["coverage_ratio"], 2), "n_headlines": t["n_headlines"], "status": t["status"], "rel_6m": _r(t["rel_6m"]), "above200": _r(t["above200"], 2),
                        "thesis": t["thesis"], "evidence": t["evidence"], "falsifiers": t["falsifiers"], "members": mem,
                        "heads": [{"title": h["title"], "link": h["link"], "source": h["source"]} for h in t["heads"]]})
        picks_e = {m: [stock_obj(r, None, {"theme": r["theme_name"], "theme_thesis": th_cfg[r["theme"]]["thesis"], "theme_one_liner": th_cfg[r["theme"]].get("one_liner"), "theme_short": th_cfg[r["theme"]].get("short"), "traits": [],
                                       "watch": ["若出現就代表論點不成立：" + x for x in th_cfg[r["theme"]]["falsifiers"]]}, prices, names,
                                 {"theme": r["theme"], "theme_name": r["theme_name"], "theme_short": th_cfg[r["theme"]].get("short"), "also_main": r["also_main"], "relaxed": r["val"]["spread"] > e["max_method_spread"]}) for r in ep[m]] for m in ("TW", "US")}
        snap["emerging"] = {"top_n": e["top_themes"], "themes": eth, "picks": picks_e, "shortage": E.shortage_stats(themes, ep, rows, p), "clues": emg.get("clues") or [], "n_scanned": ctx["scan"]["n_used"]}
    return snap
