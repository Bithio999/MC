# Frostbound Colossus

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

![Frostbound Colossus](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/frostbound_colossus.png?raw=true)

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
| Evolution | [Ymir, Lord of Winter](https://github.com/Bithio999/MC/wiki/Ymir) |

## Overview

A complete replacement for the former rime/fissure/ward kit. Aim a heavy avalanche impact, capture a single foe in an ice prison, and sustain a native barrier carapace. Ymir’s signature rains telegraphed ice onto fixed positions.

## Acquisition

Roll through Tensura’s normal reincarnation system. These Uniques join the configured primary pool; no particular roll is guaranteed. There are no skill trials, boss-awarded skills or race requirements to roll.

## Active modes

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Avalanche Crash | 200 | 4 s | 100 |
| 2. Glacial Prison | 650 | 8 s | 55 |

**0.7.0 damage adjustment:** multiply every raw damage value below by **0.90**. This applies to direct hits, ricochets, fields, counters and signature pulses. Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

### Avalanche Crash

Aim at a visible enemy up to 16 blocks away, or at the endpoint if no enemy is selected. Erupt eight rendered ice spires around the impact. Damage enemies in a 4-block radius and launch them upward and outward, respecting knockback resistance. This is an immediate aimed impact, not the old travelling fissure. It neither builds nor consumes rime.

### Glacial Prison

Aim at one visible hostile within 20 blocks. Deal the listed hit damage, then apply Tensura’s native Frost effect for up to 4 seconds, subject to its rank and resistance checks. Six rendered spires and an Ice magic circle mark the prison. The visual cage places no blocks and adds no separate collision wall. There is no projectile-interception budget.

## Toggleable passive: Colossus Carapace

While toggled on, gain 20 armor, 36 physical barrier, 13.5 magic barrier, one knockback resistance and 50% water-element boost. Tensura classifies ice under water for this boost. This uses the native barrier layer and damage rules, not absorption or the former projectile-deletion ward.

The passive also arms a **150-capacity native Multilayer Barrier**, which appears in Tensura’s shield HUD counter when its Barrier decoration is enabled. The counter is a per-hit blocking capacity, not absorption hearts or a custom health bar. Native rules spend magicules on blocked damage and can break the barrier on overflow, insufficient magicules or stronger attacks. While still toggled on, a broken barrier automatically rearms on the first paid upkeep after **ten seconds without an incoming hit**. Any new hit restarts the delay, including hits completely blocked by a surviving shield. Retoggling does not bypass the delay. Surviving shields persist across relogs.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **20 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

The toggle no longer emits constant frost crystals. Native barrier rendering and the HUD remain.

## Suggested combo

Capture an enemy with Glacial Prison → aim Avalanche Crash at the captive and nearby foes. As Ymir, use the prison to hold a target within Heavenfall Glacier’s warning circles.

## Strengths and limitations

Prison uses native Frost, so immunity, skill rank and stronger defensive effects can affect it. The cage mesh is visual. Icefall marks stay where they were placed and can be dodged. Neither spires nor falling ice change terrain. There are no rime stacks, shatter preparation, finite projectile wards or expanding frostfronts.

## In-game preview

![Glacial Prison](https://github.com/Bithio999/MC/blob/main/wiki/media/glacial-prison.png?raw=true)

Captured in the Minecraft 1.21.1 development client with Wild Hunt 0.5.0.

## Native barrier HUD

![Native shield HUD counter](https://github.com/Bithio999/MC/blob/main/wiki/media/colossus-carapace-hud.png?raw=true)

The Unique passive’s native shield counter in Wild Hunt 0.6.0. Its Ultimate uses a higher capacity.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

---

**Categories:** Tensura: Wild Hunt · Version 0.7.0
