"""推薦 SOP 的輔助規則（由績效檢討得出，詳見 reviews/ 的 SOP 檢討與 docs/methodology.md 第 19 節）。"""
from __future__ import annotations
import datetime as dt

from . import config as C
from .utils import isnum


def cooldown(feats: dict, date: str, params: dict) -> dict[str, dict]:
    """停損記憶：曾經推薦過、之後收盤價跌破『首次推薦時的參考停損價』的標的＝推薦論點已被市場否定，收復停損價前不再推薦。
    （檢討發現：163 筆推薦中有 48 筆是『已跌破首次推薦停損價、仍被重複推薦』的標的，平均超額 −8%。）"""
    days = params["selection"].get("cooldown_days", 0)
    if not days:
        return {}
    today = dt.date.fromisoformat(date)
    first: dict[str, dict] = {}
    for sub in ("picks", "emerging", "bargain"):
        d = C.path("data") / sub
        for f in sorted(d.glob("*.json")) if d.exists() else []:
            if not (today - dt.timedelta(days=days) <= dt.date.fromisoformat(f.stem) < today):
                continue
            for p in (C.load_json(f) or {}).get("picks", []):
                if isnum(p.get("stop")) and p["ticker"] not in first:
                    first[p["ticker"]] = {"date": f.stem, "stop": p["stop"], "entry": p.get("entry_price")}
    out = {}
    for t, x in first.items():
        pf = feats.get(t)
        if pf and pf["last"] < x["stop"]:
            out[t] = {**x, "last": pf["last"]}
    return out
