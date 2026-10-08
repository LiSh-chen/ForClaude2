"""便宜好貨專區：股價在低基期、財務仍然健康、且有獲利轉強（爆發）條件的標的。

與主榜／前瞻專區的差異：主榜找『現在被關注』、前瞻找『趨勢冷門』；這裡找『價格已經被打到低檔，但公司沒有壞掉』。
便宜不等於好貨——股價低常常是因為基本面在惡化（價值陷阱）。因此必須同時通過四道關卡：
  1. 低基期：距 52 週高點至少回檔 20%，且位於 52 週區間的下 40%
  2. 便宜：目標價上檔 ≥ 20%，各估值方法方向一致（分歧不過大）
  3. 好貨：財務品質分數達標、獲利為正、營收沒有明顯衰退（排除價值陷阱）
  4. 有轉機：成長分數達標，且股價已從近 20 日低點回升、近一個月沒有加速下跌（不接落下的刀）
合格不足就照實少選，不為湊數放寬。推薦標的獨立追蹤績效。
"""
from __future__ import annotations

from .journal import MKT, pick_record
from .utils import big, isnum, lin, md_escape, num, pct, wavg


def _eps_up(f: dict):
    a, b = f.get("eps_fwd_px"), f.get("eps_ttm_px")
    return (a / b - 1) if isnum(a) and isnum(b) and b > 0 else None


def _pe_discount(r: dict):
    pe, peer = r["f"].get("pe_fwd"), (r["val"] or {}).get("peer_pe")
    return (1 - pe / peer) if isnum(pe) and pe > 0 and isnum(peer) and peer > 0 else None


def eligible(r: dict, p: dict) -> tuple[bool, str]:
    b, s, m = p["bargain"], p["selection"], r["market"]
    f, pf, sc, v = r["f"], r["pf"], r["scores"], r["val"]
    if v is None:
        return False, "資料不足，無法估值"
    if (f.get("market_cap") or 0) < s["min_market_cap"][m]:
        return False, "市值過小"
    if pf["turnover"] < s["min_avg_turnover"][m]:
        return False, "流動性不足"
    if pf["from_high"] > -b["min_drawdown"] or pf["pos52"] > b["max_pos52"]:
        return False, "股價不在低基期"
    if v["spread"] > b["max_method_spread"]:
        return False, "各估值方法分歧過大"
    if v["upside"] < b["min_upside"]:
        return False, "目標價上檔空間不足"
    if sc["quality"] is None or sc["quality"] < b["min_quality_score"]:
        return False, "財務品質不足（疑似價值陷阱）"
    if (f.get("eps_ttm_px") or 0) <= 0 and (f.get("eps_fwd_px") or 0) <= 0:
        return False, "獲利為負"
    if isnum(f.get("rev_growth")) and f["rev_growth"] < b["min_rev_growth"]:
        return False, "營收衰退（疑似價值陷阱）"
    if sc["growth"] is None or sc["growth"] < b["min_growth_score"]:
        return False, "成長性不足，缺乏爆發條件"
    if isnum(pf.get("ret_1m")) and pf["ret_1m"] < b["max_ret_1m_drop"]:
        return False, "仍在加速下跌（不接落下的刀）"
    if pf["bounce20"] < b["min_bounce20"]:
        return False, "尚未出現止跌回升跡象"
    return True, ""


def score(r: dict, p: dict) -> float | None:
    w = p["bargain"]["score_weights"]
    pf, f, v = r["pf"], r["f"], r["val"]
    cheap = wavg([(lin(-pf["from_high"], 0.15, 0.50), 0.35), (lin(v["upside"], 0.10, 0.50), 0.45), (lin(_pe_discount(r), 0.0, 0.40), 0.20)])
    explosive = wavg([(r["scores"]["growth"], 0.6), (lin(_eps_up(f), 0.0, 0.50), 0.4)])
    turn = wavg([(lin(pf["bounce20"], 0.03, 0.20), 0.5), (lin(pf.get("ret_1m"), -0.10, 0.10), 0.5)])
    return wavg([(cheap, w["cheap"]), (r["scores"]["quality"], w["quality"]), (explosive, w["explosive"]), (turn, w["turn"])])


def parts(r: dict, p: dict) -> dict:
    """各構面 0–100，供儀表板長條圖。"""
    pf, f, v = r["pf"], r["f"], r["val"]
    return {"cheap": wavg([(lin(-pf["from_high"], 0.15, 0.50), 0.35), (lin(v["upside"], 0.10, 0.50), 0.45), (lin(_pe_discount(r), 0.0, 0.40), 0.20)]),
            "quality": r["scores"]["quality"],
            "explosive": wavg([(r["scores"]["growth"], 0.6), (lin(_eps_up(f), 0.0, 0.50), 0.4)]),
            "turn": wavg([(lin(pf["bounce20"], 0.03, 0.20), 0.5), (lin(pf.get("ret_1m"), -0.10, 0.10), 0.5)])}


def pick_bargain(rows: dict, main_picked: set, p: dict) -> dict:
    b = p["bargain"]
    picked = {"TW": [], "US": []}
    chosen, per_sector = set(), {}
    cands = []
    for r in rows.values():
        if r["composite"] is None or not eligible(r, p)[0]:
            continue
        sc = score(r, p)
        if sc is not None and sc >= b["min_score"]:
            cands.append((sc, r))
    for sc, r in sorted(cands, key=lambda x: -x[0]):
        m = r["market"]
        key = (m, r["sector"])
        if len(picked[m]) >= b["picks_per_market"] or per_sector.get(key, 0) >= b["max_per_sector"]:
            continue
        if r["ticker"] in chosen or r.get("dual_of") in chosen:
            continue
        picked[m].append(dict(r, bargain_score=sc, bargain_parts=parts(r, p), also_main=r["ticker"] in main_picked))
        per_sector[key] = per_sector.get(key, 0) + 1
        chosen.add(r["ticker"])
    for m in picked:
        for i, r in enumerate(picked[m], 1):
            r["pick_rank"] = i
    return picked


def facts(r: dict) -> dict:
    pf, f, v = r["pf"], r["f"], r["val"]
    return {"from_high": pf["from_high"], "pos52": pf["pos52"], "bounce20": pf["bounce20"], "ret_1m": pf.get("ret_1m"), "dist200": pf.get("dist200"),
            "pe_discount": _pe_discount(r), "eps_up": _eps_up(f), "rev_growth": f.get("rev_growth"), "upside": v["upside"]}


def chips(r: dict) -> list[dict]:
    x = facts(r)
    out = [{"t": f"低於52週高 {x['from_high'] * 100:.0f}%"}]
    if isnum(x["pe_discount"]) and x["pe_discount"] >= 0.1:
        out.append({"t": f"本益比低於同業 {x['pe_discount'] * 100:.0f}%"})
    if isnum(x["eps_up"]) and x["eps_up"] >= 0.15:
        out.append({"t": f"預估EPS較近4季 +{x['eps_up'] * 100:.0f}%"})
    elif isnum(x["rev_growth"]) and x["rev_growth"] >= 0.10:
        out.append({"t": f"營收年增 {x['rev_growth'] * 100:.0f}%"})
    return out[:3]


def why(r: dict) -> list[dict]:
    x = facts(r)
    L = [{"tag": "低基期", "t": f"股價距 52 週高點 {x['from_high'] * 100:.0f}%，位於 52 週區間的 {x['pos52'] * 100:.0f}% 位置；目標價上檔 {x['upside'] * 100:+.0f}%。"}]
    if isnum(x["pe_discount"]) and x["pe_discount"] > 0:
        L.append({"tag": "便宜", "t": f"預估本益比比同業中位數低 {x['pe_discount'] * 100:.0f}%。"})
    g = []
    if isnum(x["rev_growth"]):
        g.append(f"營收年增 {x['rev_growth'] * 100:+.0f}%")
    if isnum(x["eps_up"]):
        g.append(f"預估EPS 較近 4 季 {x['eps_up'] * 100:+.0f}%")
    if g:
        L.append({"tag": "爆發條件", "t": "、".join(g) + "：獲利動能向上，股價回到合理水準時彈性較大。"})
    L.append({"tag": "止跌", "t": f"自近 20 日低點回升 {x['bounce20'] * 100:.0f}%，近一個月 {((x['ret_1m'] or 0) * 100):+.0f}%，尚未見到加速下跌。"})
    return L


def risks_now(r: dict) -> list[dict]:
    x, out = facts(r), []
    if isnum(x["dist200"]) and x["dist200"] < 0:
        out.append({"t": f"股價仍在 200 日線之下 {abs(x['dist200']) * 100:.0f}%，長期下跌趨勢尚未扭轉，回升可能只是反彈。", "news": []})
    return out


TRAP = {"t": "價值陷阱：股價低可能是市場正在反映基本面惡化。若下一次財報營收或毛利率轉為衰退、或預估EPS被下修，『便宜』的前提就不成立。", "news": []}


def shortage_stats(rows: dict, picked: dict, p: dict) -> dict:
    out = {m: {"n": len(picked[m]), "N": p["bargain"]["picks_per_market"], "candidates": 0, "reasons": {}, "qualified": 0} for m in ("TW", "US")}
    for r in rows.values():
        if r["composite"] is None:
            continue
        o = out[r["market"]]
        o["candidates"] += 1
        ok, why_ = eligible(r, p)
        if ok:
            o["qualified"] += 1
        else:
            o["reasons"][why_] = o["reasons"].get(why_, 0) + 1
    for o in out.values():
        o["reasons"] = dict(sorted(o["reasons"].items(), key=lambda kv: -kv[1]))
    return out


def record(picked: dict, date: str, p: dict) -> dict:
    recs = []
    for m in ("TW", "US"):
        for r in picked[m]:
            d = pick_record(r)
            d.update(book="bargain", also_main=r["also_main"], bargain_score=round(r["bargain_score"], 1), facts={k: (round(v, 4) if isnum(v) else None) for k, v in facts(r).items()})
            recs.append(d)
    return {"date": date, "params_version": p["version"], "picks": recs}


def write_report(date: str, picked: dict, rows: dict, p: dict, provider: str) -> str:
    b = p["bargain"]
    A = (L := []).append
    A(f"# 便宜好貨專區 {date}\n")
    A(f"> 資料基準日：{date}｜系統參數版本：v{p['version']}｜資料來源：{provider}\n")
    if provider == "demo":
        A("> ⚠️ **DEMO 合成資料，非真實行情。**\n")
    A("> **這個專區在找什麼**：股價已被打到低基期，但公司沒有壞掉、而且獲利有轉強條件的標的。**便宜不等於好貨**——股價低常是因為基本面惡化（價值陷阱），"
      "所以必須同時通過低基期、估值便宜、財務品質、成長與止跌回升四道關卡。**不保證反彈**，僅供研究，不構成投資建議。\n")
    A("## 一、篩選條件\n")
    A(f"- 低基期：距 52 週高點回檔 ≥ {b['min_drawdown']:.0%}，且位於 52 週區間下 {b['max_pos52']:.0%}")
    A(f"- 便宜：目標價上檔 ≥ {b['min_upside']:.0%}，估值方法分歧 ≤ {b['max_method_spread']:.0%}")
    A(f"- 好貨：財務品質分 ≥ {b['min_quality_score']}、獲利為正、營收年增 ≥ {b['min_rev_growth']:.0%}（排除價值陷阱）")
    A(f"- 有轉機：成長分數 ≥ {b['min_growth_score']}、自近 20 日低點回升 ≥ {b['min_bounce20']:.0%}、近一個月跌幅不超過 {abs(b['max_ret_1m_drop']):.0%}")
    A("- 排序：" + " + ".join(f"{k}×{w:.2f}" for k, w in b["score_weights"].items()) + f"（cheap 便宜、quality 品質、explosive 爆發、turn 轉機）；總分 ≥ {b['min_score']} 才入選，同產業最多 {b['max_per_sector']} 檔。\n")
    A("## 二、推薦標的\n")
    st = shortage_stats(rows, picked, p)
    for m in ("TW", "US"):
        o = st[m]
        why_ = "、".join(f"{k} {c}" for k, c in o["reasons"].items())
        if o["n"] >= o["N"]:
            A(f"- **{MKT[m]}**：{o['n']}／{o['N']} 檔（名額已滿；另有 {o['qualified'] - o['n']} 檔合格但未入選）")
        else:
            A(f"- **{MKT[m]}**：**{o['n']}／{o['N']} 檔**——合格標的不足，不為湊數放寬門檻。{o['candidates']} 檔中未通過的原因：{why_ or '—'}。")
    A("")
    if any(picked.values()):
        A("| 市場 | # | 代號 | 名稱 | 現價 | 距52週高 | 目標價 | 上檔 | 總分 | 備註 |")
        A("|---|---|---|---|---|---|---|---|---|---|")
        for m in ("TW", "US"):
            for r in picked[m]:
                v = r["val"]
                A(f"| {MKT[m]} | {r['pick_rank']} | {r['ticker']} | {md_escape(r['zh_name'])} | {num(r['price'])} | {pct(r['pf']['from_high'], 0, True)} | **{num(v['base'])}** | {pct(v['upside'], 1, True)} | {num(r['bargain_score'], 0)} | {'亦入選主榜' if r['also_main'] else ''} |")
        A("")
        A("**為什麼是它（以及要小心什麼）**\n")
        for m in ("TW", "US"):
            for r in picked[m]:
                f = r["f"]
                A(f"- **{r['ticker']} {md_escape(r['zh_name'])}**：" + " ".join(x["t"] for x in why(r)) + f" 市值 {big(f.get('market_cap'))}、ROE {pct(f.get('roe'))}、估值信心 {r['val']['confidence']}。"
                  + ("**已出現的風險**：" + "；".join(x["t"] for x in risks_now(r)) if risks_now(r) else ""))
        A("")
    A("## 三、限制\n")
    A("- 『低基期』用的是價格相對 52 週區間，不是長期估值；產業景氣循環的低點與結構性衰退在價格上看起來很像。\n- 價值陷阱：每檔都要追蹤下一次財報是否支持『獲利轉強』的假設。\n- 推薦標的的績效獨立追蹤（績效頁『便宜好貨』），若長期沒有超額報酬，應調整或停用此專區。")
    return "\n".join(L)
