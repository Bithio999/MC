# Heavenly Restriction 1.0.0 validation

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2, Architectury 13.0.8, GeckoLib 4.9.3, SmartBrainLib 1.16.11 and TerraBlender 4.1.0.8.

## Independent build and gameplay

```sh
cd standalone/heavenly-restriction
python3 scripts/fetch-runtime.py
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

The project has its own settings, build, wrapper, runtime dependency lock, source tree and resource namespace. It does not depend on the parent project's classes, Gradle source sets or JAR. The native runtime explicitly verifies that `wildhunt` / Tensura Myths is absent.

**45 required native GameTests pass.** Coverage includes:

- Exactly five registered races, five skills and one non-damaging visual entity in the standalone namespace; no Myths content is registered.
- Fettered Human in native reincarnation selection; Chained excluded from the random Unique pool; four native-menu evolutions, human-like classification, fixed total-EP requirements and stage attribute replacement.
- Starting aura/magicule ranges; unmultiplied starting aura; 5× later capacity gains from native helpers, attributes and equipment; no compounding on reload/toggle; native magicule-ceiling enforcement; 90/10 native EP awards at every lineage stage. Native Human retains its original split.
- Reincarnation and parent-derived skill provenance, Martial Master → Heavenly Eye, rejected unrelated skills, normal Battlewill access, learning/mastery bonuses and aura-paid resistance/nullification acquisition.
- Native buff and damage callbacks, mastered resistance/status degradation, explicit dodge bypass, slotted projectile dodge and operator/void precedence.
- Instant 16-block dash, 2% current-aura payment, current 1.5√A / 3√A coefficients, path and arrival hits inside native hurt cooldown, collision-safe endpoint, blocked-cast handling and consecutive cooldown-free casts.
- Recovery grants only at evolutions two through four; specific exceptions to Chained's skill gate; strongest enabled tier alone pays aura to heal HP/SHP; preserved mastery/toggles on migration; no regrant of Chained by later stages.
- Air Flight mastery unlocks a native learning entry; latent/temporary/borrowed predecessor rules; native flight flags and authorization; aura activation/upkeep; interference, depletion, forgetting, clone and creative/other-native-flight cleanup.
- Independent persistent state, player-clone preservation, no writes to the Myths data root and no changes to foreign addon state.

The full Tensura Myths source and its 0.34.1 release remain intact. Its server tests were not rerun because its implementation is unchanged.

## Client and assets

The optional fixture under `docs/review` launches only the isolated **Restriction Validation** world, copied from this project's native test world. It is excluded from release builds. The real client player performs the dash: server-side travel is 16 blocks, aura falls from 10,000 to 9,800, and no cooldown remains. The review also exercises the dash renderer and GeckoLib recovery bloom. The client confirms the camera position and captures both effects without missing standalone resources or renderer exceptions. Screenshots: [dash](media/standalone-dash.png) and [recovery](media/standalone-recovery.png).

Art retained: five race portraits, Chained and Air Sovereignty icons, the original broken-chain geometry and smooth effect materials, recovery wisps and the original `vital_corolla` Blockbench model/GeckoLib animation/glow texture. Resources and animation keys belong to the new addon. The distribution audit validates the packaged assets, native dependency metadata and mixin classes and rejects Myths classes, bundled dependency JARs and the optional review fixture.

The build retains native dependency warnings where applicable; it does not rewrite Tensura assets. The client review uses software OpenGL on an isolated X display.

## Installation boundary

The loader rejects co-installation with `wildhunt` to avoid running two implementations of the same pact/flight hooks. This is an independent namespace, not an automatic converter for an existing Myths world. The installation guide describes that distinction and the required native dependencies.

Release: `releases/tensura-heavenly-restriction-1.0.0.jar` and `releases/tensura-heavenly-restriction-1.0.0.sha256` in the repository root. The JAR must be installed on client and server.
