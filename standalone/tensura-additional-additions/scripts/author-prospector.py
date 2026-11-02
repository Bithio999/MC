#!/usr/bin/env python3
"""Original native-size pixel icon: a prospector's pick and a faceted violet ore crystal."""
from PIL import Image,ImageDraw
from pathlib import Path
A=Path(__file__).resolve().parents[1]/'src/main/resources/assets/heavenlyrestriction/textures/skill'
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.rectangle((3,3,60,60),fill='#10242c',outline='#d9bf73',width=2);d.rectangle((7,7,56,56),outline='#344d56')
for x,y in [(12,15),(19,11),(49,18),(52,43),(14,47)]:d.rectangle((x,y,x+2,y+2),fill='#6b838b')
d.polygon([(9,47),(26,25),(32,30),(16,54),(11,53)],fill='#453321');d.line([(13,49),(29,28)],fill='#c08a4e',width=4);d.line([(13,50),(28,31)],fill='#edbf77',width=1)
d.polygon([(15,22),(22,15),(32,15),(43,23),(45,30),(34,23),(27,22),(21,27)],fill='#26383e',outline='#e7d49c')
d.polygon([(17,22),(23,17),(31,17),(41,24),(34,21),(27,20),(22,24)],fill='#d5dcda');d.line([(24,18),(31,18),(37,22)],fill='#f5edc7',width=2)
d.polygon([(31,35),(41,28),(53,34),(55,45),(45,55),(33,48)],fill='#36305c',outline='#bba3ed',width=2)
d.polygon([(41,30),(50,35),(43,40),(35,36)],fill='#e0c6fa');d.polygon([(35,37),(43,41),(44,52),(35,47)],fill='#9071bc');d.polygon([(44,41),(52,36),(53,44),(46,51)],fill='#b08cd9');d.line([(43,41),(45,52)],fill='#e1c6fa',width=1)
d.line([(50,13),(50,21)],fill='#e2cc89',width=1);d.line([(46,17),(54,17)],fill='#e2cc89',width=1)
im.save(A/'prospector.png')
print('Authored 64×64 Prospector icon')
