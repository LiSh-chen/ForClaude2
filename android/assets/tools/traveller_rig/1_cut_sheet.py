import sys, numpy as np, cv2
from PIL import Image
sys.path.insert(0,'tools')
SRC='/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/images/49.jpg'
OUT='/tmp/claude-0/-home-user-ForClaude2/196e5780-5a3f-5410-b95c-ebb5241111cb/scratchpad/rig/'
im=Image.open(SRC).convert('RGB').crop((668,150,1350,950))   # middle figure, 682x800
rgb=np.array(im)
H,W,_=rgb.shape
# background: near-white connected to the border
white=(rgb.min(axis=2)>232)
mask=(~white).astype(np.uint8)
# keep only pixels not connected-to-border-white
n,lab=cv2.connectedComponents(white.astype(np.uint8),connectivity=4)
border=set(lab[0,:])|set(lab[-1,:])|set(lab[:,0])|set(lab[:,-1])
bg=np.isin(lab,[b for b in border if b!=0]) & white
alpha=np.where(bg,0,255).astype(np.uint8)
# soften edge
alpha=cv2.GaussianBlur(alpha,(3,3),0)
hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
h,s,v=hsv[...,0].astype(int),hsv[...,1].astype(int),hsv[...,2].astype(int)
brown=((h>=5)&(h<=22)&(s>=70)&(v>=50)&(v<=200))
region=np.zeros((H,W),bool); region[55:790,0:178]=True
dark=(v<95)
staff=brown&region
# the staff must not take the hat: exclude the cream hat brim area (high value, low sat) automatically by brown test
staff=cv2.dilate(staff.astype(np.uint8),np.ones((3,3),np.uint8),iterations=2).astype(bool)&region&((brown)|(dark)|(s>40))&(alpha>0)
# lantern box (everything non-background there)
lant=np.zeros((H,W),bool); lant[165:340,8:95]=True; lant&=(alpha>0)
staff|=lant
Image.fromarray((staff*255).astype(np.uint8)).save(OUT+'staff_mask.png')
np.save(OUT+'alpha.npy',alpha)
Image.fromarray(np.dstack([rgb,alpha])).save(OUT+'full.png')
print(W,H,staff.sum())
