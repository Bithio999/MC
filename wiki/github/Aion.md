# Aion, God of Causality

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

**Aion, God of Causality** is a Godly skill built around spatial precision, battlefield control and a personal HP/SHP/energy rewind with a safe recorded position. Its active arts use magicules and space elemental damage. Its power grows with mastery, without EP damage scaling.

![Aion, God of Causality](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/aion.png?raw=true)

| Skill data | Aion, God of Causality |
| --- | --- |
| **Information** | |
| Type | Godly / native Ultimate |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Active modes | 4 |
| Toggleable passive | Unbroken Continuum |
| Passive upkeep | 180 magicules / second |
| Mastery cap | 50,000 |
| Registry ID | `wildhunt:aion` |
| **Evolution** | |
| Previous | Any of the five combination Ultimates |
| Evolution | — |
| **Obtaining** | |
| Method | Mastered Myths Ultimate, True Hero or True Demon Lord, 12M base EP |

## Usage

### Passive

[Passive - Toggle] — **Unbroken Continuum**

| Ability information | Value |
| --- | --- |
| Damage type | None — defense / slowing |
| Damage ID | — |

Adds **45 physical barrier**, **45 magic barrier**, **0.75 knockback resistance** and **1,200 native Multilayer Barrier** capacity. The latter uses Tensura’s existing barrier HUD. Its owned multilayer contribution rearms after **10 seconds without an incoming hit**, checked on the next upkeep tick. Upkeep does not repair it during combat. Turning the passive off removes its own contributions while preserving other barrier modifiers.

Enabling costs the first 180 magicules, then 180 magicules each second. It turns off when unfunded or suppressed. It emits no constant passive particle cloud.

Aion ownership also grants the three always-active [Godly classification passives](https://github.com/Bithio999/MC/wiki/Godly#classification-passives): Divine Authority, Absolute Penetration and Interference Immunity. These are independent of Unbroken Continuum's toggle and upkeep. Resistance bypass belongs to the owner's damage; the slowing aura never suppresses nearby targets' defenses against other sources.

Eligible enemies within **16 blocks** suffer causal time dilation. Movement reduction increases linearly from **15% at the edge** toward **80% at the caster**. It uses a temporary movement modifier, has no ambient particle cloud, does not stack between Aion users and expires within half a second after leaving the aura or the caster stopping it. Hostile and unowned **projectiles** are also slowed by proximity, using the same 15–80% reduction. Their speed is adjusted rather than repeatedly multiplied toward zero, and the aura’s speed reduction is removed after leaving. Own and allied shots are preserved. Native projectile collision, gravity and drag continue to operate.

### Active

| Mode | Magicule cost | Cooldown | Range / area | Base damage |
| --- | ---: | --- | --- | ---: |
| Worldline Sever | 2,400 to start; 1,200 per additional 0.5 seconds | 4 seconds after channel ends | 80-block beam, up to 8 targets per pulse, 6-second maximum | 1,200 per 0.5 seconds |
| Event Horizon | 10,000 | 16 seconds | 64-block cursor placement; 24-block pull, 10-block damage radius; 10 seconds | 700 per second |
| Causal Reversal | 12,000 to record; free return | 1 second to recast; 25 seconds after manual or automatic return | Personal checkpoint, 20-second window | — |
| Genesis Verdict | 28,000 | 45 seconds | 64-block placement; 12 / 14-block radius | 2,400 + 2,400 + 4,800 / 7,200 |

#### Worldline Sever

[Active - Mode 1] — **Worldline Sever**

| Ability information | Value |
| --- | --- |
| Damage type | [Space elemental](https://github.com/Bithio999/MC/wiki/Damage-Types#space) |
| Damage ID | `tensura:space_elemental` |

Hold the assigned skill key to channel a gold-white spatial beam that follows the current crosshair direction. Both the local preview and server damage ray use a short, frame-independent steering response, giving sweeping turns a smooth transition instead of snapping immediately. The local visual still updates every rendered frame; damage and resource use remain server-controlled. It pierces up to eight eligible enemies per pulse, passes through water and lava, and stops at solid block collision shapes (including solid waterlogged blocks). Damage pulses every half second; the opening pulse costs 2,400 magicules and each additional pulse costs 1,200. A full six-second channel has twelve pulses and costs 15,600 magicules before native cost modifiers.

Releasing the key, exhausting magicules, suppression, death, dimension changes or reaching six seconds ends the beam and starts its four-second cooldown. The channel cannot keep damaging after release. Each pulse rechecks friendly/PvP protection.

#### Event Horizon

[Active - Mode 2] — **Event Horizon**

| Ability information | Value |
| --- | --- |
| Damage type | [Space elemental](https://github.com/Bithio999/MC/wiki/Damage-Types#space) |
| Damage ID | `tensura:space_elemental` |

Place a gravity domain with a dark core, counter-rotating accretion bands and Tensura’s native **Gravity** magic circle. Enemies within **24 blocks** are pulled toward the core; only enemies within the **10-block inner radius** take space damage each second. A faint outer ring marks the pull range, while the bright inner circle marks the damage zone. Its inward pull adds 0.40 horizontal velocity per tick before knockback resistance, which reduces the pull. Hostile or unowned projectiles lose 35% velocity per tick and curve toward the center; the caster’s own and allied projectiles are preserved.

#### Causal Reversal

[Active - Mode 3] — **Causal Reversal**

| Ability information | Value |
| --- | --- |
| Damage type | None — recovery / repositioning |
| Damage ID | — |

The first press costs **12,000 magicules** and places a visible clockwork checkpoint, recording position, HP, SHP, aura and magicules **after paying that initial cast cost**. Press again after the one-second recast delay to return for **no cost**, even with no magicules remaining. If not used manually, it **automatically returns after twenty seconds**. HP, SHP, aura and magicules only **increase toward their recorded values**, limited by current caps. If a current stat is already higher than its checkpoint value, that higher value is preserved. The checkpoint stays fixed; it does not continually record new peaks. The initial cast cost is never refunded. Native **Recovery** circle effects and spatial bursts mark the return.

Reversal does not restore items, cooldowns, buffs or lost lives. The destination must remain unobstructed, inside the world border and in the same dimension. A blocked manual return keeps the pending checkpoint. If the destination is blocked at timeout, recovery happens **in place** instead of teleporting into a wall. A completed manual or automatic return consumes the checkpoint and starts the full twenty-five-second cooldown. An already-armed checkpoint still resolves while asleep or temporarily suppressed. Death, forgetting Aion, relogging and dimension changes clear it without restoring resources.

#### Genesis Verdict

[Active - Mode 4] — **Genesis Verdict**

| Ability information | Value |
| --- | --- |
| Damage type | [Space elemental](https://github.com/Bithio999/MC/wiki/Damage-Types#space) |
| Damage ID | `tensura:space_elemental` |

Place a warned spatial catastrophe at the cursor within 64 blocks. After one second of warning, three pulses strike at one-second intervals: **2,400, 2,400 and 4,800** base damage within twelve blocks.

If your Event Horizon is active, Verdict consumes it, uses its existing center and expands to **fourteen blocks**, with the final strike raised to **7,200**. The old Horizon visual is removed when consumed. A contracting cage, rising spatial beams, native **Space** circle and expanding fracture fronts telegraph the strikes. Enemies can leave the area.

## Damage and interaction

The damage multiplier is **1 + 0.5 × mastery fraction**, capped at **1.5×**. All damaging modes use native `tensura:space_elemental` damage and use native damage calculation. The shared Godly traits provide source-specific resistance/nullification bypass and +35% damage against non-Godly owners, even with Unbroken Continuum off. Other defenses, authority and PvP rules still apply. Team and owner protection follows the addon’s existing target checks. No mode destroys blocks or scales damage with EP.

## Obtaining

Use **Skills → Godly → Ascension** to replace one mastered Myths Ultimate after reaching the awakening and EP requirements. Equip Aion from the new Godly category in Tensura’s normal ability menu.

## Visual effects

Worldline Sever carries fine gold/violet helical strands and clock collars around its readable aim core. Event Horizon has a dark spherical core, tilted accretion optics, counter-rotating dials and a faint separate pull perimeter. Causal Reversal places a three-dimensional hourglass with flowing sand over its checkpoint clock. Genesis Verdict suspends mechanical dials above the field and releases timed vertical strikes.

![Event Horizon’s dark core and tilted clockwork optics](https://github.com/Bithio999/MC/blob/main/wiki/media/visual-aion.png?raw=true)

[Visual effects gallery and display settings](https://github.com/Bithio999/MC/wiki/Visual-Effects).

---

**Categories:** Tensura Myths · Skills
