"""題庫:每份 topics 模式的電子報有 newsletters/<name>/topics.json。

status: new(未用) → queued(已送去評分/審稿) / rejected(摘要不支持或評分太低) / no_papers(PubMed 找不到)
"""
from __future__ import annotations

import json
from pathlib import Path

from .profiles import Profile

STATUSES = ("new", "queued", "rejected", "no_papers")


def path_for(p: Profile) -> Path:
    return p.sources.parent / "topics.json"


def load(p: Profile) -> dict:
    f = path_for(p)
    return json.loads(f.read_text(encoding="utf-8")) if f.exists() else {"topics": []}


def save(p: Profile, data: dict) -> None:
    path_for(p).write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


def pick_new(data: dict, n: int) -> list[dict]:
    """從未使用的題目中輪流取各分類,避免一次全是同一領域。"""
    by_cat: dict[str, list[dict]] = {}
    for t in data["topics"]:
        if t.get("status", "new") == "new":
            by_cat.setdefault(t.get("category", ""), []).append(t)
    out = []
    while len(out) < n and any(by_cat.values()):
        for cat in list(by_cat):
            if by_cat[cat] and len(out) < n:
                out.append(by_cat[cat].pop(0))
    return out


def set_status(data: dict, ids: list[str], status: str) -> None:
    for t in data["topics"]:
        if t["id"] in ids:
            t["status"] = status


def remaining(data: dict) -> int:
    return sum(1 for t in data["topics"] if t.get("status", "new") == "new")
