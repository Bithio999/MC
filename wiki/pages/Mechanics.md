# Combat mechanics

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

## Damage and mastery

Raw damage is multiplied by:

```text
(1 + sqrt(clamp(baseEP / 2,000,000, 0, 1)))
× (1 + 0.5 × clamp(masteryFraction, 0, 1))
× (Ultimate ? 3 : 1)
```

Skill pages include the Ultimate’s 3× base multiplier in their displayed raw values. Do not multiply it twice. Progression caps at 3× the unprogressed Unique base or 9× that same base for a mastered Ultimate. Tensura applies its elemental boosts, rank interactions, resistances and barriers afterward. Raw damage is not guaranteed damage removed from a boss.

## Art resources

| Resource | Created by | Spent by | Limit |
| --- | --- | --- | --- |
| Storm charge | Fulgurite rod hits; movement during Storm Mantle; successful Heaven’s Thunder hits | Railbolt | 3 |
| Drenched | Water wave, whirlpool and tide hits | Persists as a timed mark; boosts water and lightning interactions | 12 seconds |
| Owned oil stacks | Oil Lash; ordinary melee during Furnace Mantle | Your initial pool ignition | 3; 15 seconds |
| Oil pools | Oil Lash; fallback ignition; Black Sun Caldera | Ignition turns pools into burning ground | 3 Unique / 5 Ultimate |
| Owned rime | Fissure and rampart hits; Absolute Winter | Your Glacial Rupture at 3 stacks | 3; 10 seconds |
| Ward budget | Glacial Rampart | Intercepted hostile projectiles | 3 Unique / 6 Ultimate |

## Aiming and movement

Railbolt is immediate and block-clipped. Water waves and ice fissures travel over about sixteen game ticks. Oil Lash selects a limited number of enemies in a facing cone. Fields stay anchored except the mobile tide and targeted storm. Dimension changes invalidate anchored effects; they do not teleport fields into a new world.

Forced movement respects knockback resistance. Friendly teams, owned pets, creative/spectator players, disabled player-vs-player combat and line of sight are checked. Strong bosses may resist elemental/status effects or movement.

## Defense and reactions

Water Veil has an attacker-targeted counter after health damage, with its own cooldown. Furnace Mantle oils ordinary melee victims. Storm Mantle trades movement for charge. Colossus Guard adds defensive attributes while Glacial Rampart spends a finite projectile budget. These are separate mechanics, not one renamed shield.

## Native effects

Tensura supplies the registered lightning particles, water bubbles, shockwaves, Black Fire, snowflake effects and animated elemental magic circles. Wild Hunt calls those APIs at runtime. It does not redistribute Tensura’s effect textures or create destructive vanilla lightning.

## Timers and persistence

Cooldown values are seconds. Travelling fronts, pools and short fields use a server tick handler; damage windows remain subject to normal invulnerability rules. Mantle attributes expire or are removed when forgetting their skill. Logged-in upgrades cap the old oversized cooldowns and cancel obsolete transient field timers while preserving rolled skills, mastery and boss history.

---

**Categories:** Tensura: Wild Hunt · Version 0.4.0
