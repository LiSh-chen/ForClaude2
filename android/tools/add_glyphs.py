#!/usr/bin/env python3
"""Add the characters the notebook font lacks to res/font/lxgw_wenkai_tc_subset.ttf, without needing the full font file.

The full LXGW WenKai TC is not always at hand; its npm package @fontsource/lxgw-wenkai-tc holds it cut into woff2 slices by unicode-range.
Usage:  npm pack @fontsource/lxgw-wenkai-tc@5.3.1 && tar xzf fontsource-lxgw-wenkai-tc-*.tgz      (gives ./package)
        python3 android/tools/add_glyphs.py ./package [extra text file ...]
Characters are taken from every res/raw/*.txt, the Kotlin sources of the UI and the traveler texts in core, and any extra files given.
Needs: pip install fonttools brotli
"""
import sys, os, re, glob, tempfile
from fontTools.ttLib import TTFont
from fontTools import subset, merge

pkg = sys.argv[1]; extra = sys.argv[2:]
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
font = os.path.join(root, 'app/src/main/res/font/lxgw_wenkai_tc_subset.ttf')
texts = glob.glob(os.path.join(root, 'app/src/main/res/raw/*.txt')) + glob.glob(os.path.join(root, 'app/src/main/kotlin/**/*.kt'), recursive=True) \
    + glob.glob(os.path.join(root, 'core/src/commonMain/kotlin/**/*.kt'), recursive=True) + extra
need = set()
for f in texts:
    need |= {c for c in open(f, encoding='utf-8').read() if '㐀' <= c <= '鿿' or '　' <= c <= '〿' or '＀' <= c <= '￯'}
have = set(TTFont(font).getBestCmap())
missing = sorted(c for c in need if ord(c) not in have)
print(len(missing), 'missing:', ''.join(missing))
if not missing: sys.exit(0)

css = open(os.path.join(pkg, '400.css'), encoding='utf-8').read()
slices = []
for blk in re.findall(r'@font-face\s*{(.*?)}', css, re.S):
    m = re.search(r'files/([^)]*?\.woff2)', blk); r = re.search(r'unicode-range:\s*([^;]+);', blk)
    if not (m and r): continue
    cps = set()
    for part in r.group(1).split(','):
        part = part.strip().lstrip('Uu+')
        if '-' in part: a, b = part.split('-'); cps |= set(range(int(a, 16), int(b, 16) + 1))
        else: cps.add(int(part, 16))
    slices.append((m.group(1), cps))

tmp = tempfile.mkdtemp(); parts = [font]; todo = set(ord(c) for c in missing); n = 0
for fn, cps in slices:
    want = todo & cps
    if not want: continue
    opts = subset.Options(); opts.layout_features = []; opts.hinting = False; opts.notdef_outline = False
    f = TTFont(os.path.join(pkg, 'files', fn))
    s = subset.Subsetter(opts); s.populate(unicodes=sorted(want)); s.subset(f)
    f.flavor = None
    out = os.path.join(tmp, f'part{n}.ttf'); f.save(out); parts.append(out); n += 1; todo -= want
if todo: print('not found in the package:', ''.join(chr(c) for c in sorted(todo)))
merged = merge.Merger().merge(parts)
merged.save(font)
print('font rebuilt, size', os.path.getsize(font))
