# Schema client review

This opt-in fixture is for a **disposable world only**. It clears the review player’s skills, completes native Schema learning, saves a six-stack build through the actual screen/network path, validates its live regeneration modifier and damage factor, previews full mastery, verifies draft cancellation and reopening, and turns the passive off. It is excluded from normal builds.

Prepare the exact pack with the [compatibility workflow](../compatibility/NEO-OTHERWORLD-1.1.3a.md). Copy a disposable flat world into `<pack-directory>/client/saves/Schema Validation`. Never select a world you intend to keep; the folder-name guard is only a check against selecting the wrong validation world.

With Java 21 and a graphical display:

```sh
gradle runClient -PneoOtherworldDirectory=/path/to/disposable-pack -PconnectorCleanPath=/path/to/clean-mapped-minecraft.jar -I docs/review-schema/client.init.gradle
```

Success writes `client/schema-review/passed.txt`, captures the initial/mastered screen and stops the client. The reviewed run retains all 263 pack projects and original overrides. The master icon can be exported independently using `python3 scripts/prepare-schema-art.py` from the repository root (ImageMagick required).
