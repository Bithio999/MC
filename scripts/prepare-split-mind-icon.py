#!/usr/bin/env python3
"""Draw the original 64px Split Mind sigil on a pixel grid; no borrowed skill artwork."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
# A restrained, octagonal aureole behind two independently lit cerebral lobes.
d.polygon([(18,4),(45,4),(59,18),(59,45),(45,59),(18,59),(4,45),(4,18)],fill='#141326',outline='#776389',width=2)
d.polygon([(19,7),(44,7),(56,19),(56,44),(44,56),(19,56),(7,44),(7,19)],outline='#413951',width=1)
for pts,c in [([(29,14),(21,12),(14,16),(11,24),(13,29),(10,35),(13,43),(20,48),(28,45),(30,39)],'#302d57'), ([(34,14),(42,12),(49,16),(52,24),(50,29),(53,35),(50,43),(43,48),(35,45),(33,39)],'#45305e')]:d.polygon(pts,fill=c,outline='#100f20',width=2)
left=[(27,16),(21,15),(16,19),(14,24),(16,29),(13,35),(16,41),(21,44),(27,41)]
right=[(36,16),(42,15),(47,19),(49,24),(47,29),(50,35),(47,41),(42,44),(36,41)]
d.line(left,fill='#90f7ed',width=2);d.line(right,fill='#e2a2ff',width=2)
for points in [[(25,20),(20,20),(19,25),(23,28),(21,33),(16,34)],[(27,30),(26,35),(28,38),(24,41)],[(15,25),(17,28)],[(18,38),(21,37)],[(38,20),(43,20),(44,25),(40,28),(42,33),(47,34)],[(36,30),(37,35),(35,38),(39,41)],[(48,25),(46,28)],[(45,38),(42,37)]]:
 d.line(points,fill='#48b6c6' if points[0][0]<32 else '#b86ade',width=2)
# Parallel signal bridge and a casting focus below it.
for yy in [23,33]:
 d.line([(27,yy),(29,yy),(31,yy+2),(33,yy+2),(35,yy),(37,yy)],fill='#eed399',width=1)
d.line([(29,43),(29,48),(26,51),(37,51),(34,48),(34,43)],fill='#cfc4ec',width=2)
d.polygon([(31,42),(35,46),(31,50),(27,46)],fill='#5b476e',outline='#f4dca3')
for x,y,c in [(7,17,'#8de4df'),(54,45,'#d899f8'),(19,7,'#b4a2d7'),(44,55,'#f1d599')]:
 d.rectangle((x,y,x+2,y+2),fill=c)
for x,y in [(31,8),(8,31),(55,31)]: d.line((x-2,y,x+2,y),fill='#e6d5ff'); d.line((x,y-2,x,y+2),fill='#e6d5ff')
p=root/'standalone/tensura-additional-additions/src/main/resources/assets/heavenlyrestriction/textures/skill/split_mind.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
out=root/'wiki/assets/additions/split_mind.png';out.parent.mkdir(parents=True,exist_ok=True);im.save(out)
print('Exported original Split Mind icon with a transparent four-pixel margin.')
