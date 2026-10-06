"""前瞻專區：找出『有結構性證據、但新聞尚未充分報導、股價尚未被擠進去』的趨勢與標的。

與主榜的差異：主榜追逐『現在被市場與新聞關注』的產業；這裡反過來，用資料檢驗四個條件：
  1. 證據（evidence）：趨勢本身的結構性證據分級 A/B/C（人工維護於 config/themes.json，附證偽條件）
  2. 低覆蓋（low_coverage）：近 48 小時新聞命中數相對主流產業的中位數越低，越『尚未被報導』
  3. 基本面（fundamental）：成分股營收/獲利已經在成長（避免只有故事）
  4. 未擁擠（not_crowded）：成分股相對大盤的 6 個月漲幅不高，且多數仍在 200 日線之上（沒有破壞性下跌）
『確定』僅指驅動力有可查證的證據，不代表股價必漲，時程與受益者仍不確定；推薦標的獨立追蹤績效。
"""
from __future__ import annotations
from . import llm
from .journal import MKT, pick_record
from .news import scan, unmatched_titles
from .utils import avg, big, isnum, lin, md_escape, median, num, pct, wavg

TIER_DESC = {"A": "A｜法規時程／人口結構／已簽約產能等可查證且不可逆的驅動力",
             "B": "B｜採用率或成本曲線已有明確資料，但規模與時程有不確定",
             "C": "C｜技術可行但商業化未定"}


def score_themes(uni, items, scan_main, sources, rows, p) -> list[dict]:
    e = p["emerging"]
    th_scan = scan(items, {"sectors": uni["themes"], "stocks": {}}, sources)
    main_heat = [sum(scan_main["sector"][s["id"]][m]["heat"] for m in ("US", "TW")) for s in uni["sectors"]]
    base = median(main_heat) or 1.0
    out = []
    for th in uni["themes"]:
        mem = [rows[t] for t in th["members"] if t in rows]
        if len(mem) < e["min_members"]:
            continue
        heat = sum(th_scan["sector"][th["id"]][m]["heat"] for m in ("US", "TW"))
        n = sum(th_scan["sector"][th["id"]][m]["n"] for m in ("US", "TW"))
        ratio = heat / base
        growth = avg([avg([lin(r["f"].get("rev_growth"), -0.05, 0.30), lin(r["f"].get("eps_growth"), -0.10, 0.40)]) for r in mem])
        pos_share = avg([1.0 if (r["f"].get("rev_growth") or 0) > 0 else 0.0 for r in mem])
        fundamental = wavg([(growth, 0.7), ((pos_share * 100) if pos_share is not None else None, 0.3)])
        rel6 = avg([r["pf"].get("rel_6m") for r in mem])
        trend = avg([1.0 if (r["pf"].get("dist200") or 0) > 0 else 0.0 for r in mem])
        crowd = wavg([(lin(rel6, 0.5, -0.1), 0.6), ((trend * 100) if trend is not None else None, 0.4)])
        comp = {"evidence": e["tier_score"][th["tier"]], "low_coverage": 100.0 * (1 - min(1.0, ratio)),
                "fundamental": fundamental, "not_crowded": crowd}
        score = wavg([(comp[k], e["score_weights"][k]) for k in comp])
        if ratio >= 1.0:
            status = "已被廣泛報導（不算冷門）"
        elif ratio >= 0.5:
            status = "報導中等"
        else:
            status = "尚未被充分報導"
        out.append({"id": th["id"], "name": th["name"], "tier": th["tier"], "thesis": th["thesis"], "evidence": th["evidence"],
                    "falsifiers": th["falsifiers"], "members": [r["ticker"] for r in mem], "score": score, "components": comp,
                    "coverage_ratio": ratio, "n_headlines": n, "status": status, "rel_6m": rel6, "above200": trend,
                    "heads": sorted([h for m in ("US", "TW") for h in th_scan["sector"][th["id"]][m]["heads"]], key=lambda h: -h["score"])[:3]})
    out.sort(key=lambda x: -(x["score"] or 0))
    for i, t in enumerate(out, 1):
        t["rank"] = i
    return out


def eligible(r: dict, p: dict) -> tuple[bool, str]:
    e, m = p["emerging"], r["market"]
    if r["val"] is None:
        return False, "資料不足，無法估值"
    if r["val"]["spread"] > e["max_method_spread"]:
        return False, "估值方法分歧過大"
    if (r["f"].get("market_cap") or 0) < e["min_market_cap"][m]:
        return False, "市值過小"
    if r["pf"]["turnover"] < e["min_avg_turnover"][m]:
        return False, "流動性不足"
    if r["val"]["upside"] < e["min_upside"]:
        return False, "目標價低於現價"
    q = r["scores"]["quality"]
    if q is None or q < e["min_quality_score"]:
        return False, "財務品質不足"
    if (r["f"].get("eps_ttm_px") or 0) <= 0 and (r["f"].get("eps_fwd_px") or 0) <= 0:
        return False, "獲利為負"
    return True, ""


def pick_emerging(themes: list[dict], rows: dict, main_picked: set, p: dict) -> dict:
    e = p["emerging"]
    picked = {"TW": [], "US": []}
    chosen = set()
    for t in themes[: e["top_themes"]]:
        cnt = {"TW": 0, "US": 0}
        cands = sorted((rows[x] for x in t["members"] if x in rows and rows[x]["composite"] is not None), key=lambda r: -r["composite"])
        for r in cands:
            m = r["market"]
            if len(picked[m]) >= e["picks_per_market"] or cnt[m] >= e["max_per_theme"]:
                continue
            if r["ticker"] in chosen or r.get("dual_of") in chosen or not eligible(r, p)[0]:
                continue
            r = dict(r, theme=t["id"], theme_name=t["name"], also_main=r["ticker"] in main_picked)
            picked[m].append(r)
            cnt[m] += 1
            chosen.add(r["ticker"])
    for m in picked:
        for i, r in enumerate(picked[m], 1):
            r["pick_rank"] = i
    return picked


def record(picked: dict, themes: list[dict], date: str, p: dict, clues=None) -> dict:
    recs = []
    for m in ("TW", "US"):
        for r in picked[m]:
            d = pick_record(r)
            d.update(theme=r["theme"], book="emerging", also_main=r["also_main"])
            recs.append(d)
    return {"date": date, "params_version": p["version"],
            "themes": [{k: (round(v, 3) if isinstance(v, float) else v) for k, v in t.items() if k in ("id", "name", "tier", "rank", "score", "coverage_ratio", "status", "n_headlines")}
                       | {"components": {k: (round(x, 1) if isnum(x) else None) for k, x in t["components"].items()}} for t in themes],
            "picks": recs, "clues": clues or []}


def discover(items, uni) -> list[dict] | None:
    titles = unmatched_titles(items, uni["sectors"] + uni["themes"])
    return llm.discover(titles) if titles else None


def write_report(date: str, themes: list[dict], picked: dict, rows: dict, p: dict, clues, provider: str, n_scanned: int = 0) -> str:
    e = p["emerging"]
    L, A = [], None
    A = L.append
    A(f"# 前瞻專區 {date}\n")
    A(f"> 資料基準日：{date}｜系統參數版本：v{p['version']}｜資料來源：{provider}\n")
    if provider == "demo":
        A("> ⚠️ **DEMO 合成資料，非真實行情。**\n")
    A("> **這個專區在找什麼**：驅動力已有可查證證據、但**新聞尚未充分報導**、且**股價尚未被擠進去**的結構性趨勢。"
      "「確定」只指驅動力（人口、法規時程、物理/成本限制、已簽約產能）有證據，**不代表股價必漲**——時間點與誰受益仍不確定，每個趨勢都列出『什麼情況下論點不成立』。"
      "趨勢清單為人工維護的研究假設（`config/themes.json`），使用前請自行查證原始來源。僅供研究，不構成投資建議。\n")
    A("## 一、趨勢雷達\n")
    A("綜合分 = " + " + ".join(f"{k}×{w:.2f}" for k, w in e["score_weights"].items()) + "。『新聞覆蓋度』= 該趨勢近 48 小時新聞命中數 ÷ 主流產業的中位數（越低越冷門）。覆蓋度 0 只代表在本次掃描的 {n} 則標題中沒有命中，**不等於全世界都沒報導**（也可能是關鍵字過窄或來源未涵蓋），因此『低覆蓋』只當作其中一項訊號，不單獨作為結論。\n".replace("{n}", str(n_scanned)))
    A("| 排名 | 趨勢 | 證據級 | 綜合分 | 證據 | 低覆蓋 | 基本面 | 未擁擠 | 覆蓋度 | 狀態 |")
    A("|---|---|---|---|---|---|---|---|---|---|")
    for t in themes:
        c = t["components"]
        star = "★ " if t["rank"] <= e["top_themes"] else ""
        A(f"| {t['rank']} | {star}{md_escape(t['name'])} | {t['tier']} | {num(t['score'], 0)} | {num(c['evidence'], 0)} | {num(c['low_coverage'], 0)} | {num(c['fundamental'], 0)} | {num(c['not_crowded'], 0)} | {t['coverage_ratio']:.2f}（{t['n_headlines']} 則） | {t['status']} |")
    A("\n證據級：" + "；".join(TIER_DESC[k] for k in ("A", "B", "C")) + "。\n")

    A("## 二、推薦標的\n")
    A("門檻比主榜寬（市值與流動性較低，容許較早期的公司），但仍要求：品質分 ≥ {q}、有可估值的目標價、各估值方法分歧度 ≤ {s:.0%}、獲利為正。同一趨勢最多 {m} 檔。\n".format(
        q=e["min_quality_score"], s=e["max_method_spread"], m=e["max_per_theme"]))
    n = sum(len(v) for v in picked.values())
    if not n:
        A("今日沒有標的同時通過所有條件（寧缺勿濫）。\n")
    else:
        A("| 市場 | # | 代號 | 名稱 | 趨勢 | 現價 | base 目標價 | 上檔 | bear / bull | 備註 |")
        A("|---|---|---|---|---|---|---|---|---|---|")
        for m in ("TW", "US"):
            for r in picked[m]:
                v = r["val"]
                A(f"| {MKT[m]} | {r['pick_rank']} | {r['ticker']} | {md_escape(r['zh_name'])} | {md_escape(r['theme_name'])} | {num(r['price'])} | **{num(v['base'])}** | {pct(v['upside'], 1, True)} | {num(v['bear'])} / {num(v['bull'])} | {'亦入選主榜' if r['also_main'] else ''}{'・' if r['also_main'] and v['capped'] else ''}{'已套用上檔上限' if v['capped'] else ''} |")
        A("")
        A("**推薦理由與估值**\n")
        for m in ("TW", "US"):
            for r in picked[m]:
                v, f, pf = r["val"], r["f"], r["pf"]
                meth = "、".join(f"{x['label']} {num(x['value'])}（{x['weight']:.0%}）" for x in v["methods"].values())
                A(f"- **{r['ticker']} {md_escape(r['zh_name'])}**（{md_escape(r['theme_name'])}）：品質分 {num(r['scores']['quality'], 0)}、ROE {pct(f.get('roe'))}、營收年增 {pct(f.get('rev_growth'), 1, True)}、"
                  f"近 6 個月相對大盤 {pct(pf.get('rel_6m'), 1, True)}、距 52 週高 {pct(pf['from_high'], 1, True)}；目標價依據：{meth}；估值信心 {v['confidence']}。")
        A("")

    A("## 三、各趨勢檢視\n")
    for t in themes[: e["top_themes"]]:
        A(f"### {t['rank']}. {t['name']}（證據級 {t['tier']}）\n")
        A(f"**論點**：{t['thesis']}\n")
        A("**證據（研究假設，請自行查證）**\n")
        for x in t["evidence"]:
            A(f"- {x}")
        A("\n**論點不成立的條件（證偽）**\n")
        for x in t["falsifiers"]:
            A(f"- {x}")
        A(f"\n**市場狀態**：近 48 小時相關新聞 {t['n_headlines']} 則（覆蓋度 {t['coverage_ratio']:.2f}，{t['status']}）；成分股近 6 個月平均相對大盤 {pct(t['rel_6m'], 1, True)}，{pct(t['above200'], 0)} 的成分股在 200 日線之上。\n")
        if t["heads"]:
            A("相關報導：")
            for h in t["heads"]:
                A(f"- [{md_escape(h['title'])}]({h['link']})（{md_escape(h['source'])}）" if h["link"] else f"- {md_escape(h['title'])}（{md_escape(h['source'])}）")
            A("")
        A("| 代號 | 名稱 | 市值 | 營收年增 | 品質 | 6M 相對 | 目標價上檔 | 結果 |")
        A("|---|---|---|---|---|---|---|---|")
        got = {r["ticker"] for m in picked for r in picked[m]}
        for tk in t["members"]:
            r = rows.get(tk)
            if not r:
                continue
            ok, why = eligible(r, p)
            res = "✅ 入選" if tk in got else (f"✗ {why}" if not ok else "未入選（名額）")
            A(f"| {tk} | {md_escape(r['zh_name'])} | {big(r['f'].get('market_cap'))} | {pct(r['f'].get('rev_growth'), 1, True)} | {num(r['scores']['quality'], 0)} | {pct(r['pf'].get('rel_6m'), 1, True)} | {pct(r['val']['upside'], 1, True) if r['val'] else '—'} | {res} |")
        A("")

    if clues:
        A("## 四、AI 觀察到的新興線索（未經驗證）\n")
        A("下列是 LLM 從『沒有命中任何已知產業/趨勢關鍵字』的近期標題中歸納的候選主題，**僅是線索，尚未查證，也不在推薦標的中**；若經你查證值得追蹤，可加入 `config/themes.json`。\n")
        for c in clues:
            A(f"- **{md_escape(c.get('topic', ''))}**：{md_escape(c.get('why', ''))}")
        A("")
    A("## 五、限制\n")
    A("- 趨勢清單由人工維護，可能遺漏更早期的題材；『新聞覆蓋度』只反映本系統的約 20 個來源。\n- 冷門不等於會漲：趨勢成真但股價提前反映、或市場長期忽視，都是常見結果。證據級與證偽條件要定期回頭檢視。\n- 推薦標的的績效獨立追蹤（績效頁『前瞻專區』），若長期沒有超額報酬，應下修或停用此專區。")
    return "\n".join(L)
