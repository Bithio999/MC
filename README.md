# Tensura: Wild Hunt

A Monster Hunter Wilds inspired addon for **Tensura 2.0.1.3**, **Minecraft
1.21.1**, **NeoForge 21.1.209+**, and **Java 21**.

## Download

The built addon is included at [releases/tensura-wild-hunt-0.6.0.jar](releases/tensura-wild-hunt-0.6.0.jar).
Install it alongside Tensura and its required dependencies on both client and server.

## Human hunter progression

Your appearance stays human. The new branch appears alongside Human's existing
choices in Tensura's evolution menu.

| Evolution | Requirements | Bonuses at this stage |
| --- | --- | --- |
| Awakened Hunter | Human; 100K base EP; 1 distinct boss type | +40 HP, +20 SHP, +2 attack, +5% movement, +0.1 knockback resistance |
| Apex Hunter | Awakened; 400K EP; 3 distinct boss types | +100 HP, +60 SHP, +4 attack, +10% movement, +0.2 knockback resistance |
| Sovereign Hunter | Apex; 2M EP; 5 distinct boss types; one mastered apex Unique | +200 HP, +120 SHP, +6 attack, +15% movement, +0.3 knockback resistance |

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

| Inspiration | Unique → Ultimate | Two active arts | Toggleable passive |
| --- | --- | --- | --- |
| Rey Dau | Storm Crown → Raijin, Lord of Thunder | Railbolt; Thunder Step | Storm Mantle |
| Uth Duna | Scarlet Deluge → Varuna, Lord of Tides | Crushing Current; Scarlet Undertow | Water Veil |
| Nu Udra | Black Flame → Agni, Lord of the Black Flame | Oil Lash; Blackflame Ignition | Furnace Mantle |
| Jin Dahaad | Frostbound Colossus → Ymir, Lord of Winter | Avalanche Crash; Glacial Prison | Colossus Carapace |

- **Rey Dau:** Railbolt ricochets through up to four enemies (six as Raijin), with
  twelve-block hops. Thunder Step follows your full aim, including up and down,
  while checking body collisions and build-height limits.
- **Uth Duna:** moving waves and aimed whirlpools control enemies. Water Veil
  uses Tensura’s native physical and magic barriers and retaliates after health
  damage. Water and ice passives also arm native Multilayer Barriers for Tensura’s
  shield HUD counter. They add no absorption hearts.
- **Nu Udra:** Oil Lash draws solid animated tentacles that extend, curl and
  recoil, including on a miss. Pull enemies onto owned pools, then ignite them.
- **Jin Dahaad:** a completely rebuilt kit: aimed avalanche impacts, single-target
  ice capture, a native barrier carapace, and Ymir’s telegraphed falling-ice siege.

**Charge, oil stacks, rime stacks and addon EP damage scaling are removed.**
Mastery adds up to 50% damage; Ultimates retain their 3× base multiplier. Native
Tensura combat rules and elemental modifiers still apply. EP progression gates
remain in place.

Passives are separate native skill toggles in Tensura’s **Ability Selection**,
not the last active mode. They stay enabled while funded, charging 10/10/15/20
aura per second for the four Unique lines (4× as an Ultimate). Turning a passive
off immediately removes its attribute contributions. Uniques have two active
modes; Ultimates add a third signature: Heaven’s Thunder, Crimson Ocean,
Black Sun Caldera, or Heavenfall Glacier.

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

Replace the old addon jar with 0.6.0; do not install both. Learned skills,
mastery, distinct boss history and native passive toggle state are retained.
Existing hunters get the stronger HP/SHP caps on login without free healing.
Already-enabled water/ice passives receive their initial native HUD barrier.
Intact barriers survive relogs; broken barriers are not automatically repaired.
Rearm them by turning the passive off and back on. If your HUD is customized,
ensure Tensura’s **Barrier** decoration is enabled.

When upgrading from a version before 0.5.0, the earlier stack/mode migration
also runs. Rerolling is not required. Centered portraits are unchanged.

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

`./gradlew test` checks progression, mastery-only damage multipliers and costs.
`runGameTestServer` checks native registration/evolution, reincarnation rolls,
boss history, ascension, all eight primary attacks and toggles, barrier attributes,
legacy-save migration, ricochets, collision-safe dashes, travelling waves,
oil pools, tentacle lifetime, native Frost capture, dodgeable icefall marks,
vertical dashes, extended ricochets, native shield breakage, and HP/SHP evolution.
`./gradlew runClient` starts a development client on a machine with graphics.

Install `build/libs/tensura-wild-hunt-0.6.0.jar` on **both client and server**,
alongside Tensura and its required dependencies. The addon jar is not a complete
modpack. Additional development dependency versions are in `runtime-mods.lock.json`.

## Scope and credits

An early playable implementation with original 64×64 pixel-art monster portraits
and distinct elemental effects built with Tensura’s native APIs plus procedural
tentacle and ice meshes. Custom monster mobs,
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
