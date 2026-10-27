#!/usr/bin/env python3
"""Original pixel art for the airborne Battlewill, authored at 32px and exported at 64px."""
from pathlib import Path
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
image=Image.new('RGBA',(32,32),(0,0,0,0))
d=ImageDraw.Draw(image)
d.rounded_rectangle((2,2,29,29),radius=4,fill='#101b28',outline='#334858')
# Two swept, shaded wing silhouettes, with a clear central updraft.
for side in [-1,1]:
    def points(coords):return [(16+side*x,y) for x,y in coords]
    d.polygon(points([(1,17),(5,11),(11,7),(10,13),(8,17),(4,21),(1,21)]),fill='#6b9cac',outline='#304b61')
    d.polygon(points([(2,17),(6,12),(10,10),(8,15),(5,18),(2,20)]),fill='#cee9e9')
    d.line(points([(3,18),(7,15),(9,12)]),fill='#f2ffff',width=1)
    d.line(points([(3,21),(7,19),(10,16)]),fill='#86c6ce',width=1)
d.polygon([(16,6),(12,12),(15,11),(15,22),(17,22),(17,11),(20,12)],fill='#ecd09b')
d.line((16,8,16,19),fill='#fff0c8',width=1)
# Curved wind under the wings; avoid a full spell-circle silhouette.
d.line((8,23,11,25,20,25,24,22),fill='#6b9cac',width=1)
d.line((11,27,20,27,23,25),fill='#d6e9df',width=1)
target=root/'src/main/resources/assets/wildhunt/textures/battlewill/air_sovereignty.png'
target.parent.mkdir(parents=True,exist_ok=True)
image.resize((64,64),Image.Resampling.NEAREST).save(target)
print('Authored the centered Air Sovereignty icon at 64x64.')
