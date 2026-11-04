# TensuraAdditionalAdditions

An independent **Tensura: Reincarnated** addon for **Minecraft 1.21.1 / NeoForge**. It focuses on Heavenly Restriction, Battlewills and world travel.

[Download 1.11.4](../../releases/TensuraAdditionalAdditions-1.11.4.jar) · [Wiki](../../wiki/pages/TensuraAdditionalAdditions.md) · [Gameplay guide](docs/GUIDE.md) · [Validation](docs/VALIDATION-1.11.4.md)

## Content

- **Split Mind**, a Unique Skill with +9 learning/mastery gain, Thought Acceleration, Chant Annulment and automatic casting of an assigned learned magic. [Skill guide](../../wiki/pages/Additions-Split-Mind.md).

- **The High Realm**, a 5-million-EP gated dimension with three biomes, elite ordinary mobs and a protected return sanctum. [Gate and travel guide](../../wiki/pages/Additions-The-High-Realm.md).
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
3. **TensuraAdditionalAdditions-1.11.4.jar**.

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

Use **Java 21**. Output: `build/libs/TensuraAdditionalAdditions-1.11.4.jar`.

- `./gradlew runClient` — development client.
- `./gradlew runGameTestServer` — native integration checks.
- `python3 scripts/verify-distribution.py` — audit the independent release package.

Optional fixtures in `docs/` are excluded from normal builds. Runtime dependency hashes are pinned in `runtime-mods.lock.json`. Original recovery-model source: [vital_corolla.bbmodel](art/blockbench/vital_corolla.bbmodel).
