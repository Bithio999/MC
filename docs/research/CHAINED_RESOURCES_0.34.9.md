# Chained resource and EP fixes

Reference: pinned Tensura 2.0.1.3, `ExistenceStorage`, `EnergyHelper`, `StatCommand` and `AreaMagiculeHelper`; native Ancient Forest biome data. All dependency inspection stays outside the distributed repository.

## Native EP command

The reported command is `/tensura edit stat @s ep max add 1000`. `StatCommand.addMaxEp` has two overloads, with and without `resetCurrent`. Both normally write half the amount directly into each raw capacity. The existing lineage split changed native kill weights only, leaving commands at 50/50.

A targeted wrapper now adjusts positive additions for Restricted races before the native attribute write: 90% raw aura / 10% raw magicules. Full Myths uses its existing split calculation, including a secondary-race bonus when applicable. Other races, zero/negative adjustments and absolute setters keep native command behavior. Both overloads are selected by exact descriptors and tested through the command dispatcher. The refill flag retains its native meaning.

For 1,000 awarded EP, Chained permits 900 raw aura capacity and blocks the 100 raw magicule share. The effective aura capacity rises by **4,500**. Blocked magicules are not reassigned to aura. Raw/base EP and effective/status EP remain distinct.

## Birth exemption after a permanent loss

The old modifier used the lesser of the original birth pool and current raw aura as its exemption, recalculating that amount upward as low capacity recovered. Lowering raw aura to 500 and adding 500 therefore could produce only 500 effective aura, matching the reported result.

The original birth roll stays saved. A separate remaining-exemption value can shrink when raw permanent capacity is lost; it never grows back merely because new capacity is gained. The native attribute setter snapshots the old exemption before mutation, which also handles older saves whose first post-update action is a command gain. Both owner data and skill tags preserve the remaining value, with native serialization/restoration covered by tests.

Fresh starting aura stays unmultiplied. Future raw capacity gains receive 5× after losses as well as above the original birth amount. Existing owners are migrated without rerolling capacity or adding a free reserve refill.

## Regeneration output

Native `handleEnergyRegen` runs every **10 ticks**, twice per second. Previously the addon preserved a full **1× native multiplier** and converted only the attribute bonus above it. This left the biome’s base magicule output with magicules and converted nothing at the ordinary multiplier of 1.

The requested rule is now based on actual output. Compute natural area magicules × native recovery factor × native regeneration multiplier, plus any active native sleep-recovery contribution. Keep up to **0.5 magicules per pulse**, or 1 per second, and convert the positive remainder to current aura. Poor/zero regeneration is not raised to the retained cap. Native aura recovery remains additive. Conversion works while magicules are full; it does not increase maximum aura or EP.

The injected wrapper targets only the native positive-recovery write, leaving spiritual maintenance and the separate one-time native sleep-entry seed intact. Native area/block modifiers feed the normal `AreaMagiculeHelper` result. Tests fill the actual `tensura:ancient_forest` biome in an isolated native chunk, reinitialize its native magicule data and run complete native regeneration ticks. The biome data contributes +19,500 area capacity and +100 environmental regeneration to the native defaults. Actual input, not a hardcoded forest output, drives conversion.

## Client authority and the apparent overfill

The real-player client check exposed a server/client mismatch: the server had 86 maximum magicules and the client reserve correctly showed 86, but the client maximum remained 50. The capacity-growth guard also ran on client attribute updates, rejecting legitimate authoritative increases during reincarnation/synchronization. It now applies only on the server; client attribute packets are accepted. The new exemption snapshot likewise runs only on the server. The graphical fixture checks both the reserve and maximum, plus total EP, rather than checking server values alone.

## Current magicule cap

Native storage setters accept over-max reserves, and native recovery helpers can retain intentional overflow. Chained now caps its `setMagicule` and combined `setEP` writes at the current effective maximum, including limited spiritual capacity. Refresh/login reconciles an old overfilled reserve after attributes are ready. The negative exhaustion sentinel is preserved. Race initialization is exempt while the new birth pool is being established; unrelated owners keep native overflow behavior.

This cap does not convert pre-existing or explicitly granted excess reserves into aura. Only computed regeneration output is converted. Capacity-growth blocking remains separate from current-reserve recovery.
