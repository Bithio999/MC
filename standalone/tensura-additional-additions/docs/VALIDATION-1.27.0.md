# Additions 1.27.0 — Laplace, Lord of Equations

Laplace is an Ultimate synthesis of fully mastered Exponentiation and Mathematician. It has exactly Universal Formula and Equation Editor. Synthesis requires completed True Hero/True Demon Lord awakening, mastery of three distinct supported native skills and a configurable 250,000 permanent-magicule payment, affected by mpSkillCost. No EP threshold. Payment follows awakening; parents are replaced, inheritance is recorded for Chained, and all affected presets are remapped.

Universal Formula reuses the existing Exponentiation output pipeline, power 1.2, exclusions and provenance guards. It adds Mathematician’s configured passive attributes, Thought Acceleration and appraisal under its own removable modifier ID. Existing stronger chant/appraisal providers are preferred, and an active native Mathematician suppresses the duplicate inherited calculating layer. Forgetting or disabling removes the added modifiers.

## Equation adapters

Nine native action/mode entries cover Self-Regeneration, aspectual Fire, Wind Cutter, Water Cutter normal/repeat, Healing, Strength, Protection and Reinforcement. Only verified aspects are exposed. In particular, Water Cutter’s Magic component is editable; its native primary Water damage is not invented as a configurable spell constant. Other skills and addons have no generic fictional formula fallback.

Native spell cast scopes capture player inputs before payment, and native projectiles receive their paid-for primary/secondary components before spawning. Enhancement hooks replace duration only. Self-Regeneration keeps native pulse and upkeep clocks and suppression; its HP/SHP pulses are edited separately. Healing links cost to actual missing-HP-clamped output, bounds its native integer bill and scales partially funded edited recovery to the actual payment. Unedited actions retain their original behavior.

The arithmetic parser has no script, command, reflection, inventory or file operations. It limits each expression to 160 characters, 64 nodes and nesting depth 16; each action has at most four helpers and twelve total equations. Identifiers, dependencies, intervals, safe divisors, nonnegative outputs, finite results and constant powers from 0–3 are validated. New helpers feed supported outputs; they do not invent effect types. Saved data is revalidated against live native constants and mastery; invalid profiles fall back entirely to native values.

Three enabled edits are allowed, four mastered. Maximum combined output is 3× native, four mastered. The minimum bill for stronger output is native bill × [1 + 2 × (ratio − 1)²]; discounts are limited to 25%. Fixed bills price the strongest permitted state. Costs are previewed separately as requested, minimum and charged. Server sessions bind owner, nonce and dimension, expire after ten minutes and use bounded packet strings/maps. Save checks learned ownership, capacity, native mode, current mastery, active casting and a five-second change timer. Losing mastery also reduces the number of effective edits immediately. A timestamp from another World Jump clock cannot extend the five-second edit lock.

## Verification

- Baseline: **50 native integration cases passed** on Tensura 2.0.1.3 / NeoForge 21.1.209: 16 Laplace cases plus 34 existing Metis/automatic-casting cases. Subsequent focused baseline runs passed all 16 after the mastery-capacity and World Jump clock safeguards.
- API compatibility: **16 Laplace native cases passed** on Tensura 2.0.1.4 / NeoForge 21.1.248, including the final mastery-capacity safeguard. Both servers saved and shut down normally.
- Real baseline Minecraft client: native Ultimate menu entry/icon, synthesis requirements and confirmation/payment, learned action catalogue, aspect picker, editing, Balance cost, a real enabled Save request, persisted profile, parameter palette and helper creation passed. Widget bounds and overlap were checked at normal size and a forced 320×180 layout stress size. Screenshots are actual client captures. The native minimum GUI scale can choose a larger effective screen at a given window size; the stress test initializes the real screen at the smaller size explicitly.
- JVM formula cases cover dependency propagation and clamped-output overrides, precedence, functions and interval bounds, cycles, undefined inputs, unsafe division/powers, parser limits, output ceilings, helper limits, convex bills, discount floors and state-dependent upkeep pricing.
- Final clean production build passed **91 JVM tests**, with no failures, errors or skips. The JAR contains production classes and assets without the opt-in review fixtures. SHA-256: `16ffa124ad88c982905aaf69847e99787eae2b9a714782a41a5f29e110931f63`.

```sh
./gradlew runGameTestServer -I docs/review-high-realm/server.init.gradle -PlaplaceFocus -PlaplaceRegression -PmentorFreshReview=laplace-regression
```

For API-profile checks add `-PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/api-runtime.init.gradle` and use a separate save tag; omit laplaceRegression for the focused suite. Run Gradle profiles sequentially, then clean-build the baseline production JAR to exclude review fixtures. Native checks are targeted compatibility tests, not a complete modpack playthrough.

The guide generator’s existing Metis indentation was corrected while adding Laplace; the shipped new guide entry matches its generator literal. The discontinued Myths gameplay and guide were not regenerated.

Distribution validation passed for the 1.27.0 metadata, registered production classes and mixins, native dependencies, source-matching icon and 25-entry in-game guide, and exclusion of opt-in review fixtures. The unchanged archive context audit also passed. Wiki verification checked 212 articles, 142 ability records and 213 exported Wiki pages; the offline bundle includes the linked captures. The new release JAR was scanned entry-by-entry, including compiled classes, for private identity strings and credential patterns. No matches were found. Public commits use the generic contributor identity.
