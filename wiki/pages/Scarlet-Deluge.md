# Scarlet Deluge

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Scarlet Deluge](../../src/main/resources/assets/wildhunt/textures/skill/scarlet_deluge.png)

| Skill data | Scarlet Deluge |
| --- | --- |
| Category | Unique Skill |
| Inspiration | Uth Duna |
| Combat role | Moving tides / native barriers |
| Active modes | 2 |
| Toggleable passive | Water Veil |
| Passive upkeep | 10 aura / second |
| Registry ID | `wildhunt:scarlet_deluge` |
| Previous | — |
| Evolution | [Varuna, Lord of Tides](Varuna.md) |

## Overview

A water-control kit with a travelling wave, an aimed whirlpool, and a sustained defensive passive. Its defense uses Tensura’s physical and magic barriers instead of absorption hearts.

## Acquisition

Roll through Tensura’s normal reincarnation system. These Uniques join the configured primary pool; no particular roll is guaranteed. There are no skill trials, boss-awarded skills or race requirements to roll.

## Active modes

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Crushing Current | 160 | 3 s | 65 |
| 2. Scarlet Undertow | 500 | 7 s | 45 |

Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

### Crushing Current

Send a broad wave up to 18 blocks over about sixteen game ticks. It carries and drenches each enemy it reaches, damaging a given entity at most once per wave. Its front is 4 blocks wide on either side. Previously drenched enemies take 50% extra primary damage. Drenched is a single twelve-second mark, never a stack counter.

### Scarlet Undertow

Place a six-second whirlpool at the aim point, up to six blocks away. The radius is 9 blocks. It hits immediately and once per second, with a gentle inward current between pulses. Raw pulse damage is 18; the table lists opening damage.

## Toggleable passive: Water Veil

While toggled on, gain 10 armor, 20 physical barrier, 20 magic barrier and 50% water boost. These native attributes participate in Tensura’s damage handling and visible barrier layer. Water breathing remains active while toggled on. No absorption hearts are added. A hostile hit that removes health triggers a 25-raw water counter (75 as Varuna) against a visible attacker within twelve blocks, with a 3-second cooldown.

The passive also arms a **100-capacity native Multilayer Barrier**, which appears in Tensura’s shield HUD counter when its Barrier decoration is enabled. The counter is a per-hit blocking capacity, not absorption hearts or a custom health bar. Native rules spend magicules on blocked damage and can break the barrier on overflow, insufficient magicules or stronger attacks. Upkeep does not silently rearm a broken barrier; turn the passive off and back on to arm it again. Surviving shields persist across relogs.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **10 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

## Suggested combo

Place Scarlet Undertow to gather enemies → send Crushing Current through the drenched group. Toggle Water Veil when you need sustained defense.

## Strengths and limitations

The veil must remain funded. Native barriers follow Tensura’s rules and do not guarantee immunity to stronger attacks. Its retaliation requires health damage, a hostile living attacker and line of sight. Knockback resistance reduces tides and wave carrying.

## Native barrier HUD

![Native shield HUD counter](../media/water-veil-hud.png)

The Unique passive’s native shield counter in Wild Hunt 0.6.0. Its Ultimate uses a higher capacity.

## Related pages

[Skills](Skills.md) · [Mechanics](Mechanics.md) · [Evolution](Evolution.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.6.0
