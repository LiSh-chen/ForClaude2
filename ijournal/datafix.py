"""資料時效修補：以官方／更新的來源補上 Yahoo Finance 較慢或缺漏的最新收盤。

實測發現的 Yahoo 問題：
  ① 台灣加權指數（^TWII）日線常比個股慢一天更新
  ② 美股個股『當日』日線在收盤後數小時內是 OHLC 全為 NaN 的佔位列（只有部分成交量），
     被丟掉後，美股個股實際用到的是前一個交易日的收盤價
修補：
  - 台股：證交所（上市）／櫃買中心（上櫃）官方日收盤資料（收盤後約 1 小時內發布）。補上缺漏的最新一日，並以官方值修正差異；
    加權指數取自證交所「市場成交資訊」。實測 8 檔個股官方收盤與 Yahoo 完全一致。
  - 美股：收盤後以 Yahoo chart 的 regularMarketPrice（時間戳＝紐約 16:00，即正式收盤時的最後價）補上缺漏的當日 bar，標示為『暫定』
    （等 Yahoo 日線定案後，下次更新會用正式資料取代）。
全部失敗都只記錄、不中斷主流程；沒有修補到就維持 Yahoo 原值。
"""
from __future__ import annotations
import csv
import datetime as dt
import io
import json
import time
from collections import Counter
from concurrent.futures import ThreadPoolExecutor

import pandas as pd

from .utils import isnum, market_of

UA = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"}
TWSE_ALL_URL = "https://www.twse.com.tw/rwd/zh/afterTrading/STOCK_DAY_ALL?response=csv"
TPEX_ALL_URL = "https://www.tpex.org.tw/openapi/v1/tpex_mainboard_daily_close_quotes"
TWSE_FMTQIK_URL = "https://www.twse.com.tw/rwd/zh/afterTrading/FMTQIK?date={ym}01&response=json"
COLS = ["Open", "High", "Low", "Close", "Volume"]


# ---------------------------------------------------------------- 解析（純函式，可離線測試）
def roc_date(s: str) -> dt.date | None:
    """民國日期：'1151007' 或 '115/10/07' → 2026-10-07"""
    s = (s or "").strip().replace("/", "")
    if len(s) != 7 or not s.isdigit():
        return None
    try:
        return dt.date(int(s[:3]) + 1911, int(s[3:5]), int(s[5:7]))
    except ValueError:
        return None


def num(x) -> float | None:
    s = str(x if x is not None else "").replace(",", "").strip()
    if s in ("", "--", "-", "---", "X0.00"):
        return None
    try:
        return float(s)
    except ValueError:
        return None


def _bar(o, h, l, c, v) -> dict | None:
    c = num(c)
    if not c or c <= 0:
        return None  # 無成交（暫停交易等）
    o, h, l = (num(o) or c), (num(h) or c), (num(l) or c)
    return {"Open": o, "High": h, "Low": l, "Close": c, "Volume": num(v) or 0.0}


def parse_twse_csv(text: str) -> tuple[dt.date | None, dict[str, dict]]:
    """證交所『每日收盤行情（全部）』CSV。欄位：日期,證券代號,證券名稱,成交股數,成交金額,開盤價,最高價,最低價,收盤價,漲跌價差,成交筆數"""
    out, day = {}, None
    for r in csv.reader(io.StringIO(text.lstrip("﻿"))):
        if len(r) < 9:
            continue
        d = roc_date(r[0])
        if d is None:
            continue
        b = _bar(r[5], r[6], r[7], r[8], r[3])
        if b:
            out[r[1].strip()] = b
            day = day or d
    return day, out


def parse_tpex_json(text: str) -> tuple[dt.date | None, dict[str, dict]]:
    """櫃買中心 openapi『上櫃股票收盤行情』。欄位：Date,SecuritiesCompanyCode,Close,Open,High,Low,TradingShares,…"""
    out, day = {}, None
    for r in json.loads(text):
        d = roc_date(r.get("Date", ""))
        b = _bar(r.get("Open"), r.get("High"), r.get("Low"), r.get("Close"), r.get("TradingShares"))
        if d and b:
            out[str(r.get("SecuritiesCompanyCode", "")).strip()] = b
            day = day or d
    return day, out


def parse_fmtqik(text: str) -> dict[dt.date, dict]:
    """證交所『市場成交資訊』月資料：日期,成交股數,成交金額,成交筆數,發行量加權股價指數,漲跌點數（只有收盤指數）。"""
    out = {}
    for row in json.loads(text).get("data", []):
        d, close = roc_date(row[0]), num(row[4])
        if d and close:
            out[d] = {"Open": close, "High": close, "Low": close, "Close": close, "Volume": num(row[1]) or 0.0}
    return out


# ---------------------------------------------------------------- 網路（可注入以便測試）
def fetch_text(url: str, tries: int = 3, timeout: int = 40) -> str:
    import requests
    last = None
    for i in range(tries):
        try:
            r = requests.get(url, headers=UA, timeout=timeout)
            r.raise_for_status()
            if "因為安全性考量" in r.text[:400]:
                raise RuntimeError("來源以安全性擋頁拒絕自動化請求")
            return r.text
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(2 * (i + 1))
    raise RuntimeError(f"{url} 取得失敗：{last}")


def fetch_us_meta(ticker: str) -> dict | None:
    import yfinance as yf
    try:
        k = yf.Ticker(ticker)
        k.history(period="1d")
        return dict(k.history_metadata)
    except Exception:  # noqa: BLE001
        return None


# ---------------------------------------------------------------- 修補
def _append(df: pd.DataFrame, day: dt.date, bar: dict) -> pd.DataFrame:
    row = pd.DataFrame([bar], index=[pd.Timestamp(day)])[COLS]
    return pd.concat([df, row])


def patch_tw(prices: dict, tickers: list[str], fetch=fetch_text) -> dict:
    rep = {"date": None, "appended": 0, "overridden": 0, "agree": 0, "missing": 0, "twii_appended": 0, "errors": []}
    official: dict[str, dict] = {}
    for name, url, parse in (("證交所", TWSE_ALL_URL, parse_twse_csv), ("櫃買中心", TPEX_ALL_URL, parse_tpex_json)):
        try:
            day, d = parse(fetch(url))
            if day:
                official.update({k: dict(v, _day=day) for k, v in d.items()})
                rep["date"] = max(filter(None, [rep["date"], day]))
        except Exception as e:  # noqa: BLE001
            rep["errors"].append(f"{name}：{str(e)[:90]}")
    for t in tickers:
        df = prices.get(t)
        code = t.split(".")[0]
        o = official.get(code)
        if df is None or not len(df):
            continue
        if o is None:
            rep["missing"] += 1
            continue
        day, bar = o["_day"], {k: o[k] for k in COLS}
        last = df.index[-1].date()
        if last < day:
            prices[t] = _append(df, day, bar)
            rep["appended"] += 1
        elif last == day:
            if abs(float(df["Close"].iloc[-1]) - bar["Close"]) > max(0.005, bar["Close"] * 0.0005):
                prices[t] = df.copy()
                prices[t].loc[df.index[-1], COLS] = [bar[c] for c in COLS]
                rep["overridden"] += 1
            else:
                rep["agree"] += 1
    # 加權指數：Yahoo ^TWII 常慢一天 → 補上證交所官方收盤指數
    try:
        tw = prices.get("^TWII")
        if tw is not None and len(tw):
            last = tw.index[-1].date()
            months = {(last.year, last.month), ((rep["date"] or last).year, (rep["date"] or last).month)}
            idx: dict = {}
            for y, m in sorted(months):
                idx.update(parse_fmtqik(fetch(TWSE_FMTQIK_URL.format(ym=f"{y}{m:02d}"))))
            for d in sorted(x for x in idx if x > last):
                tw = _append(tw, d, idx[d])
                rep["twii_appended"] += 1
            prices["^TWII"] = tw
    except Exception as e:  # noqa: BLE001
        rep["errors"].append(f"加權指數：{str(e)[:90]}")
    return rep


def patch_us(prices: dict, tickers: list[str], expected: dt.date | None, fetch_meta=fetch_us_meta, workers: int = 8) -> dict:
    """美股個股的當日日線在收盤後常為 NaN 佔位列（被丟掉）→ 用 regularMarketPrice（時間戳＝當日紐約收盤）補上，標示暫定。"""
    rep = {"date": str(expected) if expected else None, "provisional": [], "failed": 0}
    if expected is None:
        return rep
    lag = [t for t in tickers if t in prices and len(prices[t]) and prices[t].index[-1].date() < expected]
    if not lag:
        return rep
    with ThreadPoolExecutor(workers) as ex:
        metas = list(ex.map(fetch_meta, lag))
    for t, m in zip(lag, metas):
        try:
            if not m or not isnum(m.get("regularMarketPrice")):
                raise ValueError("無報價")
            when = pd.Timestamp(m["regularMarketTime"])
            when = when.tz_convert("America/New_York") if when.tzinfo else when
            if when.date() != expected:  # 報價不是該場次的收盤（例如仍在盤中或前一日）→ 不補
                raise ValueError("報價日期不符")
            c = float(m["regularMarketPrice"])
            df = prices[t]
            hi = float(m["regularMarketDayHigh"]) if isnum(m.get("regularMarketDayHigh")) else c
            lo = float(m["regularMarketDayLow"]) if isnum(m.get("regularMarketDayLow")) else c
            vol = float(m["regularMarketVolume"]) if isnum(m.get("regularMarketVolume")) else float(df["Volume"].iloc[-1])
            prices[t] = _append(df, expected, {"Open": float(df["Close"].iloc[-1]), "High": max(hi, c), "Low": min(lo, c), "Close": c, "Volume": vol})
            rep["provisional"].append(t)
        except Exception:  # noqa: BLE001
            rep["failed"] += 1
    return rep


def market_basis(prices: dict, stock_tickers: list[str]) -> dict:
    """各市場的行情基準日＝該市場個股『最後一根日期』的眾數（不看指數，因為指數與個股更新時間不同）。"""
    out = {}
    for m in ("TW", "US"):
        c = Counter(str(prices[t].index[-1].date()) for t in stock_tickers if market_of(t) == m and t in prices and len(prices[t]))
        if c:
            out[m] = c.most_common(1)[0][0]
    return out


def apply_all(prices: dict, stock_tickers: list[str]) -> tuple[dict, dict]:
    """依序修補台股、美股；回傳 (prices, 報告)。"""
    report = {"tw": None, "us": None, "errors": []}
    try:
        report["tw"] = patch_tw(prices, [t for t in stock_tickers if market_of(t) == "TW"])
    except Exception as e:  # noqa: BLE001
        report["errors"].append(f"台股修補失敗：{str(e)[:90]}")
    try:
        g = prices.get("^GSPC")
        expected = g.index[-1].date() if g is not None and len(g) else None
        report["us"] = patch_us(prices, [t for t in stock_tickers if market_of(t) == "US"], expected)
    except Exception as e:  # noqa: BLE001
        report["errors"].append(f"美股修補失敗：{str(e)[:90]}")
    return prices, report


def notes(report: dict) -> list[str]:
    """給儀表板的簡短說明（資料來源與暫定標示）。"""
    out = []
    tw, us = report.get("tw"), report.get("us")
    if tw and tw.get("date"):
        s = f"台股：以證交所／櫃買中心官方收盤（{tw['date']}）校正"
        bits = []
        if tw["appended"]:
            bits.append(f"補上 Yahoo 缺漏 {tw['appended']} 檔")
        if tw["overridden"]:
            bits.append(f"修正差異 {tw['overridden']} 檔")
        if tw["twii_appended"]:
            bits.append("加權指數取自證交所")
        out.append(s + ("（" + "、".join(bits) + "）" if bits else "（與 Yahoo 一致）"))
    if us and us["provisional"]:
        out.append(f"美股：{len(us['provisional'])} 檔 {us['date']} 收盤價為『暫定』（Yahoo 日線尚未定案，採正式收盤時的最後成交價；下次更新會換成正式資料）")
    if us and us["failed"]:
        out.append(f"美股：{us['failed']} 檔無法取得最新收盤，使用前一日資料")
    for e in (report.get("errors") or []) + ((tw or {}).get("errors") or []):
        out.append("來源問題：" + e)
    return out
