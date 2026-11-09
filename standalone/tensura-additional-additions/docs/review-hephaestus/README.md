# Hephaestus review

Review fixtures are opt-in and must not enter the production JAR. Use Java 21 and Gradle 8.14.3 from the standalone project directory.

```sh
gradle clean test runGameTestServer -I docs/review-high-realm/server.init.gradle -PforgeFocus=true --no-daemon
```

The eighteen Hephaestus cases cover native Ultimate classification and all three modes; synthesis without an EP gate; mastery, awakening, achievement and affordability failures; native awakening-before-cost ordering; both inherited containers and presets; genuine crafting; item stats and non-stacking; fractional ordinary wear and native EP durability; material consumption and preservation of engraving/Orb history; one saved masterpiece; real weapon escrow and duplicate-safe recall; session rejection; an autonomous Nullification spear hit; Keen, Conductor, Piercing and Enduring; a deployed Vesper’s delayed Afterwind; and scoped offensive engraving attributes restored after a hit.

Omit `-PforgeFocus=true` for the full native suite. Repeated tests accumulate generated terrain in `run/world`; check available space first. Only clear that disposable world after stopping the test server. Never clear a user's client save.

The real-client harness is guarded to the disposable `Wayfarer Review` save:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null gradle clean runClient -I docs/review-hephaestus/client.init.gradle --no-daemon
```

It changes the review character, nearby mobs and a small review courtyard. It exercises the native Ultimate entry and activation event, persistent two-click synthesis confirmation across refreshes, the exact permanent-magicule bill, real menu actions, both GUI scales, a masterpiece, six committed weapons, visible client lunges, actual damage and six-item recall. Require a **fresh** `/workspace/validation-wayfarer/capture/passed-hephaestus.txt`; successful Gradle exit alone is insufficient. Earlier failed markers are historical and do not establish the current result. Inspect the corresponding PNGs.

The API profile is `gradle clean test -PneoOtherworldDirectory=/tmp/realm-api-profile --no-daemon`. Do not run Gradle profiles concurrently. After any review/API profile, run a **clean baseline production build** and `python3 scripts/verify-distribution.py` before copying a release JAR.
