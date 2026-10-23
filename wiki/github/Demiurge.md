# Demiurge, God of Genesis

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Eschaton](https://github.com/Bithio999/MC/wiki/Eschaton) · [Godly Skills](https://github.com/Bithio999/MC/wiki/Godly) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

**Demiurge, God of Genesis** is Eschaton's encounter-exclusive Godly Skill. Its eight arts combine an articulated execution blade, rotating stellar machinery, seven elemental stars, silent scripture, world-eater fronts and an interruptible world seed.

![Demiurge skill crest](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/demiurge.png?raw=true)

| Skill data | Demiurge, God of Genesis |
| --- | --- |
| Tier | Godly |
| Native classification | Ultimate |
| Registry ID | `wildhunt:demiurge` |
| Owner | [Eschaton](https://github.com/Bithio999/MC/wiki/Eschaton) |
| Mastery | 100,000; boss starts mastered |
| Acquisition | Boss-exclusive; cannot be rolled, synthesized or slotted by a player |
| Native resources | Aura and magicules; varies by mode |
| Passive authority | Shared [Godly classification passives](https://github.com/Bithio999/MC/wiki/Godly#classification-passives) |

## Damage and resource rules

The tables give **raw encounter damage and fixed art costs**. Demiurge does not apply a mastery multiplier or EP damage multiplier. Native elemental boosts, source handling, difficulty, armor, barriers, dodges and Godly classification can change the final result. Thoth's native spell-cost attribute does not change these fixed art costs. Each activation pays native energy and starts its own native cooldown.

All offensive arts only hit eligible living targets in the encounter. Warnings use the same saved positions and pattern mathematics as server hits. Attacks do not break or place terrain. Cosmetic light can extend past the exact damage boundary.

## Crownless Edge

[Active - Boss cast] — **Crownless Edge**

| Ability information | Value |
| --- | --- |
| Damage type | Physical Aura Slash |
| Damage ID | `tensura:aura_slash` |
| Raw damage | 14,000 per fan |
| Resource cost | 400,000 aura |
| Cooldown | 6 seconds |
| Warning / strikes | 24 ticks; strikes at ticks 24, 40 and 56 |
| Geometry | 65° half-angle fans; radial bands 0–18, 12–32 and 24–46 blocks; vertical distance up to 9 blocks |

An execution blade winds up, cuts forward, reverses behind Eschaton and finishes with a long forward cleave. The middle fan faces the opposite direction. Moving through the open angles or changing radial bands avoids the sequence; standing behind him for all three strikes is unsafe.

## Astral Loom

[Active - Boss cast] — **Astral Loom**

| Ability information | Value |
| --- | --- |
| Damage type | Light elemental |
| Damage ID | `tensura:light_elemental` |
| Raw damage | 6,500 per pulse; at most once per victim per pulse |
| Resource cost | 900,000 magicules |
| Cooldown | 12 seconds |
| Warning / active window | 32 ticks; pulses every 10 ticks until tick 112 |
| Geometry | Four / six / eight rotating rays in forms I / II / III; 46-block reach; 1.1-block hit tolerance |

A stellar armature assembles around Eschaton and projects rotating pearl-white rays. The gaps remain safe; orbit with the pattern rather than crossing the light repeatedly. A victim intersecting multiple rays in one pulse is hit once.

## Sevenfold Genesis

[Active - Boss cast] — **Sevenfold Genesis**

| Ability information | Value |
| --- | --- |
| Damage type | Fire → Water → Lightning → Earth → Wind → Light → Space |
| Damage IDs | `tensura:fire_elemental`, `tensura:water_elemental`, `tensura:lightning_elemental`, `tensura:earth_elemental`, `tensura:wind_elemental`, `tensura:light_elemental`, `tensura:space_elemental` |
| Raw damage | 18,000 per star |
| Resource cost | 1,800,000 magicules |
| Cooldown | 14 seconds |
| Warning / cadence | Each warning locks 32 ticks before its impact; new stars every 14 ticks |
| Geometry | Seven separate 4.5-block impact spheres |

Each star snapshots the current target's position **when its warning appears**. Earlier warnings stay in place while later stars follow the target's new location. Flame tongues, water ribbons, lightning forks, earth facets, wind helices, light rays and spatial slashes distinguish the seven stars. Keep moving away from locked warnings.

## Null Testament

[Active - Boss cast] — **Null Testament**

| Ability information | Value |
| --- | --- |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Raw damage | 28,000; once per victim |
| Additional effect | 60 ticks of native Magic Interference II, attributed to Godly Demiurge |
| Resource cost | 4,000,000 magicules |
| Cooldown | 18 seconds |
| Warning | 38 ticks |
| Geometry | Three parallel 72-block segments, spaced 12 blocks apart; target hitboxes expanded 2.4 horizontally and 8 vertically |

Three tall scripture slabs open into spatial corridors. Move between the marked lanes before they cut. The disruption is applied after a successful hit and retains native status protections. Its Godly source can pass classification immunity to **non-Godly** interference; it does not guarantee every status application.

## World-Eater's Wake

[Active - Boss cast] — **World-Eater's Wake**

| Ability information | Value |
| --- | --- |
| Damage type | Darkness elemental |
| Damage ID | `tensura:darkness_elemental` |
| Raw damage | 18,000 per front; once per victim per front |
| Resource cost | 1,200,000 aura |
| Cooldown | 18 seconds |
| Warning / cadence | 28 ticks; three fronts, 20 ticks apart |
| Geometry | Expanding hollow fronts, 1.5-block radial tolerance; up to 54.8-block front radius; vertical distance up to 8 blocks |
| Safe gaps | Two opposing openings per front, each with a 22° half-angle; orientation rotates 120° between fronts |

Dark ribs and pale crests travel outward across the arena. Each front is hollow: the interior behind a passed wave is safe from that wave. Golden gaps show the openings. A wave never repeatedly damages a victim just because its surface overlaps for several ticks.

## Primordial Exchange

[Active - Boss channel] — **Primordial Exchange**

| Ability information | Value |
| --- | --- |
| Damage type | None — resource conversion |
| Damage ID | — |
| Resource payment | 20,000,000 aura, paid on activation |
| Completion | +8,000,000 current magicules, capped at current maximum |
| Conversion rate | 2.5 aura → 1 magicule |
| Cooldown | 24 seconds |
| Channel | 48 ticks before payout |

Two counter-flowing helixes feed a suspended genesis vessel. The AI favors this move when its magicules are below 15%. The payout happens once. Cancelling or interrupting the channel loses the initial aura payment without granting magicules. It does not directly heal HP or SHP, and cannot run when the magicule pool lacks room for its full payout.

## Thronefall

[Active - Boss cast] — **Thronefall**

| Ability information | Value |
| --- | --- |
| Damage type | Earth elemental |
| Damage ID | `tensura:earth_elemental` |
| Raw damage | 36,000; once per victim |
| Resource cost | 2,000,000 aura |
| Cooldown | 16 seconds |
| Warning | 36 ticks |
| Geometry | Collision-safe short ascent; locked landing four blocks from the target's activation position; 9-block impact sphere |
| Activation reach | 64 blocks |

Eschaton rises toward an apex six blocks above his starting position while a landing crest marks the ground. He descends onto the locked point in a burst of concentric impact frames. The landing stops following the target after activation. A blocked destination prevents activation; a destination obstructed during the warning cancels the actual landing and hit without refunding the cast.

## The End of All Myths

[Active - Boss cast] — **The End of All Myths**

| Ability information | Value |
| --- | --- |
| Damage type | Space elemental |
| Damage ID | `tensura:space_elemental` |
| Raw damage | 120,000; once per victim |
| Resource cost | 25,000,000 magicules |
| Cooldown | 42 seconds |
| Warning | **200 ticks / ten seconds** |
| Geometry | 56-block damage sphere centered on the saved casting origin |
| Counterplay | Three destructible Genesis Seals; golden refuges at broken seal positions; all three broken interrupts and staggers |

Creation seals feed a growing black world seed above Eschaton. A vast canopy, broad converging ribbons, saturated light plumes and rising fragments surround the lensed seed. The formation compresses before its final expanding nova and pressure front, making its buildup visible across the arena. Break a seal for a seven-block horizontal, ten-block vertical refuge and a large drain of current reserves. Break all three to cancel the nova and create an eight-second opening. See [creation seals](https://github.com/Bithio999/MC/wiki/Eschaton#creation-seals) for exact resource drains and placement rules.

![Demiurge's world seed and golden refuge](https://github.com/Bithio999/MC/blob/main/wiki/media/eschaton-apocalypse.png?raw=true)

[Native motion preview with Eschaton and Aion](https://github.com/Bithio999/MC/blob/main/wiki/media/eschaton-aion-anime-preview.mp4) · [Visual gallery](https://github.com/Bithio999/MC/wiki/Visual-Effects).

## Interruption and ownership

Demiurge is **Godly**: Lugh's non-Godly-target Countermand does not seal it. Native activation restrictions still apply, and the apocalypse has its own seal interruption mechanic. Cancelling an attack or removing its owner removes the owned warning and seal entities. Reloaded bosses retain resources and forms but do not resume an attack without its original telegraph.

---

**Categories:** Tensura Myths · Skills · Godly · Bosses
