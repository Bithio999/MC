# The High Realm

[All wikis](https://github.com/Bithio999/MC/wiki/Home) · [TensuraAdditionalAdditions](https://github.com/Bithio999/MC/wiki/TensuraAdditionalAdditions)

A late-game dimension of pale ridges, cherry groves and misty basins. **10,000,000 actual maximum EP** is required to enter. EP is checked, not spent. Supports single-player and multiplayer.

![Starwood Grove’s pale ridges and cherry trees at permanent twilight.](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/additions-high-realm-landscape.png)

## Gate

Build a vertical **5 × 6** frame in either direction. Its **3 × 4** opening must be empty, without fluids.

```text
N C A C N
C . . . C
C . . . C
C . . . C
C . . . C
N C L C N
```

| Symbol | Material | Count |
| --- | --- | ---: |
| N | Netherite Block | 4 |
| C | Crying Obsidian | 12 |
| A | Amethyst Block | 1 |
| L | Lodestone | 1 |
| . | Empty opening | — |

Use **one Nether Star** on the Lodestone to awaken the gate. In survival, the star is consumed once. Stand inside the awakened arch for **2 seconds**, or use its Lodestone, to enter. Every player must meet the EP requirement. Breaking the frame disables it.

![An awakened High Realm gate.](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/additions-high-realm-gate.png)

## Landscape and enemies

The realm stays in twilight. Terrain uses existing Minecraft and Tensura blocks, with native magic ore, emeralds and rare diamonds in the calcite and basalt.

| Biome | Landscape | Elite inhabitants |
| --- | --- | --- |
| Ivory Crags | Calcite mountains with basalt bands | Orcs, Armorsauruses, Horned Bears, Lesser Daemons |
| Starwood Grove | Pale grass and cherry groves | Direwolves, Blade Tigers, Knight Spiders |
| Mistglass Basin | Mossy basins, clay and turquoise water | Lizardmen, Basilisks, Greater Daemons |

The natural spawn pool is **entirely Tensura mobs**. **Bosses are excluded**, including the boss-class greater elemental spirits. Elites retain their models, animations and native combat behavior. They alert to nearby survival players, including players with higher EP. Owned mobs, summons and imports are not upgraded.

| Archetype | Members | Base EP | HP floor | SHP floor |
| --- | --- | ---: | ---: | ---: |
| High Bulwark | Orc, Armorsaurus, Horned Bear | 5,000,000 | 1,024 | 5,000 |
| High Sentinel | Lizardman, Basilisk, Lesser/Greater Daemon | 3,500,000 | 850 | 3,200 |
| High Stalker | Direwolf, Blade Tiger, Knight Spider | 2,500,000 | 700 | 2,400 |

Native skills can alter final stats. All elites gain **9–10 mastered skills** in addition to their retained native abilities. Their toggles are enabled; skills that require a preset slot are equipped.

### Shared skills

Every elite receives **Physical Attack Resistance**, **Magic Resistance**, **Magic Sense**, **Danger Sense**, and **Ultraspeed Regeneration**. Regeneration uses the native resource costs; these enemies do not receive Infinite Regeneration.

### Species specialties

These supplement the shared skills.

| Species | Added skills |
| --- | --- |
| Orc | **Berserker**, Steel Strength, Strengthen Body, Pain Resistance |
| Armorsaurus | **Survivor**, Body Armor, Scale Armor, Pierce Resistance |
| Horned Bear | **Fighter**, Strength, Steel Strength, Pain Resistance |
| Direwolf | Godwolf Sense, Steel Strength, Electricity Resistance, Paralysis Resistance |
| Blade Tiger | **Seer**, **Fighter**, Steel Strength, Wind Attack Resistance |
| Knight Spider | Body Armor, Steel Strength, Poison, Poison Resistance, Paralysis Resistance |
| Lizardman | Scale Armor, Water Breathing, **Fighter**, Water Attack Resistance |
| Basilisk | Scale Armor, Poison, Paralysis, Poison Nullification, Paralysis Nullification |
| Lesser Daemon | Mana Manipulation, Flame Manipulation, Water Manipulation, Wind Manipulation |
| Greater Daemon | Mana Manipulation, Flame Manipulation, Water Manipulation, Wind Manipulation, Lightning Manipulation |

### Combat magic

The daemon spells are actually used by their native combat AI, with native casting circles, attack animations, projectiles and magicule costs. Elemental manipulation skills strengthen their corresponding magic.

| Caster | Magics |
| --- | --- |
| Lesser Daemon | Fire (bolt), Water Cutter, Wind Cutter, Stone Shot, Fire Ball |
| Greater Daemon | Fire Lance, Acid Shell, Icicle Lance, Tornado Blade, Thunder Orb, Stone Shot, Sleep Mist |

### Rewards

Player or subordinate kills retain normal loot and EP and add **1–3 Amethyst Shards**, with a **20%** chance of native Magic Ore and a **2%** chance of Netherite Scrap.

## Returning

The **quartz return arch** in the arrival sanctum is the return gate. Walk through it or use its central bedrock base. **Leaving has no EP requirement.** Each player returns near their own entrance; obstructed exits use nearby safe ground or the Overworld spawn.

The small arrival sanctum is protected from normal block edits. Natural hostile spawns are suppressed within **48 blocks** of its center. The rest of the dimension can be mined and built in. **Beds do not work here.**

![The protected return sanctum.](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/additions-high-realm-arrival.png)

## Saves and settings

The dimension is added to existing saves. Its terrain, awakened gates and individual return points persist after a restart. [World Hop](https://github.com/Bithio999/MC/wiki/Additions-Parallel-World-Jumper) generates a fresh realm for a new seed; returning to a previously visited world restores that world's realm.

Existing eligible unowned Tensura elites receive the new skill package without refilling their resources or resetting their EP. Existing vanilla elites are left to despawn normally; no new vanilla elites spawn.

Server settings are in `serverconfig/tensura-additions-high-realm.toml`: `entryEP`, `gateChargeTicks`, `elitePower`, and `eliteSpawns`. Changing elite strength affects new spawns. Up to **256 awakened gates** can be registered in one world.

Find this page under **Dimensions** in the searchable **F8 guide**.

![The High Realm in the in-game guide.](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/additions-high-realm-guide.png)
