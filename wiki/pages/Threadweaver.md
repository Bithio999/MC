# Threadweaver

[Main Page](../README.md) · [Skills](Skills.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Threadweaver** is a Unique Skill focused on **single-target leash and spatial severance**.

![Threadweaver](../../src/main/resources/assets/wildhunt/textures/skill/threadweaver.png)

| Skill data | Threadweaver |
| --- | --- |
| **Information** | |
| Type | Unique Skill |
| Combat role | Single-target leash and spatial severance |
| Active modes | 2 |
| Resource | Aura |
| Toggleable passive | Silken Instinct |
| Passive upkeep | 25 aura / second |
| Registry ID | `wildhunt:threadweaver` |
| **Obtaining** | |
| Method | Reincarnation starting-skill roll |
| **Evolution** | |
| Evolution | [Synthesis](Skill-Synthesis.md) |

## Usage

### Passive

[Passive - Toggle] — **Silken Instinct**

| Ability information | Value |
| --- | --- |
| Damage type | None — support / defense |
| Damage ID | — |
| Upkeep | 25 aura / second |

Gain 30% movement speed and 25% attack speed. This creates no charges or passive stacks.

Enable the native skill toggle in Ability Selection. The first second is paid on enabling; upkeep is paid every twenty game ticks. It turns off when unfunded or blocked by native activation rules. Turning it off removes only its own modifiers. It is separate from active mode cycling.

### Active

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Loom Needle | 600 | 4 s | 450 |
| 2. Sever Thread | 1,000 | 6 s | 800 |

Damage listed here is before native combat handling and uses **1 + 0.5 × mastery fraction**, capped at **1.5×**. EP never multiplies their damage. Native elemental boosts, authority, resistances, barriers and hit immunity apply.

#### Loom Needle

[Active - Press] — **Loom Needle**

| Ability information | Value |
| --- | --- |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Resource cost | 600 aura |
| Cooldown | 4 seconds |
| Raw damage | 450 |

Aim at one visible enemy within 28 blocks. The hit pins a silk anchor at its current position for six seconds. The enemy moves freely within three blocks of that anchor; outside the leash it is pulled back, respecting knockback resistance. The tether breaks if the enemy dies, leaves 40 blocks from you or passes out of your line of sight. One tether per owner.

#### Sever Thread

[Active - Press] — **Sever Thread**

| Ability information | Value |
| --- | --- |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Resource cost | 1,000 aura |
| Cooldown | 6 seconds |
| Raw damage | 800 |

Consume your live tether and burst around its current victim, hitting up to six eligible enemies in a four-block radius. The victim must remain within 40 blocks and visible. Casting without a valid tether is free and starts no cooldown.

## Obtaining

Roll through Tensura’s normal reincarnation pool alongside the other seven Myths Uniques and configured native candidates. The roll is random; no particular skill is guaranteed. There is no race, trial or boss-kill requirement to roll.

## Evolution

Combine with Mirrorbound for [Janus](Janus.md), or with Rune Smith for [Anansi](Anansi.md), at 2M base EP and mastery of both ingredients. A mastered Threadweaver can also be combined with a mastered Ultimate during [Lugh](Lugh.md) ascension.

## Suggested combo

Land Loom Needle → let an enemy strain against the fixed leash → Sever Thread when enemies cluster nearby.

## Strengths and limitations

A leash is a pull, not a full stun. Knockback resistance weakens it. Sever Thread consumes the tether even if native defenses block the damage.

Attacks respect teams, owned pets, creative/spectator immunity and server PvP settings. Native status resistances apply. World effects are bounded, expire automatically and do not damage terrain. Anchored effects do not persist across death, relog or dimension changes.

## Related pages

[Skills](Skills.md) · [Skill synthesis](Skill-Synthesis.md) · [Damage types](Damage-Types.md) · [Mechanics](Mechanics.md)

## Visual effects

Loom Needle uses four braided strands, travelling pulses and an oriented silk-web anchor at its target. Sever Thread closes that web inward into a pink spatial snap. Its fine filaments remain distinct from Janus’s large architectural cuts.

![Braided silk and its target-facing web anchor](../media/visual-threadweaver.png)

[Visual effects gallery and display settings](Visual-Effects.md).

---

**Categories:** Tensura Myths · Skills
