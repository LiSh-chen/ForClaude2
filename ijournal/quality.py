"""資料品質關卡：每次產生日誌前自動檢查資料是否可信。

  ok    全部正常
  warn  輕微異常 → 照常發佈，但儀表板顯示警示橫幅並寫明原因
  bad   嚴重異常 → 不發佈、不覆蓋上一份好的日誌；儀表板顯示『本次更新未發佈』，workflow 標示失敗
門檻在 config/params.json 的 quality 區塊，可調整。
"""
from __future__ import annotations
import datetime as dt

from .sessions import EXCHANGES
from .utils import market_of

RANK = {"ok": 0, "warn": 1, "bad": 2}


class DataQualityError(RuntimeError):
    def __init__(self, result: dict):
        self.result = result
        bad = [i["msg"] for i in result["issues"] if i["level"] == "bad"]
        super().__init__("資料品質不合格，本次不發佈：" + "；".join(bad))


def expected_session_date(market: str, now: dt.datetime, grace_min: int = 20) -> dt.date:
    """依現在時間推算『該市場最近一個應已收盤的交易日』（只排除週末，不含國定假日）。"""
    tz, close = EXCHANGES[market]
    loc = now.astimezone(tz)
    d = loc.date()
    done = dt.datetime.combine(d, close, tzinfo=tz) + dt.timedelta(minutes=grace_min)
    if not (loc >= done and d.weekday() < 5):
        d -= dt.timedelta(days=1)
    while d.weekday() >= 5:
        d -= dt.timedelta(days=1)
    return d


def lag_bdays(basis: dt.date, expected: dt.date) -> int:
    """基準日落後應有收盤日幾個工作日（<=0 表示沒有落後）。"""
    if basis >= expected:
        return 0
    n, d = 0, basis
    while d < expected:
        d += dt.timedelta(days=1)
        n += d.weekday() < 5
    return n


def _chk(code: str, label: str, level: str, detail: str) -> dict:
    return {"code": code, "label": label, "level": level, "msg": detail}


def assess(*, universe: dict, feats: dict, basis: dict, rows: dict, prices: dict, news_log: list, data_report: dict | None,
           params: dict, now: dt.datetime | None = None) -> dict:
    """回傳 {level, checks:[...], issues:[非 ok 的 checks], stats}。now=None（回補/demo）時略過『新鮮度』檢查。"""
    q = params["quality"]
    checks: list[dict] = []

    # 1) 價格涵蓋率（分市場）
    for m, name in (("TW", "台股"), ("US", "美股")):
        tot = [t for t in universe if market_of(t) == m]
        ok = [t for t in tot if t in feats]
        cov = len(ok) / len(tot) if tot else 1.0
        lvl = "bad" if cov < q["coverage_bad"] else "warn" if cov < q["coverage_warn"] else "ok"
        miss = [t for t in tot if t not in feats]
        checks.append(_chk(f"coverage_{m}", f"{name}價格涵蓋率", lvl, f"{len(ok)}／{len(tot)} 檔有效（{cov:.0%}）" + (f"；缺：{'、'.join(miss[:6])}{'…' if len(miss) > 6 else ''}" if miss and lvl != "ok" else "")))

    # 2) 行情新鮮度
    if now is not None:
        lags = {}
        for m, name in (("TW", "台股"), ("US", "美股")):
            b = basis.get(m)
            lags[m] = lag_bdays(dt.date.fromisoformat(b), expected_session_date(m, now)) if b else 99
        both_stale = min(lags.values()) >= q["lag_bad_bdays"]
        for m, name in (("TW", "台股"), ("US", "美股")):
            n = lags[m]
            lvl = "ok" if n < q["lag_warn_bdays"] else ("bad" if both_stale else "warn")
            detail = f"行情截至 {basis.get(m) or '—'}，" + ("已是最新收盤" if n == 0 else f"落後應有收盤日 {n} 個工作日（若遇休市屬正常）")
            checks.append(_chk(f"fresh_{m}", f"{name}行情新鮮度", lvl, detail))
    else:
        checks.append(_chk("fresh", "行情新鮮度", "ok", "回補／示範模式，略過"))

    # 3) 官方來源與 Yahoo 的一致性（台股）
    tw = (data_report or {}).get("tw")
    if data_report is None:
        checks.append(_chk("tw_cross", "台股官方資料校正", "ok", "非即時模式，略過"))
    elif not tw or not tw.get("date"):
        why = "；".join((tw or {}).get("errors") or data_report.get("errors") or ["未取得"])
        checks.append(_chk("tw_cross", "台股官方資料校正", "warn", f"官方來源不可用，台股僅使用 Yahoo（{why}）"))
    else:
        n = tw["agree"] + tw["overridden"]
        rate = tw["overridden"] / n if n else 0.0
        miss = tw["missing"] / max(1, n + tw["missing"] + tw["appended"])
        lvl = "bad" if rate > q["tw_diff_bad"] else "warn" if (rate > q["tw_diff_warn"] or miss > q["tw_missing_warn"] or tw.get("errors")) else "ok"
        if tw.get("wrong_suffix"):
            checks.append(_chk("universe_suffix", "股票清單代號", "warn", "上市／上櫃別與官方不符（Yahoo 會查不到，請修正 config）：" + "、".join(tw["wrong_suffix"][:6])))
        checks.append(_chk("tw_cross", "台股官方資料校正", lvl, f"官方 {tw['date']}：與 Yahoo 一致 {tw['agree']} 檔、修正 {tw['overridden']} 檔（差異率 {rate:.0%}）、補缺 {tw['appended']} 檔、官方查無 {tw['missing']} 檔" + (f"；來源問題：{'、'.join(tw['errors'])}" if tw.get("errors") else "")))

    # 4) 美股暫定收盤補值
    us = (data_report or {}).get("us")
    if us is not None:
        tot = len(us["provisional"]) + us["failed"]
        rate = us["failed"] / tot if tot else 0.0
        lvl = "warn" if rate > q["us_patch_fail_warn"] else "ok"
        checks.append(_chk("us_patch", "美股最新收盤補值", lvl, f"暫定補上 {len(us['provisional'])} 檔、失敗 {us['failed']} 檔" + ("（失敗檔使用前一日價格）" if us["failed"] else "")))

    # 5) 價格異常跳動（可能是分割/資料錯誤）
    jumps = []
    for t in feats:
        df = prices.get(t)
        if df is not None and len(df) > 2:
            c = df["Close"].dropna()
            if len(c) > 2 and c.iloc[-2] > 0 and abs(c.iloc[-1] / c.iloc[-2] - 1) > q["jump_ret"]:
                jumps.append((t, c.iloc[-1] / c.iloc[-2] - 1))
    share = len(jumps) / max(1, len(feats))
    lvl = "bad" if share > q["jump_share_bad"] else "warn" if len(jumps) >= q["jump_count_warn"] else "ok"
    checks.append(_chk("jumps", "價格異常跳動", lvl, f"單日變動超過 {q['jump_ret']:.0%} 的標的 {len(jumps)} 檔" + (f"（{'、'.join(f'{t} {r:+.0%}' for t, r in jumps[:4])}）" if jumps else "")))

    # 6) 財報可估值比例
    scored = [r for r in rows.values()]
    vcov = sum(1 for r in scored if r["val"] is not None) / len(scored) if scored else 0.0
    lvl = "bad" if vcov < q["valuation_bad"] else "warn" if vcov < q["valuation_warn"] else "ok"
    checks.append(_chk("valuation", "財報可估值比例", lvl, f"{vcov:.0%} 的標的取得足夠財報與預估資料，可估算目標價"))

    # 7) 新聞來源
    nt = len(news_log)
    nok = sum(1 for x in news_log if x["ok"])
    ratio = nok / nt if nt else 1.0
    lvl = "warn" if (nt and ratio < q["news_ok_warn"]) else "ok"
    checks.append(_chk("news", "新聞來源", lvl, f"{nok}／{nt} 個來源成功" + ("（新聞因子可信度下降）" if lvl != "ok" else "")))

    level = max((c["level"] for c in checks), key=RANK.get)
    return {"level": level, "checks": checks, "issues": [c for c in checks if c["level"] != "ok"]}
