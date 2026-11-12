# Additions 1.26.2 — Critical payout balance

Critical Merchant now applies **1.5 × 1.25ⁿ** to already-critical damage. Tyche applies **2 × 1.35ⁿ**. Here *n* is the number of successful continuation rolls, from zero to 100. Each roll retains its 50% success chance. Merchant stops at its first failure; Tyche can reroll its first failure once. Dividend evaluates two independent Tyche chains and chooses the higher result.

Both retain **100 successful rolls and no separate payout cap**, as requested. The proposed ×6/×12 payout ceilings were not implemented. The normal finite floating-point transport guard is retained; it is not a new gameplay cap. Native critical damage is applied once, followed by one skill payout. Weapon/projectile/spiritual damage types and defenses are unchanged.

The transaction state records successes and the actual payout, rather than a power-of-two exponent. Player feedback shows the decimal multiplier, with compact notation for very large payouts. Existing skill IDs, mastery, guarantee counts and saved cooldowns remain valid. Active prices, cooldowns and ten-hit guarantees are unchanged; Tyche’s synthesis, Against All Odds and Fate Insurance are unchanged.

## Verification

**27 native integration cases passed on each supported profile:** Tensura 2.0.1.3 / NeoForge 21.1.209 and Tensura 2.0.1.4 / NeoForge 21.1.248. Both servers saved and shut down normally, and Gradle exited successfully. The final clean baseline production build passed **79 JVM tests**, with no failures, errors or skips. Release SHA-256: `0c24e0d29b5170339c9ef5c1dec1276d9c2e15946ef196334923d514748cf817`.

JVM cases cover failed chains, successive compounds, a perfect 100-success run without a multiplier ceiling, a rescued Tyche failure without losing a success slot, one reroll only, selecting rather than multiplying Dividend chains, single-chain RNG consumption, payout formatting and finite transport at extreme damage.

The native critical suite checks payouts through actual player/mob criticals, Unbound interactions, owned projectiles, direct spiritual damage, training dummies, ordinary noncritical hits, accepted/refunded guarantees, costs and cooldowns. The new Tyche case compares repeated native hits with its native critical baseline, then checks Dividend while the passive is off. Existing Tyche revival, resource/cooldown restoration, timed buffs, synthesis and save/load cases remain in the focused suite.

```sh
./gradlew test runGameTestServer -I docs/review-high-realm/server.init.gradle -PtycheFocus=true -PmentorFreshReview=critical-balance
```

For Tensura 2.0.1.4, add `-PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/api-runtime.init.gradle`, with a separate disposable save tag. Run profiles sequentially, then produce a clean baseline build to exclude review fixtures. Native tests are targeted compatibility checks rather than a full modpack playthrough. Existing menu screenshots are labeled as predating this balance release.

The distribution audit passed: 1.26.2 metadata, required skill/payout classes and registered mixins, source-matching assets and guide entries, and exclusion of review fixtures and bundled dependencies. The unchanged archive context audit also passed.

Wiki verification passed for **211 articles, 140 ability entries and 212 Wiki exports**, including the formula descriptions, labeled historical screenshots and offline edition. Current repository URLs use the renamed project; the removed standalone project remains absent. Both changed in-game guide entries match their generator source.

The tracked-tree privacy audit found no matches. A separate scan of every new release-JAR entry, including compiled class contents, also found no private identity strings or credential patterns. Public commits use the generic contributor identity.
