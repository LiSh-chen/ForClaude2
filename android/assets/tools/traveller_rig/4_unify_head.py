"""Gives every walk frame the same head: the hat and face of one reference frame (default 0) replace the head above a
seam just under the mouth, so the traveller does not swivel its head from frame to frame (the generated sheet turned it
from 3/4 front to full profile).  Usage: python3 4_unify_head.py <frames_dir> <out_dir> [ref_frame]
The eye positions below were measured on the six frames of generated/traveller_walk_sheet_cute.jpg after 3_slice_walk.py;
measure again for a new sheet (the big, near eye of each frame, in pixels of the 681 x 708 frames)."""
import cv2,numpy as np,sys,os
from PIL import Image
SRC=sys.argv[1]; OUT=sys.argv[2]; REF=int(sys.argv[3]) if len(sys.argv)>3 else 0
SEAM_=60
eyes={0:(307,260),1:(346,265),2:(340,240),3:(296,252),4:(368,245),5:(375,233)}
os.makedirs(OUT,exist_ok=True)
def load(i): return np.array(Image.open(f'{SRC}/art_traveller_walk_{i}.webp').convert('RGBA'))
def head_mask(im,i):
    rgb=im[...,:3]; a=im[...,3]
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    white=((hsv[...,1]<40)&(hsv[...,2]>185)&(a>200)).astype(np.uint8)
    cl=cv2.morphologyEx(white,cv2.MORPH_CLOSE,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(41,41)))
    n,lab=cv2.connectedComponents(cl,connectivity=4)
    ex,ey=eyes[i]
    m=(lab==lab[ey+58,ex+18]).astype(np.uint8)
    # the hat: cream and green-band pixels touching the head (the tip and brim edge are not white enough for the head mask)
    H,S,V=hsv[...,0].astype(int),hsv[...,1].astype(int),hsv[...,2].astype(int)
    cream=(H>=8)&(H<=35)&(S>=22)&(S<=120)&(V>=170)
    band=(H>=36)&(H<=90)&(S>=35)&(V>=50)&(V<=200)
    hatc=((cream|band)&(a>200)).astype(np.uint8)
    hatc=cv2.morphologyEx(hatc,cv2.MORPH_CLOSE,np.ones((5,5),np.uint8))
    n2,lab2,st2,_=cv2.connectedComponentsWithStats(hatc,connectivity=8)
    near=cv2.dilate(m,np.ones((9,9),np.uint8))
    hat=np.zeros_like(m)
    ys_cut=ey+SEAM_
    for k in range(1,n2):
        if st2[k,4]>1500 and (near[lab2==k]>0).any() and st2[k,1]+st2[k,3]<ys_cut+40 and st2[k,1]<ey:
            hat|=(lab2==k).astype(np.uint8)
    return (m|hat).astype(np.uint8)
frames=[load(i) for i in range(6)]
masks=[head_mask(frames[i],i) for i in range(6)]
SEAM=60
def seam(i):
    ys=eyes[i][1]+SEAM
    xs=np.where(masks[i][ys]>0)[0]
    return ys,xs.min(),xs.max()
R=frames[REF]; MR=masks[REF]; ysR,xlR,xrR=seam(REF)
K=np.ones((1,1),np.uint8)
def dil(m,r): return cv2.dilate(m,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(2*r+1,2*r+1)))
h,w=R.shape[:2]
yy=np.arange(h)[:,None]
for i in range(6):
    T=frames[i].copy()
    if i==REF:
        Image.fromarray(T).save(f'{OUT}/art_traveller_walk_{i}.webp',quality=92,method=6); continue
    ysT,xlT,xrT=seam(i)
    sx=(xrT-xlT)/(xrR-xlR); cxR=(xlR+xrR)/2; cxT=(xlT+xrT)/2
    # erase old head above the seam
    er=(dil(masks[i],11)>0)&(yy<ysT)
    T[er,3]=0
    # R head layer (above its seam, with an 8 px overlap band) warped onto T
    layer=R.copy()
    keep=(dil(MR,6)>0)&(yy<ysR+8)
    layer[~keep,3]=0
    M=np.float32([[sx,0,cxT-sx*cxR],[0,1,ysT-ysR]])
    wl=cv2.warpAffine(layer,M,(w,h),flags=cv2.INTER_CUBIC,borderValue=(0,0,0,0))
    al=wl[...,3].astype(np.float32)/255
    # ramp: full above ysT-2, fading to 0 by ysT+8
    ramp=np.clip((ysT+8-np.arange(h)[:,None])/10.0,0,1)
    al=al*ramp
    t_al=T[...,3].astype(np.float32)/255
    out_a=al+t_al*(1-al)
    rgb=(wl[...,:3]*al[...,None]+T[...,:3]*(t_al*(1-al))[...,None])/np.maximum(out_a[...,None],1e-6)
    T[...,:3]=np.clip(rgb,0,255).astype(np.uint8); T[...,3]=np.clip(out_a*255,0,255).astype(np.uint8)
    Image.fromarray(T).save(f'{OUT}/art_traveller_walk_{i}.webp',quality=92,method=6)
# preview grid
ims=[Image.open(f'{OUT}/art_traveller_walk_{i}.webp') for i in range(6)]
W,H=ims[0].size
sheet=Image.new('RGBA',(W//2*3,H//2*2),(235,225,200,255))
for i,im in enumerate(ims):
    sheet.alpha_composite(im.resize((W//2,H//2)),((i%3)*(W//2),(i//3)*(H//2)))
sheet.convert('RGB').save(f'{OUT}/grid.png')
