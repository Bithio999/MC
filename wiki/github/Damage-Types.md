# Damage types

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Reference](https://github.com/Bithio999/MC/wiki/Mechanics) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

**Damage types** identify the native attack source used by an ability. Aura and magicules are resources; neither is a damage type. Skill tier, elemental damage and physical-looking animation are separate properties.

## Direct damage

| Element / source | Native damage ID | Skills |
| --- | --- | --- |
| [Lightning](#lightning) | `tensura:lightning_elemental` | Storm Crown, Vritra, Thoth |
| [Water](#water) | `tensura:water_elemental` | Scarlet Deluge |
| [Fire](#fire) | `tensura:fire_elemental` | Black Flame, Vritra, Thoth |
| [Ice](#ice) | `tensura:ice_elemental` | Frostbound Colossus, Demeter, Thoth |
| [Space](#space) | `tensura:space_elemental` | Threadweaver, Janus, Aion |
| [Earth](#earth) | `tensura:earth_elemental` | Rune Smith, Verdant Keeper |
| [Light](#light) | `tensura:light_elemental` | Mirrorbound |
| [Generic magic](#generic-magic) | `tensura:magic` | Thoth: Spellbreak |

## Lightning

**Damage ID:** `tensura:lightning_elemental`. Railbolt and Thunder Step use this source. Vritra’s Plasma Bore opening hit and Thunderhead strikes use it; their delayed fire fuses use a separate Fire source. Thoth’s third Lexicon chapter and every third Library chapter use Lightning.

## Water

**Damage ID:** `tensura:water_elemental`. Crushing Current, Scarlet Undertow and Water Veil retaliation use Water. The barrier itself deals no damage.

## Fire

**Damage ID:** `tensura:fire_elemental`. Black Flame’s Oil Lash and ignition/pool hits use Fire. Vritra’s delayed fuses, thunderhead fire bursts and Cataclysm Front eruptions use Fire. Thoth’s first Lexicon and Library chapters use Fire. Vritra/Thoth do not automatically inflict Black Burn.

## Ice

**Damage ID:** `tensura:ice_elemental`. Avalanche Crash, Glacial Prison, Winter Orchard’s hostile pulses and Winter Harvest blooms use Ice. Thoth’s second Lexicon and Library chapters use Ice. Healing in Winter Orchard has no damage source. Chill, Frost and Webbed are additional statuses, not names for the direct hit.

## Space

**Damage ID:** `tensura:space_elemental`. Loom Needle, Sever Thread, Parallax Snare, Crossroads Edict, Worldline Sever, Event Horizon’s core and Genesis Verdict use Space. Gates and Causal Reversal do not deal damage. Aion’s bonus preserves the source type of the original attack.

## Earth

**Damage ID:** `tensura:earth_elemental`. Runic Mine, Command Detonation and Briar Grasp use Earth. Living Grove and Verdant Heart do not deal damage. Physical-looking branches or a hammer icon do not change the native damage ID.

## Light

**Damage ID:** `tensura:light_elemental`. Prismatic Lance and its secondary splits use Light. Mirror Aegis does no direct attack damage; a reflected arrow/fireball/etc. retains its own projectile damage type with ownership transferred to the defender.

## Generic magic

**Damage ID:** `tensura:magic`. Spellbreak uses native generic magic rather than an elemental source. Its drain, Silence and Magic Interference are separate effects. Godly owners still take the attack but are excluded from those additional disruption/drain effects.

## Black Burn

Native **Black Burn** is separate from the direct fire elemental hit that applies it. In Tensura 2.0.1.3 its ticking source is **`tensura:black_flame`**, with the Flame element, passed through native split-elemental damage handling. Its damage and behavior use Tensura’s Black Flame configuration. The direct attack’s table value is not the combined damage of the attack plus its full status duration.

Black Flame’s ignition/pool hits can apply this status. Oil Lash applies slowing instead of Black Burn.

## Non-damaging abilities

Causal Reversal restores position, HP, SHP, aura and magicules. Hunter’s Recovery, Apex Renewal and Sovereign Rebirth heal. Concordance Pulse heals and pushes enemies without a damage call. Most defensive passives add attributes or barriers; Water Veil is the exception with its typed retaliation. Aion’s slowing aura and resistance degradation do not themselves create periodic damage.

## Armor, barriers and resistance

Tensura 2.0.1.3 places the listed direct elemental damage IDs in the native **bypasses armor** tag. This does not make them barrier-bypassing, unresistable, physical, or generic magic damage. Native skill authority, elemental boosts, barriers, resistance/nullification skills and server data still govern the result. Datapacks can change native damage tags.

Aion’s active passive adds its documented resistance/nullification degradation and non-Godly bonus. It does not convert every attack into space damage. See [Aion](https://github.com/Bithio999/MC/wiki/Aion#passive) for its exact scope and [combat mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) for mastery and barrier behavior.

## Complete reference

[Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Skill overview](https://github.com/Bithio999/MC/wiki/Skills) · [Combat mechanics](https://github.com/Bithio999/MC/wiki/Mechanics)

---

**Categories:** Tensura Myths · Reference
