# Human evolution

[Main Page](../README.md) · [Races](Races.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Human evolution** is the alternate hunter race branch. It progresses through Awakened Hunter, Apex Hunter, and Sovereign Hunter while preserving the player’s Human appearance.

The alternate branch is **Human → Awakened Hunter → Apex Hunter → Sovereign Hunter**. Every stage keeps a human appearance. The base Human’s original Tensura evolution choices remain available.

| Stage | Previous | Base EP | Distinct bosses | Additional requirement |
| --- | --- | ---: | ---: | --- |
| [Awakened Hunter](Awakened-Hunter.md) | Human | 100,000 | 1 | — |
| [Apex Hunter](Apex-Hunter.md) | Awakened Hunter | 400,000 | 3 | — |
| [Sovereign Hunter](Sovereign-Hunter.md) | Apex Hunter | 2,000,000 | 5 | Master one apex Unique skill |

## HP and SHP growth

| Stage | HP bonus | SHP bonus | HP gained from previous hunter stage | SHP gained from previous hunter stage |
| --- | ---: | ---: | ---: | ---: |
| Awakened Hunter | +100 | +400 | +100 | +400 |
| Apex Hunter | +500 | +3,200 | +400 | +2,800 |
| Sovereign Hunter | +1,000 | +6,600 | +500 | +3,400 |

These are bonuses to the underlying maximums, not total health values. They use native HP and `MAX_SPIRITUAL_HEALTH` attributes. Evolution replaces the previous race modifiers instead of stacking them.

## Aura recovery and EP

Each stage automatically grants a new [aura regeneration Extra Skill](Aura-Regeneration.md): Hunter's Recovery → Apex Renewal → Sovereign Rebirth. Only the strongest toggled tier heals; earlier ones remain in the ability menu.

Native evolution energy floors are 200K / 800K / 2M total EP, split **2/3 aura, 1/3 magicules**. Default alignment already has a 2× aura and 1× magicule kill-EP multiplier; this addon preserves that native calculation, gain bonuses, player penalties and server caps. It does not convert existing saved magicules into aura or touch unrelated races.

## Distinct boss kills

The record counts **boss entity types**, not individual spawns. Ten Wither kills give one entry. Wither plus Ender Dragon gives two. Credit is cumulative, survives death and requires a player-attributed kill while Human or a hunter.

Eligible types are listed in `wildhunt:progression_bosses`: the shared `c:bosses` tag (including Tensura’s bosses), Wither and Ender Dragon. Future Monster Hunter mobs can be added by datapack. Ordinary hostile mobs do not count.

## Evolving

Use Tensura’s evolution menu or `/wildhunt evolve` after meeting the same gates. Check `/wildhunt status` for the recorded type IDs, base EP and mastery progress. Skill acquisition is independent: bosses never award the four Unique skills.

---

**Categories:** Tensura Myths · Races
