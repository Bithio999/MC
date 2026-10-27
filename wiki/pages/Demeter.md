# Demeter, Lady of Sanctuary

[Main Page](../README.md) · [Skills](Skills.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Demeter, Lady of Sanctuary** is an Ultimate Skill focused on **ice orchard, one-use allied rescue seeds and delayed crystal blooms**.

![Demeter, Lady of Sanctuary](../../src/main/resources/assets/wildhunt/textures/skill/demeter.png)

| Skill data | Demeter, Lady of Sanctuary |
| --- | --- |
| **Information** | |
| Type | Ultimate Skill |
| Combat role | Ice orchard, rescue seeds and delayed crystal blooms |
| Active modes | 3 |
| Resource | Aura |
| Toggleable passive | Evergreen Sanctuary |
| Passive upkeep | 110 aura / second |
| Registry ID | `wildhunt:demeter` |
| **Obtaining** | |
| Method | Synthesis: master both ingredients; 2M total EP |
| **Evolution** | |
| Evolution | [Aion](Aion.md), [Thoth](Thoth.md) or [Lugh](Lugh.md) |

## Usage

### Passive

[Passive - Toggle] — **Evergreen Sanctuary**

| Ability information | Value |
| --- | --- |
| Damage type | None — support / defense |
| Damage ID | — |
| Upkeep | 110 aura / second |

Gain **200% increased natural SHP regeneration**, **100% Water boost** and **0.6 knockback resistance**. Water boost also strengthens native Ice attacks where they use the shared Water/Ice attribute. Ordinary elemental sources use their strongest contribution; Godly boosts remain separate. No barrier is generated.

Enable the native skill toggle in Ability Selection. The first second is paid on enabling; upkeep is paid every twenty game ticks. It turns off when unfunded or blocked by native activation rules. Turning it off removes only its own modifiers. It is separate from active mode cycling.

### Active

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Winter Orchard | 12,000 | 20 s | 500 per enemy pulse |
| 2. Perennial Seed | 10,000 | 24 s | — |
| 3. Winter Harvest | 20,000 | 24 s | 1,400; 2,200 when consuming a live orchard |

Damage listed here is before native combat handling and uses **1 + 0.5 × mastery fraction**, capped at **1.5×**. EP never multiplies their damage. Ultimate damaging arts use native **level-1 resistance penetration**; elemental boosts, resistance attributes, native nullification fallback, barriers, authority and hit immunity retain their independent rules.

#### Winter Orchard

[Active - Press] — **Winter Orchard**

| Ability information | Value |
| --- | --- |
| Damage type | Ice elemental against enemies; allied healing has no damage type |
| Damage ID | `tensura:ice_elemental` |
| Resource cost | 12,000 aura |
| Cooldown | 20 seconds |
| Raw damage | 500 per enemy pulse |

Plant a **twelve-block-radius** crystal orchard within 32 blocks for **twelve seconds**. Each second, heal up to eight allies for **10% maximum HP and 10% maximum SHP**, and strike up to eight visible enemies for ice damage with native Chill II for two seconds. Healing respects the same ally/owned-pet rules as Living Grove. Only one orchard per owner.

#### Perennial Seed

[Active - Press] — **Perennial Seed**

| Ability information | Value |
| --- | --- |
| Damage type | None — recovery / cleanse |
| Damage ID | — |
| Resource cost | 10,000 aura |
| Cooldown | 24 seconds |
| Duration | 12 seconds or until triggered |

Plant one rescue seed on yourself and eligible allies within **sixteen blocks**, up to eight recipients total. Placing a seed cleanses ordinary Poison, Wither, Chill and Webbed effects. Effects explicitly attributed to Godly abilities are preserved.

When a living recipient's HP is **35% or lower**, the seed is consumed, restoring **40% maximum HP and 40% maximum SHP**, capped normally, and repeating the selected cleanse. It also triggers if the recipient is already wounded when planted. Each recipient has one seed; another placement replaces it rather than stacking recovery charges.

A small, quiet crystalline flower marks the active seed and blooms when consumed. It grants no absorption or barrier. It cannot resurrect a dead recipient or prevent a lethal hit that kills before the next recovery tick. Forgetting Demeter or lifecycle cleanup removes its tracked recipients' seeds; expiry removes unused markers.

#### Winter Harvest

[Active - Press] — **Winter Harvest**

| Ability information | Value |
| --- | --- |
| Damage type | Ice elemental |
| Damage ID | `tensura:ice_elemental` |
| Resource cost | 20,000 aura |
| Cooldown | 24 seconds |
| Raw damage | 1,400; 2,200 when consuming a live orchard |

Select up to six enemy positions within twelve blocks of your orchard, or an aimed location within 32 blocks if no orchard is active. Consume the live orchard to empower the blooms and immediately restore eligible allies within twelve blocks for **20% maximum HP and SHP**. Each marked position erupts after 1.2 seconds in a three-block radius. Empty target areas create one central warning. Blooms remain fixed, so enemies can leave before impact.

## Obtaining

Open **Skills → Ultimate → Synthesis**. Choose Demeter and combine **[Verdant Keeper](Verdant-Keeper.md) + [Frostbound Colossus](Frostbound-Colossus.md)**, with both mastered and at least **2,000,000 total EP**. Any race can synthesize. Both ingredients are replaced only after native storage accepts the result. Other skills remain learned. All nine presets and the longest remaining ingredient cooldown carry over; the new Ultimate starts with its own mastery.

## Suggested combo

Set Winter Orchard beneath your team → plant Perennial Seed before a dangerous exchange → consume the orchard with Winter Harvest when enemies commit to its area.

## Strengths and limitations

Harvest ends the orchard’s ongoing healing and pulse damage. Rescue seeds are one-use recovery effects and require their recipient to survive until the recovery tick.

Attacks respect teams, owned pets, creative/spectator immunity and server PvP settings. Native status resistances apply. World effects are bounded, expire automatically and do not damage terrain. Anchored effects do not persist across death, relog or dimension changes.

## Related pages

[Skills](Skills.md) · [Skill synthesis](Skill-Synthesis.md) · [Damage types](Damage-Types.md) · [Mechanics](Mechanics.md)

## Visual effects

Winter Orchard grows an ice-veined grove with curved crystal branches, snowflake tips, roots and flowing restorative ripples. Perennial Seed follows its actual recipient with a small flower marker and opens into a crystalline recovery bloom when triggered. Winter Harvest first shows its frost warning, then opens into a large eight-petalled crystalline bloom at the strike.

![Winter Orchard’s luminous veins and crystal branches](../media/visual-demeter.png)

[Visual effects gallery and display settings](Visual-Effects.md).

---

**Categories:** Tensura Myths · Skills
