# Black Flame

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

**Black Flame** is a Unique Skill inspired by **Nu Udra**. It uses visible oil tentacles to pull targets into burning ground.

![Black Flame](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/black_flame.png?raw=true)

| Skill data | Black Flame |
| --- | --- |
| **Information** | |
| Type | Unique Skill |
| Inspiration | Nu Udra |
| Combat role | Tentacle pulls / burning ground |
| Active modes | 2 |
| Toggleable passive | Furnace Mantle |
| Passive upkeep | 15 aura / second |
| Registry ID | `wildhunt:black_flame` |
| **Evolution** | |
| Previous | — |
| Evolution | [Agni, Lord of the Black Flame](https://github.com/Bithio999/MC/wiki/Agni) |
| **Obtaining** | |
| Method | Reincarnation / starting skill roll |

## Usage

### Passive

[Passive - Toggle] — **Furnace Mantle**

While toggled on, gain 8 armor, 50% melee attack and 50% flame boost. Fire-tagged damage is resisted while the passive is enabled, preserving its fire-protection utility without a lingering potion. Melee attacks do not coat targets or accumulate oil. This is an offensive passive, not an extra ignition step.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **15 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

### Active

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Oil Lash | 160 | 3 s | 70 |
| 2. Blackflame Ignition | 800 | 8 s | 90 |

**0.7.0 damage adjustment:** multiply every raw damage value below by **0.90**. This applies to direct hits, ricochets, fields, counters and signature pulses. Raw values already include the Ultimate’s 3× multiplier where applicable. Mastery adds up to 50%. **Wild Hunt applies no EP damage scaling.** Native elemental boosts, defenses, rank interactions and the listed situational modifiers still apply. EP requirements for race evolution and Ultimate ascension are unchanged.

#### Oil Lash

[Active - Press] — **Oil Lash**

Lash up to 4 enemies in a forward fan within 14 blocks and pull them toward you. Each lash has a synced three-dimensional tapered tentacle with a dark body and glowing orange bands. It extends, curls and recoils over sixteen ticks. A missed cast still shows a tentacle sweep. Leave a pool at the final victim’s location, or at the aim point about ten blocks away on a miss. Maintain up to 3 pools; the oldest is replaced first.

#### Blackflame Ignition

[Active - Press] — **Blackflame Ignition**

Ignite valid owned pools for six seconds. Each affects a 3-block radius; overlapping pools cannot damage one enemy twice in a pulse. Opening damage is immediate and fixed, with no oil-stack multiplier. Later pulses deal 24 raw damage once per second and apply native Black Burn. If no valid controllable pool exists, place a fallback pool at the aim point up to six blocks away.

## Obtaining

Roll through Tensura’s normal reincarnation system. These Uniques join the configured primary pool; no particular roll is guaranteed. There are no skill trials, boss-awarded skills or race requirements to roll.

## Evolution

| Unique Skill | Ultimate Skill | Requirements |
| --- | --- | --- |
| Black Flame | [Agni, Lord of the Black Flame](https://github.com/Bithio999/MC/wiki/Agni) | Sovereign Hunter; 2,000,000 base EP; matching Unique mastered |

Ascension replaces only the matching Unique. The Ultimate starts with its own mastery.

## Suggested combo

Use Oil Lash to pull a victim and place a pool → ignite the pool. Cover likely escape routes with separate pools instead of farming stacks or overlapping them for duplicate damage.

## Strengths and limitations

Pools are world locations, not status stacks on enemies. Unlit pools expire after fifteen seconds; ordinary ignition burns for six. Only your pools in the same dimension and within thirty-two blocks can be controlled. Native Black Burn and forced movement may be resisted.

## In-game preview

![Oil Lash tentacles](https://github.com/Bithio999/MC/blob/main/wiki/media/oil-lash.png?raw=true)

Captured in the Minecraft 1.21.1 development client with Wild Hunt 0.5.0.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

---

**Categories:** Tensura: Wild Hunt · Version 0.7.0
