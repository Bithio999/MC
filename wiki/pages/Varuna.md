# Varuna, Lord of Tides

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Varuna, Lord of Tides](../../src/main/resources/assets/wildhunt/textures/skill/varuna.png)

| Skill data | Varuna, Lord of Tides |
| --- | --- |
| Category | Ultimate Skill |
| Inspiration | Uth Duna |
| Combat role | Position control / reactive defense |
| Resource | Aura |
| Modes | 4 |
| Registry ID | `wildhunt:varuna` |
| Previous | [Scarlet Deluge](Scarlet-Deluge.md) |
| Evolution | — |

## Overview

An aquatic control art that physically moves opponents. Its primary is a travelling wave, its secondary is an aimed whirlpool, and its veil answers incoming attacks. Positioning matters more than lining up a laser.

## Acquisition

Evolve from [Scarlet Deluge](Scarlet-Deluge.md) as Sovereign Hunter with 2,000,000 base EP and mastery of that matching Unique skill. Use `/wildhunt ascend uth_duna`. The Ultimate replaces only its source Unique; mastery of the new Ultimate starts separately.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Crushing Current | 640 | 2 s | 195 |
| 2. Scarlet Undertow | 2,000 | 6 s | 135 initial / 54 pulse |
| 3. Water Veil | 1,000 | 17 s | 75 retaliation |
| 4. Crimson Ocean | 6,000 | 60 s | Signature-specific; see below |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Crushing Current

Sends a broad wave along the ground toward your facing direction. The front travels up to 26 blocks over about 0.8 seconds, carrying and drenching each enemy it hits. One wave cannot damage the same entity twice. A previously drenched victim takes 50% more primary damage.

### Scarlet Undertow

Places a six-second whirlpool at your aim point, up to six blocks away. The radius is 13 blocks. It hits immediately, then pulses once per second, and applies gentle inward current between damage pulses. Pulls respect knockback resistance.

### Water Veil

For 30 seconds, gain 25 armor, a 40-point magic barrier, 32 absorption HP, water breathing and 100% water boost. A resolved hostile hit that removes health triggers a retaliatory water strike against a visible attacker within twelve blocks. Counter cooldown: 1 seconds.

### Crimson Ocean

For ten seconds, alternate inward and outward surges within fourteen blocks. Incoming tide gathers enemies; outgoing tide pushes them away and slightly upward. The sequence follows the caster instead of remaining at the original whirlpool position.

## Suggested combo

Aim Scarlet Undertow behind or between enemies → use the current to gather them → send Crushing Current through the drenched group. Use Water Veil when expecting a counterattack.

## Strengths and limitations

A wave is a moving front, so distant targets are not hit immediately. Knockback resistance reduces carrying and currents. Environmental damage has no attacker to retaliate against. A veil counter requires health damage and a visible attacker.

## Evolution notes

A 26-block wave, thirteen-block whirlpool, a 30-second stronger veil, one-second retaliation cooldown, and a fourth mode that alternates the tide.

## Visual effects

Native water bubbles and blue shockwaves form a moving wavefront and a rotating whirlpool. Water magic circles mark the field; the veil uses orbiting bubbles instead of a recolored fire aura.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
