# Damage types

[All wikis](../README.md) · [TensuraAdditionalAdditions](TensuraAdditionalAdditions.md)

**Severance Rush and Counter deal Physical damage.** Their weapon contribution includes applicable enchantments and engravings. The dash is not a Battlewill attack.

Chained/Unbound’s toggle uses separate branches: a Battlewill attack receives its Battlewill multiplier; other Physical attacks receive the Physical multiplier. The two do not multiply each other on one hit.

Movement, world travel, Schema buffs and aura recovery deal **no direct damage**. Nullification permits spiritual contact but does not remove all other defenses.

## Native spell hit cooldowns

Tensura handles some spells as separate magic and elemental hits. Its paired-hit routine resets the hurt timer between the two components. This can let part of a rapid repeat through, while a pure magic hit remains blocked by the ordinary hurt window.

With default values on a target without extra defenses, two immediate native projectile hits produced:

| Aspectual spell | First hit | Immediate repeat |
| --- | ---: | ---: |
| Wind Cutter | 40 | 30 |
| Fire | 30 | 20 |
| Water Cutter | 40 | 0 |

The figures are before additional combat modifiers. Wind Cutter and Fire still lose some repeat-hit damage; Water Cutter’s equal repeat is blocked for roughly 10 ticks at normal game speed. Other attacks sharing that window can also affect the result. Metis separates different automated spells’ hurt windows while retaining each spell’s native repeat behavior.
