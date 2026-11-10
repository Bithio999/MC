# Tensura Addons

Three independent addon projects for **Tensura: Reincarnated**, **Minecraft 1.21.1** and **NeoForge**.

| Mod | Features | Download | Wiki |
| --- | --- | --- | --- |
| **Tensura Myths** | Hunter and Heavenly Restriction races, Unique/Ultimate/Godly skills, dual races, Myth bosses and otherworlders | [0.36.1 JAR](releases/tensura-myths-0.36.1.jar) | [Myths wiki](wiki/pages/Tensura-Myths.md) |
| **TensuraAdditionalAdditions** | Heavenly Restriction, Chained/Unbound, Battlewills, Inverted Spear of Heaven, Prospector, Exponentiation, Loan Shark, Critical Merchant, Hephaestus, Split Mind, Marauder’s Map, Fusion, Parallel World Jumper, The High Realm, Chromatic Orb and Vesper | [1.22.4 JAR](releases/TensuraAdditionalAdditions-1.22.4.jar) | [Additions wiki](wiki/pages/TensuraAdditionalAdditions.md) |


## Install

Use **Java 21**. Add the chosen JAR to your instance’s `mods` folder alongside Tensura and its required libraries, including ManasCore, Architectury, SmartBrainLib and TerraBlender. All three addons require GeckoLib 4.x.

| Addon | Supported Tensura | GeckoLib |
| --- | --- | --- |
| Tensura Myths | 2.0.1.3 | 4.9.3 |
| TensuraAdditionalAdditions | 2.0.1.3 or 2.0.1.4 | 4.9.2+ |

For multiplayer, install the same addon on client and server. **Parallel World Jumper is private single-player only.** The standalone supports [Tensura Neo Otherworld 1.1.3a](standalone/tensura-additional-additions/docs/compatibility/NEO-OTHERWORLD-1.1.3a.md).

## Guides and previews

Myths and Additions: press **F8** in-game, use the pause-menu guide button, or choose **Guide** below Tensura’s status panel. Each guide covers its installed addon.

[Choose your wiki](wiki/README.md) · [Download the styled wikis](releases/tensura-addon-wikis.zip). Extract the ZIP and open `index.html` for separate navigation, search, pictures and videos.

- [Eschaton and Aion](wiki/media/eschaton-aion-anime-preview.mp4)
- [Counter against Eschaton](docs/media/counter-preview/README.md)
- [World Hop transition](docs/media/world-hop-preview/README.md) — recorded on 1.3.0; current world travel uses a hidden library.

## Repository layout

| Location | Contents |
| --- | --- |
| [`src/`](src) | Tensura Myths source |
| [`standalone/tensura-additional-additions/`](standalone/tensura-additional-additions) | Independent TensuraAdditionalAdditions project |
| [`wiki/`](wiki/README.md) | Gameplay wikis and their styled edition |
| [`releases/`](releases) | Built mods and offline wiki |
| [`docs/`](docs) | Validation, development notes and research |
| [`art/`](art) | Original artwork and editable models |

## Build

With Java 21 and Python 3, run these commands from the project you want to build:

```sh
# Tensura Myths: repository root
# TensuraAdditionalAdditions: cd standalone/tensura-additional-additions
python3 scripts/fetch-runtime.py
./gradlew build
```

`./gradlew runClient` starts a development client; `./gradlew runGameTestServer` runs native integration checks. Build output is in that project’s `build/libs/` folder. [Standalone development details](standalone/tensura-additional-additions/README.md#development).

[Myths validation](docs/VALIDATION-0.36.1.md) · [Additions validation](standalone/tensura-additional-additions/docs/VALIDATION-1.22.4.md) · [Changelog](docs/CHANGELOG.md) · [Wiki authoring](docs/WIKI.md).

Unofficial fan project, not affiliated with ManasMods or Capcom. Get Tensura from its [official page](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated).
