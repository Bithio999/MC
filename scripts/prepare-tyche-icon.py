#!/usr/bin/env python3
"""Original native-sized Tyche coin/die emblem, with safe transparent HUD margins."""
from pathlib import Path
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
p=root/'standalone/tensura-additional-additions/src/main/resources/assets/heavenlyrestriction/textures/skill/tyche.png'
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.ellipse((4,3,27,26),fill='#765526',outline='#f2c76b',width=2)
d.ellipse((7,6,24,23),fill='#173b44',outline='#b78c42',width=1)
for y,x in [(23,4),(20,3),(17,2),(14,3),(11,4),(8,6)]:
 d.polygon([(x,y),(x-1,y-3),(x+2,y-2),(x+3,y+1)],fill='#c7ac58')
 d.polygon([(31-x,y),(32-x,y-3),(29-x,y-2),(28-x,y+1)],fill='#e8cf7b')
d.polygon([(16,8),(23,13),(20,21),(11,20),(9,13)],fill='#f1ead2',outline='#90764a')
for x,y in [(13,13),(19,14),(15,18)]:d.rectangle((x,y,x+1,y+1),fill='#274f5b')
d.line((9,25,15,29,23,25),fill='#eacf80',width=2)
d.rectangle((15,3,16,5),fill='#fff3b9');d.point((24,8),fill='#fff3b9')
p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
p.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":true}}\n')
(root/'wiki/assets/additions/tyche.png').write_bytes(p.read_bytes())
