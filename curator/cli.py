from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import date
from pathlib import Path

from . import pubmed, digest, enrich, handoff, topics, init as initmod, fetch, mailer, queue, score, subscribers
from . import profiles

ROOT = Path(__file__).resolve().parent.parent

def _load_seen(path: Path) -> set[str]:
    return set(json.loads(path.read_text())) if path.exists() else set()


def _save_seen(path: Path, urls: set[str]) -> None:
    path.parent.mkdir(exist_ok=True)
    path.write_text(json.dumps(sorted(urls)[-5000:], indent=0), encoding="utf-8")


def cmd_run(a) -> int:
    prof = profiles.get(a.profile)
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
    method = score.score_all(arts, a.model, profile=prof, interests=a.interests or prof.interests)
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

    ok = _deliver(prof, subj, text, page, picks)
    return 0 if ok else 1


def _deliver(prof, subj, text, page, picks) -> bool:
    """寄給所有訂閱者並記錄已寄;全部成功才回傳 True。"""
    to = subscribers.load()
    if not to:
        print("沒有訂閱者,不寄信。")
        return True
    if not os.environ.get("SMTP_HOST"):
        print("缺少 SMTP_HOST 等寄信設定。", file=sys.stderr)
        return False
    n = mailer.send_all(subj, text, page, to)
    print(f"已寄出 {n}/{len(to)} 封")
    if n:
        _save_seen(prof.seen_file, _load_seen(prof.seen_file) | {queue.norm_url(p.url) for p in picks})
    return n == len(to)


def cmd_collect(a) -> int:
    """每天跑:抓取→評分→寫短文→放進待審佇列。不寄信。"""
    prof = profiles.get(a.profile)
    if prof.mode == "topics":
        n = handoff.write_topic_inbox(prof, a.topics_per_run)
        print(f"inbox 寫入 {n} 個題目 → {handoff.inbox_path(prof).relative_to(ROOT)};題庫剩 {topics.remaining(topics.load(prof))} 個未用")
        return 0
    feeds = json.loads(Path(a.sources or prof.sources).read_text(encoding="utf-8"))["feeds"]
    arts = fetch.fetch_all(feeds)
    qpath = queue.path_for(prof)
    q = queue.load(qpath)
    seen = (_load_seen(prof.seen_file) | {queue.norm_url(i["url"]) for i in q["items"]}
            | handoff.scored_urls(prof))
    arts = fetch.dedupe(fetch.recent(arts, a.days), seen)
    print(f"新候選: {len(arts)}")
    if arts and a.no_llm:
        # 外部評分者模式:程式不呼叫 LLM。先用便宜的啟發式粗篩出最值得評的 N 則,寫進 inbox。
        score.score_heuristic(arts, prof)
        arts = sorted(arts, key=lambda x: x.score, reverse=True)[:a.inbox_size]
        print(f"inbox 寫入 {handoff.write_inbox(prof, arts)} 則 → {handoff.inbox_path(prof).relative_to(ROOT)}")
        return 0
    if arts:
        arts = sorted(arts, key=lambda x: x.source_weight, reverse=True)[:a.max_candidates]
        method = score.score_all(arts, a.model, profile=prof, interests=a.interests or prof.interests)
        cands = score.select(arts, a.top, a.min_score if a.min_score is not None else prof.min_score - 10,
                             a.per_source or 3)
        if prof.enrich and method == "llm":
            enrich.enrich(cands, a.write_model)
        print(f"評分方式 {method};加入待審 {queue.add(q, cands)} 則")
    print(f"逾期 {queue.expire(q)} 則;待審共 {len(queue.items(q, 'pending'))} 則")
    queue.save(qpath, q)
    queue.write_review_md(q, qpath.with_suffix('.md'))
    return 0


def cmd_ingest(a) -> int:
    """收下外部評分者寫的 outbox,放進待審佇列。沒有 outbox 就什麼都不做。"""
    prof = profiles.get(a.profile)
    qpath = queue.path_for(prof)
    q = queue.load(qpath)
    fn = handoff.ingest_topics if prof.mode == "topics" else handoff.ingest
    n = fn(prof, q, a.top, a.min_score if a.min_score is not None else prof.min_score - 10,
                       a.per_source or 3)
    if n < 0:
        print("沒有可收的 outbox。")
        return 0
    print(f"加入待審 {n} 則;逾期 {queue.expire(q)} 則;待審共 {len(queue.items(q, 'pending'))} 則")
    queue.save(qpath, q)
    queue.write_review_md(q, qpath.with_suffix('.md'))
    return 0


def cmd_queue(a) -> int:
    prof = profiles.get(a.profile)
    qpath = queue.path_for(prof)
    q = queue.load(qpath)
    if a.action == "list":
        rows = queue.items(q, a.status)
        print("\n\n".join(queue.format_item(i) for i in rows) or f"(沒有 {a.status} 的項目)")
        return 0
    status = {"approve": "approved", "reject": "rejected", "unapprove": "pending"}[a.action]
    changed = queue.set_status(q, a.ids, status, force=a.force)
    skipped = [i for i in a.ids if i not in changed]
    if skipped and status == "approved":
        print(f"未核准 {skipped}:有數字檢查旗標、已寄出或不存在。修正文字並清空 detail.flags,或加 --force")
    queue.save(qpath, q)
    queue.write_review_md(q, qpath.with_suffix('.md'))
    print(f"{a.action}: {changed or '無變動'}")
    return 0


def cmd_send(a) -> int:
    """寄出已核准的項目 (依分數取前 N 則)。沒有核准項目就不寄。"""
    prof = profiles.get(a.profile)
    qpath = queue.path_for(prof)
    q = queue.load(qpath)
    chosen, per_cat = [], {}
    for i in queue.items(q, "approved"):
        if len(chosen) >= (a.top or prof.top):
            break
        if prof.per_category and per_cat.get(i.get("category"), 0) >= prof.per_category:
            continue
        per_cat[i.get("category")] = per_cat.get(i.get("category"), 0) + 1
        chosen.append(i)
    if not chosen:
        print("沒有已核准的項目,不寄信。")
        return 0
    picks = [queue.to_article(i) for i in chosen]
    unsub = "要取消訂閱,請直接回信並註明「unsubscribe」。"
    subj, text, page = (digest.subject(picks, profile=prof), digest.render_text(picks, unsub, prof),
                        digest.render_html(picks, unsub, prof))
    if a.out:
        Path(a.out).write_text(page, encoding="utf-8")
        print(f"預覽已寫入 {a.out}")
    if a.dry_run:
        print(f"(dry-run,將寄出 {len(picks)} 則,不寄信、不改狀態)")
        return 0
    if not _deliver(prof, subj, text, page, picks):
        return 1
    for i in chosen:
        i["status"] = "sent"
        i["sent"] = date.today().isoformat()
    queue.save(qpath, q)
    queue.write_review_md(q, qpath.with_suffix('.md'))
    return 0


def cmd_init(a) -> int:
    if (profiles.NEWSLETTERS / a.name).exists():
        print(f"{a.name} 已存在。", file=sys.stderr)
        return 1
    if a.copy_from:
        d = initmod.copy_from(a.copy_from, a.name)
    elif a.brief:
        if not os.environ.get("ANTHROPIC_API_KEY"):
            print("用 --brief 產生定義需要 ANTHROPIC_API_KEY;或改用 --copy-from。", file=sys.stderr)
            return 1
        d = initmod.generate(a.brief, a.name, a.model)
    else:
        print("請指定 --brief \"一段話描述\" 或 --copy-from <現有電子報>", file=sys.stderr)
        return 1
    print(f"已建立 {d}\n下一步:1) 審閱並修改 rubric.md / newsletter.json  2) python -m curator check {a.name} --prune"
          f"\n         3) python -m curator collect --profile {a.name}  4) 加入 workflow 的 NEWSLETTERS 變數")
    return 0


def cmd_check(a) -> int:
    ok_all = True
    for name in a.names or profiles.names():
        print(name)
        prof = profiles.get(name)
        if prof.mode == "topics":
            try:
                print(f"  ✓ PubMed: {len(pubmed.search('sleep', 3))} 筆")
                continue
            except Exception as e:
                print(f"  ✗ PubMed: {type(e).__name__}: {e}")
                ok_all = False
                continue
        ok, total = initmod.check_sources(prof, a.prune)
        print(f"  → {ok}/{total} 可用")
        ok_all &= ok > 0
    return 0 if ok_all else 1


def main(argv=None) -> int:
    p = argparse.ArgumentParser(prog="curator")
    sub = p.add_subparsers(dest="cmd", required=True)
    r = sub.add_parser("run", help="抓取、評分、寄出精選")
    r.add_argument("--days", type=int, default=7, help="只看近幾天的文章")
    r.add_argument("--profile", default="insight",
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
    i = sub.add_parser("init", help="建立新電子報 (--brief 由 Claude 產生草稿,或 --copy-from 複製)")
    i.add_argument("name")
    i.add_argument("--brief", default="", help="一段話:讀者是誰、什麼內容有價值、什麼算雜訊")
    i.add_argument("--copy-from", default="")
    i.add_argument("--model", default=enrich.WRITE_MODEL)
    i.set_defaults(fn=cmd_init)
    ck = sub.add_parser("check", help="連線驗證來源網址")
    ck.add_argument("names", nargs="*")
    ck.add_argument("--prune", action="store_true", help="移除失敗的來源")
    ck.set_defaults(fn=cmd_check)
    sub.add_parser("newsletters", help="列出電子報").set_defaults(
        fn=lambda a: print("\n".join(f"{n}\t{profiles.get(n).title}" for n in profiles.names())) or 0)
    c = sub.add_parser("collect", help="(每天) 抓取評分後放進待審佇列,不寄信")
    c.add_argument("--profile", default="gems")
    c.add_argument("--days", type=int, default=3)
    c.add_argument("--top", type=int, default=12, help="每次最多放進待審幾則")
    c.add_argument("--min-score", type=float, default=None, help="預設為設定檔門檻 -10 (審稿再把關)")
    c.add_argument("--per-source", type=int, default=0)
    c.add_argument("--max-candidates", type=int, default=120)
    c.add_argument("--interests", default="")
    c.add_argument("--sources", default="")
    c.add_argument("--model", default=score.DEFAULT_MODEL)
    c.add_argument("--write-model", default=enrich.WRITE_MODEL)
    c.add_argument("--topics-per-run", type=int, default=8, help="topics 模式:每次從題庫取幾題")
    c.add_argument("--no-llm", action="store_true", help="不呼叫 LLM:候選寫到 data/inbox,由外部評分者處理")
    c.add_argument("--inbox-size", type=int, default=40, help="--no-llm 時最多放幾則進 inbox")
    c.set_defaults(fn=cmd_collect)
    ig = sub.add_parser("ingest", help="收下外部評分者的 outbox,放進待審佇列")
    ig.add_argument("--profile", default="gems")
    ig.add_argument("--top", type=int, default=12)
    ig.add_argument("--min-score", type=float, default=None)
    ig.add_argument("--per-source", type=int, default=0)
    ig.set_defaults(fn=cmd_ingest)
    sprompt = sub.add_parser("scorer-prompt", help="印出外部評分者 (如 Claude Code) 該遵循的完整指示")
    sprompt.add_argument("--profile", default="gems")
    sprompt.set_defaults(fn=lambda a: print((handoff.scorer_prompt_topics if profiles.get(a.profile).mode == "topics" else handoff.scorer_prompt)(profiles.get(a.profile))) or 0)
    qp = sub.add_parser("queue", help="審稿:list / approve / reject / unapprove")
    qp.add_argument("action", choices=["list", "approve", "reject", "unapprove"])
    qp.add_argument("ids", nargs="*", type=int)
    qp.add_argument("--force", action="store_true", help="略過數字檢查旗標強制核准")
    qp.add_argument("--status", default="pending", choices=queue.STATUSES)
    qp.add_argument("--profile", default="gems")
    qp.set_defaults(fn=cmd_queue)
    sp = sub.add_parser("send", help="寄出已核准的項目")
    sp.add_argument("--profile", default="gems")
    sp.add_argument("--top", type=int, default=0)
    sp.add_argument("--dry-run", action="store_true")
    sp.add_argument("--out")
    sp.set_defaults(fn=cmd_send)
    for name, fn in (("subscribe", subscribers.add), ("unsubscribe", subscribers.remove)):
        s = sub.add_parser(name)
        s.add_argument("email")
        s.set_defaults(fn=lambda a, fn=fn, name=name: print(("完成" if fn(a.email) else "無變動")) or 0)
    sub.add_parser("list").set_defaults(fn=lambda a: print("\n".join(subscribers.load()) or "(無訂閱者)") or 0)
    a = p.parse_args(argv)
    return a.fn(a)


if __name__ == "__main__":
    sys.exit(main())
