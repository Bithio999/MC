# Fettered Human: Neo Otherworld 1.1.3a starting-skill audit

The pack config adds **39** starting skills to Tensura’s **54** native entries. With Heavenly Restriction **1.1.6** and the pack’s shipped costs, **29 addon skills are excluded** and **10 remain eligible** for Fettered Human’s roll.

This is the Fettered compatibility filter, not a guarantee that a skill will roll: native uniqueness, server rules and pack-specific unlock rules still apply. Other races retain their pools. Native skills retain the existing filter; **Pride stays exempt from target-skill copying**, and **Creator stays eligible**, including its trusted creation-menu inheritance.

## Excluded addon skills

| Registry ID | Reason |
| --- | --- |
| `stextras:imitator` | Harvests configured skills from slain targets. |
| `traddon:total_reflection` | Can copy the skill behind a reflected projectile. Also has magicule-consuming abilities. |
| `traddon:devastator` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:butcher` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:captivator` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:coalescence` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:constant` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:corroder` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:crasher` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:dreamer` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:gardener` | Crop Mastery and food blessing charge magicules inside their ability bodies. |
| `mysticism:hidden_ruler` | Plunders and copies target skills. Also has magicule-consuming abilities. |
| `mysticism:kyurem` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:phaser` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:provider` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:reducer` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:reshiram` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:spiritualist` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:stagnator` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:subjugator` | Takeover spends magicules based on the target’s EP. |
| `mysticism:victorious_harbinger` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:zekrom` | At least one ability has a positive magicule cost in the installed configuration. |
| `mysticism:engineer` | At least one ability has a positive magicule cost in the installed configuration. |
| `tensuramoreskills:authority_of_greed` | Sustaining Lionheart beyond its free period repeatedly removes magicules. |
| `tensuramoreskills:domain_of_negation` | At least one ability has a positive magicule cost in the installed configuration. |
| `tensuramoreskills:albis_vina` | At least one ability has a positive magicule cost in the installed configuration. |
| `tensuramoreskills:satella` | Copies target skills through Devouring Shadows. Also has magicule-consuming abilities. |
| `tno_tensura_extras:all_rounder` | Copies a target’s Battlewill skills. Also has magicule-consuming abilities. |
| `tno_tensura_extras:seducer` | At least one ability has a positive magicule cost in the installed configuration. |

## Eligible addon skills

| Registry ID | Why it remains eligible |
| --- | --- |
| `mysticism:cultivator` | No magicule ability payment or target-skill copying. Chained still prevents future magicule-capacity gains. |
| `mysticism:malleable` | Its learned resistances are self-derived; it does not copy target skills. |
| `mysticism:repeater` | Repeats its own actions and manages its weapons; it does not acquire target skills. |
| `mysticism:scholar` | Study records target identities/knowledge, rather than granting their skills. |
| `tensuramoreskills:fungus_infection` | No magicule ability payment or target-skill acquisition. |
| `tensuramoreskills:boogie_woogie` | No magicule ability payment or target-skill acquisition. |
| `tensuramoreskills:adaptability` | Develops its own defenses; resistance acquisition remains allowed. |
| `tno_tensura_extras:optimist` | No magicule ability payment or target-skill acquisition. |
| `tno_tensura_extras:looter` | Captured enemies retain skills on summoned shadows; the player does not learn those skills. |
| `tno_tensura_extras:time_traveler` | No magicule ability payment or target-skill acquisition. Its original single-player restrictions still apply. |

## How the filter works

All ability modes are checked at ordinary and full mastery, with toggle state both on and off. Acquisition prices alone do not disqualify a reincarnation skill. The three documented inline adapters read live addon configuration because those abilities bypass the usual magicule-cost getter. Authority of Greed is excluded because its sustained ability consumes the user’s magicules, even though that charge is presented as a penalty.

The five target-skill-taking entries are optional datapack tag members, so none of these addons becomes a required dependency. Existing operator controls still work: `extraBlacklist` takes priority, then `allowlist`, then the `blockSkillTaking` and `blockMagiculeCosts` switches. Missing cost metadata for a known inline adapter is treated conservatively and logged. A future addon ability that hides its payment internally needs another adapter or a configured blacklist entry; reflection does not inspect arbitrary ability behavior.

The exact runtime registry and configurations were tested against [the locked pack release](NEO-OTHERWORLD-1.1.3a.md). Changes to a pack’s pool or configuration require a new audit. The opt-in integration test asserts the complete 39-entry classification.
