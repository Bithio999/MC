# Critical Merchant review

This opt-in harness tests the native critical pipeline and captures the real Tensura skill menu. Review classes and test structures must not ship in the production JAR.

From this standalone project, use Java 21 and Gradle 8.14.3:

```sh
gradle clean test runGameTestServer -I docs/review-high-realm/server.init.gradle -PmerchantFocus=true --no-daemon
```

Omit `-PmerchantFocus=true` for the full native regression suite. The thirteen focused cases cover payment, cooldown, refresh, ten accepted hits, blocked-hit refunds, natural criticals, Unbound melee and projectiles, owned projectiles, spiritual damage, native mob criticals, the Fettered reincarnation filter, ordinary hits and damage immediately healed during the transaction.

Repeated native runs accumulate terrain in the disposable `run/world` directory. Check disk space before a full suite. If necessary, stop the test server and remove only that generated test world; never remove a user's client save. A full disk prevents the map-detail persistence regression from completing.

For the client capture, the guarded disposable save must already exist at `/workspace/validation-wayfarer/client/saves/Wayfarer Review`:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null gradle clean runClient -I docs/review-critical-merchant/client.init.gradle --no-daemon
```

The harness changes the disposable character and nearby test mobs. It checks the actual 1,000-magicule payment, native Unique selection, a real player attack on Tensura's training dummy, and synchronized remaining charges. Require a freshly written `passed-critical-merchant.txt` in `/workspace/validation-wayfarer/capture`; a successful Gradle exit alone is insufficient. Inspect `critical-merchant-menu.png` and `critical-merchant-hit.png` there.

The API profile can be checked separately with `gradle clean test -PneoOtherworldDirectory=/tmp/realm-api-profile --no-daemon`. Never run profiles concurrently. Run a clean baseline production build afterward so review fixtures and alternate dependencies cannot enter the release.
