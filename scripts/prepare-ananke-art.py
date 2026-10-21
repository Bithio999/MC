#!/usr/bin/env python3
"""Original model atlas, emissive mask, pixel portraits and encounter-item artwork."""
from pathlib import Path
from PIL import Image,ImageDraw
import math,random,json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt'
(root/'textures/entity').mkdir(parents=True,exist_ok=True);(root/'textures/item').mkdir(parents=True,exist_ok=True)
palette=[(223,216,194),(27,22,41),(196,145,65),(126,86,193)]
atlas=Image.new('RGBA',(128,128));emit=Image.new('RGBA',(128,128),(0,0,0,0));rng=random.Random(2501)
for tile,base in enumerate(palette):
 ox=(tile%2)*64;oy=(tile//2)*64
 for y in range(64):
  for x in range(64):
   grain=rng.uniform(-5,5);seam=(x%16==0 or y%16==0);factor=.86 if seam else 1
   rgb=tuple(int(max(0,min(255,v*factor+grain))) for v in base)
   atlas.putpixel((ox+x,oy+y),(*rgb,255))
   if tile==3:
    bright=.6+.35*math.sin((x+y)*.1)**2;emit.putpixel((ox+x,oy+y),(int(165*bright),int(100*bright),int(245*bright),255))
atlas.save(root/'textures/entity/ananke.png');emit.save(root/'textures/entity/ananke_emissive.png')
# Pixel-art faceplate and six wings, with symmetric margins and a separate crown.
icon=Image.new('RGBA',(64,64),(0,0,0,0));d=ImageDraw.Draw(icon)
gold=(241,192,100,255);dark=(26,19,43,255);ivory=(237,228,199,255);purple=(171,122,240,255)
for side in [-1,1]:
 for tier in range(3):
  x0=32+side*9;y=25+tier*8
  d.polygon([(x0,y),(32+side*27,y-10+tier*4),(32+side*22,y+5),(x0,y+7)],fill=ivory,outline=gold)
  for f in range(4):d.line((x0+side*(f*4),y,32+side*(15+f*3),y+6),fill=gold,width=1)
d.ellipse((16,4,48,17),outline=gold,width=2)
d.polygon([(20,23),(24,15),(28,21),(32,12),(36,21),(40,15),(44,23),(42,46),(32,54),(22,46)],fill=gold,outline=dark)
d.polygon([(25,25),(39,25),(37,45),(32,49),(27,45)],fill=dark)
d.rectangle((31,28,33,42),fill=purple);d.rectangle((30,29,34,31),fill=ivory)
for path in ['textures/skill/moira.png','textures/race/eclipse_seraph.png']:(root/path).parent.mkdir(parents=True,exist_ok=True);icon.save(root/path)
for name,kind in [('writ_of_last_dawn',0),('dawn_testament',1)]:
 im=Image.new('RGBA',(32,32),(0,0,0,0));p=ImageDraw.Draw(im)
 p.polygon([(8,3),(24,3),(26,26),(20,29),(6,29)],fill=ivory,outline=gold)
 p.rectangle((9,7,21,9),fill=dark);p.rectangle((10,22,22,24),fill=dark)
 p.ellipse((10,11,22,21),fill=dark,outline=gold,width=2)
 if kind:p.line((16,12,16,19),fill=purple,width=2);p.line((12,16,20,16),fill=purple,width=2)
 else:p.arc((11,12,21,20),10,320,fill=purple,width=2)
 im.save(root/'textures/item'/f'{name}.png')
 (root/'models/item').mkdir(parents=True,exist_ok=True)
 (root/'models/item'/f'{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'wildhunt:item/{name}'}},indent=2)+'\n')
(root/'models/item/ananke_spawn_egg.json').write_text('{"parent":"minecraft:item/template_spawn_egg"}\n')
print('Generated Ananke atlas, emissive mask, centered pixel portraits and item artwork.')
