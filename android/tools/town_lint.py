#!/usr/bin/env python3
"""Lint for res/raw/town_blurbs.txt. Hard errors (E) must be zero before a review; warnings (W) go to the reviewer.
Usage: python3 android/tools/town_lint.py [path] [--json]"""
import re,sys,json,collections
path=next((a for a in sys.argv[1:] if not a.startswith('--')),'android/app/src/main/res/raw/town_blurbs.txt')
TITLE=re.compile('最|唯一|第一|全台|全國|僅存')
TITLE_SYN=re.compile('首條|首座|首次|首口|全球|全世界|少數|初代|之冠|罕見|規模龐大|獨一|世界級|無雙|台版|台灣版|被譽|美稱|之稱')
TIME=re.compile('近年|如今|現在|目前|今年|正在|至今|仍會|仍有|仍在|仍是|仍以|已停|早已|尚存|還在|可搭乘|可入內|假日|開放|票價|名額|營業')
NOTE=re.compile('（(拿掉|維持|建議|二擇一|待|請|刪)')
QUIP=re.compile('等於|堪稱|戲稱|請不要|沒有玄機|被形容|威尼斯|點鈔機|遊覽車')
VAGUE=re.compile('傳說有多種|歷史悠久|知名|有名|聞名|特別的|獨特')
ORIGIN=re.compile('舊名|舊稱|得名|地名|由來|改名|取名|稱為')
E=[];W=[];cov=collections.Counter();rows=[]
for n,l in enumerate(open(path,encoding='utf-8'),1):
    l=l.rstrip('\n')
    if not l.strip() or l.startswith('#'): continue
    f=l.split('|')
    if len(f)<3 or not 3<=len(f)+0<=5: E.append((n,l[:20],'F0 行格式'));continue
    c,d=f[0],f[1]; cov[(c,d)]=len(f)-2
    for t in f[2:]:
        if ':' not in t: E.append((n,d,'F0 缺冒號'));continue
        tag,s=t.split(':',1); checked=s.endswith('※'); core=s.rstrip('※')
        w=lambda code,msg:W.append((n,d,tag,code,msg))
        e=lambda code,msg:E.append((n,d,tag,code,msg))
        if not 14<=len(core)<=90: e('F1',f'字數 {len(core)}')
        if ':' in core or '|' in core: e('F1','句中含 : 或 |')
        if TITLE.search(core) and not checked: e('F2','稱號用字沒有 ※')
        if NOTE.search(core): e('F5','撰寫者備註外洩')
        if not core.endswith(('。','」','）')): e('F6','缺句號')
        if TITLE_SYN.search(core) and not checked: w('F3','稱號同義詞：需兩個獨立來源並標 ※，或改寫')
        m=TIME.search(core)
        if m: w('F4',f'時效／現況用語「{m.group()}」')
        m=QUIP.search(core)
        if m: w('F7',f'疑似無出處玩笑／比喻「{m.group()}」')
        m=VAGUE.search(core)
        if m: w('F9',f'籠統形容「{m.group()}」：需換成具體特點')
        if ORIGIN.search(core): w('F10','地名／由來類：必須有畫面或故事，否則刪')
        if len(core)<40 and not re.search(r'[；，]',core[10:]): w('F12','資訊量偏少，可能只有一個事實')
    rows.append((c,d))
for (c,d),k in cov.items():
    if k<2: W.append((0,d,'—','F11',f'{c}{d} 只有 {k} 條：須有候選池耗盡的紀錄，否則補足'))
by=collections.Counter(w[3] for w in W)
if '--json' in sys.argv: print(json.dumps({'errors':E,'warnings':W},ensure_ascii=False))
else:
    for e in E: print('E',e)
    print(f'errors={len(E)} warnings={len(W)} by_code={dict(by)}')
    for w in W:
        if w[3] in ('F3','F4','F7','F9','F10','F11'): print('W',w[3],w[1],w[2],w[4])
sys.exit(1 if E else 0)
