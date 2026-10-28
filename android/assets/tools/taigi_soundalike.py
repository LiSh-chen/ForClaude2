#!/usr/bin/env python3
"""Checks the Mandarin "sound-alike" (空耳) written for a Taiwanese place name against its Tâi-lô spelling.

Each Tâi-lô syllable is turned into the Mandarin syllable that sounds closest (rules below, ending with the tone numbers
that sound alike: Taiwanese 1 high level ~ Mandarin 1, 2 high falling ~ 4, 3 low falling ~ 3 or 4, 5 rising ~ 2,
7 mid level ~ 4 or 1, 8 short high ~ 2 or 4, 4 short low ~ 4), and the characters chosen must read like that.

    python3 taigi_soundalike.py            # checks the pairs listed in res/raw/local_names.txt
    python3 taigi_soundalike.py Káu-hūn    # shows the Mandarin syllables for a spelling

Needs pypinyin (pip install pypinyin). It does not choose the characters: that is a human decision.
"""
import re, sys, pathlib, unicodedata
from pypinyin import pinyin, Style

TONE_MARKS = {"́": 2, "̀": 3, "̂": 5, "̄": 7, "̍": 8}
INITIALS = [("tsh", "c"), ("ts", "z"), ("ph", "p"), ("th", "t"), ("kh", "k"), ("ng", "ng"), ("p", "b"), ("t", "d"), ("k", "g"),
            ("m", "m"), ("n", "n"), ("l", "l"), ("s", "s"), ("h", "h"), ("b", "b"), ("g", "g"), ("j", "l")]
FINALS = {  # Tâi-lô final (without checked ending) -> pinyin final
    "a": "a", "an": "an", "am": "an", "ang": "ang", "ann": "an", "ai": "ai", "ainn": "ai", "au": "ao", "aunn": "ao",
    "i": "i", "in": "in", "im": "in", "ing": "ing", "inn": "i", "ia": "ia", "iann": "ia", "iau": "iao", "iu": "iu", "ian": "ian", "iam": "ian",
    "iang": "iang", "iong": "iong", "io": "io", "iok": "io",
    "u": "u", "un": "un", "ui": "ui", "ua": "ua", "uai": "uai", "uan": "uan", "uann": "uan",
    "e": "ei", "ee": "ei", "eng": "eng", "enn": "ei", "o": "o", "oo": "o", "ong": "ong", "oa": "ua", "oe": "uei",
    "ng": "ong", "m": "mu",
}
TONE_MAP = {1: [1], 2: [4], 3: [3, 4], 4: [4], 5: [2], 7: [4, 1], 8: [2, 4]}

def syllable(s: str):
    """Mandarin candidates (pinyin with tone numbers) for one Tâi-lô syllable, and its Taiwanese tone."""
    d = unicodedata.normalize("NFD", s.lower())
    tone = next((TONE_MARKS[c] for c in d if c in TONE_MARKS), 1)
    base = "".join(c for c in d if c not in TONE_MARKS)
    base = unicodedata.normalize("NFC", base)
    checked = base[-1] in "ptkh" and len(base) > 1 and base[-1] != "h" or base.endswith("h") and len(base) > 1 and base[-2] in "aeiou"
    if checked and tone == 1: tone = 4
    core = base[:-1] if checked else base
    ini = ""
    for t, m in INITIALS:
        if core.startswith(t) and (len(core) > len(t) or t in ("ng", "m")):
            ini, core = m, core[len(t):]; break
    fin = FINALS.get(core, core)
    if fin == "o" and ini not in ("b", "p", "m", ""): fin = "uo"        # Mandarin has no "to", "zo" ...: it is "tuo", "zuo"
    if fin == "io": fin = "iao"
    if fin == "ang" and ini in ("b", "p", "m") and core == "ang": fin = "ang"
    if ini in ("g", "k", "h", "z", "c", "s", "b") and fin[:1] == "i":   # palatalised before i
        ini = {"g": "j", "k": "q", "h": "x", "z": "j", "c": "q", "s": "x"}.get(ini, ini)
    return [f"{ini}{fin}{t}" for t in TONE_MAP.get(tone, [tone])], tone

# readings the dictionary behind pypinyin leaves out but Mandarin speakers use (checked in the Ministry of Education's 國語辭典)
EXTRA = {"啊": {"a1", "a2", "a3", "a4"}, "嚼": {"jiao2"}, "得": {"dei3"}}

def reads(ch: str) -> set:
    return {p[0] for p in pinyin(ch, style=Style.TONE3, neutral_tone_with_five=True, heteronym=True)} | EXTRA.get(ch, set())

_all = None
def exists(syl: str) -> bool:
    """Whether any character reads like [syl] (some Mandarin syllables have no second tone, for instance gu)."""
    global _all
    if _all is None:
        _all = set()
        # only characters the app's own texts already use count as common enough (rare ones read gu2, say, but nobody knows them)
        raw = pathlib.Path(__file__).resolve().parents[2] / "app/src/main/res/raw"
        common = {ch for f in raw.glob("*.txt") for ch in f.read_text(encoding="utf-8") if "\u4e00" <= ch <= "\u9fff"}
        for ch in common: _all |= {x.replace("v", "u") for x in reads(ch)}
        for v in EXTRA.values(): _all |= v
    return syl in _all

def check(roman: str, chars: str):
    syls = roman.split("-")
    problems = []
    if len(syls) != len(chars): return [f"{roman}: {len(syls)} syllables but {len(chars)} characters"]
    for s, ch in zip(syls, chars):
        cand, tone = syllable(s)
        if not any(exists(c) for c in cand):             # nothing sounds like that: the same syllable in the nearest tones will do
            cand = [c[:-1] + str(t) for c in cand for t in (1, 2, 3, 4) if exists(c[:-1] + str(t))]
        got = reads(ch)
        norm = lambda x: x.replace("v", "u")
        if not any(norm(c) in {norm(g) for g in got} for c in cand):
            problems.append(f"{s} (tone {tone}) wants {'/'.join(cand)} but {ch} reads {'/'.join(sorted(got))}")
    return problems

if __name__ == "__main__":
    if len(sys.argv) > 1:
        for s in sys.argv[1].split("-"): print(s, syllable(s))
        sys.exit(0)
    raw = pathlib.Path(__file__).resolve().parents[2] / "app/src/main/res/raw/local_names.txt"
    bad = 0
    for line in raw.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"): continue
        f = line.split("|")
        problems = check(f[3], f[4])
        print(("OK   " if not problems else "FIX  ") + f"{f[2]} {f[3]} -> {f[4]}" + ("" if not problems else "   " + "; ".join(problems)))
        bad += bool(problems)
    sys.exit(1 if bad else 0)
