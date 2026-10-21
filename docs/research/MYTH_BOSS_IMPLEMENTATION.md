# First Myth: boss implementation research

Inspected 2026-10-21 for Tensura Myths 0.25.0. This document records implementation evidence and design decisions; gameplay instructions belong in the [Ananke article](../../wiki/pages/Ananke.md). Third-party binaries and inspected decompilations remain outside the repository. No third-party models, textures, animations or boss source are redistributed.

## Versions and primary sources

| Project | Inspected release | Source |
| --- | --- | --- |
| Tensura: Reincarnated | 2.0.1.3, NeoForge 1.21.1, CurseForge file 8793360 | [Project](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated) · [Pinned file](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated/files/8793360) |
| Tensura Reincarnated: Nightmares | `trnightmare-1.0.5.2.2.5-neoforge-1.21.1.jar`, file 9120824 | [Project](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated-nightmares) · [Release binary](https://edge.forgecdn.net/files/9120/824/trnightmare-1.0.5.2.2.5-neoforge-1.21.1.jar) |
| Tensura Mysticism | version number `2.2.1-neoforge`, `mysticism-neoforge-2.2.1.jar` | [Project](https://modrinth.com/mod/tensura-mysticism) · [Version metadata](https://api.modrinth.com/v2/project/tensura-mysticism/version) · [Release binary](https://cdn.modrinth.com/data/bjvFzLV3/versions/q4YA4Wke/mysticism-neoforge-2.2.1.jar) |

Nightmares SHA-256: `892d77a3169a45c14c41efc277cae8204d2b0786fec6f5416b9ad7bcaa35dd04`.
Mysticism SHA-256: `61e98fe4666cb2baa35396dca7d4e1bb7e41069f2ddfc4af9799b2bb5eb442d3`.

Nightmares declares NeoForge 21.1.220+, above our pinned 21.1.209. It was inspected separately, not installed in the test pack. These findings are implementation references, not claims that all three addons have been tested together. Website access to some CurseForge/wiki pages returned HTTP 403; the release binaries, datapacks and compiled implementations supplied the primary evidence.

## Native bosses and existence data

Inspected `CharybdisEntity`, `HinataSakaguchiEntity`, `GazelDwargoEntity`, `OrcDisasterEntity`, `IfritEntity`, `ElementalColossusEntity`, native base entity classes, `EntityExistenceData` and every shipped `entity_existence` profile. The table sums the profile's maximum aura and magicules; it does not claim to include equipment, configuration changes or every runtime transformation.

| Native profile | Maximum shipped AP + MP | SHP | Notable implementation |
| --- | ---: | ---: | --- |
| Charybdis | 1,600,000 | 3,200 | 3,000 base HP, native bossbar tracking, flight, SmartBrain behaviors, scale attacks; `hurt` halves incoming damage |
| Hinata | 1,036,332 | 3,600 | 3,000 base HP, Otherworlder storage/equipment, phase behavior, native Unique skill activation |
| Gazel | 1,036,332 | 3,600 | Native humanoid combat and large real skill package |
| Elemental Colossus | 350,000 | 4,500 | Native elemental entity mechanics |
| Orc Disaster | 250,000 | 2,700 | Native combat, consumption and skill data |
| Ifrit | 150,000 | 2,000 | Native spirit/elemental combat |

The entity-existence JSON codec contains entity type, SHP, aura/magicule ranges, abilities, optional evolution and random abilities. It is an actual native resource/skill source, not a decorative EP label. `evolution` is not used to declare Ananke's race: the race is assigned through RaceAPI. The global native attribute/storage systems also work for a custom Monster subclass; inheriting the tamable Otherworlder interaction menu is unnecessary for an untamable boss.

Native `c:bosses` and `tensura:boss_for_hero` tags provide progression recognition. Myths' existing distinct-boss progression reads the former. Native damage sources, barriers, SHP, skill timers and Godly ownership traits remain authoritative.

## Native arena support

Inspected `IArenaBoss`, `BossFightInstance`, `BossFightConfigManager`, `IBossFightHolder` and `BossFightCommand`.

- `IArenaBoss.onSpawned(level, id, instance)` is called by the native arena after entity construction/finalization. Ananke stores the arena ID and anchors its encounter at that arena's center.
- Boss fight storage belongs to the overworld holder; each fight separately records its actual dimension and positions.
- The manager saves per-world JSON in `data/boss_fights/`. Myths writes a profile only when an operator explicitly runs `/myths boss arena`; existing profiles are not overwritten.
- `/bossFight join @s ananke` is the inspected native command syntax: it needs the player selector before the fight name.
- Joined-player filtering prevents the arena boss from choosing unjoined players. Native encounter lifecycle remains managed by Tensura.
- Native restrictions specific to the `boss_area` dimension should not be assumed for an ordinary overworld lodestone ritual.

## Nightmares bosses

Inspected `AbstractTrNightmareSentientBossEntity`, Guy, Veldora, Milim/Satanael, `SentientBossCombatHandler`, `NightmareBossConfig`, reward and Dragon Core EP handlers.

| Reference | Verified implementation | Myths implication |
| --- | --- | --- |
| Abstract sentient boss | Native `PlayerLikeEntity`, once-only persistent bootstrap flag, actual learned skill lookup followed by native `onPressed`/`onHeld`, goals, pursuit and movement assistance | Assign real skills and preserve their storage; explicitly orchestrate the modes the boss actually uses |
| Veldora | Default 10,000 HP / 20,000 SHP, 30M–40M EP, armor 60; range-selected Black Lightning modes, held attack and dash/flight logic | 30M is a credible addon boss scale, substantially above inspected native profiles |
| Guy | Default 24,000 HP / 58,000 SHP, armor 80, 40M–40,777,777 EP; extensive native/addon skill package | Do not claim a 30M Myths boss outscales every addon boss |
| Combat handler | Incoming pre/final HP damage caps of 750, high bypass settings on outgoing sources | Myths deliberately uses neither an arbitrary incoming hard cap nor a copied blanket bypass formula |
| Spawns/rewards | Configured rare natural replacement, data-driven rewards | First Myths boss is an explicit ritual, not an unavoidable replacement of an ordinary world spawn |

## Mysticism bosses

Inspected `MemoiresEntity` and its dependencies: native Otherworlder / SmartBrain owner, Repeater Unique, custom recursion-depth/phase handling, teleportation and actual skill use. Its inspected base attributes include 1,800 HP, armor 100, attack 25, SHP regeneration 100, AP/MP regeneration 25. It illustrates how a bespoke encounter mechanic can matter more than an inflated health or EP label.

## Original Myths encounter

**Ananke, the Unwritten End** is an **Eclipse Seraph**, exactly 15M base aura + 15M base magicules, 48,000 HP and 120,000 SHP at initialization. Native 30M EP is the target; it is not a new EP damage multiplier. Three health phases increase attack cadence and introduce actual Thoth use. The original model has six articulated wings, ceremonial armor, emissive face/core details and orbiting halo geometry.

**Moira, Lord of Finality** is a real native Ultimate with its own skill instance, costs, cooldowns and source identity. Its rites use four different damage types and shapes: a locked fan, individually locked starfall warnings, a hollow moving wave, and a final blast with a real safe annulus. Owning Thoth gives the same Godly classification traits a player receives. Thoth's paid passive, Astral Library and Spellbreak are used; a skill merely existing in storage is not a claim that every player-only mode has NPC AI.

Native **Self-Regeneration** is used deliberately. Inspection found that Ultraspeed Regeneration's native Instant Regeneration effect can restore *all missing HP every ten ticks* while funded. With 15M magicules that would turn this encounter into resource exhaustion or a one-shot check. Self-Regeneration supplies native sustained recovery, respects healing suppression and leaves room for ordinary damage windows. There are no artificial phase invulnerability windows, damage caps or forced hit-immunity resets. Healing, resource costs and penetration still follow native systems and server configuration.

The pinned native maximum-HP attribute clamps at 10,000 by default. Ananke's entity-local health accessor evaluates its own native base and modifiers in vanilla order, allowing its actual 48,000 HP without changing the global cap. HP saves, native `getMaxHealth`, healing and client bossbars use this accessor. Other entities keep their configured limits. Native skill storage also contains negative-mastery learning records: initialization masters only the fifteen chosen skills, not every learning record in storage.

Starfall render bounds include each actual warning position and its sky lance. Boss visuals use a separate 192-block-tracking native entity type; ordinary skill visuals keep their 128-block tracking budget. Client view distance still applies normally.

Encounter plans are server-owned and short-lived. Reload cancels pending attacks rather than resurrecting untelegraphed hits. Native resources, learned skills and damage persist; initialization never refills them on load. Losing every valid target for ten seconds resets the encounter. Death/removal cancels owned effects and both bossbars. Ritual failures consume nothing; player kills have a dedicated trophy, native loot and a challenge advancement.

The release checks and limitations are recorded in the release changelog. This is a target for endgame playtesting, not proof of a universal ranking across custom server configurations or unrelated addons.
