# Tensura: Wild Hunt

A Monster Hunter Wilds inspired addon for **Tensura 2.0.1.3**, **Minecraft
1.21.1**, **NeoForge 21.1.209+**, and **Java 21**.

## Download

The built addon is included at [releases/tensura-wild-hunt-0.1.0.jar](releases/tensura-wild-hunt-0.1.0.jar).
Install it alongside Tensura and its required dependencies on both client and server.

## Human hunter progression

Your appearance stays human. The new branch appears alongside Human's existing
choices in Tensura's evolution menu.

| Evolution | Requirements | Bonuses at this stage |
| --- | --- | --- |
| Awakened Hunter | Human; 100K base EP; 20 hostile hunts | +10 HP, +2 attack, +5% movement, +0.1 knockback resistance |
| Apex Hunter | Awakened; 400K EP; 100 hunts; four trials | +20 HP, +4 attack, +10% movement, +0.2 knockback resistance |
| Sovereign Hunter | Apex; 2M EP; 250 hunts; four trials; one mastered apex Unique | +30 HP, +6 attack, +15% movement, +0.3 knockback resistance |

Bonuses replace the previous stage's bonuses. Player-attributed hostile kills
count while Human or a hunter. Trial kills count only after becoming Awakened.
Progress survives death.

## Apex skills

Each trial unlocks a native Tensura Unique skill. Equip and cycle its modes
through Tensura's normal ability interface and key bindings.

| Inspiration | Unique → Ultimate | Three modes | Trial |
| --- | --- | --- | --- |
| Rey Dau | Storm Crown → Raijin, Lord of Thunder | Railbolt; Thunderclap; Storm Mantle | 10 phantoms |
| Uth Duna | Scarlet Deluge → Varuna, Lord of Tides | Crushing Current; Scarlet Undertow; Water Veil | 10 guardians (elders count) |
| Nu Udra | Black Flame → Agni, Lord of the Black Flame | Oil Lash; Blackflame Ignition; Furnace Mantle | 10 blazes |
| Jin Dahaad | Frostbound Colossus → Ymir, Lord of Winter | Glacial Lance; Freezing Mist; Colossus Guard | 10 strays |

- Lightning deals 30% more damage to wet/drenched targets and grants a speed mantle.
- Water drenches and slows targets; undertow pulls inward; the veil grants absorption and water breathing.
- Oil Lash marks targets for ten seconds. Ignition consumes your own oil mark for double damage and burning. Furnace Mantle grants fire resistance and strength.
- Ice attacks build freezing and slow targets; Colossus Guard grants resistance.
- Ultimates strengthen all three modes and add a fourth: a ten-second elemental domain that pulses once per second.

Skills use Tensura elemental damage, resistances, aura, mastery, native storage,
and cooldowns. Attacks check teams, PvP settings, owned pets, creative/spectator
immunity, and line of sight. They do not destroy terrain.

Each Ultimate requires Sovereign Hunter, 2M base EP, its completed trial, and
mastery of its matching Unique. It replaces only that Unique. All four lines can
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
permission is needed. Trials use themed existing enemies, not new Monster Hunter
boss mobs. Datapacks can change `data/wildhunt/tags/entity_type/trials/<apex>.json`.
Trial enemies must implement Minecraft's `Enemy` interface. Balance values are
currently defined in Java.

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
registration, evolution/storage, casting/cooldown, and ascension checks.
`./gradlew runClient` starts a development client on a machine with graphics.

Install `build/libs/tensura-wild-hunt-0.1.0.jar` on **both client and server**,
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
