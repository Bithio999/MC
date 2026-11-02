#!/usr/bin/env python3
"""Original 64px pixel icon: a shark circling a coin and contract."""
from PIL import Image,ImageDraw
from pathlib import Path
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.rectangle((3,3,60,60),fill='#17232d',outline='#d9b768',width=2);d.rectangle((7,7,56,56),outline='#405263')
d.polygon([(12,15),(31,15),(35,19),(35,49),(12,49)],fill='#847047');d.rectangle((14,17,31,46),fill='#e0cc97');d.polygon([(31,15),(35,19),(31,19)],fill='#fff0b9')
for y,w in [(22,11),(26,14),(30,9),(39,12)]:d.rectangle((17,y,17+w,y+1),fill='#806e55')
d.polygon([(9,35),(20,26),(31,23),(35,12),(40,24),(48,27),(52,24),(51,34),(57,42),(47,39),(42,44),(27,44),(17,40)],fill='#293f4e',outline='#7baab3',width=2)
d.polygon([(14,34),(26,29),(42,29),(48,33),(40,38),(23,38)],fill='#638b96');d.polygon([(18,37),(28,34),(42,34),(46,36),(37,41),(25,41)],fill='#cedbd1')
d.polygon([(24,35),(41,34),(43,37),(31,40),(24,38)],fill='#1b1d26')
for x,y in [(27,35),(33,35),(39,34)]:d.polygon([(x,y),(x+3,y),(x+1,y+4)],fill='#fff3ce')
d.rectangle((44,29,46,31),fill='#0b1421');d.point((45,29),fill='#f1d887')
d.ellipse((34,39,54,59),fill='#7b552e',outline='#edc76f',width=2);d.ellipse((38,43,50,55),fill='#d5a347',outline='#ffe5a2',width=1);d.line((44,45,44,53),fill='#79502c',width=2);d.line((41,47,47,47),fill='#79502c',width=1)
p=Path(__file__).resolve().parents[1]/'src/main/resources/assets/heavenlyrestriction/textures/skill/loan_shark.png';im.save(p);print(p)
