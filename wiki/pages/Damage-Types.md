# Damage types

[Main Page](../README.md) · [Reference](Mechanics.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Damage types** identify the native attack source used by an ability. Aura and magicules are resources; neither is a damage type. Skill tier, elemental damage and physical-looking animation are separate properties.

## Direct damage

| Element / source | Native damage ID | Skills |
| --- | --- | --- |
| [Lightning](#lightning) | `tensura:lightning_elemental` | Storm Crown, Vritra, Thoth, Lugh |
| [Water](#water) | `tensura:water_elemental` | Scarlet Deluge, Aegir, Thoth, Lugh |
| [Fire](#fire) | `tensura:fire_elemental` | Black Flame, Vritra, Thoth, Lugh |
| [Ice](#ice) | `tensura:ice_elemental` | Frostbound Colossus, Demeter, Lugh |
| [Space](#space) | `tensura:space_elemental` | Threadweaver, Janus, Anansi, Aion, Thoth, Lugh |
| [Earth](#earth) | `tensura:earth_elemental` | Rune Smith, Verdant Keeper, Anansi, Thoth, Lugh |
| [Light](#light) | `tensura:light_elemental` | Mirrorbound, Thoth, Lugh |
| [Wind](#wind) | `tensura:wind_elemental` | Thoth, Lugh |
| [Physical aura slash](#physical-aura-slash) | `tensura:aura_slash` | Lugh |
| [Darkness](#darkness) | `tensura:darkness_elemental` | Lugh |
| [Gravity](#gravity) | `tensura:gravity_elemental` | Lugh |
| [Generic magic](#generic-magic) | `tensura:magic` | Thoth: Spellbreak; Lugh |

## Lightning

**Damage ID:** `tensura:lightning_elemental`. Railbolt and Thunder Step use this source. Vritra’s Plasma Bore opening hit and Thunderhead strikes use it; their delayed fire fuses use a separate Fire source. Thoth’s third phase in its seven-element Lexicon/Library cycle uses Lightning.

## Water

**Damage ID:** `tensura:water_elemental`. Crushing Current, Scarlet Undertow and Water Veil retaliation use Water. Aegir’s Refracted Harpoon and Sovereign Breaker use Water. Thoth’s second Lexicon/Library phase uses Water for its shared Water/Ice slot. The barrier itself deals no damage.

## Fire

**Damage ID:** `tensura:fire_elemental`. Black Flame’s Oil Lash and ignition/pool hits use Fire. Vritra’s delayed fuses, thunderhead fire bursts and Cataclysm Front eruptions use Fire. Thoth’s first phase in each seven-element Lexicon/Library cycle uses Fire. Vritra/Thoth do not automatically inflict Black Burn.

## Ice

**Damage ID:** `tensura:ice_elemental`. Avalanche Crash, Glacial Prison, Winter Orchard’s hostile pulses and Winter Harvest blooms use Ice. Thoth uses Water for the shared Water/Ice slot and has no separate Ice chapter. Healing in Winter Orchard has no damage source. Chill, Frost and Webbed are additional statuses, not names for the direct hit.

## Space

**Damage ID:** `tensura:space_elemental`. Loom Needle, Sever Thread, Parallax Snare, Crossroads Edict, Worldline Sever, Event Horizon’s core and Genesis Verdict use Space. Anansi’s thread contacts and first Checkmate phase use Space. Thoth’s seventh Lexicon/Library phase uses Space. Gates and Causal Reversal do not deal damage. The shared Godly damage bonus preserves the source type of the original attack.

## Earth

**Damage ID:** `tensura:earth_elemental`. Runic Mine, Command Detonation and Briar Grasp use Earth. Anansi’s second Checkmate Loom phase uses Earth. Thoth’s fourth Lexicon/Library phase uses Earth. Living Grove and Verdant Heart do not deal damage. Physical-looking branches or a hammer icon do not change the native damage ID.

## Light

**Damage ID:** `tensura:light_elemental`. Prismatic Lance and its secondary splits use Light. Thoth’s sixth Lexicon/Library phase uses Light. Mirror Aegis does no direct attack damage; a reflected arrow/fireball/etc. retains its own projectile damage type with ownership transferred to the defender.

## Wind

**Damage ID:** `tensura:wind_elemental`. Thoth’s fifth Lexicon/Library phase uses Wind, with a rising helix visual. It is a direct elemental hit rather than additional movement damage.

## Generic magic

**Damage ID:** `tensura:magic`. Spellbreak uses native generic magic rather than an elemental source. Its drain, Silence and Magic Interference are separate effects. Godly owners still take the attack but are excluded from those additional disruption/drain effects.

## Black Burn

Native **Black Burn** is separate from the direct fire elemental hit that applies it. In Tensura 2.0.1.3 its ticking source is **`tensura:black_flame`**, with the Flame element, passed through native split-elemental damage handling. Its damage and behavior use Tensura’s Black Flame configuration. The direct attack’s table value is not the combined damage of the attack plus its full status duration.

Black Flame’s ignition/pool hits can apply this status. Oil Lash applies slowing instead of Black Burn.

## Non-damaging abilities

Countermand and Skill Bank deal no damage. Perennial Seed restores HP/SHP and cleanses selected ordinary afflictions. Puppet Passage moves the caster and an anchor; Mirrorsea redirects projectiles without inventing a new damage type. Causal Reversal restores position, HP, SHP, aura and magicules. Hunter’s Recovery, Apex Renewal and Sovereign Rebirth heal. Concordance Pulse heals and pushes enemies without a damage call. Most defensive passives add attributes or barriers; Water Veil is the exception with its typed retaliation. Aion’s slowing aura and the shared Godly penetration do not themselves create periodic damage.

## Armor, barriers and resistance

Tensura 2.0.1.3 places the listed direct elemental damage IDs in the native **bypasses armor** tag. This does not make them barrier-bypassing, unresistable, physical, or generic magic damage. Native skill authority, elemental boosts, barriers, resistance/nullification skills and server data still govern the result. Datapacks can change native damage tags.

Any Godly skill owner gains the shared +35% bonus against non-Godly owners and resistance/nullification bypass for their own damage, regardless of toggles. These traits preserve the original damage type. They do not weaken the target against unrelated attacks, environmental damage or status effects. See [Godly classification passives](Godly.md#classification-passives) for the exact scope, including source-aware Magic Interference immunity, and [combat mechanics](Mechanics.md) for mastery and barrier behavior.

## Complete reference

[Ability index](Abilities.md) · [Skill overview](Skills.md) · [Combat mechanics](Mechanics.md)

## Physical aura slash

**Damage ID:** `tensura:aura_slash`. Lugh's Sovereign Edge origin entry uses this native physical source. Other recognised physical techniques can normalise to it. Aura slash retains native physical handling; it does not mean Lugh pays aura.

## Darkness

**Damage ID:** `tensura:darkness_elemental`. Lugh can reconstruct this element from an eligible observation. It is not an additional Thoth chapter.

## Gravity

**Damage ID:** `tensura:gravity_elemental`. Lugh can reconstruct this element from an eligible observation. Aion's Event Horizon uses Space damage despite its gravitational presentation.

## Reconstructed technique sources

Lugh's [archive](Lugh.md#technique-archive) can reconstruct physical aura slash (`tensura:aura_slash`), generic magic (`tensura:magic`) and Earth, Fire, Water, Ice, Wind, Lightning, Light, Darkness, Space or Gravity elemental sources. Each Reconstructed Art uses the selected entry; Grand Synthesis uses its three-entry snapshot in order. Physical aura slash is tagged as physical by Tensura.

Special sources outside that supported set are normalised to generic magic, or aura slash when recognised as physical. A copied disintegration, soul-consumption or instant-kill source does not grant its special bypass rules. Every reconstructed hit is attributed to Lugh and its current owner, with Godly ownership traits and ordinary native combat handling. See the skill article for the complete form geometry and intensity cap.

---

**Categories:** Tensura Myths · Reference
