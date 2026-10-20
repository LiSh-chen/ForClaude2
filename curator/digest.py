"""把精選文章排成電子報 (HTML + 純文字)。"""
from __future__ import annotations

import html
from datetime import date

from .fetch import Article
from .profiles import Profile, resolve

_CONF = {"high": "資料充分", "medium": "資料尚可", "low": "僅依摘要,請自行查證"}


def subject(arts: list[Article], today: date | None = None, profile: Profile | None = None) -> str:
    profile = resolve(profile)
    today = today or date.today()
    if not arts:
        return f"【{profile.subject_prefix}】{today:%m/%d}"
    lead = (arts[0].detail.get("headline") or arts[0].one_liner)[:24]
    return f"【{profile.subject_prefix}】{today:%m/%d} 值得知道的 {len(arts)} 件事 — {lead}…"


def render_text(arts: list[Article], unsub_hint: str = "", profile: Profile | None = None) -> str:
    profile = resolve(profile)
    lines = [profile.tagline + "\n"]
    for i, a in enumerate(arts, 1):
        d = a.detail
        lines.append(f"{i}. {d.get('headline') or a.title}")
        if d:
            lines += [f"   {d['what']}", f"   為什麼少人知道:{d['why_unknown']}",
                      f"   怎麼用:{d['how_to_use']}", f"   注意:{d['caveat']} ({_CONF.get(d['confidence'], '')})"]
        else:
            lines += [f"   {a.one_liner}", f"   為什麼值得讀:{a.why}"]
        lines += [f"   {a.source}  ·  價值分 {a.score:g}", f"   {a.url}\n"]
    if unsub_hint:
        lines.append(unsub_hint)
    return "\n".join(lines)


def _block(label: str, body: str, color: str) -> str:
    return (f'<p style="margin:6px 0 0;font-size:14px;line-height:1.6"><b style="color:{color}">{label}</b>'
            f'　{html.escape(body)}</p>')


def render_html(arts: list[Article], unsub_html: str = "", profile: Profile | None = None) -> str:
    profile = resolve(profile)
    e = html.escape
    cards = []
    for i, a in enumerate(arts, 1):
        d = a.detail
        cat = f" · {e(a.category)}" if a.category else ""
        meta = f'<div style="font-size:12px;color:#888">{i:02d}{cat} · {e(a.source)} · 價值分 {a.score:g}</div>'
        head = (f'<a href="{e(a.url, quote=True)}" style="font-size:19px;font-weight:600;color:#111;'
                f'text-decoration:none;line-height:1.4">{e(d.get("headline") or a.title)}</a>')
        if d:
            body = (f'<p style="margin:8px 0 4px;color:#333;font-size:15px;line-height:1.6">{e(d["what"])}</p>'
                    + _block("為什麼少人知道", d["why_unknown"], "#8a5a00")
                    + _block("怎麼用", d["how_to_use"], "#0b6b3a")
                    + _block("注意", f'{d["caveat"]} ({_CONF.get(d["confidence"], "")})', "#a33"))
        else:
            body = (f'<p style="margin:8px 0 6px;color:#333;font-size:15px;line-height:1.6">{e(a.one_liner)}</p>'
                    + _block("為什麼值得讀", a.why, "#0b6b3a"))
        cards.append(f'<div style="margin:0 0 22px;padding:0 0 18px;border-bottom:1px solid #e5e5e5">{meta}{head}{body}</div>')
    return f"""<!doctype html><html><body style="margin:0;background:#f6f6f4">
<div style="max-width:620px;margin:0 auto;padding:28px 20px;background:#fff;font-family:-apple-system,'Noto Sans TC',sans-serif">
  <h1 style="font-size:22px;margin:0 0 4px">{e(profile.title)}</h1>
  <p style="color:#666;font-size:13px;margin:0 0 24px">{e(profile.tagline)}</p>
  {''.join(cards)}
  <p style="color:#999;font-size:12px">{unsub_html}</p>
</div></body></html>"""
