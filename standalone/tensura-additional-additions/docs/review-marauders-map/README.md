# Marauder’s Map runtime review

Run the native suite with Java 21 and the normal baseline runtime dependencies:

```sh
./gradlew clean test runGameTestServer -I docs/review-high-realm/server.init.gradle
```

The new scenarios check native registration/reincarnation eligibility and learned/sealed access, private pin CRUD/validation/limits and NBT persistence, verified native structure-start bounds, world-seed isolation, incremental scan limits, cancellation and the native asynchronous structure-start invoker. JVM tests check cursor-anchored zoom, drag conversion, negative coordinates, zoom limits and nonfinite/border input.

For the integrated client, copy a **disposable normal-generation save** into `/workspace/validation-marauders-map/client/saves/Marauders Map Review`, with the normal baseline runtime dependencies. The opt-in fixture refuses other directory names. It resets the review character and its pins, locates a real village, and moves the review player there. Never use a personal save.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null ./gradlew clean runClient -I docs/review-marauders-map/client.init.gradle
```

The fixture selects the skill in Tensura’s native Unique menu and activates it through the native skill lifecycle. It exercises real map panning, zoom, recentering, help/legend, private pin create/edit/invalid input/confirmation/delete and reopen, real structure search including Tensura types and a native village marker, coordinate navigation without teleporting, terrain PNG persistence, and fitted map/pin controls at GUI scales 2 and 4. Screenshots and a pass/fail marker are written to `/workspace/validation-marauders-map/capture`.

Clean between profiles. The client fixture and the High Realm test preset must not enter a release; the distribution audit checks their exclusion.

The 1.13 review additionally checks real native terrain previews and 4×4 top-face surface detail at previously unvisited coordinates over 100,000 blocks away. It verifies no client terrain loading or character teleportation, native networked locations for a distant review player and a review player in the Nether, live position updates after moving beyond entity tracking distance, and safe selection of players in other dimensions. The review roster uses disposable FakePlayers with embedded network channels; these simulate server-side online players, not additional connected human clients. A separate `passed-world-map.txt` marker is required for this extended review.

The 1.14 extension additionally performs an actual cold stronghold-ring search from 70,000 / −70,000 and asserts that the result is beyond the old 2,048-block range and inside 100,000 blocks. It exercises the owned-skill chooser, prefills the real Spatial Motion and Instant Transmission screens, confirms that merely opening these menus does not teleport, then activates the native Warp button with Enter and verifies the actual aura-based teleport to the suggested surface arrival. Require `passed-travel.txt` as well as the two earlier pass markers.

For a targeted travel rerun after the full original review has passed, add `-PmapTravelOnly=true` to the client command. This starts at the long-range search and native-menu handoff cases in the same guarded disposable world.

The 1.15 automatic-discovery review runs with `-PmapSurveyOnly=true`. It opens a wide unexplored viewport at 300,000 / −310,000, waits for automatic vanilla/addon markers without Locate, pans to another area and changes zoom, checks that distant region files were not created and full terrain chunks were not loaded, then explicitly generates one native structure start to compare its exact type and bounding box. Require `passed-auto-structures.txt`; screenshots use the `marauders-map-auto-*` prefix.
