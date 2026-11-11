# Damage types

[All wikis](../README.md) · [TensuraAdditionalAdditions](TensuraAdditionalAdditions.md)

**Severance Rush and Counter deal Physical damage.** Their weapon contribution includes applicable enchantments and engravings. The dash is not a Battlewill attack.

Chained/Unbound’s toggle uses separate branches: a Battlewill attack receives its Battlewill multiplier; other Physical attacks receive the Physical multiplier. The two do not multiply each other on one hit.

Movement, world travel, Schema buffs and aura recovery deal **no direct damage**. Nullification permits spiritual contact but does not remove all other defenses.

## Native spell hit cooldowns

AdditionalAdditions gives each distinct native magic projectile its own hurt window. Rapid manual casts and simultaneous automatic casts therefore apply their full impacts independently. Repeated pulses from one persistent projectile retain native timing; the fix does not bypass resistances or defensive invulnerability. Ordinary arrows, melee and Battlewill projectiles retain their native behavior.

With default values on a target without extra defenses, two distinct projectiles hitting immediately produce:

| Aspectual spell | First projectile | Second projectile |
| --- | ---: | ---: |
| Wind Cutter | 40 | 40 |
| Fire | 30 | 30 |
| Water Cutter | 40 | 40 |

These are per-projectile totals before additional combat modifiers. Tensura applies mixed magic/elemental damage as paired components. Without this fix, its shared timer can reduce those immediate second impacts to 30, 20 and 0 respectively. The paired components still use Tensura’s native damage processing.
