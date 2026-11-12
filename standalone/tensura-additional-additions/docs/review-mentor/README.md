# Archive and mentor review

The opt-in native fixtures live in `docs/review-high-realm/MentorGameTests.java`. Run with Java 21:

```sh
gradle test runGameTestServer -I docs/review-high-realm/server.init.gradle -PmentorFocus=true --no-daemon
```

Omit `-PmentorFocus=true` for the complete regression suite. Add `-PmentorFreshReview=final` to use isolated native-baseline-final/native-api-final test directories; wait for one profile to exit completely before starting the next. The eight cases cover proportional EP billing and Orb consumption, one-time enforcement, temporary/equipment capacity, creative mode, native acquisition vetoes, Chained and tier restrictions, persistence and traveling-character import, ownership/session/proximity/replay checks, and rotated template placement. The archive test also invokes native `Structure.generate` and requires a non-empty jigsaw start; template-only placement cannot establish that natural generation works.

`client.init.gradle` and `MentorReview.java` are an opt-in actual-client harness. They modify only a disposable save whose folder contains **Archive Review**, and refuse other saves. Provision a fresh normal Overworld review save at the configured path before running. A previously redeemed character is expected to be unable to repeat the purchase: use a new disposable character/save for another purchase review. Do not alter a real save or clear its redemption ledger.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null gradle runClient -I docs/review-mentor/client.init.gradle --no-daemon
```

Require a fresh `/workspace/validation-mentor/passed.txt`. Inspect the menu/confirmation captures; Gradle success is not sufficient. The harness locates and generates a natural archive with its NPC, opens the server-bound menu, searches/selects Prospector, cancels one confirmation, checks normal and maximum-scale compact control bounds, then completes a real purchase and verifies permanent EP, Orb and redemption state. Inspect scenery captures separately because a terrain-loading screen is not a usable exterior image.

Do not run Gradle profiles concurrently. After review profiles, run a clean baseline production build and `python3 scripts/verify-distribution.py`; review code and namespace resources must not be packaged.
