# Ultimate balance references for 0.18.0

Inspected 2026-10-20. This is a design/implementation reference, separate from the
gameplay wiki. Third-party binaries and decompiled source remain in `/tmp` and
are not redistributed. This is not a runtime compatibility claim for those addons.

## Sources and version boundaries

- [Nightmares on CurseForge](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated-nightmares):
  inspected the previously downloaded NeoForge 1.21.1 release
  `trnightmare-1.0.5.2.2.5-neoforge-1.21.1.jar`, file 9120824,
  SHA-256 `892d77a3169a45c14c41efc277cae8204d2b0786fec6f5416b9ad7bcaa35dd04`.
  See [the earlier tier inspection](NIGHTMARES_ULTIMATE_CLASSES.md). This declares
  NeoForge 21.1.220+, above this project's pinned 21.1.209, so it was inspected
  separately rather than added as a runtime dependency.
- [Mysticism project description](https://modrinth.com/mod/tensura-mysticism)
  and [Modrinth metadata](https://api.modrinth.com/v2/project/tensura-mysticism).
  Also inspected current NeoForge 1.21.1 `mysticism-neoforge-2.2.1.jar`, SHA-256
  `61e98fe4666cb2baa35396dca7d4e1bb7e41069f2ddfc4af9799b2bb5eb442d3`.
  Its Ultimate registry/class inventory does **not** contain the wiki's combat
  roster; the class found under `skill/ultimate` is Embryo. The wiki's current
  1.21.1 navigation labels Ultimates “Not yet Implemented / None.”
- Mysticism's [Takemikazuchi](https://trmysticism.wiki.gg/wiki/Takemikazuchi)
  and [Ignis](https://trmysticism.wiki.gg/wiki/Ignis) pages were accessible during
  this inspection. Their version-history sections identify the original 1.19.2
  additions and leave the 1.21.1 entry unspecified. Their mechanics are used as
  **legacy design references**, not claimed as functioning 2.2.1 implementations.

## What the references actually show

| Reference | Verified values / behavior | Implication |
| --- | --- | --- |
| Nightmares Agni configuration | 50 Flame damage, 100 mastered, with separate execution EP-ratio thresholds of 0.5 for Ultimate targets and 0.6 otherwise; 5M MP acquisition | A small raw hit can accompany very strong conditional utility. Do not equate raw damage alone with tier strength. |
| Nightmares Surya configuration/code | Disintegration 250 / 300 mastered, extra spiritual interactions and Spiritron requirements; source code explicitly handles its coating and magic interactions | Damage type, penetration, resource gates and auxiliary mechanics matter as much as the printed number. |
| Nightmares Solomon implementation | Firebolt projectile damage 1,750; Mind Requiem attempts 3,500 HP damage and, on a successful hit, 35,000 direct spiritual damage; obtain EP cost 66.6M | This is a high-gate comparison, not a justification to copy 35,000 direct SHP damage onto a 2M Myths Ultimate. |
| Nightmares Uriel Lord of Oath implementation | Barrier resource consumption scales with absorbed damage; separate imprison/defense modes and configured cooldown/failure paths | A coherent utility kit, failure behavior and resource economy are part of Ultimate quality. |
| Mysticism legacy Takemikazuchi wiki | Melee multipliers of 5× / 10× mastered, substantial attack speed, native defense penetration and 100K AP acceleration | Useful evidence of the expected combat-role jump; not a current 1.21.1 numeric baseline. |
| Mysticism legacy Ignis wiki | Matter economy, prepared landmines, mobility and a delayed conditional bomb; ordinary modes list 10K–30K MP costs | Distinct linked mechanics and visible preparation can carry power beyond a generic area hit. |

These examples vary enormously in acquisition gate, damage type, per-hit timing,
resource storage and direct spiritual handling. There is no universal “addon
Ultimate damage” constant. No third-party immunity bypass or instant-kill formula
is copied into this addon.

## Decisions for Myths

All five Myths Ultimates retain the existing 2M base-EP/two-mastered-Unique gate,
three active modes and native Ultimate classification. Their effects are original.

- Vritra combines a direct 1,500 Lightning hit / 850 Fire fuse, a six-target
  900-Lightning / 500-Fire storm cadence and 1,800 / 2,400 empowered eruptions.
  Its storm changes the firing origin and is consumed by the finisher.
- Demeter trades the repeated ward for a one-use, threshold-triggered 40% HP/SHP
  rescue seed. Its orchard restores 10% HP/SHP per second, with 500 Ice damage;
  consuming it empowers the 1,400 / 2,200 Ice harvest and heals allies 20%.
- Janus retains safe momentum-preserving travel, with lower-cost gate recasts,
  a second snare origin at the opposite gate and delayed 1,700-damage Edict cuts.
- Anansi uses three bounded anchors, line intersections rather than a generic
  disk, actual anchor traversal and a two-phase triangular finisher.
- Aegir uses a stationary water/refraction setup, unioned ray targeting and an
  expanding once-per-victim annular wave. Reflection retains projectile behavior.

Aura costs are increased to 3.5K–25K for combat arts, matching the scale of the
new output and utility without merging aura and magicules. Janus recasts cost
400 aura; anchor placement costs 5K. Full mastery remains 1.5×, with **no EP
multiplier**. Native level-1 resistance bypass is attached to Ultimate art hits;
attribute defenses, barriers, native nullification fallback rules, hit immunity,
authority and PvP checks remain relevant. Godly classification's full native
resistance/nullification bypass remains stronger.

Ordinary elemental bonuses still select their strongest source. New Ultimate
passives do not multiply each other's elemental bonuses. Thoth's Godly bonus is
separate. New kits add no passive barriers, hit-count stacks or direct health
assignment to bypass combat rules.

Lugh is a Godly utility/combat package with three immediately usable forms,
2,000–4,000 bounded reconstruction damage, a specific six-second non-Godly
technique seal and a warned, three-step 1.8× synthesis. It requires a mastered
Ultimate plus mastered Threadweaver and the existing true-awakening/12M gate.
Reconstruction never invokes another addon's implementation or copies an
instant-kill/disintegration damage source. Generic special sources normalize to
native generic magic; physical sources can normalize to aura slash.

Native tests check source attribution, actual progression, native damage paths,
resource conservation, target geometry, warnings and hit spacing. These checks
establish functional budgets and invariants, not a claim of exact PvP parity
against every third-party configuration.
