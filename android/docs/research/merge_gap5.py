import re,glob,collections
R='docs/research/'
DROP=set('''苓仔寮 糖廠五分車 他里霧 楊梅壢 伯公岡 四四南村 五分埔 南風澳 楠梓坑 三山歸一坑 張路寮 吉野村 北投地名 里壠 關山米 前矜 清水得名 桃仔園 千塘之鄉 璞石閣 半路竹 雙溪口 澗仔壢 龍岡米干 頭圍 宛里社 加羅板 小北門 秀水地名 港仔墘 水堀頭 莿桐巷 枋寮地名 林仔邊舊名 樹林口 五汊大街 五大坑 蓮池潭 瑪陵坑 鳳林菸樓 鳳林校長 卑南遺址 道東書院 湖口老街 鯉魚潭 羅山瀑布 新瓦屋 劍獅 紅毛港 夢想社區 忠順廟 北埔遺址 龍田移民村 南靖糖廠 擎天崗 芝山岩石獅 迓三月十五 鯖魚 三貂角 楊梅壢 利澤簡 虎頭山 埤頭F4 埔鹽 土魠魚羹 寧靖王衣冠塚 西隘門 蕭家古厝 秀水地名 廣福宮 福寶酪農區 忠順廟 湖內皮蛋 挑'''.split())
SUP='最|唯一|第一|全台|全國|僅存'
weak=set()
for f in glob.glob(R+'gap5_out_[0-9]_sources.md'):
    for l in open(f,encoding='utf-8'):
        p=[x.strip() for x in l.split('|')]
        if len(p)>=3 and '*' in p[2]: weak.add((p[0],p[1]))
rows=collections.OrderedDict()
for l in open(R+'keep5_result.txt',encoding='utf-8'):
    p=l.rstrip('\n').split('|'); rows[(p[0],p[1])]=p[2:]
order=[tuple(l.strip().split('|')) for f in sorted(glob.glob(R+'input_towns_*.txt')) for l in open(f) if l.strip()]
added=0;skip=[]
for f in sorted(glob.glob(R+'gap5_out_[0-9].txt')):
    for l in open(f,encoding='utf-8'):
        p=l.strip().split('|')
        if len(p)<3: continue
        k=(p[0],p[1]); cur=rows.setdefault(k,[]); names={x.split(':')[0] for x in cur}
        for x in p[2:]:
            if ':' not in x: continue
            t,s=x.split(':',1)
            bad=t in DROP or (p[1],t) in {(b,c) for b,c in weak} or t in names or len(cur)>=3 or re.search(SUP,s) or not 14<=len(s)<=60
            if bad: skip.append(t); continue
            # tag must be the subject: sentence mentions the tag (or its key part) in the first 14 chars
            key=t[:2]
            if key not in s[:16]: skip.append(('mismatch',t)); continue
            cur.append(f'{t}:{s}'); names.add(t); added+=1
idx={k:i for i,k in enumerate(order)}
final=sorted(((k,v) for k,v in rows.items() if v),key=lambda kv:idx.get(kv[0],999))
c=collections.Counter(len(v) for k,v in final)
print('added',added,'towns',len(final),dict(c)); print('skipped',len(skip))
print('no tags:',[k[1] for k in order if k not in dict(final)])
hdr='''# county|name|tag:did you know|tag:did you know|tag:did you know   (1-3 tags per township; one is found per lap)
# Strict pass 2026-10-20: no shops, tag = subject of the sentence, memorable detail; see docs/research.
'''
open('app/src/main/res/raw/town_blurbs.txt','w',encoding='utf-8').write(hdr+'\n'.join('|'.join(list(k)+v) for k,v in final)+'\n')
