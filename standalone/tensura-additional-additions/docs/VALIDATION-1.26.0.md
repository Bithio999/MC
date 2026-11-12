# Additions 1.26.0 — Metis skill assignment

Parallel Matrix accepts owned, learned active skills and arts alongside aspectual/spiritual magic. Capacity remains three threads, four at mastery, with one thread per ability. Existing matrix IDs and saved assignments are retained. The catalogue has All / Aspectual / Spiritual / Skills / Arts filters, search and learned-mode selection.

## Menu exclusions

Tensura has no general per-mode menu flag. `MetisMenuInspection` reads callback bytecode without activating it, follows mode-specific branches, helpers and lambda bodies, and rejects detected menu/screen opening paths. Inspection is bounded and cached; unreadable or uninspectable callbacks are omitted. `MetisAbilities` applies the same learned-only policy to catalogue rows, dropdown modes, assignment requests, loaded matrix entries and running casts. Automatic activation uses the main action with shift temporarily cleared and restored. This allows a main combat action with a manual shift shortcut to remain usable without opening its shortcut menu.

Confirmed native examples: Great Sage's crafting/refining mode and Prospector's refining are excluded; Spatial Motion's Blink is allowed while Warp's coordinate menu is excluded. Passive-only skills, Metis/Split Mind controllers, Parallel World Jumper and opaque copied-technique containers are not assignable. Native container opening has an additional background-only guard for both ServerPlayer overloads. Ordinary manual containers remain functional.

## Activation and costs

Automation uses the actual owned instance and native press/hold/release lifecycle. Input, aim and the internal virtual slot are scoped to that callback. Native activation, interference, ownership, mode learning, resource and cooldown checks remain. Manual use of an ability takes priority over its automatic thread. Automatic skills do not use a manual cooldown override to cancel an active counter stance.

The existing 30%/40% magicule reduction applies only to automatic aspectual/spiritual magic. Skills and arts pay their native costs and keep their native damage/hurt-window cadence; the existing independent magic-impact handling stays limited to magic. Known healing/support modes stay self-directed; offensive modes target hostile mobs or combat opponents under the existing range, alliance, PvP and visibility rules. Resistance-based Optimal Solution scoring stays limited to magic with known native damage types; other skill damage types are not guessed or rewritten.

## Verification

**26 native integration cases passed** on both **Tensura 2.0.1.3 / NeoForge 21.1.209** and **Tensura 2.0.1.4 / NeoForge 21.1.248**. Both servers shut down normally and Gradle exited successfully. This covers targeted runtime compatibility, not a full modpack playthrough.

New cases cover owned skill assignment, rejecting menu/passive modes, preserving non-menu Great Sage actions, correct native skill costs and cooldowns, the separate magic discount, Blink versus Warp, forged requests with a valid owner session, invalid saved modes, self-healing with crouch-state restoration, and keeping an automatic counter active during its cooldown. Existing parallel-magic, analysis, synthesis, ownership/session, targeting and native hit-window regressions remain in the suite. Seven JVM cases cover per-mode switch paths, helper/alias traversal, lambda captures, learned/main-activation branches, uncertain menu paths, exception handlers and GUI/inventory aliases.

```sh
./gradlew test runGameTestServer -I docs/review-high-realm/server.init.gradle -PmetisFocus=true -PmentorFreshReview=metis-abilities
```

For the second profile, add `-PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/api-runtime.init.gradle`. Run profiles sequentially.

The actual-client review passed synthesis, real magic and skill assignment packets, mixed four-thread storage, category filtering, the Blink-only dropdown, normal/compact layout bounds, both background container guards and ordinary manual menu opening. Captures were inspected. See [review instructions](review-metis/README.md).

The final clean production build passed **76 JVM tests**, with no failures, errors or skips. Distribution audit verified 1.26.0 metadata, the new ability-policy/inspection classes and menu guard, registered mixins, packaged assets and guide entries, and absence of review fixtures or bundled dependencies. ASM is supplied by NeoForge; the standalone JVM tests add only a test dependency. The unchanged archive context audit also passed. Release SHA-256: `9878d607d64ffabbf1475b1493c5826ec20a894bb5a567796c41ae7a0b1c2017`.
