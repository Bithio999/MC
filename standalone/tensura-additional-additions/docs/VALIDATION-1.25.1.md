# Additions 1.25.1 — Timed Against All Odds

The House Always Wins is removed from casting, damage calculation, critical resolution, dodge bypass and rendering. Its protocol number is retired so an old visual cannot become another effect. Existing `TycheHouseTicks` data and the old field cooldown are removed when the skill is processed, preserving Dividend’s index 0 and Fate Insurance’s index 2/history. An old field-mode preset now opens the passive information screen without charging resources. Dividend is the only combat mode in the key cycle.

Against All Odds triggers while the passive is toggled on and either current HP or SHP is at or below half its corresponding positive maximum. It grants the full +50% attack-damage and +25-point melee/projectile evasion bonuses for 20 seconds, with a 60-second native cooldown beginning at activation. Healing does not cancel or scale the timed buff. Toggling off, forgetting the skill, respawning, logging out or changing dimension removes the active modifiers, retaining the cooldown. Native serialization retains duration and cooldown. A low pool can trigger again after both timers allow it. Duration/cooldown are server-configurable.

Tuner research used the shipped `TunerSkill`, `FateChangeEffect` and `UniqueSkillConfig.Tuner` classes. Native mastered Tuner applies amplifier 1, doubling each configured bonus. Tyche adds only the missing categories: `bonusAttackSpeed × 3`, `bonusSwim × 3` and `bonusSpeed × 2.2`. The first two bonuses are 50% stronger than mastered Tuner’s; movement is only 10% stronger. Default attribute additions are +0.6 attack speed, +3 swim multiplier and +0.088 movement speed. Attack damage and evasion keep their existing Tyche values; no native Fate Change effect, duplicate flat damage or duplicate evasion layer is added.

Threshold checks run after both physical and direct spiritual damage, with server ticks covering other stat changes. Attribute IDs are stable and refreshed by replacement, so repeated checks do not stack modifiers or extend the active window. The status screen shows active seconds and remaining cooldown. Fate Insurance still clears other ability cooldowns, including Against All Odds, without clearing its own history timer.

## Native tests

All **26 focused native cases** passed on **Tensura 2.0.1.3 / NeoForge 21.1.209** and **Tensura 2.0.1.4 / NeoForge 21.1.248**. Both runs exited successfully after normal server shutdown. This is targeted runtime compatibility, not a complete modpack playthrough.

The suite includes 13 Tyche cases and 13 Critical Merchant regressions. New/revised cases cover exact HP/SHP thresholds, full buff strength, stronger configured speed bonuses, absence of duplicate damage/evasion, persistence through healing, repeated-refresh stability, precise expiry, toggle cooldown protection, retrigger after cooldown, native serialization, both damage entry paths, removal of old field state, native mode migration and unchanged insurance data. Revival, native fatal-hit dispatch, synthesis/cost/preset carryover and guaranteed criticals remain covered.

```sh
./gradlew test runGameTestServer -I docs/review-high-realm/server.init.gradle -PtycheFocus=true -PmentorFreshReview=tyche-rework
```

For the second API profile, add `-PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/api-runtime.init.gradle`. Run profiles sequentially.

## Client and package

The actual-client review passed synthesis and payment, compact/normal bounds, stable confirmation refreshes, Dividend, full Against All Odds activation at half HP and persistence after healing, and an actual fatal player hit with complete resource restoration and correct cooldown reset. Captured and inspected the updated passive timers, native Ultimate menu, revival and absence of the old field. See the [client review](review-tyche/README.md).

The final clean production build passed **69 JVM tests**, with no failures, errors or skips. The distribution audit verified 1.25.1 metadata, registered classes/mixins, exact assets and guide entries, and absence of review fixtures or bundled dependencies. The unchanged archive context audit also passed. Release SHA-256: `e0edb8ef20946e1ff37b3f57a44d7407e9aa61472c16d68ac2f116f4ca4b145b`.
