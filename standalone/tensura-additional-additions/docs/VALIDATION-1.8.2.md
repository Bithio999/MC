# 1.8.2 — Non-energy capacities, oxygen and consolidated EP

Restore maximum HP, SHP, absorption, oxygen conservation and water/lava capacity bonuses. Maximum aura and magicules remain unamplified. The two gain attributes and per-pool permanent write multipliers are removed. Native `DeathHandler.gainEPEntity` receives a single squared positive total award before the race/alignment split, existing ordinary gain modifiers and native per-pool caps. Negative/identity inputs retain the normal beneficial-only rule. Direct permanent EnergyHelper grants and administrative base-stat edits are not converted into kill awards. Current resource recovery/regeneration bonuses remain active, as requested.

Maximum air supply is converted to seconds, squared, then converted back (300 ticks / 15 seconds → 225 seconds / 4,500 ticks for an ordinary player), separately from the oxygen-conservation attribute. Existing air is not refilled by equipping. A dedicated full-integer NBT field preserves air beyond vanilla's signed-short range; loading without the passive clamps to the normal maximum. Unequipping clamps excess air. Legacy energy cleanup gets a new marker while respecting restored HP/SHP maxima; base stats and prior earned pool gains are preserved.

Native regressions compare an empowered 9 EP award with an ordinary 81 EP award, preserving both pool shares instead of independently squaring them. Coverage includes unchanged direct permanent aura/magicule grants, unchanged gain attributes, expanded oxygen capacity, 3,500 air save/load roundtrip, unequip cleanup, native recovery, restored HP/SHP, Fusion and existing skill/race/travel regressions.

The prior multiplier preview is explicitly labeled as a **1.8.1 recording**; the wiki's current table and F8 guide describe this release. Its input measurements are archived beside the assembly script for reproducibility. It is not presented as a video of the new rules.

The baseline client review passed native equip/unequip, capacity synchronization, movement exclusions, recovery and NBT preservation.

The clean baseline release passes **190 native GameTests**, **30 unit tests**, the distribution audit and the wiki link/anchor checks.

[Previous validation](VALIDATION-1.8.1.md)
