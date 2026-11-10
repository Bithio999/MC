# Hephaestus review

Review fixtures are opt-in and must not enter the production JAR. Use Java 21 and Gradle 8.14.3 from the standalone project directory.

```sh
gradle clean test runGameTestServer -I docs/review-high-realm/server.init.gradle -PforgeFocus=true --no-daemon
```

The forty Hephaestus cases cover native Ultimate classification and all three modes; synthesis without an EP gate; mastery, awakening, achievement and affordability failures; native awakening-before-cost ordering; both inherited containers and presets; genuine crafting; item stats and non-stacking; fractional ordinary wear and native EP durability; material consumption and preservation of engraving/Orb history; one saved masterpiece; real weapon escrow and duplicate-safe recall; session rejection; an autonomous Nullification spear hit; Keen, Conductor, Piercing and Enduring; a deployed Vesper’s delayed Afterwind; and scoped offensive engraving attributes restored after a hit. Additional cases exercise real Tensura Smithing Bench normal/shift takes, Hihiirokane synthesis credit and attack bonuses, native equipped armor/toughness, crafting-table quick transfer, vanilla smithing and incremental deployment / individual recall. Armor cases cover proper slot weights and caps, armor-only criticals, additive penetration, HP/SHP elemental protection, physical exclusions and ordinary/EP wear. Equipment-growth cases compare held and all six deployed weapons on real kills, lethal-hit writeback, Growth engraving rewards, skipped rewards, caps, native initialization and item evolution with preserved forge/Chromatic identity. Engraving cases test research entry filtering and donor preservation, direct API and legacy knowledge, Lord overcap storage, native preview/final application, repeated copy/Orb attempts, existing infused equipment, normal-tier application and native blacklists. Two JVM timing cases cover every arsenal size, reversed entity tick order, idle gaps and sparse roster changes.

Omit `-PforgeFocus=true` for the full native suite. Repeated tests accumulate generated terrain in `run/world`; check available space first. Only clear that disposable world after stopping the test server. Never clear a user's client save.

The real-client harness is guarded to the disposable `Wayfarer Review` save:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null gradle clean runClient -I docs/review-hephaestus/client.init.gradle --no-daemon
```

It changes the review character, nearby mobs and a small review courtyard. It exercises the native Ultimate entry and activation event, explicit synthesis and masterpiece dialogs that survive refreshes, the exact permanent-magicule bill, real menu actions, normal GUI scales and a forced 320×180 viewport, a masterpiece, six committed weapons, individual recall and incremental redeployment through the menu, visible client lunges, actual damage and six-item recall. The UI review also tempers a chestplate, replaces its temper and captures the piece-specific preview/tooltip. The UI review also verifies action/material selectors, Utilities, pagination, separate rack controls, widget bounds and overlapping interactive regions. Require a **fresh** `/workspace/validation-wayfarer/capture/passed-hephaestus.txt`; successful Gradle exit alone is insufficient. Earlier failed markers are historical and do not establish the current result. Inspect the corresponding PNGs.

The native API-profile check also exercises Tensura 2.0.1.4 / NeoForge 21.1.248 (the dependency runtimes are included only for this review):

```sh
gradle clean test runGameTestServer -PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/server.init.gradle -I docs/review-high-realm/api-runtime.init.gradle -PforgeFocus=true --no-daemon
```
 Do not run Gradle profiles concurrently. After any review/API profile, run a **clean baseline production build** and `python3 scripts/verify-distribution.py` before copying a release JAR.
