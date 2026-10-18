# Frostbound Colossus

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Frostbound Colossus](../../src/main/resources/assets/wildhunt/textures/skill/frostbound_colossus.png)

| Skill data | Frostbound Colossus |
| --- | --- |
| Category | Unique Skill |
| Inspiration | Jin Dahaad |
| Combat role | Ground rupture / projectile defense |
| Resource | Aura |
| Modes | 3 |
| Registry ID | `wildhunt:frostbound_colossus` |
| Previous | — |
| Evolution | [Ymir, Lord of Winter](Ymir.md) |

## Overview

A deliberate ice art with ground fissures and a finite frontal projectile ward. Build rime at the rampart, interrupt incoming shots, then rupture a fully prepared opponent. Its Ultimate spreads a frostfront rather than repeating a full-radius burst.

## Acquisition

Roll this skill through Tensura’s normal reincarnation system. It is added to the configured primary pool; it is not guaranteed and has no race requirement to roll. There are no skill trials. Starting skill count, additional rolls and truly-unique restrictions come from the server’s Tensura settings.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Glacial Rupture | 200 | 4 s | 75 |
| 2. Glacial Rampart | 650 | 8 s | 45 initial / 20 pulse |
| 3. Colossus Guard | 350 | 20 s | — |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Glacial Rupture

Sends a narrow ground fissure forward, reaching up to 36 blocks in about 0.8 seconds. The fissure hits 1 targets once each. A target with three of your rime stacks takes +120 raw damage and loses those stacks. Other hits add rime; native Chill accompanies the frost effects.

### Glacial Rampart

Raises a ten-block-wide frontal frost ward four blocks ahead for up to six seconds. It chills and builds rime on enemies crossing its slab, with initial and one-second pulse damage. Intercepts up to 3 hostile moving projectiles, then collapses. Your shots and allied shots are preserved.

### Colossus Guard

For 15 seconds, gain 20 armor, a 15-point magic barrier, one knockback resistance and 50% water-element boost (Tensura classifies ice under water). This is defense and stability, not blanket boss invulnerability.

## Suggested combo

Stand behind Glacial Rampart while it chills enemies and blocks shots → wait for three rime stacks → aim Glacial Rupture. As Ymir, Absolute Winter can prepare multiple enemies before a piercing rupture.

## Strengths and limitations

The ward is a frontal plane, not an enclosing dome or a solid world block. It cannot stop hitscan beams, stationary effects or ownerless shots. The Unique ward does not intercept native projectiles marked as Ultimate skills. Projectile budgets and ordinary rank/resistance rules still apply.

## Evolution notes

One fissure target, thirty-six-block reach, three projectile interceptions, and a 15-second guard. Four fissure targets, fifty-six-block reach, six interceptions, a 30-second stronger guard, and an expanding frostfront that primes the shatter combo.

## Visual effects

Native snowflake effects outline jagged fissures and frost growth. Upright Ice magic circles identify the ward plane. The frostfront grows outward from its anchored origin.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
