# 0.34.9 validation — Magicule reserves, regeneration and EP growth

All **296 required native GameTests** and **11 JUnit tests** passed. The independent Heavenly Restriction runtime passed all **63 required native GameTests**, with Tensura Myths absent. The final real-player client review also passed.

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
python standalone/tensura-additional-additions/scripts/verify-distribution.py
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

[Root causes and accounting](research/CHAINED_RESOURCES_0.34.9.md).

## Native regressions

- Current magicule setters and combined current-EP writes are capped at the effective maximum, including a lower spiritual limit. Saved overfill is reconciled after loading. The -1 exhaustion sentinel and unrelated owners’ native overflow behavior are preserved.
- Two native half-second recovery pulses retain exactly **1 magicule**, converting the computed remainder to aura. Native biome output, rate bonuses and sleep recovery are included. Full magicules do not prevent conversion; poor regeneration is not artificially increased. Other owners keep native recovery and excess-reserve decay.
- An actual `tensura:ancient_forest` biome is applied to an isolated native chunk. Native chunk data is reinitialized and filled, and complete `handleEnergyRegen` calls use the real `AreaMagiculeHelper` output. Expected normal aura recovery plus converted output and 1-MP-per-second recovery are checked together.
- The exact `/tensura edit stat @s ep max add 1000` command and its `true` refill overload are run through the native dispatcher. Both add 900 raw aura capacity, giving **4,500 effective aura capacity** under Chained, while the 100-magicule growth share is blocked. The normal variant does not refill current aura; the refill variant does.
- Lowering EP before the gain no longer suppresses the multiplier. The original birth roll stays saved while its remaining exemption shrinks after a permanent capacity loss. Older-save migration, native skill serialization, root-data fallback and later growth are checked. Restricted races without Chained receive native 900/100 capacity additions; ordinary Humans retain their original 500/500 command behavior.
- Existing movement, Physical/Battlewill exclusivity, skill learning, aura recovery, flight, races, secondary progression and boss regressions pass.

## Client/server synchronization

The first real-client test exposed the apparent overfill: server maximum/current MP was 86 while the client maximum remained 50. The capacity-growth guard was incorrectly rejecting authoritative client attribute updates. Both growth blocking and exemption snapshots now run only on the server.

The final isolated client test observed:

- The exact command: **900 raw / 4,500 effective aura capacity gained**, with unchanged maximum magicules.
- Actual Ancient Forest area magicules: **20,000**. Two native recovery pulses changed current MP **10 → 11** and current aura **0 → 409** under the review’s default configuration, including normal aura recovery.
- Client and server current/maximum MP both **76**; total EP synchronized at **5,076** after the controlled command sequence. The client skill tag retained the 500 remaining exemption separately from the original 1,887 birth roll.

[Captured client HUD](../standalone/tensura-additional-additions/docs/media/chained-resource-status.png). The [opt-in review](../standalone/tensura-additional-additions/docs/resource-review/ChainedResourceReview.java) runs only in **Restriction Validation** and is excluded from release JARs:

```sh
# From standalone/tensura-additional-additions, on an available graphical display:
gradle runClient -I docs/resource-review/client.init.gradle
```

## Distribution

Normal Myths **0.34.9** and standalone **1.0.8** JARs are rebuilt/audited after the client run. Metadata, compiled production classes and packaged mixin declarations/classes match. The independent distribution includes no Myths classes, dependency JARs or optional client fixtures. SHA-256 files accompany both JARs and the offline wiki archive.

The in-game description, wiki and standalone guide now describe flat recovery, reserve caps, 90/10 command gains and growth after permanent losses. All 162 HTML pages, 86 ability anchors and 9,640 local references validate; all 789 archive entries match wiki sources. Install the selected addon on client and server.
