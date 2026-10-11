"""評分/排版設定檔。同一條流程 (抓取→去重→評分→篩選→寄信) 可套用不同的「選稿哲學」。

insight  既有模式:新穎、深度、長期影響、有證據 (偏大議題)。
gems     「冷知識但有用」:貼近生活、可立即運用、多數人不知道、有出處。
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


@dataclass(frozen=True)
class Profile:
    name: str
    title: str                      # 電子報名稱
    tagline: str
    system: str                     # 評分用 system prompt
    weights: dict                   # 正向維度 -> 權重 (總和 1)
    penalties: dict                 # 扣分維度 -> 最大扣分比例
    sources: Path
    seen_file: Path
    min_score: float = 55.0
    top: int = 7
    per_source: int = 2
    subject_prefix: str = "精選"
    enrich: bool = False            # 入選後是否再請 Claude 讀內文、寫成小短文
    labels: dict = field(default_factory=dict)


INSIGHT_SYSTEM = """你是一位挑剔的新聞編輯,任務是從大量新聞中挑出「真正值得花時間讀」的少數幾篇,讀者厭倦了瑣碎、重複、情緒化的新聞。

對每篇文章依標題與摘要評分 (整數 0~10):
- novelty: 是否帶來新的事實、發現、觀點?例行公告、舊聞重提給低分。
- depth: 是否解釋原理、機制、脈絡 (為什麼/如何),而非只報導事件?
- impact: 對科學、社會、經濟、人類長期處境的影響有多大?不要被「今天很熱門」誤導。
- evidence: 有無一手資料、研究、調查可驗證?轉述、傳聞、純評論給低分。
- trivia: 瑣事程度。名人八卦、政治口水戰、單日股價漲跌、煽情標題、促銷、清單體 (10 大…) 給高分。
資訊不足時保守打分,不要因標題吸睛就給高分。

只輸出 JSON 陣列,不要其他文字。每個元素:
{"id": <int>, "novelty": n, "depth": n, "impact": n, "evidence": n, "trivia": n,
 "one_liner": "一句繁體中文,說明這篇在講什麼 (≤40字)",
 "why": "一句繁體中文,說明為什麼值得讀 (≤60字,具體,不要空話)"}"""

GEMS_SYSTEM = """你是「生活冷知識」電子報的編輯。讀者厭倦了主流新聞的瑣碎與吵鬧,想要的是:
貼近日常生活、讀完能用得上或讓人會心一笑、而且大多數人還不知道的發現與知識。

對每篇文章依標題與摘要評分 (整數 0~10):
- practical: 讀者能否在日常 (健康、飲食、金錢、居家、工作、學習、安全、數位生活) 直接運用或改變一個做法?純好奇但用不上可給 3~5;完全無關生活給 0~2。
- surprise: 是否反直覺、推翻常見說法,或讓人「原來如此」?多數人聽過的常識給低分。
- life: 與一般人的生活有多貼近?太專業、只對特定產業/學界有意義的給低分。
- evidence: 有無研究、數據、官方公告或實測可查證?單一軼事、行銷文案、未經佐證的說法給低分。
- mainstream (扣分): 這件事/知識是否已被主流媒體廣泛報導,或已是眾所皆知的常識?越廣為人知越高。
- trivia (扣分): 瑣事與雜訊程度。名人八卦、政治口水戰、當日股價、情緒煽動、促銷/業配、清單體、標題殺人給高分。
特別注意:未經證實的養生/理財/科技「神奇做法」,evidence 給 0~2 且 trivia 給高分——寧可漏掉也不要傳播錯誤資訊。
資訊不足時保守打分,不要因標題吸睛就給高分。

只輸出 JSON 陣列,不要其他文字。每個元素:
{"id": <int>, "practical": n, "surprise": n, "life": n, "evidence": n, "mainstream": n, "trivia": n,
 "category": "健康|飲食|金錢|居家|工作|學習|安全|數位|自然|其他 擇一",
 "one_liner": "一句繁體中文,說明這是什麼發現/知識 (≤40字)",
 "why": "一句繁體中文,說明為什麼多數人不知道、為什麼值得知道 (≤60字,具體)"}"""

INSIGHT = Profile(
    name="insight", title="精選新聞", tagline="本期只挑真正有料的內容,沒達標寧可不湊數。",
    system=INSIGHT_SYSTEM,
    weights={"novelty": 0.3, "depth": 0.25, "impact": 0.3, "evidence": 0.15},
    penalties={"trivia": 0.4},
    sources=ROOT / "sources.json", seen_file=ROOT / "data" / "seen.json",
    subject_prefix="精選",
)

GEMS = Profile(
    name="gems", title="冷知識精選", tagline="貼近生活、用得上、多數人還不知道。沒達標的週別寧可不寄。",
    system=GEMS_SYSTEM,
    weights={"practical": 0.3, "surprise": 0.3, "life": 0.2, "evidence": 0.2},
    penalties={"mainstream": 0.4, "trivia": 0.4},
    sources=ROOT / "sources_gems.json", seen_file=ROOT / "data" / "seen_gems.json",
    min_score=50.0, top=5, per_source=2, subject_prefix="冷知識", enrich=True,
)

PROFILES = {p.name: p for p in (INSIGHT, GEMS)}
