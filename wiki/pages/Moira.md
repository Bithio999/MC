# Moira, Lord of Finality

[Main Page](../README.md) · [Ananke](Ananke.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Moira, Lord of Finality** is Ananke's intrinsic Ultimate Skill. Its four rites turn visible geometry into distinct movement challenges.

![Moira, Lord of Finality](../../src/main/resources/assets/wildhunt/textures/skill/moira.png)

| Skill data | Moira, Lord of Finality |
| --- | --- |
| **Information** | |
| Type | Ultimate Skill; boss intrinsic |
| Registry ID | `wildhunt:moira` |
| Active modes | 4 |
| Resource | Magicules |
| Damage scaling | Fixed encounter values; no mastery or EP multiplier |
| **Obtaining** | |
| Owner | [Ananke](Ananke.md), [Eclipse Seraph](Eclipse-Seraph.md) |
| Reincarnation / synthesis | None |
| Player slotting | Unavailable |

## Usage

Ananke's AI selects these rites through a real native skill instance. Native activation checks, magicule costs and per-mode cooldowns apply. Only one Moira rite can be in progress at a time. Moira is Ultimate, not Godly, and Lugh's Countermand can interrupt it. Ananke separately owns Thoth's Godly traits and elemental boosts.

| Mode | Magicules | Cooldown | Fixed raw damage |
| --- | ---: | ---: | ---: |
| Fatebreaker | 4,000 | 4 s | 1,800 |
| Starfall Canticle | 10,000 | 10 s | 1,000 per star |
| Eventide Procession | 14,000 | 12 s | 2,200 once per victim |
| The Last Dawn | 50,000 | 24 s | 4,800 |

### Fatebreaker

[Active - Boss cast] — **Fatebreaker**

| Ability information | Value |
| --- | --- |
| Damage type | Aura slash |
| Damage ID | `tensura:aura_slash` |
| Raw damage | 1,800 |
| Resource cost / cooldown | 4,000 magicules / 4 seconds |
| Warning | 24 ticks / 1.2 seconds |

Locks a **110° horizontal fan** toward the target at the cast's origin. After the warning, enemies within 16 blocks and less than five vertical blocks from the origin are struck once. Gold border beams and a rose-violet floor wedge identify the footprint. Step around the fan or leave its radius; its facing does not chase you during preparation.

### Starfall Canticle

[Active - Boss cast] — **Starfall Canticle**

| Ability information | Value |
| --- | --- |
| Damage type | Light elemental |
| Damage ID | `tensura:light_elemental` |
| Raw damage | 1,000 per star |
| Resource cost / cooldown | 10,000 magicules / 10 seconds |
| Warning | 28 ticks / 1.4 seconds per star |

Creates six starfall markers, increasing to eight from phase two. New warnings appear **12 ticks / 0.6 seconds apart**, around the target's current position. Each marker locks when it appears and strikes a three-block-radius sphere 28 ticks later. The first marker centers on the original target; later markers have small fixed scatter. Move out of each seal before its stellar lance descends. Previously placed seals never slide after you.

### Eventide Procession

[Active - Boss cast] — **Eventide Procession**

| Ability information | Value |
| --- | --- |
| Damage type | Darkness elemental |
| Damage ID | `tensura:darkness_elemental` |
| Raw damage | 2,200 once per victim |
| Resource cost / cooldown | 14,000 magicules / 12 seconds |
| Warning | 30 ticks / 1.5 seconds |

After preparation, a **hollow circular front** starts at radius three and advances 0.55 blocks per tick for 40 ticks, ending around radius 24.5. Enemies within 1.2 blocks of the moving front and within three vertical blocks of the cast origin are hit once. The interior is safe from this rite after the front passes. A violet scripture wall with a gold crest marks its motion; it is not a filled area of damage.

### The Last Dawn

[Active - Boss cast] — **The Last Dawn**

| Ability information | Value |
| --- | --- |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Raw damage | 4,800 |
| Resource cost / cooldown | 50,000 magicules / 24 seconds |
| Warning | 75 ticks / 3.75 seconds |

Available to Ananke's final phase. Opposed celestial seals gather a dark sun above the cast origin. The blast strikes once throughout a **32-block sphere**, except a safe annulus **7–12 horizontal blocks** from the origin. Stand between the two gold circles or leave the sphere. The gold lattice remains visible through the impact. This refuge only excludes The Last Dawn's hit; other attacks can still reach it.

## Limits

These fixed values are before native combat handling and Thoth's independent elemental boosts. They do not receive Myths' normal player mastery multiplier. Hit immunity is never reset to force damage, and the rites do not destroy blocks. Creative/spectator players, allies, Ananke itself and other Anankes are excluded. Native arena instances also exclude unjoined players.

Movement releases eight ticks after the last damaging frame; the cosmetic tail can finish independently. Interrupted, expired, reset or unloaded rites are cancelled with their owned visual entities. Learning Moira through an operator command does not make it a player-usable skill.

---

**Categories:** Tensura Myths · Skills · Ultimate Skills · Boss skills
