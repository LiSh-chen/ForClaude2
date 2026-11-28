"""用 NCBI E-utilities 查 PubMed 摘要 (免費、不需 key;請勿高頻呼叫)。"""
from __future__ import annotations

import json
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

EUTILS = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"
TOOL = "news-curator"
# 證據等級排序:系統性回顧/統合分析 > 隨機對照試驗 > 其他臨床試驗 > 一般
_RANK = {"Meta-Analysis": 4, "Systematic Review": 4, "Randomized Controlled Trial": 3,
         "Clinical Trial": 2, "Review": 1}


def _get(url: str, timeout: int = 25) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": f"{TOOL}/0.1"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read()


def search(query: str, n: int = 8, pause: float = 0.4) -> list[str]:
    q = urllib.parse.urlencode({"db": "pubmed", "retmode": "json", "retmax": n, "sort": "relevance",
                                "term": query, "tool": TOOL})
    data = json.loads(_get(f"{EUTILS}/esearch.fcgi?{q}"))
    time.sleep(pause)
    return data.get("esearchresult", {}).get("idlist", [])


def parse_papers(xml_bytes: bytes) -> list[dict]:
    out = []
    for art in ET.fromstring(xml_bytes).iter("PubmedArticle"):
        pmid = (art.findtext(".//MedlineCitation/PMID") or "").strip()
        title = "".join(art.find(".//ArticleTitle").itertext()).strip() if art.find(".//ArticleTitle") is not None else ""
        parts = []
        for ab in art.findall(".//Abstract/AbstractText"):
            txt = "".join(ab.itertext()).strip()
            if txt:
                parts.append(f"{ab.get('Label')}: {txt}" if ab.get("Label") else txt)
        year = (art.findtext(".//JournalIssue/PubDate/Year") or
                (art.findtext(".//JournalIssue/PubDate/MedlineDate") or "")[:4])
        types = [t.text for t in art.findall(".//PublicationType") if t.text]
        out.append({"pmid": pmid, "title": title, "journal": art.findtext(".//Journal/Title") or "",
                    "year": year, "types": types, "abstract": " ".join(parts),
                    "url": f"https://pubmed.ncbi.nlm.nih.gov/{pmid}/"})
    return out


def fetch(pmids: list[str], pause: float = 0.4) -> list[dict]:
    if not pmids:
        return []
    q = urllib.parse.urlencode({"db": "pubmed", "retmode": "xml", "id": ",".join(pmids), "tool": TOOL})
    papers = parse_papers(_get(f"{EUTILS}/efetch.fcgi?{q}"))
    time.sleep(pause)
    return papers


def rank(papers: list[dict], keep: int = 3, abstract_limit: int = 2500) -> list[dict]:
    """只留有摘要的;證據等級高者優先,其次較新。"""
    usable = [p for p in papers if len(p["abstract"]) > 200]
    usable.sort(key=lambda p: (max((_RANK.get(t, 0) for t in p["types"]), default=0), p["year"]), reverse=True)
    return [{**p, "abstract": p["abstract"][:abstract_limit]} for p in usable[:keep]]


def find(query: str, keep: int = 3) -> list[dict]:
    return rank(fetch(search(query)), keep)
