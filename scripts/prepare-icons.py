#!/usr/bin/env python3
"""Slice the original generated 2x2 portrait sheet into nearest-filtered game icons.
Art preparation only: Pillow is not required to build or run the addon.
"""
from pathlib import Path
import json
from PIL import Image

root = Path(__file__).resolve().parents[1]
source = Image.open(root / 'art/apex-portraits-centered.png').convert('RGBA')
original = Image.open(root / 'art/apex-portraits.png').convert('RGBA')
width, height = source.size
out = root / 'src/main/resources/assets/wildhunt/textures/skill'
for (column, row), ids in zip([(0, 0), (1, 0), (0, 1), (1, 1)], [
    ('storm_crown', 'raijin'), ('scarlet_deluge', 'varuna'),
    ('black_flame', 'agni'), ('frostbound_colossus', 'ymir'),
]):
    # Preserve the existing Nu Udra and Jin Dahaad portraits exactly.
    sheet = original if row == 1 else source
    tile = sheet.crop((column * width // 2, row * height // 2,
                        (column + 1) * width // 2, (row + 1) * height // 2))
    # Normalize the visible portrait, not the original quadrant's uneven gutters.
    # Preserve the artwork and aspect ratio; provide at least four clear pixels.
    bounds = tile.getchannel('A').point(lambda alpha: 255 if alpha > 32 else 0).getbbox()
    if bounds is None:
        raise ValueError('Portrait has no visible pixels: ' + ids[0])
    tile = tile.crop(bounds)
    scale = 56 / max(tile.size)
    tile = tile.resize((max(1, round(tile.width * scale)), max(1, round(tile.height * scale))), Image.Resampling.NEAREST)
    # Nearest sampling can drop a thin outer fringe; center the final visible pixels.
    tile = tile.crop(tile.getchannel('A').point(lambda alpha: 255 if alpha > 32 else 0).getbbox())
    canvas = Image.new('RGBA', (64, 64))
    canvas.alpha_composite(tile, ((64 - tile.width) // 2, (64 - tile.height) // 2))
    tile = canvas
    for name in ids:
        tile.save(out / (name + '.png'))
        (out / (name + '.png.mcmeta')).write_text(json.dumps({'texture': {'blur': False, 'clamp': True}}) + '\n')
    print('Prepared', *ids)
