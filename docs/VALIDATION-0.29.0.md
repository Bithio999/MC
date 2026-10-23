# Tensura Myths 0.29.0 validation

Validated 23 October 2026 using Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

## Build and gameplay

The native dedicated-server suite passed **206 required GameTests** with zero required failures. The final clean release build passed **11 JUnit tests** with zero failures/errors. Gameplay tests cover the existing skill economy, aiming, targeting, channel cleanup, ownership, native defenses, Ananke's funded-regeneration priority, fatal HP/SHP revivals, full pool restoration, final rewards and persistence. Final placement/material refinements are confined to client rendering and assets.

The release is built without the optional visual-preview init script. Inspection confirms **67 runtime geometry files**, version **0.29.0**, the item-atlas texture and both item-model references. `VisualPreview`, the old package audit probe, retired recovery class and former Java `AnankeModel` are absent from the JAR.

## Authoring and resources

**69 original projects** were loaded and exported through real Blockbench **5.2.2** and its official GeckoLib Models & Animations **4.2.5** plugin. They include 62 active effect IDs, two impact material variants, Ananke, two NPC equipment rigs and two Java-format ritual items. The animated exports contain **2,878 cubes and 140 clips**.

The asset-graph validator passes complete protocol/routing coverage, unique bone/element identities, acyclic parents, animation references, named renderer bone contracts, UV bounds, texture/mask dimensions, editor/export cube correspondence and both item models. Item textures must exist and participate in Minecraft's block/item atlas; this check prevents the missing-texture issue found and corrected during native review.

## Native client review

Only the isolated **Myths Validation** world was opened. All **62 effect types** were staged, rendered and captured with their procedural optics. Captures use a controlled arena, selected camera angles and Night Vision for visibility; their geometry is not evidence of damage or range. A subset was inspected closer after the complete sweep.

The client review verified hinged Thoth books, field layouts that spread assemblies without enlarging individual books/trees/mirrors, translucent ice/glass, curved segmented Oil Lash, fixed-plane black-hole disks, Lugh constructs, boss warnings and the new Ananke model. Confirmed damage visibly flashes red. An actual phase transition delivered its announcement and triggered the ascending wing/body animation, crown details and ascension formation. Native HP/SHP bars remain visible with the new rig.

Alma/Gemma keep their native skins, held equipment and body renderer; the attached journal/tool packs were checked from the front and back. The object-renderer's block-origin offset was cancelled so they attach to the characters. Both ritual items were checked in the hotbar and first-person hand after their atlas-path correction. No custom-model/animation loading failures were found in the reviewed client logs. Existing base-mod model/recipe warnings and unavailable audio/authentication services in the isolated environment are unrelated to these assets.

This is an integrated-client visual review and a dedicated-server gameplay run, not a separate multiplayer-client session, GPU benchmark, external shader-pack compatibility test or exhaustive NPC armor/pose matrix. No third-party addon assets are redistributed.

## Wiki and release

Both wiki editions and the offline archive were regenerated. Validation resolved **8,269 local references** across **151 HTML views** and matched all **726 archive files** against the wiki sources. Browser checks passed six responsive home/boss/install views, **72 ability anchors**, search and media, plus the new **62-image formation gallery** at 1440px and 390px with no overflow, missing images or page errors.

Release JAR: `releases/tensura-myths-0.29.0.jar`.

SHA-256: `23141f1d714f8e425e625115ea6a94d732a69d516f90015ffedff0624546b233`.

The checksum matches the published artifact. Editable projects, export/validation scripts and the opt-in preview fixture are committed separately from the runtime JAR. Gameplay instructions remain in the wiki; authoring and development history remain in `art/` and `docs/`.
