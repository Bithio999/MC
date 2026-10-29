#!/usr/bin/env python3
"""Export the image-generated Schema icon into both distributions with a transparent margin."""
from pathlib import Path
import shutil,subprocess
ROOT=Path(__file__).resolve().parents[1]
exporter=shutil.which('magick') or shutil.which('convert')
if not exporter:raise SystemExit('ImageMagick is required for icon export.')
for root,ns in [(ROOT,'wildhunt'),(ROOT/'standalone/heavenly-restriction','heavenlyrestriction')]:
    target=root/f'src/main/resources/assets/{ns}/textures/battlewill/schema.png';target.parent.mkdir(parents=True,exist_ok=True)
    subprocess.run([exporter,str(ROOT/'art/battlewills/schema.png'),'-filter','point','-resize','60x60','-gravity','center','-background','none','-extent','64x64','-channel','A','-threshold','50%','+channel','-strip',str(target)],check=True)
print('Exported both 64-pixel Schema icons with a two-pixel transparent margin.')
