# Additions 1.25.0 — Tyche, Lord of Fortune

Tyche is a native Ultimate skill registered as `heavenlyrestriction:tyche`. Its synthesis replaces fully mastered Critical Merchant and Tuner after completed true awakening and configurable achievements, with a configurable permanent magicule price and no EP gate. Skill presets, pending guaranteed hits and native Tuner lockouts carry over. A discoverable unfinished native Ultimate entry opens server-validated synthesis confirmation. Shift + Dividend opens live insurance history without charging magicules or requiring Dividend’s cooldown to expire.

Fate Insurance follows native Tuner’s incoming damage-message grouping and fatal-hit exclusions. It restores current-maximum HP/SHP/magicules/aura, removes harmful effects and clears other native skill cooldowns. Its separate native cooldown slot is the five-minute history-reset clock. New damage types may revive during that window, repeated types cannot, later revivals never extend it, and revival never clears its own timer/history. Native serialization retains both. The two castable combat modes remain Dividend and House; the clock's native entry is informational.

## Native integration

The focused suite has **24 cases**: 11 Tyche integration cases and 13 existing Critical Merchant regressions. All passed on **Tensura 2.0.1.3 / NeoForge 21.1.209** and **Tensura 2.0.1.4 / NeoForge 21.1.248**, using isolated native review saves. Both servers shut down normally and Gradle exited successfully. These are targeted API/runtime profiles, not a full playthrough of every installed modpack combination.

Coverage includes complete resource restoration and harmful-effect cleanup, other cooldown resets with own-clock preservation, repeated/new damage types, a fixed window and expiration, native instance serialization, void/kill exceptions, an actual fatal-hit dispatch, Dividend payment and blocked-hit refunds, current-health adversity without stacking, House payment/range/cleanup, native critical suppression, scoped dodge bypass, full synthesis requirements/payment/parent replacement, retained pending hits/history/presets, and prevention of ordinary learning bypass. Existing melee, projectile and direct spiritual critical behavior remains covered by the parent regressions. NeoForge FakePlayer intentionally forbids player-on-player harm; the native critical-suppression fixture uses an eligible living defender and separately verifies that PvP restrictions are honored.

Reproduce from this project with Java 21:

```sh
./gradlew test runGameTestServer -I docs/review-high-realm/server.init.gradle -PtycheFocus=true -PmentorFreshReview=tyche-check
```

For the second supported API, add `-PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/api-runtime.init.gradle`; the profile directory supplies the documented compatible compile libraries. Run profiles sequentially.

## Actual client

The opt-in [client review](review-tyche/README.md) uses a disposable save and real server/client packets. It passed synthesis payment and consumption, stable confirmation across heartbeat refreshes, normal/compact layouts, Dividend, House, and an actual fatal player hit. HP, SHP, magicules and aura were verified at their current maximums after revival; Dividend/House cooldowns reset while the insurance clock remained active. Gold effects, the icon, synthesis and live damage-history screens were captured and inspected. Review fixtures are excluded from the production package.

## Production package

The final **clean production build** passed **69 JVM tests**, with no failures, errors or skips. The distribution audit verified 1.25.0 metadata, the 24-page in-game guide, Tyche’s Ultimate tag and classes, its exact icon, all registered mixins, and absence of review fixtures or bundled dependencies. The unchanged archive’s full context audit also passed. Release SHA-256: `c31a37ee3b3f1d595d08ec4283014721078addde7fb3fdb6c278571ac18d985d`.
