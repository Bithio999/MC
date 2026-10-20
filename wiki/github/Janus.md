# Janus, Lord of Crossroads

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

**Janus, Lord of Crossroads** is an Ultimate Skill focused on **linked gates, opposing spatial blades and safe displacement**.

![Janus, Lord of Crossroads](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/janus.png?raw=true)

| Skill data | Janus, Lord of Crossroads |
| --- | --- |
| **Information** | |
| Type | Ultimate Skill |
| Combat role | Linked gates, opposing spatial blades and safe displacement |
| Active modes | 3 |
| Resource | Aura |
| Toggleable passive | Threshold Dominion |
| Passive upkeep | 70 aura / second |
| Registry ID | `wildhunt:janus` |
| **Obtaining** | |
| Method | Synthesis: master both ingredients; 2M base EP |
| **Evolution** | |
| Evolution | [Aion](https://github.com/Bithio999/MC/wiki/Aion) or [Thoth](https://github.com/Bithio999/MC/wiki/Thoth) |

## Usage

### Passive

[Passive - Toggle] — **Threshold Dominion**

| Ability information | Value |
| --- | --- |
| Damage type | None — support / defense |
| Damage ID | — |
| Upkeep | 70 aura / second |

Gain 35% movement speed, 30% attack speed, 0.5 knockback resistance and 60% Space boost. Threshold Dominion rewards movement and spatial offense.

Elemental boosts use the [strongest ordinary source](https://github.com/Bithio999/MC/wiki/Mechanics#elemental-bonus-sources); Godly bonuses apply separately.

Enable the native skill toggle in Ability Selection. The first second is paid on enabling; upkeep is paid every twenty game ticks. It turns off when unfunded or blocked by native activation rules. Turning it off removes only its own modifiers. It is separate from active mode cycling.

### Active

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Twin Gates | 1,600 | 3 s | — |
| 2. Parallax Snare | 2,200 | 7 s | 780 |
| 3. Crossroads Edict | 4,500 | 18 s | 620 |

Damage listed here is before native combat handling and uses **1 + 0.5 × mastery fraction**, capped at **1.5×**. These new kits do not use the apex skills’ 0.90 multiplier or the former 3× Ultimate multiplier. EP never multiplies their damage. Native elemental boosts, authority, resistances, barriers and hit immunity apply.

#### Twin Gates

[Active - Press] — **Twin Gates**

| Ability information | Value |
| --- | --- |
| Damage type | None |
| Damage ID | — |
| Resource cost | 1,600 aura |
| Cooldown | 3 seconds |
| Raw damage | — |

First cast places a gate at your feet and a second safe point up to 36 blocks along the aimed route. The pair lasts twenty seconds. Recast within four blocks of either gate to travel to the other, preserving horizontal and vertical momentum. Each placement or successful transit costs 1,600 aura and has a three-second cooldown. A blocked destination or invalid placement is free and cannot teleport you into solid terrain.

#### Parallax Snare

[Active - Press] — **Parallax Snare**

| Ability information | Value |
| --- | --- |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Resource cost | 2,200 aura |
| Cooldown | 7 seconds |
| Raw damage | 780 |

Mark a point within 36 blocks. Opposing spatial blades close around a six-block radius and strike after 0.8 seconds. The warning stays fixed; up to eight visible enemies can be hit.

#### Crossroads Edict

[Active - Press] — **Crossroads Edict**

| Ability information | Value |
| --- | --- |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Resource cost | 4,500 aura |
| Cooldown | 18 seconds |
| Raw damage | 620 |

Choose a field center within 32 blocks. Up to six visible enemies within ten blocks are displaced to their mirrored horizontal positions across the center. Each destination is checked against collision, build height and world border. An unsafe displacement leaves that enemy in place; the spatial hit still applies. Successful hits attempt native Movement Interference for 2.5 seconds.

## Obtaining

Open **Skills → Ultimate → Synthesis**. Choose Janus and combine **[Threadweaver](https://github.com/Bithio999/MC/wiki/Threadweaver) + [Mirrorbound](https://github.com/Bithio999/MC/wiki/Mirrorbound)**, with both mastered and at least **2,000,000 base EP**. Any race can synthesize. Both ingredients are replaced only after native storage accepts the result. Other skills remain learned. All nine presets and the longest remaining ingredient cooldown carry over; the new Ultimate starts with its own mastery.

## Suggested combo

Place Twin Gates across a safe lane → cross to change your attack angle → mark Parallax Snare and use Crossroads Edict to disturb enemy positioning.

## Strengths and limitations

Only your owner can use the gates. Gates do not transport across dimensions. Hostile teleportation never bypasses destination safety; native defenses can still block status or damage.

Attacks respect teams, owned pets, creative/spectator immunity and server PvP settings. Native status resistances apply. World effects are bounded, expire automatically and do not damage terrain. Anchored effects do not persist across death, relog or dimension changes.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Skill synthesis](https://github.com/Bithio999/MC/wiki/Skill-Synthesis) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics)

---

**Categories:** Tensura Myths · Skills
