# 0.34.3 validation — Martial Master movement and baseline MP recovery

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

**All 280 required native GameTests passed.** The standalone runtime also passed all **47 required native GameTests** with Tensura Myths absent.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

The [native implementation reference](research/CHAINED_DASH_0.34.3.md) explains the old downward-aim failure and the inspected native targeting/regeneration code.

Gameplay checks compare real native Martial Master casts with Chained at equal aim reach for level and downward aim. They cover ground/water activation, fluid-ignoring targeting, block-snapped landing, blocked body clearance, 2% current-aura payment, path/arrival hits and unchanged damage coefficients. Cooldown checks verify a 1-second timer survives passive refresh, rejects an immediate repeat without movement/payment and permits a ready cast.

Regeneration checks retain 1× MP recovery, redirect only bonuses above 1× into aura, preserve lower-rate penalties, work with full magicules and leave native sleep, overflow and non-Chained recovery intact. All previous race, mastery, true-passive Broken Limit, acquisition, aura-recovery, flight, boss and combat regressions remain covered.

Both current guides and the wiki describe the native-style movement and baseline regeneration split. The offline edition and prepared GitHub Wiki mirror are regenerated. The standalone's opt-in client review checks the real player's native comparison, replicated landing and cooldown/recast behavior; it remains excluded from release JARs.

Releases: `releases/tensura-myths-0.34.3.jar`, `releases/tensura-myths-wiki-0.34.3.zip` and matching checksum file; standalone `releases/tensura-heavenly-restriction-1.0.2.jar` and checksum file. Use the appropriate addon on client and server, with only one addon version installed.

Real-client review passed under software OpenGL: at an 8-degree downward aim, both native Martial Master and Chained landed at `(12.5, 71, 0.5)` from `(0.5, 71, 0.5)`. Chained paid exactly 200 of 10,000 aura, retained cooldown through refresh, rejected an immediate repeat, synchronized the landing to the client and successfully recast after native expiry. Dash and recovery screenshots were captured without standalone renderer failures.

Final package verification: 11 JUnit tests passed; both normal release builds succeeded and report the correct versions. The offline wiki validates 162 HTML pages, 86 ability anchors and 9,638 local references; all 789 archive entries match their source files. The standalone package audit confirms the new movement class and excludes optional review fixtures and bundled dependency JARs.
