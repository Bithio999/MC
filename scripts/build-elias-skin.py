#!/usr/bin/env python3
"""Build Elias on Kael's established 64px otherworlder UV/pixel template.

Kael's native-sized pixel boundaries, face placement and shading density are
retained. The archivist has a warmer silver hair palette, a short beard,
teal tailoring and an ivory waistcoat. No high-resolution/generated atlas is used.
"""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
TEXTURES=ROOT/'standalone/tensura-additional-additions/src/main/resources/assets/heavenlyrestriction/textures/entity'
source=Image.open(TEXTURES/'kael_veyr.png').convert('RGBA')
assert source.size==(64,64)
skin=source.copy()
# Preserve the original shading shapes; shift material colours coherently.
for y in range(64):
 for x in range(64):
  r,g,b,a=source.getpixel((x,y))
  if not a:continue
  if y<16:
   if max(r,g,b)-min(r,g,b)<24 and r>90:
    v=(r+g+b)//3
    color=(min(228,v+13),min(228,v+14),min(222,v+7),a)
   else:color=(r,g,b,a)
  else:
   is_skin=r>g+20 and g>b+8 and r>140
   is_gold=r>90 and g>75 and r>b+24 and abs(r-g)<42
   if is_skin or is_gold:color=(r,g,b,a)
   elif max(r,g,b)-min(r,g,b)<23 and max(r,g,b)>80:
    # Kael's metal shoulder shapes become cloth rather than shiny gold armour.
    v=(r+g+b)//3;color=(int(v*.33),int(v*.51),int(v*.54),a)
   elif b>r and b>=g:color=(int(r*.83),min(110,int(g*1.2)),min(105,int(b*1.05)),a)
   else:color=(r,g,b,a)
  skin.putpixel((x,y),color)
# The same sparse eye/face vocabulary as Kael, with a less obstructive fringe.
face=[
 ['#a9adaa','#ced0c9','#a6aca9','#a9afac','#aab0ad','#b2b7b0','#b8bdb6','#929b9b'],
 ['#c3c8c1','#d2d3ca','#bbc1ba','#bec4bd','#c5c9c1','#bdc3bc','#a7afaa','#929b9b'],
 ['#b9c0b9','#c9cec5','#b7beb6','#a0aaa5','#a0aaa5','#bec5bd','#a5afa9','#879294'],
 ['#9fa9a4','#edc7ad','#edc7ac','#edc6ab','#eac3aa','#edc6ab','#eac3aa','#909b97'],
 ['#9ba7a1','#ebc6ad','#b7bdb4','#edc7ad','#eac5ac','#b7bdb4','#e8c3a9','#89958f'],
 ['#edc7ac','#edc6ac','#4a646b','#eac4aa','#e7bea5','#48646d','#edc6ab','#e5bda4'],
 ['#e6bfa7','#a3ada6','#ebc5aa','#d3a98f','#d1aa90','#ebc5aa','#a3ada6','#e4bca3'],
 ['#dfb79f','#e2bba2','#aab4ac','#c1c7bb','#c1c7bb','#aab4ac','#dfb79e','#dfb59c'],
]
for y,row in enumerate(face):
 for x,c in enumerate(row):skin.putpixel((8+x,8+y),tuple(bytes.fromhex(c[1:]))+(255,))
# Keep the hat's front transparent so it cannot conceal the eyes or become a blank mask.
for y in range(8,16):
 for x in range(40,48):skin.putpixel((x,y),(0,0,0,0))
# Two-pixel waistcoat and the same compact gold accents as Kael's outfit.
for y in range(20,30):
 for x in [23,24]:skin.putpixel((x,y),(208,200,177,255) if x==23 else (187,180,160,255))
for y in [24,27]:skin.putpixel((24,y),(115,98,69,255))
for x in [22,25]:skin.putpixel((x,23),(183,159,111,255))
for x in [23,24]:skin.putpixel((x,30),(92,71,52,255))
# Clear only the centre of the jacket overlay to leave the waistcoat readable.
for y in range(36,48):
 for x in [23,24]:skin.putpixel((x,y),(0,0,0,0))
# An understated open-book mark replaces the warrior's back heraldry.
for y in range(23,29):
 for x in range(33,39):skin.putpixel((x,y),(30,51,59,255))
for x,y in [(33,24),(33,25),(33,26),(38,24),(38,25),(38,26),(34,27),(35,28),(36,28),(37,27)]:skin.putpixel((x,y),(175,153,106,255))
for y in [24,25,26]:
 for x in [34,35,36,37]:skin.putpixel((x,y),(191,188,164,255) if x in [34,37] else (142,151,139,255))
for y in range(36,48):
 for x in range(32,40):skin.putpixel((x,y),(0,0,0,0))
# All base faces must be opaque; unused UV islands and selected outer layers stay clear.
def faces(x,y,w,h,d):
 return [(x+d,y,w,d),(x+d+w,y,w,d),(x,y+d,d,h),(x+d,y+d,w,h),(x+d+w,y+d,d,h),(x+2*d+w,y+d,w,h)]
for uv in [(0,0,8,8,8),(16,16,8,12,4),(40,16,4,12,4),(0,16,4,12,4),(32,48,4,12,4),(16,48,4,12,4)]:
 for x,y,w,h in faces(*uv):
  for xx in range(x,x+w):
   for yy in range(y,y+h):
    assert skin.getpixel((xx,yy))[3]==255,(xx,yy,'transparent base face')
skin.save(TEXTURES/'elias.png')
(TEXTURES/'elias.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":true}}\n')
print('Elias: Kael-style classic 64x64 skin, native UVs, opaque base faces.')
