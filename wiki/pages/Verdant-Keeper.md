# Verdant Keeper

[Main Page](../README.md) · [Skills](Skills.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Verdant Keeper** is a Unique Skill focused on **allied hp/shp support and forward root control**.

![Verdant Keeper](../../src/main/resources/assets/wildhunt/textures/skill/verdant_keeper.png)

| Skill data | Verdant Keeper |
| --- | --- |
| **Information** | |
| Type | Unique Skill |
| Combat role | Allied HP/SHP support and forward root control |
| Active modes | 2 |
| Resource | Aura |
| Toggleable passive | Verdant Heart |
| Passive upkeep | 20 aura / second |
| Registry ID | `wildhunt:verdant_keeper` |
| **Obtaining** | |
| Method | Reincarnation starting-skill roll |
| **Evolution** | |
| Evolution | [Synthesis](Skill-Synthesis.md) |

## Usage

### Passive

[Passive - Toggle] — **Verdant Heart**

| Ability information | Value |
| --- | --- |
| Damage type | None — support / defense |
| Damage ID | — |
| Upkeep | 20 aura / second |

Increase native spiritual-health regeneration by 50%. This modifies natural recovery; it does not create an additional periodic HP-heal loop.

Enable the native skill toggle in Ability Selection. The first second is paid on enabling; upkeep is paid every twenty game ticks. It turns off when unfunded or blocked by native activation rules. Turning it off removes only its own modifiers. It is separate from active mode cycling.

### Active

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Living Grove | 700 | 14 s | — |
| 2. Briar Grasp | 300 | 5 s | 190 |

Damage listed here is before native combat handling and uses **1 + 0.5 × mastery fraction**, capped at **1.5×**. These new kits do not use the apex skills’ 0.90 multiplier or the former 3× Ultimate multiplier. EP never multiplies their damage. Native elemental boosts, authority, resistances, barriers and hit immunity apply.

#### Living Grove

[Active - Press] — **Living Grove**

| Ability information | Value |
| --- | --- |
| Damage type | None |
| Damage ID | — |
| Resource cost | 700 aura |
| Cooldown | 14 seconds |
| Raw damage | — |

Grow a stationary grove within 24 blocks. For eight seconds it restores 5% maximum HP and 4% maximum SHP each second to up to eight beneficiaries within seven blocks. Beneficiaries are yourself, teammates/allies and your owned pets; neutral strangers are excluded. Healing caps at each pool’s current maximum. Only one grove per owner; other allies need your line of sight.

#### Briar Grasp

[Active - Press] — **Briar Grasp**

| Ability information | Value |
| --- | --- |
| Damage type | Earth elemental |
| Damage ID | `tensura:earth_elemental` |
| Resource cost | 300 aura |
| Cooldown | 5 seconds |
| Raw damage | 190 |

Lash up to six visible enemies in a forward fan within twelve blocks. Successful hits attempt to apply native Webbed for 3.5 seconds. Animated briars erupt around the selected victims. Native resistance and rank rules still govern the status.

## Obtaining

Roll through Tensura’s normal reincarnation pool alongside the other seven Myths Uniques and configured native candidates. The roll is random; no particular skill is guaranteed. There is no race, trial or boss-kill requirement to roll.

## Suggested combo

Place Living Grove where your allies can fight → use Briar Grasp to hold approaching enemies outside their formation.

## Strengths and limitations

The grove stays where it was cast. It heals allies instead of damaging enemies. Webbed is a native effect and is not guaranteed against every target.

Attacks respect teams, owned pets, creative/spectator immunity and server PvP settings. Native status resistances apply. World effects are bounded, expire automatically and do not damage terrain. Anchored effects do not persist across death, relog or dimension changes.

## Related pages

[Skills](Skills.md) · [Skill synthesis](Skill-Synthesis.md) · [Damage types](Damage-Types.md) · [Mechanics](Mechanics.md)

---

**Categories:** Tensura Myths · Skills
