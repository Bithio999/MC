# 1.20.2 — Vesper blade connection

Vesper's blade root now follows the diagonal grip centerline through the guard's center jewel. A short silver collar connects the blade to the guard, removing the sideways step that made the previous join look crooked. The existing silver/navy blade, gold star guard, teal jewel, navy grip and gold pommel are retained.

The original source was edited with image generation and repacked through `scripts/pack-vesper-art.sh`. The runtime texture remains **64×64 RGBA**, using nearest sampling and binary alpha. Pixel inspection found **one edge-connected component (419 opaque pixels)** and only alpha values **0 / 255**, so the packed sword has no floating pixel islands or translucent internal seams. The native handheld item model consumes that single connected texture.

The item and engraving registry IDs are preserved. The same texture resource is replaced on existing swords.

## Verification

The opt-in client profile captures the normal and Chromatic Orb tooltips, native combat, and a first-person view of the held sword. The source and packed sprite were inspected visually at native and enlarged scales.

The guarded client run passed with the corrected texture: normal and Orb-upgraded item rendering, native Wind Cutter → sword combat, 105-damage attributes and 30% / 75% Afterwind tooltips. The held sword was also viewed in first person.

The final clean build passed **57 JVM tests**, with no failures, errors or skips. The distribution audit verified the packed resource bytes and excluded review fixtures. Wiki validation passed for **210 articles / 131 ability links**, with **211 GitHub Wiki export pages** and refreshed item screenshots in the offline bundle.
