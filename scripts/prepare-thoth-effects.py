#!/usr/bin/env python3
"""Reproducible original spell textures. Pillow is needed only to regenerate assets."""
from pathlib import Path
import math
from PIL import Image, ImageDraw, ImageFilter
OUT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt/textures/effect'
OUT.mkdir(parents=True,exist_ok=True)
N=1024
ink=Image.new('RGBA',(N,N));d=ImageDraw.Draw(ink)
def xy(a,r):return (512+math.cos(a)*r,512+math.sin(a)*r)
for r,w in [(486,3),(475,1),(455,2),(394,2),(381,1),(336,3),(322,1),(241,2),(225,1),(100,2)]:
 d.ellipse((512-r,512-r,512+r,512+r),outline='white',width=w)
for n in range(120):
 a=n*math.tau/120;d.line([xy(a,459),xy(a,473 if n%5 else 485)],fill='white',width=2 if n%5 else 3)
for ring,r in enumerate([422,355,271]):
 for n in range(36):
  a=n*math.tau/36+ring*.09
  # Individually constructed angular astral glyphs, with clear space between characters.
  tangent=(-math.sin(a),math.cos(a));rad=(math.cos(a),math.sin(a));c=xy(a,r)
  def p(x,y):return (c[0]+tangent[0]*x+rad[0]*y,c[1]+tangent[1]*x+rad[1]*y)
  d.line([p(-6,-10),p(-6,9),p(5,9),p(5,-4),p(-6,-4)],fill='white',width=2)
  if n%3:d.line([p(-9,0),p(9,0)],fill='white',width=2)
  if n%2:d.line([p(0,-11),p(0,12)],fill='white',width=2)
for n in range(8):
 a=n*math.tau/8;d.line([xy(a,213),xy(a+3*math.tau/8,213)],fill='white',width=2)
for n in range(4):
 a=n*math.pi/2;d.line([xy(a,82),xy(a+math.pi/2,82)],fill='white',width=3)
# Soft local halos retain the fine central lines, without requiring an external shader.
glow=ink.filter(ImageFilter.GaussianBlur(5));glow.putalpha(glow.getchannel('A').point(lambda a:int(a*.65)))
Image.alpha_composite(glow,ink).save(OUT/'thoth_seal.png')
# UV page panel, independently rendered on each animated half of the open volume.
b=Image.new('RGBA',(256,256),(23,35,73,255));q=ImageDraw.Draw(b)
q.rounded_rectangle((3,3,252,252),radius=8,outline=(209,164,70),width=7)
q.rectangle((14,12,241,241),fill=(238,224,180),outline=(255,241,204),width=3)
for n in range(20):
 y=29+n*10
 for col in range(5):
  x=25+col*43;end=x+29-((n*17+col*7)%15)
  q.line((x,y,end,y),fill=(73,91,117),width=2)
  if (n+col)%3==0:q.line((x+8,y-2,x+8,y+2),fill=(105,91,64),width=2)
q.rectangle((19,19,236,236),outline=(169,130,57),width=2)
b.save(OUT/'thoth_grimoire.png')
# White soft glow: tint, size and exposure are animated in the renderer.
g=Image.new('RGBA',(128,128));px=g.load()
for y in range(128):
 for x in range(128):
  r=math.hypot((x-63.5)/64,(y-63.5)/64)
  a=max(0,1-r)**3
  value=round(a*255)
  px[x,y]=(value,value,value,value)
g.save(OUT/'thoth_glow.png')
print('Generated three original Thoth effect textures')
