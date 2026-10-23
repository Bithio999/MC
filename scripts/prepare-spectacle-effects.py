#!/usr/bin/env python3
"""Author straight-alpha masks for the anime spectacle renderer."""
from pathlib import Path
from PIL import Image
import math
out=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt/textures/effect'
# Straight-alpha masks: white RGB retains color saturation; opacity carries falloff.
for name in ['anime_streak','anime_flare','anime_smoke','anime_halo']:
 n=256;mask=Image.new('L',(n,n));px=mask.load()
 for y in range(n):
  for x in range(n):
   u=(x+.5)/n*2-1;v=(y+.5)/n*2-1
   if name=='anime_streak':
    edge=max(0,1-abs(v));a=edge**1.3
   elif name=='anime_flare':
    angle=math.atan2(v,u);r=math.hypot(u,v);reach=.17+.7*abs(math.cos(angle*2))**12+.1*abs(math.sin(angle*7))**20;a=max(0,1-r/reach)**.55
   elif name=='anime_halo':
    a=max(0,1-math.hypot(u,v))**2.8
   else:
    angle=math.atan2(v,u);r=math.hypot(u,v);reach=.64+.13*math.sin(angle*5)+.07*math.cos(angle*9);a=max(0,min(1,(reach-r)*7))*.7
   px[x,y]=int(255*max(0,min(1,a)))
 Image.merge('RGBA',(Image.new('L',(n,n),255),)*3+(mask,)).save(out/(name+'.png'))
print('Authored four straight-alpha anime masks.')
