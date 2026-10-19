#!/usr/bin/env python3
"""Original pixel glyphs for the native-sized Godly button and right-side tabs."""
from PIL import Image, ImageDraw
from pathlib import Path
out=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt/textures/gui'
out.mkdir(parents=True,exist_ok=True)
im=Image.new('RGBA',(32,64))
for frame in range(2):
 d=ImageDraw.Draw(im); oy=frame*32
 def poly(points,fill):d.polygon([(x,y+oy) for x,y in points],fill=fill)
 poly([(9,0),(22,0),(31,9),(31,22),(22,31),(9,31),(0,22),(0,9)],'#050610')
 poly([(9,1),(22,1),(30,9),(30,22),(22,30),(9,30),(1,22),(1,9)],'#9d8de0' if not frame else '#75e1e7')
 poly([(10,3),(21,3),(28,10),(28,21),(21,28),(10,28),(3,21),(3,10)],'#29223d' if not frame else '#3b3552')
 poly([(10,5),(21,5),(26,10),(26,21),(21,26),(10,26),(5,21),(5,10)],'#100e20')
 # Causality: orbit, crown and hourglass, centered on the native 32px grid.
 d.ellipse((6,oy+6,25,oy+25),outline='#615090',width=1)
 poly([(9,9),(12,12),(15,7),(18,12),(22,9),(21,14),(10,14)],'#f7dc8e')
 poly([(11,16),(20,16),(16,20),(20,24),(11,24),(15,20)],'#d1a04f')
 d.line((12,oy+17,18,oy+23),fill='#fff2bd',width=1)
 for x,y in [(6,16),(25,16),(16,27)]:d.point((x,y+oy),fill='#fff2bd')
im.save(out/'godly_category.png')
for name in ['secondary_tab','awakening_tab']:
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 if name=='secondary_tab':
  d.ellipse((1,2,10,12),outline='#69d7eb',width=2);d.ellipse((6,3,15,13),outline='#ead18c',width=2)
  d.line((5,7,10,9),fill='#ffffff',width=1)
 else:
  d.polygon([(8,0),(10,5),(15,7),(10,9),(8,15),(6,10),(0,8),(5,6)],fill='#f0d27e')
  d.polygon([(8,4),(11,8),(8,11),(5,8)],fill='#fff9df')
 im.save(out/(name+'.png'))
