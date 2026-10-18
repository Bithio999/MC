# Ymir, Lord of Winter

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Ymir, Lord of Winter](../../src/main/resources/assets/wildhunt/textures/skill/ymir.png)

| Skill data | Ymir, Lord of Winter |
| --- | --- |
| Category | Ultimate Skill |
| Inspiration | Jin Dahaad |
| Combat role | Ground rupture / projectile defense |
| Resource | Aura |
| Modes | 4 |
| Registry ID | `wildhunt:ymir` |
| Previous | [Frostbound Colossus](Frostbound-Colossus.md) |
| Evolution | — |

## Overview

A deliberate ice art with ground fissures and a finite frontal projectile ward. Build rime at the rampart, interrupt incoming shots, then rupture a fully prepared opponent. Its Ultimate spreads a frostfront rather than repeating a full-radius burst.

## Acquisition

Evolve from [Frostbound Colossus](Frostbound-Colossus.md) as Sovereign Hunter with 2,000,000 base EP and mastery of that matching Unique skill. Use `/wildhunt ascend jin_dahaad`. The Ultimate replaces only its source Unique; mastery of the new Ultimate starts separately.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Glacial Rupture | 800 | 3 s | 225 |
| 2. Glacial Rampart | 2,600 | 7 s | 135 initial / 60 pulse |
| 3. Colossus Guard | 1,400 | 19 s | — |
| 4. Absolute Winter | 6,000 | 60 s | Signature-specific; see below |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Glacial Rupture

Sends a narrow ground fissure forward, reaching up to 56 blocks in about 0.8 seconds. The fissure hits 4 targets once each. A target with three of your rime stacks takes +360 raw damage and loses those stacks. Other hits add rime; native Chill accompanies the frost effects.

### Glacial Rampart

Raises a ten-block-wide frontal frost ward four blocks ahead for up to six seconds. It chills and builds rime on enemies crossing its slab, with initial and one-second pulse damage. Intercepts up to 6 hostile moving projectiles, then collapses. Your shots and allied shots are preserved.

### Colossus Guard

For 30 seconds, gain 40 armor, a 40-point magic barrier, one knockback resistance and 100% water-element boost (Tensura classifies ice under water). This is defense and stability, not blanket boss invulnerability.

### Absolute Winter

Anchors a growing frostfront at the cast position. It begins at radius two and reaches fourteen blocks over nine seconds of its ten-second lifetime. Each newly reached enemy can be hit once and is primed with three owned rime stacks for Glacial Rupture.

## Suggested combo

Stand behind Glacial Rampart while it chills enemies and blocks shots → wait for three rime stacks → aim Glacial Rupture. As Ymir, Absolute Winter can prepare multiple enemies before a piercing rupture.

## Strengths and limitations

The ward is a frontal plane, not an enclosing dome or a solid world block. It cannot stop hitscan beams, stationary effects or ownerless shots. The Unique ward does not intercept native projectiles marked as Ultimate skills. Projectile budgets and ordinary rank/resistance rules still apply.

## Evolution notes

Four fissure targets, fifty-six-block reach, six interceptions, a 30-second stronger guard, and an expanding frostfront that primes the shatter combo.

## Visual effects

Native snowflake effects outline jagged fissures and frost growth. Upright Ice magic circles identify the ward plane. The frostfront grows outward from its anchored origin.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
