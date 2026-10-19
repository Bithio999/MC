# Human evolution

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

The alternate branch is **Human → Awakened Hunter → Apex Hunter → Sovereign Hunter**. Every stage keeps a human appearance. The base Human’s original Tensura evolution choices remain available.

| Stage | Previous | Base EP | Distinct bosses | Additional requirement |
| --- | --- | ---: | ---: | --- |
| [Awakened Hunter](Awakened-Hunter.md) | Human | 100,000 | 1 | — |
| [Apex Hunter](Apex-Hunter.md) | Awakened Hunter | 400,000 | 3 | — |
| [Sovereign Hunter](Sovereign-Hunter.md) | Apex Hunter | 2,000,000 | 5 | Master one apex Unique skill |

## HP and SHP growth

| Stage | HP bonus | SHP bonus | HP gained from previous hunter stage | SHP gained from previous hunter stage |
| --- | ---: | ---: | ---: | ---: |
| Awakened Hunter | +40 | +20 | +40 | +20 |
| Apex Hunter | +100 | +60 | +60 | +40 |
| Sovereign Hunter | +200 | +120 | +100 | +60 |

These are bonuses to the underlying maximums, not total health values. They use native HP and `MAX_SPIRITUAL_HEALTH` attributes. Evolution replaces the previous race modifiers instead of stacking them. Login upgrades existing hunter maximums while preserving current HP and SHP; it does not refill them.

## Distinct boss kills

The record counts **boss entity types**, not individual spawns. Ten Wither kills give one entry. Wither plus Ender Dragon gives two. Credit is cumulative, survives death and requires a player-attributed kill while Human or a hunter.

Eligible types are listed in `wildhunt:progression_bosses`: the shared `c:bosses` tag (including Tensura’s bosses), Wither and Ender Dragon. Future Monster Hunter mobs can be added by datapack. Ordinary hostile mobs do not count.

## Evolving

Use Tensura’s evolution menu or `/wildhunt evolve` after meeting the same gates. Check `/wildhunt status` for the recorded type IDs, base EP and mastery progress. Skill acquisition is independent: bosses never award the four Unique skills.

---

**Categories:** Tensura: Wild Hunt · Version 0.6.0
