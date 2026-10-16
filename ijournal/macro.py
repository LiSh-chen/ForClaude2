"""總經與市場環境分析：把『市場已經發生的壓力』與『尚未發生、但市場在預期的事件』分開評估，產生風險分數與選股門檻調整。

資料來源（皆為公開來源，沒有付費 API）：
  - Yahoo：美債殖利率、VIX、美元指數、原油、信用債 ETF、聯邦基金利率期貨（隱含市場預期的政策利率路徑）
  - FRED 公開 CSV：CPI／核心 CPI、失業率、非農就業、聯邦基金有效利率與目標區間、2 年期殖利率、通膨預期
  - 新聞標題：近期與總經相關的報導（通膨、聯準會、殖利率、就業、關稅、油價）
風險分數 0–100（越高越需要謹慎），分兩群因子：
  已發生（價格已經反映）：殖利率衝擊、波動率、股市走勢、美元、油價、信用利差
  未來／預期（尚未發生）：升息機率與利率路徑、通膨、就業與景氣
限制：聯邦基金期貨隱含的是『市場定價』（含少量期限溢酬），不是聯準會的承諾；FRED 資料有發布時差。這些都只是風險提示，不是預測。
"""
from __future__ import annotations
import datetime as dt
import re

import pandas as pd

from .utils import avg, isnum, lin, wavg

MONTH_CODE = "FGHJKMNQUVXZ"
LEVELS = [("normal", "正常"), ("watch", "留意"), ("warn", "警戒"), ("high", "高風險")]
LABEL = dict(LEVELS)


# ---------------------------------------------------------------- 資料取得
def ff_tickers(today: dt.date, n: int = 9) -> dict[str, str]:
    """未來 n 個月的 30 天聯邦基金期貨代號：{'2026-11': 'ZQX26.CBT', ...}（價格＝100−該月隱含平均利率）。"""
    out, y, m = {}, today.year, today.month
    for _ in range(n):
        out[f"{y}-{m:02d}"] = f"ZQ{MONTH_CODE[m - 1]}{str(y)[2:]}.CBT"
        m += 1
        if m > 12:
            y, m = y + 1, 1
    return out


def all_tickers(cfg: dict, today: dt.date) -> list[str]:
    return list(cfg["tickers"].values()) + list(ff_tickers(today).values())


def parse_fred_csv(text: str) -> list[tuple[dt.date, float]]:
    out = []
    for line in text.strip().splitlines()[1:]:
        parts = line.strip().split(",")
        if len(parts) < 2 or parts[1] in ("", "."):
            continue
        try:
            out.append((dt.date.fromisoformat(parts[0]), float(parts[1])))
        except ValueError:
            continue
    return out


def fetch_fred(cfg: dict, fetch, today: dt.date) -> tuple[dict, list[str]]:
    """回傳 ({名稱: [(日期, 值)...]}, 錯誤清單)；單一序列失敗不影響其他。"""
    out, errs = {}, []
    start = (today - dt.timedelta(days=520)).isoformat()
    for name, sid in cfg["fred"].items():
        try:
            rows = parse_fred_csv(fetch(f"https://fred.stlouisfed.org/graph/fredgraph.csv?id={sid}&cosd={start}"))
            if rows:
                out[name] = rows
            else:
                errs.append(f"{sid} 無資料")
        except Exception as e:  # noqa: BLE001
            errs.append(f"{sid}：{str(e)[:60]}")
    return out, errs


# ---------------------------------------------------------------- 小工具
def _s(prices: dict, t: str):
    df = prices.get(t)
    if df is None or not len(df):
        return None
    c = df["Close"].dropna()
    return c if len(c) else None


def _last(prices, t):
    c = _s(prices, t)
    return float(c.iloc[-1]) if c is not None else None


def _chg(prices, t, n):
    """n 個交易日的絕對變動。"""
    c = _s(prices, t)
    return float(c.iloc[-1] - c.iloc[-1 - n]) if c is not None and len(c) > n else None


def _ret(prices, t, n):
    c = _s(prices, t)
    return float(c.iloc[-1] / c.iloc[-1 - n] - 1) if c is not None and len(c) > n else None


def _dd(prices, t, n=60):
    """距近 n 日高點的回檔幅度（正數）。"""
    c = _s(prices, t)
    return float(1 - c.iloc[-1] / c.tail(n).max()) if c is not None and len(c) >= 20 else None


def _state(score, p):
    if score is None:
        return None
    return "risk" if score >= p["state_risk"] else "watch" if score >= p["state_watch"] else "ok"


def _series(prices, t, n=60):
    c = _s(prices, t)
    return [round(float(v), 3) for v in c.tail(n)] if c is not None else []


def level_of(score, p) -> str:
    lv = p["levels"]
    return "high" if score >= lv["high"] else "warn" if score >= lv["warn"] else "watch" if score >= lv["watch"] else "normal"


# ---------------------------------------------------------------- 聯準會路徑
def fed_path(prices: dict, fred: dict, cfg: dict, today: dt.date) -> dict:
    """由聯邦基金期貨推算每次 FOMC 後的『市場隱含政策利率』與升／降息 25bp 的機率。
    取『會議月的下一個月』的期貨：該月完全落在會後，隱含平均利率≈會後利率。"""
    effr = fred.get("effr", [])[-1][1] if fred.get("effr") else None
    upper = fred.get("target_upper", [])[-1][1] if fred.get("target_upper") else None
    implied = {}
    for ym, t in ff_tickers(today).items():
        v = _last(prices, t)
        if isnum(v) and 80 < v < 100:
            implied[ym] = 100.0 - v
    if effr is None:
        front = _last(prices, "ZQ=F")
        effr = 100.0 - front if isnum(front) else None
    out = {"effr": effr, "target_upper": upper, "meetings": [], "n_contracts": len(implied)}
    if effr is None:
        return out
    prev = effr
    for ds in cfg["fomc_decisions"]:
        d = dt.date.fromisoformat(ds)
        if d <= today:
            continue
        ny, nm = (d.year + 1, 1) if d.month == 12 else (d.year, d.month + 1)
        after = implied.get(f"{ny}-{nm:02d}")
        if after is None:
            break
        delta = (after - prev) * 100
        out["meetings"].append({"date": ds, "days": (d - today).days, "after": round(after, 3), "delta_bp": round(delta, 1),
                                "prob": round(min(1.0, abs(delta) / 25.0), 2), "dir": "hike" if delta > 3 else "cut" if delta < -3 else "hold",
                                "cum_bp": round((after - effr) * 100, 1)})
        prev = after
        if len(out["meetings"]) >= 5:
            break
    return out


# ---------------------------------------------------------------- 通膨／就業
def _yoy(rows, back=12):
    return rows[-1][1] / rows[-1 - back][1] - 1 if len(rows) > back else None


def _mom(rows, k=1):
    return rows[-1][1] / rows[-1 - k][1] - 1 if len(rows) > k else None


def inflation_labor(fred: dict) -> dict:
    out = {}
    for key, name in (("cpi", "headline"), ("core_cpi", "core")):
        r = fred.get(key)
        if not r:
            continue
        mom = _mom(r)
        out[name] = {"date": str(r[-1][0]), "yoy": _yoy(r), "mom": mom, "prev_mom": (r[-2][1] / r[-3][1] - 1) if len(r) > 2 else None,
                     "ann3m": ((r[-1][1] / r[-4][1]) ** 4 - 1) if len(r) > 3 else None}
    ur = fred.get("unrate")
    if ur:
        vals = [v for _, v in ur[-12:]]
        out["unrate"] = {"date": str(ur[-1][0]), "value": ur[-1][1], "above_12m_low": ur[-1][1] - min(vals)}
    pay = fred.get("payems")
    if pay and len(pay) > 3:
        out["payrolls"] = {"date": str(pay[-1][0]), "avg3m_k": (pay[-1][1] - pay[-4][1]) / 3.0, "last_k": pay[-1][1] - pay[-2][1]}
    if fred.get("breakeven"):
        out["breakeven"] = fred["breakeven"][-1][1]
    if fred.get("us2y"):
        out["us2y"] = fred["us2y"][-1][1]
    return out


# ---------------------------------------------------------------- 因子
def _fmt_bp(x):
    return f"{x * 100:+.0f}bp" if isnum(x) else "—"


def factors(prices: dict, fed: dict, infl: dict, p: dict) -> list[dict]:
    F = []

    def add(fid, label, group, score, value, detail, tw=False):
        F.append({"id": fid, "label": label, "group": group, "score": None if score is None else round(score, 1), "state": _state(score, p),
                  "value": value, "detail": detail, "tw_only": tw})

    y10, c20, c5 = _last(prices, "^TNX"), _chg(prices, "^TNX", 20), _chg(prices, "^TNX", 5)
    add("rate_shock", "殖利率衝擊", "happened", wavg([(lin(c20 * 100 if isnum(c20) else None, 10, 60), 0.5), (lin(y10, 4.0, 5.5), 0.5)]),
        f"10年期 {y10:.2f}%（20日 {_fmt_bp(c20)}）" if isnum(y10) else "無資料",
        "美債殖利率快速上升或處於高檔，會壓縮成長股與高本益比股的評價，也增加企業融資成本。")
    vix, v5 = _last(prices, "^VIX"), _chg(prices, "^VIX", 5)
    add("vol", "波動率（VIX）", "happened", wavg([(lin(vix, 14, 30), 0.6), (lin(v5, 0, 6), 0.4)]),
        f"VIX {vix:.1f}（5日 {v5:+.1f}）" if isnum(vix) and isnum(v5) else "無資料", "VIX 代表市場對未來一個月的避險需求；急升表示恐慌或避險買盤增加。")
    sp_dd, nq5, sox5 = _dd(prices, "^GSPC"), _ret(prices, "^IXIC", 5), _ret(prices, "^SOX", 5)
    add("equity", "美股走勢", "happened", wavg([(lin(sp_dd, 0.02, 0.10), 0.4), (lin(-nq5 if isnum(nq5) else None, 0, 0.05), 0.3), (lin(-sox5 if isnum(sox5) else None, 0, 0.10), 0.3)]),
        f"S&P 距60日高 −{sp_dd * 100:.1f}%；Nasdaq 5日 {nq5 * 100:+.1f}%；費半 5日 {sox5 * 100:+.1f}%" if all(isnum(x) for x in (sp_dd, nq5, sox5)) else "無資料",
        "股市本身已經回檔的幅度；半導體走弱會直接影響本系統的 AI 晶片／伺服器相關持股與台股。")
    dx = _ret(prices, "DX-Y.NYB", 20)
    add("usd", "美元強度", "happened", lin(dx, 0.0, 0.04), f"美元指數 20日 {dx * 100:+.1f}%" if isnum(dx) else "無資料", "美元急升通常伴隨全球流動性收緊，新興市場與台股資金可能流出。")
    oil = _ret(prices, "CL=F", 20)
    add("oil", "油價（通膨壓力）", "happened", lin(oil, 0.0, 0.15), f"原油 20日 {oil * 100:+.1f}%" if isnum(oil) else "無資料", "油價上漲會推升通膨，使聯準會更難降息。")
    hy, lq = _s(prices, "HYG"), _s(prices, "LQD")
    cr = None
    if hy is not None and lq is not None and len(hy) > 21 and len(lq) > 21:
        cr = float((hy / lq).dropna().iloc[-1] / (hy / lq).dropna().iloc[-21] - 1)
    add("credit", "信用利差", "happened", lin(-cr if isnum(cr) else None, 0, 0.03), f"高收益債/投資級債 20日 {cr * 100:+.1f}%" if isnum(cr) else "無資料",
        "高收益債相對投資級債走弱，代表市場對違約與景氣的擔憂升高。")
    # --- 未來／預期
    m = (fed.get("meetings") or [None])[0]
    cum6 = None
    if fed.get("meetings"):
        cum6 = max((x["cum_bp"] for x in fed["meetings"][:4]), default=None)
    hike_prob = (m["prob"] if m and m["dir"] == "hike" else 0.0) if m else None
    nxt = f"{m['date'][5:]}（{m['days']} 天後）" if m else ""
    add("fed", "升息機率與利率路徑", "ahead", wavg([(lin(hike_prob, 0, 0.6), 0.5), (lin(cum6, 0, 50), 0.5)]),
        (f"下次 FOMC {nxt}：{'升息' if m['dir'] == 'hike' else '降息' if m['dir'] == 'cut' else '按兵不動'}機率約 {m['prob'] * 100:.0f}%；市場隱含 {len(fed['meetings'][:4])} 次會議內累計 {cum6:+.0f}bp" if m else "無期貨資料"),
        "由聯邦基金期貨推算的市場定價（非聯準會承諾）：升息預期升高，代表『更高、更久』的利率仍未結束。")
    hc, cc = infl.get("headline") or {}, infl.get("core") or {}
    add("inflation", "通膨（CPI）", "ahead", wavg([(lin(cc.get("mom"), 0.0015, 0.004), 0.4), (lin(cc.get("ann3m"), 0.025, 0.045), 0.3), (lin(hc.get("yoy"), 0.025, 0.045), 0.3)]),
        (f"CPI {hc.get('date', '')[:7]}：年增 {hc['yoy'] * 100:.1f}%、月增 {hc['mom'] * 100:+.2f}%；核心月增 {cc['mom'] * 100:+.2f}%、近3月年化 {cc['ann3m'] * 100:.1f}%" if hc and cc and isnum(hc.get("yoy")) and isnum(cc.get("ann3m")) else "無資料"),
        "通膨居高不下會讓聯準會無法降息、甚至升息；這裡看已公布的數字，下一次公布若再超預期才是新的風險。")
    ur, pay = infl.get("unrate") or {}, infl.get("payrolls") or {}
    add("growth", "就業與景氣", "ahead", wavg([(lin(ur.get("above_12m_low"), 0.0, 0.6), 0.5), (lin(pay.get("avg3m_k"), 150, 0), 0.5)]),
        (f"失業率 {ur['value']:.1f}%（較12個月低點 +{ur['above_12m_low']:.1f}）；非農近3月平均 {pay['avg3m_k']:+.0f} 千人" if ur and pay else "無資料"),
        "就業轉弱代表景氣降溫（利率壓力下降但獲利風險上升）；兩者都要看。")
    # --- 台股專屬
    twd, taiex5 = _ret(prices, "TWD=X", 20), _ret(prices, "^TWII", 5)
    add("tw_fx", "台幣貶值（資金面）", "happened", lin(twd, 0.0, 0.03), f"美元兌台幣 20日 {twd * 100:+.1f}%" if isnum(twd) else "無資料", "台幣快速貶值常伴隨外資賣超。", tw=True)
    add("tw_semi", "費半與加權指數", "happened", wavg([(lin(-sox5 if isnum(sox5) else None, 0, 0.10), 0.6), (lin(-taiex5 if isnum(taiex5) else None, 0, 0.05), 0.4)]),
        f"費半 5日 {sox5 * 100:+.1f}%；加權 5日 {taiex5 * 100:+.1f}%" if isnum(sox5) and isnum(taiex5) else "無資料", "台股高度連動費半（AI 供應鏈）。", tw=True)
    return F


def score_all(F: list[dict], p: dict) -> dict:
    w = p["weights"]
    glob = wavg([(f["score"], w[f["id"]]) for f in F if f["id"] in w and not f["tw_only"]])
    tw_extra = wavg([(f["score"], 1.0) for f in F if f["tw_only"]])
    tw = wavg([(glob, 1 - p["tw_blend"]), (tw_extra, p["tw_blend"])])
    return {"US": round(glob, 1) if glob is not None else None, "TW": round(tw, 1) if tw is not None else None}


def overlay(scores: dict, p: dict) -> dict:
    """依風險等級調整選股門檻（有上限、可解釋）：警戒／高風險時，提高目標價上檔門檻；高估值或高 Beta 股再加碼。"""
    o = p["overlay"]
    out = {}
    for m, s in scores.items():
        lv = level_of(s, p) if s is not None else "normal"
        out[m] = {"level": lv, "label": LABEL[lv], "extra_upside": o["extra_upside"][lv] if lv in o["extra_upside"] else 0.0,
                  "sens_extra": o["sensitive_extra"][lv] if lv in o["sensitive_extra"] else 0.0, "pe": o["pe_sensitive"], "beta": o["beta_sensitive"]}
    return out


def rate_sensitive(f: dict, ov: dict) -> bool:
    return (isnum(f.get("pe_fwd")) and f["pe_fwd"] > ov["pe"]) or (isnum(f.get("beta")) and f["beta"] > ov["beta"])


# ---------------------------------------------------------------- 事件與新聞
def events(fed: dict, infl: dict, today: dt.date) -> list[dict]:
    out = []
    for m in fed.get("meetings", [])[:2]:
        out.append({"date": m["date"], "days": m["days"], "name": "FOMC 利率決議", "note": f"市場定價{'升息' if m['dir'] == 'hike' else '降息' if m['dir'] == 'cut' else '按兵不動'}機率約 {m['prob'] * 100:.0f}%"})
    for key, name in (("headline", "下一次 CPI 公布"), ("payrolls", "下一次非農就業公布")):
        d = (infl.get(key) or {}).get("date")
        if d:
            last = dt.date.fromisoformat(d)
            ny, nm = last.year + (last.month + 1) // 12, (last.month + 1) % 12 + 1    # 某月資料在『下個月』公布 → 下一筆（次月資料）在『再下個月』公布
            out.append({"date": f"{ny}-{nm:02d}", "days": None, "name": name, "note": f"{d[:7]} 月資料已公布；下一筆（{'CPI 約在月中' if key == 'headline' else '非農通常在月初'}）預計 {ny}/{nm:02d}，確切日期請查勞工統計局行事曆"})
    return out


def headlines(items: list[dict], cfg: dict, n: int = 8) -> list[dict]:
    rx = {k: re.compile("|".join(re.escape(w) for w in ws), re.I) for k, ws in cfg["headline_keywords"].items()}
    out, seen = [], set()
    for it in sorted(items, key=lambda x: x.get("published") or dt.datetime.min.replace(tzinfo=dt.timezone.utc), reverse=True):
        for k, r in rx.items():
            if r.search(it["title"]):
                key = it["title"][:40]
                if key not in seen:
                    seen.add(key)
                    out.append({"theme": k, "title": it["title"], "link": it.get("link", ""), "source": it.get("source", "")})
                break
        if len(out) >= n:
            break
    return out


# ---------------------------------------------------------------- 主函式
def analyze(prices: dict, fred: dict, items: list[dict], cfg: dict, p: dict, today: dt.date, fred_errors: list[str] | None = None) -> dict:
    fed = fed_path(prices, fred, cfg, today)
    infl = inflation_labor(fred)
    F = factors(prices, fed, infl, p)
    sc = score_all(F, p)
    ov = overlay(sc, p)
    ok = sum(1 for f in F if f["score"] is not None)
    miss = [f["label"] for f in F if f["score"] is None]
    keys = ("^TNX", "^IRX", "DX-Y.NYB", "CL=F", "^VIX", "^GSPC", "^SOX")
    return {"asof": str(today), "scores": sc, "overlay": ov, "factors": F, "fed": fed, "inflation": infl, "events": events(fed, infl, today),
            "headlines": headlines(items, cfg), "coverage": {"ok": ok, "total": len(F), "missing": miss, "fred_errors": fred_errors or [], "contracts": fed.get("n_contracts", 0)},
            "series": {t: _series(prices, t) for t in keys},
            "levels_now": {"us10y": _last(prices, "^TNX"), "us3m": _last(prices, "^IRX"), "us30y": _last(prices, "^TYX"), "vix": _last(prices, "^VIX")}}


def record(m: dict) -> dict:
    return {"date": m["asof"], "US": m["scores"]["US"], "TW": m["scores"]["TW"], "level": {k: v["level"] for k, v in m["overlay"].items()},
            "states": {f["id"]: f["score"] for f in m["factors"]}}


def headline_text(m: dict) -> str:
    us, tw = m["overlay"]["US"], m["overlay"]["TW"]
    return f"美股環境「{us['label']}」（{m['scores']['US']:.0f} 分）、台股環境「{tw['label']}」（{m['scores']['TW']:.0f} 分）" if all(isnum(m["scores"][k]) for k in ("US", "TW")) else "總經資料不足"
