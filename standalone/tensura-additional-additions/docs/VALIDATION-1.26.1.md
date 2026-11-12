# Additions 1.26.1 — Metis cleansing conditions

Metis now uses an explicit per-ability/per-mode policy for nine native cleansing modes: Antidote; Healer Heal; Chef; Lust Invigorate; Predator and Gluttony Isolation; Law Manipulation Abnormality Cleanse; Spatial Domination Spatial Cleanse; and Reverser Inverted Fusion [Buff]. Read-only conditions select the player as the target and distinguish pure cleansing from combined healing/cleansing. No native effects, resistance rules or removal callbacks are replaced.

## Native behavior audit

The policy follows Tensura's native callbacks and tags, rather than treating every negative effect as removable:

- Antidote's own activation predicate checks Poison, Nausea and Weakness, plus Fatal Poison at mastery. Its Fatal Poison treatment remains the native level/duration reduction.
- Healer's main self-heal removes Infection even at full HP. Its HP and mastered SHP healing triggers remain.
- Chef removes harmful effects outside `tensura:skill_effect`, clears Severance and heals HP. Invigorate heals and removes effects outside that tag; automation triggers only on harmful effects or low HP, although native Invigorate also removes unprotected beneficial effects when cast. Invigorate's native per-effect cost is zero in the audited version; Chef's per-level cost is checked, including Schema's cost penalty.
- Isolation removes harmful effects and clears Severance with an empty hand. Holding an item selects equipment processing in native code, so automatic Isolation pauses. Native short-circuit behavior may clear Severance before clearing remaining statuses on a later cast.
- Law Manipulation uses the native `getCleansingEffect()` predicate directly and recognizes Severance and `CookSkill.COOK` maximum-HP reduction separately. Native datapack tags remain authoritative.
- Learned Spatial Cleanse treats Severance only, preserving native costs and partial removal when resources are insufficient for a complete cure.
- Inverted Fusion [Buff] removes/reverses harmful effects outside `tensura:skill_debuff`, excluding Magicule Poison. Mastered automatic use requires an empty hand so its earlier item-processing branch cannot modify a held potion or engraving.

Enemy buff removal, passive revival cleansing and transformation cancellation are not reclassified as self-cleanses. Degenerate Separate remains an offensive/manual mode; its situational self-cleanse is not automated. Other addons' modes are not inferred from names alone. This is an explicit supported-mode policy, not a universal effect-removal detector.

## Casting safeguards

Pure cleanses can trigger at full HP and do not trigger on unrelated statuses or low HP alone. Combined modes remain available for ordinary healing. Native ownership, mode learning, cooldowns, resources, anti-skill and activation restrictions still apply. Skills retain normal costs; the existing Metis discount stays limited to aspectual/spiritual magic.

Before an automatic cast and during preparation, the mode's need is rechecked. A different thread curing the same status during the same update prevents a redundant subsequent cast. Item-sensitive modes are checked again as well. No menu, camera, movement, damage or progression behavior is changed.

## Verification

**34 native integration cases passed on each supported profile:** Tensura 2.0.1.3 / NeoForge 21.1.209 and Tensura 2.0.1.4 / NeoForge 21.1.248. Both servers finished saving and Gradle exited successfully. Eight added cases cover exact Antidote status/mastery checks, full-health Infection treatment, protected tags, per-level costs and Schema penalties, native Law/Cook/Severance removal, Spatial Cleanse learning and removal scope, item-sensitive modes, native cleansing callbacks without an enemy, and redundant-thread prevention. Existing assignment, analysis, synthesis, targeting, costs, cooldowns, parallel casting and hit-window regressions remain covered.

```sh
./gradlew test runGameTestServer -I docs/review-high-realm/server.init.gradle -PmetisFocus=true -PmentorFreshReview=metis-cleansing-final
```

For the second profile, add `-PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/api-runtime.init.gradle` and use a distinct disposable save tag. Profiles were run sequentially. Tests use disposable native GameTest players and real owned ability instances; this is targeted integration coverage, not a complete modpack playthrough. The GUI layout is unchanged from the actual-client-verified 1.26.0 release.

The final clean production build passed **76 JVM tests**, with no failures, errors or skips. Release SHA-256: `f6aae39142f5912aa703fa61ff7645bba15a832089c013244e2fb5c6145f1d2c`. The distribution audit passed, including 1.26.1 metadata, the packaged cleansing policy, registered mixins and assets, guide entries, absence of review fixtures/bundled dependencies, and the unchanged archive context audit.

Wiki verification passed for **215 articles, 146 ability entries and 216 GitHub Wiki exports**, including the mode-specific cleansing table and offline HTML bundle. The in-game Metis guide entry matches its generator source; the guide still has 24 entries.
