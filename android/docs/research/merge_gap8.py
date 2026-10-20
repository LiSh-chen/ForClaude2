import re,glob
R='docs/research/'
KEEP=set('肉粿 池上米 黃金蜆 樹豆 芋蔥粿 桃園夜市 油飯魚丸湯 傳道冰 沙鍋魚頭 鳳眼糕 燒炸粿 芹菜管 茶葉集散 哆羅滿 福鹿茶 花園夜市'.split())
SUP='最|唯一|第一|全台|全國|僅存'
P='app/src/main/res/raw/town_blurbs.txt'
lines=open(P,encoding='utf-8').read().split('\n')
hdr=[l for l in lines if l.startswith('#')]
rows={}
order=[]
for l in lines:
    if l.startswith('#') or not l.strip(): continue
    p=l.split('|'); rows[(p[0],p[1])]=p[2:]; order.append((p[0],p[1]))
added=[]
for f in sorted(glob.glob(R+'gap8_out_[0-9].txt')):
    for l in open(f,encoding='utf-8'):
        p=l.strip().split('|')
        if len(p)<3: continue
        k=(p[0],p[1])
        for x in p[2:]:
            if ':' not in x: continue
            t,s=x.split(':',1)
            cur=rows.setdefault(k,[])
            if k not in order: order.append(k)
            if t in KEEP and t not in {y.split(':')[0] for y in cur} and len(cur)<3 and not re.search(SUP,s) and t[:2] in s[:16]:
                cur.append(f'{t}:{s}'); added.append((k[1],t))
print(len(added),added)
open(P,'w',encoding='utf-8').write('\n'.join(hdr)+'\n'+'\n'.join('|'.join(list(k)+rows[k]) for k in order if rows[k])+'\n')
