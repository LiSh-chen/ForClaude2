"""電子報定義 (Profile) —— 從 newsletters/<name>/ 載入,引擎本身不含任何題材。

目錄內容:
  newsletter.json  名稱、標題、權重、扣分項、門檻、數量…
  rubric.md        評分標準 (給 LLM 的說明);輸出格式由程式依維度自動附加,改 rubric 不會弄壞格式
  sources.json     RSS/Atom 來源
新增一份電子報:`python -m curator init <name> --brief "..."` 或複製現有目錄後修改。
"""
from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NEWSLETTERS = ROOT / "newsletters"


class ProfileError(ValueError):
    pass


@dataclass(frozen=True)
class Profile:
    name: str
    title: str                      # 電子報名稱
    tagline: str
    rubric: str                     # 評分標準說明
    weights: dict                   # 正向維度 -> 權重 (總和 1)
    penalties: dict                 # 扣分維度 -> 最大扣分比例
    sources: Path
    seen_file: Path
    min_score: float = 55.0
    top: int = 7
    per_source: int = 2
    subject_prefix: str = "精選"
    enrich: bool = False            # 入選後是否再請 Claude 讀內文,寫成小短文
    categories: tuple = ()
    interests: str = ""             # 讀者關注主題 (可被 --interests 覆蓋)
    labels: dict = field(default_factory=dict)

    @property
    def system(self) -> str:
        """評分用 system prompt = rubric + 依維度自動產生的輸出格式。"""
        dims = ", ".join(f'"{k}": n' for k in (*self.weights, *self.penalties))
        cat = f'\n "category": "{"|".join(self.categories)} 擇一",' if self.categories else ""
        return (f"{self.rubric}\n\n只輸出 JSON 陣列,不要其他文字。每個元素:\n"
                f'{{"id": <int>, {dims},{cat}\n'
                ' "one_liner": "一句繁體中文,說明這篇在講什麼 (≤40字)",\n'
                ' "why": "一句繁體中文,說明為什麼值得讀 (≤60字,具體,不要空話)"}')


def validate(cfg: dict) -> None:
    """設定檔常見錯誤要在載入時就報,而不是寄信時才爆。"""
    for k in ("name", "title", "weights"):
        if not cfg.get(k):
            raise ProfileError(f"缺少欄位 {k}")
    if not str(cfg["name"]).replace("-", "").replace("_", "").isalnum():
        raise ProfileError("name 只能用英數、- 與 _")
    w, pen = cfg["weights"], cfg.get("penalties", {})
    if abs(sum(w.values()) - 1) > 0.01:
        raise ProfileError(f"weights 總和須為 1,目前 {sum(w.values()):.2f}")
    if set(w) & set(pen):
        raise ProfileError("weights 與 penalties 的維度名稱不可重複")
    if not all(0 < v <= 0.6 for v in pen.values()):
        raise ProfileError("penalties 每項須介於 0~0.6")
    if not 0 <= cfg.get("min_score", 55) <= 100:
        raise ProfileError("min_score 須介於 0~100")


def load(directory: Path) -> Profile:
    cfg = json.loads((directory / "newsletter.json").read_text(encoding="utf-8"))
    validate(cfg)
    name = cfg["name"]
    return Profile(
        name=name, title=cfg["title"], tagline=cfg.get("tagline", ""),
        rubric=(directory / "rubric.md").read_text(encoding="utf-8").strip(),
        weights=cfg["weights"], penalties=cfg.get("penalties", {}),
        sources=directory / cfg.get("sources", "sources.json"),
        seen_file=ROOT / cfg.get("seen_file", f"data/seen_{name}.json"),
        min_score=cfg.get("min_score", 55.0), top=cfg.get("top", 7), per_source=cfg.get("per_source", 2),
        subject_prefix=cfg.get("subject_prefix", cfg["title"]), enrich=cfg.get("enrich", False),
        categories=tuple(cfg.get("categories", ())), interests=cfg.get("interests", ""),
    )


def names() -> list[str]:
    return sorted(p.parent.name for p in NEWSLETTERS.glob("*/newsletter.json"))


def get(name: str) -> Profile:
    d = NEWSLETTERS / name
    if not (d / "newsletter.json").exists():
        raise ProfileError(f"找不到電子報 {name!r};現有:{', '.join(names()) or '(無)'}")
    return load(d)


def resolve(profile: Profile | None) -> Profile:
    """未指定時用 insight (舊行為);沒有 insight 就用第一份。"""
    return profile or get("insight" if "insight" in names() else names()[0])
