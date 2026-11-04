#!/usr/bin/env python3
"""Author the original 128px Kael skin on the standard 64px player UV layout; no external art."""
from pathlib import Path
from PIL import Image, ImageDraw
S=2
img=Image.new('RGBA',(128,128));draw=ImageDraw.Draw(img)
emissive=Image.new('RGBA',(128,128));glow=ImageDraw.Draw(emissive)
def rect(x,y,w,h,color): draw.rectangle((round(x*S),round(y*S),round((x+w)*S)-1,round((y+h)*S)-1),fill=color)
def line(points,color,width=.5): draw.line([(round(x*S),round(y*S)) for x,y in points],fill=color,width=max(1,round(width*S)))
NAVY='#1c293b';COAT='#22354a';EDGE='#101a2b';TEAL='#71cdd4';GOLD='#d8bf86';SILVER='#dce7e8';SKIN='#ceb1a1'
def panel(x,y,w,h,color,shade=True):
 rect(x,y,w,h,color)
 if shade:
  rect(x,y,.5,h,'#152236');rect(x+w-.5,y,.5,h,'#2c4255')
# Head base: complete hair coverage above the forehead, including top/back and temples.
for x,y,w,h in [(8,0,8,8),(16,0,8,8),(0,8,8,8),(8,8,8,8),(16,8,8,8),(24,8,8,8)]:rect(x,y,w,h,SKIN)
for x,y,w,h in [(8,0,8,8),(24,8,8,8),(0,8,8,6),(16,8,8,6)]:
 rect(x,y,w,h,'#adbfc6')
 for i in range(int(w*2)):
  xx=x+i*.5
  line([(xx,y+.5),(xx+.3,y+min(h-1,2+(i*7)%9/2))],['#e1e9e8','#bdcdd1','#d1dcdf','#91aab6'][i%4])
# Face with asymmetric swept fringe and clearly visible forehead/eyes.
rect(8,8,8,2,'#b8cbd2');rect(8,9,1.5,3,'#b8cbd2');rect(14.5,9,1.5,2.5,'#b8cbd2')
line([(8,8.5),(12.5,8.5),(10.5,10.5)],SILVER);line([(15.5,8.5),(13,8.5),(13,10)],SILVER)
rect(10,11,1.5,.5,'#697e89');rect(13,11,1.5,.5,'#697e89')
for eye in [10,13]:
 rect(eye,11.5,1.5,1,'#edf4ed');rect(eye+.5,11.5,.5,1,TEAL);rect(eye+.5,12,.5,.5,'#35636e')
rect(11.5,12.5,1,1,'#b58e80');rect(11,14,2,.5,'#977667');rect(9,14.5,5,1,'#c2a395')
rect(8,15.5,8,.5,'#495463')
# Transparent outer hair: shaped locks, not a solid helmet or bald forehead.
for x,y,w,h in [(40,0,8,8),(56,8,8,6.5),(32,8,8,5.5),(48,8,8,5)]:
 rect(x,y,w,h,'#b8cbd1')
 for i in range(int(w*2)):
  xx=x+i*.5;line([(xx,y+.5),(xx+.5,y+h-1)],['#dce7e8','#c3d2d8','#9bb3be'][i%3])
for x,y,w,h in [(40,8,8,1.5),(40,9.5,1,2),(46.5,9,1.5,2),(41,9,2,.5),(44,9,1.5,1)]:rect(x,y,w,h,'#d1dfe1')
# Body and sleeves: layered navy coat, white collar, gold piping and teal cuffs.
for x,y,w,h in [(20,16,8,4),(28,16,8,4),(16,20,4,12),(20,20,8,12),(28,20,4,12),(32,20,8,12)]:panel(x,y,w,h,COAT)
rect(20,20,8,1,'#536575');rect(22,21,4,7,EDGE)
line([(21,20),(23,22),(22,25)],SILVER);line([(27,20),(25,22),(26,25)],SILVER)
line([(20.5,22),(21.5,27),(21.5,30)],GOLD);line([(27.5,22),(26.5,27),(26.5,30)],GOLD)
rect(20,29,8,1,EDGE);rect(23,29,2,1,GOLD);rect(23.5,29,.8,.5,TEAL)
for y in [25.5,27]:rect(23.5,y,.5,.5,GOLD)
# Back compass embroidery.
line([(36,22),(38,25),(36,28),(34,25),(36,22)],GOLD)
line([(36,23),(36,27)],TEAL);line([(34.5,25),(37.5,25)],TEAL)
for x,y in [(44,20),(36,52)]:
 for dx,dy,w,h in [(-4,0,4,12),(0,0,4,12),(4,0,4,12),(8,0,4,12)]:panel(x+dx,y+dy,w,h,NAVY)
 rect(x,y+1,4,.5,GOLD);rect(x+.5,y+2,3,.5,TEAL);rect(x,y+7,4,2.5,'#344b5d');rect(x,y+9,4,.5,TEAL);rect(x,y+9.5,4,2.5,'#151c28')
 line([(x+1,y+8),(x+3,y+8)],GOLD)
for x,y in [(44,16),(48,16),(36,48),(40,48)]:rect(x,y,4,4,COAT)
# Trousers and boots, base and outer layer.
for bx,by in [(0,16),(16,48)]:
 for xx in [bx,bx+4,bx+8,bx+12]:
  panel(xx,by+4,4,12,'#293448');rect(xx,by+8.5,4,.5,'#516073');rect(xx,by+11,4,5,'#111a28');rect(xx,by+11,4,.5,GOLD)
  rect(xx+.5,by+15,3,.5,'#6a7b88')
 rect(bx+4,by,8,4,EDGE)
# Outer coat: seam highlights, shoulder cape and scarf. Empty areas remain transparent.
for x,y,w,h in [(20,36,8,12),(32,36,8,12),(16,36,4,12),(28,36,4,12)]:
 panel(x,y,w,h,COAT);rect(x,y+h-1,w,.5,GOLD)
rect(22,37,4,8,EDGE);line([(20.5,36.5),(22,40),(21,45)],GOLD);line([(27.5,36.5),(26,40),(27,45)],GOLD)
rect(20,36,8,1,'#5e6f82');rect(20,37,2,1,'#7e91a0');rect(26,37,2,1,'#7e91a0')
rect(23,41.5,2,2,TEAL);rect(23.5,42,1,1,'#e3ffff')
line([(36,38),(38,41),(36,44),(34,41),(36,38)],GOLD);line([(36,39),(36,43)],TEAL);line([(34.5,41),(37.5,41)],TEAL)
for x,y in [(44,36),(52,52)]:
 panel(x,y,4,12,COAT);rect(x,y+1,4,.5,GOLD);rect(x,y+9,4,.5,TEAL)
# Glowing thread and pupils only; never an opaque secondary skin.
for box in [(23,41.5,2,2),(35.5,39,.5,4),(34.5,41,3,.5),(44,45,4,.5),(52,61,4,.5),(10.5,11.5,.5,.5),(13.5,11.5,.5,.5)]:
 x,y,w,h=box;glow.rectangle((int(x*S),int(y*S),int((x+w)*S)-1,int((y+h)*S)-1),fill='#8ae9ec')
path=Path(__file__).resolve().parents[1]/'src/main/resources/assets/heavenlyrestriction/textures/entity';path.mkdir(parents=True,exist_ok=True)
img.save(path/'kael_veyr.png');emissive.save(path/'kael_veyr_emissive.png')
print('Authored original Kael skin and emissive thread at 128×128.')
