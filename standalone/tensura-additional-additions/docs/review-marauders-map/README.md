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
