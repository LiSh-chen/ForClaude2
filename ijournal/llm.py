"""選用：以 Claude 為當日市場與入選個股撰寫敘事評論。未設定 ANTHROPIC_API_KEY 時完全略過。
量化選股與目標價不依賴 LLM（確保可重現、可回測）；LLM 只負責『把證據整理成可讀的研究評論』。"""
from __future__ import annotations
import json
import os
import re

import requests

MODEL = os.environ.get("IJ_LLM_MODEL", "claude-sonnet-5-5")


def available() -> bool:
    return bool(os.environ.get("ANTHROPIC_API_KEY"))


def commentary(context: dict) -> dict | None:
    if not available():
        return None
    prompt = (
        "你是資深賣方研究分析師。以下是今日系統以量化方式產出的事實資料（JSON）。請只根據這些資料撰寫，"
        "不得捏造資料中沒有的數字或新聞。輸出『純 JSON』，格式："
        '{"summary":"250字內的今日市場與產業重點（繁體中文）","comments":{"<ticker>":"80字內的投資論點與主要風險"}}。\n\n'
        + json.dumps(context, ensure_ascii=False)[:60000]
    )
    try:
        r = requests.post("https://api.anthropic.com/v1/messages", timeout=120,
                          headers={"x-api-key": os.environ["ANTHROPIC_API_KEY"], "anthropic-version": "2023-06-01", "content-type": "application/json"},
                          json={"model": MODEL, "max_tokens": 4000, "messages": [{"role": "user", "content": prompt}]})
        r.raise_for_status()
        text = "".join(b.get("text", "") for b in r.json().get("content", []))
        m = re.search(r"\{.*\}", text, re.S)
        return json.loads(m.group(0)) if m else None
    except Exception as e:
        print(f"[llm] 略過：{e}")
        return None


def discover(titles: list[str]) -> list[dict] | None:
    """從沒有命中任何已知關鍵字的標題中，歸納值得追蹤的新興主題線索（未驗證）。"""
    if not available() or not titles:
        return None
    prompt = ("以下是近期財經新聞標題，已排除所有命中既有產業/趨勢關鍵字者。請找出其中『反覆出現、可能代表尚未被主流歸類的新興產業或技術趨勢』的線索，"
              "最多 5 個；只能根據標題內容，不可捏造。輸出純 JSON："
              '{"clues":[{"topic":"主題（15字內）","why":"依據哪些標題、為何值得追蹤（60字內，繁體中文）"}]}\n\n' + "\n".join(titles))
    try:
        r = requests.post("https://api.anthropic.com/v1/messages", timeout=120,
                          headers={"x-api-key": os.environ["ANTHROPIC_API_KEY"], "anthropic-version": "2023-06-01", "content-type": "application/json"},
                          json={"model": MODEL, "max_tokens": 1500, "messages": [{"role": "user", "content": prompt}]})
        r.raise_for_status()
        text = "".join(b.get("text", "") for b in r.json().get("content", []))
        m = re.search(r"\{.*\}", text, re.S)
        return json.loads(m.group(0)).get("clues") if m else None
    except Exception as e:
        print(f"[llm] 線索探索略過：{e}")
        return None
