# Tensura: Heavenly Restriction

An independent **NeoForge 1.21.1 addon for Tensura: Reincarnated**, extracted from Tensura Myths, with Unbound and evolved movement Battlewills. It includes the Heavenly Restriction race lineage, Chained and its Unbound Ultimate evolution, aura recovery skills, Air Walk, and Instant Transmission. **Tensura Myths is not required.**

[Download 1.1.8](../../releases/tensura-heavenly-restriction-1.1.8.jar) · [Gameplay guide](docs/GUIDE.md) · [Validation](docs/VALIDATION-1.1.8.md)

## Tensura Neo Otherworld 1.1.3a

Use **1.1.6 or newer** with this pack. The addon accepts its pinned Tensura 2.0.1.4 and GeckoLib 4.9.2, preserves the pack’s configs and respects Mysticism’s Constant Energy regeneration lock. All 39 addon starting skills are audited: 29 are excluded for magicule costs or target-skill copying, while 10 remain eligible. Pride’s exception and Creator inheritance are preserved.

Replace the older Heavenly Restriction JAR in the instance’s `mods` folder. On multiplayer, update the server too. Keep Tensura Myths out of this standalone instance. [Compatibility report and exact pack lock](docs/compatibility/NEO-OTHERWORLD-1.1.3a.md).

## Included content

- **Fettered Human → Tempered Vessel → Unshackled → Heavenly Restricted → Heavenbreaker**, through Tensura's reincarnation and evolution menus.
- **Chained**, granted only by Fettered Human and retained through evolution, with 5× later aura-capacity gains. Its combat toggle grants 5× damage sourced from Battlewill abilities, or 3× other outgoing Physical damage. These bonuses use exclusive branches. Aura Sword is excluded from the separate 3× buff-effectiveness bonus; the dash supports upward aiming, finds safe standing room around terrain and canopies, and stops before aimed enemies.
- **Hunter's Recovery**, **Apex Renewal**, and **Sovereign Rebirth**, granted at evolutions two, three, and four. Only the strongest enabled recovery tier heals.
- **Instant Transmission**, the native-coordinate self-teleportation evolution of mastered Instant Move: 100-tick charge (instant mastered), 10 aura per block (5 mastered).
- **Air Walk**, the aura-powered Battlewill evolution of mastered native Air Flight / Air Float.
- The original race/skill icons, broken-chain teleport visual, recovery bloom's Blockbench/GeckoLib rig, smooth surface rendering, and aura-flight particles.

**Broken Limit** is a **Passive — True — Mastered** ability: resistance degradation and dodge bypass remain active while mastered Chained is owned, independently of toggle and slots.

At Heavenbreaker, fully mastered permanent Chained automatically evolves into mastered Unbound once you have True Hero or True Demon Lord status. No menu activation, learning input or payment is required. Saved unfinished Unbound entries complete when eligible.

The current Chained dash uses a **16-block eye ray**, retains native block-surface landings, supports upward air aiming and stops before the aimed enemy, has a **1-second cooldown**, costs **2% of current aura**, and deals **Physical damage** at **1.5√A path / 3√A arrival damage**, with A the aura paid. EP growth remains **90% aura / 10% magicules**, with Chained blocking subsequent magicule-capacity growth. Fixed evolution gates check **total EP** shown in Status. Chained retains up to **1 magicule per second** and converts all remaining computed biome/rate/sleep regeneration to aura. Current magicules are capped at their effective maximum; native low-rate penalties and spiritual maintenance remain. Positive `ep max add` commands follow 90/10, and future aura-capacity gains retain 5× after permanent losses.

## Inverted Spear of Heaven

The standalone includes the spear with **80 attack damage / 1.6 attack speed**, matching Hihiirokane Sword. It is a **2% additional ominous-vault reward** with built-in **Nullification I**: attack spiritual entities, shatter native barriers, clear skill status effects and prevent skill activation for **2 seconds**.

Chained/Unbound dash and Counter carry main-hand weapon damage, enchantments and engraving hit effects. Equipped Unerring Body also grants Fear, Slowness, Burden and Haki immunity. See the [gameplay guide](docs/GUIDE.md).

## Installation

Use **Java 21**, **Minecraft 1.21.1**, and **NeoForge 21.1.209 or later in the 21.1 line**. Install on both the client and server:

1. **Tensura: Reincarnated 2.0.1.3 or 2.0.1.4** and its dependencies, including **ManasCore 4.0.0.2**, **Architectury 13.0.8+**, **SmartBrainLib**, and **TerraBlender**.
2. **GeckoLib 4.9.2** (4.x).
3. **tensura-heavenly-restriction-1.1.8.jar**.

Use this addon **instead of Tensura Myths**. The loader rejects installing both because both implement the same Chained and Battlewill hooks. The full Tensura Myths mod remains available separately with all its existing content.

This addon uses its own **`heavenlyrestriction` namespace** and persistent state. It does **not** automatically convert `wildhunt` race/skill IDs or worlds from Tensura Myths. Start a separate instance/world for the standalone addon; retain Myths in existing Myths worlds unless performing a deliberate migration. No existing world files are changed by building or installing the standalone JAR.

## Development

This directory is a complete Gradle project. It does not compile against the parent project or its release JAR.

```sh
cd standalone/heavenly-restriction
python3 scripts/fetch-runtime.py
./gradlew build runGameTestServer
```

Java 21 is required. The fetch script verifies the exact runtime dependency hashes in `runtime-mods.lock.json`. Gradle resolves the pinned Tensura, ManasCore and Architectury versions. Output: `build/libs/tensura-heavenly-restriction-1.1.8.jar`.

`./gradlew runClient` starts an isolated development client. Native tests cover the extracted mechanics and prove that Tensura Myths is absent. Optional visual-review code in `docs/review` is excluded from regular builds; see its instructions.

Original Blockbench authoring source: [vital_corolla.bbmodel](art/blockbench/vital_corolla.bbmodel). Registry IDs and gameplay implementation are isolated under `dev.heavenlyrestriction` / `heavenlyrestriction`. This distribution adds five races, seven skills and one non-damaging visual entity. It includes no Myths bosses, hunter races, otherworlders, Godly skills, secondary-race system or synthesis UI.
