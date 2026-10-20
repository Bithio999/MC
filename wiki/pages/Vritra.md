# Vritra, Lord of Cataclysm

[Main Page](../README.md) · [Skills](Skills.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Vritra, Lord of Cataclysm** is an Ultimate Skill focused on **piercing plasma, delayed fire and travelling demolition**.

![Vritra, Lord of Cataclysm](../../src/main/resources/assets/wildhunt/textures/skill/vritra.png)

| Skill data | Vritra, Lord of Cataclysm |
| --- | --- |
| **Information** | |
| Type | Ultimate Skill |
| Combat role | Piercing plasma, delayed fire and travelling demolition |
| Active modes | 3 |
| Resource | Aura |
| Toggleable passive | Catastrophe Engine |
| Passive upkeep | 80 aura / second |
| Registry ID | `wildhunt:vritra` |
| **Obtaining** | |
| Method | Synthesis: master both ingredients; 2M base EP |
| **Evolution** | |
| Evolution | [Aion](Aion.md) or [Thoth](Thoth.md) |

## Usage

### Passive

[Passive - Toggle] — **Catastrophe Engine**

| Ability information | Value |
| --- | --- |
| Damage type | None — support / defense |
| Damage ID | — |
| Upkeep | 80 aura / second |

Gain 50% Flame boost, 50% Lightning boost and 25% attack damage.

Elemental boosts use the [strongest ordinary source](Mechanics.md#elemental-bonus-sources); Godly bonuses apply separately.

Enable the native skill toggle in Ability Selection. The first second is paid on enabling; upkeep is paid every twenty game ticks. It turns off when unfunded or blocked by native activation rules. Turning it off removes only its own modifiers. It is separate from active mode cycling.

### Active

| Mode | Aura | Cooldown | Raw damage |
| --- | ---: | ---: | --- |
| 1. Plasma Bore | 1,400 | 4 s | 650 lightning; 350 per fire fuse |
| 2. Thunderhead Crucible | 3,600 | 15 s | 260 lightning + 140 fire per pulse |
| 3. Cataclysm Front | 6,000 | 24 s | 780 per eruption |

Damage listed here is before native combat handling and uses **1 + 0.5 × mastery fraction**, capped at **1.5×**. These new kits do not use the apex skills’ 0.90 multiplier or the former 3× Ultimate multiplier. EP never multiplies their damage. Native elemental boosts, authority, resistances, barriers and hit immunity apply.

#### Plasma Bore

[Active - Press] — **Plasma Bore**

| Ability information | Value |
| --- | --- |
| Damage type | Lightning elemental + delayed Fire elemental |
| Damage ID | `tensura:lightning_elemental`; `tensura:fire_elemental` |
| Resource cost | 1,400 aura |
| Cooldown | 4 seconds |
| Raw damage | 650 lightning; 350 per fire fuse |

Fire a solid-block-clipped plasma lance for 48 blocks, piercing up to eight visible enemies. Each struck location receives a fixed fire fuse: after 0.8 seconds it erupts in a three-block radius. The fuse remains at the marked position; a moving enemy can leave it. Native hit immunity may limit overlapping detonations.

#### Thunderhead Crucible

[Active - Press] — **Thunderhead Crucible**

| Ability information | Value |
| --- | --- |
| Damage type | Lightning elemental + delayed Fire elemental |
| Damage ID | `tensura:lightning_elemental`; `tensura:fire_elemental` |
| Resource cost | 3,600 aura |
| Cooldown | 15 seconds |
| Raw damage | 260 lightning + 140 fire per pulse |

Place a nine-block-radius storm furnace within 40 blocks for eight seconds. Once per second it strikes up to six visible enemies with lightning; each strike leaves a fire burst at that position after half a second, in a 2.5-block radius. One thunderhead per owner.

#### Cataclysm Front

[Active - Press] — **Cataclysm Front**

| Ability information | Value |
| --- | --- |
| Damage type | Fire elemental |
| Damage ID | `tensura:fire_elemental` |
| Resource cost | 6,000 aura |
| Cooldown | 24 seconds |
| Raw damage | 780 per eruption |

Send seven warned eruptions forward at four-block spacing, reaching 28 blocks. Each covers a 4.5-block radius. The first erupts after 0.8 seconds and later eruptions follow every 0.2 seconds. A solid obstruction ends the advancing line. Targets can leave the warned circles; no terrain is destroyed.

## Obtaining

Open **Skills → Ultimate → Synthesis**. Choose Vritra and combine **[Storm Crown](Storm-Crown.md) + [Black Flame](Black-Flame.md)**, with both mastered and at least **2,000,000 base EP**. Any race can synthesize. Both ingredients are replaced only after native storage accepts the result. Other skills remain learned. All nine presets and the longest remaining ingredient cooldown carry over; the new Ultimate starts with its own mastery.

## Suggested combo

Cast Thunderhead Crucible to pressure a group → Plasma Bore through their escape line → send Cataclysm Front across the remaining formation.

## Strengths and limitations

This is a new combined kit, not an upgraded Railbolt or Oil Lash. Fixed warnings can be dodged. Damage scales with mastery, never EP.

Attacks respect teams, owned pets, creative/spectator immunity and server PvP settings. Native status resistances apply. World effects are bounded, expire automatically and do not damage terrain. Anchored effects do not persist across death, relog or dimension changes.

## Related pages

[Skills](Skills.md) · [Skill synthesis](Skill-Synthesis.md) · [Damage types](Damage-Types.md) · [Mechanics](Mechanics.md)

## Visual effects

Plasma Lance combines a continuous hot core, orange/violet helices and conductor collars; its delayed fuses converge into fire impacts. Thunderhead Furnace carries soft storm-cloud layers, periodic branching lightning and a suspended hot ring. Cataclysm Front warns with a fractured ground inscription before volcanic spires and flame jets erupt at the strike.

![Thunderhead Furnace’s storm cloud and conductor diagram](../media/visual-vritra.png)

[Visual effects gallery and display settings](Visual-Effects.md).

---

**Categories:** Tensura Myths · Skills
