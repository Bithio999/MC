# Scarlet Deluge

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Scarlet Deluge](../../src/main/resources/assets/wildhunt/textures/skill/scarlet_deluge.png)

| Skill data | Scarlet Deluge |
| --- | --- |
| Category | Unique Skill |
| Inspiration | Uth Duna |
| Combat role | Position control / reactive defense |
| Resource | Aura |
| Modes | 3 |
| Registry ID | `wildhunt:scarlet_deluge` |
| Previous | — |
| Evolution | [Varuna, Lord of Tides](Varuna.md) |

## Overview

An aquatic control art that physically moves opponents. Its primary is a travelling wave, its secondary is an aimed whirlpool, and its veil answers incoming attacks. Positioning matters more than lining up a laser.

## Acquisition

Roll this skill through Tensura’s normal reincarnation system. It is added to the configured primary pool; it is not guaranteed and has no race requirement to roll. There are no skill trials. Starting skill count, additional rolls and truly-unique restrictions come from the server’s Tensura settings.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Crushing Current | 160 | 3 s | 65 |
| 2. Scarlet Undertow | 500 | 7 s | 45 initial / 18 pulse |
| 3. Water Veil | 250 | 18 s | 25 retaliation |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Crushing Current

Sends a broad wave along the ground toward your facing direction. The front travels up to 18 blocks over about 0.8 seconds, carrying and drenching each enemy it hits. One wave cannot damage the same entity twice. A previously drenched victim takes 50% more primary damage.

### Scarlet Undertow

Places a six-second whirlpool at your aim point, up to six blocks away. The radius is 9 blocks. It hits immediately, then pulses once per second, and applies gentle inward current between damage pulses. Pulls respect knockback resistance.

### Water Veil

For 15 seconds, gain 10 armor, a 20-point magic barrier, 16 absorption HP, water breathing and 50% water boost. A resolved hostile hit that removes health triggers a retaliatory water strike against a visible attacker within twelve blocks. Counter cooldown: 3 seconds.

## Suggested combo

Aim Scarlet Undertow behind or between enemies → use the current to gather them → send Crushing Current through the drenched group. Use Water Veil when expecting a counterattack.

## Strengths and limitations

A wave is a moving front, so distant targets are not hit immediately. Knockback resistance reduces carrying and currents. Environmental damage has no attacker to retaliate against. A veil counter requires health damage and a visible attacker.

## Evolution notes

An 18-block wave, nine-block whirlpool, and 15-second veil with a three-second retaliation cooldown. A 26-block wave, thirteen-block whirlpool, a 30-second stronger veil, one-second retaliation cooldown, and a fourth mode that alternates the tide.

## Visual effects

Native water bubbles and blue shockwaves form a moving wavefront and a rotating whirlpool. Water magic circles mark the field; the veil uses orbiting bubbles instead of a recolored fire aura.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
