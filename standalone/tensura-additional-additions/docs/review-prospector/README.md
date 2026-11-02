# Real-client Prospector review

Use a disposable copied save named **Prospector Review** under `/workspace/validation-prospector/client/saves`. The fixture verifies that exact name before changing the world. It clears an arena near the origin at Y=159, replaces nearby actors/blocks, resets the review player's skills and freezes daylight/weather. Do not use a personal save.

With Java 21 and an X11 display:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient -I docs/review-prospector/client.init.gradle
```

The real connected player receives Prospector in native skill storage. The fixture finds it in Tensura's actual **Skills → Unique** screen and clicks the native toggle checkbox, including its packet. It checks all eleven vanilla/Tensura ore families behind a solid stone wall, magic-vein grouping, the 32-block range and the per-tick section/cell limits. A pixel check verifies that colored glyphs actually reach the framebuffer through the grayscale wall in both Fancy and Fabulous rendering; detecting ore in memory alone is insufficient. Toggle-off must clear the survey.

With the passive off, it presses the real skill's Refine handler and checks the native `RefiningScreen`/`RefiningMenu` protocol. The native client brew button sends the normal packet. A glass bottle in base slot 0 and hipokute grass in reagent slot 3 must become a high potion through the existing recipe and normal skill ticks. Closing/reopening must preserve exactly one result.

Screenshots and a pass/failure marker go to `/workspace/validation-prospector/capture`. Captures are actual gameplay with a prepared test arena; hidden ores have not been made visible by removing the wall. The fixture is opt-in and must not appear in the shipped JAR. This is a baseline client review, not a full modpack test.
