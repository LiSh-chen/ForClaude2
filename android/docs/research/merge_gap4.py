import re,glob,collections
R='docs/research/'
EXC={'虎頭山','廟口小吃','碧侯溫泉','牽罟','鴨肉珍','永隆溝蚵殼','鴨母寮市場','龜山朝日','酪農專區','官邸菊展','燒馬蛋','南田石','五層粿','情人橋','週一夜市','蒜頭花生','招潮蟹','熱氣球'}
SUP='最|唯一|第一|全台|全國|僅存'
rows=collections.OrderedDict()
for l in open(R+'keep4_result.txt',encoding='utf-8'):
    p=l.rstrip('\n').split('|'); rows[(p[0],p[1])]=p[2:]
order=[tuple(l.strip().split('|')) for f in sorted(glob.glob(R+'input_towns_*.txt')) for l in open(f) if l.strip()]
added=0;skip=[];long_=[]
for f in sorted(glob.glob(R+'gap4_out_[0-9].txt')):
    for l in open(f,encoding='utf-8'):
        p=l.strip().split('|')
        if len(p)<3: continue
        k=(p[0],p[1]); cur=rows.setdefault(k,[]); names={x.split(':')[0] for x in cur}
        for x in p[2:]:
            if ':' not in x: continue
            t,s=x.split(':',1)
            if t in EXC or t in names or len(cur)>=3 or re.search(SUP,s) or not 14<=len(s)<=60: skip.append((p[1],t,len(s))); continue
            if len(s)>46: long_.append((p[1],t,len(s)))
            cur.append(f'{t}:{s}'); names.add(t); added+=1
idx={k:i for i,k in enumerate(order)}
final=sorted(((k,v) for k,v in rows.items() if v),key=lambda kv:idx.get(kv[0],999))
c=collections.Counter(len(v) for k,v in final)
print('added',added,'skipped',skip,'towns',len(final),dict(c),'long',len(long_))
print('no tags',[k[1] for k in order if k not in dict(final)])
hdr='''# county|name|tag:did you know|tag:did you know|tag:did you know   (1-3 tags per township; one is found per lap)
# Quality pass 2026-10-20 (round 2): dry history/admin/architecture entries removed; see docs/research.
'''
open('app/src/main/res/raw/town_blurbs.txt','w',encoding='utf-8').write(hdr+'\n'.join('|'.join(list(k)+v) for k,v in final)+'\n')
# landmarks
LEXC={('qiaotou','每百公尺'),('jingzijiao','QR'),('anping','奧倫治')}
names={}
for f in ['01','02','03']:
    for l in open(R+f'input_landmarks_{f}.txt'):
        p=l.strip().split('|')
        if len(p)>=5: names[p[0]]=p[1]
facts={i:[] for i in names}
for l in open(R+'keep4_lm.txt',encoding='utf-8'):
    p=l.strip().split('|'); facts[p[0]]=p[1:]
sk=[]
for l in open(R+'gap4_lm_out.txt',encoding='utf-8'):
    p=l.strip().split('|')
    if len(p)<3 or p[0] not in facts: continue
    for s in p[2:]:
        s=s.strip()
        if not s: continue
        if any(i==p[0] and w in s for i,w in LEXC) or re.search(SUP,s) or not 15<=len(s)<=60 or s in facts[p[0]]: sk.append((p[0],s[:16])); continue
        if len(facts[p[0]])<2: facts[p[0]].append(s)
print('lm skipped',sk); print('lm <2:',{i:len(v) for i,v in facts.items() if len(v)<2})
open('app/src/main/res/raw/landmark_facts.txt','w',encoding='utf-8').write('# id|did you know|did you know (quality pass 2026-10-20)\n'+'\n'.join('|'.join([i]+v) for i,v in facts.items() if v)+'\n')
