# TensuraAdditionalAdditions

An active addon for **Tensura: Reincarnated**, **Minecraft 1.21.1** and **NeoForge**.

[Download 1.29.0](releases/TensuraAdditionalAdditions-1.29.0.jar) · [Gameplay wiki](wiki/pages/TensuraAdditionalAdditions.md) · [Development guide](standalone/tensura-additional-additions/README.md) · [Validation](standalone/tensura-additional-additions/docs/VALIDATION-1.29.0.md)

Heavenly Restriction and its race evolutions, Chained/Unbound, Battlewills, Inverted Spear of Heaven, Unique and Ultimate skills, Marauder’s Map, Fusion, Parallel World Jumper, The High Realm and its encounters, Chromatic Orb, Vesper and the Archive of Accord.


**New in 1.29.0:** Laplace’s Equation Editor can pay supported actions with magicules, aura, HP, SHP or a percentage mix. Choose **Payment…**, check the live bill, then **Apply & Enable**. Healing and regeneration use magicules/aura only.

## Install

Use **Java 21**, **Tensura 2.0.1.3 or 2.0.1.4**, its required libraries (ManasCore, Architectury, SmartBrainLib and TerraBlender), and **GeckoLib 4.9.2+**. Put the addon JAR in your instance’s `mods` folder. Install matching versions on client and server. **Parallel World Jumper is private single-player only.**

Supports [Tensura Neo Otherworld 1.1.3a](standalone/tensura-additional-additions/docs/compatibility/NEO-OTHERWORLD-1.1.3a.md). Do not install AdditionalAdditions together with the discontinued Myths distribution.

## Guides and previews

Press **F8**, use the pause-menu guide button, or choose **Guide** below Tensura’s status panel.

[Online source wiki](wiki/README.md) · [Styled offline wiki](releases/tensura-addon-wikis.zip) — extract and open `index.html` for navigation, search, pictures and videos.

- [Counter demonstration](docs/media/counter-preview/README.md).
- [World Hop demonstration](docs/media/world-hop-preview/README.md) — recorded on 1.3.0; current world travel uses a hidden library.

## Build AdditionalAdditions

```sh
cd standalone/tensura-additional-additions
python3 scripts/fetch-runtime.py
./gradlew build
```

Use Java 21 and Python 3. Output is in that project’s `build/libs/`. `./gradlew runClient` starts a development client; `./gradlew runGameTestServer` runs native integration checks. See the [development guide](standalone/tensura-additional-additions/README.md#development) for details.

## Archived project: Tensura Myths

**Discontinued.** The final release is **0.36.1**. No further features, fixes or compatibility updates are planned. Its existing source, releases and [wiki](wiki/pages/Tensura-Myths.md) remain available as a historical reference. AdditionalAdditions is the maintained project; it is a separate distribution and does not include all Myths content. See [archive status](DISCONTINUED.md).

| Location | Contents |
| --- | --- |
| [`standalone/tensura-additional-additions/`](standalone/tensura-additional-additions) | Active addon source and build |
| [`src/`](src) | Discontinued Myths source |
| [`wiki/`](wiki/README.md) | Active documentation and clearly marked Myths archive |
| [`releases/`](releases) | Built mods and offline wiki |
| [`docs/`](docs) | Validation, research and development notes |
| [`art/`](art) | Artwork and editable models |

[Changelog](docs/CHANGELOG.md) · [Wiki authoring](docs/WIKI.md) · [Privacy and repository history](docs/PUBLIC-REPOSITORY.md)

Unofficial fan project, not affiliated with ManasMods or Capcom. Get Tensura from its [official page](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated).
