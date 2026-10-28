# 0.34.8 validation — Terrain and headroom dash landings

All **291 required native GameTests** and **11 JUnit tests** passed. The independent Heavenly Restriction runtime passed all **58 required native GameTests**, with Tensura Myths absent.

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

## Reproduction and regressions

Before the resolver change, native tests failed in four controlled scenes: a no-dummy birch canopy/trunk scene matching the reported screenshot, a two-block corridor with slight upward aim, a slab/full-block rise at the intended landing and an enemy stop about one block away. [Root cause and algorithm](research/CHAINED_TERRAIN_0.34.8.md).

Five new native regressions in each addon now cover those cases and close-wall contact. They verify movement in the correct direction, actual standing heights, safe body clearance, target hits, one 2% current-aura payment and the 1-second cooldown. The truly blocked-destination fixture puts the body against the wall; a centered body in a one-block cell still has room for a legal short dash and is no longer treated as fully blocked.

Existing native comparisons for successful downward Martial Master landings pass, along with level/upward aim, enemy stopping, wall occlusion, allied/off-axis targets, fluid rays, cooldown, acquisition, flight, regeneration and race checks. Exclusive damage-branch regressions, including real native Aura Slash 35/70 and the controlled Aura Sword melee reference, are unchanged and pass.

## Real-player client review

The isolated **Restriction Validation** client fixture now repeats the reported terrain scene with a real server player, before spawning either of its test enemies. The following checks passed:

| Scene | Actual server landing |
| --- | --- |
| No-dummy birch canopy and trunk | `(11.5, 71, 0.5)`, body clear of leaves |
| 1° upward aim under two-block ceiling | approximately `(16.498, 71, 0.5)` |
| Slab support | `(16.5, 71.5, 0.5)` |
| Full-block rise | `(16.5, 72, 0.5)` |
| Close wall, start X 0.6 | approximately `(0.65, 71, 0.5)`, short forward movement |
| Aimed enemy at X 8.5 | approximately `(7.75, 71, 0.5)`, no overlap |

Each controlled cast pays exactly 200 aura from 10,000 and starts a 1-second cooldown. All seven level/upward open-air angles also pass. The enemy receives damage, the distant target retains full health, and the client’s position matches the server landing. Myths is absent in this standalone run.

```sh
# From standalone/heavenly-restriction, on an available graphical display:
gradle runClient -I docs/targeting-review/client.init.gradle
```

[Opt-in review fixture](../standalone/heavenly-restriction/docs/targeting-review/ChainedTargetingReview.java). It clears only its prepared platform area in the isolated validation world and removes its test mobs. Optional fixtures are excluded from normal source sets and JARs.

## Distribution

The normal Myths **0.34.8** and standalone **1.0.7** JARs are rebuilt/audited after review. Version metadata, production class bytes, standalone dependencies/assets and exclusion of optional review fixtures are checked. No dependency JARs or Myths classes are bundled in the independent addon. Checksums accompany both JARs and the updated offline wiki archive.

The in-game skill description, wiki and standalone guide explain shorter safe landings, terrain support and free rejection when no forward landing exists. All 162 HTML pages, 86 ability anchors and 9,640 local references validate; all 789 archive entries match the wiki sources. Install the selected addon on client and server.
