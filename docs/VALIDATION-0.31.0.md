# 0.31.0 validation — Eschaton and Aion visual review

## Build and gameplay regression

Validated with Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle -I docs/research/fixtures/visuals-0.31.0/preview.init.gradle compileJava --console=plain
gradle clean build runGameTestServer --console=plain
python scripts/verify-visual-assets.py
```

**BUILD SUCCESSFUL. All 230 required native GameTests and all 11 JUnit tests passed.** The normal build follows a clean, so preview compilation cannot leak fixture classes into the release.

The existing native suite covers skill payments, cooldowns, channel behavior, targeting, twenty independent homing singularity hits, checkpoint resources/automatic return, shared Godly traits, progression and native UI integrations. Its 24 Eschaton checks cover actual billion EP, resources, regeneration-first fatal forms, all eight paid arts, exact damage patterns, seals/refuges/interruption, persistence, rewards, arena protection and live stored-skill AI activation. No gameplay values or test assertions were changed for this visual pass.

The packaged JAR contains `SpectacleMesh`, all four new optical masks and the updated Eschaton animation. It contains none of `OriginReview.class`, `SpectacleReview.class` or `VisualPreview.class`. Its mod metadata is 0.31.0.

## Authored assets and native review

The real Blockbench/official GeckoLib plugin export completed for **83 editable projects**. The graph validator passed **72 active effect IDs**, **79 animated editor/export pairs**, **171 animation clips**, **13,737 editor preview cubes**, **1,176 continuous/faceted surfaces**, **73 smooth rings**, and all **four Java item exports**. Each new mask is a 256×256 RGBA PNG with white RGB and opacity in alpha. Legacy ONE/ONE optics retain their existing convention; the new light masks use SRC_ALPHA/ONE.

The isolated **Myths Validation** world was used for Eschaton's actual stored form-III casts and real player Aion activations, including held Worldline Sever, cursor-placed Event Horizon, the checkpoint's native 20-second automatic return and twenty homing singularities. Review covered arena-distance silhouettes, first-person geometry, stable disks, tapered cuts, curled fronts, golden openings and the apocalypse release. Eleven individual formation photographs were refreshed alongside the featured shots. Other families' existing previews were retained.

The [native motion reel](../wiki/media/eschaton-aion-anime-preview.mp4) is **1280×720 at 30 fps, 47.87 seconds**. It cuts between casts and explicitly labels the skipped waiting interval before Causal Reversal returns. Two animated WebP previews accompany the articles. No shader pack or added post-production glow is used. [Comparison and review build](VISUAL-REVIEW-0.31.0.md) · [Material/direction findings](research/ANIME_VISUAL_DIRECTION_0.31.0.md) · [Opt-in fixture](research/fixtures/visuals-0.31.0/README.md).

The client used a software-rendered headless X display with staged NoAI/invulnerable subjects and creative player casts. Audio was unavailable. These are rendering references, not a hardware-performance or multiplayer-DPS benchmark. Reduced-particle preservation was checked in the render paths; the published motion captures use full particle settings. Visual approval remains the purpose of this focused release before extending the style to other families.

## Wiki and distribution

- **153 articles plus Main Page**, **80 valid ability anchors**, **8,816 valid local HTML references**.
- **14 responsive article checks** at 1440px desktop and 390px mobile, plus the full **72-preview gallery** at both widths: no missing images, horizontal overflow or page errors.
- The browser decoded the new H.264 reel at its expected resolution and duration. Animated previews load in the articles. Search and ability navigation checks passed.
- The offline archive matches all wiki files byte-for-byte: **756 entries**. Browser checks used a local HTTP server; the managed browser blocks direct `file:` navigation, so that launch method was not tested here.
- The prepared GitHub Wiki mirror contains Home, all articles, sidebar and footer. Publishing to GitHub's separate disabled Wiki remains unavailable; the repository and offline editions are complete.

Distribution: `releases/tensura-myths-0.31.0.jar`, `releases/tensura-myths-wiki-0.31.0.zip` and `releases/tensura-myths-0.31.0.sha256`. Replace the previous addon on both client and server; install only one version. The namespace and existing registrations remain `wildhunt`.
