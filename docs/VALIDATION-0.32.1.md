# 0.32.1 validation — Chained balance and lineage growth

Validated with Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
python scripts/verify-visual-assets.py
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

**BUILD SUCCESSFUL. All 255 required native GameTests and all 11 JUnit tests passed.** The native suite includes 25 Chained checks and the existing 230 gameplay regressions.

## Gameplay checks

- Chained applies a single 5× maximum-aura modifier to base and external capacity sources with the toggle off and skill unslotted. A fresh Fettered Human has 20,000 base aura, 100,000 effective maximum aura and 500 maximum magicules. Reload and forgetting do not compound or leave behind the modifier.
- A saved 0.32.0 modifier of +99 is replaced with +4 under the same modifier ID on refresh. Base capacity and skill mastery are preserved. Current aura continues to use native capacity/overflow handling.
- Only Fettered Human lists and grants Chained as an intrinsic skill. Direct selection of each later stage does not grant it; normal native evolution retains the existing skill and mastery through all four stages. A forgotten Chained is not regranted by evolution.
- Actual native kill rewards use 90% aura / 10% magicule weights for every stage, preserving the original total growth weight. Tests account for native gain attributes and their caps. Chained multiplies the resulting aura capacity by five and still blocks the magicule share. The blocked share is not reallocated.
- A secondary race preserves this split and adds its existing 10% bonus in actual native awards. The progression-menu snapshot reports the same weights. Existing standard Human/Hunter and Convergent growth tests also pass.
- Severance Rush uses path damage **4 × √(aura paid)** and arrival damage **8 × √(aura paid)**, with zero damage at zero payment. Native teleport tests verify the once-only 2% current-aura payment, sixteen-block movement, separate path/arrival hits, radius limits, cooldown, native Battlewill classification and collision handling. Martial Supremacy retains its native 8× Battlewill multiplier.
- All prior learning, inheritance, acquisition-cost, regeneration, buff, resistance, dodge, evolution and synthesis checks continue to pass.

The new EP fixture initially assumed an uncapped gain attribute; its final expectations use the effective native attribute values. No gameplay workaround was introduced for the fixture.

## Documentation and distribution

- Updated Chained, all five race articles, race/Unique directories and release links. Historical research and validation reports remain historical.
- **160 HTML pages**, **9,342 valid local references**, **85 valid ability anchors** and **782 archive entries** matching the wiki files byte-for-byte.
- **16 desktop/mobile page checks** passed, including all six lineage/skill articles. Search, portraits and the existing formation gallery have no missing images, horizontal overflow or browser errors.
- The existing asset graph passes: **73 active effect IDs**, **79 editor/export pairs**, **171 animation clips**, **1,176 curved/faceted surfaces** and **73 continuous rings**. This release does not change visual geometry or animations; a graphical Minecraft client was not rerun for the numerical rebalance.
- Release JAR metadata reports 0.32.1 and contains the updated gameplay classes. Opt-in client review fixtures remain excluded.

Distribution: `releases/tensura-myths-0.32.1.jar`, `releases/tensura-myths-wiki-0.32.1.zip` and `releases/tensura-myths-0.32.1.sha256`. Replace the previous addon on both client and server and install only one addon version. The `wildhunt` namespace remains unchanged. The repository and offline wiki are updated; the separate GitHub Wiki remains disabled.
