# 1.1.10 validation — Schema Strength Build

Both distributions add **Strength Build: +5 base attack, +2% damage taken per stack**, and lower Mitochondrial Build’s outgoing penalty to **5% per stack**. Existing Mitochondrial trees retain their IDs and order. Mixed effects share mastery weights and six slots.

Passed **136 standalone native GameTests**, **45 exact-pack integration GameTests** and **370 full-mod native GameTests**, plus **11 JUnit tests**. The suites check actual weapon-inclusive attack modifiers, one-time Chained scaling, full drawbacks, melee/projectile/magic/environmental/direct-SHP damage, save validation, native learning and recovery. The full 263-project client review passed. Its fixture exercises both Add buttons, ordering, real Apply packets, mastery previews, draft cancellation and cleanup. Optional fixtures are excluded from the independent production JAR.

[Full validation](../../../docs/VALIDATION-0.35.9.md) · [Player guide](../../../wiki/pages/Schema.md).

Initial and mastered mixed builds matched the server’s actual regeneration/attack values, with −15% outgoing damage and +6% incoming damage. Screenshots are included in the full validation report. Production builds, independent distribution/asset checks and hashes also passed.
