"""替文章打「價值分」。

核心想法:不是看熱度,而是問四件事 (各 0~10):
  novelty  新穎度:是新發現/新觀點,還是老調重彈、例行公告?
  depth    洞見深度:是否解釋了「為什麼/如何」,而不只是「發生了什麼」?
  impact   影響力:對人類、社會、科學、產業的長期影響有多大 (不是今天有多吵)?
  evidence 證據品質:有一手資料、研究、實地調查,還是轉述與臆測?
並扣掉「瑣事」(trivia, 0~10):名人八卦、政治口水戰、單日漲跌、情緒煽動、
促銷與清單文。

有 ANTHROPIC_API_KEY 時用 Claude 評分;沒有時退回關鍵字啟發式 (品質較差)。
"""
from __future__ import annotations

import json
import os
import re
import urllib.request

from .fetch import Article
from .profiles import Profile, resolve

API_URL = "https://api.anthropic.com/v1/messages"
DEFAULT_MODEL = "claude-haiku-4-5-20251001"


def compose(d: dict, profile: Profile | None = None) -> float:
    """維度分數 -> 0~100 的總分。扣分維度各以其最大比例連乘折減。"""
    profile = resolve(profile)
    base = sum(d.get(k, 0) * w for k, w in profile.weights.items()) * 10
    for k, frac in profile.penalties.items():
        base *= 1 - frac * d.get(k, 0) / 10
    return round(max(0.0, base), 1)


def extract_json(text: str):
    m = re.search(r"\[.*\]", text, re.S)
    if not m:
        raise ValueError("回應中找不到 JSON 陣列")
    return json.loads(m.group(0))


def _call_claude(items: list[dict], model: str, api_key: str, system: str, timeout: int = 120) -> list[dict]:
    body = json.dumps({
        "model": model,
        "max_tokens": 4096,
        "system": system,
        "messages": [{"role": "user", "content": json.dumps(items, ensure_ascii=False)}],
    }).encode()
    req = urllib.request.Request(API_URL, data=body, headers={
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    })
    with urllib.request.urlopen(req, timeout=timeout) as r:
        resp = json.load(r)
    return extract_json("".join(b.get("text", "") for b in resp["content"]))


def score_with_llm(arts: list[Article], model: str = DEFAULT_MODEL, batch: int = 20, log=print,
                   profile: Profile | None = None, interests: str = "") -> None:
    profile = resolve(profile)
    key = os.environ["ANTHROPIC_API_KEY"]
    system = profile.system + (f"\n\n讀者特別關注:{interests}。相關主題可略微加分,但不相關的好內容仍應給高分。" if interests else "")
    for i in range(0, len(arts), batch):
        chunk = arts[i:i + batch]
        items = [{"id": j, "source": a.source, "title": a.title, "summary": a.summary[:400]}
                 for j, a in enumerate(chunk)]
        try:
            results = _call_claude(items, model, key, system)
        except Exception as e:
            log(f"  ! LLM 評分失敗 (批次 {i // batch + 1}): {type(e).__name__}: {e}")
            score_heuristic(chunk, profile)
            continue
        by_id = {r.get("id"): r for r in results if isinstance(r, dict)}
        for j, a in enumerate(chunk):
            r = by_id.get(j)
            if not r:
                score_heuristic([a], profile)
                continue
            a.dims = {k: max(0, min(10, int(r.get(k, 0)))) for k in (*profile.weights, *profile.penalties)}
            a.score = compose(a.dims, profile)
            a.category = str(r.get("category", ""))[:8]
            a.one_liner = str(r.get("one_liner", ""))[:80]
            a.why = str(r.get("why", ""))[:120]


# ---- 沒有 API key 時的退路 --------------------------------------------------
_TRIVIA = re.compile(
    r"(top \d+|\d+ (best|ways|things)|celebrity|gossip|kardashian|trump says|slams|blasts|"
    r"stock(s)? (rise|fall|jump|slide)|shares (rise|fall)|deal of|coupon|"
    r"八卦|爆料|怒轟|開轟|大罵|懶人包|優惠|折扣|股價(大漲|大跌|收)|網友)", re.I)
_SUBSTANCE = re.compile(
    r"(study|researchers?|discover|paper|data|evidence|experiment|trial|mechanism|"
    r"proof|theorem|first (time|ever)|breakthrough|analysis|investigation|"
    r"研究|發現|實驗|證據|調查|數據|機制|證明|首度|首次|突破|分析)", re.I)


_PRACTICAL = re.compile(
    r"(study|researchers?|found|surprising|actually|myth|hidden|tip|how to|why|health|sleep|food|money|"
    r"safe|cheap|home|study shows|原來|其實|迷思|研究|發現|冷知識|竟然|健康|睡眠|飲食|省錢|居家|安全|怎麼|為什麼)", re.I)


def score_heuristic(arts: list[Article], profile: Profile | None = None) -> None:
    profile = resolve(profile)
    for a in arts:
        text = f"{a.title} {a.summary}"
        pat = _PRACTICAL if "practical" in profile.weights else _SUBSTANCE
        sub = min(len(pat.findall(text)), 4) / 4
        triv = 1.0 if _TRIVIA.search(text) else 0.0
        length = min(len(a.summary) / 400, 1.0)
        a.score = round(100 * max(0.0, 0.45 * a.source_weight + 0.35 * sub + 0.2 * length - 0.4 * triv), 1)
        a.dims = {}
        a.one_liner = a.title
        a.why = f"來自 {a.source},內容含研究/資料線索" if sub >= 0.5 else f"來自 {a.source}"


def score_all(arts: list[Article], model: str = DEFAULT_MODEL, log=print,
              profile: Profile | None = None, interests: str = "") -> str:
    """回傳實際使用的評分方式 ('llm' | 'heuristic')。"""
    profile = resolve(profile)
    if os.environ.get("ANTHROPIC_API_KEY"):
        score_with_llm(arts, model, log=log, profile=profile, interests=interests)
        return "llm"
    log("  ! 未設定 ANTHROPIC_API_KEY,使用啟發式評分 (品質有限)")
    score_heuristic(arts, profile)
    return "heuristic"


def select(arts: list[Article], top_n: int = 7, min_score: float = 55.0, per_source: int = 2) -> list[Article]:
    """取高分、設門檻 (寧缺勿濫)、限制單一來源數量以維持多樣性。"""
    picked, count = [], {}
    for a in sorted(arts, key=lambda x: x.score, reverse=True):
        if a.score < min_score or count.get(a.source, 0) >= per_source:
            continue
        picked.append(a)
        count[a.source] = count.get(a.source, 0) + 1
        if len(picked) >= top_n:
            break
    return picked
