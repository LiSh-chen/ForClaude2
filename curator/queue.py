"""審稿佇列:系統每天備好候選,人工核准後才寄。

狀態:pending(待審) → approved(核准,等下次寄出) → sent(已寄);或 rejected / expired。
佇列存成 JSON (data/queue/<profile>.json),可直接用編輯器改 detail 文字再核准。
"""
from __future__ import annotations

import json
from dataclasses import asdict
from datetime import date, datetime, timedelta
from pathlib import Path

from .fetch import Article
from .profiles import Profile

ROOT = Path(__file__).resolve().parent.parent
STATUSES = ("pending", "approved", "sent", "rejected", "expired")


def path_for(profile: Profile) -> Path:
    return ROOT / "data" / "queue" / f"{profile.name}.json"


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"next_id": 1, "items": []}


def save(path: Path, q: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(q, ensure_ascii=False, indent=1), encoding="utf-8")


def norm_url(u: str) -> str:
    return u.split("#")[0].rstrip("/")


def add(q: dict, arts: list[Article], today: date | None = None) -> int:
    """加入新候選 (網址已在佇列者略過),回傳新增數。"""
    today = today or date.today()
    have = {norm_url(i["url"]) for i in q["items"]}
    n = 0
    for a in arts:
        if norm_url(a.url) in have:
            continue
        d = asdict(a)
        d["published"] = a.published.isoformat() if a.published else None
        d.update(id=q["next_id"], status="pending", added=today.isoformat())
        q["items"].append(d)
        q["next_id"] += 1
        have.add(norm_url(a.url))
        n += 1
    return n


def expire(q: dict, days: int = 14, today: date | None = None) -> int:
    today = today or date.today()
    n = 0
    for i in q["items"]:
        if i["status"] == "pending" and (today - date.fromisoformat(i["added"])) > timedelta(days=days):
            i["status"] = "expired"
            n += 1
    return n


def set_status(q: dict, ids: list[int], status: str, force: bool = False) -> list[int]:
    """回傳實際變更的 id。只允許從 pending/approved 之間互轉或駁回,已寄的不動。
    有數字檢查旗標 (detail.flags) 的項目,核准時需先修正 (清空 flags) 或 force。"""
    changed = []
    for i in q["items"]:
        if i["id"] in ids and i["status"] in ("pending", "approved", "rejected"):
            if status == "approved" and not force and (i.get("detail") or {}).get("flags"):
                continue
            i["status"] = status
            changed.append(i["id"])
    return changed


def to_article(d: dict) -> Article:
    pub = datetime.fromisoformat(d["published"]) if d.get("published") else None
    return Article(d["title"], d["url"], d["summary"], d["source"], pub, d.get("source_weight", 0.5),
                   d["score"], d["why"], d["one_liner"], d.get("dims", {}), d.get("category", ""), d.get("detail", {}))


def items(q: dict, status: str) -> list[dict]:
    return sorted((i for i in q["items"] if i["status"] == status), key=lambda i: -i["score"])


def format_item(i: dict) -> str:
    d = i.get("detail") or {}
    head = d.get("headline") or i["title"]
    lines = [f"[{i['id']}] {i['score']:g}分 · {i.get('category') or '-'} · {i['source']}  <{i['status']}>", f"    {head}"]
    if d.get("myth"):
        lines += [f"    迷思:{d['myth']}", f"    真相:{d['truth']}", f"    機制:{d['mechanism']}",
                  f"    怎麼做:{d['how_to_use']}", f"    注意:{d['caveat']}",
                  f"    證據:{d['evidence']} ({d['confidence']})", f"    論文:{d.get('source_title', '')}"]
        if d.get("flags"):
            lines.append(f"    ⚠ 摘要中找不到這些數字:{', '.join(d['flags'])}  → 修正文字並清空 flags,或 approve --force")
    elif d:
        lines += [f"    發生什麼:{d['what']}", f"    怎麼用:{d['how_to_use']}",
                  f"    注意:{d['caveat']} ({d['confidence']})"]
    else:
        lines += [f"    {i['one_liner']}", f"    {i['why']}"]
    lines.append(f"    {i['url']}")
    return "\n".join(lines)


def write_review_md(q: dict, path: Path) -> None:
    """產生手機也好讀的待審清單 (GitHub 上直接點開即可)。"""
    pend, appr = items(q, "pending"), items(q, "approved")
    def block(rows): return "\n\n".join("```\n" + format_item(i) + "\n```" for i in rows) or "_(無)_"
    path.write_text(f"# 待審清單\n\n核准:Actions → gems → Run workflow → task=approve, ids=1,3,5\n\n"
                    f"## 已核准,等待寄出 ({len(appr)})\n\n{block(appr)}\n\n## 待審 ({len(pend)})\n\n{block(pend)}\n",
                    encoding="utf-8")
