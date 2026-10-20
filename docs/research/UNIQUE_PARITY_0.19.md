# External Unique-skill audit of Tensura Myths 0.19.0

Inspected 2026-10-20 against Myths commit `2a853d3`. **Conclusion: the release
has a clearer internal Unique → Ultimate → Godly hierarchy, but its Uniques
have not achieved demonstrated parity with native Tensura or addon Uniques.**
Most of its readily repeatable fixed attack damage is high relative to the
inspected ordinary combat examples. Healing economics also need attention.
That does not mean every Myths kit beats every external Unique: penetration,
conditional counters, resource growth and special damage can reverse a matchup.

This is an audit and recommendation, **not a gameplay balance change**.
The [numeric evidence](UNIQUE_PARITY_0.19_DATA.json) records eight Myths budgets,
seven native configuration sections and ten addon configuration sections.

## Scope and evidence

- **Tensura: Reincarnated 2.0.1.3**, [file 8793360](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated/files/8793360):
  inspected the pinned binary, `UniqueSkillConfig`, actual skill implementations
  and relevant projectile/entity paths. The development world's generated
  `tensura/ability/skill/unique_config.toml` matches the cited native defaults.
- **Mysticism NeoForge 1.21.1, 2.2.1**, [project](https://modrinth.com/mod/tensura-mysticism):
  inspected its released binary/configuration and current Unique implementations.
  Unlike the Ultimate roster previously discussed, these Unique classes are
  present in this 1.21.1 JAR. SHA-256:
  `61e98fe4666cb2baa35396dca7d4e1bb7e41069f2ddfc4af9799b2bb5eb442d3`.
- **Nightmares NeoForge 1.21.1, 1.0.5.2.2.5**, [file 9120824](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated-nightmares/files/9120824):
  inspected `NightmareUniqueSkillConfig`, `main.uniques` implementations and
  Sun Wheel's attack helper. SHA-256:
  `892d77a3169a45c14c41efc277cae8204d2b0786fec6f5416b9ad7bcaa35dd04`.
  Its NeoForge minimum is 21.1.220, above this project's pinned 21.1.209;
  it was inspected separately and not installed into the runtime.

This is released-binary/configuration and source-path inspection, **not a new
cross-mod combat tournament or compatibility test**. Decompiler output and
third-party binaries remain outside the repository. No third-party implementation
is redistributed. Numbers below are configuration inputs/per-hit budgets,
not guaranteed full-cast HP damage or universal DPS.

**Correction to the old baseline:** native Black Lightning and Black Flame are
**Extra skills**, not Unique skills. Those earlier examples help assess elemental
attacks but cannot establish Unique parity. Likewise, the 0.19 native tests
prove behavior, payment and improvements against our former budgets; they do
not prove balance against external skills.

## Comparable native Uniques

| Native Unique | Verified defaults / implementation | What the comparison actually establishes |
| --- | --- | --- |
| Musician — Sonic Blast | 75 raw, 150 mastered; 50 MP; 1-second cooldown; 8 / 12-block range | Much smaller printed hits than our openers, but faster and cheaper. The ray iterates overlapping sample boxes; do not turn one printed hit into an assumed full-cast damage total. It excludes spiritual-race targets. |
| Absolute Severance — projectile | 50 raw, 300 mastered; 20,000 MP; 3-second cooldown; size 5 / 8; life 20 / 40 ticks | Higher Myths raw damage is not the whole matchup. `SeveranceCutterProjectile` explicitly adds resistance bypass 1, barrier bypass 1 and dodge bypass, and pierces entities/blocks. Myths Uniques have no such built-in penetration. |
| Fusionist — landmine | 300 raw, 500 mastered; 1,000 MP plus 5 matter; configured radius 25. Mastered reinforcement adds 50 damage / 5 radius for 10 more matter, up to radius 50. | Rune Smith's compact command has substantially greater per-detonation raw damage, but the native mine covers a much larger region and has a matter economy. Its placement and triggered entity are different from a direct area hit. |
| Healer | Can restore missing HP; 80 MP/HP, 40 mastered. Mastered healing also restores SHP at 60 MP/SHP; 5 / 3-second heal cooldown. | Native recovery scales its bill with the actual points restored. Verdant's fixed-cost percentage recovery becomes far more economical as maximum pools and beneficiary count rise, though Healer can recover immediately. |
| Guardian | Iron Wall gives 15 / 40 armor and 0.4 / 1 knockback resistance. Protection grants allies 10 armor and 30 barrier, radius 25, duration 3,600 ticks; activation base cost 100 MP. | Substantial defensive utility is legitimate at Unique tier. Our water/ice defenses do not need an automatic blanket reduction simply because they include barriers. Native effect/barrier types are not interchangeable. |
| Reflector | Held Full Counter returns eligible incoming damage at 2.5×, with a 5-second cooldown and exclusions. Echo attack is 3× / 5× stored damage; storage limit is 100 + floor(EP × 0.001). | A native Unique can far exceed our fixed attacks after preparation or against a powerful hit. This has conditions and an EP-dependent ceiling, not free repeatable fixed burst. |
| Martial Master | 1.5 battlewill multiplier, 25% melee dodge, 100% projectile dodge setting, 15 / 20-block Ultra movement and 15 / 75 bonus damage; Aura cost 100 | Mobility/defense/cross-skill amplification can be a Unique's main value. Comparing only its movement hit to Thunder Step would understate the native kit. |

Native references are `config.ability.skill.UniqueSkillConfig` and the matching
`ability.skill.unique.*Skill` classes; projectile penetration is in
`entity.projectile.magic.SeveranceCutterProjectile`.

## Comparable released addon Uniques

| Addon Unique | Verified budget / mechanics | Interpretation |
| --- | --- | --- |
| Mysticism Zekrom | Bolt Strike sets 150 lightning damage, costs 800 MP, cooldown 20 / 10 seconds, radius 7.5. Toggle adds 1.5 to Lightning Boost. Acquisition checks 100K EP and lightning/dragon essence. | Storm Crown's 550 raw / 3-second aimed chain is aggressive even before its mastery bonus. Zekrom's stronger elemental attribute, area lightning entity, other modes and acquisition conditions must also be considered. The 150 is a hit input, not an asserted entire-bolt total. |
| Mysticism Reshiram | Blue Flare 60 raw, 800 MP, cooldown 10 / 5 seconds, radius 20. Fusion Flare 200 raw, 700 MP, 20-second cooldown; cross-skill combination bonus. Toggle adds 1.5 Flame Boost. | Myths Black Flame has much larger fixed lash/ignition numbers and quicker repeated use. Reshiram has wide area attacks, different projectile/field behavior and its own strong boost/combinations. |
| Mysticism Kyurem | Glaciate damage grows in 10-point increments to a 100 / 200 cap; 900 MP, 30-second cooldown. Pulse 40, 700 MP, cooldown 10 / 5 seconds; other freezing/field modes. | Frostbound's immediate 700 Ice / 3-second Avalanche is well above the inspected ordinary ice damage inputs. Freeze-source interactions, buildup and distinct field mechanics prevent equating every printed point across the two. |
| Mysticism Restricted | 5× native Battlewill damage while toggled; adds 2× the owner's attack attribute to eligible melee hits. Suppresses magicule gain and boosts aura growth/regeneration; dash costs 500 AP and counter 1,500 AP. | A low printed dash bonus hides powerful amplification and a major resource tradeoff. This is a counterexample to declaring all external Uniques weaker from fixed hit values. |
| Mysticism Engineer | Build a dispenser for 500 MP after a 100-tick held build. It grows through owner proximity: 2,400 ticks for level 2, 6,000 for level 3. Level 3 heals allied HP by 5% and restores 3% of maximum aura/MP every 60 ticks; up to two dispensers. | Percentage support is not inherently above Unique tier. It requires a stationary, destructible, matured construct; it does not restore allied SHP. Verdant is immediately deployable, heals 7% HP and 6% SHP per second for eight seconds, then has downtime. Engineer trades slower HP recovery for persistent energy support. |
| Nightmares Sun Wheel | At mastered 100% ignition: Sun Circle 100 raw / 2,000 MP / 3 seconds; Fire Moon 100 / 3,000 MP / 6 seconds, with a 50-damage fire wall. Red Sun channels 200 hits every 10 held ticks, costs 1,500 MP initially and per 20 held ticks, with 2-second release cooldown. Red Moon 300 / 8,000 MP / 30 seconds plus burnout. Unmastered ignition is capped at 30%. | Myths Black Flame has markedly larger fixed damage inputs, especially its ignition opening plus damage over time. Sun Wheel has channel/field duration, ignition and burnout mechanics, so 100 or 300 is not its whole-kit damage. |
| Nightmares Freezing Flame | White coating adds 50 / 100 flat Attack Damage and an associated effect; activation cost configured 0. Afterworld costs 5,000 MP with a 30-second cooldown. | A native-style Unique can be strong through melee and special utility even without a large fixed area burst. It is not a direct Oil Lash damage comparator. |
| Nightmares Elementalist | Actual passive implementation grants 30% / 40% across eight elemental attributes. | Rune Smith's +50% across three elements is high but defensible as a narrower specialty; the passive alone is not the main outlier. |
| Nightmares Ending | Pull deals 44 **Disintegration**, costs 4,444 MP, cooldown 44 seconds, alongside other utility. | Comparing 44 directly to our 450–1,000 ordinary elemental hits would be misleading: the native damage type is different. |
| Nightmares Stasis | Fixed state defaults to 600 ticks with a 60-second cooldown; air wall upkeep 1,000 MP/second. | Control and defenses need their own cost/duration assessment; there is no single numeric damage target for the whole Unique tier. |

Configuration facts are preserved in the JSON. The Engineer dispenser timing
and healing were checked in `entity.skill.DispenserEntity`, not inferred from
the configuration names. Sun Wheel scaling/cadence was checked in
`main.uniques.SunWheelSkill` and `main.flame.SunWheelHelper`.

## Assessment of all eight Myths Uniques

The following assumes elemental boost base 1, full mastery, only the skill's own
toggle, and **no** Godly ownership, equipment or other skill bonuses. Myths mastery
is 1.5×; an owned +50% elemental toggle adds another 1.5× where applicable.
The numbers still precede native authority, defenses and hit handling.

| Myths Unique | Current representative output | Assessment |
| --- | --- | --- |
| Storm Crown | Railbolt 550 raw → 1,237.5 with mastery/own toggle; up to four distinct victims with diminishing hops; 800 AP / 3 seconds. Thunder Step 350 raw, 16-block movement / 4 seconds. | **High repeatable attack budget.** Long reach, wall-ignoring ricochets and an inexpensive escape make its complete offensive package generous. A priority for external damage/cadence calibration. |
| Scarlet Deluge | Current 420 raw → 945, or 1,417.5 against already drenched targets; 600 AP / 3 seconds. Undertow 200 opening + 110 per later second, with crowd control and personal defense. | **High offense; coherent Unique utility.** Pay particular attention to the drenched bonus plus wave cadence. Its modest native barriers are not the main issue. |
| Black Flame | Lash 450 raw → 1,012.5; ignition 650 → 1,462.5, then 337.5 per later pulse; 1,800 AP / 7 seconds. Overlapping own pools are deduplicated. | **High prepared burst and sustained area damage.** Its preparation is real, but fallback ignition can also create a pool without a prior lash. Compare whole casts and positioning rather than treating all of that power as setup-gated. |
| Frostbound Colossus | Avalanche 700 raw → 1,575 every 3 seconds for 800 AP; Prison 300 raw plus native Frost. Stable barrier/armor utility. | **High immediate area burst.** One of the clearest direct-damage candidates for reduction relative to the inspected ordinary ice inputs; preserve its distinct capture/defense role. |
| Threadweaver | Needle 450 → 675; tether-consuming Sever 800 → 1,200, for 600 + 1,000 AP. Requires a live visible victim and two activations. | **High burst, with meaningful restrictions.** Its lack of native penetration and required tether matter. Less reason for a blanket utility nerf than for calibrating the burst reward. |
| Rune Smith | Proximity 650 → 1,462.5, or command 1,000 → 2,250; placement + command costs 1,400 AP. One compact mine; command can fire before proximity arming. | **High per-detonation budget.** Damage exceeds the native Fusionist default substantially, but Fusionist has much larger area. Count both aura payments and account for the command bypassing the warning/arming delay. |
| Verdant Keeper | Grove restores 7% HP and 6% SHP per second for eight seconds to up to eight beneficiaries, 1,600 AP, 14-second cooldown. Briar 500 → 750; passive +100% natural SHP regeneration. | **Healing-economy concern, not a simple damage complaint.** Its bill is independent of recipient count and total HP/SHP restored. High-pool group healing is especially favorable versus Healer. Engineer shows that persistent percentage support can still belong at Unique tier, with other tradeoffs. |
| Mirrorbound | Primary 600 → 1,350; two secondary 350 → 787.5 hits. 750 AP / 3 seconds. Six-second projectile reflection, cooldown 10 seconds. | **High direct light damage; reflection utility is not inherently excessive.** Native Reflector's conditional counter can be stronger against a single major attack; Mirrorbound is stronger against streams during its active window. |

### Healing example

For one beneficiary with maximum **1,000 HP / 10,000 SHP**, continuously missing
enough health, eight Grove pulses can restore **560 HP / 4,800 SHP** for the one
1,600-AP cast payment. Native mastered Healer's configured bill for those same
points is **310,400 MP**: `560 × 40 + 4,800 × 60`.

This illustrates a scaling difference, not a claim that every Grove cast delivers
that healing. Staying in range, time, pool caps, ally eligibility and healing
suppression matter; Healer can act immediately. Grove's own toggle, if enabled,
adds upkeep but not a per-point healing bill. Eight eligible wounded beneficiaries
increase the potential benefit without increasing the cast payment.

Engineer is an important counterexample: two fully developed level-3 dispensers
can offer persistent percentage HP recovery and energy regeneration without
Healer's per-point bill. Grove has more immediate HP/SHP throughput and mobility
of deployment, while dispensers have persistent resource support and setup risk.
It would be wrong to use Healer alone to declare all percentage healing invalid.

## Recommendation

1. **Treat 0.19 as an internally calibrated release, not confirmed external parity.**
   A large gap beneath our own Ultimates does not independently validate its
   power against another project's Uniques.
2. **Prioritize repeatable damage** on Storm Crown, Frostbound, Rune Smith and
   Black Flame; then review wet Current and Mirrorbound's primary. Preserve the
   mechanics that distinguish each kit instead of applying one global nerf.
3. **Review Verdant's actual healing economy.** A bill tied to restored points,
   a per-cast healing budget, or a per-beneficiary resource cost could control
   scaling. Pick one coherent approach after group/high-SHP measurements;
   percentage healing itself need not disappear.
4. **Keep strong utility in perspective.** Native penetration, countering,
   resource tradeoffs, special damage and permanent constructs make some
   external Uniques extremely strong despite small tooltip values. Threadweaver
   and Mirrorbound in particular need role-based comparisons.
5. **Separate AP from MP and exclude Godly ownership during Unique testing.**
   Hunters normally gain aura/magicules at 2:1. Equal printed AP/MP prices are
   not equal opportunity costs. Owning any Godly skill also upgrades ordinary
   damage through shared traits; merely switching off its toggle is insufficient
   isolation. Strongest-source ordinary elemental rules apply across native
   and addon skills; Thoth adds a separate Godly contribution.

A defensible numerical correction should use matched base/mastered loadouts,
ordinary and resistant targets, single enemies and groups, and full-cast/time/
resource measurements in a compatible addon profile. This audit supplies the
verified reference inputs and identifies what to measure; it does not invent
an exact universal damage target or claim those cross-mod runtime trials occurred.
