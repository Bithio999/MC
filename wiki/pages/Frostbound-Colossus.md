# Frostbound Colossus

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Frostbound Colossus](../../src/main/resources/assets/wildhunt/textures/skill/frostbound_colossus.png)

| Skill data | Frostbound Colossus |
| --- | --- |
| Category | Unique Skill |
| Inspiration | Jin Dahaad |
| Combat role | Avalanche impact / capture / ice siege |
| Active modes | 2 |
| Toggleable passive | Colossus Carapace |
| Passive upkeep | 20 aura / second |
| Registry ID | `wildhunt:frostbound_colossus` |
| Previous | — |
| Evolution | [Ymir, Lord of Winter](Ymir.md) |

## Overview

A complete replacement for the former rime/fissure/ward kit. Aim a heavy avalanche impact, capture a single foe in an ice prison, and sustain a native barrier carapace. Ymir’s signature rains telegraphed ice onto fixed positions.

## Acquisition

Roll through Tensura’s normal reincarnation system. These Uniques join the configured primary pool; no particular roll is guaranteed. There are no skill trials, boss-awarded skills or race requirements to roll.

## Active modes

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Avalanche Crash | 200 | 4 s | 100 |
| 2. Glacial Prison | 650 | 8 s | 55 |

Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

### Avalanche Crash

Aim at a visible enemy up to 16 blocks away, or at the endpoint if no enemy is selected. Erupt eight rendered ice spires around the impact. Damage enemies in a 4-block radius and launch them upward and outward, respecting knockback resistance. This is an immediate aimed impact, not the old travelling fissure. It neither builds nor consumes rime.

### Glacial Prison

Aim at one visible hostile within 20 blocks. Deal the listed hit damage, then apply Tensura’s native Frost effect for up to 4 seconds, subject to its rank and resistance checks. Six rendered spires and an Ice magic circle mark the prison. The visual cage places no blocks and adds no separate collision wall. There is no projectile-interception budget.

## Toggleable passive: Colossus Carapace

While toggled on, gain 20 armor, 40 physical barrier, 15 magic barrier, one knockback resistance and 50% water-element boost. Tensura classifies ice under water for this boost. This uses the native barrier layer and damage rules, not absorption or the former projectile-deletion ward.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **20 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

## Suggested combo

Capture an enemy with Glacial Prison → aim Avalanche Crash at the captive and nearby foes. As Ymir, use the prison to hold a target within Heavenfall Glacier’s warning circles.

## Strengths and limitations

Prison uses native Frost, so immunity, skill rank and stronger defensive effects can affect it. The cage mesh is visual. Icefall marks stay where they were placed and can be dodged. Neither spires nor falling ice change terrain. There are no rime stacks, shatter preparation, finite projectile wards or expanding frostfronts.

## In-game preview

![Glacial Prison](../media/glacial-prison.png)

Captured in the Minecraft 1.21.1 development client with Wild Hunt 0.5.0.

## Related pages

[Skills](Skills.md) · [Mechanics](Mechanics.md) · [Evolution](Evolution.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.5.0
