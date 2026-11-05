#!/usr/bin/env python3
"""Original, deterministic 64px crystal and orb sprites for the High Realm crafting chain."""
from pathlib import Path
import colorsys, math
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/heavenlyrestriction/textures/item'
ROOT.mkdir(parents=True,exist_ok=True)
def shard(name,hue,points):
    im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
    palette=[tuple(round(v*255) for v in colorsys.hsv_to_rgb(hue,s,v))+(255,) for s,v in [(0.62,.24),(.75,.55),(.68,.86),(.38,1),(.12,1)]]
    d.polygon([(x+1,y+2) for x,y in points],fill=(8,12,26,100))
    d.polygon(points,fill=palette[0]);cx,cy=31,30
    for n in range(len(points)):
        a,b=points[n],points[(n+1)%len(points)]
        d.polygon([a,b,(cx,cy)],fill=palette[1+n%3])
    d.line(points+[points[0]],fill=palette[0],width=2)
    d.line([points[0],(cx,cy),points[-1]],fill=palette[4],width=2)
    d.line([points[0],points[1]],fill=palette[4],width=2)
    d.polygon([(cx-1,cy-4),(cx+3,cy),(cx,cy+7)],fill=palette[3])
    d.line([(17,53),(22,49),(26,51)],fill=palette[1],width=2)
    im.save(ROOT/(name+'.png'))
shard('bulwark_shard',.012,[(27,6),(47,15),(51,35),(35,56),(14,46),(10,23)])
shard('sentinel_shard',.57,[(31,3),(49,25),(42,48),(30,60),(17,45),(13,25)])
shard('skirmisher_shard',.37,[(43,4),(48,31),(34,55),(17,58),(23,35),(16,23)])

im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.polygon([(32,3),(49,17),(56,36),(43,55),(27,61),(9,42),(11,20)],fill=(9,13,28,255))
facets=[([(32,3),(49,17),(31,28)],(62,86,126,255)), ([(49,17),(56,36),(31,28)],(25,43,78,255)), ([(56,36),(43,55),(31,28)],(18,25,49,255)), ([(43,55),(27,61),(31,28)],(43,59,83,255)), ([(27,61),(9,42),(31,28)],(18,33,57,255)), ([(9,42),(11,20),(31,28)],(32,59,83,255)), ([(11,20),(32,3),(31,28)],(107,154,172,255))]
for p,c in facets:d.polygon(p,fill=c)
d.line([(12,20),(32,5),(48,18)],fill=(223,189,112,255),width=2)
d.polygon([(31,13),(35,25),(45,29),(35,33),(31,47),(27,34),(19,29),(27,26)],fill=(71,190,208,255))
d.polygon([(31,18),(33,27),(40,29),(33,31),(31,40),(29,31),(23,29),(29,28)],fill=(225,252,240,255))
im.save(ROOT/'starless_core.png')

im=Image.new('RGBA',(64,64));pixels=im.load();cx=cy=31.5
for y in range(64):
 for x in range(64):
  xx=(x-cx)/25;yy=(y-cy)/25;r=math.hypot(xx,yy)
  if r>1.09:continue
  theta=math.atan2(yy,xx);h=(theta/(2*math.pi)+.58)%1
  if r>1.01:
   pixels[x,y]=(8,18,35,255 if r<1.055 else 100);continue
  z=math.sqrt(max(0,1-r*r));light=max(.12,(-xx*.43-yy*.57+z*.75));
  bands=.68+.24*math.sin(theta*6+r*8)
  sat=.68 if r<.92 else .32;value=min(1,.25+light*.7+(.15 if r>.9 else 0))
  color=colorsys.hsv_to_rgb(h,sat,value*bands)
  pixels[x,y]=tuple(int(v*255) for v in color)+(255,)
d=ImageDraw.Draw(im)
d.arc((10,9,51,52),190,265,fill=(222,250,255,255),width=2)
d.polygon([(22,13),(30,12),(34,16),(27,20),(20,20),(18,16)],fill=(238,255,255,235))
d.line([(34,48),(41,44),(46,34)],fill=(143,236,241,255),width=2)
d.line([(9,31),(54,31)],fill=(214,199,255,80),width=1)
d.polygon([(46,10),(48,15),(53,17),(48,19),(46,24),(44,19),(39,17),(44,15)],fill=(250,250,221,255))
im.save(ROOT/'chromatic_orb.png')
print('Authored five 64px Chromatic crafting sprites.')
