#!/usr/bin/env bash
set -euo pipefail
forge_project="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
magick "$forge_project/art/hephaestus/source.png" -filter point -resize 64x64! \
  -strip -depth 8 -define png:color-type=6 \
  "$forge_project/src/main/resources/assets/heavenlyrestriction/textures/skill/hephaestus.png"
