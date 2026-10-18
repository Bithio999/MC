# Storm Crown

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Storm Crown](../../src/main/resources/assets/wildhunt/textures/skill/storm_crown.png)

| Skill data | Storm Crown |
| --- | --- |
| Category | Unique Skill |
| Inspiration | Rey Dau |
| Combat role | Precision / charge management |
| Resource | Aura |
| Modes | 3 |
| Registry ID | `wildhunt:storm_crown` |
| Previous | — |
| Evolution | [Raijin, Lord of Thunder](Raijin.md) |

## Overview

A precision lightning art built around a three-charge battery. Its array chooses individual targets; it does not blanket an area with damage. Store charge, line up a shot, and spend the battery on a piercing railgun discharge.

## Acquisition

Roll this skill through Tensura’s normal reincarnation system. It is added to the configured primary pool; it is not guaranteed and has no race requirement to roll. There are no skill trials. Starting skill count, additional rolls and truly-unique restrictions come from the server’s Tensura settings.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Railbolt | 200 | 4 s | 90 |
| 2. Fulgurite Array | 600 | 8 s | 60 initial / 20 pulse |
| 3. Storm Mantle | 300 | 20 s | — |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Railbolt

An instant, block-clipped railgun reaches 36 blocks and pierces 2 direct targets. It consumes all stored charge for +25% direct-hit damage per charge, up to +75%. Chains deal 55% of the uncharged primary base damage and reach six blocks from the direct target. Each charge adds one possible chain target. Wet or drenched targets take 30% more lightning damage.

### Fulgurite Array

Creates three fulgurite rods in a line centered five blocks ahead, spaced four blocks apart. Each rod chooses one visible enemy within six blocks, with no duplicate victim within a pulse. Initial hits use the secondary raw damage; later hits use the pulse raw damage once per second for six seconds. Every successful rod hit restores one charge, capped at three.

### Storm Mantle

For 15 seconds, gain 35% movement, 25% attack speed, 6 armor and 50% lightning boost. Moving at least three and at most sixteen blocks between one-second samples restores a charge. Teleport-sized jumps do not recharge it.

## Suggested combo

Place the Fulgurite Array where enemies will cross it → let hits charge the battery → align several enemies → fire Railbolt. Storm Mantle helps reposition and replenish charge between shots.

## Strengths and limitations

Railbolt spends charge even if the shot misses. Rods need valid visible targets to recharge. Charges are not an extra skill tier; the maximum remains three. Both source and victim line-of-sight checks restrict chains.

## Evolution notes

Two direct targets and up to three base chain targets; a 15-second mobile mantle. Four direct targets, five base chain targets, a 56-block railgun, and a 30-second mantle. Its fourth mode supplies targeted storm judgments and charge recovery.

## Visual effects

Native Tensura lightning particles and animated Lightning magic circles mark the railgun, fulgurite rods and selected storm victims. No destructive vanilla lightning entity is spawned.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
