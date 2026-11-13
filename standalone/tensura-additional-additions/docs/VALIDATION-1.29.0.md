# Additions 1.29.0 — Laplace resource payment

Equation Editor keeps its balanced `magicule_cost` equation, presented as **Cost budget** in MP equivalents. A per-action payment plan splits that budget among magicules, aura, HP and SHP. The Payment dialog supplies one-resource presets, four percentage fields, derived conversion equations, live resource amounts and an explicit Use payment mix step. This changes the local draft; Apply & Enable saves it with the equations. Cancel preserves the earlier draft. Refreshes preserve unapplied payment changes. Existing profiles without payment metadata use 100% magicules.

Default server rates in `tensura-additions-laplace.toml` are 1 MP equivalent per aura and 10 per HP/SHP. The original cost floor and output limits apply to the budget before splitting. Native casting cost modifiers and automatic discounts are applied before converting the final bill. The board shows its budget before these additional native modifiers. Costs are not exponentiated.

## Payment behavior

- Native EnergyHelper skill dispatch and Healing's direct-consumption boundary establish a scoped payment policy. Narrow call-site hooks in Magic and EnergyHelper expose funding for the final modified bill and replace its MP write with one prepared resource transaction. Other owners, actions, ordinary direct energy calls and disabled/invalid profiles retain their native behavior. Scopes restore in `finally`.
- Every required pool is checked before any selected resource is written. HP/SHP payments leave at least 1 point, use direct resource deductions and do not become attacks or trigger hurt feedback/defense. Tiny positive fees round to a representable deduction instead of disappearing into a large float/double reserve. Unsafe converted bills fail validation before rendering or payment.
- Converted/mixed casts use the caster's pools and do not consume staff EP. Magicule-only casts retain native staff funding. Native creative exemptions, activation restrictions, damage types, timing, projectile counts, recovery suppression, cooldowns and progression are preserved.
- Healing and Self-Regeneration cannot select HP/SHP. Converted Healing prices actual missing-HP-clamped recovery, rounds its budget up before native discounts and requires the whole converted payment; failed mixes consume nothing and heal nothing. Magicule-only Healing retains its existing partial-payment behavior. Self-Regeneration retains native upkeep; enabled equation/payment changes require toggling it off first, while disabling remains available.
- Profiles persist normalized resource shares on the actual owned Laplace instance. Bounded version-2 requests validate all four finite nonnegative shares totaling 100%. Server-owned rates and action restrictions cannot be overridden by packets. Disable uses the saved profile and ignores packet equations/payment. Invalid saved metadata falls back to native behavior.

## Verification

- **66 native baseline cases passed** on Tensura 2.0.1.3 / NeoForge 21.1.209: 18 existing Laplace, 14 resource-payment and 34 Metis/automatic-casting cases.
- **66 native API-profile cases passed** on Tensura 2.0.1.4 / NeoForge 21.1.248 with the same combined suite. Both servers saved and shut down normally. These are focused native compatibility checks, not a complete modpack playthrough.
- New cases cover zero-MP aura-funded Fire, actual mixed payments and all-or-nothing failure, HP/SHP costs and reserve guards, native cost multipliers, converted Healing and rejected vital funding, no partial healing, regeneration upkeep and active-edit guard, serialized profiles/model/request shares, malformed packet/save rejection, independent Disable, repeated Water payments, Metis's single discount including Healing, and native-vs-converted staff behavior.
- An initial API combined run failed an older delayed Wind projectile survivor-count assertion. Its isolated run passed. The fixture now observes actual native projectile spawn events for Wind and repeated Water, verifies their count and edited components at creation, and unregisters its observer in `finally`; the final combined suites pass on both profiles. No projectile gameplay behavior was changed for that test adjustment.
- Actual native client interactions test the full equation-board workflow plus resource presets, four-way percentages, invalid totals, cancel preserving a draft, immediate bills, Apply & Enable server persistence, disabled healing vital presets and normal / forced 320×180 layout bounds. Captures use the isolated review save. The final capture pass includes the adjusted Payment button height so it clears the bill line.
- Ten additional JVM cases cover share/rate validation, four-resource conversion, recovery restrictions, prepared transactions, vital reserves, representable deductions at large capacities, fractional native discounts, retained convex budget floors, converted Healing rounding and excessive converted amounts rejected before use.

Native review commands (run profiles sequentially):

```sh
./gradlew runGameTestServer -I docs/review-high-realm/server.init.gradle -PlaplaceFocus -PlaplaceRegression -PmentorFreshReview=payment-baseline
./gradlew runGameTestServer -I docs/review-high-realm/server.init.gradle -I docs/review-high-realm/api-runtime.init.gradle -PneoOtherworldDirectory=/tmp/realm-api-profile -PlaplaceFocus -PlaplaceRegression -PmentorFreshReview=payment-api
```

Use `docs/review-laplace/client.init.gradle` only with its disposable review save. Review harnesses and fixtures are excluded from production packaging. The F8 entry is updated without regenerating the discontinued Myths guide, and matches its generator literal. The gameplay wiki includes rates, examples, restrictions and actual-client captures.

The final clean baseline production build passed **113 JVM tests**, with zero failures, errors or skips. Release SHA-256: `f52fa1fc8df7bc3e37091f9ff4035bd4be70c0245e4e5fa23902f6da8bd607ee`. Distribution validation checks 1.29.0 metadata, production payment classes and registered mixins, source-matching assets, the 25-entry guide and exclusion of opt-in review classes. Wiki and privacy audit results are recorded with the release.

Wiki verification passed for **212 articles, 142 ability records and 213 exported GitHub Wiki pages**, including the offline edition. The source guide has 25 entries, and Laplace matches its generator literal. The tracked-tree audit and a raw entry-by-entry JAR scan check private identity literals and credential patterns, including compiled classes. Public commits use the generic contributor identity. The discontinued Myths gameplay and F8 guide are unchanged.

Client captures: [payment mix](../../../wiki/media/laplace-payment-mixed.png), [saved bill](../../../wiki/media/laplace-payment-live.png), [healing guard](../../../wiki/media/laplace-payment-healing.png), [compact layout](../../../wiki/media/laplace-payment-compact.png).
