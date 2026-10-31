# 0.35.1 validation — airborne Severance Rush and race information

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3 and ManasCore 4.0.0.2. The independent addon version is **1.1.1**.

## Changes

- Remove the grounded/water-only activation gate from the shared dash destination solver. Both Chained and Unbound can activate in midair. Preserve 16-block aim, enemy stopping, safe body clearance, fluid-ignoring targeting, Physical damage, 2% current-aura cost and a one-second cooldown.
- Restore Fettered Human’s missing native reincarnation information. Tensura’s `ReincarnationScreen.renderRace` only supplies the panel for `DefaultRace`; this addon extends `TensuraRace`. A client-only mixin fills the existing native information regions for this lineage using its live difficulty, resource ranges, intrinsic skills and description. Native races and the Random option keep their normal rendering. Fettered Human retains `Difficulty.HARD`.
- Replace Unbound’s duplicated Chained icon with an original ivory fist, shattered gold shackle and broken chains. Transparent 64 × 64 production art is identical in both distributions; the [source and export command](artwork/unbound/README.md) are included.

## Verification

All **402 tests passed**: **312 full-mod native GameTests**, **79 independent standalone native GameTests**, and **11 JUnit tests**. Both normal production builds and the isolated real-client review completed successfully.

Run the native regression suites and normal production builds in both projects:

```sh
gradle build runGameTestServer --console=plain
# Repeat from standalone/tensura-additional-additions.
python3 standalone/tensura-additional-additions/scripts/verify-distribution.py
python3 scripts/verify-visual-assets.py
PYTHONPATH=/workspace/toolchains/wiki-python python3 scripts/build-wiki.py
```

New native coverage checks upward, level and downward midair movement, exactly one payment, unchanged cooldown, rejection of immediate recasts, water along the aim ray, and Unbound stopping safely before a targeted enemy while dealing damage. Earlier terrain, resource, learning, evolution, Counter, critical and flight regressions remain. The movement tests use tracked living owners because native FakePlayer ignores connection teleports.

The opt-in [real-client review](../standalone/tensura-additional-additions/docs/airborne-review/README.md) runs without Myths in an isolated validation save. An actual survival player casts Chained in midair, evolves through the native learning system, and casts Unbound in midair. Both travel 16 blocks, pay 2% current aura once and retain the one-second cooldown. The client opens the unmodified native menus, verifying the mixin loads and renders.

- [Fettered Human: Hard difficulty, starting stats, Chained and description](../standalone/tensura-additional-additions/docs/media/fettered-human-selection-1.1.1.png).
- [Unbound’s new icon in the Ultimate menu](../standalone/tensura-additional-additions/docs/media/unbound-icon-1.1.1.png).

Normal builds exclude review/capture classes. The standalone auditor now checks its client mixin as well as common mixins; the visual auditor accounts for the existing procedural Counter stance and slash. Wiki articles, generated GitHub pages, offline HTML and the downloadable archive describe current mechanics. Both JARs and the wiki archive receive SHA-256 files.

The full suite completed all assertions successfully. Accumulated disposable GameTest regions filled the workspace during final world saving; build shutdown still completed successfully. Task-generated region/entity files were removed from the two `Test Level` server worlds to restore space before packaging and Git publication. Client validation saves were retained.
