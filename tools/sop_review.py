"""SOP 檢討報告產生器（人工執行；需要網路取得價格歷史）。
用法：python tools/sop_review.py 2026-11-05
讀取 data/ 內的候選池、候選池前瞻報酬、推薦紀錄與績效，加上 Yahoo 價格歷史（計算波動率等特徵），輸出 reviews/<日期>-sop.md。
目的：不只描述績效，而是檢驗『選股流程的哪個環節沒有預測力』，並記錄調整前的證據（調整本身記在 config/params_history.json）。"""
from __future__ import annotations
import glob
import json
import math
import sys
import warnings
from pathlib import Path

import numpy as np
import pandas as pd

warnings.filterwarnings("ignore")
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from ijournal import config as C  # noqa: E402

date = sys.argv[1] if len(sys.argv) > 1 else str(pd.Timestamp.today().date())
data = ROOT / "data"
pool = json.load(open(data / "pool_returns.json"))
cand = {Path(f).stem: json.load(open(f))["rows"] for f in sorted(glob.glob(str(data / "candidates" / "*.json")))}
perf = json.load(open(data / "performance.json"))["positions"]
import yfinance as yf  # noqa: E402

tick = sorted({r["ticker"] for rs in cand.values() for r in rs})
df = yf.download(tick, period="2y", interval="1d", auto_adjust=True, group_by="ticker", progress=False, threads=True)
px = {}
for t in tick:
    try:
        s = df[t]["Close"].dropna()
        s.index = pd.to_datetime(s.index).tz_localize(None).normalize()
        px[t] = s
    except Exception:  # noqa: BLE001
        pass
rows = []
for d, rs in cand.items():
    D = pd.Timestamp(d)
    for r in rs:
        s, pr = px.get(r["ticker"]), (pool.get(d) or {}).get(r["ticker"])
        if s is None or not pr or pr.get("a5") is None:
            continue
        i = s.index.searchsorted(D, side="right") - 1
        if i < 130:
            continue
        c = s.iloc[: i + 1]
        rows.append({"date": d, "ticker": r["ticker"], "market": r["market"], "comp": r["composite"], "picked": r["picked"], "a5": pr["a5"], "vol": c.pct_change().tail(60).std() * math.sqrt(252),
                     "dd52": c.iloc[-1] / c.tail(252).max() - 1, **{"s_" + k: v for k, v in r["scores"].items()}})
X = pd.DataFrame(rows)
nd = X.date.nunique()


def ic(f):
    out = []
    for _, g in X.groupby("date"):
        g = g[[f, "a5"]].dropna()
        if len(g) >= 15 and g[f].nunique() > 1:
            out.append(g[f].rank().corr(g["a5"].rank()))
    return float(np.mean(out)), float(np.std(out) / np.sqrt(len(out)))


L = []
A = L.append
A(f"# SOP 檢討報告 {date}\n")
A(f"> 範圍：{min(cand)}～{max(cand)}，候選池 {len(X)} 筆觀察（{nd} 個日期）、{len([p for p in perf if p['book'] == 'main'])} 筆主榜推薦。**樣本只涵蓋一段急跌行情**，結論是『目前流程在這種行情下哪裡出問題』，不代表所有行情。\n")
A("## 一、結論\n")
A("- 選股排序（綜合分）與後續 5 日超額報酬**沒有正相關、反而略為負相關**；被選中的標的表現不如候選池平均。")
A("- 最強的訊號是**波動率**：波動越高，後續超額報酬越差；而推薦名單的波動明顯高於候選池。")
A("- 流程缺陷：推薦沒有**停損記憶**（已跌破停損價仍每天重複推薦）、沒有**同題材跨市場上限**（AI 硬體供應鏈占推薦六成）。")
A("- 已據此調整為 SOP v2（調整內容見 `config/params_history.json` 與儀表板『SOP 檢討與調整紀錄』）；因子權重因樣本單一行情、20 日樣本不足而**暫不調整**。\n")
A("## 二、診斷證據\n")
A("### 2-1 各因子對後續 5 日超額報酬的排名相關（日均 Rank IC）\n")
A("| 因子 | IC | 標準誤 |")
A("|---|---|---|")
names = {"comp": "綜合分", "s_quality": "品質", "s_growth": "成長", "s_valuation": "估值", "s_momentum": "動能", "s_news": "新聞", "s_sector": "產業", "s_leader": "龍頭", "vol": "波動率", "dd52": "距52週高"}
for f, nm in names.items():
    m, se = ic(f)
    A(f"| {nm} | {m:+.3f} | {se:.3f} |")
A("\nIC > 0 表示分數越高、後續越好。波動率 IC 為 −0.5：波動越高越差（此行情的主要特徵）。\n")
A("### 2-2 波動率五分位（全候選池）\n")
A("| 五分位 | 平均年化波動 | 後續 5 日平均超額 | 觀察數 |")
A("|---|---|---|---|")
q = pd.qcut(X.vol, 5, labels=False)
for k, g in X.groupby(q):
    tag = "（最低）" if k == 0 else "（最高）" if k == 4 else ""
    A(f"| {k + 1}{tag} | {g.vol.mean():.0%} | {g.a5.mean() * 100:+.1f}% | {len(g)} |")
A("")
pk = X[X.picked]
A(f"被選中的 {len(pk)} 筆：平均超額 {pk.a5.mean() * 100:+.2f}%，平均波動 {pk.vol.mean():.0%}（候選池 {X.vol.mean():.0%}，台股被選中者 {pk[pk.market == 'TW'].vol.mean():.0%}）；候選池平均 {X.a5.mean() * 100:+.2f}%。\n")
A("### 2-3 風險調整排序的效果（前 30% 標的的後續 5 日平均超額）\n")
A("| 排序方式 | 平均超額 | 平均波動 |")
A("|---|---|---|")
X["volr"] = X.groupby("date").vol.rank(pct=True) * 100
for k in (0, 0.25, 0.4, 0.5, 1.0):
    X["adj"] = X.comp - k * X.volr
    cut = X.groupby("date").adj.transform(lambda s: s.quantile(0.7))
    t = X[X.adj >= cut]
    A(f"| 綜合分 − {k} × 波動百分位{'（原排序）' if k == 0 else '（SOP v2 採用）' if k == 0.4 else ''} | {t.a5.mean() * 100:+.2f}% | {t.vol.mean():.0%} |")
A("")
A("### 2-4 推薦流程本身\n")
main = [p for p in perf if p["book"] == "main"]
first = {}
for p in sorted(main, key=lambda p: p["date"]):
    first.setdefault(p["ticker"], p)
rep = [p for p in main if p is not first[p["ticker"]] and p["entry"] < first[p["ticker"]]["stop"]]
ex = [(p["stop"] / p["entry"] - 1) if p["stop_hit"] else p["rlast"] for p in first.values()]
A(f"- {len(main)} 筆推薦只有 {len(first)} 檔不同股票；其中 **{len(rep)} 筆（{len(rep) / len(main):.0%}）是已跌破首次推薦停損價、仍被重複推薦**，平均超額 {np.mean([p['alast'] for p in rep]) * 100:+.1f}%。")
A(f"- 停損觸及率 {sum(p['stop_hit'] for p in main) / len(main):.0%}；若每檔嚴守參考停損，平均報酬 {np.mean(ex) * 100:+.1f}%，不守停損 {np.mean([p['rlast'] for p in first.values()]) * 100:+.1f}%。停損規則本身有效，缺的是『被停損的標的不再推薦』。")
AI = {"ai_chips", "ai_servers", "cooling_power", "networking_optics", "memory_storage", "semi_equipment", "pcb_materials"}
A(f"- AI 硬體供應鏈（{len(AI)} 個產業）占推薦 {np.mean([p['sector'] in AI for p in main]):.0%}，美股與台股的同一題材同時入選。\n")
A("## 三、調整（SOP v2）與回溯檢驗\n")
A("| 規則 | 內容 |")
A("|---|---|")
A("| 波動率上限 | 年化波動 > 70% 不推薦（前瞻專區 75%） |")
A("| 風險調整排序 | 綜合分 − 0.4 × 同市場波動百分位 |")
A("| 停損記憶 | 90 天內曾推薦、之後跌破首次推薦停損價的標的，收復前不再推薦 |")
A("| 同題材上限 | AI 硬體供應鏈每日最多 4 檔（跨市場） |\n")
rows2 = []
for f in sorted(glob.glob(str(data / "picks" / "*.json"))):
    d = json.load(open(f))
    fs = {}
    for p in sorted(d["picks"], key=lambda p: (p["market"] != "TW", p["rank"] or 9)):
        k = (d["date"], p["ticker"])
        pos = next((x for x in main if x["date"] == d["date"] and x["ticker"] == p["ticker"]), None)
        if pos:
            rows2.append({"date": d["date"], "sec": p["sector"], "vol": p["tech"].get("vol_ann"), "a": pos["alast"], "r": pos["rlast"], "t": p["ticker"], "entry": p["entry_price"]})
seen = {}
for r in rows2:
    seen.setdefault(r["t"], None)
f1 = {}
for p in sorted(main, key=lambda p: p["date"]):
    f1.setdefault(p["ticker"], p)
for r in rows2:
    r["rep"] = r["entry"] < f1[r["t"]]["stop"] and r["date"] != f1[r["t"]]["date"]
keep_ai = set()
by = {}
for i, r in enumerate(rows2):
    by.setdefault(r["date"], []).append(i)
for d, idx in by.items():
    n = 0
    for i in idx:
        if rows2[i]["sec"] in AI:
            if n >= 4:
                continue
            n += 1
        keep_ai.add(i)
A("**回溯檢驗（樣本內，只用來看方向；對象是已推薦的 {} 筆）**\n".format(len(rows2)))
A("| 情境 | 保留筆數 | 平均超額（迄今） | 平均報酬 |")
A("|---|---|---|---|")
for lab, keep in (("原本", lambda i, r: True), ("只套用波動上限", lambda i, r: r["vol"] is None or r["vol"] <= 0.7), ("只套用停損記憶", lambda i, r: not r["rep"]), ("只套用題材上限", lambda i, r: i in keep_ai),
                  ("三項全套用", lambda i, r: i in keep_ai and not r["rep"] and (r["vol"] is None or r["vol"] <= 0.7))):
    k = [r for i, r in enumerate(rows2) if keep(i, r)]
    A(f"| {lab} | {len(k)}（{len(k) / len(rows2):.0%}） | {np.mean([r['a'] for r in k]) * 100:+.1f}% | {np.mean([r['r'] for r in k]) * 100:+.1f}% |")
A("\n套用後仍為負：同期大盤本身大幅下跌，且回溯檢驗只涵蓋已被選中的標的（沒有包含新規則下會遞補進來的標的）。這只說明方向，不是對未來的保證。\n")
A("## 四、暫不調整的項目\n")
A("- **因子權重**（成長、品質 IC 為負）：樣本只涵蓋一段急跌行情；20 日 IC 僅有少數日期，按既有流程在 ≥20 個日期後由月度檢討處理。")
A("- **停損幅度**：嚴守停損可把平均損失減半，不調整。\n")
A("## 五、後續監控與回退條件\n")
A("- 每日在候選池記錄舊排序（綜合分）與新排序（風險調整後），週報比較兩者前 30% 的後續 5 日超額；累積 ≥15 個日期後，若新排序落後舊排序超過 1 個百分點，建議回退風險調整排序。")
A("- 合格標的變少是預期結果（照實少選）；某市場連續 10 個交易日 0 檔時，重新檢討波動上限。")
A("- 下一次完整檢討：月度流程（每月第一個週六）。")
out = ROOT / "reviews" / f"{date}-sop.md"
out.write_text("\n".join(L), encoding="utf-8")
print("wrote", out)
