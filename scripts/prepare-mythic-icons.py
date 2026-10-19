#!/usr/bin/env python3
"""Export the eight independent atlas cells into centered, padded game textures.
Optional dependency: Pillow. The original generated atlas remains in art/.
"""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
source=Image.open(ROOT/'art/mythic-sigils.png').convert('RGBA')
assert source.width==source.height*2, 'Expected a 4 by 2 square-cell atlas'
names=['threadweaver','rune_smith','verdant_keeper','mirrorbound','vritra','demeter','janus','thoth']
out=ROOT/'src/main/resources/assets/wildhunt/textures/skill'
for index,name in enumerate(names):
    x,y=index%4,index//4
    # Integer cell boundaries prevent one tile ever sampling a neighboring icon.
    cell=source.crop((source.width*x//4,source.height*y//2,source.width*(x+1)//4,source.height*(y+1)//2))
    cell=cell.resize((56,56),Image.Resampling.NEAREST)
    result=Image.new('RGBA',(64,64),(9,15,31,255));result.alpha_composite(cell,(4,4));result.save(out/f'{name}.png')
print('Exported eight centered 64x64 icons with four-pixel safe margins.')
