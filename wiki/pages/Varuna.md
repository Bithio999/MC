# Varuna, Lord of Tides

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Varuna, Lord of Tides](../../src/main/resources/assets/wildhunt/textures/skill/varuna.png)

| Skill data | Varuna, Lord of Tides |
| --- | --- |
| Category | Ultimate Skill |
| Inspiration | Uth Duna |
| Combat role | Moving tides / native barriers |
| Active modes | 3 |
| Toggleable passive | Water Veil |
| Passive upkeep | 40 aura / second |
| Registry ID | `wildhunt:varuna` |
| Previous | [Scarlet Deluge](Scarlet-Deluge.md) |
| Evolution | — |

## Overview

A water-control kit with a travelling wave, an aimed whirlpool, and a sustained defensive passive. Its defense uses Tensura’s physical and magic barriers instead of absorption hearts.

## Acquisition

Evolve [Scarlet Deluge](Scarlet-Deluge.md) as Sovereign Hunter with 2,000,000 base EP and mastery of that Unique. Use `/wildhunt ascend uth_duna`. It replaces only the matching Unique; the Ultimate starts with its own mastery.

## Active modes

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Crushing Current | 640 | 2 s | 195 |
| 2. Scarlet Undertow | 2,000 | 6 s | 135 |
| 3. Crimson Ocean | 6,000 | 60 s | See signature below |

Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

### Crushing Current

Send a broad wave up to 26 blocks over about sixteen game ticks. It carries and drenches each enemy it reaches, damaging a given entity at most once per wave. Its front is 5 blocks wide on either side. Previously drenched enemies take 50% extra primary damage. Drenched is a single twelve-second mark, never a stack counter.

### Scarlet Undertow

Place a six-second whirlpool at the aim point, up to six blocks away. The radius is 13 blocks. It hits immediately and once per second, with a gentle inward current between pulses. Raw pulse damage is 54; the table lists opening damage.

### Crimson Ocean

For ten seconds, alternate inward and outward tides within fourteen blocks around the caster. Incoming tides gather; outgoing tides push away and slightly upward. Raw damage is 135 on the opening pulse and 81 on later pulses, before mastery and native modifiers.

## Toggleable passive: Water Veil

While toggled on, gain 25 armor, 40 physical barrier, 40 magic barrier and 100% water boost. These native attributes participate in Tensura’s damage handling and visible barrier layer. Water breathing remains active while toggled on. No absorption hearts are added. A hostile hit that removes health triggers a 25-raw water counter (75 as Varuna) against a visible attacker within twelve blocks, with a 1-second cooldown.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **40 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

## Suggested combo

Place Scarlet Undertow to gather enemies → send Crushing Current through the drenched group. Toggle Water Veil when you need sustained defense.

## Strengths and limitations

The veil must remain funded. Native barriers follow Tensura’s rules and do not guarantee immunity to stronger attacks. Its retaliation requires health damage, a hostile living attacker and line of sight. Knockback resistance reduces tides and wave carrying.

## Related pages

[Skills](Skills.md) · [Mechanics](Mechanics.md) · [Evolution](Evolution.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.5.0
