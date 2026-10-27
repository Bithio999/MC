#!/usr/bin/env python3
"""Original 32-pixel drawings, exported at 64px with nearest-neighbor sampling."""
from pathlib import Path
from PIL import Image, ImageDraw
R=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wildhunt/textures'
INK='#0c101b';EDGE='#202936';SILVER='#9eafb8';LIGHT='#e4edf0';GOLD='#c99b52'
def canvas():
 im=Image.new('RGBA',(32,32),(0,0,0,0));d=ImageDraw.Draw(im);d.rounded_rectangle((2,2,29,29),radius=4,fill=INK,outline=EDGE,width=1);return im,d
def save(im,path):im.resize((64,64),Image.Resampling.NEAREST).save(R/path)
im,d=canvas()
# Fist silhouette and four separately shaded knuckles.
d.polygon([(10,12),(11,8),(14,7),(24,9),(25,14),(22,23),(12,24),(7,19),(7,15)],fill='#514851',outline='#22202e')
for x,y in [(11,9),(14,8),(17,9),(20,10)]:
 d.rectangle((x,y,x+2,y+6),fill='#c2a99a');d.line((x,y,x+1,y),fill='#f1d6b8')
d.polygon([(10,16),(13,14),(17,16),(16,19),(12,19),(13,23),(10,21),(8,18)],fill='#e1bea0')
d.line((15,21,20,21,22,15),fill='#8a7168',width=2)
# Severed shackle around the wrist, rather than a colored spell orb.
d.polygon([(11,23),(20,22),(22,25),(20,27),(11,27),(9,25)],fill=SILVER,outline=EDGE)
d.line((12,24,19,24),fill=LIGHT,width=1)
for x,y in [(4,13),(5,17),(24,17),(25,21)]:
 d.rectangle((x,y,x+3,y+3),outline=SILVER,width=1);d.point((x+1,y),fill=LIGHT)
for xy in [(5,9,8,7),(25,11,28,8),(22,5,23,3)]:d.line(xy,fill=GOLD,width=1)
save(im,Path('skill/chained.png'))
names=['fettered_human','tempered_vessel','unshackled','heavenly_restricted','heavenbreaker']
for stage,name in enumerate(names):
 im,d=canvas();accent=[SILVER,'#aab9bd','#c3b28d','#d6b062','#f2dca0'][stage]
 d.polygon([(6,27),(7,24),(12,21),(12,19),(21,19),(21,22),(26,25),(26,28)],fill='#363a42',outline='#10131c')
 d.polygon([(12,9),(23,10),(23,17),(21,21),(17,23),(12,19),(10,13)],fill='#b68d75')
 d.polygon([(12,10),(20,10),(21,18),(18,21),(14,19),(12,16)],fill='#dfb89a')
 d.polygon([(8,13),(8,8),(12,5),(20,5),(24,9),(23,14),(21,10),(20,13),(18,10),(15,12),(15,9),(11,13)],fill='#171c29')
 d.line((12,7,18,6,22,9),fill='#424852',width=1)
 d.line((13,14,15,14),fill=LIGHT);d.line((19,14,21,14),fill=LIGHT)
 d.point((15,14),fill='#29313b');d.point((19,14),fill='#29313b');d.line((16,19,19,19),fill='#6b5350')
 d.line((21,15,20,18),fill='#8e5551')
 if stage==0:
  d.line((12,17,17,18,21,17),fill='#7d7d7a',width=2);d.point((16,18),fill=LIGHT)
 if stage>=1:
  d.polygon([(6,24),(10,22),(13,24),(11,28),(6,28)],fill=accent,outline=EDGE)
 if stage>=2:d.line((22,13,24,16,23,19),fill=accent,width=1)
 if stage>=3:
  d.line((12,8,16,6,20,7),fill=accent);d.line((15,14,16,14),fill='#f3dfa6')
 if stage==4:
  d.polygon([(12,6),(15,4),(21,5),(23,8),(19,6),(16,8)],fill='#d4d9d4');d.line((19,14,21,14),fill='#f5e3ad');d.line((5,19,3,17),fill=accent);d.line((26,15,28,12),fill=accent)
 # Neck shackle progresses from an intact band to two separated pieces.
 if stage<3:
  d.line((12,23,16,25,21,23),fill=accent,width=2);d.point((16,25),fill=LIGHT)
 else:
  d.line((11,23,14,25),fill=accent,width=2);d.line((20,25,23,23),fill=accent,width=2)
 save(im,Path('race')/(name+'.png'))
print('Authored six original centered pixel icons at 64x64.')
