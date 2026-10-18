# Tensura: Wild Hunt

A Monster Hunter Wilds inspired addon for **Tensura 2.0.1.3**, **Minecraft
1.21.1**, **NeoForge 21.1.209+**, and **Java 21**.

## Download

The built addon is included at [releases/tensura-wild-hunt-0.2.0.jar](releases/tensura-wild-hunt-0.2.0.jar).
Install it alongside Tensura and its required dependencies on both client and server.

## Human hunter progression

Your appearance stays human. The new branch appears alongside Human's existing
choices in Tensura's evolution menu.

| Evolution | Requirements | Bonuses at this stage |
| --- | --- | --- |
| Awakened Hunter | Human; 100K base EP; 1 distinct boss type | +10 HP, +2 attack, +5% movement, +0.1 knockback resistance |
| Apex Hunter | Awakened; 400K EP; 3 distinct boss types | +20 HP, +4 attack, +10% movement, +0.2 knockback resistance |
| Sovereign Hunter | Apex; 2M EP; 5 distinct boss types; one mastered apex Unique | +30 HP, +6 attack, +15% movement, +0.3 knockback resistance |

Bonuses replace the previous stage's bonuses. Boss credit is cumulative while
Human or a hunter and keyed by **entity type**, not individual spawn: ten Wither
kills count as one distinct boss. Progress survives death. Ordinary mobs do not
count. A kill must be attributed to you, including through your skills.

Eligible bosses are the Wither, Ender Dragon, and the bosses included in the
shared `c:bosses` tag (including Tensura's bosses). Add future Monster Hunter
entities through `data/wildhunt/tags/entity_type/progression_bosses.json` in a
datapack; no code changes are needed to extend the eligible list.

## Apex skills

All four Unique skills are **rollable through Tensura's normal reincarnation
system**. They join its configured primary skill pool without duplicate addon
entries. Rolls remain random; an apex skill is not guaranteed. Tensura's starting
skill count, additional-skill chances, and truly-unique rules still apply.
Ultimates are not starting rolls. Equip and cycle modes through Tensura's normal
ability interface and key bindings.

| Inspiration | Unique → Ultimate | Three modes |
| --- | --- | --- |
| Rey Dau | Storm Crown → Raijin, Lord of Thunder | Railbolt; Thunderclap; Storm Mantle |
| Uth Duna | Scarlet Deluge → Varuna, Lord of Tides | Crushing Current; Scarlet Undertow; Water Veil |
| Nu Udra | Black Flame → Agni, Lord of the Black Flame | Oil Lash; Blackflame Ignition; Furnace Mantle |
| Jin Dahaad | Frostbound Colossus → Ymir, Lord of Winter | Glacial Lance; Freezing Mist; Colossus Guard |

- Lightning deals 30% more damage to wet/drenched targets and grants a speed mantle.
- Water drenches and slows targets; undertow pulls inward; the veil grants absorption and water breathing.
- Oil Lash marks targets for ten seconds. Ignition consumes your own oil mark for double damage and burning. Furnace Mantle grants fire resistance and strength.
- Ice attacks build freezing and slow targets; Colossus Guard grants resistance.
- Ultimates strengthen all three modes and add a fourth: a ten-second elemental domain that pulses once per second.

Skills use Tensura elemental damage, resistances, aura, mastery, native storage,
and cooldowns. Attacks check teams, PvP settings, owned pets, creative/spectator
immunity, and line of sight. They do not destroy terrain.

Each Ultimate requires Sovereign Hunter, 2M base EP, and mastery of its matching
Unique. There are no skill trials. It replaces only that Unique. All four lines can
be earned and evolved.

```text
/wildhunt guide
/wildhunt status
/wildhunt evolve
/wildhunt ascend rey_dau
/wildhunt ascend uth_duna
/wildhunt ascend nu_udra
/wildhunt ascend jin_dahaad
```

Commands affect only the issuing player and enforce requirements. No operator
permission is needed. Actual Monster Hunter boss mobs are planned for later;
existing eligible bosses provide progression now. Balance values are currently
defined in Java.

### Updating from 0.1.0

Replace the old addon jar with 0.2.0; do not install both. Already learned skills
are retained. Old ordinary-hunt totals and trial flags do not grant boss credit
or gate skills, and boss kills before this update cannot be reconstructed.
New boss kills begin the distinct-boss record.

## Build and test

Install a Java 21 JDK and Python 3. From this checkout:

```sh
python3 scripts/fetch-runtime.py
./gradlew build
./gradlew runGameTestServer
```

The checksum-pinned Gradle wrapper fetches the build tool. Gradle obtains Tensura
and ManasCore from Curse Maven, and Architectury from its Maven repository.
ManasCore API modules are extracted into `build/` for compilation. The runtime
helper downloads pinned GeckoLib, SmartBrainLib and TerraBlender releases and
verifies published SHA-512 hashes. Dependency jars are not bundled in the addon.

`./gradlew test` runs progression unit tests. `runGameTestServer` runs native
registration, evolution/storage, casting/cooldown, ascension, boss deduplication,
reincarnation rolling, and death-persistence checks.
`./gradlew runClient` starts a development client on a machine with graphics.

Install `build/libs/tensura-wild-hunt-0.2.0.jar` on **both client and server**,
alongside Tensura and its required dependencies. The addon jar is not a complete
modpack. Additional development dependency versions are in `runtime-mods.lock.json`.

## Scope and credits

An early playable implementation with original simple icons and Minecraft
particles. Custom monster mobs, equipment, animations, and a custom progression
screen are not included. Client visuals and balance require in-game playtesting.

Unofficial fan project inspired by Capcom's Monster Hunter Wilds; not affiliated
with Capcom or ManasMods. No Monster Hunter or Tensura assets are redistributed.
Get Tensura from its [official page](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated).
The official [Human wiki page](https://tensura.wiki.gg/wiki/Races/Human) informed
the EP balancing reference.
