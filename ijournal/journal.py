"""把當日分析結果寫成 Markdown 研究日誌，以及可供追蹤的結構化 JSON。"""
from __future__ import annotations
import datetime as dt

from .utils import big, isnum, md_escape, num, pct

MKT = {"TW": "台股", "US": "美股"}
METHOD_ORDER = ["pe", "peg", "dcf", "consensus"]


def pick_record(r: dict) -> dict:
    v, f, pf = r["val"], r["f"], r["pf"]
    stop = pf["last"] - max(min(2.5 * pf["atr14"], 0.18 * pf["last"]), 0.06 * pf["last"])
    return {
        "ticker": r["ticker"], "name": r["name"], "zh_name": r["zh_name"], "market": r["market"], "sector": r["sector"],
        "rank": r.get("pick_rank"), "entry_price": round(pf["last"], 4), "entry_date": pf["date"],
        "composite": round(r["composite"], 2), "scores": {k: (round(x, 1) if isnum(x) else None) for k, x in r["scores"].items()},
        "target": {"base": round(v["base"], 2), "bull": round(v["bull"], 2), "bear": round(v["bear"], 2), "ev": round(v["ev"], 2),
                   "confidence": v["confidence"], "upside": round(v["upside"], 4), "ev_upside": round(v["ev_upside"], 4), "horizon_days": 250,
                   "methods": {k: {"value": round(m["value"], 2), "weight": round(m["weight"], 3), "upside": round(m["upside"], 4)} for k, m in v["methods"].items()}},
        "stop": round(stop, 2),
        "fundamentals": {k: (round(x, 4) if isnum(x) else None) for k, x in f.items()
                         if k in ("market_cap", "pe_fwd", "pe_ttm", "peg", "roe", "gross_margin", "op_margin", "net_margin", "rev_growth", "eps_growth", "de", "fcf_margin", "beta", "target_mean", "n_analysts")},
        "tech": {"ret_1m": pf["ret_1m"], "ret_3m": pf["ret_3m"], "rel_3m": pf.get("rel_3m"), "dist200": pf.get("dist200"), "vol_ann": pf["vol_ann"]},
    }


def candidate_record(r: dict, picked: set, focus: set) -> dict:
    v = r["val"]
    return {"ticker": r["ticker"], "market": r["market"], "sector": r["sector"], "price": round(r["price"], 4), "composite": round(r["composite"], 2) if r["composite"] is not None else None,
            "scores": {k: (round(x, 1) if isnum(x) else None) for k, x in r["scores"].items()},
            "method_upside": {k: round(m["upside"], 4) for k, m in v["methods"].items()} if v else {},
            "upside": round(v["upside"], 4) if v else None, "picked": r["ticker"] in picked, "in_focus": r["sector"] in focus.get(r["market"], set())}


def _head(h: dict) -> dict:
    return {"title": h["title"], "link": h.get("link", ""), "source": h["source"]}


def analyze(r: dict, sec: dict | None, ctx: dict, sec_heads: list | None = None) -> dict:
    """由資料推導『為什麼推薦』與風險。風險分兩組，避免混淆：
      now   已出現：目前的財務/價格/新聞資料已經顯示的狀況
      watch 待觀察：尚未發生、只是可能發生的事件（產業風險；前瞻專區則是『論點證偽條件』）
    ctx: {"traits": [...], "watch": [...], "theme": 趨勢名稱或 None, "theme_thesis": str}"""
    f, pf, v, sc, m = r["f"], r["pf"], r["val"], r["scores"], MKT[r["market"]]
    why: list[dict] = []
    short: list[str] = []

    def add(text: str, tag: str, link: dict | None = None):
        why.append({"t": text, "tag": tag, **({"news": link} if link else {})})

    if ctx.get("theme"):
        add(f"屬於「{ctx['theme']}」趨勢：{ctx.get('theme_one_liner') or ctx.get('theme_thesis', '')}", "趨勢")
        short.append(ctx["theme"])
    if sec and sec["rank"] <= 5:
        tags = "、".join(sec["tags"]) if sec["tags"] else "熱門"
        senti = "偏正面" if sec["news"]["sent"] > 0.1 else "偏負面" if sec["news"]["sent"] < -0.1 else "中性"
        add(f"產業熱度：「{sec['name']}」為{m}今日第 {sec['rank']} 名產業（{tags}），近期相關新聞 {sec['news']['n']} 則、情緒{senti}", "產業")
        short.append(f"{sec['name']}產業熱度第{sec['rank']}")
    if r["n_in_sector"] >= 3 and r["rank_in_sector"] <= 2:
        add(f"龍頭地位：同產業{m}標的中市值第 {r['rank_in_sector']}／{r['n_in_sector']}（{big(f.get('market_cap'))} {f.get('currency') or ''}）", "龍頭")
    rg, eg = f.get("rev_growth"), f.get("eps_growth")
    if isnum(rg) and rg >= 0.10:
        add(f"成長：營收年增 {pct(rg, 1, True)}" + (f"、獲利年增 {pct(eg, 1, True)}" if isnum(eg) else ""), "成長")
        short.append(f"營收年增{pct(rg, 0, True)}")
    roe, om = f.get("roe"), f.get("op_margin")
    if (isnum(roe) and roe >= 0.15) or (isnum(om) and om >= 0.20):
        add(f"獲利體質：ROE {pct(roe)}、營業利益率 {pct(om)}、品質分 {num(sc['quality'], 0)}", "品質")
        if len(short) < 2 and isnum(roe) and roe >= 0.15:
            short.append(f"ROE {pct(roe, 0)}")
    cons = f.get("target_mean")
    val = f"估值：模型 base 目標價 {num(v['base'])}（上檔 {pct(v['upside'], 1, True)}）"
    if isnum(cons) and cons > pf["last"]:
        val += f"；券商共識目標價 {num(cons)}（{pct(cons / pf['last'] - 1, 1, True)}）"
    if isnum(f.get("pe_fwd")) and isnum(v.get("peer_pe")) and f["pe_fwd"] < v["peer_pe"] * 0.9:
        val += f"；預估本益比 {f['pe_fwd']:.1f} 低於同業中位數 {v['peer_pe']:.1f}"
    add(val, "估值")
    if isnum(pf.get("rel_3m")) and pf["rel_3m"] > 0.05:
        add(f"動能：近 3 個月股價強於大盤 {pct(pf['rel_3m'], 1, True)}", "動能")
    elif isnum(pf.get("rel_6m")) and pf["rel_6m"] < -0.2 and v["upside"] > 0.1:
        add(f"股價相對低檔：近 6 個月落後大盤 {pct(abs(pf['rel_6m']), 0)}、距 52 週高 {pct(pf['from_high'], 0)}，尚未被市場追價（也可能代表基本面有疑慮，見風險）", "低檔")
    pos = [h for h in ((r.get("news") or {}).get("heads") or []) if h.get("sent", 0) >= 0.3]
    if pos:
        add(f"近期利多報導：{pos[0]['title']}（關鍵字判斷）", "新聞", _head(pos[0]))
    short.append(f"目標價上檔 {pct(v['upside'], 0, True)}")
    headline = "、".join(short[:3])

    now: list[dict] = []

    def risk(text: str, news: list | None = None):
        now.append({"t": text, **({"news": [_head(h) for h in news]} if news else {})})

    if isnum(f.get("pe_fwd")) and f["pe_fwd"] > 35:
        risk(f"預估本益比 {f['pe_fwd']:.0f} 倍偏高：價格已反映高成長預期，若成長不如預期，評價可能壓縮")
    if isnum(f.get("de")) and f["de"] > 1.5:
        risk(f"負債權益比 {f['de']:.1f} 偏高，財務槓桿大")
    if isnum(f.get("beta")) and f["beta"] > 1.5:
        risk(f"Beta {f['beta']:.1f}：大盤回檔時此股波動會放大（特性，不是事件）")
    if pf.get("dist200") is not None and pf["dist200"] > 0.35:
        risk(f"股價高於 200 日線 {pf['dist200'] * 100:.0f}%，短線追高風險")
    if isnum(rg) and rg < 0:
        risk(f"營收年減 {pct(abs(rg), 1)}，成長動能轉弱")
    if isnum(pf.get("rel_3m")) and pf["rel_3m"] < -0.15:
        risk(f"近 3 個月落後大盤 {pct(abs(pf['rel_3m']), 0)}：市場目前並不買單，可能已在反映疑慮")
    if v["capped"]:
        risk(f"模型原始上檔空間 {v['raw_blend'] / r['price'] - 1:.0%}，已套用上檔上限；估值可能過度樂觀，請與券商共識交叉比對")
    if v["confidence"] == "低":
        risk(f"各估值方法差距大（分歧度 {v['spread']:.0%}），目標價可信度偏低")
    elif v["n_methods"] < 3:
        risk("可用估值方法少於 3 種，目標價可信度較低")
    neg = [h for h in ((r.get("news") or {}).get("heads") or []) if h.get("sent", 0) <= -0.3]
    neg += [h for h in (sec_heads or []) if h.get("sent", 0) <= -0.3 and h["title"] not in {x["title"] for x in neg}]
    if neg:
        risk(f"近期已有 {len(neg[:3])} 則偏負面報導（以關鍵字判斷，可能誤判，請點開確認內容）", neg[:3])
    for x in ctx.get("traits") or []:
        risk(f"產業特性：{x}")

    watch = [{"t": x} for x in ctx.get("watch") or []]
    for drop in ("低檔", "動能", "龍頭"):  # 理由最多 5 條：超過時先捨棄次要項目，確保估值與成長一定保留
        if len(why) > 5:
            why = [w for w in why if w["tag"] != drop]
    return {"why": why[:5], "headline": headline, "risks_now": now, "risks_watch": watch}


def thesis(r: dict, sec: dict | None, ctx: dict) -> tuple[str, list[str]]:
    """研究摘要（完整段落，給日誌與側欄『研究摘要』）；風險另由 analyze() 產生。"""
    f, pf, v, sc = r["f"], r["pf"], r["val"], r["scores"]
    parts = []
    if sec:
        tags = "、".join(sec["tags"]) if sec["tags"] else "一般關注"
        parts.append(f"**產業面**：所屬「{sec['name']}」在{MKT[r['market']]}產業評分排名第 {sec['rank']}（{tags}），"
                     f"近期相關新聞 {sec['news']['n']} 則、情緒{'偏正面' if sec['news']['sent'] > 0.1 else '偏負面' if sec['news']['sent'] < -0.1 else '中性'}。")
    parts.append(f"**龍頭地位**：同產業{MKT[r['market']]}標的中市值排名第 {r['rank_in_sector']}／{r['n_in_sector']}（市值 {big(f.get('market_cap'))} {f.get('currency') or ''}）。")
    q = [f"ROE {pct(f.get('roe'))}", f"營業利益率 {pct(f.get('op_margin'))}", f"毛利率 {pct(f.get('gross_margin'))}", f"營收年增 {pct(f.get('rev_growth'), 1, True)}"]
    parts.append(f"**財務體質**（品質分 {num(sc['quality'], 0)}）：" + "、".join(q) + (f"、負債權益比 {num(f.get('de'))}" if isnum(f.get("de")) else "") + "。")
    parts.append(f"**股價結構**：近 3 個月相對大盤 {pct(pf.get('rel_3m'), 1, True)}；距 200 日均線 {pct(pf.get('dist200'), 1, True)}；距 52 週高 {pct(pf['from_high'], 1, True)}。")
    parts.append(f"**估值**：預估本益比 {num(f.get('pe_fwd'), 1)}、同業中位數 {num(v['peer_pe'], 1)}；base 目標價 {num(v['base'])}，隱含 {pct(v['upside'], 1, True)}。")
    a = analyze(r, sec, ctx)
    return "\n\n".join(parts), [x["t"] for x in a["risks_now"]] + [x["t"] for x in a["risks_watch"]]


def write_journal(date: str, ctx: dict) -> str:
    p, uni, sec_scores, picks, rows = ctx["params"], ctx["uni"], ctx["sec_scores"], ctx["picks"], ctx["rows"]
    secs = {s["id"]: s for s in uni["sectors"]}
    L: list[str] = []
    A = L.append
    A(f"# 投資研究日誌 {date}\n")
    A(f"> 資料基準日：{date}｜系統參數版本：v{p['version']}｜資料來源：{ctx['provider']}｜{'已加入 AI 敘事評論' if ctx.get('llm') else '純量化模式（未啟用 LLM 評論）'}\n")
    if ctx["provider"] == "demo":
        A("> ⚠️ **DEMO 合成資料，非真實行情，僅供驗證系統流程。**\n")
    A("> 本日誌由系統自動產生，僅供研究與教育用途，**不構成投資建議**。目標價為模型估算，必有誤差；系統會定期回頭檢驗並公開檢討。\n")

    # 0 摘要
    A("## 一、今日結論\n")
    if ctx.get("llm") and ctx["llm"].get("summary"):
        A(f"**AI 摘要**：{ctx['llm']['summary']}\n")
    A("| 市場 | # | 代號 | 名稱 | 產業 | 現價 | base 目標價 | 上檔 | 情境 bear / bull | 綜合分 |")
    A("|---|---|---|---|---|---|---|---|---|---|")
    for m in ("TW", "US"):
        for r in picks[m]:
            v = r["val"]
            A(f"| {MKT[m]} | {r['pick_rank']} | {r['ticker']} | {md_escape(r['zh_name'])} | {md_escape(secs[r['sector']]['name'])} | {num(r['price'])} | **{num(v['base'])}** | {pct(v['upside'], 1, True)} | {num(v['bear'])} / {num(v['bull'])} | {num(r['composite'], 1)} |")
    A("")
    N = p["selection"]["picks_per_market"]
    for m in ("TW", "US"):
        k, st = len(picks[m]), ctx["stats"][m]
        if k >= N:
            A(f"- **{MKT[m]}**：{k}／{N} 檔。")
        else:
            why = "、".join(f"{w} {c}" for w, c in st["reasons"].items()) or "—"
            A(f"- **{MKT[m]}**：**{k}／{N} 檔**——合格標的不足，不為湊數放寬門檻、也不改從其他產業補位。重點產業共 {st['candidates']} 檔候選，未入選原因：{why}。")
    A("")

    # 1 大盤
    A("## 二、市場概況\n")
    A("| 指標 | 收盤 | 1日 | 1個月 | 3個月 |")
    A("|---|---|---|---|---|")
    for ix in ctx["indices"]:
        A(f"| {ix['name']} | {num(ix['last'])} | {pct(ix['ret_1d'], 2, True)} | {pct(ix['ret_1m'], 1, True)} | {pct(ix['ret_3m'], 1, True)} |")
    A("")

    # 2 新聞
    A("## 三、新聞掃描\n")
    fl = ctx["fetch_log"]
    okn = sum(1 for x in fl if x["ok"])
    A(f"掃描 {okn}/{len(fl)} 個新聞來源，共蒐集 {ctx['scan']['n_items']} 則不重複報導，其中 {ctx['scan']['n_used']} 則在回溯時間窗內並納入計分。")
    bad = [x for x in fl if not x["ok"]]
    if bad:
        A("\n擷取失敗的來源：" + "、".join(f"{x['source']}" for x in bad) + "（不影響其他來源）。")
    A("")

    # 3 產業
    A("## 四、產業評分與重點產業\n")
    A("產業分數 = 新聞熱度×{:.0%} + 價格動能×{:.0%} + 基本面成長×{:.0%}（皆為跨產業百分位排名）。標示 ★ 者為今日重點產業。\n".format(
        p["sector_weights"]["news"], p["sector_weights"]["momentum"], p["sector_weights"]["fundamental"]))
    for m in ("TW", "US"):
        A(f"### {MKT[m]}\n")
        A("| 排名 | 產業 | 總分 | 新聞熱度 | 動能 | 成長 | 近1月 | 新聞數 | 標籤 |")
        A("|---|---|---|---|---|---|---|---|---|")
        order = sorted(sec_scores[m], key=lambda k: sec_scores[m][k]["rank"])
        for k in order:
            s = sec_scores[m][k]
            star = "★ " if s["rank"] <= p["selection"]["focus_sectors_per_market"] else ""
            A(f"| {s['rank']} | {star}{s['name']} | {num(s['score'], 0)} | {num(s['heat_rank_score'], 0)} | {num(s['mom_rank_score'], 0)} | {num(s['fund_rank_score'], 0)} | {pct(s['ret_1m'], 1, True)} | {s['news']['n']} | {'、'.join(s['tags'])} |")
        A("")
        A("**重點產業的新聞證據**\n")
        for k in order[: p["selection"]["focus_sectors_per_market"]]:
            heads = ctx["scan"]["sector"][k][m]["heads"][:3] or ctx["scan"]["sector"][k]["US" if m == "TW" else "TW"]["heads"][:2]
            A(f"- **{sec_scores[m][k]['name']}**")
            for h in heads:
                A(f"  - [{md_escape(h['title'])}]({h['link']})（{md_escape(h['source'])}）" if h["link"] else f"  - {md_escape(h['title'])}（{md_escape(h['source'])}）")
        A("")

    # 4 龍頭與績優
    A("## 五、龍頭股與財務績優股篩選\n")
    A(f"篩選門檻：市值（美股 ≥ {big(p['selection']['min_market_cap']['US'])}、台股 ≥ {big(p['selection']['min_market_cap']['TW'])}）、近 20 日平均成交值、財務品質分 ≥ {p['selection']['min_quality_score']}、"
      f"（產業市值龍頭 或 品質分 ≥ {p['selection']['leader_or_quality']['quality_score']}）、目標價上檔 ≥ {p['selection']['min_upside']:.0%}；同產業最多 {p['selection']['max_per_sector']} 檔。\n")
    A("綜合分 = " + " + ".join(f"{k}×{w:.2f}" for k, w in p["factor_weights"].items()) + "（因子分數皆 0–100）。\n")
    for m in ("TW", "US"):
        focus = sorted(sec_scores[m], key=lambda k: sec_scores[m][k]["rank"])[: p["selection"]["focus_sectors_per_market"]]
        A(f"### {MKT[m]}：重點產業候選標的\n")
        A("| 產業 | 代號 | 名稱 | 產業市值排名 | 品質 | 成長 | 估值 | 動能 | 綜合 | 結果 |")
        A("|---|---|---|---|---|---|---|---|---|---|")
        picked = {r["ticker"] for r in picks[m]}
        for k in focus:
            cands = sorted((r for r in rows.values() if r["market"] == m and r["sector"] == k and r["composite"] is not None), key=lambda r: -r["composite"])
            for r in cands:
                ok, why = ctx["elig"](r)
                res = "✅ 入選" if r["ticker"] in picked else ("未入選（名額/分散）" if ok else f"✗ {why}")
                sc = r["scores"]
                A(f"| {md_escape(secs[k]['name'])} | {r['ticker']} | {md_escape(r['zh_name'])} | {r['rank_in_sector']}/{r['n_in_sector']} | {num(sc['quality'], 0)} | {num(sc['growth'], 0)} | {num(sc['valuation'], 0)} | {num(sc['momentum'], 0)} | {num(r['composite'], 1)} | {res} |")
        A("")

    # 5 個股
    A("## 六、入選個股研究筆記\n")
    for m in ("TW", "US"):
        for r in picks[m]:
            v, f, pf = r["val"], r["f"], r["pf"]
            sec = sec_scores[m].get(r["sector"])
            rctx = {"traits": secs[r["sector"]].get("risk_traits"), "watch": secs[r["sector"]].get("risk_watch")}
            an = analyze(r, sec, rctx, ctx["scan"]["sector"][r["sector"]][m]["heads"])
            th, _ = thesis(r, sec, rctx)
            A(f"### {MKT[m]} #{r['pick_rank']}　{r['ticker']} {r['zh_name']}\n")
            A(f"**一句話**：{an['headline']}\n")
            A("**為什麼推薦（利多）**\n")
            for w in an["why"]:
                A(f"- 〔{w['tag']}〕{w['t']}")
            A("")
            if ctx.get("llm") and ctx["llm"].get("comments", {}).get(r["ticker"]):
                A(f"> AI 評論：{ctx['llm']['comments'][r['ticker']]}\n")
            A("<details><summary>研究摘要（完整段落）</summary>\n\n" + th + "\n\n</details>\n")
            A("**目標價估算**（12 個月）\n")
            A("| 方法 | 估值 | 權重 | 隱含報酬 | 計算說明 |")
            A("|---|---|---|---|---|")
            for k in METHOD_ORDER:
                if k in v["methods"]:
                    mm = v["methods"][k]
                    A(f"| {mm['label']} | {num(mm['value'])} | {mm['weight']:.0%} | {pct(mm['upside'], 1, True)} | {md_escape(mm['detail'])} |")
            pr = p["scenario_prob"]
            if v["dropped"]:
                A("\n> 已剔除離群估值（偏離現價逾 3 倍或低於 0.35 倍）：" + "、".join(m["label"] for m in v["dropped"].values()))
            A(f"\n估值信心：**{v['confidence']}**（可用方法 {v['n_methods']} 種，方法間分歧度 {v['spread']:.0%}）。")
            A(f"\n加權後 {num(v['blend'])}，經 target_shrink={p['target_shrink']:.2f} 調整得 **base 目標價 {num(v['base'])}（{pct(v['upside'], 1, True)}）**。\n")
            A(f"- 情境：bull {num(v['bull'])}（{pct(v['bull'] / r['price'] - 1, 0, True)}，機率 {pr['bull']:.0%}）／ base {num(v['base'])}（{pr['base']:.0%}）／ bear {num(v['bear'])}（{pct(v['bear'] / r['price'] - 1, 0, True)}，{pr['bear']:.0%}）→ 機率加權期望值 {num(v['ev'])}（{pct(v['ev_upside'], 1, True)}）")
            A(f"- 風險報酬比（上檔／下檔）：{num(v['rr'], 2) if v['rr'] else '—'}；參考停損價 {num(pr_stop(pf))}（2.5×ATR，限制在 6%–18%）")
            A(f"- 券商共識目標價：{num(f.get('target_mean'))}（{int(f['n_analysts']) if f.get('n_analysts') else '—'} 位）")
            A("\n**已出現的風險**（目前的資料已經顯示，不是未來的假設）\n")
            for x in an["risks_now"] or [{"t": "目前資料未顯示明顯警訊。"}]:
                A(f"- {x['t']}")
            A("\n**未來需留意（尚未發生）**——下列是可能發生的事件，目前並沒有發生，列出供追蹤\n")
            for x in an["risks_watch"]:
                A(f"- {x['t']}")
            nd = r["news"]
            if nd and nd["heads"]:
                A("\n**個股相關新聞**\n")
                for h in nd["heads"][:3]:
                    A(f"- [{md_escape(h['title'])}]({h['link']})（{md_escape(h['source'])}）" if h["link"] else f"- {md_escape(h['title'])}（{md_escape(h['source'])}）")
            A("")

    # 6 變動
    A("## 七、與上一期名單比較\n")
    prev = ctx.get("prev")
    if prev:
        cur = {r["ticker"] for m in picks for r in picks[m]}
        old = {x["ticker"] for x in prev["picks"]}
        A(f"- 上一期：{prev['date']}")
        A(f"- 新進榜：{'、'.join(sorted(cur - old)) or '無'}")
        A(f"- 退出榜：{'、'.join(sorted(old - cur)) or '無'}")
        A(f"- 續留：{'、'.join(sorted(cur & old)) or '無'}")
    else:
        A("- 這是第一期日誌，無前期可比較。")
    A("")

    if ctx.get("emerging_top"):
        A("## 附：前瞻專區\n")
        A("主榜追逐『現在被關注』的產業；另有專區找『證據已具備、但新聞尚未充分報導』的結構性趨勢（見網站「前瞻專區」）。今日趨勢雷達前三：" +
          "、".join(f"{n}（{s}）" for n, s in ctx["emerging_top"]) + "。\n")
    # 7 自我檢視
    A("## 八、系統自我檢視\n")
    perf = ctx.get("perf_summary")
    if perf:
        A("過往推薦的實際績效摘要（完整內容見「績效追蹤」頁）：\n")
        A("| 持有天數 | 已到期檔數 | 勝率（相對大盤） | 平均報酬 | 平均超額報酬 |")
        A("|---|---|---|---|---|")
        for h, s in perf.items():
            if s["n"]:
                A(f"| {h} 個交易日 | {s['n']} | {pct(s['win_alpha'], 0)} | {pct(s['avg_ret'], 1, True)} | {pct(s['avg_alpha'], 1, True)} |")
        A("")
    else:
        A("尚無到期的歷史推薦可供驗證。\n")
    hist = ctx.get("param_history") or []
    if hist:
        last = hist[-1]
        A(f"最近一次參數回饋：v{last['version']}（{last['date']}）— " + "；".join(last.get("changes", [])[:4]) + "\n")
    A("評估流程：每日推薦存檔 → 每週追蹤 5/20/60/120 個交易日的實際報酬與超額報酬 → 每月檢討因子有效性與目標價偏誤 → 在樣本足夠時小步幅調整權重 → 下一期日誌採用新參數。\n")
    return "\n".join(L)


def pr_stop(pf: dict) -> float:
    return pf["last"] - max(min(2.5 * pf["atr14"], 0.18 * pf["last"]), 0.06 * pf["last"])
