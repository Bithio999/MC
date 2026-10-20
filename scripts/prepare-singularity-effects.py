#!/usr/bin/env python3
"""Original periodic accretion striations. No downloaded game/NASA imagery."""
from pathlib import Path
from PIL import Image
import math
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt/textures/effect'
width,height=512,96
image=Image.new('RGBA',(width,height));pixels=image.load()
for y in range(height):
 v=(y+.5)/height;envelope=math.sin(v*math.pi)**.65
 for x in range(width):
  a=(x+.5)/width*math.tau
  streak=(.56+.19*math.sin(a*11+v*21)+.12*math.sin(a*23-v*35)+.08*math.cos(a*47+v*18))
  fine=.72+.28*math.sin(v*40+a*7)**4
  value=max(0,min(255,int(255*streak*fine)))
  pixels[x,y]=(value,value,value,int(255*envelope))
image.save(root/'singularity_accretion.png')
print('Generated original seamless accretion strip',image.size)
