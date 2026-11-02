#!/usr/bin/env python3
"""Original 64px icon: a squared sigil on a violet mathematical field."""
from pathlib import Path
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.rectangle((3,3,60,60),fill='#17142d',outline='#cba961',width=2)
d.rectangle((7,7,56,56),outline='#514269')
for x,y in [(11,15),(14,49),(49,46),(45,11)]:d.rectangle((x,y,x+2,y+2),fill='#8d79aa')
# Stepped, symmetric orbital frame, with room around the central x².
d.line([(12,28),(12,21),(21,12),(38,12)],fill='#896dba',width=2)
d.line([(51,35),(51,43),(42,52),(25,52)],fill='#896dba',width=2)
d.line([(16,26),(36,46)],fill='#25172b',width=10)
d.line([(36,26),(16,46)],fill='#25172b',width=10)
d.line([(17,27),(35,45)],fill='#ad783c',width=7)
d.line([(35,27),(17,45)],fill='#ad783c',width=7)
d.line([(17,26),(35,44)],fill='#f6d58c',width=4)
d.line([(35,26),(17,44)],fill='#f6d58c',width=4)
d.line([(40,17),(47,17),(49,19),(49,22),(40,29),(50,29)],fill='#fff2bd',width=3)
d.line([(40,31),(51,31)],fill='#ad783c',width=1)
d.rectangle((26,35,27,36),fill='#fff8dc')
d.line([(48,39),(48,45)],fill='#e6c980',width=1);d.line([(45,42),(51,42)],fill='#e6c980',width=1)
path=root/'src/main/resources/assets/heavenlyrestriction/textures/skill/exponentiation.png';im.save(path)
print(path)
