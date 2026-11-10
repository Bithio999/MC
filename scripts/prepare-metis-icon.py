#!/usr/bin/env python3
"""Original pixel-art owl, open codex and four-thread constellation for Metis."""
from pathlib import Path
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.polygon([(17,4),(46,4),(59,17),(59,46),(46,59),(17,59),(4,46),(4,17)],fill='#0a182b',outline='#8d704c',width=2)
d.polygon([(18,7),(45,7),(56,18),(56,45),(45,56),(18,56),(7,45),(7,18)],outline='#3d667a')
# The owl's four separate threads converge at a faceted wisdom eye.
for x,y in [(16,15),(26,10),(37,10),(47,15)]:
 d.line([(x,y+2),(x,23),(31,31)],fill='#487d9e',width=1)
 d.polygon([(x,y-2),(x+2,y),(x,y+2),(x-2,y)],fill='#e5e7b8',outline='#70dce6')
d.polygon([(14,20),(23,25),(31,22),(40,25),(49,20),(45,38),(37,45),(31,49),(24,45),(18,38)],fill='#755132',outline='#e4bc70',width=2)
d.polygon([(18,25),(26,29),(31,26),(37,29),(45,25),(42,38),(32,45),(22,38)],fill='#c3924f')
d.polygon([(19,28),(29,29),(29,36),(25,40),(20,36)],fill='#183245',outline='#f6dba1')
d.polygon([(34,29),(44,28),(43,36),(38,40),(34,36)],fill='#183245',outline='#f6dba1')
for x in [25,38]:
 d.rectangle((x-1,31,x+1,35),fill='#8ae5ef');d.point((x,32),fill='#fff8de')
d.polygon([(31,33),(34,39),(31,43),(28,39)],fill='#fff1c3',outline='#9c673b')
# A codex at the base: two separated pages and a bright gold spine.
d.polygon([(12,42),(22,43),(31,47),(41,43),(51,42),(50,52),(40,52),(31,56),(22,52),(13,52)],fill='#102839',outline='#e2bf78')
d.polygon([(15,45),(23,46),(29,49),(29,53),(22,50),(15,49)],fill='#82b8cd')
d.polygon([(34,49),(41,46),(48,45),(48,49),(40,50),(34,53)],fill='#bdd3d9')
d.line((31,47,31,55),fill='#f6df9f',width=2)
for yy in [47,49]:
 d.line((17,yy,24,yy+1),fill='#315f7d');d.line((39,yy+1,46,yy),fill='#467a95')
for x,y in [(8,31),(55,31),(31,7)]:
 d.line((x-2,y,x+2,y),fill='#d9f5f2');d.line((x,y-2,x,y+2),fill='#d9f5f2')
for p in [root/'standalone/tensura-additional-additions/src/main/resources/assets/heavenlyrestriction/textures/skill/metis.png',root/'wiki/assets/additions/metis.png']:
 p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
print('Exported Metis with four-pixel transparent margin.')
