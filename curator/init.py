"""建立新電子報:複製現有的,或用一段話請 Claude 產出定義草稿 (必須人工審過)。"""
from __future__ import annotations

import json
import os
import re
import shutil
import urllib.request
from pathlib import Path

from . import fetch, profiles
from .score import API_URL

META_SYSTEM = """你是電子報策展系統的設計師。使用者會用一段話描述他的電子報:讀者是誰、什麼內容有價值、什麼算雜訊。
請產出這份電子報的「定義」,只輸出一個 JSON 物件 (不要其他文字),欄位:
{
 "name": "英數與-_ 的短代號",
 "title": "電子報名稱", "tagline": "一句話承諾 (讀者看到的)", "subject_prefix": "信件標題前綴 (≤4字)",
 "weights": {"維度英文名": 權重, ...},   // 3~5 個「越高越好」的維度,權重總和=1
 "penalties": {"維度英文名": 0.3~0.5},    // 1~3 個「越高越要扣分」的維度 (例如已廣為人知、瑣事/業配)
 "min_score": 45~60, "top": 3~7, "per_source": 2, "enrich": true,
 "categories": ["3~8 個讀者看得懂的分類"],
 "rubric": "給評分者的說明:每個維度 0~10 分代表什麼、哪些給低分、對不可靠資訊要保守。繁體中文,條列。",
 "feeds": [{"name": "...", "url": "...", "weight": 0.5~0.9}]   // 8~12 個你確定真實存在的 RSS/Atom 網址;不確定的不要列
}
維度名稱用英文小寫單字,且 evidence (證據品質) 一定要有。若題材涉及健康、金錢、法律、安全,rubric 必須要求對缺乏證據的說法給低分。"""


def _write(directory: Path, cfg: dict, feeds: list[dict], rubric: str) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    (directory / "rubric.md").write_text(rubric.strip() + "\n", encoding="utf-8")
    (directory / "sources.json").write_text(
        json.dumps({"_comment": "網址未經驗證,請執行 python -m curator check", "feeds": feeds}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")
    (directory / "newsletter.json").write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def parse_definition(text: str) -> tuple[dict, list[dict], str]:
    m = re.search(r"\{.*\}", text, re.S)
    if not m:
        raise profiles.ProfileError("回應中找不到 JSON")
    d = json.loads(m.group(0))
    rubric, feeds = d.pop("rubric", ""), d.pop("feeds", [])
    profiles.validate(d)
    if "evidence" not in d["weights"]:
        raise profiles.ProfileError("weights 必須包含 evidence")
    if not rubric.strip():
        raise profiles.ProfileError("缺少 rubric")
    feeds = [f for f in feeds if isinstance(f, dict) and str(f.get("url", "")).startswith("http") and f.get("name")]
    return d, feeds, rubric


def generate(brief: str, name: str, model: str, timeout: int = 180) -> Path:
    key = os.environ["ANTHROPIC_API_KEY"]
    body = json.dumps({"model": model, "max_tokens": 4096, "system": META_SYSTEM,
                       "messages": [{"role": "user", "content": brief}]}).encode()
    req = urllib.request.Request(API_URL, data=body, headers={
        "x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        resp = json.load(r)
    cfg, feeds, rubric = parse_definition("".join(b.get("text", "") for b in resp["content"]))
    cfg["name"] = name
    cfg["sources"] = "sources.json"
    d = profiles.NEWSLETTERS / name
    _write(d, cfg, feeds, rubric)
    return d


def copy_from(src: str, name: str) -> Path:
    s, d = profiles.NEWSLETTERS / src, profiles.NEWSLETTERS / name
    if not (s / "newsletter.json").exists():
        raise profiles.ProfileError(f"找不到來源電子報 {src!r}")
    shutil.copytree(s, d)
    cfg = json.loads((d / "newsletter.json").read_text(encoding="utf-8"))
    cfg["name"] = name
    cfg.pop("seen_file", None)          # 不沿用原本的已寄紀錄
    (d / "newsletter.json").write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return d


def check_sources(prof: profiles.Profile, prune: bool = False, log=print) -> tuple[int, int]:
    """逐一連線驗證來源;prune=True 時把失敗的從 sources.json 移除。回傳 (成功, 總數)。"""
    data = json.loads(prof.sources.read_text(encoding="utf-8"))
    good = []
    for f in data["feeds"]:
        try:
            n = len(fetch.parse_feed(fetch.fetch_feed(f["url"]), f["name"]))
            log(f"  ✓ {f['name']}: {n} 篇" if n else f"  ✗ {f['name']}: 連得上但沒有文章")
            if n:
                good.append(f)
        except Exception as e:
            log(f"  ✗ {f['name']}: {type(e).__name__}: {e}")
    if prune and good != data["feeds"]:
        data["feeds"] = good
        prof.sources.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(good), len(data["feeds"])
