# High Realm runtime review

Requires Java 21 and the supported baseline runtime dependencies.

```sh
./gradlew test runGameTestServer -I docs/review-high-realm/server.init.gradle
```

Minecraft's GameTestServer bakes a hardcoded flat preset against an empty dimension-stem registry. The opt-in fixture extends **only that test preset** to include the real High Realm generator. This runs all 216 existing scenarios plus the realm and loadout scenarios. It tests biome registration/spawn pools, the multiblock in both orientations and obstruction cases, actual attribute EP and foreign travel events, saved gates/individual return points, safe exits, native elite attributes/skills, reload without healing or growth, despawning, boss/owned/summoned/foreign-mob exclusions, natural spawn marking, sanctum spawn/explosion protection, native kill-EP rewards, arrows and elite drops. The loadout suite additionally checks all ten Tensura species, mastered/enabled passives, equipped Fighter, migration without refills/replay, valid surface spawning, stronger-player alerts and actual autonomous Greater/Arch Daemon magic projectiles/casting circles, highest non-boss native evolutions, evolution NBT persistence and replay/ownership/resource safety.

For the client flow, copy a disposable existing save to `/workspace/validation-high-realm/client/saves/High Realm Review`, then run:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew clean runClient -I docs/review-high-realm/client.init.gradle
```

The fixture refuses other save-directory names. It resets the review character and modifies its test gate area; never use a personal save. It checks that the dimension is automatically added to an existing save, uses a real C2S block-interaction packet to consume one Nether Star, walks through the charged gate, checks inventory/skill preservation, returns with insufficient EP, reuses the gate without another star, and walks through the return arch with no EP. It captures actual game terrain, both arches and the searchable Dimensions guide at two GUI scales. Clouds are disabled for unobstructed terrain captures. Markers and PNGs are written to `/workspace/validation-high-realm/capture`.

The client fixture intentionally does **not** load the test-only flat preset. Clean between profiles so review classes and the preset override cannot reach a release. `scripts/verify-distribution.py` checks their exclusion.
