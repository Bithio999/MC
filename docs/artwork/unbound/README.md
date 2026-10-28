# Unbound icon

Original generated pixel-art source: [unbound-source.png](unbound-source.png). Ivory fist, shattered gold shackle and symmetrical broken chains distinguish the Ultimate from Chained’s silver icon.

The production icon uses nearest-neighbor sampling at 56 × 56 inside a transparent 64 × 64 canvas; it is identical in both addon distributions.

```sh
convert docs/artwork/unbound/unbound-source.png -filter point -resize 56x56 -gravity center -background none -extent 64x64 PNG32:src/main/resources/assets/wildhunt/textures/skill/unbound.png
```
