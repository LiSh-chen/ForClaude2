from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

from . import digest, enrich, fetch, mailer, score, subscribers
from .profiles import PROFILES

ROOT = Path(__file__).resolve().parent.parent

def _load_seen(path: Path) -> set[str]:
    return set(json.loads(path.read_text())) if path.exists() else set()


def _save_seen(path: Path, urls: set[str]) -> None:
    path.parent.mkdir(exist_ok=True)
    path.write_text(json.dumps(sorted(urls)[-5000:], indent=0), encoding="utf-8")


def cmd_run(a) -> int:
    prof = PROFILES[a.profile]
    top = a.top or prof.top
    min_score = prof.min_score if a.min_score is None else a.min_score
    per_source = a.per_source or prof.per_source
    feeds = json.loads(Path(a.sources or prof.sources).read_text(encoding="utf-8"))["feeds"]
    print("抓取來源…")
    arts = fetch.fetch_all(feeds)
    seen = _load_seen(prof.seen_file)
    arts = fetch.dedupe(fetch.recent(arts, a.days), seen)
    print(f"候選文章 (近 {a.days} 天、未寄過): {len(arts)}")
    if not arts:
        return 0
    if a.max_candidates and len(arts) > a.max_candidates:
        # 控制 LLM 成本:先用來源先驗粗篩
        arts = sorted(arts, key=lambda x: x.source_weight, reverse=True)[:a.max_candidates]
    method = score.score_all(arts, a.model, profile=prof, interests=a.interests)
    picks = score.select(arts, top, min_score, per_source)
    print(f"評分方式: {method};入選 {len(picks)} 篇")
    for p in picks:
        print(f"  {p.score:5.1f}  [{p.source}] {p.title}")
    if not picks:
        print("本期沒有達標內容,不寄信。")
        return 0

    if prof.enrich and method == "llm":
        enrich.enrich(picks, a.write_model)
    unsub = "要取消訂閱,請直接回信並註明「unsubscribe」。"
    subj = digest.subject(picks, profile=prof)
    text = digest.render_text(picks, unsub, prof)
    page = digest.render_html(picks, unsub, prof)
    if a.out:
        Path(a.out).write_text(page, encoding="utf-8")
        print(f"預覽已寫入 {a.out}")
    if a.dry_run:
        print("(dry-run,不寄信、不更新已寄紀錄)")
        return 0

    to = subscribers.load()
    if not to:
        print("沒有訂閱者,不寄信。")
        return 0
    if not os.environ.get("SMTP_HOST"):
        print("缺少 SMTP_HOST 等寄信設定。", file=sys.stderr)
        return 1
    n = mailer.send_all(subj, text, page, to)
    print(f"已寄出 {n}/{len(to)} 封")
    if n:
        _save_seen(prof.seen_file, seen | {p.url.split('#')[0].rstrip('/') for p in picks})
    return 0 if n == len(to) else 1


def main(argv=None) -> int:
    p = argparse.ArgumentParser(prog="curator")
    sub = p.add_subparsers(dest="cmd", required=True)
    r = sub.add_parser("run", help="抓取、評分、寄出精選")
    r.add_argument("--days", type=int, default=7, help="只看近幾天的文章")
    r.add_argument("--profile", choices=sorted(PROFILES), default="insight",
                   help="insight=大議題精選;gems=生活冷知識 (貼近生活、實用、少人知道)")
    r.add_argument("--interests", default="", help='讀者關注主題,例如 "育兒, 理財, 居家" (僅微調評分)')
    r.add_argument("--write-model", default=enrich.WRITE_MODEL, help="gems 模式寫短文用的模型")
    r.add_argument("--top", type=int, default=0, help="0=用設定檔預設")
    r.add_argument("--min-score", type=float, default=None, help="低於此分不入選 (寧缺勿濫);預設依設定檔")
    r.add_argument("--per-source", type=int, default=0)
    r.add_argument("--max-candidates", type=int, default=120)
    r.add_argument("--sources", default="", help="來源 JSON;預設依 profile")
    r.add_argument("--model", default=score.DEFAULT_MODEL)
    r.add_argument("--dry-run", action="store_true")
    r.add_argument("--out", help="把 HTML 預覽寫到此檔")
    r.set_defaults(fn=cmd_run)
    for name, fn in (("subscribe", subscribers.add), ("unsubscribe", subscribers.remove)):
        s = sub.add_parser(name)
        s.add_argument("email")
        s.set_defaults(fn=lambda a, fn=fn, name=name: print(("完成" if fn(a.email) else "無變動")) or 0)
    sub.add_parser("list").set_defaults(fn=lambda a: print("\n".join(subscribers.load()) or "(無訂閱者)") or 0)
    a = p.parse_args(argv)
    return a.fn(a)


if __name__ == "__main__":
    sys.exit(main())
