# 0.32.0 validation — Heavenly Restriction and Chained

## Build and native integration

Validated on Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
python scripts/verify-visual-assets.py
```

**BUILD SUCCESSFUL. All 252 required native GameTests and all 11 JUnit tests passed.** This includes 22 new Chained checks and the 230 existing gameplay regressions.

The new native checks cover:

- Native reincarnation availability, guaranteed Chained, exclusion from ordinary Unique rolls, five linked races and Human-family classification.
- Correct literal colored name; starting native resource initialization before locking capacity.
- 100× aura capacity from base/modifier sources; direct/helper/modifier MP growth restrictions with the toggle off and skill unslotted; an effective spiritual limit retained as the MP ceiling.
- Real reincarnation-granted Martial Master and its actual nested Heavenly Eye grant; skill-before-race inheritance; blocked unrelated gifts and update-API additions without spending acquisition magicules.
- Battlewill and Resistance/Nullification eligibility; selective +9 learning/mastery and +19 direct resistance learning; native Diamond Path and Aura Sword numeric buff tripling without compounding.
- Converted natural regeneration at full MP and while the MP pool is depleted.
- Live mastered resistance/dodge bypass, one-tier status immunity handling and source isolation; guaranteed slotted projectile dodge while operator damage remains possible.
- Real living-caster teleport movement, exact once-only 2% current-aura payment, separate path/arrival hits, native hurt-cooldown handling, radius/cooldown constraints, solid-wall clipping and a free fully blocked activation.
- Fixed base-EP/training gates, four actual native race changes preserving the skill/mastery and replacing racial modifiers, and save/reload/forget cleanup.
- Native paid Resistance/Nullification acquisition and latent completion using configured current-aura costs, including an unfunded rollback, while current and permanent magicules remain unchanged.
- Actual inherited Unique synthesis into Vritra and subsequent Godly ascension into Aion, preserving ancestry through the native addon systems.

Two pre-existing fixtures were made independent of asynchronous entity timing. Anansi starts its unchanged 12-tick arming window after target tracking is ready. Thoth identifies each new volley ray by entity identity rather than assuming its tick counter remains zero. Their combat/duration/source assertions remain intact; neither ability’s implementation changed.

## Native client and assets

The isolated **Myths Validation** world reviewed the actual player teleport and new procedural wake. The client logged **16.0 blocks travelled** and **1,000 aura paid from a staged 50,000-aura pool**. The native Status menu displayed Fettered Human and the resulting aura/magicule values. Native photographs are in `wiki/media/chained-teleport.png` and `chained-status.png`. These use staged values rather than a fresh survival reincarnation.

Client review caught the inherited colored-name method exposing a translation key. The release overrides it with the literal Unique-colored name, provides matching English locale entries and verifies the colored name in the native suite. The final acquisition-payment and inheritance changes were validated by the native server tests. The client was software rendered without a shader pack; audio was unavailable, and the review is not a hardware-performance or multiplayer-DPS benchmark.

The asset graph passes for **73 active effect IDs**: **72 authored rig routes plus the procedural Chained wake**. Existing **83 Blockbench projects**, **79 geometry/export pairs**, **171 animation clips**, **1,176 curved/faceted surfaces**, **73 continuous rings** and **four item models** remain valid. Six original, centered 64×64 pixel icons use four-pixel outer margins. The chain wake needs no additional authored rig because its links deform along the actual teleport journey.

The normal release JAR contains Chained, RestrictedRace, the new mixins, renderer, icons, locale entries and version 0.32.0 metadata. It excludes `ChainedReview.class`, `OriginReview.class`, `SpectacleReview.class` and `VisualPreview.class`. The opt-in [client fixture](research/fixtures/chained-0.32.0/README.md) is separate from normal sources. [Integration notes](research/HEAVENLY_RESTRICTION_0.32.0.md) document the pinned native callbacks and ancestry handling.

## Wiki and distribution

- **159 articles plus Main Page**, **85 valid ability anchors**, **9,342 valid local HTML references**.
- **16 responsive page checks** at 1440px and 390px, covering Chained, all five new race pages, Unique Skills and the visual gallery. All six new articles have the standard infobox and portrait layout.
- All **72 existing formation previews** remain available. New icons, native photographs and Severance Rush search work without missing images, horizontal overflow or page errors.
- The offline archive matches every wiki file byte-for-byte: **782 entries**. Browser checks used a local HTTP server. The prepared GitHub Wiki mirror includes Home, every article, sidebar and footer; the repository/offline editions are available while the separate GitHub Wiki remains disabled.

Distribution: `releases/tensura-myths-0.32.0.jar`, `releases/tensura-myths-wiki-0.32.0.zip` and `releases/tensura-myths-0.32.0.sha256`. Replace the previous addon on client and server; install only one version. Existing registrations retain the `wildhunt` namespace.
