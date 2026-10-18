# Agni, Lord of the Black Flame

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Agni, Lord of the Black Flame](../../src/main/resources/assets/wildhunt/textures/skill/agni.png)

| Skill data | Agni, Lord of the Black Flame |
| --- | --- |
| Category | Ultimate Skill |
| Inspiration | Nu Udra |
| Combat role | Trap placement / oil detonation |
| Resource | Aura |
| Modes | 4 |
| Registry ID | `wildhunt:agni` |
| Previous | [Black Flame](Black-Flame.md) |
| Evolution | — |

## Overview

A setup-and-detonate art with limited, owned oil pools. Its lash grabs targets, its mantle oils melee victims, and ignition only burns the caster’s own pool network. Enemies outside those locations avoid the detonation.

## Acquisition

Evolve from [Black Flame](Black-Flame.md) as Sovereign Hunter with 2,000,000 base EP and mastery of that matching Unique skill. Use `/wildhunt ascend nu_udra`. The Ultimate replaces only its source Unique; mastery of the new Ultimate starts separately.

## Modes

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Oil Lash | 640 | 2 s | 210 |
| 2. Blackflame Ignition | 3,200 | 7 s | 270 initial / 72 pulse |
| 3. Furnace Mantle | 1,400 | 19 s | — |
| 4. Black Sun Caldera | 6,000 | 60 s | Signature-specific; see below |

Raw damage in this table already includes the Ultimate’s 3× base multiplier where applicable. EP, mastery, elemental boosts, defenses and combo modifiers apply separately. [Damage formula](Mechanics.md#damage-and-mastery).

### Oil Lash

Lashes up to 6 enemies in a forward fan within 22 blocks, pulls them toward you, and adds one of three oil stacks lasting fifteen seconds. Curved tentacle trails connect caster and victims. Leaves an oil pool at the last struck target; an empty cast places one about ten blocks ahead. Maximum owned pools: 5. Oldest pools are replaced first.

### Blackflame Ignition

Ignites your valid oil pools for six seconds. Each pool damages enemies within 4 blocks; overlapping pools cannot hit one enemy twice in the same pulse. Initial ignition consumes your own oil stacks for +60% damage per stack, up to 2.8× base. Burning pools pulse once per second and apply native Black Burn. If you have no pools, ignition places a fallback pool at your aim point.

### Furnace Mantle

For 30 seconds, gain 18 armor, fire resistance, 100% melee attack and 100% flame boost. Successful ordinary melee attacks coat living victims with your oil. Skill damage does not recursively count as an ordinary melee hit.

### Black Sun Caldera

Establishes five ignited pools in a ring six blocks around the caster. For ten seconds, the caldera pulses through valid owned pools, with a thirty-two-block control leash. This is a location-based trap network, not a caster-centered damage aura.

## Suggested combo

Pull a target into an oil pool with Oil Lash → add oil with lashes or mantle-assisted melee → use Blackflame Ignition. Layer pools to cover escape paths rather than stacking them for duplicate hits.

## Strengths and limitations

Only your oil amplifies your ignition. Pools expire after fifteen seconds when unlit or six seconds after ordinary ignition. They must be in the same dimension and within thirty-two blocks when controlled. Bosses can resist pulls, fire and status effects.

## Evolution notes

Six lash targets, twenty-two-block reach, five pools, four-block ignition radius, stronger Black Burn, a 30-second mantle, and the five-pool caldera.

## Visual effects

Native black clouds shape curved tentacle trails, Black Fire marks ignition, and Flame magic circles identify the art. Pool footprints show the areas that can detonate.

## Related pages

[Skills](Skills.md) · [Human evolution](Evolution.md) · [Combat mechanics](Mechanics.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
