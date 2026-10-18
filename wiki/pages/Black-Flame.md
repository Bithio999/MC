# Black Flame

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Black Flame](../../src/main/resources/assets/wildhunt/textures/skill/black_flame.png)

| Skill data | Black Flame |
| --- | --- |
| Category | Unique Skill |
| Inspiration | Nu Udra |
| Combat role | Trap placement / oil detonation |
| Resource | Aura |
| Modes | 3 |
| Registry ID | `wildhunt:black_flame` |
| Previous | — |
| Evolution | [Agni, Lord of the Black Flame](Agni.md) |

## Overview

A setup-and-detonate art with limited, owned oil pools. Its lash grabs targets, its mantle oils melee victims, and ignition only burns the caster’s own pool network. Enemies outside those locations avoid the detonation.

## Acquisition

Roll this skill through Tensura’s normal reincarnation system. It is added to the configured primary pool; it is not guaranteed and has no race requirement to roll. There are no skill trials. Starting skill count, additional rolls and truly-unique restrictions come from the server’s Tensura settings.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Oil Lash | 160 | 3 s | 70 |
| 2. Blackflame Ignition | 800 | 8 s | 90 initial / 24 pulse |
| 3. Furnace Mantle | 350 | 20 s | — |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Oil Lash

Lashes up to 4 enemies in a forward fan within 14 blocks, pulls them toward you, and adds one of three oil stacks lasting fifteen seconds. Curved tentacle trails connect caster and victims. Leaves an oil pool at the last struck target; an empty cast places one about ten blocks ahead. Maximum owned pools: 3. Oldest pools are replaced first.

### Blackflame Ignition

Ignites your valid oil pools for six seconds. Each pool damages enemies within 3 blocks; overlapping pools cannot hit one enemy twice in the same pulse. Initial ignition consumes your own oil stacks for +60% damage per stack, up to 2.8× base. Burning pools pulse once per second and apply native Black Burn. If you have no pools, ignition places a fallback pool at your aim point.

### Furnace Mantle

For 15 seconds, gain 8 armor, fire resistance, 50% melee attack and 50% flame boost. Successful ordinary melee attacks coat living victims with your oil. Skill damage does not recursively count as an ordinary melee hit.

## Suggested combo

Pull a target into an oil pool with Oil Lash → add oil with lashes or mantle-assisted melee → use Blackflame Ignition. Layer pools to cover escape paths rather than stacking them for duplicate hits.

## Strengths and limitations

Only your oil amplifies your ignition. Pools expire after fifteen seconds when unlit or six seconds after ordinary ignition. They must be in the same dimension and within thirty-two blocks when controlled. Bosses can resist pulls, fire and status effects.

## Evolution notes

Four lash targets, fourteen-block reach, three pools, three-block ignition radius, and a 15-second mantle. Six lash targets, twenty-two-block reach, five pools, four-block ignition radius, stronger Black Burn, a 30-second mantle, and the five-pool caldera.

## Visual effects

Native black clouds shape curved tentacle trails, Black Fire marks ignition, and Flame magic circles identify the art. Pool footprints show the areas that can detonate.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
