# Tensura Myths visual pipeline

The 0.29.0 world-visual rework uses original Blockbench projects exported through Blockbench 5.2.2 and its GeckoLib Models & Animations 4.2.5 plugin. The runtime is GeckoLib 4.9.3 on Minecraft 1.21.1 / NeoForge. No shader pack is required.

## Asset ownership and export

The artist-facing sources are [art/blockbench](../art/blockbench). There are **69 editable projects**: 62 protocol effect rigs, two elemental-impact material variants, Ananke, two NPC accessory rigs and two Java-format ritual items. The 67 animated exports contain 2,878 cubes and 140 animation clips. Structural metal, engraved trim, parchment and translucent crystal/glass use original atlas materials with separate luminous masks.

`scripts/export-blockbench.cjs` opens each project in the real editor with the official plugin and invokes its geometry/animation or Java-item codec. It does not emulate the exporter. The initial procedural authoring recipe is available separately; rerunning it overwrites artist edits. The exported models, animations and textures are committed alongside their projects.

## Runtime

- `ArtVisualEntity` implements `GeoEntity`, retaining its existing protocol, ownership, interpolation, lifetime and cleanup. `VisualRig` routes all 62 active effect IDs. ID 61 remains retired.
- `MythsArtModel` provides the authored assets and smooth entrance/release. An appearance clip leads into each looping articulated motion. Radial layouts spread individual assemblies without inflating books, trees or mirror facets into giant walls. Field size and damage remain server owned.
- `ArtVisualRenderer` uses `GeoEntityRenderer` with selective glowing inlays and translucent surfaces. Existing custom geometry supplies fluid sheets, filaments, trails, optical effects and exact warning boundaries. Authored structures are omitted beyond 96 blocks for ordinary effects; the procedural effect remains, and boss warnings retain their longer tracking/rendering range. The Particles setting still reduces decorative sprites.
- Ananke has a 77-bone, 363-cube rig, separate idle/flight and ritual animation controllers, phase-specific crown vanes, and a triggered ascent on phase revival. Head tracking and cast wing spread supplement keyframes. The former Java `AnankeModel` is retired. Its confirmed HP/SHP hit overlay is implemented on the new renderer, including active Pain Nullification.
- Alma and Gemma retain Tensura's native body, skin, equipment, subordinate rendering and AI. Their original journal/scroll and smithing-tool packs use GeckoLib accessory rigs. Sitting, swimming, invisibility, sleeping and scale are respected. The existing hair/skin corrections remain.
- The ritual scroll and Dawn Testament use editor-exported Java item models. They remain ordinary stackable native items. Spawn eggs use vanilla rendering.

## Visual families

| Family | Authored structure and motion | Custom rendering retained |
| --- | --- | --- |
| Storm Crown | Engraved conductor coils and ion armatures | Branched lightning, white-hot beam and movement wake |
| Scarlet Deluge | Coral crests and articulated vortex fins | Curled caustic water sheet, foam and spiral flow |
| Black Flame | Ten articulated oil-tendon sections, molten seams and obsidian vents | Continuous hot filaments, oil surface, flame and soot |
| Frostbound Colossus | Layered spires, falling icicles and crystalline armatures | Snowflake inscriptions and strike accents |
| Threadweaver / Anansi | Spindles, articulated eight-legged fate looms and checkmate blades | Braided filaments, travelling pulses and network edges |
| Rune Smith / Vritra | Runic igniters, pylons, plasma capacitors and fault machinery | Lightning, cloud, flame, fracture and impact fields |
| Verdant Keeper / Demeter | Distinct living/crystal groves, corollas, seeds and lotus petals | Root/vine ribbons, restorative ripples and frost signatures |
| Mirrorbound / Aegir | Faceted glass assemblies, harpoons, refraction lenses and coral crests | Refracted rays, caustic basin and moving water fronts |
| Janus | Engraved gate segments, cardinal frames and opposing verdict weapons | Spiral apertures, spatial cuts and transit streaks |
| Aion | Astrolabe, hourglass, Worldline armature and horizon orrery | Cursor-smoothed channel, clockwork and gravitational optics |
| Thoth | Hinged parchment grimoires, meridians, spellbreak obelisks and mana alembic | Seven-element signatures, continuous conversion streams and orbit trails |
| Lugh | Recessed codices, synthesis tablets, artisan spindles and reconstructed weapons | Scan rings, convergence beams and attack wakes |
| Moira / Ananke | Blade, star-lance, censer, dark-sun and ascension armatures | Exact fan/seal/front/refuge geometry and strike timing |

Genesis Verdict's twenty moving black holes remain a **custom renderer**: velocity-deformed horizons, camera-dependent lens arches and stable world-space disk planes are a better fit for procedural optics than a cube rig. The updated accretion material adds stable warm receding/cool approaching sides while UV flow moves the matter. It keeps all twenty independent trajectories, impacts and damage timings. The authored singularity armature is used by the non-barrage visual path.

## Verification

`python scripts/verify-visual-assets.py` verifies complete protocol coverage, runtime routing, unique bones, parent graphs, UV limits, texture dimensions, luminous masks, animation/bone references and editor/export cube correspondence. It also checks both Java-item exports. A dedicated server run exercises gameplay and server-side animatable classes without loading client renderers. Client review uses the isolated **Myths Validation** world and an opt-in preview fixture; that fixture is excluded from release JARs.

Keyframes and rendering do not execute combat damage. Existing skill costs, balance, cooldowns, movement, race progression and regeneration/phase rules remain the gameplay authority. Cosmetic illumination is broader than some hit volumes; ability articles and exact warning boundaries remain the reference.
