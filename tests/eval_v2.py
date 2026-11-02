"""Python reference port of the V2 Kotlin detector, for image fixture regression tests."""
from PIL import Image
import numpy as np
from math import fabs

def detector(p):
 b=Image.open(p).convert('RGB')
 if max(b.size)>1750:b=b.resize((b.width//2,b.height//2))
 a=np.asarray(b)
 H,W=a.shape[:2];r=a[:,:,0].astype(np.int16);g=a[:,:,1].astype(np.int16);bl=a[:,:,2].astype(np.int16)
 maskfill=(r>190)&((r-g)>47)&((r-bl)>40)&(g>=76)&(g<=219)&(bl<220)
 mx=np.maximum.reduce([r,g,bl]);mn=np.minimum.reduce([r,g,bl]);mask=maskfill|((r>=151)&(r<=239)&((mx-mn)<11))
 x1=int(W*.05);x2=min(W-1,int(W*.70));minT=int(W*.24);maxT=int(W*.60);maxGap=max(4,int(W*.013));edgeTol=max(5,int(W*.025));rows=[]
 for y in range(int(H*.35),int(H*.94),2):
  first=-1;last=-1;gap=0;best=None
  for x in range(x1,x2+1):
   if mask[y,x]:
    if first<0:first=x
    last=x;gap=0
   elif first>=0:
    gap+=1
    if gap>maxGap:
     ln=last-first+1
     if minT<=ln<=maxT and (best is None or ln>best[2]-best[1]+1):best=y,first,last
     first=-1;last=-1;gap=0
  if first>=0:
   ln=last-first+1
   if minT<=ln<=maxT and (best is None or ln>best[2]-best[1]+1):best=y,first,last
  if best:rows.append(best)
 if not rows:return None
 tracks=[]
 for y,l,rr in rows:
  if tracks and y-tracks[-1][1]<=4 and abs(l-tracks[-1][2])<=edgeTol and abs(rr-tracks[-1][3])<=edgeTol:
   t=tracks[-1];t[1]=y
   if rr-l>t[3]-t[2]:t[2]=l;t[3]=rr;t[4]=y
  else:tracks.append([y,y,l,rr,y])
 filtered=[t for t in tracks if t[1]-t[0]>=max(3,int(H*.003)) and t[1]-t[0]<=H*.020]
 chosen=None;score=-1
 for ai,first in enumerate(filtered):
  for bi in range(ai+1,len(filtered)):
   mid=filtered[bi];gap1=mid[4]-first[4]
   if gap1<H*.025 or gap1>H*.12:continue
   for ci in range(bi+1,len(filtered)):
    last=filtered[ci];gap2=last[4]-mid[4]
    if abs(gap1-gap2)>max(6,max(gap1,gap2)*.24):continue
    if max(abs(first[2]-mid[2]),abs(first[2]-last[2]))>edgeTol:continue
    if max(abs(first[3]-mid[3]),abs(first[3]-last[3]))>edgeTol:continue
    sc=sum(t[3]-t[2]+1 for t in [first,mid,last])
    if sc>score:score=sc;chosen=[first,mid,last]
 if chosen is None:return 'found tracks but no three',filtered
 out=[]
 for _,_,left,right,y in chosen:
  vals=np.count_nonzero(maskfill[y-2:y+3,left:right+1],axis=0)>=2
  ix=np.where(vals)[0]
  if not len(ix):out.append(0);continue
  firstfill,lastfill=ix[0],ix[-1]
  if firstfill>max(5,int(W*.012)) or len(ix)/(lastfill-firstfill+1)<.78:out.append(None);continue
  out.append(int(np.floor(15*(lastfill+1)/(right-left+1)+.5)))
 return out,chosen
