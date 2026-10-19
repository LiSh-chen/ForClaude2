import re,glob,collections,sys
B='app/src/main/res/raw/town_blurbs.txt'
SKIP={'紅葉溫泉'}   # judged BAD in an earlier check
rows=collections.OrderedDict();hdr=[]
for l in open(B,encoding='utf-8'):
    if l.startswith('#') or not l.strip(): hdr.append(l);continue
    p=l.rstrip('\n').split('|'); rows[(p[0],p[1])]=p[2:]
added=0;rej=[]
for f in sorted(glob.glob('docs/research/gap_out_[0-9].txt')):
    for l in open(f,encoding='utf-8'):
        p=l.strip().split('|')
        if len(p)<3: continue
        k=(p[0],p[1]); cur=rows.setdefault(k,[])
        names={x.split(':')[0] for x in cur}
        for x in p[2:]:
            if ':' not in x: continue
            t,s=x.split(':',1)
            if t in SKIP or t in names or re.search('最|唯一|第一|全台|全國|僅存',s) or not 15<=len(s)<=50 or len(cur)>=3: rej.append((p[1],t)); continue
            cur.append(f'{t}:{s}'); names.add(t); added+=1
rows={k:v for k,v in rows.items() if v}
open(B,'w',encoding='utf-8').write(''.join(hdr)+'\n'.join('|'.join(list(k)+v) for k,v in rows.items())+'\n')
c=collections.Counter(len(v) for v in rows.values())
print('added',added,'rejected',rej,'towns',len(rows),dict(c))
