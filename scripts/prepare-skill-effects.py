#!/usr/bin/env python3
"""Deterministic original optical masks and surface textures; Pillow is regeneration-only."""
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
OUT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt/textures/effect'
OUT.mkdir(parents=True,exist_ok=True)
N=512
def mask(name,paint,size=N):
 im=Image.new('L',(size,size));paint(ImageDraw.Draw(im),size)
 halo=im.filter(ImageFilter.GaussianBlur(2));im=Image.frombytes('L',im.size,bytes(max(a,int(b*.38)) for a,b in zip(im.tobytes(),halo.tobytes())))
 Image.merge('RGBA',(im,im,im,im)).save(OUT/(name+'.png'))
def point(a,r,n=N):return (n/2+math.cos(a)*r,n/2+math.sin(a)*r)
def arc(d,r,n=N,w=2):d.ellipse((n/2-r,n/2-r,n/2+r,n/2+r),outline=255,width=w)
def silk(d,n):
 for k in range(16):d.line([point(k*math.tau/16,12),point(k*math.tau/16,n*.47)],fill=230,width=1)
 for r in [35,59,88,125,170,220,242]:
  points=[point(k*math.tau/128,r*(.94+.06*math.cos(k*math.pi/4))) for k in range(129)];d.line(points,fill=255,width=2)
 for k in range(8):
  a=k*math.pi/4;c=point(a,172);d.polygon([(c[0],c[1]-8),(c[0]+5,c[1]),(c[0],c[1]+8),(c[0]-5,c[1])],fill=230)
def circuit(d,n):
 for r in [72,126,198,240]:
  pts=[point(k*math.pi/3,r) for k in range(7)];d.line(pts,fill=255,width=3)
 for k in range(12):
  a=k*math.pi/6;b=a+.11;d.line([point(a,80),point(a,148),point(b,168),point(b,235)],fill=255,width=2)
  c=point(b,211);d.rectangle((c[0]-4,c[1]-4,c[0]+4,c[1]+4),outline=255,width=1)
  for r in [100,180]:d.line([point(a-.055,r),point(a+.055,r)],fill=220,width=2)
def frost(d,n):
 for k in range(6):
  a=k*math.pi/3;d.line([point(a,0),point(a,243)],fill=255,width=3)
  for r in [60,105,154,198]:
   p=point(a,r)
   for side in [-1,1]:d.line([p,(p[0]+math.cos(a+side*.8)*42,p[1]+math.sin(a+side*.8)*42)],fill=255,width=2)
 for r in [98,222]:d.line([point(k*math.pi/3,r) for k in range(7)],fill=200,width=1)
def leaf(d,n):
 for k in range(8):
  a=k*math.pi/4
  pts=[point(a+math.sin(t*math.pi)*.18,20+t*218) for t in [v/48 for v in range(49)]];d.line(pts,fill=235,width=2)
  for r in [85,135,190]:
   c=point(a,r);tip=point(a+.2,r+33);side=point(a-.1,r+17);d.line([c,tip,side,c],fill=250,width=2)
 arc(d,239,w=1)
def mirror(d,n):
 for r in [65,125,192,238]:
  d.line([point(k*math.pi/3+math.pi/6,r) for k in range(7)],fill=255,width=2)
 for k in range(6):
  a=k*math.pi/3;d.line([point(a,45),point(a+.65,175),point(a,237),point(a-.65,175),point(a,45)],fill=220,width=2)
def clock(d,n):
 for r,w in [(n*.47,3),(n*.455,1),(n*.405,2),(n*.385,1),(n*.30,2),(n*.16,2)]:arc(d,r,n,w)
 for k in range(120):
  a=k*math.tau/120;d.line([point(a,n*.41,n),point(a,n*(.45 if k%5 else .47),n)],fill=255,width=3 if k%5==0 else 1)
 for k in range(12):
  a=k*math.pi/6
  c=point(a,n*.345,n);rr=12 if n==512 else 24
  d.ellipse((c[0]-rr,c[1]-rr,c[0]+rr,c[1]+rr),outline=255,width=2)
  for j in range(k%4+1):d.line((c[0]-8+j*5,c[1]-7,c[0]-8+j*5,c[1]+7),fill=220,width=2)
 for k in range(3):
  c=point(k*math.tau/3,n*.12,n);d.ellipse((c[0]-n*.075,c[1]-n*.075,c[0]+n*.075,c[1]+n*.075),outline=200,width=2)
def gate(d,n):
 for k in range(6):
  a=k*math.pi/3;pts=[point(a+t*5,15+t*225) for t in [i/120 for i in range(121)]];d.line(pts,fill=255,width=2)
 for r in [70,160,239]:arc(d,r,w=2)
 for k in range(48):
  a=k*math.tau/48;d.line([point(a,210),point(a+.035,235)],fill=210,width=2)
def rift(d,n):
 for k in range(9):
  a=k*math.tau/9;pts=[point(a+math.sin(j*2.3+k)*.12,12+j*23) for j in range(11)];d.line(pts,fill=255,width=2)
  for j in [3,6,8]:d.line([pts[j],point(a+.27,j*25+14)],fill=200,width=1)
for name,paint in [('vfx_silk',silk),('vfx_circuit',circuit),('vfx_frost',frost),('vfx_leaf',leaf),('vfx_mirror',mirror),('vfx_clock',clock),('vfx_gate',gate),('vfx_rift',rift)]:mask(name,paint,1024 if name=='vfx_clock' else N)
# Repeating flow/surface textures: original deterministic analytic fields.
for name in ['wave','stone','ice','bark','cloud','beam','flame']:
 w,h=(256,64) if name=='beam' else (256,256)
 im=Image.new('RGBA',(w,h));p=im.load()
 for y in range(h):
  for x in range(w):
   u=x/w;v=y/h;a=255
   if name=='wave':
    f=abs(math.sin(u*math.tau*3+math.sin(v*math.tau*3)*1.7)*math.sin(v*math.tau*4+math.sin(u*math.tau*2)));b=int(95+150*(1-f)**8);rgb=(b,b,b)
   elif name=='ice':
    f=abs(math.sin(u*14+v*9)*math.sin(v*22-u*12));b=int(180+70*f);rgb=(int(b*.8),int(b*.94),b)
   elif name=='bark':
    f=abs(math.sin(u*80+math.sin(v*19)*1.8));b=int(55+75*f**8);rgb=(b,int(b*.9),int(b*.65))
   elif name=='stone':
    f=abs(math.sin(u*15+math.sin(v*18))*math.sin(v*15+math.sin(u*21)));b=int(20+45*f);rgb=(b,int(b*.8),int(b*.8))
   elif name=='cloud':
    r=math.hypot(u*2-1,v*2-1);f=sum(math.sin((u+v)*k+math.sin(v*k))*1/k for k in [9,17,29]);a=int(max(0,min(1,(1-r)*1.5))**2*(150+55*f));a=max(0,min(230,a));rgb=(150,150,160)
   elif name=='beam':
    across=abs(v*2-1);end=min(1,u*12,(1-u)*12);a=int(255*(1-across)**3*max(0,end));rgb=(a,a,a)
   else:
    width=(1-v)**.7*(.65+.15*math.sin(v*17));f=max(0,1-abs(u*2-1-math.sin(v*18)*v*.22)/max(.01,width));a=int(f**2*min(1,(1-v)*8,v*12)*255);rgb=(a,a,a)
   p[x,y]=(*rgb,a)
 im.save(OUT/('vfx_'+name+'.png'))
def sprite(d,n):
 pts=[(n*.5,n*.96),(n*.12,n*.5),(n*.23,n*.2),(n*.5,n*.04),(n*.78,n*.2),(n*.86,n*.5)]
 d.polygon(pts,fill=175);d.line((n*.5,n*.92,n*.5,n*.12),fill=255,width=3)
 for y in [.3,.45,.6,.75]:
  for side in [-1,1]:d.line((n*.5,n*y,n*(.5+side*.21),n*(y-.15)),fill=240,width=2)
mask('vfx_leaf_sprite',sprite,128)
Image.new('RGBA',(2,2),'white').save(OUT/'vfx_white.png')
print('Generated 17 original skill-effect textures')
