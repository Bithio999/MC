#!/usr/bin/env python3
"""Original periodic accretion striations. No downloaded game/NASA imagery."""
from pathlib import Path
from PIL import Image
import math
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt/textures/effect'
width,height=512,96
image=Image.new('RGBA',(width,height));pixels=image.load()
for y in range(height):
 v=(y+.5)/height;envelope=math.sin(v*math.pi)**.42
 for x in range(width):
  a=(x+.5)/width*math.tau
  streak=(.72+.10*math.sin(a*7+v*18)+.08*math.sin(a*17-v*24)+.055*math.cos(a*31+v*12))
  fine=.82+.18*math.sin(v*36+a*3)**6
  value=max(0,min(255,int(255*streak*fine)))
  pixels[x,y]=(int(value*envelope),int(value*envelope),int(value*envelope),int(255*envelope))
image.save(root/'singularity_accretion.png')
print('Generated original seamless accretion strip',image.size)

# RGB is premultiplied by the mask: the native additive shader blends ONE, ONE.
# An original optical burst: a soft core, tapered radial filaments and two anamorphic streaks.
size=512;burst=Image.new('RGBA',(size,size));pixels=burst.load()
for y in range(size):
 for x in range(size):
  u=(x+.5-size/2)/(size/2);v=(y+.5-size/2)/(size/2);r=math.hypot(u,v);a=math.atan2(v,u)
  envelope=max(0,1-r)**2
  spikes=(max(0,math.cos(a*11+.8*math.sin(a*5))))**32
  core=math.exp(-r*r*60)*.8
  ray=spikes*math.exp(-r*3)*envelope
  streak=math.exp(-abs(v)*170)*math.exp(-abs(u)*4)*.4
  corona=.4*math.exp(-((r-.32)/.10)**2)*max(0,.55+.45*math.sin(a*9+math.sin(a*3)*2+r*26))**3
  value=min(1,core+ray+streak+corona);pixels[x,y]=(int(value*255),int(value*255),int(value*255),int(value*255))
burst.save(root/'singularity_burst.png')
print('Generated original optical burst',burst.size)
