# Tensura Myths 0.27.0 validation

Validated 21 October 2026 using Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3 and ManasCore 4.0.0.2.

The release build passed **197 required native GameTests** and **11 JUnit tests**, with zero required failures.

## Native package

Ananke now owns and masters **53 skills**: Moira, Thoth, seven native support/recovery skills and all **44 defenses** from Tensura's resistance registry. The defense audit uses the actual public registry suppliers independently of the boss's explicit list and verifies every stored defense instance is mastered and ON. This covers 22 resistances and 22 nullifications, including physical/spiritual/magic/holy, elemental, pain/status, thermal and pierce coverage.

Native Infinite Regeneration is mastered and enabled. The native runtime probe removes 1,000 HP and 1,000 SHP, observes both return to their 48,000 / 120,000 caps within fifteen ticks, and verifies that magicules were spent. The removed recovery action does not restart. The pinned native Instant Regeneration implementation checks every ten ticks, consumes native magicules and applies the base mod's healing/death-prevention rules.

## Migration and behavior

- Existing 0.25 ghost modifiers still migrate once without multiplying native Space/dodge bonuses. The migration fixture supplies enough MP to fund the much stronger native recovery without exhausting Thoth's upkeep and legitimately losing its transient bonus.
- Existing 0.26 saves remove Self-Regeneration, gain Infinite Regeneration and all defenses, and advance the support revision. Old recovery-cooldown data is ignored and omitted from new saves. No recovery cast or bossbar mode can resume.
- Native resistance callbacks are tested with their paired nullification temporarily disabled, then nullification callbacks are tested enabled. This preserves native processing order: a resistance can defer to its stronger nullification.
- A level-one Ultimate hit no longer passes through native nullification. The actual Godly owner damage path still penetrates without turning off the target's learned defenses. Native poison/pain protection remains functional.
- Fresh ritual/command spawns, saved wounds/resources, real native HP/SHP/EP, range-aware paid attacks, collision-checked repositioning, all Moira warning shapes, owned visual cleanup and late-phase AI remain covered. Spawn fixtures wait for actual native initialization/indexing rather than assuming asynchronous chunk/entity insertion has finished after two or five ticks.
- Phase thresholds are observed before the native regeneration tick, so immediate healing cannot erase a threshold reached between ticks. Existing phase-AI checks isolate regeneration while testing its attack economy.

## Removed action

The custom Unwritten Renewal implementation, rendering, bossbar state, wiki entry and current gallery capture are removed. Historical changelogs, validation reports and the 0.26 offline release remain historical records. Spatial Reposition and the existing combat visuals remain available.

## Documentation and limits

The current wiki lists all 22 defense pairs and the native regeneration cadence/costs, with **None** as its damage type. Both editions and the offline archive are regenerated. Validation resolved all **8,200 local references** across **151 HTML views**, checked every file in the **659-file** archive against its source, and passed six responsive home/Ananke/install checks plus **72 ability anchors**, images and search with zero browser page errors. Native configuration, healing rules, magicule funding and defense bypass continue to apply. No external addon or head-to-head boss ranking is claimed for this change.

Release JAR: `releases/tensura-myths-0.27.0.jar`. SHA-256: `78721a9dee6b6307480965a2dcc16f961fdbc02531e095027ec047a2e2b61254`. Inspection confirms the retired recovery class and temporary audit instrumentation are absent from the JAR.
