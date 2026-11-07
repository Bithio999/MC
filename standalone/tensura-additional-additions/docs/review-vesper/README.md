# Vesper runtime review

The comparison item is `tensura_neb:night_rose` (Valentine’s Night Rose), from **TenSura: Not Enough Bosses 2.0.0.3**, CurseForge file **8022509**. It is an optional test reference, not a required or bundled dependency.

A native game test reads both actual items at runtime. Night Rose supplies **90 damage** including the player’s base 1, **1.7 attack speed**, **+1 reach**, and **3,600 durability**. Its constructor requests 9,000 durability, but the Hihiirokane tier overwrites that request with 3,600; compare actual stacks rather than constructor literals. Vesper has **105 damage**, the same speed/reach and **3,960 durability**. Night Rose’s native Energy Steal II is replaced by Vesper’s original Afterwind I engraving.

From this addon directory, with Java 21 and Gradle configured:

```sh
gradle clean test runGameTestServer \
  -I docs/review-high-realm/server.init.gradle \
  -I docs/review-vesper/reference.init.gradle \
  -PvesperReferenceJar=/path/to/tensura-not-enough-bosses-8022509.jar
```

Omit both `reference.init.gradle` and its property to run without the reference addon. All functional tests still run; only the live Night Rose comparison is skipped. The Vesper tests cover real HP/SHP damage, immune hits, expiry, cooldown, weapon switching, nonrecursive delayed hits, raw attack scaling, over-cap engraving preservation, boss loot and migration of a saved Kael without resource restoration.

Add `-PvesperFocus=true` to run only the eleven Vesper tests. The full profile includes both the existing addon tests and the dedicated `vesperreview` template namespace. The three native training-dummy regressions cover constant-HP damage, immune/stale hits and counter resets, using the real dummy entity on its required block. The loot regression invokes the complete native `dropAllDeathLoot` transaction, including the loot table and equipment, after the death-prevention boundary; it checks exactly one sword and preserved level-II engraving/wear.

The guarded real-client review uses a disposable `Wayfarer Review` save. It verifies Kael’s equipped sword, his native Wind Cutter → sword combo, one delayed Afterwind hit, and native item rendering for both Afterwind I (30%) and Orb-enhanced Afterwind II (75%):

```sh
gradle clean runClient -PvesperOnly=true -I docs/review-wayfarer/client.init.gradle
```

A fresh `passed-vesper.txt` marker is required: a successful Gradle exit alone does not mean the fixture passed. Always clean before building production; these review profiles add fixture classes/resources that must never ship.
