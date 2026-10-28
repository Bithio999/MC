# Chained exclusive Battlewill / Physical damage branches

Based on the [Mysticism Restricted 2.2.1 inspection](RESTRICTED_DAMAGE_0.34.6.md). Chained now follows its exclusive branch structure and exact Battlewill-source criterion.

While Chained’s combat toggle is active:

1. Read the source’s Tensura ability instance. If its skill is a native `Battlewill`, multiply damage by **5**.
2. **Otherwise**, if `TensuraDamageHelper.isPhysicalAttack(source)` accepts it, multiply damage by **3**.
3. Other damage is unchanged. These bonuses are not applied while the toggle is off.

The native `isBattlewill` helper is intentionally not the branch selector. It also accepts direct Physical hits under Aura Sword or Ogre Guillotine, even when no Battlewill ability produced the hit. Those ordinary attacks use Chained’s Physical branch. Sources explicitly produced by a Battlewill retain priority even if tagged Physical, converted to Physical, or elemental.

## Retained Chained rule

Unlike Mysticism’s additive Physical branch (`amount + attack attribute × 2`, limited to direct hits), Chained retains the user’s existing **3× other outgoing Physical damage** rule. This covers physical projectiles and Physical skills as well as melee. The change copies the exclusive branches and Battlewill-source check; it does not restore attack-stat scaling or narrow Chained to direct hits.

| Controlled reference, before defenses and other modifiers | Chained multiplier / result |
| --- | --- |
| Physical Battlewill ability | **5×**, not 15× |
| Non-Physical Battlewill ability | **5×** |
| Other Physical melee / projectile / skill | **3×** |
| Explicit Physical-converted non-Battlewill damage | **3×** |
| Chained dash | **3×** |
| Diamond sword native Aura Slash, unmastered / mastered | 7 × 5 = **35** / 14 × 5 = **70** |
| Ordinary 7-damage melee with Aura Sword’s native +7 | (7 + 7) × 3 = **42** |
| Unrelated elemental damage / another attacker / disabled Chained | No Chained bonus |

Native Aura Slash projectiles are captured from actual ability casts, including their source skill metadata. Tests dispatch their real damage through Tensura’s callback with Aura Sword active and verify the 35/70 results and toggle-off cleanup. The native coefficient defaults remain 1 / 2. Aura Sword’s numeric increment remains excluded from Chained’s separate 3× buff-effectiveness bonus. Other numeric Battlewill buffs keep their existing enhancement.

No movement, regeneration, race-growth, cost or cooldown code changed. Mysticism itself remains a static reference, not an installed interoperability test.
