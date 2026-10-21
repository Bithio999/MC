# Ananke skill-package audit — 0.25.0

Audited 2026-10-21 against release commit `9b3c947a2b6106f26e9c2f05a1bdaca1668e0d11`. This is a comparison and diagnostic report; no gameplay changes or replacement release JAR accompany it.

## Verdict

**The offensive core is sufficient, but the current support package is not yet reliable or complete enough for a polished 30M-EP Myth boss.** Moira supplies four distinct attacks and Thoth adds two AI-controlled attack modes plus Godly ownership traits. More offensive skills are not the first priority. A confirmed activation bug, very weak proportional recovery and avoidable AI openings matter more than the headline EP or number of learned skills.

The comparison does not establish a universal boss ranking. Server configuration, native damage classification, Godly ownership, equipment and AI behavior substantially change a fight.

## Evidence and limits

- Inspected native Tensura 2.0.1.3 skill implementations, entity-existence profiles, boss AI and regeneration effects. Spawned Ananke, Hinata, Gazel and Charybdis in an isolated native GameTest server with NeoForge 21.1.209 and ManasCore 4.0.0.2.
- Two diagnostic runs completed successfully, one isolated GameTest per run. The final run exported spawn state, 40-tick recovery, a native Physical Attack Resistance callback comparison, an entity save/load and a further 20-tick recovery observation. See the [raw snapshot](data/ANANKE_SKILL_PACKAGE_0.25.0.json) and [reproduction fixture](fixtures/ananke-0.25.0/README.md).
- Read the pinned Nightmares and Mysticism release implementations separately. **Those addons were not installed or run in the test pack.** Nightmares requires a newer NeoForge than this project's pin. Their declared skill lists and defaults are implementation evidence, not verified runtime inventories or measured fight results.
- Version URLs, binary hashes and the initial native EP-profile comparison are in [the original boss research](MYTH_BOSS_IMPLEMENTATION.md). Third-party decompiled source and binaries are not redistributed here.
- These probes inspect actual skills and callbacks but are not player-versus-boss DPS benchmarks. Passing a diagnostic export test does not mean the identified behavior is correct.

## Reference packages

EP below means the indicated native profile's maximum AP + MP or an addon's declared default range, before equipment or custom configuration.

| Boss | EP reference | Relevant skill package and actual behavior | Implication for Ananke |
| --- | ---: | --- | --- |
| Hinata — native | 1,036,332 | Usurper and Mathematician; Earth, Flame, Spatial, Water, Wind and Electricity Nullification; Physical, Spiritual, Magic and Holy resistance; Poison/Corrosion nullification; spells and elemental summons. AI uses arts, spells, movement and phases. | Even a much lower-EP native boss has broader dedicated elemental protection and more varied tactical behavior. |
| Gazel — native | 1,036,332 | Eight elemental resistances, Physical/Spiritual/Magic/Holy resistance, status defenses, Earth Domination, melee arts and earth spells. AI closes gaps and teleports. | Ananke needs reliable coverage and effective use of mobility, rather than merely owning Spatial Domination. |
| Charybdis — native | 1,600,000 | Eight profile skills, including Magic Jamming, Gravity Manipulation, Physical Attack Resistance and Ultraspeed Regeneration; custom lasers/scales and incoming-damage halving. | A short list can still be formidable. Native Ultraspeed Regeneration can restore all missing HP every ten ticks while funded; Ananke's current recovery is vastly weaker. |
| Veldora — Nightmares | 30M–40M | Divide, elemental dominations, Spatial Domination/Motion, Black Lightning/Flame, spell families, Infinite Regeneration and a shared nullification list. Range-selected attacks, held attacks, flight and dashes. | This is the closest EP-scale reference: broad defense, substantial regeneration and active pursuit accompany its offense. |
| Guy — Nightmares | 40M–40,777,777 | Requests Lucifer, Metatron, Leviathan, Gabriel and Satanael, plus spatial skills, recovery, barriers and many support skills. Shared combat code caps incoming HP hits and adds high outgoing bypass. | Much stronger coverage than a skill count alone suggests. Its damage caps and bypass are additional boss mechanics, not evidence that Ananke should copy five Ultimates. |
| Milim of Wrath — Nightmares | 30M–37.6M | Requests Satanael, Law Domination, Ultraspeed/Self Regeneration, Multilayer Barrier, spatial movement and projectile attacks; persistent Rampage behavior. | Another example where a relatively compact explicit list plus strong support and entity code matters more than quantity. |
| Memoires — Mysticism | Not used as an EP benchmark | Repeater, bespoke recursion/phase behavior, teleportation and scripted HP/SHP recovery. Inspected base attributes include 1,800 HP, armor 100 and SHP regeneration 100. | A distinctive encounter mechanic and meaningful recovery can provide pressure without inflating the skill list. |

### Counts require care

Hinata's shipped profile lists 27 skills and Gazel's lists 24; runtime inspection found 28 and 25 nonnegative-mastery records respectively. Each had 18 toggles ON. Charybdis had eight learned records, six ON. Active skills do not need a passive toggle, so these figures must not be treated as counts of functioning attacks.

Nightmares Veldora requests 28 base IDs plus 22 shared nullification IDs, with two overlaps: **48 distinct requested IDs**, before helper grants or optional-ID resolution. Guy requests 30 explicit IDs plus resistance-name matching and helper grants; Milim requests ten plus similar helpers. Missing optional IDs may be skipped. Nightmares' normal `grantAndToggleAll` resolves the actual stored instances and activates them; its default path does **not** automatically master every skill.

## Confirmed activation defect in Ananke

The published boss owns and masters its intended fifteen skills. Fourteen are toggleable, but only four stored instances are ON after spawning and after 40 normal server ticks:

| Stored toggle | Skills |
| --- | --- |
| ON | Thoth, Universal Perception, Thought Acceleration, Self-Regeneration |
| OFF — defensive | Physical Attack Resistance, Spiritual Attack Resistance, Magic Resistance, Holy Attack Resistance, Abnormal Condition Nullification, Pain Nullification |
| OFF — other support | Mathematician, Observer, Chant Annulment, Spatial Domination |
| Not toggleable | Moira; its active attacks are unaffected by this flag |

**All six listed resistance/nullification defenses are therefore inactive.** Native `ResistSkill` checks the stored instance's toggle in its damage/effect callbacks. The native physical callback probe recognized a physical source: the stored OFF instance allowed a 100-point test hit through that callback; temporarily toggling that same stored instance ON caused the callback to reject the hit. This demonstrates the activation difference, not a full final-damage calculation.

### Why it happens

1. `AnankeEntity.initializeMyth()` runs during `finalizeSpawn`, before `addFreshEntity`. It learns and masters the intended skills through `HunterData.learnReward` and explicitly enables only the four supports above.
2. Native `AttributeHandler.initEntityExistence` subsequently processes Ananke's data profile. For each profile skill it creates a fresh instance, calls `SkillHelper.learnSkill`, then toggles that fresh instance and invokes its `onToggleOn`.
3. Native `SkillHelper.learnSkill` refuses to replace an already learned permanent instance. The profile initialization does not check that return value, so it toggles an **unstored instance** instead of the boss's actual learned instance.
4. Attribute side effects can still apply to the entity, while callbacks that consult the stored toggle remain disabled.

This leaves an inconsistent hybrid state. The observed raw melee/projectile dodge attributes were **30 / 100**, resistance degradation **1**, and Space Boost **5.25** despite several relevant stored toggles being OFF. These are raw attributes, not proof of guaranteed projectile immunity; native bypass/dodge rules still apply. Saving and reloading preserves the false flags and permanent modifiers. Space Boost initially read 3 on reload, then returned to 5.25 after Thoth resumed its transient contribution.

The original release checks covered skill presence and mastery but missed this activation defect. A fix needs one authoritative bootstrap path using the **actual stored instances**, once-only modifier application and save/load checks. Blindly re-running every `onToggleOn` risks duplicated or orphaned modifiers.

## Recovery is too small for the health pools

Observed on the project's current default configuration, with AI disabled so encounter reset did not interfere:

| Measurement | Result |
| --- | --- |
| Maximum HP / SHP | 48,000 / 120,000 |
| HP after removing 1,000 and waiting 40 ticks | 47,008: **4 HP/second**, about **0.0083% maximum HP/second** |
| SHP in the same wounded interval | 119,096: **48 SHP/second**, about **0.04% maximum SHP/second** |
| SHP after a separate 20 ticks with HP full | 119,040 from 119,000: **40 SHP/second** |

Mastered Self-Regeneration supplies 4 HP and 8 SHP per second under these defaults. Native base SHP recovery supplies the other 40 SHP/second. The Self-Regeneration effect returns early when HP is full, so its SHP supplement is absent then. Native/server configuration can change these rates.

Ignoring future damage, the observed HP rate would take approximately **200 minutes** to replenish the entire HP pool. SHP would take about 42–50 minutes at the measured rates. The full encounter reset after ten seconds without a valid target is separate from in-combat sustain.

Charybdis's Ultraspeed Regeneration and Nightmares' regeneration packages are far stronger. Copying native instant/full regeneration onto a boss with 15M magicules would create an opposite problem. A bounded, paid recovery mechanic with a delay or interruptible window is a better candidate.

## Offense and AI

Moira's four warned geometries and two active Thoth modes are a credible offensive core. Thoth ownership already gives Ananke the shared Godly classification traits: bonus damage against non-Godly owners, suppression for its own damage and protection against non-Godly magic interference. Those ownership traits are independent of the support-toggle defect.

The current AI leaves exploitable openings:

- Flight/chase is its main repositioning tool, whereas reference bosses actively teleport or dash. Merely owning Spatial Domination does not provide an AI gap closer: its active modes are not an automatic teleport routine, and Ananke never invokes them.
- Moira selection follows a fixed rotation without checking whether the target is in Fatebreaker's 16-block fan. The boss can pay for a cast and stop moving while its target is already out of reach.
- Starfall's plan lasts 150 ticks. Its last hit is at tick 88 in phase one and tick 112 in later phases, leaving approximately 3.1 or 1.9 seconds of stationary plan lifetime after the last strike. The AI also has a decision delay afterward, although movement resumes when the plan ends. Phase one has no Thoth support attacks.

Range-aware selection and limited spatial repositioning would improve pressure more coherently than adding more attack skills. Visual fade can continue after a damaging sequence ends without necessarily keeping the boss movement-locked.

## Defense coverage and recommended priorities

Magic Resistance is not blanket protection against every elemental skill: its native classifier checks Tensura magic damage. Physical, Spiritual and Holy defenses likewise have their own source classifications. Ananke lacks Hinata/Gazel/Veldora's dedicated elemental coverage.

Abnormal Condition Nullification already includes substantial status protection, including several vanilla debuffs and native burden/fragility/curse/poison/paralysis/petrification/hypnosis effects, **when actually ON**. Adding redundant poison defenses should not be the first response to the present activation bug.

Recommended order:

1. **Correct initialization and activation.** Prove physical, spiritual, magic, holy, status and pain defenses work through their native behavior; verify save/load and no duplicated support modifiers. Establish a consistent dodge baseline before increasing anything.
2. **Improve AI use of the existing package.** Gate short-range casts by range, add controlled repositioning and separate cosmetic fade from movement locks.
3. **Provide meaningful, bounded HP/SHP recovery.** Prefer a resource-funded recovery window with counterplay over unbounded instant regeneration. Set exact rates after the corrected baseline is measured.
4. **Add deliberate elemental coverage.** Light, Darkness and Space are thematic starting points, with other coverage chosen from actual endgame damage tests. Avoid copying a blanket all-nullification package or stacking barriers simply to increase the list.
5. **Test against Myths Godly players as well as ordinary endgame builds.** Godly users suppress resistances/nullifications for their own damage, so additional resistance skills alone cannot supply the intended challenge. Do not claim “strongest native boss” until direct combat tests support it.

The result should keep Moira and Thoth as Ananke's identity while making its supporting skills real, its recovery relevant and its decisions competent. The current version has the right offensive foundation, but its support package needs correction before a fair numerical rebalance.
