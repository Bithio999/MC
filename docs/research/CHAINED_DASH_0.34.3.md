# Chained movement and regeneration reference — 0.34.3 / standalone 1.0.2

Reference binary: Tensura: Reincarnated **2.0.1.3**, CurseForge file **8793360**, the pinned development/runtime dependency. Inspected `MartialMasterSkill`, `ObjectSelectionHelper` and `ExistenceStorage`; no reference-mod source or binaries are bundled in the addon.

## Native dash

`MartialMasterSkill.onPressed` requires ground or water/bubble contact. It uses `ObjectSelectionHelper.getPlayerPOVHitResult` from the eyes with `ClipContext.Fluid.NONE`, selects the hit cell relative to its face, and calls `getFloorPos` to center the landing on a block. If the selected cell cannot be replaced, it selects one cell above. It checks the native skill-not-teleportable tag and world border, then dismounts, resets fall distance, teleports, swings the main hand and plays `INSTANT_MOVE`.

`getFloorPos` centers X/Z at block +0.5 and uses the block's Y coordinate; it does not search down to a distant floor. Native default reach is 15 blocks, or 20 mastered. Chained retains its existing **16-block aim reach**, damage formula, aura cost and hit radii while using the same targeting/landing algorithm. Block snapping means measured displacement is not an exact 16-block vector.

The previous Chained implementation advanced a full body bounding box every quarter-block along a feet-origin look vector. On flat ground, a slightly downward aim pushed that box into the floor immediately. The new implementation checks the destination body instead. It retains loaded-chunk, body-clearance, build-height and world-border guards to reject unsafe landings before payment.

Severance Rush now sets a native **1-second cooldown**. The old passive-refresh line that reset any cooldown to zero is removed. The cast checks cooldown before targeting/payment, and only a successful move starts it.

Native regressions compare actual Martial Master and Chained casts at equal reach for level and downward aim, restore the reference configuration after each comparison, check fluids/ground activation, validate path/arrival damage and ensure failed/blocked casts do not pay. The real-client fixture additionally checks the replicated landing and recast behavior.

## Regeneration split

Native `ExistenceStorage.handleMagiculeRegen` uses:

`area magicules × configured area recovery × magicule regeneration multiplier`

It also handles its own sleep bonus, spiritual maintenance, overflow decay and poison handling. Chained now redirects only the multiplier's portion **above 1** into current aura. The native magicule calculation receives **min(original rate, 1)**. For example, a 3× rate supplies 1× native MP recovery and redirects the remaining 2× worth of MP recovery into aura. A rate below 1 stays below 1; the pact does not override penalties.

Conversion still works when magicules are full and is limited by maximum aura. Native sleep MP recovery stays with magicules; native aura sleep recovery retains its original behavior. Spiritual loss and over-capacity handling remain native. This restores current resources only and does not raise maximum magicule capacity or change EP growth.

The implementation modifies the native rate expression rather than cancelling the complete native regeneration method. Tests verify 3×, 1× and 0.5× rates, full-MP conversion, sleep, overflow and an unrelated owner's unmodified recovery.
