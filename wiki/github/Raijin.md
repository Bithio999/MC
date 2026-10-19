# Raijin, Lord of Thunder

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

**Raijin, Lord of Thunder** is a Ultimate Skill inspired by **Rey Dau**. It combines ricochet lightning with an aimed movement art.

![Raijin, Lord of Thunder](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/raijin.png?raw=true)

| Skill data | Raijin, Lord of Thunder |
| --- | --- |
| **Information** | |
| Type | Ultimate Skill |
| Inspiration | Rey Dau |
| Damage type | Lightning elemental |
| Damage ID | `tensura:lightning_elemental` |
| Combat role | Ricochet lightning / mobility |
| Active modes | 3 |
| Toggleable passive | Storm Mantle |
| Passive upkeep | 40 aura / second |
| Registry ID | `wildhunt:raijin` |
| **Evolution** | |
| Previous | [Storm Crown](https://github.com/Bithio999/MC/wiki/Storm-Crown) |
| Evolution | [Aion, God of Causality](https://github.com/Bithio999/MC/wiki/Aion) |
| **Obtaining** | |
| Method | Ascend the matching mastered Unique |

All damaging arts in this skill use **lightning elemental damage**, including its area hits and pulses.

## Usage

### Passive

[Passive - Toggle] — **Storm Mantle**

While toggled on, gain 12 armor, 60% movement, 50% attack speed and 100% lightning boost. Movement generates no resource.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **40 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

### Active

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Railbolt | 800 | 3 s | 270 |
| 2. Thunder Step | 2,400 | 4 s | 240 |
| 3. Heaven’s Thunder | 6,000 | 60 s | See signature below |

**Damage calculation:** multiply the listed base values by **0.90**, then by the mastery multiplier (up to **1.50**). Ultimate table values already include their 3× base multiplier. Elemental boosts, defenses, barriers, and situational modifiers also apply. EP does not multiply damage.

#### Railbolt

[Active - Press] — **Railbolt**

Aim the first hit along a block-clipped ray up to 56 blocks. The bolt then ricochets from its latest victim to the nearest eligible enemy within twelve blocks. It can hit up to 6 different targets, including the initial victim, and never revisits one. Each hop retains 90% of the preceding hop’s raw damage. Ricochets ignore blocks and line of sight; the initial aimed shot still stops at blocks. Wet or drenched enemies take 30% extra lightning damage.

#### Thunder Step

[Active - Press] — **Thunder Step**

Dash along your full aim direction for up to 24 blocks, including upward and downward, and shock enemies within 2.5 blocks of the travelled segment. The path sweeps the caster’s whole bounding box in half-block increments; walls, low ceilings, collisions the world border and build-height limits stop it. The teleport preserves your current horizontal and vertical velocity and syncs it to the client.

#### Heaven’s Thunder

[Active - Press] — **Heaven’s Thunder**

For ten seconds, strike up to five visible enemies within fourteen blocks once per second, prioritizing maximum health. Raw damage is 240 on the opening strike and 90 on later strikes, before mastery and native modifiers. This signature supplies damage directly and never creates charge.

## Obtaining

Evolve [Storm Crown](https://github.com/Bithio999/MC/wiki/Storm-Crown) as Sovereign Hunter with 2,000,000 base EP and mastery of that Unique. Use `/wildhunt ascend rey_dau`. It replaces only the matching Unique; the Ultimate starts with its own mastery.

## Evolution

| Unique Skill | Ultimate Skill | Requirements |
| --- | --- | --- |
| [Storm Crown](https://github.com/Bithio999/MC/wiki/Storm-Crown) | Raijin, Lord of Thunder | Sovereign Hunter; 2,000,000 base EP; matching Unique mastered |

Ascension replaces only the matching Unique. The Ultimate starts with its own mastery.

## Suggested combo

Use Thunder Step to find a clear angle → aim Railbolt at one edge of a group → let its successive hops reach enemies beyond the initial victim’s bounce radius.

## Strengths and limitations

A missed initial ray cannot start a ricochet. Bounces pass through walls but cannot revisit an enemy. The dash follows a collision-checked 3D path and can carry you off a ledge or into the air. Normal gravity resumes afterward; it is not sustained flight.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

---

**Categories:** Tensura Myths

## Godly evolution

At mastery, True Hero or True Demon Lord awakening, and 12M base EP, use **Skills → Godly → Ascension** to evolve this Ultimate into [Aion, God of Causality](https://github.com/Bithio999/MC/wiki/Aion). It replaces the selected Ultimate while preserving presets and remaining cooldowns.
