import re,glob,collections
R='docs/research/'
weak=set()
for f in glob.glob(R+'gap3_out_[0-9]_sources.md'):
    for l in open(f,encoding='utf-8'):
        p=[x.strip() for x in l.split('|')]
        if len(p)>=3 and '*' in p[2]: weak.add((p[0],p[1]))
EXTRA={('前金區','切仔担'),('前金區','火山拉麵'),('前金區','厚皮蔥油餅'),('林園區','烏魚鰻苗')}
rows=collections.OrderedDict()
for l in open(R+'keep3_result.txt',encoding='utf-8'):
    p=l.rstrip('\n').split('|')
    rows[(p[0],p[1])]=p[2:]
order=[tuple(l.strip().split('|')) for f in sorted(glob.glob(R+'input_towns_*.txt')) for l in open(f) if l.strip()]
added=0;skipped=[]
for f in sorted(glob.glob(R+'gap3_out_[0-9].txt')):
    for l in open(f,encoding='utf-8'):
        p=l.strip().split('|')
        if len(p)<3: continue
        k=(p[0],p[1]); cur=rows.setdefault(k,[]); names={x.split(':')[0] for x in cur}
        for x in p[2:]:
            if ':' not in x: continue
            t,s=x.split(':',1)
            if (p[1],t) in weak or (p[1],t) in EXTRA or t in names or len(cur)>=3 or re.search('最|唯一|第一|全台|全國|僅存',s) or not 14<=len(s)<=50: skipped.append((p[1],t)); continue
            cur.append(f'{t}:{s}'); names.add(t); added+=1
idx={k:i for i,k in enumerate(order)}
final=sorted(((k,v) for k,v in rows.items() if v),key=lambda kv:idx.get(kv[0],999))
c=collections.Counter(len(v) for k,v in final)
print('added',added,'skipped',len(skipped),'towns',len(final),dict(c))
print('no tags:',[k[1] for k in order if k not in dict(final)])
open('/tmp/final_towns.txt','w',encoding='utf-8').write('\n'.join('|'.join(list(k)+v) for k,v in final)+'\n')
print('weak',len(weak))
