# Aura regeneration

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

**Aura regeneration** is a line of three toggleable Extra Skills granted by the hunter and Heavenly Restriction race branches. Each tier recovers health using aura instead of magicules.

| Skill data | Hunter recovery Extra Skills |
| --- | --- |
| **Information** | |
| Type | Extra |
| Damage type | None — healing / survival |
| **Obtaining** | |
| Acquisition | Hunter or Heavenly Restriction race evolution; no permanent magicule acquisition cost |
| Activation | Native toggle; new racial tier enabled automatically |
| Resource | Aura only |
| Idle cost | None at full applicable health |

## Usage

### Passive

[Passive - Toggle] — Only the strongest enabled recovery tier heals.

**Damage type:** None for all three tiers. Open an individual skill article below for its costs, acquisition and limitations.

### Recovery tiers

| Extra Skill | Race | Native inspiration | Healing | Aura cost |
| --- | --- | --- | --- | --- |
| [Hunter’s Recovery](https://github.com/Bithio999/MC/wiki/Hunters-Recovery) | Awakened Hunter / Unshackled | Self Regeneration | 4 HP/second; 8 mastered | 20 per HP |
| [Apex Renewal](https://github.com/Bithio999/MC/wiki/Apex-Renewal) | Apex Hunter / Heavenly Restricted | Ultraspeed Regeneration | Missing HP every half second | 30 per HP; 20 mastered |
| [Sovereign Rebirth](https://github.com/Bithio999/MC/wiki/Sovereign-Rebirth) | Sovereign Hunter / Heavenbreaker | Infinite Regeneration | Missing HP and SHP every half second | 30 per HP / 10 per SHP; 20 / 5 mastered |

Registry IDs: `wildhunt:hunters_recovery`, `wildhunt:apex_renewal`, `wildhunt:sovereign_rebirth`.

Only the strongest **toggled** aura recovery tier works. Earlier skills stay learned and can be used if you turn a stronger one off. Recovery uses only available aura and heals proportionately if you cannot afford all missing health. HP is funded before SHP. Full health costs nothing. Mastery increases only on a funded recovery, and existing skill mastery is preserved.

## Native integration and limits

All three are native Extra Skills, granted through Tensura's racial skill learning system. Their icons reference native regeneration icons at runtime. They do not apply native regeneration effects that would charge magicules on a later tick.

Healing respects native severance maximum HP and interaction suppression. Hunter's Recovery and Apex Renewal also respect native healing suppression such as Frost and Curse. Like native Infinite Regeneration, Sovereign Rebirth can recover through ordinary Curse, Frost and strong Black Burn, but Infinite Imprisonment prevents it.

Sovereign Rebirth can stop an ordinary lethal hit by paying 30 aura (20 mastered) to preserve 1 HP; subsequent half-second recovery pays for the remaining healing. It cannot stop invulnerability-bypassing damage, barrier bypass level 1.75 or higher, complete severance, spiritual-health depletion, suppressed interactions or an unfunded recovery. It does not grant free immortality or spend magicules. Repeated lethal hits must each be funded.

## Energy split

The Heavenly Restriction lineage first gains recovery at its second evolution, [Unshackled](https://github.com/Bithio999/MC/wiki/Unshackled), then [Heavenly Restricted](https://github.com/Bithio999/MC/wiki/Heavenly-Restricted) and [Heavenbreaker](https://github.com/Bithio999/MC/wiki/Heavenbreaker). Earlier tiers remain learned. These specific racial grants bypass Chained’s learning restriction; existing eligible characters receive missing tiers automatically. Its EP growth remains 90% aura / 10% magicules before Chained blocks the magicule share.

Hunter base evolution energy is split **2/3 aura and 1/3 magicules**. Tensura's default alignment also gives kill EP in that 2:1 ratio; native gain bonuses, penalties and server caps still apply. Spending aura does not transfer EP, change maximum magicules, or alter the allocation of later gains.

## Visual effects

Successful healing briefly draws a botanical seal and rising restorative wisps. Hunter’s Recovery, Apex Renewal and Sovereign Rebirth use increasingly developed wisps; the pulse follows the caster. Full-health upkeep emits no recovery pulse.

---

**Categories:** Tensura Myths · Skills
