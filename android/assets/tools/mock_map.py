#!/usr/bin/env python3
"""Mock-up of the planned real-world map: real terrain (SRTM), roads, water and land cover (Overture/OSM) drawn as an old atlas.

Only for checking the look before it goes into the app.  python3 tools/mock_map.py
"""
import math, sys, random, pathlib
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import pyarrow.dataset as ds, pyarrow.fs as pafs, pyarrow.compute as pc
from shapely import wkb
from shapely.geometry import Polygon
import matplotlib; matplotlib.use('Agg')
import matplotlib.pyplot as plt

SCR='/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/'
ASSETS=pathlib.Path(__file__).resolve().parent.parent
FS=pafs.S3FileSystem(anonymous=True,region='us-west-2')
REL='overturemaps-us-west-2/release/2026-09-23.1/'
FONT=ImageFont.truetype('/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc',26)
FONT_S=ImageFont.truetype('/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc',22)

def fetch(theme,typ,bbox,cols,flt=None):
    d=ds.dataset(REL+f'theme={theme}/type={typ}/',filesystem=FS,format='parquet')
    x0,y0,x1,y1=bbox
    f=(pc.field('bbox','xmin')<x1)&(pc.field('bbox','xmax')>x0)&(pc.field('bbox','ymin')<y1)&(pc.field('bbox','ymax')>y0)
    if flt is not None: f=f&flt
    return d.to_table(filter=f,columns=cols).to_pylist()

_tiles={}
def tile(lat,lon):
    k=(lat,lon)
    if k not in _tiles:
        p=pathlib.Path(SCR)/'dem'/f'N{lat:02d}E{lon:03d}.hgt'
        _tiles[k]=np.fromfile(p,dtype='>i2').reshape(3601,3601).astype(np.float32) if p.exists() else None
    return _tiles[k]
def elevation(lat,lon):
    """bilinear sample of SRTM at arrays lat/lon"""
    out=np.zeros(lat.shape,np.float32)
    la0=np.floor(lat).astype(int);lo0=np.floor(lon).astype(int)
    for a in np.unique(la0):
        for b in np.unique(lo0):
            m=(la0==a)&(lo0==b)
            if not m.any(): continue
            t=tile(int(a),int(b))
            if t is None: continue
            y=(1-(lat[m]-a))*3600;x=(lon[m]-b)*3600
            y0=np.clip(np.floor(y).astype(int),0,3599);x0=np.clip(np.floor(x).astype(int),0,3599)
            fy=y-y0;fx=x-x0
            v=t[y0,x0]*(1-fx)*(1-fy)+t[y0,x0+1]*fx*(1-fy)+t[y0+1,x0]*(1-fx)*fy+t[y0+1,x0+1]*fx*fy
            out[m]=v
    out[out<-100]=0
    return out

def geom_pts(g):
    if g.geom_type=='LineString': return [list(g.coords)]
    if g.geom_type=='MultiLineString': return [list(x.coords) for x in g.geoms]
    return []
def polys(g):
    if g.geom_type=='Polygon': return [g]
    if g.geom_type=='MultiPolygon': return list(g.geoms)
    return []

def render(center,width_km,out,aspect=0.92,W=920,route=None,stops=(),walker=None,seed=3,atlas=False):
    lat0,lon0=center
    kx=111.32*math.cos(math.radians(lat0));ky=110.57
    H=int(W/aspect)
    wkm=width_km;hkm=wkm/aspect
    dlon=wkm/kx;dlat=hkm/ky
    west,east=lon0-dlon/2,lon0+dlon/2;south,north=lat0-dlat/2,lat0+dlat/2
    S=2   # supersample
    w,h=W*S,H*S
    def px(lon,lat): return ((lon-west)/(east-west)*w,(north-lat)/(north-south)*h)
    ys,xs=np.mgrid[0:h,0:w]
    lons=west+(xs+.5)/w*(east-west);lats=north-(ys+.5)/h*(north-south)
    el=elevation(lats,lons)
    # coastline polygon -> land mask
    full=np.load(SCR+'full.npy')
    land_img=Image.new('L',(w,h),0);ImageDraw.Draw(land_img).polygon([px(x,y) for x,y in full],fill=255)
    land=np.asarray(land_img)>0
    coast=Image.new('RGBA',(w,h),(0,0,0,0))
    # hillshade
    mpp=(east-west)*kx*1000/w
    from scipy.ndimage import gaussian_filter
    el_s=gaussian_filter(el,S*1.2)
    gy,gx=np.gradient(el_s,mpp)
    slope=np.arctan(2.2*np.hypot(gx,gy));aspect_=np.arctan2(-gx,gy)
    az=math.radians(315);alt=math.radians(42)
    shade=np.sin(alt)*np.cos(slope)+np.cos(alt)*np.sin(slope)*np.cos(az-aspect_)
    shade=np.clip(shade,0,1)
    # hypsometric tint
    stops_h=[(0,(240,230,194)),(40,(232,228,176)),(150,(212,216,160)),(400,(184,200,138)),(800,(160,178,120)),(1500,(170,158,116)),(3000,(205,196,176))]
    e=np.clip(el_s,0,3000)
    col=np.zeros((h,w,3),np.float32)
    xs_=[s[0] for s in stops_h]
    for c in range(3): col[...,c]=np.interp(e,xs_,[s[1][c] for s in stops_h])
    col*= ((0.88+0.22*shade) if atlas else (0.74+0.40*shade))[...,None]
    sea=np.array([214,226,222],np.float32) if atlas else np.array([201,222,224],np.float32)
    img=np.where(land[...,None],col,sea[None,None,:])
    im=Image.fromarray(np.clip(img,0,255).astype(np.uint8))
    d=ImageDraw.Draw(im,'RGBA')
    ImageDraw.Draw(im,'RGBA').line([px(x,y) for x,y in full]+[px(*full[0])],fill=(100,78,52,255),width=int(2*S))
    # land cover
    for r in fetch('base','land_cover',(west,south,east,north),['subtype','geometry']):
        g=wkb.loads(r['geometry']);t=r['subtype']
        colr={'forest':(80,120,66,55),'crop':(225,205,120,70),'urban':(214,184,160,70),'wetland':(110,160,170,90),'shrub':(150,150,90,50),'barren':(200,190,170,70),'grass':(170,200,120,40),'mangrove':(90,140,100,90)}.get(t)
        if not colr: continue
        for p in polys(g):
            d.polygon([px(x,y) for x,y in p.exterior.coords],fill=colr)
    # sea hatching
    sea_mask=~land
    hat=Image.new('RGBA',(w,h),(0,0,0,0));hd=ImageDraw.Draw(hat)
    gap=9*S
    for y in range(0,h,gap): hd.line([(0,y),(w,y)],fill=(90,130,135,70),width=int(1.2*S))
    sm=Image.fromarray((sea_mask*255).astype(np.uint8))
    im.paste(hat,(0,0),Image.composite(hat.split()[3],Image.new('L',(w,h),0),sm))
    d=ImageDraw.Draw(im,'RGBA')
    # contours
    sub=el_s[::S*2,::S*2]
    xs2=np.arange(sub.shape[1])*S*2;ys2=np.arange(sub.shape[0])*S*2
    fig=plt.figure();axc=fig.add_axes([0,0,1,1])
    for lev,alpha,wd in [(l,170 if l%100==0 else 85,1.6 if l%100==0 else .8) for l in (range(100,3600,100) if atlas else range(20,3600,20))]:
        if lev>=sub.max() or lev<=sub.min(): continue
        cs=plt.contour(xs2,ys2,sub,[lev]);
        for seg in cs.allsegs[0]:
            if len(seg)>2: d.line([tuple(p) for p in seg],fill=(120,85,50,alpha//2 if atlas else alpha),width=int(wd*S))
        plt.cla()
    plt.close(fig)
    # water
    for r in fetch('base','water',(west,south,east,north),['class','subtype','geometry','is_intermittent']):
        g=wkb.loads(r['geometry']);cl=r['class']
        if cl in('drain','ditch','swimming_pool','basin','canal'): continue
        if g.geom_type in('LineString','MultiLineString'):
            wd={'river':3.2,'stream':1.4}.get(cl,1.2)
            for pts in geom_pts(g):
                P=[px(x,y) for x,y in pts]
                d.line(P,fill=(86,128,142,230),width=int((wd+1.2)*S)); d.line(P,fill=(176,208,214,255),width=int(wd*S))
        else:
            for p in polys(g):
                if p.area*kx*ky*1e6<8000: continue
                d.polygon([px(x,y) for x,y in p.exterior.coords],fill=(170,205,214,255),outline=(86,128,142,255))
    # roads
    roads=fetch('transportation','segment',(west,south,east,north),['class','geometry','routes'],pc.field('subtype')=='road')
    order={'tertiary':1,'unclassified':0,'secondary':2,'primary':3,'trunk':3}
    for r in sorted(roads,key=lambda r:order.get(r['class'],-1)):
        cl=r['class']
        if cl not in order: continue
        g=wkb.loads(r['geometry']);P=[px(x,y) for x,y in g.coords] if g.geom_type=='LineString' else None
        if not P: continue
        if cl in('primary','trunk'): d.line(P,fill=(246,236,205,255),width=int(5.5*S));d.line(P,fill=(140,96,56,255),width=int(1.6*S))
        elif cl=='secondary': d.line(P,fill=(246,236,205,230),width=int(4*S));d.line(P,fill=(150,108,70,255),width=int(1.2*S))
        elif cl=='tertiary': d.line(P,fill=(150,108,70,170),width=int(.9*S))
    # trees in forest
    rnd=random.Random(seed)
    fm=np.zeros((h,w),bool)
    forest=Image.new('L',(w,h),0);fd=ImageDraw.Draw(forest)
    for r in fetch('base','land_cover',(west,south,east,north),['subtype','geometry'],pc.field('subtype')=='forest'):
        for p in polys(wkb.loads(r['geometry'])): fd.polygon([px(x,y) for x,y in p.exterior.coords],fill=255)
    urb=Image.new('L',(w,h),0);ud=ImageDraw.Draw(urb)
    for r in fetch('base','land_cover',(west,south,east,north),['subtype','geometry'],pc.field('subtype')=='urban'):
        for p in polys(wkb.loads(r['geometry'])): ud.polygon([px(x,y) for x,y in p.exterior.coords],fill=255)
    fm=(np.asarray(forest)>0)&(np.asarray(urb)==0)&(el_s>45)
    # trees: clustered, not on a grid; round crowns low down, pointed conifers high up
    from scipy.ndimage import gaussian_filter as gf
    nz=gf(np.random.default_rng(seed).random((h//(8*S)+1,w//(8*S)+1)),2.5);nz=(nz-nz.min())/(nz.max()-nz.min())
    step=20*S
    for gy_ in (range(step//2,h,step) if not atlas else []):
        for gx_ in range(step//2,w,step):
            x=gx_+rnd.randint(-9*S,9*S);y=gy_+rnd.randint(-9*S,9*S)
            if not(0<=x<w and 0<=y<h and fm[y,x] and land[y,x] and slope[y,x]<0.95): continue
            dens=nz[min(y//(8*S),nz.shape[0]-1),min(x//(8*S),nz.shape[1]-1)]
            if rnd.random()>0.12+0.9*max(0,dens-0.35): continue
            r_=rnd.uniform(4.5,7.5)*S
            if el_s[y,x]>900:
                d.polygon([(x,y-r_*1.9),(x-r_*.8,y),(x+r_*.8,y)],fill=(62,102,64,225),outline=(40,64,40,235))
            else:
                d.ellipse([x-r_,y-r_*1.25-3*S,x+r_,y+r_*.8-3*S],fill=(98,138,80,215),outline=(52,76,42,235))
                d.line([(x,y-2*S),(x,y)],fill=(70,56,38,230),width=int(1.3*S))
    im=im.resize((W,H),Image.LANCZOS)
    # paper grain
    rng=np.random.default_rng(1);grain=rng.normal(0,5,(H,W))[...,None]
    arr=np.asarray(im).astype(np.float32)+grain
    yy,xx=np.mgrid[0:H,0:W];vig=1-0.16*(((xx-W/2)/(W/2))**2+((yy-H/2)/(H/2))**2)
    arr=arr*vig[...,None]*np.array([1.0,0.97,0.9])[None,None,:]
    im=Image.fromarray(np.clip(arr,0,255).astype(np.uint8))
    d=ImageDraw.Draw(im,'RGBA')
    def px1(lon,lat): return ((lon-west)/(east-west)*W,(north-lat)/(north-south)*H)
    # route
    if route:
        P=[px1(x,y) for x,y in route]
        d.line(P,fill=(246,236,205,255),width=9);d.line(P,fill=(176,48,31,255),width=4)
    for name,lat,lon,art in stops:
        x,y=px1(lon,lat)
        R=46
        if art:
            a=Image.open(art).convert('RGB');m=min(a.size);a=a.crop(((a.width-m)//2,(a.height-m)//2,(a.width+m)//2,(a.height+m)//2)).resize((2*R,2*R),Image.LANCZOS)
            mask=Image.new('L',(2*R,2*R),0);ImageDraw.Draw(mask).ellipse([0,0,2*R-1,2*R-1],fill=255)
            d.ellipse([x-R-4,y-R-4,x+R+4,y+R+4],fill=(247,235,203,255),outline=(120,85,50,255),width=3)
            im.paste(a,(int(x-R),int(y-R)),mask)
        else:
            d.ellipse([x-9,y-9,x+9,y+9],fill=(247,235,203,255),outline=(70,110,60,255),width=4)
        tw=d.textlength(name,font=FONT)
        ty=y+R+8 if art else y+12
        d.rounded_rectangle([x-tw/2-7,ty-3,x+tw/2+7,ty+30],radius=6,fill=(247,235,203,235),outline=(185,143,82,255))
        d.text((x-tw/2,ty),name,font=FONT,fill=(59,42,26,255))
    if walker:
        a=Image.open(ASSETS.parent/'app/src/main/res/drawable-nodpi/art_traveller_fox.webp').convert('RGBA');hh=110;ww=int(a.width*hh/a.height);a=a.resize((ww,hh),Image.LANCZOS)
        x,y=px1(walker[1],walker[0]);im.paste(a,(int(x-ww/2),int(y-hh+6)),a)
    im.save(out)
    print('saved',out,im.size,'px =',round(mpp,1),'m')

if __name__=='__main__':
    pass
