#!/usr/bin/env python3
"""Original 64px mathematical astrolabe: intersecting golden orbits and a luminous equation."""
from pathlib import Path
from PIL import Image, ImageDraw
import math
root=Path(__file__).resolve().parents[1]
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.polygon([(18,4),(45,4),(59,18),(59,45),(45,59),(18,59),(4,45),(4,18)],fill='#091d2d',outline='#967549',width=2)
d.polygon([(19,7),(44,7),(56,19),(56,44),(44,56),(19,56),(7,44),(7,19)],outline='#345f78')
for radius in [23,19]:d.ellipse((31-radius,31-radius,31+radius,31+radius),outline='#629cae' if radius==23 else '#31516d',width=1)
for theta in [0,math.pi/3,2*math.pi/3]:
 points=[]
 for step in range(97):
  a=2*math.pi*step/96;xx=24*math.cos(a);yy=9*math.sin(a);points.append((round(31+xx*math.cos(theta)-yy*math.sin(theta)),round(31+xx*math.sin(theta)+yy*math.cos(theta))))
 d.line(points,fill='#c59b58',width=1)
# Coherent sigma glyph, two bright output bars and one flowing input thread.
d.polygon([(19,21),(34,21),(34,25),(26,25),(31,31),(25,38),(34,38),(34,42),(19,42),(19,38),(26,31),(19,25)],fill='#f3deb0',outline='#80643c')
d.rectangle((39,27,47,29),fill='#a6edee');d.rectangle((39,34,47,36),fill='#a6edee')
for x,y in [(31,7),(10,42),(52,43)]:
 d.rectangle((x-2,y-2,x+2,y+2),fill='#275e77');d.rectangle((x-1,y-1,x+1,y+1),fill='#f7e6b3');d.point((x,y),fill='white')
for x,y in [(12,18),(49,15),(21,53),(46,52)]:d.point((x,y),fill='#94d8e6')
for p in [root/'standalone/tensura-additional-additions/src/main/resources/assets/heavenlyrestriction/textures/skill/laplace.png',root/'wiki/assets/additions/laplace.png']:
 p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
print('Exported Laplace with transparent safe margin.')
