# Native Instant Transmission client review

This opt-in fixture is for a **disposable world only**. It clears the review player’s skills, grants the predecessor, validates the learning evolution, disables that actor’s natural regeneration, and submits two requests through the native Spatial Movement screen. It is excluded from normal builds.

Prepare the exact pack using the [compatibility instructions](../compatibility/NEO-OTHERWORLD-1.1.3a.md). Copy a disposable flat world into `<pack-directory>/client/saves/Transmission Validation`. Never point the fixture at a world you intend to keep; the folder-name check is only a guard against selecting the wrong test world.

Run with Java 21 and a graphical display:

```sh
gradle runClient -PneoOtherworldDirectory=/path/to/disposable-pack -PconnectorCleanPath=/path/to/clean-mapped-minecraft.jar -I docs/review-transmission/client.init.gradle
```

The fixture opens the real native coordinate screen, fills its ordinary coordinate fields and invokes the same action used by its Warp button. It verifies an ordinary 20-block trip, then a mastered 20-block trip. Success writes `client/transmission-review/passed.txt` and the screen captures before stopping the client. The reviewed run used all 263 pack projects and the original overrides; writable client cache files remain inside the test workspace.
