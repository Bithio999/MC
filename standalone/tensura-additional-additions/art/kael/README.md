# Kael Veyr skin source

Kael uses a classic-arm **64×64** Minecraft skin, matching the pixel density and compact anime faces of Tensura’s native otherworlders. The design retains silver hair, navy cloth, a pale collar, gold trim and a small compass on the coat back.

`native-style-source.png` contains the authored pixel-art islands before UV registration; it is source art, not a runtime skin atlas. Native Hinata and Kyoya skins were inspected as style references and are not redistributed.

Rebuild the checked-in runtime textures with ImageMagick 7:

```sh
bash scripts/pack-wayfarer-skin.sh
```

The packer registers the artwork to the exact classic player UV rectangles, maintains opaque base faces and transparent unused/outer regions, and derives a subtle gold-only embroidery mask from the final base texture. `scripts/create-wayfarer-skin.py` invokes the same packer for compatibility. Normal Gradle builds use the checked-in textures and do not require ImageMagick.
