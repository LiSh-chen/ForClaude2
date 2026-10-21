import numpy as np, cv2
from PIL import Image
f=np.array(Image.open('full.png')); rgb=f[...,:3].copy(); alpha=f[...,3].copy()
H,W=alpha.shape
hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV); h,s,v=[hsv[...,i].astype(int) for i in range(3)]
brown=((h>=5)&(h<=24)&(s>=55)&(v>=35)&(v<=215))
reg=np.zeros((H,W),bool); reg[55:790,0:172]=True
b=(brown&reg).astype(np.uint8)
b=cv2.morphologyEx(b,cv2.MORPH_CLOSE,np.ones((3,3),np.uint8))
n,lab=cv2.connectedComponents(b,connectivity=8)
comp=(lab==lab[769,135])|(lab==lab[250,71])
# keep only pixels near the staff's own centre line (drops hat texture and cape edge)
staff=np.zeros((H,W),bool)
cx=None
for y in range(H):
    xs=np.where(comp[y])[0]
    if y<150:
        staff[y,xs[xs<=108]]=True
        continue
    xs=xs[xs<=150]
    if len(xs)==0: continue
    m=np.median(xs) if cx is None else cx
    keep=xs[np.abs(xs-m)<=22]
    if len(keep): staff[y,keep]=True; cx=np.median(keep)
core=staff.copy()
# outline: dark pixels touching the staff
dil=cv2.dilate(staff.astype(np.uint8),np.ones((3,3),np.uint8),iterations=2).astype(bool)
dark=(v<110)
staff=staff|(dil&dark&(alpha>0)&np.pad(np.ones((H,W-120),bool),((0,0),(0,120)))[:H,:W]&(np.arange(W)[None,:]<=150))
# lantern
lant=np.zeros((H,W),bool); lant[165:340,8:96]=True; lant&=(alpha>0)
staff|=lant
HAND=(72,398,142,468)  # x0,y0,x1,y1
hx0,hy0,hx1,hy1=HAND
handbox=np.zeros((H,W),bool); handbox[hy0:hy1,hx0:hx1]=True
hand=handbox&(alpha>0)&~core
staff&=~hand
# staff layer: fill the part hidden behind the hand by repeating the row above
st_rgb=rgb.copy(); st_a=np.where(staff,alpha,0).astype(np.uint8)
y_above=hy0-1
for y in range(hy0,hy1):
    row=staff[y_above]
    cols=np.where(row)[0]
    if len(cols)==0: continue
    shift=int(round((y-y_above)*0.12))
    for c in cols:
        cc=min(W-1,c+shift)
        st_rgb[y,cc]=rgb[y_above,c]; st_a[y,cc]=255
# body layer: everything except the staff/lantern; holes over the hat tip are filled
hole=staff&(alpha>0)
tip=np.zeros((H,W),bool); tip[40:135,55:150]=True
fill=hole&tip&~lant
bd_rgb=cv2.inpaint(rgb,(fill*255).astype(np.uint8),5,cv2.INPAINT_TELEA)
bd_a=np.where(staff&~fill,0,alpha).astype(np.uint8)
bd_a[handbox&hand]=alpha[handbox&hand]
ha_a=np.where(hand,alpha,0).astype(np.uint8)
def save(name,r,a): Image.fromarray(np.dstack([r,a])).save(name)
save('L_body.png',bd_rgb,bd_a); save('L_staff.png',st_rgb,st_a); save('L_hand.png',rgb,ha_a)
# rest-pose check
base=Image.new('RGBA',(W,H),(255,255,255,255))
for n_ in ['L_body.png','L_staff.png','L_hand.png']: base=Image.alpha_composite(base,Image.open(n_))
orig=Image.alpha_composite(Image.new('RGBA',(W,H),(255,255,255,255)),Image.open('full.png'))
d=np.abs(np.array(base).astype(int)-np.array(orig).astype(int)).sum(axis=2)
print('pixels differing >60:',(d>60).sum())
base.save('rest.png')
