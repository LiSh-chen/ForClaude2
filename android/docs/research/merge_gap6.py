import re,glob,collections
R='docs/research/'
SUP='最|唯一|第一|全台|全國|僅存'
SKIPTAG=set('''求子燈花 脆筍肉包 臺東米苔目 鴨賞捲 溪州羊肉爐 八德浣衣池 八德白鷺里 苓雅改名 福興橋 北投石 瓜仔尾醬瓜 龍膽石斑 元宵踩街 蘇澳羊羹 魚苗托兒所 猴祭 石頭火鍋 粉紅超跑 年貨臘味'''.split())
rows=collections.OrderedDict()
for l in open('app/src/main/res/raw/town_blurbs.txt',encoding='utf-8'):
    if l.startswith('#') or not l.strip(): continue
    p=l.rstrip('\n').split('|'); rows[(p[0],p[1])]=p[2:]
order=[tuple(l.strip().split('|')) for f in sorted(glob.glob(R+'input_towns_*.txt')) for l in open(f) if l.strip()]
added=0;skip=[]
for f in sorted(glob.glob(R+'gap6_out_[0-9].txt')):
    for l in open(f,encoding='utf-8'):
        p=l.strip().split('|')
        if len(p)<3: continue
        k=(p[0],p[1]); cur=rows.setdefault(k,[]); names={x.split(':')[0] for x in cur}
        for x in p[2:]:
            if ':' not in x: continue
            t,s=x.split(':',1)
            if t in SKIPTAG or t in names or len(cur)>=3 or re.search(SUP,s) or not 14<=len(s)<=60: skip.append((p[1],t)); continue
            if t[:2] not in s[:16]: skip.append((p[1],t,'mismatch')); continue
            cur.append(f'{t}:{s}'); names.add(t); added+=1
idx={k:i for i,k in enumerate(order)}
final=sorted(((k,v) for k,v in rows.items() if v),key=lambda kv:idx.get(kv[0],999))
c=collections.Counter(len(v) for k,v in final)
print('added',added,'towns',len(final),dict(c)); print('skipped',skip)
print('no tags:',[k[1] for k in order if k not in dict(final)])
hdr='''# county|name|tag:did you know|tag:did you know|tag:did you know   (1-3 tags per township; one is found per lap)
# Strict pass 2026-10-20 (round 2): no shops, tag = subject of the sentence, memorable detail; see docs/research.
'''
open('app/src/main/res/raw/town_blurbs.txt','w',encoding='utf-8').write(hdr+'\n'.join('|'.join(list(k)+v) for k,v in final)+'\n')
