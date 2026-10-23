# Eschaton / Aion visual direction

The selected direction is **anime spectacle: bold shapes, dramatic motion and sharp bursts**. This pass establishes a reviewable standard on Eschaton and Aion before extending it to other skill families.

## Findings

The 0.30.0 arena captures relied on fine lines and small models over large attack footprints. Their detail was visible close up, but contributed little to the silhouette from fighting distance. Some authored rigs also stayed near the casting origin despite much larger or displaced server geometry. Larger counts of cubes, glyphs or particles would not fix that hierarchy.

The renderer also needs two distinct color conventions. Inspection of the pinned Minecraft 1.21.1 shader and render-state sources confirmed:

- `rendertype_eyes.fsh` multiplies the texture and vertex color without an alpha discard.
- Native `ADDITIVE_TRANSPARENCY` uses **ONE / ONE**; legacy optics correctly encode their light intensity in RGB.
- Native `LIGHTNING_TRANSPARENCY` uses **SRC_ALPHA / ONE**; the new white-RGB masks can put falloff in alpha while retaining saturated RGB.
- Native translucent emissive faces use alpha blending. Multiplying both their vertex RGB and alpha by the fade unnecessarily squares the apparent intensity.

The solution preserves legacy additive behavior and adds a separate straight-alpha emission path for these two families. A repeated longitudinal fade also caused beaded edges on segmented strokes; the new strip mask varies only across its width.

## Visual language

| Family | Main silhouette | Motion and contact |
| --- | --- | --- |
| Aion | Gold/violet negative-space seams, folded time markers and dark gravitational optics | Moving time fragments, inward-folding petals, stable black-hole disks, compressed flight trails and distinct collision bursts |
| Eschaton | Pearl/cyan execution blades, dark curled fronts, monumental scripture and a world-seed canopy | Exaggerated torso/forearm windup, accelerated elemental descent, broad cleave smears, finite local impact frames and an extended apocalypse release |

Solid fragments and light are separate shapes. Faceted crystals provide hard debris; curved light plumes provide energy, avoiding large opaque pyramids as a substitute for a beam. Dark backing and saturated rims provide contrast without relying on a shader pack or a persistent cloud of particles.

Warnings remain authoritative: fan bounds, ray widths, locked star positions, landing centers and the apocalypse sphere retain their server rules. Golden refuges remain visible. Curled front faces stop exactly at the real safe-gap angles. Flare opacity falls near the camera, and Minimal particles retains the primary attack geometry.

## Review method

The actual native Minecraft client is used at both arena distance and player view. Eschaton is initialized with its native package and made NoAI for repeatable framing. Aion's four modes are activated through real stored player skills, including held input and an actual automatic checkpoint return. The opt-in [fixture](fixtures/visuals-0.31.0/README.md) is excluded from release builds.

Captures are staged visual references, not damage, duration or hardware-performance measurements. The release report records the final gameplay regression, asset and documentation results. The direction remains open to the user's visual feedback before it is applied elsewhere.
