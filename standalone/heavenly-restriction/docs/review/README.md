# Optional client review

This fixture is excluded from normal source sets and release JARs. It automatically compares a real player's downward dash against native Martial Master, verifies client/server landing synchronization, aura payment and the 1-second cooldown/recast, then renders the copied dash and GeckoLib recovery bloom, saves two screenshots to `docs/media`, and exits.

Run it only against the disposable **Restriction Validation** development world. After the native test server has stopped, copy its isolated `run/world` to `run/saves/Restriction Validation` once, then run:

```sh
./gradlew runClient -I docs/review/client.init.gradle
```

A working graphical display/OpenGL context is required. The fixture builds a small platform, replaces the test player's race, skills/resources, and camera position, and spawns temporary visual entities. It does not run against or alter any external world. Regular `./gradlew build` excludes this fixture; rebuild normally after using it before packaging a release.
