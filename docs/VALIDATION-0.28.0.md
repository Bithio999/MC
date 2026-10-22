# Tensura Myths 0.28.0 validation

Validated 21 October 2026 using Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3 and ManasCore 4.0.0.2.

The release build passed **206 required native GameTests** and **11 JUnit tests**, with zero required failures.

## Regeneration and phases

Nine new native tests exercise the actual boss, native skills and death pathways:

- An ordinary lethal Godly hit leaves funded Infinite Regeneration in control: Ananke stays in phase I, recovers full HP and spends magicules. Exhausting its MP then permits the next fatal hit to advance the phase.
- Fatal HP damage with exhausted regeneration advances exactly one phase, restores HP/SHP/aura/magicules to their current native caps, and awards no intermediate boss progression. The third exhausted defeat produces the final death and boss credit.
- Fatal native spiritual damage also advances exactly one phase and replenishes all four pools.
- Nonfatal wounds, including HP below the former thresholds, never advance phases.
- Revival cancels pending rites and their owned visuals without dropping intermediate loot.
- Save/load retains the spent phase and wounded/depleted pools; it does not grant a fresh life or refill on load.
- Confirmed HP and SHP losses set synchronized red-feedback state despite active native Pain Nullification. A nullified ordinary attack does not flash, and the brief flash expires.
- Operator kill removes the boss directly.

Native Infinite Regeneration eligibility remains authoritative, including its qualifying damage bypasses. An exhausted lingering effect cannot prevent final death without funding; the native NPC reserve is ten magicules. Revival preserves native skill cooldowns, resets pending attacks and provides a 45-tick attack decision interval. It does not grant a new custom invulnerability window.

The existing late-phase AI test now enters phase III through two actual fatal hits rather than percentage HP. A cross-dimension visual cleanup fixture waits for native entity indexing before cleanup; production Aion behavior is unchanged.

## Client feedback

Opened only the isolated **Myths Validation** world in the native Minecraft client. Captured the same live boss before damage, during its red overlay and after eleven simulation ticks. The main model visibly flashes red and returns to its ordinary material while native regeneration remains enabled. The current wiki includes the unedited in-client hit capture.

Tensura suppresses the usual hurt overlay when Pain Nullification applies. The override is confined to Ananke's model and confirmed HP/SHP losses; the native skill stays mastered and toggled on. Other entities and Tensura's global rendering are unchanged. Client feedback was checked in a local integrated server; a separate dedicated-client network session was not exercised.

## Documentation and artifacts

Both wiki editions and the offline archive were regenerated. Validation resolved **8,201 local references** across **151 HTML views**, checked every file in the **660-file** archive against its source, and passed six responsive home/Ananke/install checks plus **72 ability anchors**, images and search with zero browser page errors. The gameplay article describes current encounter rules; development history remains in the changelog.

Release JAR: `releases/tensura-myths-0.28.0.jar`. SHA-256: `6c16b2f1f4c5b745e8d7b3e0f3a8c5e7f61e3dec4426e5bb321fa78a08ff89d7`. The checksum matches, the embedded version is 0.28.0, and the retired recovery class and temporary package audit probe are absent.
