# 0.34.0 validation — Total EP progression and Chained tuning

Validated with Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
python scripts/verify-visual-assets.py
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

**BUILD SUCCESSFUL. All 276 required native GameTests and all 11 JUnit tests passed.** This includes five new EP checks alongside the 271 existing gameplay regressions.

## Total EP progression

Myths progression gates now use `EnergyHelper.getMaxEP`, matching the native Status menu: effective maximum aura plus effective maximum magicules. This includes Chained and other capacity modifiers. Current aura/magicule depletion does not lower this EP value. Threshold numbers and required boss kills, mastery, names, physical forms, final evolutions and true awakenings retain their existing roles.

Covered systems: Hunter and Heavenly Restriction race evolution, `/wildhunt evolve` and status, secondary selection/evolution, Convergent Awakening, Ultimate synthesis, Godly ascension, shared server snapshots and their menu labels/tooltips. Existing configuration keys and values are preserved with updated descriptions.

The native checks verify:

- Hunter native-menu and command evolution agree at 100,000 total EP with only 1,000 raw EP. The fixed Chained evolution boundary also counts its aura bonus while preserving Battlewill/mastery gates.
- Secondary selection at 5M total EP and evolution at the fixed 7.5M/10M totals succeed with substantially lower raw EP. Native weighted requirements use total EP in both server evaluation and menu detail. Removing a capacity bonus updates readiness.
- Both races still reach their final stages before Convergent Awakening. Its 12.5M fixture threshold remains fixed as modifiers change; the final action accepts effective EP. A native intrinsic changes the fixture’s capacity by 100 EP, so the final test explicitly funds the effective threshold after intrinsic grants rather than assuming an unchanged raw pool.
- Synthesis and Godly ascension reject totals below their fixed 2M/12M thresholds and accept capacity bonuses at the thresholds, even with empty current resource bars. Other prerequisites remain enforced.
- Native secondary EP queries are owner-scoped. Nested queries and exceptions restore the previous context. Native EP queries outside the secondary calculation and raw gain/loot accounting are not globally rewritten.
- All pre-existing native secondary branching/naming audits and boss/combat regressions pass.

## Chained damage and Restricted research

Severance Rush now uses **3 × √A path damage** and **6 × √A arrival damage**, where A is the aura paid. The toggled Battlewill damage multiplier is **5×**. Existing formula, actual dash payment/hits/collision checks and native Aura Sword damage-pipeline tests pass with the revised values. Attack and numerical Battlewill buff multipliers remain 3×.

[Restricted EP research](research/RESTRICTED_EP_0.34.0.md) traces the current Mysticism 2.2.1 NeoForge binary and pinned native EP implementation. Restricted applies +2 TOTAL to `AURA_GAIN`, producing 3× gain rate. Native kill awards write the boosted result into raw aura capacity, so the result contributes to both base and total EP. A native test reproduces that exact reference modifier and verifies both getters, then verifies Chained’s raw/effective capacity distinction. Mysticism was inspected separately and was not installed or run with Myths; this is not an addon-interoperability test.

## Wiki, assets and distribution

- **161 articles plus Main Page**, **86 valid ability anchors**, **9,612 valid local references** and **789 archive entries** matching the wiki files byte-for-byte.
- **20 responsive checks** across desktop/mobile, including Chained, evolution, secondary/awakening, synthesis/Godly and fusion articles. Infoboxes, search and images work without missing assets, overflow or browser errors.
- Current requirements, all 105 fusion references, in-game wording and Chained numerical examples use the updated mechanics. Historical validation/research reports remain historical. The prepared GitHub Wiki mirror is regenerated; the separate GitHub Wiki remains disabled.
- The unchanged asset graph passes for 73 protocol effects, 79 editor/export pairs and 171 animation clips. No visual geometry is changed. A graphical Minecraft client was not rerun; native tests verify server readiness and menu payloads, while browser checks validate the wiki.
- Release JAR metadata reports 0.34.0 and includes ProgressionEP, its requirement type, the secondary EP mixin and revised Chained classes. Opt-in client review fixtures remain excluded.

Distribution: `releases/tensura-myths-0.34.0.jar`, `releases/tensura-myths-wiki-0.34.0.zip` and `releases/tensura-myths-0.34.0.sha256`. Replace the addon on both client and server and install only one version. Existing registry IDs retain the `wildhunt` namespace.
