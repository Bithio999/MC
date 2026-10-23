# Modeling and animation tools in Tensura and its addons

Inspected 23 October 2026. This is a binary/asset inspection of the specific NeoForge 1.21.1 releases below, not a claim about every version or every contributor's authoring tools.

## Findings

**Blockbench and GeckoLib are an established workflow in this ecosystem.** All three inspected mods contain JSON assets explicitly credited `Made with Blockbench`, GeckoLib geometry/animation resources, and compiled model/renderer classes referencing GeckoLib. Blockbench is the editor; GeckoLib loads and animates exported assets in Minecraft.

| Mod and inspected release | `.geo.json` files | `.animation.json` files | JSON files with explicit Blockbench credit |
| --- | ---: | ---: | ---: |
| Tensura: Reincarnated 2.0.1.3, file 8793360 | 123 | 86 | 37 |
| Nightmares 1.0.5.2.2.5, file 9120824 | 62 | 39 | 62 |
| Tensura Mysticism 2.2.1-neoforge | 22 | 15 | 3 |

Counts cover all resource categories, including armor, items, mobs and effect entities; they are not counts of animated bosses. Every counted geometry and animation file parsed as JSON. No `.bbmodel` editor projects were present in these JARs. Explicit Blockbench credits occur on block/item exports; their presence establishes use of the editor within each project, but does not prove that every geometry asset was authored in it. The geometry format is compatible with the Blockbench/GeckoLib workflow. Blender usage was not established by this inspection.

## Direct implementation evidence

### Tensura: Reincarnated

- `assets/tensura/models/block/charybdis_core.json`, `magic_engine.json` and `smithing_bench.json` contain `"credit": "Made with Blockbench"`.
- `assets/tensura/geo/entity/ifrit.geo.json` and `charybdis.geo.json` contain named bones and cube geometry. The magic circle has `geo/entity/misc/magic_circle.geo.json`, including separate circle bones.
- `TensuraEntityGeoModel` extends GeckoLib's `DefaultedEntityGeoModel`. `IfritRenderer`, `CharybdisRenderer` and `MagicCircleRenderer` extend `GeoEntityRenderer`.
- Its NeoForge metadata declares GeckoLib with version range `[4.7.5,)`.

### Nightmares

- Item exports such as `assets/trnightmare/God exports/avalon.json` and `excalibur.json` contain explicit Blockbench credits.
- `BeelzebuthBlackHoleModel` extends `GeoModel` and references `geo/beelzebuth_black_hole.geo.json` and its corresponding animation JSON. `GateOfBabylonGeoModel` also uses GeckoLib.
- More complex effects have a different rendering path: `VoidBlackHoleLensRenderer` extends Minecraft's `EntityRenderer` and works with a custom post-effect registry, render targets and `PostChain`. The JAR includes fragment/vertex shader resources under `assets/trnightmare/shaders/`.

This is evidence for combining animated assets with custom graphics rather than treating a modeling editor as the entire effects system.

### Tensura Mysticism

- `assets/mysticism/models/item/axiom.json`, `ritual_scythe.json` and `waltz.json` contain explicit Blockbench credits.
- `DraconicPulseModel` extends GeckoLib's `GeoModel` and references `geo/draconic_pulse.geo.json` and `animations/draconic_pulse.animation.json`. `BlueFlareModel` and `BubbleShieldBarrierEntityModel` also use GeckoLib.
- Its `BlackHoleModel` and `BlackHoleRenderer` use a separate Java model/renderer path. GeckoLib is not used for every visual.

## Recommended Tensura Myths workflow

1. **Blockbench for original authored models, textures and animation clips.** Store editable `.bbmodel` projects in Git alongside exported geometry, textures and animations. Use it for Ananke's rig, articulated wings, armor, windups, attack poses and phase transitions, and for modeled skill constructs.
2. **GeckoLib for runtime playback.** Drive clips from synchronized combat state and blend locomotion, casting and recovery. Keep damage timing and resource use authoritative on the server. Carry over Ananke's confirmed-hit overlay when changing renderers.
3. **Custom rendering for effects that need it.** Retain or improve procedural trails, beams, expanding rings, black-hole disks and impact meshes. Add shaders where they provide useful distortion or other material effects. GeckoLib models can be part of these effects without replacing every procedural element.
4. **Judge quality in motion and under load.** Authoring software enables iteration, but silhouette, textures, easing, animation overlap, transparent-layer ordering and performance still determine the result. Test the twenty-hole Aion barrage and multiple bosses, as well as isolated assets.

The repository already has GeckoLib on its compile/runtime classpaths (`libs/runtime/geckolib-neoforge-1.21.1-4.9.3.jar`). Ananke currently uses a Java `EntityModel`/`ModelPart` rig; our skill effects are largely procedural renderers. Adopting the workflow therefore requires original authored assets and renderer/controller integration, rather than simply installing another dependency. This research does not change gameplay or migrate the renderer.

## Sources and reproducibility

- [Tensura: Reincarnated inspected file](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated/files/8793360). SHA-256: `914043ed6d84877a1565daa6164ea489426acb518d797b954386523c1d85a676`.
- [Nightmares inspected file](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated-nightmares/files/9120824). SHA-256: `892d77a3169a45c14c41efc277cae8204d2b0786fec6f5416b9ad7bcaa35dd04`.
- [Mysticism project](https://modrinth.com/mod/tensura-mysticism), [inspected release binary](https://cdn.modrinth.com/data/bjvFzLV3/versions/q4YA4Wke/mysticism-neoforge-2.2.1.jar). SHA-256: `61e98fe4666cb2baa35396dca7d4e1bb7e41069f2ddfc4af9799b2bb5eb442d3`.
- [Blockbench](https://www.blockbench.net/) and [GeckoLib documentation](https://github.com/bernie-g/geckolib/wiki).

Counts came from ZIP entry suffixes; Blockbench evidence came from literal credits in JSON; runtime evidence came from compiled class references and generic superclass signatures. These checks were performed on the existing pinned dependency and previously downloaded addon JARs. Third-party binaries, models, textures and animations are not added to the repository.
