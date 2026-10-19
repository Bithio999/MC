# Tensura Myths

An expanded progression and skills addon for **Tensura 2.0.1.3**, **Minecraft
1.21.1**, **NeoForge 21.1.209+**, and **Java 21**.

## Download

The built addon is included at [releases/tensura-myths-0.11.0.jar](releases/tensura-myths-0.11.0.jar).
Install it alongside Tensura and its required dependencies on both client and server.

## Godly skills and Duo Races

**[Aion, God of Causality](wiki/pages/Aion.md)** introduces the **Godly** tier: a separate category in Tensura’s skill menu that retains native Ultimate classification. Master any one of this addon’s four Ultimates, become a True Hero or True Demon Lord, and reach **12M base EP** to ascend through **Skills → Godly → Ascension**. Aion has four spatial/causal arts and a magicule-powered barrier, resistance/nullification suppression and a proximity aura that slows enemies and hostile projectiles. Ascension replaces the chosen Ultimate and preserves presets and cooldowns.

**[Duo Races](wiki/pages/Duo-Races.md)** unlock at a final primary evolution, True Hero/True Demon Lord and **5M base EP**. Choose a permanent secondary ancestry from the other native race families and evolve it through native requirements plus 2.5M additional base EP per stage. Use the **right-side Secondary Race and Convergent Awakening tabs** in Tensura’s menu. Its half-strength attributes, learned intrinsic skills and racial reactions complement the primary body. Assign **Secondary racial ability** in Controls to use its native press/hold/release action.

After **both races reach their final evolution**, **[Convergent Awakening](wiki/pages/Convergent-Awakening.md)** requires **125% of the highest preceding evolution EP gate, with a 10M base EP minimum**. All **105 combinations** among the 15 native ancestral families have a named, registered fusion race, original icon and distinct EP growth profile. The awakening grants a one-time 25% increase to both base energy pools. Thresholds and the bonus are server configurable.

A secondary grants **+10% EP gain** while preserving the primary race’s aura/magicule split. Default Human/Hunter awards of 6 aura / 3 magicules become **6.6 aura / 3.3 magicules**. The awakened race keeps its unique growth profile and inherits the same 10% bonus. Status displays independent lineages as **Primary Race/Secondary Race**.

The display name is now **Tensura Myths**. All previous hunter content remains; the `wildhunt` namespace is preserved for existing saves and datapacks.

## Human hunter progression

Your appearance stays human. The new branch appears alongside Human's existing
choices in Tensura's evolution menu.

| Evolution | Requirements | Bonuses at this stage |
| --- | --- | --- |
| Awakened Hunter | Human; 100K base EP; 1 distinct boss type | +100 HP, +400 SHP, +2 attack, +5% movement, +0.1 knockback resistance |
| Apex Hunter | Awakened; 400K EP; 3 distinct boss types | +500 HP, +3,200 SHP, +4 attack, +10% movement, +0.2 knockback resistance |
| Sovereign Hunter | Apex; 2M EP; 5 distinct boss types; one mastered apex Unique | +1,000 HP, +6,600 SHP, +6 attack, +15% movement, +0.3 knockback resistance |

Bonuses replace the previous stage's bonuses. Boss credit is cumulative while
Human or a hunter and keyed by **entity type**, not individual spawn: ten Wither
kills count as one distinct boss. Progress survives death. Ordinary mobs do not
count. A kill must be attributed to you, including through your skills.

Eligible bosses are the Wither, Ender Dragon, and the bosses included in the
shared `c:bosses` tag (including Tensura's bosses). Add future Monster Hunter
entities through `data/wildhunt/tags/entity_type/progression_bosses.json` in a
datapack; no code changes are needed to extend the eligible list.

## Aura recovery and otherworlders

Each race stage grants a new native Extra Skill: **Hunter's Recovery → Apex Renewal → Sovereign Rebirth**. They mirror self, ultraspeed and infinite regeneration using **aura only**. Only the strongest toggled tier heals; idle recovery is free. Hunter base energy and normal default-alignment EP gains retain the **2/3 aura, 1/3 magicule** split. [Recovery costs and limits](wiki/pages/Aura-Regeneration.md).

**[Alma](wiki/pages/Alma.md)** and **[Gemma](wiki/pages/Gemma.md)** spawn through Tensura's native otherworlder distribution as Awakened Hunters, with seven fitting Tensura abilities each. They inherit native humanoid/subordinate behavior and use bundled original skins based on online character references. Spawn directly with `/summon wildhunt:alma` or `/summon wildhunt:gemma`; spawn eggs are available through `/give`.

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
  twelve-block hops through walls after the block-clipped first hit. Thunder Step follows your full aim, including up and down,
  while checking body collisions and build-height limits. Range is 16/24 blocks, cooldown 5/4 seconds, and momentum is preserved.
- **Uth Duna:** moving waves and aimed whirlpools control enemies. Water Veil
  uses Tensura’s native physical and magic barriers and retaliates after health
  damage. Water and ice passives also arm native Multilayer Barriers for Tensura’s
  shield HUD counter. They add no absorption hearts.
- **Nu Udra:** Oil Lash draws solid animated tentacles that extend, curl and
  recoil, including on a miss. Pull enemies onto owned pools, then ignite them.
- **Jin Dahaad:** a completely rebuilt kit: aimed avalanche impacts, single-target
  ice capture, a native barrier carapace, and Ymir’s telegraphed falling-ice siege.

**Charge, oil stacks, rime stacks and addon EP damage scaling are removed.**
Version 0.7.0 reduces all raw art damage by 10%. Mastery adds up to 50% damage; Ultimates retain their 3× base multiplier. Native
Tensura combat rules and elemental modifiers still apply. EP progression gates
remain in place.

Passives are separate native skill toggles in Tensura’s **Ability Selection**,
not the last active mode. They stay enabled while funded, charging 10/10/15/20
aura per second for the four Unique lines (4× as an Ultimate). Turning a passive
off immediately removes its attribute contributions. Uniques have two active
modes; Ultimates add a third signature: Heaven’s Thunder, Crimson Ocean,
Black Sun Caldera, or Heavenfall Glacier.

Read the [Tensura Myths Wiki](wiki/README.md) for skill pages, exact costs,
combos, race infoboxes, commands and mechanics. The [GitHub Wiki edition](wiki/github/Home.md)
includes the prepared Home, articles, sidebar, and footer. See [publishing instructions](docs/WIKI.md)
for syncing these to the separate [GitHub Wiki](https://github.com/Bithio999/MC/wiki).
**[Download the styled offline wiki](releases/tensura-myths-wiki-0.11.0.zip)**, extract
the ZIP, and open `index.html`. It includes a wiki-style portal, mobile navigation,
infoboxes, and offline search. GitHub shows HTML source; download the edition to
see its styling. It is also included at `wiki/index.html` in the repository. The
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

Replace the old addon jar with 0.11.0; do not install both. Learned skills,
mastery, distinct boss history and native passive toggle state are retained.
Secondary lineages retain their recorded selection EP and stage. Saves predating
0.10.0 initialize the fresh-growth origin at the first progression check.
Aion uses magicules, including passive upkeep. Causal Reversal records aura and magicules
**after** its initial cast payment, returns for free, and automatically returns at timeout.
Convergent Awakening now uses a lineage-dependent EP gate; already fused races remain fused.
Existing hunters get the stronger HP/SHP caps on login without free healing.
Already-enabled water/ice passives receive their initial native HUD barrier.
Intact barriers survive relogs; broken barriers recover automatically after ten
seconds without a hit while the toggle stays enabled and funded. If your HUD is customized,
ensure Tensura’s **Barrier** decoration is enabled.

When upgrading from a version before 0.5.0, the earlier stack/mode migration
also runs. Rerolling is not required. Portraits now keep Rey Dau’s full horns within their own centered tile.

## Wiki reference

The [wiki](wiki/README.md) organizes all gameplay reference by [skill tier](wiki/pages/Skills.md), [race ancestry](wiki/pages/Races.md), and [otherworlder](wiki/pages/Otherworlders.md). The [ability index](wiki/pages/Abilities.md) lists all 37 active/passive entries with damage types; [Damage types](wiki/pages/Damage-Types.md) separates direct hits, status damage and non-damaging utility.

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
vertical dashes, through-wall ricochets, momentum preservation, native shield
breakage and timed recovery, HP/SHP evolution, aura regeneration and native NPC loadouts.
`./gradlew runClient` starts a development client on a machine with graphics.

The [0.11.0 validation report](docs/VALIDATION-0.11.0.md) covers the Godly/Duo additions, all 64 native GameTests and the actual client checks.

Install `build/libs/tensura-myths-0.11.0.jar` on **both client and server**,
alongside Tensura and its required dependencies. The addon jar is not a complete
modpack. Additional development dependency versions are in `runtime-mods.lock.json`.

## Scope and credits

An early playable implementation with original 64×64 pixel-art monster portraits
and distinct elemental effects built with Tensura’s native APIs plus procedural
tentacle and ice meshes. Custom monster mobs,
equipment and animated monster models are not included.
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

Water and ice toggles no longer emit constant custom particles. Their physical/magic barrier bonuses are reduced 10%; broken native multilayer barriers rearm automatically after ten seconds without a hit while the toggle stays funded. Undertow is placed along the crosshair ray within 24/36 blocks and renders rotating teal/coral water ribbons.
