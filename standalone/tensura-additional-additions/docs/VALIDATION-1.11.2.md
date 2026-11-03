# 1.11.2 — High Realm EP balance

The completed native elite package has its base aura and magicule capacities halved once, after native evolution and loadout acquisition. Their ratio and unrelated attributes/skills are preserved. Defaults are 2.5M / 1.75M / 1.25M base EP for Bulwark / Sentinel / Stalker; custom elitePower still scales the baseline.

Saved unowned elites migrate on entity load, after any loadout upgrade. A persistent marker prevents repeated reduction. Current aura/magicules are capped without refilling them; HP and SHP are preserved. Owned/summoned/tamed creatures are excluded. The entry requirement remains 5M actual EP.

The existing native elite scenario now expects half EP. A new save/load scenario exercises unequal pool proportions, resource clamping, HP/SHP/damage preservation, repeat-load safety and ownership exclusion.

All **250 native GameTests and 31 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. The saved-mob fixture explicitly completes native first-join resource initialization before saving, as real existing mobs do.

The clean release build and distribution audit passed. The audit verifies dependency/version metadata, assets and mixins and excludes review fixtures, bundled dependencies and Myths classes. The release includes a SHA-256 checksum.
