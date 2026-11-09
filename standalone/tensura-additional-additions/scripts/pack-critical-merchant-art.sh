#!/usr/bin/env bash
set -euo pipefail
merchant_project="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
magick "$merchant_project/art/critical-merchant/source.png" -filter point -resize 64x64! \
  -strip -depth 8 -define png:color-type=6 \
  "$merchant_project/src/main/resources/assets/heavenlyrestriction/textures/skill/critical_merchant.png"
