# Additions 1.30.0 — Expanded Laplace catalogue

Laplace’s Equation Editor now enumerates learned Tensura Reincarnated and Additional Additions skills, magic, Battlewills and passive callbacks. Only the `tensura` and `heavenlyrestriction` namespaces are accepted; Laplace cannot edit itself. Existing tailored boards and saved profiles are retained. Native mode-learning and slot restrictions are checked using copied instances, without activating abilities.

## Implementation and limits

A bounded, cached bytecode inspector discovers native numerical configuration reads reachable from the selected mode/callback. A generated optional Mixin target inventory covers both supported Tensura APIs. The plugin injects stack-neutral primitive read hooks; a separate pure transformer can be tested without booting Mixin. Per-owner callback scopes supply validated outputs and restore in `finally`. Shared configuration singletons are never written, and absent profiles preserve native values exactly, including sentinel values.

Explicit bridges cover Additions’ authored mining, Schema benefits, auto-cast range, Rush/Counter, Fusion duration/regeneration/damage, critical payout and aura recovery parameters. Native attribute templates are refreshed when edited. Sustained-state guards require ending the affected active state before applying enabled changes; stopping Universal Formula refreshes held bonuses and ends affected native toggles.

Expanded boards multiply the ratios of numerical increases against one shared 3× / mastered 4× limit. Decreasing another output cannot subsidize an increase. Integer effects round before pricing, retain at least one unit, and minimum/maximum pairs cannot be inverted. Convex cost floors, the 25% discount floor, three/four enabled actions, four helpers, five-second edit interval and bounded expression evaluation remain. Up to 128 equations accommodate discovered fields plus helpers; the largest audited native board is well below that bound.

Progression/acquisition/learning/mastery requirements, permanent EP gains, inventories, crafting/engraving rules, roll chances, native defensive-reduction coefficients, cast timing and cooldown constants remain protected. Some actions have only an editable actual resource bill; entirely fixed actions are labelled and cannot be enabled. The catalogue is not a claim that arbitrary hard-coded Minecraft behavior can be rewritten. Native targeting, activation prerequisites and damage classifications remain.

The selected expanded board is loaded on demand; the server sends the catalogue as metadata instead of transmitting every equation graph. Client refreshes preserve drafts. Protocol version 3 carries bounded equation maps and the existing validated resource shares.

## Payments and delayed effects

Skill dispatch, raw native Magic payments, EnergyHelper transactions and consuming helpers use the final native bill, preserving native cost modifiers and automatic-cast discounts. Expanded consuming payments are whole transactions, including reserves; an unfunded bill consumes nothing. Existing tailored magicule-only Healing retains its native proportional behavior. Automatic-cast preparation checks the selected resource pools.

Coordinate confirmations use the native spatial interface’s actual mode index, rather than assuming Blink and Warp share a mode. Instant Transmission and native Spatial Motion have actual-distance payment checks. Opening a coordinate menu is free; the selected equation is rebased to the destination bill on confirmation. A native advertised cost without an actual transaction is not turned into an invented fee.

Spawned nonliving entities and actual status-effect instances retain a frozen model, equations, payment policy and caster identity. Tick scopes restore those paid-for parameters even after Universal Formula stops or the live profile changes. Effect copy, update/promotion and save/load retain snapshots, including hidden weaker effects. Counter and Fusion capture their authored output factors when starting. No live player/entity reference is retained in the weak snapshot cache.

## Verification

The final verification totals and release checksum are appended below. Native cases include the original Laplace/payment/Metis regressions plus expanded Fire Ball charged damage and payment, no shared-config mutation, registry-wide board evaluation, actual Aura Slash aura billing, Prospector mining events, Agility duration, zero-MP aura-funded charged casts and automatic preparation, status-effect copy/save/load including hidden effects, lazy selected-board loading, native attribute multiplier restoration, consuming reserves and atomic failure, frozen upkeep after Universal Formula stops, and coordinate payments for Instant Transmission and native Spatial Motion.

Actual native-client interactions cover synthesis and the existing board/payment workflows, then select a lazily loaded Fire Ball board, drag a charged-damage multiplier, select aura, apply and verify server persistence, and open Prospector’s passive board. The review also exercises compact layout bounds. Captures: [expanded Fire Ball](../../../wiki/media/laplace-expanded-live.png) and [Prospector passive](../../../wiki/media/laplace-expanded-prospector.png). Review code uses a disposable save and is excluded from the release.

Native commands, run sequentially:

```sh
./gradlew runGameTestServer -I docs/review-high-realm/server.init.gradle -PlaplaceFocus -PlaplaceRegression -PmentorFreshReview=laplace-baseline
./gradlew runGameTestServer -I docs/review-high-realm/server.init.gradle -I docs/review-high-realm/api-runtime.init.gradle -PneoOtherworldDirectory=/tmp/realm-api-profile -PlaplaceFocus -PlaplaceRegression -PmentorFreshReview=laplace-api
./gradlew clean test build
python3 scripts/verify-distribution.py
```

The full registry audit inspects numerical metadata and evaluates default boards; representative casts and integration cases test actual behavior. These checks do not replace a full modpack playthrough. F8 changes are limited to the active Laplace entry and its matching generator literal; discontinued Myths gameplay and F8 data remain unchanged.

## Final results

- **126 JVM tests passed**, with zero failures, errors or skips, in a clean production `test build`.
- **79 native baseline tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209.
- **79 native API-profile tests passed** on Tensura 2.0.1.4 / NeoForge 21.1.248. Both servers saved and shut down normally.
- The final registry audit evaluates **642 boards across 374 eligible skills**, with **1,180 exposed numerical fields** counted across modes. The boards comprise 605 native Tensura entries and 37 Additions entries. Fixed actions and cost-only entries are included in the catalogue count; this does not mean every game mechanic is editable. Both API inventories agree.
- The actual client completed board selection, drag editing, resource choice, Apply & Enable persistence and authored passive-board checks, alongside the existing synthesis, payment and compact-layout checks.

Release SHA-256: `b7d692419e32ff547c1d6fb50784f25a71b8d4e7c088e725f55fc6faad1404d7`.

Distribution validation passed for the 1.30.0 metadata, registered mixins/plugin, expanded runtime classes, source-matching resources and review-fixture exclusions. Wiki verification passed for **212 articles, 142 ability records and 213 GitHub Wiki exports**, with the offline ZIP rebuilt. The active Laplace guide matches its generator literal. A raw release-JAR privacy scan passed, including compiled classes. The public tree is scanned before committing; commits retain the generic contributor identity.
