# Mysticism Restricted — Battlewill / Physical stacking

Inspected **Mysticism 2.2.1-neoforge**, the same pinned research release used in [Restricted EP research](RESTRICTED_EP_0.34.0.md). [Release metadata](https://api.modrinth.com/v2/version/q4YA4Wke) · [download](https://cdn.modrinth.com/data/bjvFzLV3/versions/q4YA4Wke/mysticism-neoforge-2.2.1.jar).

SHA-256: `61e98fe4666cb2baa35396dca7d4e1bb7e41069f2ddfc4af9799b2bb5eb442d3`. No dependency JARs or decompiled sources are redistributed.

## Finding

**Restricted does not multiply its Physical and Battlewill bonuses together on the same hit.** Its toggled `RestrictedSkill.onDamageEntity` has an `if … else if …`:

1. If the source carries an ability instance whose skill is a native `Battlewill`, multiply the incoming amount by configured `battlewillMultiplier` (default **5**).
2. Otherwise, if it is Physical and the direct entity equals the attacker, **add** the attacker’s attack attribute times (`physicalAttackMultiplier` − 1), default **+2 × attack attribute**.
3. Other sources get neither bonus from this callback. Toggle off returns without changes.

The Physical branch is an additive attack-stat contribution, not a universal 3× multiplier on the entire damage amount. Restricted does not use Tensura’s broader `isBattlewill` helper here: Aura Sword turning ordinary melee into Battlewill for other native systems does not route that ordinary melee through Restricted’s Battlewill branch unless its source actually carries a Battlewill skill instance.

The inspected `onToggleOn` modifies movement speed, sneaking speed, safe fall distance and armor; it does not add an attack-damage multiplier. Searching class references to the two multiplier config fields found the config and `RestrictedSkill` only. The only Restricted/Battlewill references outside that skill are language generation; no equivalent to Chained’s Aura Sword buff-tripling wrapper was found.

## Practical comparison

| Hit, ignoring defenses and other modifiers | Mysticism Restricted | Chained after 0.34.6 |
| --- | --- | --- |
| Native Aura Slash with 7 base projectile damage | 7 × 5 = **35** | 7 × 5 × 3 = **105** |
| Mastered Aura Slash, native default 2× weapon coefficient | 14 × 5 = **70** | 14 × 5 × 3 = **210** |
| Ordinary fully charged Physical melee, base 7 and native Aura Sword bonus 7, attack attribute 7 | 7 + 7 + 2 × 7 = **28** | (7 + 7) × 5 × 3 = **210** |

Aura Slash’s normal/mastered coefficients are **1 / 2** in pinned Tensura 2.0.1.3. Aura Sword adds its configured weapon bonus only on direct Physical hits; the slash projectile is indirect and does not receive that bonus.

Chained’s **5× Battlewill + 3× all outgoing Physical** remain independent and multiply for qualifying hits, as requested. This update removes **only** Aura Sword’s additional 3× buff-effectiveness bonus; other numerical Battlewill buffs retain it. This is a code inspection of Mysticism, not a runtime interoperability claim or a change to its balance.
