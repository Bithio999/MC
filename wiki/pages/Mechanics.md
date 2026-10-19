# Combat mechanics

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

## Damage and mastery

Wild Hunt’s damage multiplier is:

```text
(1 + 0.5 × clamp(masteryFraction, 0, 1)) × (Ultimate ? 3 : 1)
```

**No EP term is present.** Mastery caps at 1.5× the Unique’s raw base, or 4.5× that same base for a mastered Ultimate. Skill-page tables already include the Ultimate’s 3× multiplier. Native Tensura elemental boosts, global combat rules, rank interactions, barriers and resistances still apply; raw values do not promise the same health damage against every enemy.

EP remains a requirement for race evolution and Ultimate ascension. Those progression gates do not increase these attacks’ raw damage.

## No passive stacks

Storm charge, owned oil stacks and owned rime stacks are removed. No attack builds, consumes or amplifies damage from them. Old marks are ignored and removed as entities tick. There is no shatter threshold or melee coating.

Oil pools remain finite **world locations**, not passive stacks on a character. Drenched remains one timed water mark: it has no counter or buildup and supports the documented water/lightning interactions.

## Toggleable passives

Each skill has two active modes (three for an Ultimate) plus a separate native skill toggle in Tensura’s Ability Selection. The old final buff modes are removed from mode cycling.

| Line | Passive | Unique aura / second | Ultimate aura / second |
| --- | --- | ---: | ---: |
| Rey Dau | Storm Mantle | 10 | 40 |
| Uth Duna | Water Veil | 10 | 40 |
| Nu Udra | Furnace Mantle | 15 | 60 |
| Jin Dahaad | Colossus Carapace | 20 | 80 |

The first second is paid when enabling; upkeep is charged once every twenty game ticks while active. Passives stay on until disabled, unfunded or blocked by activation requirements. They do not expire after fifteen or thirty seconds. Attribute contributions use stable IDs, so refreshes cannot accumulate duplicate bonuses. Turning off or forgetting the skill removes its modifiers. Saved native toggle state is restored on login.

## Native barriers

Water Veil and Colossus Carapace contribute to Tensura’s `PHYSICAL_BARRIER` and `MAGIC_BARRIER` attributes. Tensura handles their interaction with damage and renders its native barrier layer around the character. Wild Hunt adds no absorption hearts and does not implement a parallel shield-health pool. Other skills’ barrier contributions remain separate.

Water Veil retains water breathing and an attacker-targeted retaliation after actual health damage. Furnace Mantle retains fire protection while enabled; both utility effects stop immediately on toggle-off without leftover timed potions. Furnace Mantle no longer oils melee victims. The old frost ward’s projectile deletion and interception budget are removed.

## Aiming and movement

Railbolt begins with a block-clipped aimed hit, then ricochets from the latest victim. Thunder Step sweeps the whole caster bounding box and stops at collisions. Water waves sweep continuous segments so targets cannot hide between movement updates. Oil Lash selects a limited forward fan and creates synced tentacle geometry. Avalanche Crash is an aimed area impact; Glacial Prison targets one enemy. Heavenfall Glacier warns fixed positions for half a second before impact.

Attacks respect teams, owned pets, creative/spectator immunity, PvP settings and line of sight. Movement respects knockback resistance. Dimension changes invalidate anchored effects. Native status resistance and skill ranks can prevent Frost or Chill. No art destroys terrain or places permanent ice.

## Visual effects

Oil Lash uses a synced procedural mesh with a dark tapered body, glowing orange bands, extension and recoil, including on a miss. Ice spires, prison bars and falling ice use rendered faceted crystals. Those visual entities deal no damage by themselves, have no collision and expire automatically; server skill logic owns targeting and damage.

Tensura supplies elemental particles, shockwaves, native barrier rendering and animated magic circles. Its assets are used at runtime and are not copied into the addon.

## Updating saved characters

Version 0.5.0 keeps learned skills, mastery and distinct boss history. It removes old transient combat timers and resources, drops the old buff mode, and moves the former fourth Ultimate mode into the third active slot while preserving/capping its cooldown. Equipped presets migrate with the mode list. Legacy toggle passives start off; no skill reroll is needed. A matching still-active legacy Water Veil absorption effect is cleared during migration.

---

**Categories:** Tensura: Wild Hunt · Version 0.5.0
