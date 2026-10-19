# Varuna, Lord of Tides

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

![Varuna, Lord of Tides](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/varuna.png?raw=true)

| Skill data | Varuna, Lord of Tides |
| --- | --- |
| Category | Ultimate Skill |
| Inspiration | Uth Duna |
| Combat role | Moving tides / native barriers |
| Active modes | 3 |
| Toggleable passive | Water Veil |
| Passive upkeep | 40 aura / second |
| Registry ID | `wildhunt:varuna` |
| Previous | [Scarlet Deluge](https://github.com/Bithio999/MC/wiki/Scarlet-Deluge) |
| Evolution | — |

## Overview

A water-control kit with a travelling wave, an aimed whirlpool, and a sustained defensive passive. Its defense uses Tensura’s physical and magic barriers instead of absorption hearts.

## Acquisition

Evolve [Scarlet Deluge](https://github.com/Bithio999/MC/wiki/Scarlet-Deluge) as Sovereign Hunter with 2,000,000 base EP and mastery of that Unique. Use `/wildhunt ascend uth_duna`. It replaces only the matching Unique; the Ultimate starts with its own mastery.

## Active modes

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Crushing Current | 640 | 2 s | 195 |
| 2. Scarlet Undertow | 2,000 | 6 s | 135 |
| 3. Crimson Ocean | 6,000 | 60 s | See signature below |

**0.7.0 damage adjustment:** multiply every raw damage value below by **0.90**. This applies to direct hits, ricochets, fields, counters and signature pulses. Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

### Crushing Current

Send a broad wave up to 26 blocks over about sixteen game ticks. It carries and drenches each enemy it reaches, damaging a given entity at most once per wave. Its front is 5 blocks wide on either side. Previously drenched enemies take 50% extra primary damage. Drenched is a single twelve-second mark, never a stack counter.

### Scarlet Undertow

Place a six-second whirlpool at the aim point, up to 36 blocks away along the crosshair ray. The radius is 13 blocks. A block or water surface under the crosshair sets the position; a miss places it at the bounded ray endpoint. It hits immediately and once per second, with a gentle inward current between pulses. Raw pulse damage is 54; the table lists opening damage.

### Crimson Ocean

For ten seconds, alternate inward and outward tides within fourteen blocks around the caster. Incoming tides gather; outgoing tides push away and slightly upward. Raw damage is 135 on the opening pulse and 81 on later pulses, before mastery and native modifiers.

## Toggleable passive: Water Veil

While toggled on, gain 25 armor, 36 physical barrier, 36 magic barrier and 100% water boost. These native attributes participate in Tensura’s damage handling and visible barrier layer. Water breathing remains active while toggled on. No absorption hearts are added. A hostile hit that removes health triggers a 25-raw water counter (75 as Varuna) against a visible attacker within twelve blocks, with a 1-second cooldown.

The passive also arms a **250-capacity native Multilayer Barrier**, which appears in Tensura’s shield HUD counter when its Barrier decoration is enabled. The counter is a per-hit blocking capacity, not absorption hearts or a custom health bar. Native rules spend magicules on blocked damage and can break the barrier on overflow, insufficient magicules or stronger attacks. While still toggled on, a broken barrier automatically rearms on the first paid upkeep after **ten seconds without an incoming hit**. Any new hit restarts the delay, including hits completely blocked by a surviving shield. Retoggling does not bypass the delay. Surviving shields persist across relogs.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **40 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

No constant bubble orbit is emitted while the passive is enabled. Native barrier rendering and the shield HUD remain. Active waves use a curled teal water mesh with foam, and Undertow uses rotating teal/coral spiral ribbons.

## Suggested combo

Place Scarlet Undertow to gather enemies → send Crushing Current through the drenched group. Toggle Water Veil when you need sustained defense.

## Strengths and limitations

The veil must remain funded. Native barriers follow Tensura’s rules and do not guarantee immunity to stronger attacks. Its retaliation requires health damage, a hostile living attacker and line of sight. Knockback resistance reduces tides and wave carrying.

## Active visuals

![Crushing Current foam crest](https://github.com/Bithio999/MC/blob/main/wiki/media/crushing-current.png?raw=true)

![Scarlet Undertow spiral ribbons](https://github.com/Bithio999/MC/blob/main/wiki/media/scarlet-undertow.png?raw=true)

Captured in the 0.7.0 Minecraft client. These are short-lived active arts, not ambient toggle effects.

## Native barrier HUD

![Native shield HUD counter](https://github.com/Bithio999/MC/blob/main/wiki/media/water-veil-hud.png?raw=true)

The Unique passive’s native shield counter in Wild Hunt 0.6.0. Its Ultimate uses a higher capacity.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

---

**Categories:** Tensura: Wild Hunt · Version 0.7.0
