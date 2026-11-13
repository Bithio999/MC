# TensuraAdditionalAdditions

An independent **Tensura: Reincarnated** addon for **Minecraft 1.21.1 / NeoForge**. It focuses on Heavenly Restriction, Battlewills and world travel.

[Download 1.27.0](../../releases/TensuraAdditionalAdditions-1.27.0.jar) · [Wiki](../../wiki/pages/TensuraAdditionalAdditions.md) · [Gameplay guide](docs/GUIDE.md) · [Validation](docs/VALIDATION-1.27.0.md)


**New in 1.27.0:** [Laplace, Lord of Equations](../../wiki/pages/Additions-Laplace.md) combines Exponentiation and Mathematician. Its two abilities are Universal Formula and Equation Editor, with live native/edited values, linked equations and a parameter palette.

## Content

- **Tyche, Lord of Fortune**, an Ultimate synthesized from Critical Merchant + Tuner. Loaded Dice, Guaranteed Dividend, a timed low-HP/SHP buff and damage-type-based full revival with a five-minute history window. [Skill guide](../../wiki/pages/Additions-Tyche.md).

- **Archive of Accord**, a rare Overworld library housing Elias. Choose one permanent Unique Skill per player for a configurable permanent EP cost and one Chromatic Orb. [Mentorship guide](../../wiki/pages/Additions-Archive-of-Accord.md).

- **Metis, Lord of Wisdom**, an Ultimate synthesized from Split Mind + Great Sage. Three/four parallel magic/skill/art threads with menu-mode exclusion, automatic circle analysis and native Great Sage utilities. [Skill guide](../../wiki/pages/Additions-Metis.md).
- **Hephaestus, Lord of the Forge**, an Ultimate synthesized from Prospector + Godly Craftsman. Divine Forge, Terraprisma-style Living Arsenal and one Magnum Opus; no EP gate. [Skill guide](../../wiki/pages/Additions-Hephaestus.md).

- **Critical Merchant**, compounding critical damage and ten guaranteed critical hits. [Skill guide](../../wiki/pages/Additions-Critical-Merchant.md).

- **Marauder’s Map**, a detailed dimension atlas with shared exploration, cached progressive terrain, sharp zoomed-out surfaces, live players, automatic structure discovery, contextual menus and private pins. [Map guide](../../wiki/pages/Additions-Marauders-Map.md).

- **Split Mind**, a Unique Skill with +9 learning/mastery gain, Thought Acceleration, Chant Annulment and automatic casting of an assigned learned magic. [Skill guide](../../wiki/pages/Additions-Split-Mind.md).

- **The High Realm**, a 5-million-EP gated dimension with three biomes, elite ordinary mobs and a protected return sanctum. [Gate and travel guide](../../wiki/pages/Additions-The-High-Realm.md).
- **Chromatic Orb**, a one-time engraving enhancement crafted from High Realm shards and Kael’s Starless Core. [Crafting and use](../../wiki/pages/Additions-Chromatic-Orb.md).
- **Vesper**, Kael’s unique sword, with Afterwind’s wind-to-melee combo. [Weapon guide](../../wiki/pages/Additions-Vesper.md).
- **Kael Veyr**, a rare 15M-EP otherworlder boss with native sword arts, wind magic and an original skin. [Encounter guide](../../wiki/pages/Additions-Kael-Veyr.md).
- **Fettered Human** and four evolutions ending in **Heavenbreaker**.
- **Chained**, its automatic Ultimate evolution **Unbound**, and three aura recovery skills.
- **Schema**, **Air Walk** and **Instant Transmission** Battlewills.
- **Inverted Spear of Heaven**, with its Nullification engraving.
- **Exponentiation**, an in-slot Unique that raises supported beneficial stats and outputs to ^1.2, excluding movement.
- **Prospector**, faster mining, stacking Fortune, subtle ore vision and native refining.
- **Loan Shark**, skill lending with retained mastery, EP income and a searchable claim ledger.
- **Fusion**, temporary body and skill sharing with subordinates or consenting players.
- **Parallel World Jumper**, including World Jump and the searchable World Select menu.

World travel keeps your character and progression in a hidden library of up to **128 worlds including the active world**. Only the active save appears in Singleplayer. **Deleting that save deletes the entire library.** Recorded older travel saves migrate on the next jump. [World travel guide](../../wiki/pages/Additions-Parallel-World-Jumper.md).

Press **F8** for the in-game guide, or open it from the pause/status menus. [Download the styled wiki](../../releases/tensura-addon-wikis.zip).

## Install

Use **Java 21**, **Minecraft 1.21.1** and **NeoForge 21.1.209+**. Install:

1. **Tensura: Reincarnated 2.0.1.3 or 2.0.1.4** and its required libraries: ManasCore 4.0.0.2, Architectury 13.0.8+, SmartBrainLib and TerraBlender.
2. **GeckoLib 4.9.2+**, within the 4.x series.
3. **TensuraAdditionalAdditions-1.27.0.jar**.

Install the addon on both client and server for multiplayer. Parallel World Jumper works only in private single-player. Use this addon **instead of Tensura Myths**; the loader rejects installing both.

Supported modpack: **[Tensura Neo Otherworld 1.1.3a](docs/compatibility/NEO-OTHERWORLD-1.1.3a.md)**. Choose **Fettered Human** for this addon; Mysticism’s Restricted Human is a separate lineage.

## Existing worlds

Replace the previous standalone JAR with the current release. The `heavenlyrestriction` mod ID, registry IDs and saved-data/configuration names are retained for existing standalone worlds. This repository folder rename does not require a new world.

Myths worlds use the separate `wildhunt` namespace and are not automatically converted into standalone worlds. Keep the matching addon installed for each world.

## Development

This folder is a complete Gradle project; Tensura Myths is not required to build it.

```sh
cd standalone/tensura-additional-additions
python3 scripts/fetch-runtime.py
./gradlew build
```

Use **Java 21**. Output: `build/libs/TensuraAdditionalAdditions-1.27.0.jar`.

- `./gradlew runClient` — development client.
- `./gradlew runGameTestServer` — native integration checks.
- `python3 scripts/verify-distribution.py` — audit the independent release package.

Optional fixtures in `docs/` are excluded from normal builds. Runtime dependency hashes are pinned in `runtime-mods.lock.json`. Original recovery-model source: [vital_corolla.bbmodel](art/blockbench/vital_corolla.bbmodel).
