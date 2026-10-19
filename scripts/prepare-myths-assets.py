#!/usr/bin/env python3
"""Draw original pixel sigils for Myths. Optional dependency: Pillow. No source art is edited."""
from pathlib import Path
import json,math
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/wildhunt/textures'
PALETTES=[0xD9A663,0x91CEE8,0xD55863,0x82B55B,0xC4A88C,0x8E9270,0x69B899,0x63C8C9,0xB0C5FF,0xE6CD9F,0xC78B56,0x79C9F5,0xBD6AA7,0xC7CED7,0xA281C3]
FAMILIES=['beastfolk','harpy','ogre','goblin','giant','orc','lizardman','merfolk','elf','human','dwarf','slime','vampire','wight','daemon']
def rgb(c):return ((c>>16)&255,(c>>8)&255,c&255,255)
def glyph(d,x,y,index,color):
    # All fifteen lineages have a different small heraldic rune.
    points=[]
    for n in range(5):
        a=(n*72-90)*math.pi/180;r=8 if n%2==index%2 else 6
        points.append((x+round(math.cos(a)*r),y+round(math.sin(a)*r)))
    if index in [0,2,5,6,14]:
        d.line([(x-8,y-5),(x-5,y+5),(x,y-2),(x+5,y+5),(x+8,y-5)],fill=color,width=2)
        d.polygon([(x-4,y+3),(x,y+8),(x+4,y+3)],fill=color)
    elif index in [1,7,8]:
        d.line([(x-9,y+2),(x-3,y-6),(x,y),(x+3,y-6),(x+9,y+2)],fill=color,width=2)
        d.line((x,y-4,x,y+8),fill=color,width=2)
    elif index in [3,4,9,10]:
        d.polygon(points,outline=color,width=2)
        d.line((x-5,y+7,x+5,y-7),fill=color,width=2)
    else:
        d.polygon([(x,y-9),(x-7,y+2),(x-4,y+7),(x+4,y+7),(x+7,y+2)],outline=color,width=2)
        d.rectangle((x-1,y+1,x+1,y+4),fill=color)
    d.point((x-8+(index%4),y+9-(index//4)),fill=(255,255,255,255))
for p in json.loads((ROOT/'src/main/resources/assets/wildhunt/myths/fusions.json').read_text()):
    a=FAMILIES.index(p['first']);b=FAMILIES.index(p['second']);image=Image.new('RGBA',(64,64));d=ImageDraw.Draw(image)
    d.polygon([(32,2),(57,15),(57,46),(32,61),(6,46),(6,15)],fill=(16,21,36,255),outline=(228,204,148,255),width=2)
    d.line([(8,16),(32,5),(55,16)],fill=rgb(PALETTES[a]),width=3)
    d.line([(8,45),(32,58),(55,45)],fill=rgb(PALETTES[b]),width=3)
    glyph(d,22,31,a,rgb(PALETTES[a]));glyph(d,43,31,b,rgb(PALETTES[b]))
    d.polygon([(32,19),(35,23),(32,27),(29,23)],fill=(245,230,181,255))
    key='_'.join(sorted([p['first'],p['second']]));out=OUT/'race/convergence'/f'{key}.png';out.parent.mkdir(parents=True,exist_ok=True);image.save(out)
image=Image.new('RGBA',(64,64));d=ImageDraw.Draw(image)
d.ellipse((5,5,58,58),fill=(18,18,40,255),outline=(122,108,192,255),width=2)
d.ellipse((9,9,54,54),outline=(236,201,120,255),width=2)
for n in range(12):
    a=n*math.pi/6
    d.line([(32+int(math.sin(a)*22),32+int(math.cos(a)*22)),(32+int(math.sin(a)*25),32+int(math.cos(a)*25))],fill=(249,228,172,255),width=2)
d.polygon([(20,13),(44,13),(41,24),(34,32),(41,40),(44,51),(20,51),(23,40),(30,32),(23,24)],fill=(166,145,238,255),outline=(254,230,158,255),width=2)
d.polygon([(24,19),(40,19),(32,29)],fill=(247,214,121,255));d.polygon([(32,37),(40,47),(24,47)],fill=(247,214,121,255))
d.line((18,12,46,12),fill=(255,239,186,255),width=3);d.line((18,52,46,52),fill=(255,239,186,255),width=3)
d.line((31,30,33,35),fill=(255,255,255,255),width=1)
for x,y in [(4,29),(59,32),(31,1),(33,61)]:d.polygon([(x,y-2),(x+2,y),(x,y+2),(x-2,y)],fill=(230,219,255,255))
out=OUT/'skill/aion.png';out.parent.mkdir(parents=True,exist_ok=True);image.save(out)
print('Prepared 105 original fusion sigils and Aion icon.')
