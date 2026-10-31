# Heavenly Restriction 1.0.2 validation

**All 47 required native GameTests passed**, with Tensura Myths absent from the independent runtime.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Chained's dash now uses Martial Master's eye ray, fluid-ignoring targeting, adjacent landing cell and block-centering fallback. It retains Chained's 16-block aim reach, 2% current-aura cost, 1.5√A / 3√A damage and 3/4-block hit radii. Ground/water activation matches the reference; endpoint body/chunk/border/height guards reject unsafe landings. The old swept-body path no longer stops a downward aim at the player's feet.

Successful casts have a **1-second cooldown** that passive refresh no longer clears. Tests verify native/reference landing agreement at multiple pitch angles, fluid traversal, airborne rejection, blocked landings, payment/hits, blocked immediate repeats and ready recasts.

Chained retains **1× native magicule regeneration**, converting only the rate above 1× into aura. Tests cover 3×, 1× and 0.5× rates, full magicules, native sleep recovery, overflow decay and an unrelated owner's unchanged recovery. Maximum magicules, EP growth and Broken Limit's mastered true-passive behavior retain their existing rules.

The native suite retains all previous independent race, inheritance, recovery, damage, dodge and flight coverage. The current guide/install notes are updated. The optional review in `docs/review` exercises a real client player's landing synchronization and cooldown/recast behavior and is excluded from the normal release build.

Release: `releases/tensura-heavenly-restriction-1.0.2.jar` and `.sha256` in the repository root. Install on both client and server instead of Tensura Myths.

Real-client review passed under software OpenGL: at an 8-degree downward aim, both native Martial Master and Chained landed at `(12.5, 71, 0.5)` from `(0.5, 71, 0.5)`. Chained paid exactly 200 of 10,000 aura, retained cooldown through refresh, rejected an immediate repeat, synchronized the landing to the client and successfully recast after native expiry. Dash and recovery screenshots were captured without standalone renderer failures.
