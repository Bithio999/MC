# Vesper sprite

`source.png` is original generated pixel artwork: a silver blade with a navy spine, gold star guard and navy grip. The corrected blade root follows the grip through the center jewel and seats into a solid silver collar. Native Tensura Hihiirokane weapon art was inspected as a style reference; no native or Night Rose texture is redistributed.

Run `scripts/pack-vesper-art.sh` from this addon directory to produce the checked-in 64×64 RGBA item texture. Packing uses nearest sampling and binary alpha for the native handheld item model. Gradle consumes the checked-in texture and does not run image generation or ImageMagick.
