# 0.33.0 validation — Air Sovereignty and Chained tuning

Validated with Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
python scripts/verify-visual-assets.py
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

**BUILD SUCCESSFUL. All 271 required native GameTests and all 11 JUnit tests passed.** This includes 12 new Air Sovereignty integration checks and all 259 existing gameplay regressions.

## Native gameplay

- Real Air Flight mastery callbacks reveal a native learning instance of Air Sovereignty while retaining the mastered predecessor. Existing masters discover the art on login; repeated discovery preserves learning/mastery. Unmastered, temporary, sub-instance and detached parent instances cannot unlock permanent progression.
- The new art has native Battlewill type/tag, 400 maximum mastery, a literal red name, its own icon and no magicule cost. It stays out of ordinary Unique rolls.
- A latent entry is trained through the native activation/learning path. It cannot fly before completion. Learning consumes aura without magicules.
- Flight activation is compared directly with native Gravity Manipulation activation: both set the same vanilla `mayfly` and `flying` flags. Native flight authorization recognizes the art only while enabled. Key release retains flight, and stopping is free even with zero aura.
- Activation and exactly 20 airborne ticks each pay the configured aura-only cost. Idle permission does not incur upkeep. Depletion ends flight without overdrawing aura or spending magicules.
- Magic Interference and movement restriction revoke flight. Forbidden activation spends no aura. Forgetting the art removes its permission; other native flight permissions and creative flight are preserved.
- Skill-state reload preserves a matching lease. Detached state without its persistent lease is revoked, and cloning clears copied flight flags and entitlement. The clone listener runs after persistent-data copying.
- Chained can learn the evolved Battlewill, use it at zero magicules and receive its normal +9 Battlewill mastery bonus.
- Chained’s existing damage-formula and live teleport tests pass with **3.5√A path damage and 7√A arrival damage**, a 12.5% reduction at fixed aura payment. Cost, travel distance, radii, collision checks, separate arrival hits and cooldown retain their existing behavior.

The final suite runs the updated ownership, clone/lease and native area-check handling. The [integration reference](research/AIR_SOVEREIGNTY_0.33.0.md) records the native classes researched and the chosen progression/permission design. Native GameTests compare flight flags, authorization and costs; a graphical Minecraft client flight playtest was not performed. Movement is supplied by the vanilla client, with no new steering code.

## Documentation and assets

- **161 articles plus Main Page**, **86 valid ability anchors**, **9,612 valid local references** and **789 byte-identical archive entries**.
- **20 responsive page checks** cover the new art/directory, Chained and lineage pages, indexes and formation gallery. Search, icons and infoboxes work without missing images, overflow or browser errors.
- New Battlewill infoboxes list **Damage type: None**. Chained’s formula and numerical examples are updated. Current release links and the prepared GitHub Wiki mirror are regenerated; the separate GitHub Wiki remains disabled.
- The original 64×64 RGBA wing/updraft icon is centered with four-pixel margins. Its editable pixel-generation script is included. Activation/deactivation use native Tensura pressure-wave particles and sounds.
- The existing visual graph still passes for **73 active protocol effects**, **79 editor/export pairs**, **171 animation clips**, **1,176 curved/faceted surfaces** and **73 smooth rings**. Existing skill and boss visuals are unchanged.
- Release JAR metadata reports 0.33.0 and includes the art, mastery/flight hooks, native Battlewill tag and texture. Opt-in review fixture classes remain excluded.

Distribution: `releases/tensura-myths-0.33.0.jar`, `releases/tensura-myths-wiki-0.33.0.zip` and `releases/tensura-myths-0.33.0.sha256`. Replace the previous addon on both client and server; install only one addon version. The `wildhunt` namespace remains stable.
