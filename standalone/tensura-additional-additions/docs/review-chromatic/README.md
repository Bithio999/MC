# Chromatic Orb client review

This optional fixture resets the character and uses the native anvil in the existing disposable **Wayfarer Review** save under `/workspace/validation-wayfarer/client/saves`. It refuses another save-directory name. Never run it on a personal save.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null ./gradlew clean runClient -I docs/review-chromatic/client.init.gradle
```

It checks client/server preview state and the actual infusion tooltip, captures the native anvil, sends ordinary C2S inventory clicks to take the result, verifies exactly one orb and one XP level are spent, places the equipment back and checks that another infusion is rejected. Screenshots and pass/fail markers are written to `/workspace/validation-chromatic/capture`.

Headless tests are in `docs/review-high-realm/ChromaticGameTests.java` and run with the High Realm server profile. That profile alone includes protected-enchantment fixtures and a tag overlay. Clean before production packaging; the distribution audit rejects review classes, the protected-enchantment fixtures and their overlay.
