#!/usr/bin/env python3
"""Export the reference-matched spear sprite and original seal into both distributions.

Requires ImageMagick for nearest-neighbor export of the imagegen-authored master.
"""
from pathlib import Path
import math, struct, zlib, shutil, subprocess
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

# The user reference calls for two broad, unequal prongs and a long wrapped grip.
# Keep the high-resolution authoring master; this step only exports a Minecraft sprite.
master=ROOT/'art/items/inverted_spear_of_heaven.png'
exporter=shutil.which('magick') or shutil.which('convert')
if exporter is None:raise SystemExit('ImageMagick is required to export the spear sprite.')

seal=canvas(18)
for y in range(18):
 for x in range(18):
  d=math.hypot(x-8.5,y-8.5)
  if 5.2<d<7.5 and abs((x+y)-17)>2:seal[y][x]=(184,216,225,255) if x<9 else (108,142,163,255)
polygon(seal,[(2,14),(13,1),(16,1),(5,16),(2,17)],'edfaff')
polygon(seal,[(4,14),(14,2),(15,2),(5,15)],'75c3d1')
for base,ns in [(ROOT,'wildhunt'),(ROOT/'standalone/heavenly-restriction','heavenlyrestriction')]:
    output=base/f'src/main/resources/assets/{ns}/textures/item/inverted_spear_of_heaven.png'
    output.parent.mkdir(parents=True,exist_ok=True)
    subprocess.run([exporter,str(master),'-filter','point','-resize','64x64','-channel','A','-threshold','50%','+channel','-strip',str(output)],check=True)
    png(seal,base/f'src/main/resources/assets/{ns}/textures/mob_effect/nullification.png')
print('Exported the corrected 64-pixel split blade and mirrored Nullification art.')
