# Tensura Myths

An expanded progression and skills addon for **Tensura 2.0.1.3**, **Minecraft
1.21.1**, **NeoForge 21.1.209+**, and **Java 21**.

## Download

The built addon is included at [releases/tensura-myths-0.16.0.jar](releases/tensura-myths-0.16.0.jar).
Install it alongside Tensura and its required dependencies on both client and server.

## Skills

Eight Unique Skills are rollable through Tensura’s normal reincarnation pool. Equip active modes and toggle passives in its native Ability Selection menu.

| Unique | Role | Active arts |
| --- | --- | --- |
| [Storm Crown](wiki/pages/Storm-Crown.md) | Ricochet lightning and movement | Railbolt; Thunder Step |
| [Scarlet Deluge](wiki/pages/Scarlet-Deluge.md) | Water control and native barriers | Crushing Current; Scarlet Undertow |
| [Black Flame](wiki/pages/Black-Flame.md) | Tentacle pulls and owned oil pools | Oil Lash; Blackflame Ignition |
| [Frostbound Colossus](wiki/pages/Frostbound-Colossus.md) | Aimed ice impacts and capture | Avalanche Crash; Glacial Prison |
| [Threadweaver](wiki/pages/Threadweaver.md) | Fixed silk leash and spatial burst | Loom Needle; Sever Thread |
| [Rune Smith](wiki/pages/Rune-Smith.md) | Proximity trap and remote detonation | Runic Mine; Command Detonation |
| [Verdant Keeper](wiki/pages/Verdant-Keeper.md) | Allied HP/SHP grove and root control | Living Grove; Briar Grasp |
| [Mirrorbound](wiki/pages/Mirrorbound.md) | Splitting light and missile reflection | Prismatic Lance; Mirror Aegis |

**[Skill synthesis](wiki/pages/Skill-Synthesis.md)** combines two mastered Uniques at **2M base EP**, available to any race through **Skills → Ultimate → Synthesis**:

| Combination Ultimate | Mastered ingredients | New kit |
| --- | --- | --- |
| [Vritra, Lord of Cataclysm](wiki/pages/Vritra.md) | Storm Crown + Black Flame | Piercing plasma, delayed fire fuses, thunderhead furnace, advancing cataclysm |
| [Demeter, Lady of Sanctuary](wiki/pages/Demeter.md) | Verdant Keeper + Frostbound Colossus | Allied ice orchard, shared native barriers, dodgeable crystal blooms |
| [Janus, Lord of Crossroads](wiki/pages/Janus.md) | Threadweaver + Mirrorbound | Linked gates, opposing spatial blades, safe enemy displacement |

The result replaces both ingredients, preserves all nine presets and inherits the longest outstanding source cooldown. Unrelated skills remain learned. New Ultimates have three active arts and a separate toggle; their kits do not reuse their ingredients’ modes.

**[Godly Skills](wiki/pages/Godly.md)** appear in their own native skill-menu category while retaining Ultimate classification for compatibility. Master a combination Ultimate, become a True Hero or True Demon Lord and reach **12M base EP**. In **Skills → Godly → Ascension**, choose:

- **[Aion, God of Causality](wiki/pages/Aion.md):** smooth channelled Worldline Sever, Event Horizon, upward-only Causal Reversal and Genesis Verdict; recovering barriers, resistance/nullification suppression and proximity slowing.
- **[Thoth, God of Magic](wiki/pages/Thoth.md):** seven-element Lexicon, Spellbreak disruption/drain, a held, cooldown-free 2:1 aura-to-magicule channel and an autonomous orbiting Astral Library cycling the same seven elements. Also combines a mastered Rune Smith. Its passive strengthens elemental magic, native chanting and spell efficiency. Its original emissive seals, animated grimoires and chapter-colored spells require no shader pack.

Godly attacks and passives use **magicules**; held Mana Crucible trades aura for magicules. All skills use native damage sources, authority, status rules and resource storage. **Damage never scales with EP.** Every ability’s costs, cooldowns, damage types and limitations are in the [wiki ability index](wiki/pages/Abilities.md).

## Visuals

Every kit has its own textured effects: branching lightning, curled caustic water, molten tentacles and black flames, veined ice, braided silk, rune circuitry, living groves, faceted mirrors, a storm forge, crystal orchards, architectural gates and Aion’s astronomical clockwork. Thoth retains its layered inscriptions and animated grimoires. No shader pack is required. Native particle settings reduce decoration, distance reduces curve detail, and rendering batches materials per effect. [In-game gallery](wiki/pages/Visual-Effects.md).

## Human hunter progression

The hunter branch preserves the player’s appearance and appears alongside native Human evolution choices.

| Evolution | Requirements | Stage bonuses |
| --- | --- | --- |
| Awakened Hunter | Human; 100K base EP; 1 distinct boss type | +100 HP, +400 SHP, +2 attack, +5% movement, +0.1 knockback resistance |
| Apex Hunter | Awakened; 400K EP; 3 distinct boss types | +500 HP, +3,200 SHP, +4 attack, +10% movement, +0.2 knockback resistance |
| Sovereign Hunter | Apex; 2M EP; 5 distinct boss types; one mastered Myths Unique | +1,000 HP, +6,600 SHP, +6 attack, +15% movement, +0.3 knockback resistance |

Bonuses replace earlier stages. Boss credit is cumulative, survives death and is keyed by entity type. Repeated kills of one boss type count once. The Wither, Ender Dragon and shared `c:bosses` tag qualify; extend `data/wildhunt/tags/entity_type/progression_bosses.json` in a datapack.

Each stage grants an aura-only recovery Extra: **Hunter’s Recovery → Apex Renewal → Sovereign Rebirth**. Only the strongest enabled tier heals; idle recovery is free. [Recovery details](wiki/pages/Aura-Regeneration.md).

**[Alma](wiki/pages/Alma.md)** and **[Gemma](wiki/pages/Gemma.md)** spawn through Tensura’s native otherworlder distribution as Awakened Hunters with seven fitting native abilities each and bundled original skins.

## Duo Races and Convergent Awakening

[Duo Races](wiki/pages/Duo-Races.md) unlock at a final primary evolution, True Hero/True Demon Lord and **5M base EP**. Choose another native ancestral family using the **Secondary Race tab on the right of Tensura’s menu**. Half-strength shared attributes, intrinsic skills and a separately bound racial action complement the primary. Status shows **Primary Race/Secondary Race**.

Secondary evolution thresholds are fixed: **5M + 2.5M × stage** gives **7.5M, 10M, 12.5M, 15M…**. Native weighted requirements also apply. Your current EP, EP when selecting the race and saved past EP never raise a gate. “Be named” means receiving a Tensura name yourself, not naming other mobs.

Both lineages must finish evolving before [Convergent Awakening](wiki/pages/Convergent-Awakening.md), available in the next right-side tab. It requires **125% of the highest fixed preceding evolution gate**, with a **10M minimum**. A two-stage Slime secondary normally gives a **12.5M** awakening gate; a four-stage Daemon gives **18.75M**, unless a native lineage gate is higher. Server settings control the thresholds.

All **105 ancestral combinations** have a registered fusion race, name, icon and EP profile. Awakening grants a one-time 25% increase to base aura and magicule caps. **Concordance Pulse** restores 50% maximum HP and SHP without a fixed healing cap, costs 1,500 aura and has a 30-second cooldown.

An independent secondary grants **10% extra native EP gain**, retaining the primary race’s aura/magicule proportions. Default Human/Hunter awards of 6 aura / 3 magicules become **6.6 / 3.3**. Fusion inherits the same bonus with its own growth profile. Human/Hunter retains the normal **2/3 aura, 1/3 magicule** split.

## Updating

Replace the previous addon JAR with **0.16.0** on client and server. Install only one addon version. The `wildhunt` namespace remains stable for existing saves.

Ordinary elemental attribute bonuses use the strongest source per element; Godly bonuses remain separate. Mirrorbound, Demeter, Janus and Thoth no longer give passive shields; old saved contributions are cleaned automatically. Water Veil, Colossus Carapace, active Crystal Bulwark and Aion retain their dedicated defenses.

The four earlier Ultimates are retired from gameplay. Existing **Raijin/Agni** ownership migrates into **Vritra**, and **Varuna/Ymir** into **Demeter**. Mastery fraction, longest outstanding cooldown and all preset references are preserved; duplicate replacement ownership merges without downgrading the destination. Old passive/barrier contributions are removed. Existing Aion, Uniques, races and boss histories remain valid. Old selection-EP snapshots are ignored by the new fixed gates.

The [changelog](docs/CHANGELOG.md) records development changes separately from the gameplay wiki.

## Wiki

Read the [Tensura Myths Wiki](wiki/README.md) for individual skill/race articles, per-ability damage types, progression and commands. [Download the styled offline edition](releases/tensura-myths-wiki-0.16.0.zip), extract it and open `index.html` for the wiki layout, infoboxes, screenshots and offline search. The [GitHub Wiki edition](wiki/github/Home.md) is prepared for the separate Wiki when it is available; [publishing instructions](docs/WIKI.md).

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
boss history, ascension, all four apex attacks and toggles, barrier attributes,
legacy-save migration, ricochets, collision-safe dashes, travelling waves,
oil pools, tentacle lifetime, native Frost capture, dodgeable icefall marks,
vertical dashes, through-wall ricochets, momentum preservation, native shield
breakage and timed recovery, HP/SHP evolution, aura regeneration and native NPC loadouts.
`./gradlew runClient` starts a development client on a machine with graphics.

The [0.16.0 validation report](docs/VALIDATION-0.16.0.md) covers 108 native GameTests, 11 JUnit tests, real-client visual checks and responsive wiki validation.

Install `build/libs/tensura-myths-0.16.0.jar` on **both client and server**,
alongside Tensura and its required dependencies. The addon jar is not a complete
modpack. Additional development dependency versions are in `runtime-mods.lock.json`.

## Scope and credits

An early playable implementation with original 64×64 pixel-art monster portraits
and distinct elemental effects built with Tensura’s native APIs plus procedural
tentacle and ice meshes. Custom monster mobs,
equipment and animated monster models are not included.
Client visuals and balance require in-game playtesting.

The eight additional skill sigils are in `art/mythic-sigils.png`; export their
centered, padded textures with `python3 scripts/prepare-mythic-icons.py` (Pillow).
Portrait source art is in `art/apex-portraits.png`; optional regeneration uses
`python3 scripts/prepare-icons.py` with Pillow installed. Normal builds do not
need Pillow. Rebuild the optional offline wiki with
`python3 -m pip install Markdown==3.7` followed by `python3 scripts/build-wiki.py`.

Unofficial fan project inspired by Capcom's Monster Hunter Wilds; not affiliated
with Capcom or ManasMods. No Monster Hunter or Tensura assets are redistributed.
Get Tensura from its [official page](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated).
The official [Human wiki page](https://tensura.wiki.gg/wiki/Races/Human) informed
the EP balancing reference.
