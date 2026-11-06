#!/usr/bin/env bash
# Pack the authored pixel-art islands into the exact classic Minecraft 64x64 UV layout.
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source_art="$project_dir/art/kael/native-style-source.png"
texture_dir="$project_dir/src/main/resources/assets/heavenlyrestriction/textures/entity"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
magick -size 64x64 canvas:none -colorspace sRGB -type TrueColorAlpha PNG32:"$work_dir/skin.png"
# Technical UV packing only. All visible art comes from the authored source islands.
patch() {
 local crop="$1" size="$2" x="$3" y="$4"
 magick "$source_art" -crop "$crop" +repage -filter point -resize "${size}!" -alpha on \
  -channel A -threshold 50% +channel -background '#263144' -alpha remove -alpha on PNG32:"$work_dir/patch.png"
 magick "$work_dir/skin.png" -colorspace sRGB -type TrueColorAlpha "$work_dir/patch.png" -geometry "+$x+$y" -compose over -composite PNG32:"$work_dir/next.png"
 mv "$work_dir/next.png" "$work_dir/skin.png"
}
# Head: eight-pixel faces and a small outer fringe, matching native otherworlder density.
patch 219x165+175+0 8x8 8 0
patch 60x25+243+318 8x8 16 0
patch 157x180+17+165 8x8 0 8
# Register the face strips so native one-pixel-high eyes survive atlas downsampling.
patch 192x113+173+165 8x5 8 8
patch 192x35+173+282 8x1 8 13
patch 192x21+173+324 8x2 8 14
patch 157x180+394+165 8x8 16 8
patch 179x180+551+165 8x8 24 8
patch 192x44+173+165 8x2 40 8
patch 219x165+175+0 8x8 40 0
patch 157x90+17+165 8x4 32 8
patch 157x90+394+165 8x4 48 8
patch 179x140+551+165 8x6 56 8
# Torso top/bottom, side panels and front/back.
patch 20x20+610+460 8x4 20 16
patch 164x14+400+681 8x4 28 16
patch 51x279+867+416 4x12 16 20
patch 231x279+333+416 8x12 20 20
patch 80x279+1018+416 4x12 28 20
patch 223x279+594+416 8x12 32 20
# Identical garment construction, with separately authored front/side/back cloth.
arm() {
 local x="$1" y="$2"
 patch 20x20+610+460 4x4 "$((x+4))" "$y"
 patch 20x12+975+682 4x4 "$((x+8))" "$y"
 patch 51x279+867+416 4x12 "$x" "$((y+4))"
 patch 100x279+918+416 4x12 "$((x+4))" "$((y+4))"
 patch 80x279+1018+416 4x12 "$((x+8))" "$((y+4))"
 patch 90x217+903+754 4x12 "$((x+12))" "$((y+4))"
}
arm 40 16
arm 32 48
leg() {
 local x="$1" y="$2"
 patch 30x20+440+1040 4x4 "$((x+4))" "$y"
 patch 30x12+430+1238 4x4 "$((x+8))" "$y"
 patch 58x222+340+1032 4x12 "$x" "$((y+4))"
 patch 57x222+400+1032 4x12 "$((x+4))" "$((y+4))"
 patch 52x222+457+1032 4x12 "$((x+8))" "$((y+4))"
 patch 57x222+510+1032 4x12 "$((x+12))" "$((y+4))"
}
leg 0 16
leg 16 48
# Jacket overlay provides Minecraft's usual second-layer depth.
patch 20x20+610+460 8x4 20 32
patch 164x14+400+681 8x4 28 32
patch 51x279+867+416 4x12 16 36
patch 231x279+333+416 8x12 20 36
patch 80x279+1018+416 4x12 28 36
patch 223x279+594+416 8x12 32 36
# Register the small compass separately so its narrow spokes survive 64px sampling.
patch 90x94+661+481 5x5 33 23
patch 90x94+661+481 5x5 33 39
# Remove low-alpha edge remnants from extraction; never spread colors into empty UV space.
magick "$work_dir/skin.png" -colorspace sRGB -type TrueColorAlpha -strip -depth 8 -define png:color-type=6 "$texture_dir/kael_veyr.png"
# Emission is restricted to the existing gold star pixels on the jacket back.
magick "$work_dir/skin.png" -crop 8x8+32+37 +repage \
 -channel A -fx '(r>.55 && g>.45 && b<.72 && r>g*1.02 && g>b*1.1 && a>.5) ? .25 : 0' +channel -channel RGB -evaluate Multiply .18 +channel "$work_dir/star.png"
magick -size 64x64 canvas:none "$work_dir/star.png" -geometry +32+37 -compose over -composite \
 -strip -depth 8 -define png:color-type=6 "$texture_dir/kael_veyr_emissive.png"
printf '%s\n' 'Packed Kael skin and aligned subtle embroidery mask at native 64x64 density.'
