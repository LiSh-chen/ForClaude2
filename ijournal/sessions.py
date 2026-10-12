"""只使用『已收盤』的資料：交易所尚在盤中時，yfinance 會回傳當天『未完成』的 K 線（價格是即時價，不是收盤價），
必須剔除，否則同一天不同時間執行會得到不同的『收盤價』。"""
from __future__ import annotations
import datetime as dt
from zoneinfo import ZoneInfo

# 交易所 → (時區, 收盤時間)。美股指數/VIX 以 16:00（VIX 到 16:15）後再加寬限時間處理。
EXCHANGES = {"TW": (ZoneInfo("Asia/Taipei"), dt.time(13, 30)), "US": (ZoneInfo("America/New_York"), dt.time(16, 0))}


def exchange_of(ticker: str) -> str | None:
    if ticker.endswith("=X"):
        return None  # 外匯 24 小時交易，不處理
    return "TW" if ticker.endswith((".TW", ".TWO")) or ticker == "^TWII" else "US"


def strip_partial(prices: dict, now: dt.datetime | None = None, grace_min: int = 20) -> tuple[dict, int]:
    """回傳 (剔除未完成 K 線後的價格, 被剔除的檔數)。now 預設為現在（UTC）。"""
    now = now or dt.datetime.now(dt.timezone.utc)
    out, dropped = {}, 0
    for t, df in prices.items():
        ex = exchange_of(t)
        if ex is None or df is None or not len(df):
            out[t] = df
            continue
        tz, close = EXCHANGES[ex]
        local = now.astimezone(tz)
        session_done = dt.datetime.combine(local.date(), close, tzinfo=tz) + dt.timedelta(minutes=grace_min)
        if df.index[-1].date() == local.date() and now < session_done:
            df = df.iloc[:-1]
            dropped += 1
        out[t] = df
    return out, dropped


def _nth_weekday(y: int, m: int, wd: int, n: int) -> dt.date:
    d = dt.date(y, m, 1)
    d += dt.timedelta(days=(wd - d.weekday()) % 7 + 7 * (n - 1))
    return d


def nyse_holidays(year: int) -> set[dt.date]:
    """紐約證交所休市日（依規則推算；週六休市提前到週五、週日休市順延到週一，元旦遇週六不補）。"""
    from dateutil.easter import easter
    obs = lambda d: d - dt.timedelta(days=1) if d.weekday() == 5 else d + dt.timedelta(days=1) if d.weekday() == 6 else d
    out = {_nth_weekday(year, 1, 0, 3), _nth_weekday(year, 2, 0, 3), easter(year) - dt.timedelta(days=2),
           dt.date(year, 5, 31) - dt.timedelta(days=dt.date(year, 5, 31).weekday()),
           _nth_weekday(year, 9, 0, 1), _nth_weekday(year, 11, 3, 4), obs(dt.date(year, 7, 4)), obs(dt.date(year, 12, 25))}
    if year >= 2022:
        out.add(obs(dt.date(year, 6, 19)))
    ny = dt.date(year, 1, 1)
    if ny.weekday() != 5:
        out.add(obs(ny))
    return out


def closed_days(market: str, tw_holidays: set[dt.date] | None, years: range | list[int]) -> set[dt.date]:
    """該市場已知的休市日（週末另外處理）。台股由證交所公告的休市表提供，未提供就視為沒有。"""
    if market == "US":
        return set().union(*(nyse_holidays(y) for y in years))
    return set(tw_holidays or ())
