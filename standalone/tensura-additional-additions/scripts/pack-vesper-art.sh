#!/usr/bin/env bash
set -euo pipefail
vesper_project="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
magick "$vesper_project/art/vesper/source.png" -filter point -resize 64x64! \
  -channel A -threshold 50% +channel -strip -depth 8 -define png:color-type=6 \
  "$vesper_project/src/main/resources/assets/heavenlyrestriction/textures/item/vesper.png"
