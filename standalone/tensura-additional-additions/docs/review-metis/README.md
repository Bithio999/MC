# Metis review

Use Java 21 / Gradle 8.14.3. The 17 native cases are opt-in review fixtures, excluded from production builds.

```sh
gradle clean test runGameTestServer -I docs/review-high-realm/server.init.gradle -PmetisFocus=true --no-daemon
```

Omit `-PmetisFocus=true` for the full native regression suite. For the second supported API:

```sh
gradle clean test runGameTestServer -PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/server.init.gradle -I docs/review-high-realm/api-runtime.init.gradle -PmetisFocus=true --no-daemon
```

The real-client harness modifies only the disposable **Wayfarer Review** save and writes captures under `/workspace/validation-wayfarer/capture`:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null gradle clean runClient -I docs/review-metis/client.init.gradle --no-daemon
```

Require a fresh `passed-metis.txt` and inspect its screenshots; Gradle success alone is insufficient. The review exercises native synthesis confirmation, a real assignment packet, mode selection, mastery/four slots, the actual native Ultimate menu, and normal/320×180 GUI bounds. Do not run profiles concurrently. Finish with a clean baseline production build and `python3 scripts/verify-distribution.py` before copying a release JAR.

Combat cases verify automatic/manual arm swings, independent spell windows and native higher-hit differences, aging, unmodified manual timers, defensive invulnerability, delayed/saved projectile provenance, copied sources, paired-hit cadence and the actual native Wind Cutter / Fire / Water Cutter repeat-damage comparison.
