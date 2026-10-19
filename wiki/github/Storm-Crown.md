# Storm Crown

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

**Storm Crown** is a Unique Skill inspired by **Rey Dau**. It combines ricochet lightning with an aimed movement art.

![Storm Crown](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/storm_crown.png?raw=true)

| Skill data | Storm Crown |
| --- | --- |
| **Information** | |
| Type | Unique Skill |
| Inspiration | Rey Dau |
| Combat role | Ricochet lightning / mobility |
| Active modes | 2 |
| Toggleable passive | Storm Mantle |
| Passive upkeep | 10 aura / second |
| Registry ID | `wildhunt:storm_crown` |
| **Evolution** | |
| Previous | — |
| Evolution | [Raijin, Lord of Thunder](https://github.com/Bithio999/MC/wiki/Raijin) |
| **Obtaining** | |
| Method | Reincarnation / starting skill roll |

## Usage

### Passive

[Passive - Toggle] — **Storm Mantle**

While toggled on, gain 6 armor, 35% movement, 25% attack speed and 50% lightning boost. Movement generates no resource.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **10 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

### Active

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Railbolt | 200 | 4 s | 90 |
| 2. Thunder Step | 600 | 5 s | 80 |

**0.7.0 damage adjustment:** multiply every raw damage value below by **0.90**. This applies to direct hits, ricochets, fields, counters and signature pulses. Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

#### Railbolt

[Active - Press] — **Railbolt**

Aim the first hit along a block-clipped ray up to 36 blocks. The bolt then ricochets from its latest victim to the nearest eligible enemy within twelve blocks. It can hit up to 4 different targets, including the initial victim, and never revisits one. Each hop retains 90% of the preceding hop’s raw damage. Ricochets ignore blocks and line of sight; the initial aimed shot still stops at blocks. Wet or drenched enemies take 30% extra lightning damage.

#### Thunder Step

[Active - Press] — **Thunder Step**

Dash along your full aim direction for up to 16 blocks, including upward and downward, and shock enemies within 2.5 blocks of the travelled segment. The path sweeps the caster’s whole bounding box in half-block increments; walls, low ceilings, collisions the world border and build-height limits stop it. The teleport preserves your current horizontal and vertical velocity and syncs it to the client. It replaces Fulgurite Array completely and creates no persistent rods or charge.

## Obtaining

Roll through Tensura’s normal reincarnation system. These Uniques join the configured primary pool; no particular roll is guaranteed. There are no skill trials, boss-awarded skills or race requirements to roll.

## Evolution

| Unique Skill | Ultimate Skill | Requirements |
| --- | --- | --- |
| Storm Crown | [Raijin, Lord of Thunder](https://github.com/Bithio999/MC/wiki/Raijin) | Sovereign Hunter; 2,000,000 base EP; matching Unique mastered |

Ascension replaces only the matching Unique. The Ultimate starts with its own mastery.

## Suggested combo

Use Thunder Step to find a clear angle → aim Railbolt at one edge of a group → let its successive hops reach enemies beyond the initial victim’s bounce radius.

## Strengths and limitations

A missed initial ray cannot start a ricochet. Bounces pass through walls but cannot revisit an enemy. The dash follows a collision-checked 3D path and can carry you off a ledge or into the air. Normal gravity resumes afterward; it is not sustained flight.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

---

**Categories:** Tensura: Wild Hunt · Version 0.7.0
