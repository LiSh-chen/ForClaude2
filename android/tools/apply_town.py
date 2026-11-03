#!/usr/bin/env python3
"""Replace or add whole district lines in town_blurbs.txt, then rebuild the font subset.
Usage (from repo root /home/user/ForClaude2): python3 android/tools/apply_town.py changes.txt
changes.txt: lines `縣市|區|🙂 標籤:句子|…` (replace the district's line, or add it). A line `-縣市|區` deletes it.
Applies in memory; checks format and banned characters; restores nothing because nothing is written on failure."""
import sys, re, subprocess, os
F='android/app/src/main/res/raw/town_blurbs.txt'
BAN=re.compile('最|唯一|第一|全台|全國|僅存'); NOTE=re.compile('（(拿掉|維持|建議|二擇一|待|請|刪)')
lines=open(F,encoding='utf-8').read().split('\n')
idx={}
for i,l in enumerate(lines):
    f=l.split('|')
    if l.strip() and not l.startswith('#') and len(f)>=3: idx[(f[0],f[1])]=i
errs=[];log=[]
for c in open(sys.argv[1],encoding='utf-8').read().split('\n'):
    if not c.strip() or c.startswith('#'): continue
    if c.startswith('-'):
        k=tuple(c[1:].split('|'))
        if k in idx: lines[idx[k]]=None; log.append('DEL '+'|'.join(k))
        continue
    f=c.split('|'); k=(f[0],f[1])
    if not 3<=len(f)<=5: errs.append('bad line '+c); continue
    for x in f[2:]:
        t=x.split(':',1)
        if len(t)!=2: errs.append('bad tag '+x); continue
        s=t[1].strip(); chk=s.endswith('※'); core=s.rstrip('※')
        if not 14<=len(core)<=90: errs.append(f'{k} length {len(core)}')
        if not chk and BAN.search(s): errs.append(f'{k} banned word')
        if NOTE.search(s): errs.append(f'{k} editor note')
    if k in idx: lines[idx[k]]=c; log.append('SET '+'|'.join(k))
    else: lines.append(c); log.append('ADD '+'|'.join(k))
if errs: print('\n'.join(errs)); sys.exit(1)
open(F,'w',encoding='utf-8').write('\n'.join(l for l in lines if l is not None))
print('\n'.join(log))
# font subset
chars=set(chr(c) for c in range(32,127))
for fn in os.listdir('android/app/src/main/res/raw'):
    if fn.endswith('.txt'): chars|=set(open('android/app/src/main/res/raw/'+fn,encoding='utf-8').read())
out='android/app/src/main/res/font/lxgw_wenkai_tc_subset.ttf'
from fontTools.ttLib import TTFont
chars|=set(chr(c) for c in TTFont(out).getBestCmap())
open('/tmp/chars.txt','w',encoding='utf-8').write(''.join(sorted(chars)))
src='/tmp/claude-0/fonts2/lxgwwenkaitc.ttf'
subprocess.check_call(['pyftsubset',src,'--text-file=/tmp/chars.txt','--output-file='+out,"--layout-features=*",'--no-hinting'])
print('font rebuilt')
