import datetime as dt
import os
import sys
import tempfile
from pathlib import Path

import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
os.environ["IJ_OUT"] = tempfile.mkdtemp()
(Path(os.environ["IJ_OUT"]) / "config").mkdir()

from ijournal import config as C  # noqa: E402
from ijournal.news import scan  # noqa: E402
from ijournal.providers import DemoProvider, parse_feed  # noqa: E402
from ijournal.tracker import _fwd  # noqa: E402
from ijournal.valuation import dcf_per_share, value_stock  # noqa: E402

P = C.load_params()


def test_parse_rss_and_atom():
    rss = b"<rss><channel><item><title>A</title><link>http://x</link><pubDate>Tue, 06 Oct 2026 01:00:00 GMT</pubDate></item></channel></rss>"
    assert parse_feed(rss)[0]["title"] == "A"
    atom = b'<feed xmlns="http://www.w3.org/2005/Atom"><entry><title>B</title><link href="http://y"/><updated>2026-10-06T01:00:00Z</updated></entry></feed>'
    r = parse_feed(atom)[0]
    assert r["title"] == "B" and r["link"] == "http://y" and r["published"] is not None


def test_news_sector_and_ticker_matching():
    uni, src = C.load_universe(), C.load_sources()
    now = dt.datetime.now(dt.timezone.utc)
    items = [{"title": "台積電 CoWoS 擴產 訂單強勁", "summary": "", "link": "", "source": "t", "market": "TW", "weight": 1.0, "published": now},
             {"title": "Nvidia stock surges on record AI chip demand", "summary": "", "link": "", "source": "t", "market": "US", "weight": 1.0, "published": now},
             {"title": "old news about GPU", "summary": "", "link": "", "source": "t", "market": "US", "weight": 1.0, "published": now - dt.timedelta(days=5)}]
    r = scan(items, uni, src, now)
    assert r["sector"]["ai_chips"]["TW"]["n"] == 1 and r["sector"]["ai_chips"]["US"]["n"] == 1  # 過期新聞被排除
    assert r["sector"]["ai_chips"]["US"]["sent"] > 0
    assert "2330.TW" in r["tickers"] and "NVDA" in r["tickers"]


def _f(**kw):
    base = {"eps_fwd_px": 10.0, "eps_ttm_px": 9.0, "same_ccy": True, "fcf_ps": 6.0, "beta": 1.0, "target_mean": 150.0, "n_analysts": 10,
            "target_low": 100, "target_high": 200, "eps_growth": 0.2, "rev_growth": 0.15}
    base.update(kw)
    return base


def test_valuation_blend_and_scenarios():
    v = value_stock(100.0, _f(), {"vol_ann": 0.3}, "US", {"pe": 18.0, "g": 0.1, "n": 5}, P)
    assert v and v["n_methods"] == 4
    assert abs(sum(m["weight"] for m in v["methods"].values()) - 1) < 1e-9
    assert v["bear"] < v["base"] < v["bull"]
    assert v["confidence"] in "高中低"


def test_outlier_method_dropped_and_insufficient_data_rejected():
    v = value_stock(100.0, _f(target_mean=1000.0), {"vol_ann": 0.3}, "US", {"pe": 18.0, "g": 0.1, "n": 5}, P)
    assert "consensus" in v["dropped"] and "consensus" not in v["methods"]
    assert value_stock(100.0, _f(eps_fwd_px=None, eps_ttm_px=None, fcf_ps=None), {"vol_ann": 0.3}, "US", {"pe": None, "g": None, "n": 0}, P) is None


def test_target_shrink_reduces_upside():
    p2 = dict(P, target_shrink=0.5)
    a = value_stock(100.0, _f(), {"vol_ann": 0.3}, "US", {"pe": 18.0, "g": 0.1, "n": 5}, P)
    b = value_stock(100.0, _f(), {"vol_ann": 0.3}, "US", {"pe": 18.0, "g": 0.1, "n": 5}, p2)
    assert abs(b["upside"] - a["upside"] * 0.5) < 1e-9


def test_dcf_negative_fcf_skipped():
    assert dcf_per_share({"fcf_ps": -1.0}, 0.1, "US", 1.0, P)[0] is None


def test_forward_returns_and_alpha():
    idx = pd.bdate_range("2026-01-01", periods=30)
    stock = pd.DataFrame({"Close": [100 + i for i in range(30)], "High": [101 + i for i in range(30)], "Low": [99 + i for i in range(30)]}, index=idx)
    bench = pd.DataFrame({"Close": [100.0] * 30, "High": [100.0] * 30, "Low": [100.0] * 30}, index=idx)
    f = _fwd(stock, bench, str(idx[0].date()))
    assert abs(f["r5"] - 0.05) < 1e-9 and abs(f["a5"] - 0.05) < 1e-9
    assert f["r60"] is None  # 尚未到期
    assert f["n_days"] == 29


def test_end_to_end_with_feedback_loop():
    from ijournal.daily import run_daily
    from ijournal.review import run_review
    from ijournal.tracker import run_tracker
    prov = DemoProvider(end=dt.date(2026, 9, 30))
    dates = [run_daily(prov, asof=d) for d in ("2026-06-01", "2026-06-08", "2026-06-15", "2026-06-22")]
    assert all(dates)
    assert run_daily(prov, asof="2026-06-01") is None  # 同日不重複產生
    data = C.path("data")
    pj = C.load_json(data / "picks" / "2026-06-01.json")
    assert len(pj["picks"]) <= 10 and all(p["target"]["bear"] < p["target"]["base"] for p in pj["picks"])
    perf = run_tracker(prov)
    assert perf["positions"] and perf["summary"]["20"]["n"] > 0
    out = Path(run_review("monthly", tune=True, date="2026-09-30"))
    assert out.exists()
    # 樣本不足（僅 4 個推薦日）→ 不應調整任何參數
    assert C.load_params()["version"] == 1
    # 放寬門檻 → 會調整並留下稽核紀錄
    run_review("monthly", tune=True, date="2026-09-30", min_dates=2)
    assert C.load_params()["version"] == 2 and C.load_json(C.history_path())[-1]["changes"]
    w = C.load_params()["factor_weights"]
    assert abs(sum(w.values()) - 1) < 1e-9 and all(0.03 <= x <= 0.35 for x in w.values())
    from ijournal.site import build_site
    build_site()
    assert (C.path("site") / "index.html").exists() and (C.path("site") / "performance.html").exists()


def test_emerging_zone_pipeline():
    import json
    from ijournal.daily import run_daily
    from ijournal.tracker import run_tracker
    uni = C.load_universe()
    assert len(uni["themes"]) >= 6 and any(s.get("emerging_only") for s in uni["stocks"].values())
    # 趨勢專屬標的不得出現在主榜產業內
    main_ids = {s["id"] for s in uni["sectors"]}
    assert all(s["sector"] in main_ids for s in uni["stocks"].values() if not s.get("emerging_only"))
    prov = DemoProvider(end=dt.date(2026, 9, 30))
    d = run_daily(prov, asof="2026-07-06")
    md = (C.path("emerging") / f"{d}.md").read_text(encoding="utf-8")
    assert "前瞻專區" in md and "論點不成立的條件" in md
    rec = C.load_json(C.path("data") / "emerging" / f"{d}.json")
    assert rec["themes"] and all(p["book"] == "emerging" and p["theme"] for p in rec["picks"])
    # 主榜推薦不含趨勢專屬標的；前瞻績效與主榜分開統計
    main = C.load_json(C.path("data") / "picks" / f"{d}.json")
    assert not ({p["ticker"] for p in main["picks"]} & {s["ticker"] for s in uni["stocks"].values() if s.get("emerging_only")})
    perf = run_tracker(prov)
    assert "emerging" in perf and all(p["book"] in ("main", "emerging", "bargain") for p in perf["positions"])
    assert perf["summary"]["5"]["n"] == len([p for p in perf["positions"] if p["book"] == "main" and p.get("r5") is not None])


def test_emerging_scoring_prefers_low_coverage():
    import datetime as dt2
    from ijournal.emerging import score_themes
    from ijournal.news import scan as nscan
    uni, src = C.load_universe(), C.load_sources()
    now = dt2.datetime.now(dt2.timezone.utc)
    # 大量報導 water 主題、完全不報導 pq 主題
    items = [{"title": f"drought water scarcity ultrapure water story {i}", "summary": "", "link": "", "source": "t", "market": "US", "weight": 1.0, "published": now} for i in range(40)]
    main = nscan(items, uni, src, now)
    from ijournal.daily import run_daily  # noqa: F401  (確保模組可匯入)
    from ijournal.features import norm_fundamentals, price_features
    prov = DemoProvider(end=dt.date(2026, 9, 30))
    px = prov.prices(list(uni["stocks"]) + ["^GSPC", "^TWII"])
    from ijournal.selection import score_stocks
    feats = {t: price_features(px[t], px["^TWII" if uni["stocks"][t]["market"] == "TW" else "^GSPC"]) for t in uni["stocks"]}
    funds = {t: norm_fundamentals(prov.fundamentals(t), feats[t]["last"], "USD") for t in uni["stocks"]}
    rows = score_stocks(uni, feats, funds, {"US": {}, "TW": {}}, main, P)
    th = {t["id"]: t for t in score_themes(uni, items, main, src, rows, P)}
    assert th["water_infra"]["coverage_ratio"] > th["pq_security"]["coverage_ratio"]
    assert th["pq_security"]["components"]["low_coverage"] > th["water_infra"]["components"]["low_coverage"]


def test_no_padding_when_few_qualify():
    from ijournal.daily import run_daily
    from ijournal.selection import pick  # noqa: F401
    p = C.load_params()
    p["selection"]["min_upside"] = 5.0  # 幾乎無人能過 → 應該照實少選，而不是放寬或跨產業補位
    C.save_json(C.params_path(), p)
    try:
        d = run_daily(DemoProvider(end=dt.date(2026, 9, 30)), asof="2026-08-03")
        pj = C.load_json(C.path("data") / "picks" / f"{d}.json")
        assert len(pj["picks"]) == 0
        md = (C.path("journal") / f"{d}.md").read_text(encoding="utf-8")
        assert "合格標的不足，不為湊數放寬門檻" in md and "放寬至排名較後" not in md
    finally:
        p["selection"]["min_upside"] = 0.05
        C.save_json(C.params_path(), p)


def test_emerging_spread_rule_direction_consistency():
    from ijournal.emerging import eligible
    p = C.load_params()
    base = {"market": "US", "f": {"market_cap": 5e10, "eps_ttm_px": 5.0}, "pf": {"turnover": 1e9}, "scores": {"quality": 60}}
    mk = lambda ups, spread: dict(base, val={"upside": 0.3, "spread": spread, "methods": {str(i): {"upside": u} for i, u in enumerate(ups)}})
    assert eligible(mk([0.1, 0.5], 0.4), p)[0]                       # 分歧小 → 過
    assert eligible(mk([0.1, 0.7], 0.7), p)[0]                       # 分歧 70% 但方向一致 → 放行
    assert not eligible(mk([-0.2, 0.7], 0.7), p)[0]                  # 分歧 70% 且有方法看空 → 不放行
    assert not eligible(mk([0.1, 0.9], 0.9), p)[0]                   # 超過硬上限 → 不放行


def test_dashboard_snapshot_and_site_assets():
    import shutil
    import subprocess
    from ijournal.daily import run_daily
    from ijournal.site import build_site
    d = run_daily(DemoProvider(end=dt.date(2026, 9, 30)), asof="2026-07-13")
    sn = C.load_json(C.path("data") / "snapshots" / f"{d}.json")
    assert {"date", "indices", "picks", "shortage", "sectors", "emerging", "news", "generated_at", "basis"} <= set(sn)
    assert sn["generated_at"].endswith("+00:00") and sn["basis"]["TW"] and sn["basis"]["US"]
    assert all(len(x["series"]) > 1 for x in sn["indices"])
    for m in ("TW", "US"):
        assert sn["shortage"][m]["N"] == 5 and sn["shortage"][m]["n"] == len(sn["picks"][m])
        for p in sn["picks"][m]:
            assert p["target"]["bear"] < p["target"]["base"] < p["target"]["bull"] and p["methods"] and p["series"]["c"] and p["thesis"]
    allp = [p for m in ("TW", "US") for p in sn["picks"][m]] + [p for m in ("TW", "US") for p in sn["emerging"]["picks"][m]]
    for p in allp:  # 每檔都要有「為什麼推薦」（一定含估值）與兩組風險（已出現／尚未發生）
        assert p["headline"] and 1 <= len(p["why"]) <= 5 and any(w["tag"] == "估值" for w in p["why"]), p["ticker"]
        assert isinstance(p["risks_now"], list) and p["risks_watch"], p["ticker"]
    for p in allp:  # 亮點標籤：最多 3 個、皆為非空短字串（沒有突出事實就不放，不硬湊）
        assert len(p["highlights"]) <= 3 and all(isinstance(x["t"], str) and 0 < len(x["t"]) <= 14 for x in p["highlights"]), p["ticker"]
    assert sn["emerging"]["top_n"] == C.load_params()["emerging"]["top_themes"] and all(x["short"] for x in sn["emerging"]["themes"])
    assert all(p["risks_watch"][0]["t"].startswith("若出現就代表論點不成立") for m in ("TW", "US") for p in sn["emerging"]["picks"][m])
    md = (C.path("journal") / f"{d}.md").read_text(encoding="utf-8")
    assert "**為什麼推薦（利多）**" in md and "**已出現的風險**" in md and "**未來需留意（尚未發生）**" in md
    assert sn["sectors"]["TW"] and sn["emerging"]["themes"] and set(sn["emerging"]["shortage"]) == {"TW", "US"}
    build_site()
    site = C.path("site")
    for f in ("index.html", "app.js", "style.css", "data/index.json", "data/performance.json", f"data/snap/{d}.json", "performance.html", "emerging.html"):
        assert (site / f).exists(), f
    assert C.load_json(site / "data" / "index.json")["dates"][0]["date"] == max(x["date"] for x in C.load_json(site / "data" / "index.json")["dates"])
    # 舊網址導向儀表板；儀表板不可用 innerHTML 渲染資料（新聞標題來自外部 RSS）
    assert "index.html#perf" in (site / "performance.html").read_text(encoding="utf-8")
    assert "innerHTML" not in (site / "app.js").read_text(encoding="utf-8")
    if shutil.which("node"):
        assert subprocess.run(["node", "--check", str(site / "app.js")], capture_output=True).returncode == 0


def test_strip_partial_bars_only_uses_completed_sessions():
    import numpy as np
    from zoneinfo import ZoneInfo
    from ijournal.sessions import exchange_of, strip_partial
    idx = pd.to_datetime(["2026-10-06", "2026-10-07"])
    mk = lambda: pd.DataFrame({"Close": np.array([1.0, 2.0])}, index=idx)
    prices = {"2330.TW": mk(), "^TWII": mk(), "NVDA": mk(), "TWD=X": mk()}
    tw = ZoneInfo("Asia/Taipei")
    # 台灣 10/07 10:00：台股盤中 → 台股當日（未完成）K 線剔除；外匯不處理
    out, n = strip_partial(prices, dt.datetime(2026, 10, 7, 10, 0, tzinfo=tw))
    assert len(out["2330.TW"]) == 1 and len(out["^TWII"]) == 1 and len(out["TWD=X"]) == 2
    # 台灣 10/07 14:30：台股已收盤（13:30+寬限）→ 保留
    out, n = strip_partial(prices, dt.datetime(2026, 10, 7, 14, 30, tzinfo=tw))
    assert len(out["2330.TW"]) == 2 and len(out["^TWII"]) == 2
    # 台灣 10/08 07:48（排程目標時間）：台股前一日 K 線完整、美股 10/07 已收盤 → 全部保留
    out, n = strip_partial(prices, dt.datetime(2026, 10, 8, 7, 48, tzinfo=tw))
    assert n == 0 and all(len(v) == 2 for v in out.values())
    # 美股盤中（台灣 10/07 23:00 = 紐約 10/07 11:00）：美股當日 K 線剔除
    out, n = strip_partial(prices, dt.datetime(2026, 10, 7, 23, 0, tzinfo=tw))
    assert len(out["NVDA"]) == 1 and len(out["2330.TW"]) == 2
    assert exchange_of("TWD=X") is None and exchange_of("^TWII") == "TW" and exchange_of("8299.TWO") == "TW" and exchange_of("BRK-B") == "US"


def test_two_updates_per_day_newer_close_overwrites_and_duplicates_skip():
    from ijournal.daily import run_daily
    from ijournal.utils import market_of

    class UsLagging(DemoProvider):
        """模擬台股收盤後（美股仍是前一日收盤）的資料。"""
        def prices(self, tickers, days=800):
            return {t: (d.iloc[:-1] if market_of(t) == "US" else d) for t, d in super().prices(tickers, days).items()}

    snaps = C.path("data") / "snapshots"
    end = dt.date(2026, 9, 30)  # 週三
    # ① 台股收盤後：台股 9/30、美股 9/29 → 基準日 9/30
    d1 = run_daily(UsLagging(end=end))
    assert d1 == "2026-09-30"
    b1 = C.load_json(snaps / f"{d1}.json")["basis"]
    assert b1["TW"] == "2026-09-30" and b1["US"] == "2026-09-29"
    # ② 美股收盤後：兩邊都 9/30 → 同一基準日，美股收盤日更新 → 覆蓋
    d2 = run_daily(DemoProvider(end=end))
    assert d2 == "2026-09-30"
    assert C.load_json(snaps / f"{d2}.json")["basis"] == {"US": "2026-09-30", "TW": "2026-09-30"}
    # ③ 排程重複觸發（資料沒有更新）→ 略過
    assert run_daily(DemoProvider(end=end)) is None
    # 台股收盤後的『舊』資料（美股較舊）不可覆蓋較新的結果
    assert run_daily(UsLagging(end=end)) is None
    assert C.load_json(snaps / f"{d2}.json")["basis"]["US"] == "2026-09-30"


# ---- 資料修補（樣本取自 2026-10-07 證交所／櫃買中心實際回傳格式）
TWSE_CSV = '''"日期","證券代號","證券名稱","成交股數","成交金額","開盤價","最高價","最低價","收盤價","漲跌價差","成交筆數"
"1151007","2330","台積電","14466183","37400000000","2565.00","2585.00","2560.00","2585.00","0.0000","20000"
"1151007","2383","台光電","2600543","15500000000","6015.00","6325.00","5950.00","5950.00","-45.0000","9000"
"1151007","1234","停牌股","0","0","--","--","--","--","0.0000","0"
備註：測試用說明列,
'''
TPEX_JSON = '[{"Date":"1151007","SecuritiesCompanyCode":"8299","CompanyName":"群聯","Close":"2060.00","Change":"+10","Open":"2050.00","High":"2070.00","Low":"2040.00","TradingShares":"1000000"}]'
FMTQIK = '{"stat":"OK","data":[["115/10/06","10,889,677,530","1,026,204,771,139","4,923,225","49,822.55","110.51"],["115/10/07","10,357,959,027","986,280,041,197","4,929,172","49,806.37","-16.18"]]}'


def test_datafix_parsers_use_real_formats():
    from ijournal.datafix import parse_fmtqik, parse_tpex_json, parse_twse_csv, roc_date
    assert roc_date("1151007") == dt.date(2026, 10, 7) and roc_date("115/10/07") == dt.date(2026, 10, 7) and roc_date("abc") is None
    day, d = parse_twse_csv(TWSE_CSV)
    assert day == dt.date(2026, 10, 7) and d["2330"]["Close"] == 2585.0 and d["2383"]["Volume"] == 2600543.0 and "1234" not in d  # 停牌（無成交）略過
    day, d = parse_tpex_json(TPEX_JSON)
    assert day == dt.date(2026, 10, 7) and d["8299"]["Close"] == 2060.0
    f = parse_fmtqik(FMTQIK)
    assert f[dt.date(2026, 10, 7)]["Close"] == 49806.37


def _frame(last: str, close: float, n: int = 3):
    idx = pd.bdate_range(end=last, periods=n)
    return pd.DataFrame({"Open": close, "High": close, "Low": close, "Close": close, "Volume": 1000.0}, index=idx)


def test_datafix_tw_appends_missing_day_overrides_diff_and_fixes_twii():
    from ijournal import datafix
    prices = {"2330.TW": _frame("2026-10-06", 2585.0), "2383.TW": _frame("2026-10-07", 5900.0), "8299.TWO": _frame("2026-10-07", 2060.0), "^TWII": _frame("2026-10-06", 49822.55)}
    fetch = lambda url: {datafix.TWSE_ALL_URL: TWSE_CSV, datafix.TPEX_ALL_URL: TPEX_JSON}.get(url, FMTQIK)
    rep = datafix.patch_tw(prices, ["2330.TW", "2383.TW", "8299.TWO"], fetch=fetch)
    assert rep["appended"] == 1 and rep["overridden"] == 1 and rep["agree"] == 1 and rep["twii_appended"] == 1 and not rep["errors"]
    assert prices["2330.TW"].index[-1] == pd.Timestamp("2026-10-07") and prices["2330.TW"]["Close"].iloc[-1] == 2585.0
    assert prices["2383.TW"]["Close"].iloc[-1] == 5950.0 and len(prices["2383.TW"]) == 3   # 差異以官方值修正、不新增列
    assert prices["^TWII"]["Close"].iloc[-1] == 49806.37
    # 清單中的上市／上櫃別與官方不符 → 偵測並回報（Yahoo 對錯誤後綴查不到）
    p3 = {"^TWII": _frame("2026-10-06", 49822.55)}
    rep3 = datafix.patch_tw(p3, ["2383.TWO", "8299.TW"], fetch=fetch)
    assert sorted(rep3["wrong_suffix"]) == ["2383.TWO→2383.TW", "8299.TW→8299.TWO"]
    # 來源失敗 → 只記錄、不中斷、維持 Yahoo 原值
    p2 = {"2330.TW": _frame("2026-10-06", 2585.0), "^TWII": _frame("2026-10-06", 1.0)}
    def boom(url): raise RuntimeError("down")
    rep2 = datafix.patch_tw(p2, ["2330.TW"], fetch=boom)
    assert rep2["errors"] and len(p2["2330.TW"]) == 3


def test_datafix_us_provisional_close_only_for_matching_session():
    from ijournal import datafix
    prices = {"NVDA": _frame("2026-10-06", 239.24), "MSFT": _frame("2026-10-07", 529.5), "AAPL": _frame("2026-10-06", 333.6), "AMD": _frame("2026-10-06", 100.0)}
    ny = "America/New_York"
    metas = {"NVDA": {"regularMarketPrice": 237.47, "regularMarketDayHigh": 239.08, "regularMarketDayLow": 236.39, "regularMarketVolume": 81e6, "regularMarketTime": pd.Timestamp("2026-10-07 16:00", tz=ny)},
             "AAPL": {"regularMarketPrice": 336.67, "regularMarketTime": pd.Timestamp("2026-10-06 16:00", tz=ny)},  # 報價日期不符（前一日）→ 不補
             "AMD": None}  # 取不到
    rep = datafix.patch_us(prices, ["NVDA", "MSFT", "AAPL", "AMD"], dt.date(2026, 10, 7), fetch_meta=lambda t: metas.get(t))
    assert rep["provisional"] == ["NVDA"] and rep["failed"] == 2
    assert prices["NVDA"].index[-1] == pd.Timestamp("2026-10-07") and prices["NVDA"]["Close"].iloc[-1] == 237.47
    assert len(prices["AAPL"]) == 3 and len(prices["MSFT"]) == 3
    assert any("暫定" in n for n in datafix.notes({"tw": None, "us": rep, "errors": []}))
    # 基準日：各市場個股最後一根日期的眾數（不看指數）
    prices["^TWII"] = _frame("2026-10-01", 1.0)
    assert datafix.market_basis(prices, ["NVDA", "MSFT", "AAPL", "AMD"]) == {"US": "2026-10-07"}


# ---- 資料品質關卡
def _qctx(**kw):
    """建立一組『全部正常』的品質檢查輸入；測試時覆寫其中一項。"""
    tick = [f"{i:04d}.TW" for i in range(10)] + [f"US{i}" for i in range(10)]
    prices = {t: _frame("2026-10-07", 100.0, 5) for t in tick}
    base = dict(universe={t: {} for t in tick}, feats={t: {} for t in tick}, basis={"TW": "2026-10-07", "US": "2026-10-07"},
                rows={t: {"val": {"x": 1}} for t in tick}, prices=prices, news_log=[{"ok": True}] * 10,
                data_report={"tw": {"date": "2026-10-07", "agree": 9, "overridden": 0, "appended": 0, "missing": 1, "errors": []}, "us": {"provisional": ["US0"], "failed": 0}, "errors": []},
                params=C.load_params(), now=dt.datetime(2026, 10, 7, 22, 0, tzinfo=dt.timezone.utc))
    base.update(kw)
    return base


def test_quality_expected_session_dates_and_lag():
    from zoneinfo import ZoneInfo
    from ijournal.quality import expected_session_date as ex, lag_bdays
    tw = ZoneInfo("Asia/Taipei")
    assert ex("TW", dt.datetime(2026, 10, 7, 10, 0, tzinfo=tw)) == dt.date(2026, 10, 6)     # 盤中 → 前一個交易日
    assert ex("TW", dt.datetime(2026, 10, 7, 14, 30, tzinfo=tw)) == dt.date(2026, 10, 7)    # 收盤後
    assert ex("TW", dt.datetime(2026, 10, 10, 9, 0, tzinfo=tw)) == dt.date(2026, 10, 9)     # 週六 → 週五
    assert ex("US", dt.datetime(2026, 10, 8, 7, 37, tzinfo=tw)) == dt.date(2026, 10, 7)     # 台灣早晨：美股前一日已收盤
    assert lag_bdays(dt.date(2026, 10, 7), dt.date(2026, 10, 7)) == 0 and lag_bdays(dt.date(2026, 10, 2), dt.date(2026, 10, 7)) == 3


def test_quality_levels_for_each_failure_mode():
    from ijournal.quality import assess
    assert assess(**_qctx())["level"] == "ok"
    # 涵蓋率：台股只剩 5/10 → bad
    c = _qctx(); c["feats"] = {t: {} for t in c["universe"] if not (t.endswith(".TW") and int(t[:4]) >= 5)}
    r = assess(**c); assert r["level"] == "bad" and any(i["code"] == "coverage_TW" for i in r["issues"])
    # 新鮮度：只有美股落後 1 日 → warn；長假（台股落後 4 日、美股最新）→ 只 warn；兩邊都落後 ≥3 日 → bad
    assert assess(**_qctx(basis={"TW": "2026-10-07", "US": "2026-10-06"}))["level"] == "warn"
    assert assess(**_qctx(basis={"TW": "2026-10-01", "US": "2026-10-07"}))["level"] == "warn"
    assert assess(**_qctx(basis={"TW": "2026-10-01", "US": "2026-10-01"}))["level"] == "bad"
    assert assess(**_qctx(now=None, basis={"TW": "2026-01-01", "US": "2026-01-01"}))["level"] == "ok"      # 回補／示範模式略過新鮮度
    # 官方與 Yahoo 差異：30% → bad；官方來源失敗 → warn
    tw = {"date": "2026-10-07", "agree": 7, "overridden": 3, "appended": 0, "missing": 0, "errors": [], "wrong_suffix": []}
    assert assess(**_qctx(data_report={"tw": tw, "us": None, "errors": []}))["level"] == "bad"
    assert assess(**_qctx(data_report={"tw": {"errors": ["連線逾時"]}, "us": None, "errors": []}))["level"] == "warn"
    ws = {"date": "2026-10-07", "agree": 9, "overridden": 0, "appended": 0, "missing": 1, "errors": [], "wrong_suffix": ["6690.TW→6690.TWO"]}
    r = assess(**_qctx(data_report={"tw": ws, "us": None, "errors": []})); assert r["level"] == "warn" and any(i["code"] == "universe_suffix" for i in r["issues"])
    # 價格異常跳動：大量標的單日 ±50% → bad
    c = _qctx()
    for t in list(c["prices"])[:6]:
        c["prices"][t] = c["prices"][t].copy(); c["prices"][t].iloc[-1, c["prices"][t].columns.get_loc("Close")] = 160.0
    r = assess(**c); assert r["level"] == "bad" and any(i["code"] == "jumps" for i in r["issues"])
    # 可估值比例過低 → bad；新聞來源多數失敗 → warn
    c = _qctx(); c["rows"] = {t: {"val": (None if i < 14 else {"x": 1})} for i, t in enumerate(c["rows"])}
    assert assess(**c)["level"] == "bad"
    assert assess(**_qctx(news_log=[{"ok": True}] * 3 + [{"ok": False}] * 7))["level"] == "warn"


def test_bad_quality_blocks_publishing_and_keeps_last_good_journal():
    from ijournal.daily import run_daily
    from ijournal.quality import DataQualityError

    class FewPrices(DemoProvider):
        """只回傳約兩成標的的價格（模擬來源大量失效）。"""
        def prices(self, tickers, days=800):
            full = super().prices(tickers, days)
            keep = {t for i, t in enumerate(sorted(full)) if i % 5 == 0} | {"^GSPC", "^TWII", "2330.TW"}
            return {t: d for t, d in full.items() if t in keep}

    end = dt.date(2026, 9, 28)
    d = run_daily(DemoProvider(end=end))
    jp = C.path("journal") / f"{d}.md"
    before = jp.read_text(encoding="utf-8")
    assert C.load_json(C.path("data") / "pipeline_status.json")["published"] is True
    with pytest.raises(DataQualityError):
        run_daily(FewPrices(end=end), force=True)
    st = C.load_json(C.path("data") / "pipeline_status.json")
    assert st["level"] == "bad" and st["published"] is False and st["issues"]
    assert jp.read_text(encoding="utf-8") == before                      # 上一份好日誌未被覆蓋
    log = C.load_json(C.path("data") / "quality_log.json")
    assert log[-1]["level"] == "bad" and log[-1]["published"] is False
    sn = C.load_json(C.path("data") / "snapshots" / f"{d}.json")
    assert sn["quality"]["level"] in ("ok", "warn") and sn["quality"]["checks"]


def _bargain_row(**over):
    pf = {"last": 70.0, "from_high": -0.35, "pos52": 0.15, "bounce20": 0.08, "ret_1m": 0.02, "dist200": -0.10, "turnover": 5e8}
    f = {"market_cap": 5e11, "eps_ttm_px": 3.0, "eps_fwd_px": 4.0, "rev_growth": 0.12, "pe_fwd": 15.0, "roe": 0.2}
    v = {"upside": 0.4, "spread": 0.2, "peer_pe": 22.0, "confidence": "高", "methods": {}}
    r = {"market": "US", "sector": "x", "ticker": "T", "composite": 60.0, "pf": pf, "f": f, "val": v, "scores": {"quality": 70.0, "growth": 65.0}}
    for k, x in over.items():
        r[k].update(x)
    return r


def test_bargain_eligibility_gates_block_value_traps():
    from ijournal import bargain as B
    assert B.eligible(_bargain_row(), P)[0]
    assert B.eligible(_bargain_row(pf={"from_high": -0.05, "pos52": 0.9}), P)[1] == "股價不在低基期"
    assert "價值陷阱" in B.eligible(_bargain_row(f={"rev_growth": -0.2}), P)[1]
    assert "價值陷阱" in B.eligible(_bargain_row(scores={"quality": 30.0}), P)[1]
    assert "落下的刀" in B.eligible(_bargain_row(pf={"ret_1m": -0.3}), P)[1]
    assert "止跌" in B.eligible(_bargain_row(pf={"bounce20": 0.0}), P)[1]
    assert B.eligible(_bargain_row(val={"upside": 0.05}), P)[1] == "目標價上檔空間不足"
    sc = B.score(_bargain_row(), P)
    assert 50 <= sc <= 100


def test_bargain_zone_pipeline_and_no_padding():
    from ijournal.daily import run_daily
    from ijournal.tracker import run_tracker
    d = run_daily(DemoProvider(end=dt.date(2026, 9, 30)), asof="2026-07-20")
    md = (C.path("bargain") / f"{d}.md").read_text(encoding="utf-8")
    assert "便宜好貨專區" in md and "便宜不等於好貨" in md
    rec = C.load_json(C.path("data") / "bargain" / f"{d}.json")
    assert all(p["book"] == "bargain" and p["facts"]["from_high"] <= -P["bargain"]["min_drawdown"] for p in rec["picks"])
    assert len(rec["picks"]) <= 2 * P["bargain"]["picks_per_market"]
    sn = C.load_json(C.path("data") / "snapshots" / f"{d}.json")
    ba = sn["bargain"]
    assert set(ba["shortage"]) == {"TW", "US"} and ba["rules"]["drawdown"] == P["bargain"]["min_drawdown"]
    for m in ("TW", "US"):
        for p in ba["picks"][m]:
            assert p["risks_watch"][0]["t"].startswith("價值陷阱") and p["why"][0]["tag"] == "低基期" and p["highlights"]
    perf = run_tracker(DemoProvider(end=dt.date(2026, 9, 30)))
    assert "bargain" in perf and all(p["book"] in ("main", "emerging", "bargain") for p in perf["positions"])
