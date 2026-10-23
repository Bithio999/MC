# Opt-in native visual preview

This source is excluded from the normal mod source set and release JAR. It adds operator-only `/vfx <kind>` and `/vfxclear` commands for native renderer inspection. It stages cosmetic entities only; it does not demonstrate damage or normal skill acquisition.

Create a separate creative world with cheats named **Myths Validation**, then run Java 21 / Gradle 8.14.3 with:

```sh
gradle -I docs/research/fixtures/visuals-0.29.0/preview.init.gradle runClient
```

Use only that isolated development world. The fixture uses the point `(0,70,8)` and follows the viewer for recipient-bound effects. Use `/tick freeze`, `/vfx 25`, then `/tick step 20` for Lexicon, or choose the active IDs from `art/blockbench/manifest.json`. ID 61 is retired. Clear old visuals between previews. Camera angle and third-person view help inspect different structures. The fixture supplies seed/lease data for owned markers, star warnings and the singularity barrage; it does not run boss AI or real combat.

Close the client and run `gradle clean build` **without the init script** before distributing a JAR. Confirm no `VisualPreview.class` exists in the archive. The model/texture validator can be run independently with `python scripts/verify-visual-assets.py`.
