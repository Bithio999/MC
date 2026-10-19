# Combat mechanics

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Reference](https://github.com/Bithio999/MC/wiki/Mechanics) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

This page explains **Tensura Myths combat mechanics**, including native barriers, skill toggles, damage modifiers, and aura consumption.

## Damage types

The [ability index](https://github.com/Bithio999/MC/wiki/Abilities) and [damage reference](https://github.com/Bithio999/MC/wiki/Damage-Types) distinguish every direct elemental source, native status and non-damaging utility. Vritra and Thoth use multiple elements within one skill; each strike keeps its own native damage source. Reflection preserves the projectile’s own type. Healing, gates, resource conversion and barriers do not invent attack damage.

## Damage and mastery

The original four apex Uniques use **0.90 × (1 + 0.5 × mastery fraction)**. The four additional Uniques, combination Ultimates, Aion and Thoth use **1 + 0.5 × mastery fraction**. Mastery fractions are clamped to 0–1, giving a maximum of 1.5× before native modifiers. Each new kit has its own raw damage values; there is no shared Ultimate 3× multiplier.

**EP never multiplies damage.** It is a fixed progression requirement. Native elemental boosts, skill authority, barriers, resistance/nullification, hit immunity and server rules still determine actual health loss. Overlapping warnings do not reset vanilla/native hit immunity to force multiple full hits.

Aion’s active passive adds 35% damage against non-Godly owners while preserving the original attack type. Thoth’s elemental boosts and native magic-efficiency attributes are separate from Aion’s resistance suppression.

## World effects

Oil pools occupy finite world locations. Drenched is a timed water mark that supports the water/lightning interactions described on the skill pages.

## Toggleable passives

Unique skills have two active modes, combination Ultimates three, and Godly skills four, with a separate native skill toggle in Tensura’s Ability Selection.

| Skill | Upkeep per second | Native multilayer capacity |
| --- | ---: | ---: |
| Storm Crown | 10 aura | — |
| Scarlet Deluge | 10 aura | 100 |
| Black Flame | 15 aura | — |
| Frostbound Colossus | 20 aura | 150 |
| Threadweaver | 15 aura | — |
| Rune Smith | 20 aura | — |
| Verdant Keeper | 20 aura | — |
| Mirrorbound | 15 aura | — |
| Vritra | 80 aura | — |
| Demeter | 70 aura | 500 |
| Janus | 70 aura | 300 |
| Aion | 180 magicules | 1,200 |
| Thoth | 180 magicules | 1,000 |

The first second is paid when enabling; upkeep is charged once every twenty game ticks while active. Passives stay on until disabled, unfunded or blocked by activation requirements. They do not expire after fifteen or thirty seconds. Attribute contributions use stable IDs, so refreshes cannot accumulate duplicate bonuses. Turning off or forgetting the skill removes its modifiers. Saved native toggle state is restored on login.

## Native barriers

Water Veil and Colossus Carapace contribute to Tensura’s `PHYSICAL_BARRIER` and `MAGIC_BARRIER` attributes and arm `MULTILAYER_BARRIER`, which is the attribute read by its shield HUD counter. Tensura handles their interaction with damage and renders its native barrier layer around the character. Wild Hunt adds no separate absorption-heart pool. Other skills’ barrier contributions remain separate.

Demeter, Janus, Aion and Thoth also use native recovering personal multilayer barriers. Crystal Bulwark lends an additional six-second, 250-capacity ward to eligible allies; that ward expires rather than regenerating, and owns separate modifiers for each caster.

The displayed value is native per-hit blocking capacity. Tensura charges magicules for blocked damage. Overflow, inadequate magicules and native power interactions can break the shield. A broken multilayer barrier automatically rearms at the first paid upkeep after ten seconds without a hit (normally 10–11 seconds). Hits, including fully blocked hits, restart recovery. Retoggling cannot skip this delay. The shield modifier is saved across relogs; turning off removes only this art’s contribution. Ensure Tensura’s **Barrier** HUD decoration is enabled if you have customized its HUD.

Water Veil retains water breathing and an attacker-targeted retaliation after actual health damage. Furnace Mantle retains fire protection while enabled; both utility effects stop immediately on toggle-off without leftover timed potions.

## Aiming and movement

Railbolt begins with a block-clipped aimed hit, then ricochets from the latest victim. Thunder Step follows the full aim vector, including vertical pitch, and sweeps the whole caster bounding box. Collisions, build-height limits and the world border stop it. Railbolt bounces reach twelve blocks through walls from the most recent victim. Thunder Step reaches 16 blocks with a 5-second cooldown and preserves all three components of player momentum. Undertow is placed on the crosshair ray within 24 blocks, using the hit surface or bounded endpoint. Water waves sweep continuous segments so targets cannot hide between movement updates. Oil Lash selects a limited forward fan and creates synced tentacle geometry. Avalanche Crash is an aimed area impact; Glacial Prison targets one enemy. Winter Harvest warns fixed crystal blooms for 1.2 seconds; Parallax Snare warns a spatial convergence for 0.8 seconds.

Attacks respect teams, owned pets, creative/spectator immunity and PvP settings. They check line of sight except for Railbolt ricochets after the initial hit. Movement respects knockback resistance. Dimension changes invalidate anchored effects. Native status resistance and skill ranks can prevent Frost or Chill. No art destroys terrain or places permanent ice.

## Aura regeneration

See [Aura regeneration](https://github.com/Bithio999/MC/wiki/Aura-Regeneration) for the three race-granted Extra Skills, healing speed, aura costs, suppression and death-prevention limits. They never charge magicules. Default native EP gain stays split two thirds aura and one third magicules.

## Visual effects

Water waves render a broad curled mesh with a foam crest; Undertow renders rotating teal and coral spiral ribbons. Water and ice toggles emit no constant custom particles. Oil Lash uses a synced procedural mesh with a dark tapered body, glowing orange bands, extension and recoil, including on a miss. Ice spires and prison bars use rendered faceted crystals. New kits add silk strands, inscribed mines, growing briars and crystal trees, orbiting mirrors, plasma conduits, architectural gates, closing spatial blades and open orbiting spellbooks. Those visual entities deal no damage by themselves, have no collision and expire automatically; server skill logic owns targeting and damage.

Tensura supplies elemental particles, shockwaves, native barrier rendering and animated magic circles. Its assets are used at runtime and are not copied into the addon.

## Barrier HUD examples

![Water Veil native HUD showing 100](https://github.com/Bithio999/MC/blob/main/wiki/media/water-veil-hud.png?raw=true)

![Colossus Carapace native HUD showing 150](https://github.com/Bithio999/MC/blob/main/wiki/media/colossus-carapace-hud.png?raw=true)

The examples show the Unique passives enabled.

![Centered portraits and combined native barrier HUD](https://github.com/Bithio999/MC/blob/main/wiki/media/hunter-icons-and-barriers.png?raw=true)

The native HUD shows 250 combined capacity with both Unique barrier passives enabled (100 Water Veil + 150 Colossus Carapace); no custom bubble/frost orbit is emitted.

## Hunter vitality

Race HP bonuses are +100 / +500 / +1,000; native SHP bonuses are +400 / +3,200 / +6,600. These replace earlier hunter-stage bonuses. See [Human evolution](https://github.com/Bithio999/MC/wiki/Evolution#hp-and-shp-growth).

---

**Categories:** Tensura Myths · Reference
