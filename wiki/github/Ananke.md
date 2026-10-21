# Ananke, the Unwritten End

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Myth bosses](https://github.com/Bithio999/MC/wiki/Bosses) · [Moira](https://github.com/Bithio999/MC/wiki/Moira) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

**Ananke, the Unwritten End** is the first Myth of Tensura Myths: a six-winged arbiter that guards the final page of an unwritten world. It combines fixed, readable attack patterns with native Ultimate and Godly skills.

![Ananke's Eclipse Seraph crest](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/race/eclipse_seraph.png?raw=true)

| Boss data | Ananke, the Unwritten End |
| --- | --- |
| **Information** | |
| Race | [Eclipse Seraph](https://github.com/Bithio999/MC/wiki/Eclipse-Seraph) |
| Classification | Myth boss; native True Demon Lord |
| Base EP | 30,000,000 |
| Aura / magicules | 15,000,000 / 15,000,000 |
| Maximum HP / SHP | 48,000 / 120,000 |
| Armor / toughness | 40 / 20 |
| Base melee damage | 180; native mob attack |
| Entity ID | `wildhunt:ananke` |
| **Skills** | |
| Signature Ultimate | [Moira, Lord of Finality](https://github.com/Bithio999/MC/wiki/Moira) |
| Godly Skill | [Thoth, God of Magic](https://github.com/Bithio999/MC/wiki/Thoth) |
| Learned package | 18 mastered skills |
| **Spawning** | |
| Survival | Writ of Last Dawn on a lodestone |
| Natural spawning | None |
| Operator summon | `/myths boss summon` |

## Summoning

Craft a **Writ of Last Dawn** (`wildhunt:writ_of_last_dawn`) with this shaped recipe:

| | | |
| --- | --- | --- |
| End Crystal | Echo Shard | End Crystal |
| Dragon's Breath | Nether Star | Dragon's Breath |
| End Crystal | Echo Shard | End Crystal |

Use it on a lodestone with room for a 1.6-block-wide, 4.1-block-tall entity above it. Prepare at least 32 blocks of clear fighting space around the spawn and more room to retreat. The ritual requires a hostile difficulty and no Ananke within 128 blocks. Failure leaves the writ intact; creative players keep it on success.

Spawn eggs and `/summon wildhunt:ananke` also work for operators. Ordinary ritual summons use the current world; the encounter does not generate a dimension or build an arena for you.

## Combat

Ananke flies after valid targets, fights nearby players and retaliates when attacked. Its arena anchor limits valid targets to 72 blocks from the spawn. Both **HP and SHP bossbars** appear at the upper right to tracking players within 80 blocks, clear of Tensura’s player HUD. The health bar shows phase I, II or III, and switches to the gold-refuge warning during Last Dawn.

| Phase | HP threshold | Changes |
| --- | --- | --- |
| Sealed Testament | Above 65% | Fatebreaker, six-star Starfall Canticle, Eventide Procession and melee |
| Unwritten Crown | 65% or less | Faster decisions, eight-star Starfall, Astral Library and Spellbreak |
| Last Dawn | 30% or less | Fastest decisions; The Last Dawn joins the rotation |

Phases only advance during an encounter; healing does not relock a phase. Phase changes announce themselves with a six-point ascension effect. There is no forced invulnerability window or incoming damage cap. All attacks keep native hit immunity.

### Moira's rites

See [Moira](https://github.com/Bithio999/MC/wiki/Moira) for individual costs and full per-ability damage tables.

| Rite | Damage type | Fixed raw damage | Warning and counterplay |
| --- | --- | ---: | --- |
| Fatebreaker | Aura slash — `tensura:aura_slash` | 1,800 | 1.2 s warning; leave the locked 110° fan within 16 blocks |
| Starfall Canticle | Light — `tensura:light_elemental` | 1,000 per star | Each three-block-radius marker gives 1.4 s warning; later markers track new positions, existing markers stay fixed |
| Eventide Procession | Darkness — `tensura:darkness_elemental` | 2,200 once per victim | 1.5 s preparation; dodge the moving hollow wave, stay behind its front or move above its damage band |
| The Last Dawn | Space — `tensura:space_elemental` | 4,800 | 3.75 s warning; stand in the **gold ring 7–12 blocks** from the cast center, or leave the 32-block sphere |

The gold refuge protects against **The Last Dawn only**. It does not protect against melee, Thoth or other simultaneous attacks. All rite origins lock when casting begins. No EP multiplier applies to their damage; Thoth's elemental boosts and native combat handling are separate.

### Godly authority and Thoth

Ananke owns Thoth and therefore has the shared [Godly classification passives](https://github.com/Bithio999/MC/wiki/Godly#classification-passives): +35% damage against non-Godly owners, resistance/nullification penetration for its own damage, and immunity to non-Godly-origin Magic Interference. Other Godly owners prevent the ownership-based damage bonus against themselves; they do not disable penetration.

Ananke enables **Arcane Sovereignty**, paying its normal 180-magicule-per-second upkeep, for elemental boosts and native chanting/cost attributes. From phase two it uses:

- **Astral Library:** three books take turns firing every 0.25 seconds for ten seconds, cycling Fire, Water, Lightning, Earth, Wind, Light and Space. Each bolt is 800 raw damage before the ordinary 1.5× mastered multiplier and native modifiers. Cost 24,000 magicules; cooldown 35 seconds.
- **Spellbreak:** 900 raw magic damage before 1.5× mastery, plus Thoth's native interference/drain rules. Godly owners take the damage but are exempt from this mode’s drain and disruption. Cost 6,500 magicules; cooldown 15 seconds. See [Spellbreak's full behavior](https://github.com/Bithio999/MC/wiki/Thoth#spellbreak).

Moira itself is **Ultimate**, so Lugh's Countermand can seal and interrupt its rites. Thoth is Godly and cannot be sealed by Countermand. Native activation restrictions can also stop a cast. Ananke has finite native resources; the AI pays skill costs and does not refill resources during an active fight.

### Native skill package

| Skill | Registry ID | Use |
| --- | --- | --- |
| Moira, Lord of Finality | `wildhunt:moira` | Four autonomous encounter rites; aura slash, Light, Darkness and Space damage |
| Thoth, God of Magic | `wildhunt:thoth` | Godly ownership, paid Arcane Sovereignty, Astral Library and Spellbreak |
| Mathematician | `tensura:mathematician` | Enabled native analysis and support; no invented extra NPC attack |
| Observer | `tensura:observer` | Enabled native perception and dodge support; no invented extra NPC attack |
| Universal Perception | `tensura:universal_perception` | Enabled native perception toggle |
| Thought Acceleration | `tensura:thought_acceleration` | Enabled native support toggle |
| Chant Annulment | `tensura:chant_annulment` | Enabled native support toggle |
| Spatial Domination | `tensura:spatial_domination` | Enabled native elemental support; the encounter has a separate paid repositioning action |
| Self-Regeneration | `tensura:self_regeneration` | Enabled native sustained recovery; respects healing suppression |
| Physical Attack Resistance | `tensura:physical_attack_resistance` | Native physical defense |
| Spiritual Attack Resistance | `tensura:spiritual_attack_resistance` | Native spiritual defense |
| Magic Resistance | `tensura:magic_resistance` | Native magical defense |
| Abnormal Condition Nullification | `tensura:abnormal_condition_nullification` | Native status protection |
| Pain Nullification | `tensura:pain_nullification` | Native pain protection |
| Holy Attack Resistance | `tensura:holy_attack_resistance` | Native holy defense |
| Light Attack Resistance | `tensura:light_attack_resistance` | Dedicated native Light defense |
| Darkness Attack Resistance | `tensura:darkness_attack_resistance` | Dedicated native Darkness defense |
| Spatial Attack Resistance | `tensura:spatial_attack_resistance` | Dedicated native Space defense |

All eighteen are registered, learned and mastered skills. Native supports are enabled on their actual stored instances. Native server configuration can change their effects. Owning a support skill does not imply every player-only mode has custom boss AI. Self-Regeneration is gradual; Ananke does not have Ultraspeed or Infinite Regeneration, a stacking barrier passive or automatic phase healing.

### Unwritten Renewal

[Active - Boss recovery] — **Unwritten Renewal**

| Ability information | Value |
| --- | --- |
| Damage type / Damage ID | None — recovery only |
| Starting condition | Below 85% HP or 80% SHP; four seconds without HP/SHP damage |
| Windup | Two seconds |
| Recovery | Four pulses, one second apart; each restores up to 3% maximum HP and SHP |
| Maximum sequence recovery | 12% maximum HP and SHP |
| Resource cost | 5,000 magicules to begin, then 50 per restored HP and 20 per restored SHP |
| Cooldown | 30 seconds from the start, including interrupted attempts |

Gold scripture gathers into Ananke's core before recovery begins. Its bossbar displays **Unwritten Renewal**. Deal a hit or damage its spiritual body to interrupt the sequence; no invulnerability or new barrier protects the channel. Native healing suppression and Lugh's Countermand against Moira can also stop it. Last Dawn takes priority when ready and in range.

Recovery clamps at the native HP/SHP limits, respects severance, and scales both pools proportionally if magicules cannot fund a full pulse. Reduced or cancelled HP healing does not consume its unused recovery budget. The passive Self-Regeneration remains separate. Reload cancels an unfinished sequence and preserves its remaining cooldown.

### Spatial Reposition

[Active - Boss movement] — **Spatial Reposition**

| Ability information | Value |
| --- | --- |
| Damage type / Damage ID | None — movement only |
| Trigger distance | Target more than 22 and no more than 64 blocks away |
| Destination | Approximately ten blocks from the target, within 60 blocks of the encounter anchor |
| Resource cost / cooldown | 20,000 magicules / ten seconds |
| Follow-through | One second before the next attack decision |

Between casts, Ananke can dissolve into violet and gold seals and reappear nearer a distant opponent. It checks loaded chunks, solid blocks, liquids and the world border before spending magicules. It never moves the origin of an active rite. This is an encounter movement action, not an automatic player mode of Spatial Domination.

Moira selection checks range before payment: the boss prefers a usable distant Starfall over a fan that cannot reach. Movement resumes eight ticks after a rite's final damaging frame while its remaining visual fades at the original position.

### Leaving and reloading

After **10 seconds without a valid target**, Ananke resets to phase one and restores its HP, SHP and resources. It moves back toward its original anchor and does not despawn through ordinary distance rules.

Saving/loading preserves HP, resources, race, learned skills and remaining recovery/repositioning cooldowns. Pending rites are cancelled on reload, preventing an invisible attack from resuming. Death, removal, interruption and encounter reset cancel owned visual/attack plans.

## Rewards

A player-attributed kill drops **one Dawn Testament** (`wildhunt:dawn_testament`) and **1–3 Nether Stars**. The boss grants 1,500 base experience; the **First Myth** challenge advancement grants 2,000 experience. Native loot/experience rules still apply. Ananke is in the native boss tags and counts as one distinct boss type for Hunter progression.

Drink a Dawn Testament for **2.4 seconds** to heal **50% maximum HP**, restore **50% maximum SHP**, and fill aura and magicules to their current native caps. HP healing uses the native heal event; SHP restores directly to its native pool. Both clamp at the current maximum. The consumable starts a **five-minute cooldown**. It does not grant EP, change race or unlock Moira.

## Gallery

![Ananke’s original six-winged model and halo](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke.png?raw=true)

![Native HP and spiritual-health bossbars](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-bossbars.png?raw=true)

![Fatebreaker’s fixed fan warning](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-fatebreaker.png?raw=true)

![Starfall Canticle’s fixed seals and descending stellar lance](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-starfall.png?raw=true)

![Eventide Procession’s hollow moving front](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-procession.png?raw=true)

![The Last Dawn’s dark sun and golden refuge annulus](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-last-dawn.png?raw=true)

[Watch The Last Dawn in the Minecraft client](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-last-dawn.mp4).

Captures use an isolated validation arena. Warning stills pause the native encounter at specific moments; the clip shows the running cast. No shader pack or external rendering is used.

![Unwritten Renewal gathering scripture before its first recovery pulse](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-renewal.png?raw=true)

![Spatial Reposition leaving violet and gold rings at its destination](https://github.com/Bithio999/MC/blob/main/wiki/media/ananke-reposition.png?raw=true)

## Native boss arena

For a managed encounter, stand at the center of a prepared arena and run:

```mcfunction
/myths boss arena
/bossFight join @s ananke
```

The first command requires operator permission and creates a native profile named `ananke` in the current dimension: radius 48, up to four joined players, five-second start delay, fifteen-minute timer. It sets the boss spawn one block above the center and the entrance 24 blocks along +Z. The native arena system handles the fight lifecycle. Build safe ground and clear space at both locations first.

The profile is saved in the world's native `data/boss_fights/` configuration. Existing `ananke` profiles are not overwritten. Use Tensura's `/bossFight` editor for adjustments; the join command also needs the permissions required by the base mod. Ananke's arena targeting is restricted to joined players. Boss-area-dimension restrictions are not automatically added to an ordinary lodestone ritual.

---

**Categories:** Tensura Myths · Bosses
