#!/usr/bin/env python3
"""Export original standalone sigils at the native 64px skill-texture resolution."""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
for name in ('lugh','anansi','aegir'):
    source=Image.open(ROOT/'art'/f'{name}-sigil.png').convert('RGBA')
    assert source.width==source.height
    source.resize((64,64),Image.Resampling.NEAREST).save(ROOT/'src/main/resources/assets/wildhunt/textures/skill'/f'{name}.png')
