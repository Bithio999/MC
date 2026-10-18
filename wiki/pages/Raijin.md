# Raijin, Lord of Thunder

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Raijin, Lord of Thunder](../../src/main/resources/assets/wildhunt/textures/skill/raijin.png)

| Skill data | Raijin, Lord of Thunder |
| --- | --- |
| Category | Ultimate Skill |
| Inspiration | Rey Dau |
| Combat role | Precision / charge management |
| Resource | Aura |
| Modes | 4 |
| Registry ID | `wildhunt:raijin` |
| Previous | [Storm Crown](Storm-Crown.md) |
| Evolution | — |

## Overview

A precision lightning art built around a three-charge battery. Its array chooses individual targets; it does not blanket an area with damage. Store charge, line up a shot, and spend the battery on a piercing railgun discharge.

## Acquisition

Evolve from [Storm Crown](Storm-Crown.md) as Sovereign Hunter with 2,000,000 base EP and mastery of that matching Unique skill. Use `/wildhunt ascend rey_dau`. The Ultimate replaces only its source Unique; mastery of the new Ultimate starts separately.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Railbolt | 800 | 3 s | 270 |
| 2. Fulgurite Array | 2,400 | 7 s | 180 initial / 60 pulse |
| 3. Storm Mantle | 1,200 | 19 s | — |
| 4. Heaven’s Thunder | 6,000 | 60 s | Signature-specific; see below |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Railbolt

An instant, block-clipped railgun reaches 56 blocks and pierces 4 direct targets. It consumes all stored charge for +25% direct-hit damage per charge, up to +75%. Chains deal 55% of the uncharged primary base damage and reach six blocks from the direct target. Each charge adds one possible chain target. Wet or drenched targets take 30% more lightning damage.

### Fulgurite Array

Creates three fulgurite rods in a line centered five blocks ahead, spaced four blocks apart. Each rod chooses one visible enemy within six blocks, with no duplicate victim within a pulse. Initial hits use the secondary raw damage; later hits use the pulse raw damage once per second for six seconds. Every successful rod hit restores one charge, capped at three.

### Storm Mantle

For 30 seconds, gain 60% movement, 50% attack speed, 12 armor and 100% lightning boost. Moving at least three and at most sixteen blocks between one-second samples restores a charge. Teleport-sized jumps do not recharge it.

### Heaven’s Thunder

For ten seconds, judge up to five visible enemies within fourteen blocks once per second, prioritizing maximum health. Each successful strike replenishes charge. Lightning descends on selected enemies rather than damaging the whole radius.

## Suggested combo

Place the Fulgurite Array where enemies will cross it → let hits charge the battery → align several enemies → fire Railbolt. Storm Mantle helps reposition and replenish charge between shots.

## Strengths and limitations

Railbolt spends charge even if the shot misses. Rods need valid visible targets to recharge. Charges are not an extra skill tier; the maximum remains three. Both source and victim line-of-sight checks restrict chains.

## Evolution notes

Four direct targets, five base chain targets, a 56-block railgun, and a 30-second mantle. Its fourth mode supplies targeted storm judgments and charge recovery.

## Visual effects

Native Tensura lightning particles and animated Lightning magic circles mark the railgun, fulgurite rods and selected storm victims. No destructive vanilla lightning entity is spawned.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
