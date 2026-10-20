# Current balance: 0.19.0

> **External parity audit:** [native/addon Unique comparison](research/UNIQUE_PARITY_0.19.md)
> identifies high repeatable damage and healing-economy concerns. These values
> describe the shipped internal tier balance, not confirmed cross-mod parity.

The approved five combination Ultimates remain the benchmark. Uniques provide
repeatable early combat and narrower utility; Ultimates add native penetration,
stronger fields and linked third arts; Godly skills add exceptional sustained
pressure or adaptive damage alongside their existing ownership traits. This is
role-based balance, not one fixed multiplier across every attack.

All current playable skills now use **1–1.5× mastery**. The original four
Uniques no longer have the extra 0.90 penalty. No damage formula uses EP.
Uniques retain ordinary native authority and defenses; Ultimate attacks retain
level-1 resistance penetration; Godly owners retain source-scoped resistance/
nullification bypass and their non-stacking +35% bonus against non-Godly owners.
Printed damage below excludes these modifiers, elemental attributes and defenses.

| Unique | Raw damage / support | Aura costs | Cooldowns / upkeep |
| --- | --- | --- | --- |
| Storm Crown | Railbolt 550, four distinct victims, 90% retained per hop; Thunder Step 350 | 800 / 1,000 | 3 / 4 s; 25/s |
| Scarlet Deluge | Current 420, 630 if already drenched; Undertow 200 + 110/s; retaliation 100 | 600 / 1,600 | 3 / 6 s; 30/s |
| Black Flame | Lash 450; ignition 650 + 150/s, overlapping owned pools deduplicated | 650 / 1,800 | 3 / 7 s; 35/s |
| Frostbound Colossus | Avalanche 700; Prison 300 + native Frost | 800 / 1,400 | 3 / 7 s; 40/s |
| Threadweaver | Needle 450; tether-consuming Sever 800; +30% movement / +25% attack speed | 600 / 1,000 | 4 / 6 s; 25/s |
| Rune Smith | Proximity 650 or command 1,000, not both; +50% Earth/Flame/Lightning | 600 placement + 800 command | 6 / 4 s; 30/s |
| Verdant Keeper | Grove 7% HP / 6% SHP per second; Briar 500; +100% natural SHP regeneration | 1,600 / 700 | 14 / 5 s; 35/s |
| Mirrorbound | Lance 600 + two distinct 350 splits; six-second reflection; +20% movement | 750 / 1,400 | 3 / 10 s; 25/s |

The two Unique personal barriers retain 100 / 150 Multilayer capacity and their
existing Physical/Magic values and ten-second quiet recovery. Healing remains
capped by current maxima. No new barriers, passive stacks or elemental stacking
exceptions are added. Demeter still exceeds Grove's pulse, area and duration.

| Godly | Raw combat budget | Magicule costs / cadence |
| --- | --- | --- |
| Aion | Worldline 1,200 per half-second; Horizon 700/s; Verdict 2,400 + 2,400 + 4,800, or 7,200 final when empowered | Worldline 2,400 opening + 1,200/pulse (15,600 full channel); Horizon 10K; Reversal 12K; Verdict 28K. Existing cooldowns and durations. |
| Thoth | Lexicon 1,100/chapter; Spellbreak 900; Library 800/bolt | 12K / 6.5K / free Crucible / 24K. Existing 12 / 15 / 0 / 35 s cooldowns. |
| Lugh | Origin arts 3,800 / 3,800 / 4,200; appraised raw bounded to 3,500–6,000; synthesis 1.6× each archived art | 1.2K / 10K / 9K / 30K. Existing 2 / 4 / 18 / 40 s cooldowns. |

Aion's rewind preserves its fixed post-payment checkpoint, increase-only stat
restoration, free return, twenty-second auto-return and all safety/cleanup rules.
Its initial payment is now priced for this exceptional recovery/resource utility.
Thoth keeps exactly seven passive-backed elements and the existing seventy-tick
Lexicon / two-hundred-tick Library choreography. The 2:1 continuous Crucible,
resource drain caps, reflection behavior and Godly toggle budgets stay intact.
Lugh's old origin records upgrade without losing an appraised source, attack
form or selected slot. Existing observations are bounded to the new range;
synthesis reduces its per-record multiplier as basic reconstruction rises.

Rapid visual hits are not a guarantee of fixed HP loss: native hit handling,
element rules, barriers and authority still apply. The validation suite compares
measured native damage, full channel costs and seven-element cadence, rather
than assuming that a scheduled hit always removes HP. See
[0.19.0 validation](VALIDATION-0.19.0.md) for results and test limits.

The [0.18 addon research](research/ULTIMATE_BALANCE_0.18.md) still informs the
Ultimate benchmark. No additional third-party runtime compatibility is claimed.
Current ability-level damage IDs and values are in the [wiki](../wiki/README.md).

---

# Historical balance: 0.18.0

The five combination Ultimates have distinct assault, sustain, spatial, trap and
water-refraction roles. Acquisition remains two specific mastered Uniques and
2M base EP. Godly skills retain the separate true-awakening/12M gate.

| Ultimate | Combat budget before mastery / native modifiers | Costs / cooldowns |
| --- | --- | --- |
| Vritra | 1,500 Lightning + 850 Fire fuse; 900 Lightning + 500 Fire per storm pulse; 1,800 / 2,400 empowered eruptions | 6K / 15K / 25K aura; 5 / 16 / 26 s |
| Demeter | 10% HP/SHP orchard healing + 500 Ice per second; one-use 40% HP/SHP rescue seeds; 1,400 / 2,200 harvest | 12K / 10K / 20K aura; 20 / 24 / 24 s |
| Janus | 48-block safe gate; 1,900 Space snare with possible opposite-gate origin; 1,700 delayed Space Edict | 4K placement / 400 recast, 9K / 16K aura; 2 / 8 / 18 s |
| Anansi | Three-anchor geometry; 1,300 thread contacts once per victim/second; 2,800 Space + 1,900 Earth finisher | 5K / 3.5K / 22K aura; 2 / 5 / 24 s |
| Aegir | 1,600 / 2,200 refracted Water lance; projectile redirection; 2,100 / 3,000 empowered expanding Water rim | 7K / 12K / 20K aura; 5 / 16 / 24 s |

Mastery remains 1–1.5×. Ultimate hits carry native level-1 resistance penetration;
attribute defenses, barriers, nullification fallback, hit immunity, authority and
PvP protections retain their independent rules. EP never multiplies damage.
The ordinary elemental strongest-source policy and separate Godly bonuses remain.
Demeter uses a threshold rescue effect rather than an active team barrier.

[Lugh](../wiki/pages/Lugh.md) starts with three reconstruction forms. Observations
produce 2K–4K bounded raw damage, with ordinary source normalization; its finisher
snapshots three attacks at 1.8× each, sixteen ticks apart. Countermand blocks one
recent non-Godly technique for six seconds without changing learned state.
Its shared Godly traits require ownership, while its speed/mastery toggle has
separate upkeep. No kit uses hit-count passive stacks or direct health assignment
to bypass combat rules.

[Addon reference research](research/ULTIMATE_BALANCE_0.18.md) documents the exact
Nightmares release/defaults and Mysticism's legacy/current-version distinction.
Those references inform utility, costs and penetration; they do not establish
universal numerical parity across different servers and addon configurations.
Current player-facing tables are in the [wiki](../wiki/README.md).

---

# Historical balance: 0.14.0

All values below are fixed raw damage before native authority, defenses, elemental
attributes and mastery. There is no EP damage multiplier. Costs remain unchanged.
The four original apex skills apply 0.90 × mastery (1–1.5); other kits apply
mastery (1–1.5). Historical comparisons follow this current section.

| Unique | Opening / second art | Cooldowns |
| --- | --- | --- |
| Storm Crown | 160 / 135 | 3 / 4 s |
| Scarlet Deluge | 120 / 80 + 32 per pulse | 3 / 6 s |
| Black Flame | 125 / 165 + 42 per pulse | 3 / 7 s |
| Frostbound Colossus | 180 / 100 | 3 / 7 s |
| Threadweaver | 160 / 300 | 4 / 6 s |
| Rune Smith | 280 proximity / 420 command | 6 / 4 s |
| Verdant Keeper | 5% HP + 4% SHP per second / 190 | 14 / 5 s |
| Mirrorbound | 220 + two 130 splits / six-second reflection | 3 / 10 s |

The native reference remains Tensura 2.0.1.3: strong Black Lightning is 135
lightning + 15 magic at seven seconds / 1,000 MP; Black Flame's Flare is
270 fire + 30 magic for 2,000 MP. Storm Crown now starts at 144 before mastery,
with ricochets and a three-second cadence; Rune Smith's prepared command blast
is 420 before mastery. These are meaningful Unique attack budgets without
requiring multiple elemental passives. Aura and magicules are distinct resources.

| Ultimate | Fixed raw damage / support | Cooldowns |
| --- | --- | --- |
| Vritra | 650 lightning + 350 fire fuse; 260 lightning + 140 fire per thunder pulse; 780 front eruption | 4 / 15 / 24 s |
| Demeter | 140 Ice per orchard pulse and 7% HP / 6% SHP healing; 500-capacity, 50 Physical/Magic ward for 8 s; 600 / 900 empowered harvest | 18 / 16 / 20 s |
| Janus | Safe gates; 780 spatial snare; 620 spatial displacement | 3 / 7 / 18 s |

Ultimate damage rises about 67–115%, depending on art. Warnings remain dodgeable,
line-of-sight and target caps remain, and overlapping attacks respect native hit
immunity. The three Ultimates retain their demolition, team sustain and spatial
control roles. Automated combat tests compare old/new Unique and Ultimate raw
hits through the same actual native damage pipeline, rather than only testing constants.

Only Water Veil, Colossus Carapace and Aion supply persistent personal barriers.
Demeter spends an active mode on its team ward. Mirrorbound reflects projectiles;
Janus relies on mobility; Thoth focuses on magic. Their old passive shields are
removed automatically, including saved permanent modifiers.

Each elemental attribute uses its strongest positive ordinary modifier. Godly
Thoth applies separately; native innate base values and penalties remain. Native
skills, equipment and other modifiers participate in this rule. This is source
selection, not a hard cap that weakens a single powerful native source. Damage
paths that do not consult elemental boost attributes retain their native rules.

> **0.7.0:** All art damage is multiplied by 0.90; current costs, native barriers,
> recovery, Human vitality benchmarks and aura regeneration are in the
> [wiki](../wiki/README.md) and [native integration notes](NATIVE-INTEGRATION.md).
> The tables below retain their historical version labels.

# 0.3.0: comparison and rebalance of all four skill lines

> Historical 0.3.0 comparison. The 0.6.0 [wiki](../wiki/README.md) documents
> the current distinct arts, targeting, costs and combos. The native Tensura
> benchmarks below remain useful reference values.

The reference is Tensura: Reincarnated **2.0.1.3**, using the defaults in its
Extra/Unique skill and Aspectual Magic configuration. This compares **all four
addon Unique skills and their Ultimate evolutions**, not every ability in Tensura.
Costs below distinguish aura from magicules; they are different resources.
Numbers are raw values before Tensura's elemental boosts, resistances, barriers,
rank rules and attribute caps. They are not guaranteed health removed from a boss.

## Problems found in 0.2.0

- Every Unique had the same 8-damage beam and 5-damage area attack. Ultimates had
  only 18/10, regardless of progression or mastery.
- ManasCore expects cooldowns in **seconds**, but the addon supplied 80/200/400
  and 1,200 for the Ultimate domain. These were not 4/10/20/60-second cooldowns.
- The generic elemental damage source omitted Tensura's Unique/Ultimate rank,
  ability instance, mode and cost metadata.
- ManasCore calls normal skill `onTick` once per five seconds. The advertised
  once-per-second domain could not work through that callback.
- The inherited colored-name method bypassed the custom literal name. Missing
  translation entries exposed keys such as `wildhunt.skill.storm_crown`.

All five issues are corrected. Timed fields, mantles and domain visuals now run
from a server entity-tick handler. Custom attributes use expiring, non-stacking
modifiers and are removed when the skill is forgotten. Existing saves have
the old oversized cooldowns capped to the new limits on first login, preserving
rolled skills and mastery.

## Storm Crown / Raijin — Rey Dau

**Reference:** Extra skill Black Lightning defaults to 45 lightning + 5 magic
damage with a five-second cooldown for 500 magicules; its strong mode uses
135 + 15 damage, seven seconds and 1,000 magicules. These were already well above
our old Unique's eight damage and 80-second cooldown.

**New Unique:** a 90-damage, 36-block piercing railbolt (two direct targets),
chaining at 55% damage to up to three nearby enemies. A 60-damage Thunderstorm
leaves a six-second field with 20-damage pulses. Wet/drenched enemies take 30%
extra damage. The mantle adds 35% movement, 25% attack speed, six armor and 50%
lightning boost for 15 seconds.

**Ultimate:** triple base damage, 56-block railbolt, four direct targets and up to
five secondary chain targets per direct hit. The mantle rises to 60% movement,
50% attack speed, 12 armor and 100% lightning boost for 30 seconds. Heaven's
Thunder adds a ten-second, 14-block-radius storm domain.

## Scarlet Deluge / Varuna — Uth Duna

**Reference:** Water Manipulation's ball defaults to 12 damage for 100 magicules;
its manipulation/domination boosts are 1.5/3. Guardian provides ten protection
armor, a 30-point magic barrier and a much longer protection duration for a
support-focused kit.

**New Unique:** a 65-damage pressure cone reaching 18 blocks, with 50% bonus
against drenched targets. Undertow hits for 45, then pulls and slows targets with
18-damage pulses in a nine-block whirlpool for six seconds. Water Veil gives
16 absorption HP, ten armor, a 20-point magic barrier, water breathing and 50%
water boost for 15 seconds. Its defense is deliberately shorter than Guardian's;
it trades sustained support for offensive pressure and crowd control.

**Ultimate:** triple base damage, a 26-block cone and a 13-block whirlpool.
Water Veil provides 32 absorption HP, 25 armor, a 40-point barrier and 100% water
boost for 30 seconds. Crimson Ocean is its mobile ten-second domain.

## Black Flame / Agni — Nu Udra

**Reference:** Tensura's Extra skill Black Flame defaults to a 45 fire + 5 magic
orb, a one-second orb cooldown and 100 magicules. Flare has a 270 + 30 raw hit
for 2,000 magicules. Berserk also has persistent flame/strength mechanics, rather
than a single weak fire particle burst.

**New Unique:** Oil Lash is a 70-damage, 14-block tentacle fan that adds one of
three oil stacks lasting 15 seconds. Ignition starts at 90 damage; it consumes
your own oil for +60% per stack, reaching **252 raw damage** at three stacks
before progression multipliers. It leaves a six-second burning field with
24-damage pulses and applies Tensura's Black Burn. Furnace Mantle adds 50% melee
attack, eight armor, fire resistance and 50% flame boost for 15 seconds.

**Ultimate:** triple base damage, a 22-block fan, a 12-block field and stronger
Black Burn. The mantle gives 100% melee attack, 18 armor and 100% flame boost for
30 seconds. Black Sun Caldera adds a ten-second domain.

## Frostbound Colossus / Ymir — Jin Dahaad

**Reference:** Ice Breaker defaults to 100 magic damage, +100 against Frost and
+25 per Chill level (+50 with mastery), with a three-second cooldown, a cast
requirement and a high 70,000-magicule cost. Guardian's wall gives 15/40 armor
and 0.4/1 knockback resistance. These combine damage setups and meaningful defense.

**New Unique:** a 75-damage, 36-block lance. Freezing Mist deals 45 initially and
20 per pulse for six seconds in a nine-block radius, applies native Chill and
builds three rime stacks. A lance against your fully rimed target consumes the
stacks and adds 120 damage: **195 raw damage** before progression. Colossus Guard
gives 20 armor, a 15-point barrier, one knockback resistance and 50% water-element
boost for 15 seconds. It controls movement; it does not universally stun bosses.

**Ultimate:** triple base damage, 56-block lance, four direct targets and a
13-block mist. Guard rises to 40 raw armor, a 40-point barrier and 100% elemental
boost for 30 seconds. Absolute Winter adds a ten-second domain.

## Costs, cadence and progression

| Unique | Primary cooldown / aura | Field cooldown / aura | Mantle cooldown / aura |
| --- | --- | --- | --- |
| Storm Crown | 4 s / 200 | 8 s / 600 | 20 s / 300 |
| Scarlet Deluge | 3 s / 160 | 7 s / 500 | 18 s / 250 |
| Black Flame | 3 s / 160 | 8 s / 800 | 20 s / 350 |
| Frostbound Colossus | 4 s / 200 | 8 s / 650 | 20 s / 350 |

Ultimate modes use four times the aura and one second less cooldown. All domains
cost 6,000 aura and have a 60-second cooldown. Each lasts ten seconds, includes an
initial hit and then pulses once per second, and uses 1.5 times that line's field
pulse damage. Field pulses neither bypass invulnerability frames nor ignore walls.

All raw damage is multiplied by:

```text
(1 + sqrt(clamp(baseEP / 2,000,000, 0, 1)))
× (1 + 0.5 × clamp(masteryFraction, 0, 1))
× (Ultimate ? 3 : 1)
```

Unique progression therefore caps at 3× starting raw damage, and Ultimate raw
power caps at 9×. An unmastered newly evolved Ultimate is not instantly granted
full Ultimate mastery. Elemental mantles and Tensura's own combat rules apply
after this baseline. Modifiers remain subject to engine and server attribute caps.

## Visuals, icons and verification

Beams use colored cores and elemental helices; impacts have distinct elemental
bursts; fields have persistent rings; mantles orbit the caster; domains add
concentric seals and elemental pillars. Effects do not create destructive
lightning entities or change terrain.

The four portraits are original generated pixel art inspired by the monsters,
not extracted official emblems. The source sheet is `art/apex-portraits.png`.
`scripts/prepare-icons.py` slices it into 64×64 nearest-filtered textures. Unique
and Ultimate forms use their matching monster portrait. Pillow is needed only
to regenerate art, not to build or play. English translations and fallback names
cover all eight skills and their modes.

Regression tests exercise attacks and names across all eight skills, oil
consumption, rime shatter, temporary-attribute expiry, one-second domain scheduling,
reincarnation rolling and boss progression. Automated tests verify behavior; they
do not establish PvP balance or replace a client playtest of the new visuals.
