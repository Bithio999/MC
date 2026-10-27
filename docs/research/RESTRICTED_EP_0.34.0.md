# Restricted aura growth and EP — Mysticism 2.2.1

**Answer: yes. Restricted’s increased aura gains become permanent raw aura capacity, so they increase both base EP and effective/status EP.** This is a gain-rate bonus; acquiring the skill does not instantly triple existing aura capacity.

## Inspected releases

- [Tensura Mysticism](https://modrinth.com/mod/tensura-mysticism), current NeoForge 1.21.1 release **2.2.1-neoforge**, version ID `q4YA4Wke`. [Version metadata](https://api.modrinth.com/v2/version/q4YA4Wke) and [inspected JAR](https://cdn.modrinth.com/data/bjvFzLV3/versions/q4YA4Wke/mysticism-neoforge-2.2.1.jar).
- Download SHA-256: `61e98fe4666cb2baa35396dca7d4e1bb7e41069f2ddfc4af9799b2bb5eb442d3`, matching the previously recorded release. Modrinth reports SHA-512 `03450ee8e2717971008d568867a2349b133eefb5b61d059370abb9002f3a95948d67af3346aae5b69464258721a42077db3bb48157466d7aa67d624b6949ec16`.
- Native EP/gain handling was traced in the installed **Tensura 2.0.1.3** JAR. CFR 0.152 and `javap` were used for inspection. Dependency binaries and decompiled sources are not distributed in this repository.

## Code trace

1. `io.github.Memoires.mysticism.ability.skill.unique.RestrictedSkill.onLearnSkill` adds an attribute modifier to `TensuraAttributes.AURA_GAIN` with **amount 2.0** and **ADD_MULTIPLIED_TOTAL**. That operation yields **3×**, because the factor is 1 + amount. `onRespawn` restores the same modifier.
2. The skill separately applies the same factor to `AURA_REGENERATION_MULTIPLIER`. Regeneration restores current aura; regeneration alone does not increase maximum aura or EP. It also suppresses `MAGICULE_GAIN` through a large negative additive modifier.
3. Native `DeathHandler.gainEPEntity` reads the effective `AURA_GAIN`, calculates the aura share, applies native penalties/caps, and writes that result into **MAX_AURA’s base value** using `setBaseValue(oldBase + awardedAura)`.
4. Native `EnergyHelper.getBaseMaxEP` sums base maximum aura and base maximum magicules. `getMaxEP` sums their effective maxima. Increased raw aura therefore enters both calculations. Native `MainScreen` uses `getMaxEP` for its EP readout.
5. Mysticism’s `MixinEnergyHelper` modifies energy costs/draining; it does not remove this gain from either EP getter. Its `RestrictedHandler` also reacts to raw aura-base increases for EP-related progression and acquisition handling.

The gain attribute affects systems that read it, such as normal kill EP. This is not proof that every direct scripted capacity grant is tripled. Native attribute bounds, gain limits, penalties and other modifiers still apply.

## Comparison with Chained

| Mechanism | Mysticism Restricted | Tensura Myths Chained |
| --- | --- | --- |
| Multiplied quantity | Aura gain rate, 3× from the inspected modifier | Effective maximum-aura growth, 5× with the starting pool excluded |
| Writes boosted kill gains into raw aura | Yes | The normal raw award is retained; the extra capacity comes from modifiers |
| Bonus contributes to effective/status EP | Yes | Yes |
| Bonus contributes to base EP | Yes, through permanent raw gains | The additional modifier capacity does not; normal raw growth still does |
| Instantly multiplies the starting pool | No capacity multiplier is applied by the inspected Restricted learning callback | No; the saved starting pool is explicitly exempt |

For a normalized 100 aura award before the gain-rate bonus and caps, Restricted can write 300 raw aura, contributing 300 to both EP measures. With Chained, a later 100 raw aura gain contributes 100 base EP and 500 effective EP in the absence of other modifiers/caps. Different alignment splits and native gain attributes must be accounted for when comparing actual kills.

## Verification and scope

A native GameTest reproduces Restricted’s exact +2 TOTAL modifier on `AURA_GAIN`, compares real native kill awards, and checks both EP getters. It then verifies Chained’s 100-base/500-effective EP distinction on a fresh Fettered Human. The reference modifier is emulated in the existing test runtime; Mysticism itself was inspected separately and was not installed or run with Myths. This does not claim addon interoperability testing.

Version 0.34.0 applies the requested damage changes and switches Myths progression gates to effective/status EP. Capacity and raw gain accounting remain as described; aura bonuses now count toward the addon’s progression requirements.
