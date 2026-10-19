# 0.3.0: comparison and rebalance of all four skill lines

> Historical 0.3.0 comparison. The 0.5.0 [wiki](../wiki/README.md) documents
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
