# Tensura Myths 0.29.1 validation

Validated 23 October 2026 with Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

## Build and gameplay

All **206 required dedicated-server GameTests** passed. **11 JUnit tests** passed with zero failures/errors. This patch changes rendering, authored assets and documentation; it does not change gameplay values, skill targeting, costs, progression, regeneration priority or phase behavior.

## Assets and rendering

All **69 original Blockbench projects** were exported with Blockbench 5.2.2 and GeckoLib Models & Animations 4.2.5. The 67 animated projects retain **140 animation clips**. **976 continuous/faceted surface anchors include 73 smooth ring structures**. The projects contain 11,078 editor cubes. Curved-surface previews are omitted during export, and cached surfaces are drawn on the same animated bones; **361 ordinary cubes** are exported normally. Two ritual items retain vanilla Java item models.

The asset validator checks all 62 active protocol IDs, routing, bone/parent graphs, named renderer contracts, animation references, texture/mask/UV bounds, surface marker dimensions, continuous coverage of the original ring bones, preview/export correspondence, and both item-atlas references. It passes.

Ring cross sections are beveled, with continuous circumferential normals and luminous inset rails. Near/middle/far meshes have 96/64/32 angular segments. Vertex caches share identical geometry between distance levels. Gems and weapon tips have real facets and points; curved leaves/petals/feathers meet at closed edges. Book covers, codices, conductors, pylons and selected armor/accessories use beveled surfaces. Oil Lash retains its continuous tube, with its opaque cuboid shells removed.

Custom surfaces use GeckoLib's existing vertex consumer, texture, mask, light, packed color and overlay. The native pivot transform is restored with GeckoLib's own utility, preserving its reflected X coordinate. Janus's frame and aperture share a center and a 2.8-block height while their width follows the original radius parameter. Neither rendering correction changes traversal checks.

## Native client review

The isolated **Myths Validation** world was used. All **62 active effect types** were staged and captured in the final client; the captures are distinct. Close views checked the continuous gate outline and aperture alignment, Thoth book/page/inscription alignment, and surfaces following animated bones. The gallery shows staged geometry, not measured damage ranges or live combat balance.

Ananke's beveled armor and tapered feathers render with its animated halo. A confirmed damage event visibly flashes **all new surfaces red**. A forced operator damage event triggered the phase announcement, ascent animation and updated ascension formation. This operator staging is visual evidence; regeneration priority and combat lifecycle are covered by the dedicated-server tests.

Alma/Gemma's native bodies and skins remain, with attached beveled journal/tool packs checked from front and back. Both ritual items rendered in hand and in their hotbar slots with valid textures. No Myths model/animation-loading errors appeared. The environment still reports pre-existing base-mod resource warnings and unavailable audio/authentication services.

This is an integrated-client review, not a separate multiplayer-client session, GPU benchmark or shader-pack compatibility test. The standard GeckoLib material/overlay pass is used; no shader pack is required.

## Release

The clean release build passes, excludes the optional visual-preview fixture, and contains 67 runtime geometry assets and the new surface renderer. The wiki's current gallery images and both generated editions have been refreshed.

JAR: `releases/tensura-myths-0.29.1.jar`.

SHA-256: `b864c1c8169224f0bda7ad73e0c10650e952647c31e3f634f7fdc1463787227d`.

Wiki validation resolves **8,269 local references across 151 HTML views**, and all **726 offline archive files** match the current wiki byte for byte. Browser checks pass six responsive home/boss/install views, 72 ability anchors and search, plus all 62 formation images at 1440px and 390px without missing images, overflow or page errors.
