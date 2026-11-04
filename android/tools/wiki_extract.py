#!/usr/bin/env python3
"""Fetch a zh.wikipedia page once (cached), strip markup, and print only what a township researcher needs.
Usage: python3 android/tools/wiki_extract.py "溪口鄉_(台灣)" [--sections 旅遊,產業,特產,歷史,文化] [--max 1800] [--grep 關鍵字] [--all] [--outline]
- Handles URL encoding, 429 (sleeps and retries), caches raw text in .cache/wiki/ (shared by every agent).
- Default output: section outline + the key sections, each cut to --max characters. --all prints everything (expensive).
"""
import sys, os, re, time, urllib.request, urllib.parse, hashlib
args = sys.argv[1:]
if not args: sys.exit(__doc__)
title = args[0]
def opt(name, default=None):
    if name in args:
        i = args.index(name); return args[i+1]
    return default
sections = (opt('--sections', '歷史,產業,特產,旅遊,景點,文化,名勝,古蹟,宗教,節慶,交通,地理,簡介') or '').split(',')
maxc = int(opt('--max', '1800')); grep = opt('--grep'); show_all = '--all' in args; outline_only = '--outline' in args
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '.cache', 'wiki'); os.makedirs(root, exist_ok=True)
fn = os.path.join(root, hashlib.md5(title.encode()).hexdigest() + '.txt')
if not os.path.exists(fn):
    url = 'https://zh.wikipedia.org/w/index.php?action=raw&title=' + urllib.parse.quote(title.replace(' ', '_'))
    for attempt in range(6):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'ForClaude2-research/1.0'})
            data = urllib.request.urlopen(req, timeout=30).read().decode('utf-8', 'replace'); break
        except urllib.error.HTTPError as e:
            if e.code == 429: time.sleep(int(e.headers.get('Retry-After', '20')) + 2); continue
            print(f'HTTP {e.code} for {title}'); sys.exit(1)
        except Exception as e:
            time.sleep(5)
    else:
        print('FAILED after retries (rate limited)'); sys.exit(1)
    open(fn, 'w', encoding='utf-8').write(data)
raw = open(fn, encoding='utf-8').read()
m = re.match(r'#(?:REDIRECT|重定向)\s*\[\[([^\]#|]+)', raw, re.I)
if m: print(f'[redirect -> {m.group(1)}]'); sys.exit(0)
t = re.sub(r'<ref[^>/]*/>', '', raw); t = re.sub(r'<ref[^>]*>.*?</ref>', '', t, flags=re.S)
t = re.sub(r'<!--.*?-->', '', t, flags=re.S)
for _ in range(4): t = re.sub(r'\{\{[^{}]*\}\}', '', t)
t = re.sub(r'\[\[(?:File|Image|檔案|文件|Category|分類)[^\]]*\]\]', '', t, flags=re.I)
t = re.sub(r'\[\[(?:[^\]|]*\|)?([^\]]+)\]\]', r'\1', t); t = re.sub(r"'{2,}", '', t)
t = re.sub(r'<[^>]+>', '', t); t = re.sub(r'\n{3,}', '\n\n', t)
parts = re.split(r'\n(={2,}[^=\n]+={2,})\n', '\n' + t)
secs = [('導言', parts[0].strip())] + [(parts[i].strip('= '), parts[i+1].strip()) for i in range(1, len(parts) - 1, 2)]
print(f'# {title}  ({len(t)} chars after cleaning)'); print('outline: ' + ' | '.join(f'{h}({len(b)})' for h, b in secs))
if outline_only: sys.exit(0)
if grep:
    for h, b in secs:
        for ln in b.split('\n'):
            if grep in ln: print(f'[{h}] {ln.strip()[:400]}')
    sys.exit(0)
for h, b in secs:
    if show_all or h == '導言' or any(k in h for k in sections):
        print(f'\n## {h}\n{b if show_all else b[:maxc]}' + ('' if show_all or len(b) <= maxc else ' …(cut)'))
