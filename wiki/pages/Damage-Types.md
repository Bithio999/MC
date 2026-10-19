# Damage types

[Main Page](../README.md) · [Reference](Mechanics.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Damage types** identify the native attack source used by an ability. Aura and magicules are resources; neither is a damage type. Skill tier, elemental damage and physical-looking animation are separate properties.

## Direct damage

| Element | Native damage ID | Addon skill lines |
| --- | --- | --- |
| [Lightning](#lightning) | `tensura:lightning_elemental` | Storm Crown / Raijin |
| [Water](#water) | `tensura:water_elemental` | Scarlet Deluge / Varuna |
| [Fire](#fire) | `tensura:fire_elemental` | Black Flame / Agni |
| [Ice](#ice) | `tensura:ice_elemental` | Frostbound Colossus / Ymir |
| [Space](#space) | `tensura:space_elemental` | Aion |

## Lightning

**Damage ID:** `tensura:lightning_elemental`.

Railbolt, Thunder Step and Heaven’s Thunder deal lightning damage, including ricochets and area hits.

[Storm Crown](Storm-Crown.md) · [Raijin, Lord of Thunder](Raijin.md)

## Water

**Damage ID:** `tensura:water_elemental`.

Crushing Current, Scarlet Undertow, Crimson Ocean and Water Veil retaliation deal water damage. The barrier itself does not deal damage.

[Scarlet Deluge](Scarlet-Deluge.md) · [Varuna, Lord of Tides](Varuna.md)

## Fire

**Damage ID:** `tensura:fire_elemental`.

Oil Lash, ignition opening hits, burning-pool pulses and Black Sun Caldera direct hits deal fire elemental damage. Oil Lash is not a physical-damage attack. Ignition and caldera can additionally inflict native Black Burn.

[Black Flame](Black-Flame.md) · [Agni, Lord of the Black Flame](Agni.md)

## Ice

**Damage ID:** `tensura:ice_elemental`.

Avalanche Crash, Glacial Prison and Heavenfall Glacier impacts deal ice damage. Impact and falling-ice visuals do not change them into physical damage. Chill and Frost are additional effects.

[Frostbound Colossus](Frostbound-Colossus.md) · [Ymir, Lord of Winter](Ymir.md)

## Space

**Damage ID:** `tensura:space_elemental`.

Worldline Sever, Event Horizon’s inner damage area and Genesis Verdict deal space damage. Causal Reversal does not deal damage. Unbroken Continuum’s bonus preserves the original type of the attack it strengthens.

[Aion, God of Causality](Aion.md)

## Black Burn

Native **Black Burn** is separate from the direct fire elemental hit that applies it. In Tensura 2.0.1.3 its ticking source is **`tensura:black_flame`**, with the Flame element, passed through native split-elemental damage handling. Its damage and behavior use Tensura’s Black Flame configuration. The direct attack’s table value is not the combined damage of the attack plus its full status duration.

Black Flame and Agni’s ignition/pool and caldera hits can apply this status. Oil Lash applies slowing instead of Black Burn.

## Non-damaging abilities

Causal Reversal restores position/HP/SHP. Hunter’s Recovery, Apex Renewal and Sovereign Rebirth heal. Concordance Pulse heals and pushes enemies without a damage call. Most defensive passives add attributes or barriers; Water Veil is the exception with its typed retaliation. Aion’s slowing aura and resistance degradation do not themselves create periodic damage.

## Armor, barriers and resistance

Tensura 2.0.1.3 places all five listed direct elemental damage IDs in the native **bypasses armor** tag. This does not make them barrier-bypassing, unresistable, physical, or generic magic damage. Native skill authority, elemental boosts, barriers, resistance/nullification skills and server data still govern the result. Datapacks can change native damage tags.

Aion’s active passive adds its documented resistance/nullification degradation and non-Godly bonus. It does not convert every attack into space damage. See [Aion](Aion.md#passive) for its exact scope and [combat mechanics](Mechanics.md) for mastery and barrier behavior.

## Complete reference

[Ability index](Abilities.md) · [Skill overview](Skills.md) · [Combat mechanics](Mechanics.md)

---

**Categories:** Tensura Myths · Reference
