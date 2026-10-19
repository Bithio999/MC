# Raijin, Lord of Thunder

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

![Raijin, Lord of Thunder](../../src/main/resources/assets/wildhunt/textures/skill/raijin.png)

| Skill data | Raijin, Lord of Thunder |
| --- | --- |
| Category | Ultimate Skill |
| Inspiration | Rey Dau |
| Combat role | Ricochet lightning / mobility |
| Active modes | 3 |
| Toggleable passive | Storm Mantle |
| Passive upkeep | 40 aura / second |
| Registry ID | `wildhunt:raijin` |
| Previous | [Storm Crown](Storm-Crown.md) |
| Evolution | — |

## Overview

A direct lightning kit with sequential ricochets and a collision-checked dash. There is no charge battery, rod array, or stack preparation.

## Acquisition

Evolve [Storm Crown](Storm-Crown.md) as Sovereign Hunter with 2,000,000 base EP and mastery of that Unique. Use `/wildhunt ascend rey_dau`. It replaces only the matching Unique; the Ultimate starts with its own mastery.

## Active modes

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Railbolt | 800 | 3 s | 270 |
| 2. Thunder Step | 2,400 | 4 s | 240 |
| 3. Heaven’s Thunder | 6,000 | 60 s | See signature below |

**0.7.0 damage adjustment:** multiply every raw damage value below by **0.90**. This applies to direct hits, ricochets, fields, counters and signature pulses. Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

### Railbolt

Aim the first hit along a block-clipped ray up to 56 blocks. The bolt then ricochets from its latest victim to the nearest eligible enemy within twelve blocks. It can hit up to 6 different targets, including the initial victim, and never revisits one. Each hop retains 90% of the preceding hop’s raw damage. Ricochets ignore blocks and line of sight; the initial aimed shot still stops at blocks. Wet or drenched enemies take 30% extra lightning damage.

### Thunder Step

Dash along your full aim direction for up to 24 blocks, including upward and downward, and shock enemies within 2.5 blocks of the travelled segment. The path sweeps the caster’s whole bounding box in half-block increments; walls, low ceilings, collisions the world border and build-height limits stop it. The teleport preserves your current horizontal and vertical velocity and syncs it to the client. It replaces Fulgurite Array completely and creates no persistent rods or charge.

### Heaven’s Thunder

For ten seconds, strike up to five visible enemies within fourteen blocks once per second, prioritizing maximum health. Raw damage is 240 on the opening strike and 90 on later strikes, before mastery and native modifiers. This signature supplies damage directly and never creates charge.

## Toggleable passive: Storm Mantle

While toggled on, gain 12 armor, 60% movement, 50% attack speed and 100% lightning boost. Movement generates no resource.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **40 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

## Suggested combo

Use Thunder Step to find a clear angle → aim Railbolt at one edge of a group → let its successive hops reach enemies beyond the initial victim’s bounce radius.

## Strengths and limitations

A missed initial ray cannot start a ricochet. Bounces pass through walls but cannot revisit an enemy. The dash follows a collision-checked 3D path and can carry you off a ledge or into the air. Normal gravity resumes afterward; it is not sustained flight.

## Related pages

[Skills](Skills.md) · [Mechanics](Mechanics.md) · [Evolution](Evolution.md) · [Commands](Commands.md)

---

**Categories:** Tensura: Wild Hunt · Version 0.7.0
