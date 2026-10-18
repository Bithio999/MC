#!/usr/bin/env python3
"""Slice the original generated 2x2 portrait sheet into nearest-filtered game icons.
Art preparation only: Pillow is not required to build or run the addon.
"""
from pathlib import Path
import json
from PIL import Image

root = Path(__file__).resolve().parents[1]
source = Image.open(root / 'art/apex-portraits.png').convert('RGBA')
width, height = source.size
out = root / 'src/main/resources/assets/wildhunt/textures/skill'
for (column, row), ids in zip([(0, 0), (1, 0), (0, 1), (1, 1)], [
    ('storm_crown', 'raijin'), ('scarlet_deluge', 'varuna'),
    ('black_flame', 'agni'), ('frostbound_colossus', 'ymir'),
]):
    tile = source.crop((column * width // 2, row * height // 2,
                        (column + 1) * width // 2, (row + 1) * height // 2))
    tile = tile.resize((64, 64), Image.Resampling.NEAREST)
    for name in ids:
        tile.save(out / (name + '.png'))
        (out / (name + '.png.mcmeta')).write_text(json.dumps({'texture': {'blur': False, 'clamp': False}}) + '\n')
    print('Prepared', *ids)
