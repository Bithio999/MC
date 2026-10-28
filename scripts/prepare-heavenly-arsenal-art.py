#!/usr/bin/env python3
"""Original pixel art: forked cursed blade and fractured seal, mirrored into the standalone."""
from pathlib import Path
import math, struct, zlib
ROOT=Path(__file__).resolve().parents[1]
def canvas(n): return [[(0,0,0,0) for _ in range(n)] for _ in range(n)]
def polygon(im,points,color):
    n=len(im);rgb=tuple(bytes.fromhex(color))+(255,)
    for y in range(n):
        for x in range(n):
            inside=False;j=len(points)-1
            for i,(px,py) in enumerate(points):
                qx,qy=points[j]
                if (py>y+.5)!=(qy>y+.5) and x+.5<(qx-px)*(y+.5-py)/(qy-py)+px:inside=not inside
                j=i
            if inside: im[y][x]=rgb

def png(im,path):
    n=len(im)
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    path.parent.mkdir(parents=True,exist_ok=True)
    raw=b''.join(b'\0'+b''.join(bytes(c) for c in row) for row in im)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',n,n,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b''))

blade=canvas(32)
# Grip, pommel and guard: dark wrapped handle with aged brass furniture.
polygon(blade,[(2,27),(6,31),(17,20),(12,16)],'151c28')
polygon(blade,[(4,27),(6,29),(15,20),(13,18)],'354b55')
for d in range(4):
    x=6+d*2;y=27-d*2
    polygon(blade,[(x-1,y-1),(x,y-2),(x+2,y),(x+1,y+1)],'7e9294')
polygon(blade,[(2,27),(4,25),(8,29),(6,31)],'403b37')
polygon(blade,[(3,27),(4,26),(7,29),(6,30)],'b7a078')
polygon(blade,[(9,17),(11,14),(19,21),(17,24)],'171d27')
polygon(blade,[(10,17),(11,16),(18,22),(17,23)],'aa9572')
polygon(blade,[(12,17),(14,16),(17,19),(16,21)],'d1bd91')
# Asymmetric three-pronged silver head, with open negative space between tines.
polygon(blade,[(13,17),(14,13),(16,11),(15,7),(17,3),(18,10),(20,8),(21,4),(25,1),(25,8),(23,13),(28,10),(30,6),(30,13),(27,17),(20,20),(17,20)],'1c2635')
polygon(blade,[(14,16),(15,13),(18,11),(16,7),(17,5),(19,12),(21,9),(22,5),(24,3),(24,8),(22,13),(21,15),(26,13),(28,12),(29,9),(29,13),(26,16),(20,19),(17,19)],'889baa')
polygon(blade,[(15,16),(18,13),(21,12),(23,7),(23,5),(22,10),(20,14),(17,17)],'dce6e8')
polygon(blade,[(19,17),(22,16),(27,14),(28,12),(27,15),(24,17),(20,18)],'c3d4da')
polygon(blade,[(16,7),(17,6),(18,10),(18,12),(17,11)],'d7e2e5')
polygon(blade,[(15,17),(16,16),(19,18),(18,19)],'4c6575')
# A small cold-blue mark in the blade's dark fuller.
for x,y,c in [(18,15,'bde4e4'),(19,14,'6bafbc'),(19,16,'345e70')]:blade[y][x]=tuple(bytes.fromhex(c))+(255,)

seal=canvas(18)
for y in range(18):
 for x in range(18):
  d=math.hypot(x-8.5,y-8.5)
  if 5.2<d<7.5 and abs((x+y)-17)>2:seal[y][x]=(184,216,225,255) if x<9 else (108,142,163,255)
polygon(seal,[(2,14),(13,1),(16,1),(5,16),(2,17)],'edfaff')
polygon(seal,[(4,14),(14,2),(15,2),(5,15)],'75c3d1')
for base,ns in [(ROOT,'wildhunt'),(ROOT/'standalone/heavenly-restriction','heavenlyrestriction')]:
    png(blade,base/f'src/main/resources/assets/{ns}/textures/item/inverted_spear_of_heaven.png')
    png(seal,base/f'src/main/resources/assets/{ns}/textures/mob_effect/nullification.png')
print('Authored and mirrored spear and Nullification pixel art.')
