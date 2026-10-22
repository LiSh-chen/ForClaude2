import json
from datetime import datetime, timedelta, timezone
from pathlib import Path

import pytest

from curator import digest, fetch, score, subscribers

RSS = b"""<rss version="2.0"><channel>
<item><title>Study finds new mechanism</title><link>https://x.com/a</link>
<description>&lt;p&gt;Researchers discover data&lt;/p&gt;</description>
<pubDate>Mon, 05 Oct 2026 10:00:00 GMT</pubDate></item>
<item><title>No link item</title></item></channel></rss>"""
ATOM = b"""<feed xmlns="http://www.w3.org/2005/Atom"><entry><title>T</title>
<link rel="alternate" href="https://y.com/b"/><summary>S</summary>
<updated>2026-10-05T10:00:00Z</updated></entry></feed>"""


def test_parse_rss_and_atom():
    r = fetch.parse_feed(RSS, "S")
    assert len(r) == 1 and r[0].summary == "Researchers discover data" and r[0].published
    a = fetch.parse_feed(ATOM, "S")
    assert a[0].url == "https://y.com/b"


def test_recent_and_dedupe():
    now = datetime(2026, 10, 6, tzinfo=timezone.utc)
    old = fetch.Article("Old", "https://o", "", "S", now - timedelta(days=30))
    new = fetch.Article("New", "https://n/", "", "S", now - timedelta(days=1))
    dup = fetch.Article("new!", "https://other", "", "S", now)
    assert fetch.recent([old, new], 7, now) == [new]
    assert fetch.dedupe([new, dup, new]) == [new]
    assert fetch.dedupe([new], {"https://n"}) == []


def test_compose_penalizes_trivia():
    d = dict(novelty=8, depth=8, impact=8, evidence=8, trivia=0)
    assert score.compose(d) == 80.0
    assert score.compose({**d, "trivia": 10}) == pytest.approx(48.0)


def test_extract_json_with_noise():
    assert score.extract_json('好的:\n[{"id":0}]') == [{"id": 0}]


def test_heuristic_prefers_substance():
    good = fetch.Article("Researchers discover mechanism", "u1", "A study with data and evidence " * 15, "Nature", None, 0.9)
    bad = fetch.Article("10 best ways to save: coupon deal", "u2", "short", "Blog", None, 0.5)
    score.score_heuristic([good, bad])
    assert good.score > bad.score


def test_select_threshold_and_diversity():
    arts = [fetch.Article(f"t{i}", f"u{i}", "", "A" if i < 4 else "B", None) for i in range(6)]
    for i, a in enumerate(arts):
        a.score = 90 - i * 10
    picked = score.select(arts, top_n=5, min_score=50, per_source=2)
    assert [a.title for a in picked] == ["t0", "t1", "t4"]  # A 最多 2 篇;t5=40 低於門檻


def test_digest_escapes_html():
    a = fetch.Article("<script>x</script>", "https://a?x=1&y=2", "", "S", None)
    a.one_liner, a.why, a.score = "o", "w", 70
    out = digest.render_html([a])
    assert "<script>" not in out and "&amp;y=2" in out


def test_subscribers_roundtrip(tmp_path, monkeypatch):
    f = tmp_path / "s.txt"
    monkeypatch.delenv("NEWS_SUBSCRIBERS", raising=False)
    assert subscribers.add("A@b.com", f) and not subscribers.add("a@b.com", f)
    with pytest.raises(ValueError):
        subscribers.add("nope", f)
    monkeypatch.setenv("NEWS_SUBSCRIBERS", "c@d.com, bad,\ne@f.org")
    assert subscribers.load(f) == ["c@d.com", "e@f.org", "a@b.com"]
    assert subscribers.remove("a@b.com", f) and not subscribers.remove("a@b.com", f)


def test_gems_compose_penalizes_mainstream_and_trivia():
    from curator import profiles
    GEMS = profiles.get("gems")
    d = dict(practical=8, surprise=8, life=8, evidence=8, mainstream=0, trivia=0)
    assert score.compose(d, GEMS) == 80.0
    assert score.compose({**d, "mainstream": 10}, GEMS) == pytest.approx(48.0)
    assert score.compose({**d, "mainstream": 10, "trivia": 10}, GEMS) == pytest.approx(28.8)


def test_gems_digest_renders_detail_and_falls_back():
    from curator import profiles
    GEMS = profiles.get("gems")
    a = fetch.Article("T", "https://a", "", "S", None)
    a.score, a.one_liner, a.why = 70, "一句話", "原因"
    assert "一句話" in digest.render_html([a], "", GEMS)
    a.detail = {"headline": "標題X", "what": "W", "why_unknown": "U", "how_to_use": "H",
                "caveat": "C", "confidence": "low"}
    page, text = digest.render_html([a], "", GEMS), digest.render_text([a], "", GEMS)
    assert "標題X" in page and "怎麼用" in page and "請自行查證" in text
    assert "冷知識" in digest.subject([a], profile=GEMS)


def test_select_respects_threshold():
    a = fetch.Article("T", "https://a", "", "S", None)
    a.score = 40
    assert score.select([a], min_score=50) == []


def _art(n, score_=70):
    a = fetch.Article(f"T{n}", f"https://a/{n}", "s", "S", None)
    a.score, a.one_liner, a.why = score_, "o", "w"
    return a


def test_queue_flow(tmp_path, monkeypatch):
    from curator import cli, queue
    from curator import profiles
    GEMS = profiles.get("gems")
    monkeypatch.setattr(queue, "ROOT", tmp_path)
    p = queue.path_for(GEMS)
    q = queue.load(p)
    assert queue.add(q, [_art(1), _art(2, 90), _art(1)]) == 2      # 同網址不重複
    assert [i["id"] for i in queue.items(q, "pending")] == [2, 1]  # 依分數排序
    assert queue.set_status(q, [2], "approved") == [2]
    queue.save(p, q)
    # send --dry-run 不改狀態;實際寄出 (無訂閱者視為成功) 會標記 sent
    monkeypatch.setattr(cli, "_deliver", lambda *a, **k: True)
    assert cli.main(["send", "--dry-run"]) == 0
    assert queue.items(queue.load(p), "approved")
    assert cli.main(["send"]) == 0
    q2 = queue.load(p)
    assert not queue.items(q2, "approved") and queue.items(q2, "sent")[0]["id"] == 2
    assert queue.set_status(q2, [2], "rejected") == []             # 已寄的不可改


def test_queue_expire_and_send_nothing(tmp_path, monkeypatch, capsys):
    from datetime import date
    from curator import cli, queue
    monkeypatch.setattr(queue, "ROOT", tmp_path)
    q = queue.load(tmp_path / "x.json")
    queue.add(q, [_art(1)], today=date(2026, 1, 1))
    assert queue.expire(q, 14, today=date(2026, 2, 1)) == 1
    assert cli.main(["send"]) == 0 and "沒有已核准" in capsys.readouterr().out


def test_profile_validation_and_system_prompt():
    from curator import profiles
    good = {"name": "x", "title": "T", "weights": {"evidence": 0.5, "a": 0.5}, "penalties": {"junk": 0.4}}
    profiles.validate(good)
    for bad in ({**good, "weights": {"evidence": 0.9}}, {**good, "name": "a b"},
                {**good, "penalties": {"evidence": 0.3}}, {**good, "penalties": {"junk": 0.9}}):
        with pytest.raises(profiles.ProfileError):
            profiles.validate(bad)
    sysm = profiles.get("gems").system
    assert '"practical": n' in sysm and '"mainstream": n' in sysm and "category" in sysm


def test_all_shipped_newsletters_load():
    from curator import profiles
    assert {"gems", "insight"} <= set(profiles.names())
    for n in profiles.names():
        p = profiles.get(n)
        assert p.sources.exists()
        assert p.mode == "topics" or json.loads(p.sources.read_text())["feeds"]


def test_parse_definition_and_copy(tmp_path, monkeypatch):
    from curator import init, profiles
    ok = json.dumps({"name": "n", "title": "T", "weights": {"evidence": 0.5, "use": 0.5}, "penalties": {"junk": 0.4},
                     "rubric": "r", "feeds": [{"name": "f", "url": "https://f"}, {"name": "bad", "url": "ftp://x"}]})
    cfg, feeds, rubric = init.parse_definition("here:\n" + ok)
    assert len(feeds) == 1 and rubric == "r" and "rubric" not in cfg
    with pytest.raises(profiles.ProfileError):   # 沒有 evidence 維度
        init.parse_definition(ok.replace('"evidence"', '"proof"'))
    monkeypatch.setattr(profiles, "NEWSLETTERS", tmp_path)
    init._write(tmp_path / "a", {**cfg, "name": "a"}, feeds, rubric)
    init.copy_from("a", "b")
    assert profiles.get("b").name == "b" and profiles.get("b").seen_file.name == "seen_b.json"


def test_handoff_roundtrip(tmp_path, monkeypatch):
    from curator import handoff, profiles, queue
    monkeypatch.setattr(handoff, "ROOT", tmp_path)
    monkeypatch.setattr(queue, "ROOT", tmp_path)
    g = profiles.get("gems")
    arts = [fetch.Article(f"T{i}", f"https://a/{i}", "summary", f"S{i}", None) for i in range(3)]
    assert handoff.write_inbox(g, arts, with_text=False) == 3
    keys = [handoff.key_of(a.url) for a in arts]
    detail = {"headline": "H", "what": "W", "why_unknown": "U", "how_to_use": "X", "caveat": "C", "confidence": "high"}
    good = dict(practical=8, surprise=8, life=8, evidence=8, mainstream=0, trivia=0, one_liner="o", why="w", detail=detail)
    bad = dict(practical=9, surprise=9, life=9, evidence=9, mainstream=10, trivia=10, one_liner="o", why="w")
    # 評分者亂填分數 (99、負數) 應被夾住;總分由程式算 (評分者填的 score 無效)
    out = [{"key": keys[0], **good, "score": 100}, {"key": keys[1], **bad},
           {"key": keys[2], **good, "practical": 99, "evidence": -5}, {"key": "nope", **good}]
    handoff.outbox_path(g).parent.mkdir(parents=True)
    handoff.outbox_path(g).write_text(json.dumps(out))
    q = queue.load(tmp_path / "q.json")
    assert handoff.ingest(g, q, top=12, min_score=40, per_source=3) == 2        # bad 被門檻擋掉
    by_title = {i["title"]: i for i in q["items"]}
    assert by_title["T0"]["score"] == 80.0 and by_title["T0"]["detail"]["headline"] == "H"
    assert by_title["T2"]["dims"]["practical"] == 10 and by_title["T2"]["dims"]["evidence"] == 0
    assert not handoff.inbox_path(g).exists() and len(handoff.scored_urls(g)) == 3   # 已評過的不再評
    assert handoff.ingest(g, q, 12, 40, 3) == -1                                # 沒有 outbox


def test_scorer_prompt_mentions_files_and_dims():
    from curator import handoff, profiles
    t = handoff.scorer_prompt(profiles.get("gems"))
    assert "data/inbox/gems.json" in t and "data/outbox/gems.json" in t and '"mainstream"' in t
    assert "detail" in t and "忽略" in t


PUBMED_XML = b"""<PubmedArticleSet><PubmedArticle><MedlineCitation><PMID>111</PMID><Article>
<Journal><Title>Am J Physiol</Title><JournalIssue><PubDate><Year>2016</Year></PubDate></JournalIssue></Journal>
<ArticleTitle>Leg <i>endothelial</i> dysfunction prevented by fidgeting</ArticleTitle>
<Abstract><AbstractText Label="RESULTS">Sitting for 3 hours reduced FMD by 1.6%; fidgeting for 1 minute each 5 minutes prevented the decline in 11 participants.</AbstractText>
<AbstractText>""" + b"x" * 200 + b"""</AbstractText></Abstract>
<PublicationTypeList><PublicationType>Randomized Controlled Trial</PublicationType></PublicationTypeList></Article></MedlineCitation></PubmedArticle>
<PubmedArticle><MedlineCitation><PMID>222</PMID><Article><Journal><Title>J</Title></Journal><ArticleTitle>No abstract</ArticleTitle></Article></MedlineCitation></PubmedArticle></PubmedArticleSet>"""


def test_pubmed_parse_and_rank():
    from curator import pubmed
    papers = pubmed.parse_papers(PUBMED_XML)
    assert papers[0]["pmid"] == "111" and papers[0]["title"] == "Leg endothelial dysfunction prevented by fidgeting"
    assert papers[0]["year"] == "2016" and "Randomized Controlled Trial" in papers[0]["types"]
    kept = pubmed.rank(papers)
    assert [p["pmid"] for p in kept] == ["111"]          # 沒有摘要的被排除


def test_ground_check():
    from curator import ground
    src = "Sitting for 3 hours reduced FMD by 1.6%; 16,010 women followed for 2.5 years (2012)."
    assert ground.check({"truth": "久坐 3 小時使 FMD 下降 1.6%"}, src) == []
    assert ground.check({"truth": "追蹤 1.6 萬名女性 2.5 年"}, src) == []        # 萬 換算 + 6% 容許
    assert ground.check({"truth": "風險高 22%,150 萬隻"}, src) == ["truth:22", "truth:150 萬"]
    assert ground.check({"truth": "2012 年的研究"}, src) == []                    # 年份不檢查


def test_topics_pick_balances_categories():
    from curator import topics
    d = {"topics": [{"id": "a1", "category": "A"}, {"id": "a2", "category": "A"},
                    {"id": "b1", "category": "B"}, {"id": "c1", "category": "C", "status": "rejected"}]}
    assert [t["id"] for t in topics.pick_new(d, 3)] == ["a1", "b1", "a2"]


def _myth_setup(tmp_path, monkeypatch):
    from curator import handoff, profiles, queue, topics
    monkeypatch.setattr(handoff, "ROOT", tmp_path)
    monkeypatch.setattr(queue, "ROOT", tmp_path)
    nd = tmp_path / "newsletters" / "myth"
    nd.mkdir(parents=True)
    import shutil
    for f in ("newsletter.json", "rubric.md", "sources.json"):
        shutil.copy(profiles.NEWSLETTERS / "myth" / f, nd / f)
    (nd / "topics.json").write_text(json.dumps({"topics": [
        {"id": "t1", "category": "健康", "claim": "c1", "query": "q", "status": "new"},
        {"id": "t2", "category": "飲食", "claim": "c2", "query": "q", "status": "new"},
        {"id": "t3", "category": "居家", "claim": "c3", "query": "q", "status": "new"}]}), encoding="utf-8")
    monkeypatch.setattr(profiles, "NEWSLETTERS", tmp_path / "newsletters")
    return profiles.get("myth")


def test_myth_collect_and_ingest(tmp_path, monkeypatch):
    from curator import handoff, pubmed, queue, topics
    prof = _myth_setup(tmp_path, monkeypatch)
    paper = pubmed.parse_papers(PUBMED_XML)[0]
    monkeypatch.setattr(pubmed, "find", lambda q, keep=3: [paper] if q == "q" else [])
    assert handoff.write_topic_inbox(prof, 3, log=lambda *a: None) == 3
    det = dict(headline="H", myth="M", truth="久坐 3 小時 FMD 降 1.6%,22 人", mechanism="X", how_to_use="每 5 分鐘動 1 分鐘",
               caveat="C", evidence="RCT,11 人", confidence="medium")
    good = dict(practical=8, surprise=8, life=8, evidence=8, mainstream=0, one_liner="o", why="w", pmid="111", usable=True)
    out = [{"key": "t1", **good, "detail": det},
           {"key": "t2", "usable": False},
           {"key": "t3", **good, "pmid": "999", "detail": det}]        # 引用不存在的 pmid → 駁回
    handoff.outbox_path(prof).parent.mkdir(parents=True, exist_ok=True)
    handoff.outbox_path(prof).write_text(json.dumps(out))
    q = queue.load(tmp_path / "q.json")
    assert handoff.ingest_topics(prof, q, 12, 40, 5, log=lambda *a: None) == 1
    item = q["items"][0]
    assert item["detail"]["flags"] == ["truth:22"]                      # 22 不在摘要中
    assert item["url"].endswith("/111/") and item["score"] == 80.0
    st = {t["id"]: t["status"] for t in topics.load(prof)["topics"]}
    assert st == {"t1": "queued", "t2": "rejected", "t3": "rejected"}
    # 有旗標不能核准,除非 force
    assert queue.set_status(q, [item["id"]], "approved") == []
    assert queue.set_status(q, [item["id"]], "approved", force=True) == [item["id"]]
    assert "⚠" in queue.format_item(item)


def test_send_caps_per_category(tmp_path, monkeypatch):
    from curator import cli, queue
    prof = _myth_setup(tmp_path, monkeypatch)
    q = queue.load(queue.path_for(prof))
    for n, cat in enumerate(["健康", "健康", "健康", "飲食"], 1):
        a = _art(n, 90 - n)
        a.category = cat
        queue.add(q, [a])
    queue.set_status(q, [1, 2, 3, 4], "approved")
    queue.save(queue.path_for(prof), q)
    monkeypatch.setattr(cli, "_deliver", lambda *a, **k: True)
    assert cli.main(["send", "--profile", "myth"]) == 0
    sent = [i["id"] for i in queue.items(queue.load(queue.path_for(prof)), "sent")]
    assert sorted(sent) == [1, 2, 4]                                  # 健康最多 2 則


def test_myth_digest_layout():
    from curator import digest, profiles
    a = _art(1)
    a.detail = dict(headline="H", myth="M1", truth="T1", mechanism="X1", how_to_use="U1", caveat="C1",
                    evidence="E1", confidence="medium", source_title="Paper", flags=["truth:9"])
    page = digest.render_html([a], "", profiles.get("myth"))
    text = digest.render_text([a], "", profiles.get("myth"))
    for w in ("常見說法", "實際上", "怎麼做", "證據與限制", "Paper"):
        assert w in page
    assert "M1" in text and "truth:9" not in page and "truth:9" not in text   # 旗標只給審稿者看
