"""數字檢查:短文裡出現的每個數字,都必須能在引用的論文摘要中找到。找不到就標記,審稿時一眼可見。

這是用來抓「模型憑記憶寫出的數字」,不能證明內容正確;摘要有該數字只代表有出處。
"""
from __future__ import annotations

import re

_NUM = re.compile(r"(\d+(?:,\d{3})*(?:\.\d+)?)\s*(萬|万|千|百分之)?")
_UNIT = {"萬": 1e4, "万": 1e4, "千": 1e3}


def numbers(text: str) -> list[float]:
    out = []
    for m in _NUM.finditer(text or ""):
        v = float(m.group(1).replace(",", ""))
        out.append(v * _UNIT.get(m.group(2), 1))
    return out


def _is_year(v: float) -> bool:
    return v == int(v) and 1900 <= v <= 2100


def check(fields: dict[str, str], source: str) -> list[str]:
    """回傳在 source 找不到的數字描述。數值 ≥100 容許 6% 誤差 (中文常寫「近 20 萬」)。"""
    src = numbers(source)
    flags = []
    for name, text in fields.items():
        for m in _NUM.finditer(text or ""):
            raw = m.group(0).strip()
            v = float(m.group(1).replace(",", "")) * _UNIT.get(m.group(2), 1)
            if _is_year(v):
                continue
            ok = any(abs(v - s) <= (0.06 * s if v >= 100 else 1e-9) for s in src)
            if not ok:
                flags.append(f"{name}:{raw}")
    return flags
