# Tensura: Wild Hunt

A Monster Hunter Wilds inspired addon for **Tensura 2.0.1.3**, **Minecraft
1.21.1**, **NeoForge 21.1.209+**, and **Java 21**.

## Download

The built addon is included at [releases/tensura-wild-hunt-0.4.0.jar](releases/tensura-wild-hunt-0.4.0.jar).
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
| Rey Dau | Storm Crown → Raijin, Lord of Thunder | Railbolt; Fulgurite Array; Storm Mantle |
| Uth Duna | Scarlet Deluge → Varuna, Lord of Tides | Crushing Current; Scarlet Undertow; Water Veil |
| Nu Udra | Black Flame → Agni, Lord of the Black Flame | Oil Lash; Blackflame Ignition; Furnace Mantle |
| Jin Dahaad | Frostbound Colossus → Ymir, Lord of Winter | Glacial Rupture; Glacial Rampart; Colossus Guard |

- **Rey Dau:** piercing railbolts spend a three-charge battery. Three fulgurite
  rods select nearby enemies to recharge it; moving during Storm Mantle also
  builds charge. Raijin adds targeted sky judgments.
- **Uth Duna:** a travelling wave carries enemies through an aimed whirlpool.
  Water Veil retaliates after health damage. Varuna alternates inward and
  outward tides around the caster.
- **Nu Udra:** curved tentacle lashes pull and oil enemies, leaving limited owned
  pools. Ignition detonates those locations; Furnace Mantle coats melee victims.
  Agni establishes a five-pool caldera.
- **Jin Dahaad:** ground fissures consume rime for shatter damage. Glacial Rampart
  intercepts a finite number of hostile projectiles. Ymir adds an expanding,
  anchored frostfront that primes each reached enemy once.

Damage grows with EP and mastery. Unique primaries take 3–4 seconds, secondary
arts 7–8 seconds, and mantles 18–20 seconds. Ultimates triple base damage,
strengthen each art, and add its fourth signature mode.

Read the [Wild Hunt Wiki](wiki/README.md) for all eight skill pages, exact costs,
combos, race infoboxes, commands and mechanics. An offline visual edition is
included at `wiki/index.html`; open it after downloading the repository. The
[earlier balance comparison](docs/BALANCE.md) preserves the Tensura benchmarks.

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

### Updating from an earlier release

Replace the old addon jar with 0.4.0; do not install both. Learned skills, mastery,
and recorded distinct bosses are retained. Old ordinary-hunt totals and trial
flags do not grant boss credit or gate skills. Login corrects oversized legacy
cooldowns and cancels obsolete transient fields; rerolling is not required.

Version 0.4.0 centers the existing portraits, separates every art’s combat loop,
and uses Tensura’s native particles and animated elemental magic circles.

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
reincarnation rolling, death persistence, all eight primary attacks, combo damage,
mantle cleanup, domain pulse timing, charge consumption, travelling waves, oil
pool placement, projectile wards, expanding frostfronts and reactive mantles.
`./gradlew runClient` starts a development client on a machine with graphics.

Install `build/libs/tensura-wild-hunt-0.4.0.jar` on **both client and server**,
alongside Tensura and its required dependencies. The addon jar is not a complete
modpack. Additional development dependency versions are in `runtime-mods.lock.json`.

## Scope and credits

An early playable implementation with original 64×64 pixel-art monster portraits
and distinct elemental effects built with Tensura’s native APIs. Custom monster mobs,
equipment, animated 3D models, and a custom progression screen are not included.
Client visuals and balance require in-game playtesting.

Portrait source art is in `art/apex-portraits.png`; optional regeneration uses
`python3 scripts/prepare-icons.py` with Pillow installed. Normal builds do not
need Pillow. Rebuild the optional offline wiki with
`python3 -m pip install Markdown==3.7` followed by `python3 scripts/build-wiki.py`.

Unofficial fan project inspired by Capcom's Monster Hunter Wilds; not affiliated
with Capcom or ManasMods. No Monster Hunter or Tensura assets are redistributed.
Get Tensura from its [official page](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated).
The official [Human wiki page](https://tensura.wiki.gg/wiki/Races/Human) informed
the EP balancing reference.
