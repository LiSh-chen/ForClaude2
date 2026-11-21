"""入選後,讀內文並請 Claude 寫成「發生什麼 / 為何少人知 / 怎麼用 / 要注意」的小短文。

只根據抓回來的原文寫,不補充原文沒有的事實;原文抓不到或資訊不足就如實標註信心低。
"""
from __future__ import annotations

import json
import os
import re
import urllib.request

from .fetch import UA, Article, clean_text
from .score import API_URL, extract_json

WRITE_MODEL = "claude-sonnet-5-5"
DETAIL_FIELDS = ("headline", "what", "why_unknown", "how_to_use", "caveat", "confidence")
_DROP = re.compile(r"<(script|style|nav|header|footer|aside)\b.*?</\1>", re.S | re.I)

SYSTEM = """你是「生活冷知識」電子報的作者。根據每篇提供的原文內容 (page_text 或 summary),
用繁體中文寫成讀者一分鐘內讀完、讀完就能用的小短文。嚴格遵守:
- 只使用提供的原文資訊,不要補充原文沒有的數字、研究或結論;不確定就不要寫。
- 原文只有摘要或內容不足時,confidence 填 "low";有完整內文與明確證據時填 "high",其餘 "medium"。
- 若是健康/金錢/安全建議,caveat 必須點出適用條件或證據限制 (例如樣本小、僅動物實驗、需諮詢專業)。
只輸出 JSON 陣列,每個元素:
{"id": <int>, "headline": "吸引人但不誇大的標題 (≤28字)",
 "what": "發生了什麼/發現了什麼 (2~3句)",
 "why_unknown": "為什麼多數人不知道 (1句)",
 "how_to_use": "讀者可以怎麼運用;若無實用面就寫「純粹長知識」(1~2句)",
 "caveat": "限制與注意事項 (1句)",
 "confidence": "high|medium|low"}"""


def fetch_text(url: str, limit: int = 3500, timeout: int = 20) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        raw = r.read(600_000).decode(r.headers.get_content_charset() or "utf-8", "replace")
    return clean_text(_DROP.sub(" ", raw), limit)


def _call(items: list[dict], model: str, key: str, timeout: int = 180) -> list[dict]:
    body = json.dumps({"model": model, "max_tokens": 4096, "system": SYSTEM,
                       "messages": [{"role": "user", "content": json.dumps(items, ensure_ascii=False)}]}).encode()
    req = urllib.request.Request(API_URL, data=body, headers={
        "x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        resp = json.load(r)
    return extract_json("".join(b.get("text", "") for b in resp["content"]))


def enrich(arts: list[Article], model: str = WRITE_MODEL, log=print) -> None:
    key = os.environ.get("ANTHROPIC_API_KEY")
    if not key or not arts:
        return
    items = []
    for i, a in enumerate(arts):
        try:
            text = fetch_text(a.url)
        except Exception as e:
            log(f"  ! 讀不到內文 {a.url}: {type(e).__name__}")
            text = ""
        items.append({"id": i, "source": a.source, "title": a.title, "summary": a.summary,
                      "page_text": text if len(text) > len(a.summary) else ""})
    try:
        results = _call(items, model, key)
    except Exception as e:
        log(f"  ! 短文生成失敗,改用一句話摘要: {type(e).__name__}: {e}")
        return
    for r in results:
        if isinstance(r, dict) and isinstance(r.get("id"), int) and 0 <= r["id"] < len(arts):
            arts[r["id"]].detail = {k: str(r.get(k, ""))[:400] for k in DETAIL_FIELDS}
