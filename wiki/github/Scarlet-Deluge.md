# Scarlet Deluge

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

**Scarlet Deluge** is a Unique Skill inspired by **Uth Duna**. It combines travelling currents, aimed whirlpools, and native barriers.

![Scarlet Deluge](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/scarlet_deluge.png?raw=true)

| Skill data | Scarlet Deluge |
| --- | --- |
| **Information** | |
| Type | Unique Skill |
| Inspiration | Uth Duna |
| Damage type | Water elemental |
| Damage ID | `tensura:water_elemental` |
| Combat role | Moving tides / native barriers |
| Active modes | 2 |
| Toggleable passive | Water Veil |
| Passive upkeep | 10 aura / second |
| Registry ID | `wildhunt:scarlet_deluge` |
| **Evolution** | |
| Previous | — |
| Evolution | Standalone Unique |
| **Obtaining** | |
| Method | Reincarnation / starting skill roll |

All damaging arts in this skill use **water elemental damage**, including its area hits and pulses.

## Usage

### Passive

[Passive - Toggle] — **Water Veil**

| Ability information | Value |
| --- | --- |
| Damage type | [Water elemental](https://github.com/Bithio999/MC/wiki/Damage-Types#water) |
| Damage ID | `tensura:water_elemental` |
| Effect | Retaliation deals water elemental damage; the barrier itself deals none. |

While toggled on, gain 10 armor, 18 physical barrier, 18 magic barrier and 50% water boost. These native attributes participate in Tensura’s damage handling and visible barrier layer. Water breathing remains active while toggled on. No absorption hearts are added. A hostile hit that removes health triggers a 25-raw water counter against a visible attacker within twelve blocks, with a 3-second cooldown.

The passive also arms a **100-capacity native Multilayer Barrier**, which appears in Tensura’s shield HUD counter when its Barrier decoration is enabled. The counter is a per-hit blocking capacity, not absorption hearts or a custom health bar. Native rules spend magicules on blocked damage and can break the barrier on overflow, insufficient magicules or stronger attacks. While still toggled on, a broken barrier automatically rearms on the first paid upkeep after **ten seconds without an incoming hit**. Any new hit restarts the delay, including hits completely blocked by a surviving shield. Retoggling does not bypass the delay. Surviving shields persist across relogs.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **10 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

No constant bubble orbit is emitted while the passive is enabled. Native barrier rendering and the shield HUD remain. Active waves use a curled teal water mesh with foam, and Undertow uses rotating teal/coral spiral ribbons.

### Active

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Crushing Current | 160 | 3 s | 65 |
| 2. Scarlet Undertow | 500 | 7 s | 45 |

**Damage calculation:** multiply the listed base values by **0.90**, then by the mastery multiplier (up to **1.50**). Elemental boosts, defenses, barriers, and situational modifiers also apply. EP does not multiply damage.

#### Crushing Current

[Active - Press] — **Crushing Current**

| Ability information | Value |
| --- | --- |
| Damage type | [Water elemental](https://github.com/Bithio999/MC/wiki/Damage-Types#water) |
| Damage ID | `tensura:water_elemental` |
| Effect | Direct hits use this native element. |

Send a broad wave up to 18 blocks over about sixteen game ticks. It carries and drenches each enemy it reaches, damaging a given entity at most once per wave. Its front is 4 blocks wide on either side. Previously drenched enemies take 50% extra primary damage. Drenched is a single twelve-second mark, never a stack counter.

#### Scarlet Undertow

[Active - Press] — **Scarlet Undertow**

| Ability information | Value |
| --- | --- |
| Damage type | [Water elemental](https://github.com/Bithio999/MC/wiki/Damage-Types#water) |
| Damage ID | `tensura:water_elemental` |
| Effect | Direct hits use this native element. |

Place a six-second whirlpool at the aim point, up to 24 blocks away along the crosshair ray. The radius is 9 blocks. A block or water surface under the crosshair sets the position; a miss places it at the bounded ray endpoint. It hits immediately and once per second, with a gentle inward current between pulses. Raw pulse damage is 18; the table lists opening damage.

## Obtaining

Roll through Tensura’s normal reincarnation system. These Uniques join the configured primary pool; no particular roll is guaranteed. There are no skill trials, boss-awarded skills or race requirements to roll.

## Evolution

This skill remains a standalone Unique. It currently has no combination Ultimate recipe.

## Suggested combo

Place Scarlet Undertow to gather enemies → send Crushing Current through the drenched group. Toggle Water Veil when you need sustained defense.

## Strengths and limitations

The veil must remain funded. Native barriers follow Tensura’s rules and do not guarantee immunity to stronger attacks. Its retaliation requires health damage, a hostile living attacker and line of sight. Knockback resistance reduces tides and wave carrying.

## Active visuals

![Crushing Current foam crest](https://github.com/Bithio999/MC/blob/main/wiki/media/crushing-current.png?raw=true)

![Scarlet Undertow spiral ribbons](https://github.com/Bithio999/MC/blob/main/wiki/media/scarlet-undertow.png?raw=true)

These visuals appear during active arts.

## Native barrier HUD

![Native shield HUD counter](https://github.com/Bithio999/MC/blob/main/wiki/media/water-veil-hud.png?raw=true)

The Unique passive’s native shield counter. Its Ultimate uses a higher capacity.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

---

**Categories:** Tensura Myths · Skills
