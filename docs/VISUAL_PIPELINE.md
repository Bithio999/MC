# Tensura Myths visual pipeline

The world-visual pipeline uses original Blockbench projects exported through Blockbench 5.2.2 and its GeckoLib Models & Animations 4.2.5 plugin. The runtime is GeckoLib 4.9.3 on Minecraft 1.21.1 / NeoForge. No shader pack is required.

## Asset ownership and export

The artist-facing sources are [art/blockbench](../art/blockbench). There are **83 editable projects**: 72 protocol effect rigs, two elemental-impact material variants, two bosses, Genesis Seal, two NPC accessory rigs and four Java-format ritual items. The 79 animated exports retain 171 animation clips. Their 1,176 curved/faceted surfaces include 73 continuous rings and tapered 280° crescents. Blockbench contains cube previews for these surfaces; runtime exports retain their animation bones and omit the preview cubes. Structural metal, engraved trim, parchment and translucent crystal/glass use original atlas materials with separate luminous masks.

`scripts/export-blockbench.cjs` opens each project in the real editor with the official plugin and invokes its geometry/animation or Java-item codec. It does not emulate the exporter. The initial procedural authoring recipe is available separately; rerunning it overwrites artist edits. The exported models, animations and textures are committed alongside their projects.

## Curved surfaces

`SmoothGeoGeometry` renders smooth annuli and tapered crescents, faceted crystals, curved leaves/petals and beveled solid panels on GeckoLib bones. Ring outlines use 96 / 64 / 32 angular segments by distance, beveled cross sections and luminous inset rails. Cached vertices avoid rebuilding trigonometry each frame. Gems have eight true facets and pointed ends; foliage and feathers taper into continuous tips. Books, codices and selected armor/accessories have chamfered edges. The custom oil tube supplies the lash body without overlapping cuboid tendon shells.

The generator creates `surface_<shape>_<id>_<x>_<y>_<z>_<material>` child bones; dimensions are hundredths of a Blockbench pixel. Their pivot and rotation remain editable in Blockbench. The exporter removes only these bones’ cube previews from runtime geometry. Renderers substitute their cached surfaces inside the normal bone cube draw, using the existing texture consumer, glow mask, packed light, color and overlay. This preserves animation transforms and confirmed red damage feedback without another render pass. Keep the bone-name dimensions synchronized when editing a surface’s proportions. Ritual items use vanilla Java item geometry and do not use these markers.

## Runtime

- `ArtVisualEntity` implements `GeoEntity`, retaining its existing protocol, ownership, interpolation, lifetime and cleanup. `VisualRig` routes all 72 active effect IDs. ID 61 remains retired.
- `MythsArtModel` provides the authored assets and smooth entrance/release. An appearance clip leads into each looping articulated motion. Radial layouts spread individual assemblies without inflating books, trees or mirror facets into giant walls. Field size and damage remain server owned.
- `ArtVisualRenderer` uses `GeoEntityRenderer` with selective glowing inlays and translucent surfaces. Existing custom geometry supplies fluid sheets, filaments, trails, optical effects and exact warning boundaries. Authored structures are omitted beyond 96 blocks for ordinary effects; the procedural effect remains, and boss warnings retain their longer tracking/rendering range. The Particles setting still reduces decorative sprites.
- Ananke has an articulated six-winged rig, separate idle/flight and ritual animation controllers, phase-specific crown vanes, and a triggered ascent on phase revival. Head tracking and cast wing spread supplement keyframes. The former Java `AnankeModel` is retired. Its confirmed HP/SHP hit overlay is implemented on the new renderer, including active Pain Nullification.
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
| Janus | Continuous engraved gate frames, cardinal anchors and opposing verdict weapons | Spiral apertures, spatial cuts and transit streaks |
| Aion | Astrolabe, hourglass, Worldline armature and horizon orrery | Cursor-smoothed channel, clockwork and gravitational optics |
| Thoth | Hinged parchment grimoires, meridians, spellbreak obelisks and mana alembic | Seven-element signatures, continuous conversion streams and orbit trails |
| Lugh | Recessed codices, synthesis tablets, artisan spindles and reconstructed weapons | Scan rings, convergence beams and attack wakes |
| Moira / Ananke | Blade, star-lance, censer, dark-sun and ascension armatures | Exact fan/seal/front/refuge geometry and strike timing |

Genesis Verdict's twenty moving black holes remain a **custom renderer**: velocity-deformed horizons, camera-dependent lens arches and stable world-space disk planes are a better fit for procedural optics than a cube rig. The updated accretion material adds stable warm receding/cool approaching sides while UV flow moves the matter. It keeps all twenty independent trajectories, impacts and damage timings. The authored singularity armature is used by the non-barrage visual path.

Eschaton adds a four-armed articulated sovereign, nine cloth panels, a jointed tail, unfolding crescent mantle and seven floating final-form blades. Its eight Demiurge rigs and two lifecycle effects use shared server/client pattern geometry. Genesis Seals are separate damageable GeoEntities. A 20-chunk tracking channel and expanded bounds preserve arena-wide warnings; the original Aion lens renderer supplies the large world seed with stable accretion planes. The decorative celestial canopy never changes the ground damage boundary.

## Eschaton and Aion: anime visual direction

The 0.31.0 pass gives these two families a hierarchy of large silhouettes, motion accents and close-range detail. Broad blade smears, continuous curled fronts, tapered spatial tears, saturated energy plumes and local impact stars carry the attack. Authored mechanisms and inscriptions provide secondary detail. Eschaton's execution animation now includes torso twist and forearm follow-through; the apocalypse has a sustained ten-second buildup, compression and release.

`SpectacleMesh` separates solid, cel-colored facets from emitted light. Translucent solid faces retain their RGB color and fade through alpha. New white-RGB optical masks use `VfxMaterials.radiant` with native **SRC_ALPHA / ONE** blending. Existing premultiplied optics retain native **ONE / ONE** blending and their original color packing. Using one fade convention for both paths would either dim translucent faces twice or make additive shapes fail to fade. The new stroke mask has no repeated fade along its length, so adjacent curved segments form a continuous edge.

Light plumes use smooth tapered ribbons, while crystal fragments retain faceted solid geometry. The emission halo and local flare reduce opacity close to the camera. The Particles setting reduces optional flares, smoke and motes without removing primary cuts, wave crests, black holes or warnings. World-Eater front strips terminate exactly at the native 22-degree gap boundaries; their solid faces do not intrude into the safe opening. Large curves retain enough segments at arena distance to preserve their silhouette.

Sevenfold Genesis's authored stars follow the same accelerated visual descent as their elemental signature. Null Testament's obelisks occupy the actual three cut centers. The moving wake's ribs follow its first front. The exchange vessel, falling regalia and apocalypse cage occupy their respective channel, landing and seed positions instead of sharing an unscaled root placement.

The gameplay plans, native damage, costs, cooldowns, targeting, twenty singularity trajectories and stable accretion-plane rules are unchanged. Only Eschaton and Aion use this new visual grammar in this release.

## Chained teleport wake

Severance Rush uses one additional procedural effect (ID 73): broken chain half-links, separating fragments, a brief speed seam and a four-block arrival pressure burst. The entity records the actual collision-limited journey; player movement has already completed instantly. This deformation is drawn directly, without adding an authored rig or changing the 83 existing editor projects. The graph validator checks this explicit procedural route alongside the 72 authored effect routes.

## Verification

`python scripts/verify-visual-assets.py` verifies complete protocol coverage, runtime routing, unique bones, parent graphs, UV limits, texture dimensions, luminous masks, animation/bone references and editor/export correspondence, surface marker dimensions and ring coverage. It also checks both Java-item exports. A dedicated server run exercises gameplay and server-side animatable classes without loading client renderers. Client review uses the isolated **Myths Validation** world and an opt-in preview fixture; that fixture is excluded from release JARs.

Keyframes and rendering do not execute combat damage. Existing skill costs, balance, cooldowns, movement, race progression and regeneration/phase rules remain the gameplay authority. Cosmetic illumination is broader than some hit volumes; ability articles and exact warning boundaries remain the reference.
