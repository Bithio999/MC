# Tensura Myths 0.15.0 validation

Java 21.0.12.1+1, Gradle 8.14.3, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2; repository-pinned dependencies.

## Build and gameplay

`gradle build runGameTestServer --console=plain` passed **104 required native
GameTests** and **11 JUnit tests**, zero failures. The final client-only muzzle
adjustment was followed by another successful `gradle build`, all 11 JUnit
tests and an in-game presentation check. Server skill code did not change
after the 104-test run.

Five added native tests cover cosmetic entity expiry, lack of damage/collision,
actual NPC ownership, one refreshed visual lease per oil pool, matching ignited
radius, retirement of only the replaced pool's surface, ground projection
without moving the stored damage center, and bounded tier-specific recovery
pulses only after actual healing. Existing damage, costs, targeting, friendly/PvP
protection, barrier recovery, Mana Crucible conservation/held lifecycle,
secondary evolution, fusion and save-migration tests remain passing.

The first visual-test fixtures needed the established entity-manager tracking
wait before inspecting newly spawned entities. The initial fixture failures
were corrected without removing their assertions. A real client miss-cast
also exposed oil graphics buried beneath terrain; the cosmetic projection fix
and its native regression test passed in the final suite.

## Native client presentation

The development client ran in the isolated **Myths Validation** world at
1280 × 800 using software OpenGL. Thirty-seven staged render captures exercised
all non-Thoth visual kinds/styles used by this release, including the three
recovery tiers, lit/unlit oil and both gate endpoints. These invoke the actual
entity renderer; staged geometry alone is not evidence of skill activation.

Additional casts through native learned skill instances exercised Undertow,
Oil Lash/ignition, Avalanche Crash, Living Grove, Runic Mine/detonation,
Mirror Aegis/Prismatic Lance, Thunderhead/Plasma Lance, Winter Orchard,
Twin Gate and Aion's Horizon/checkpoint/Verdict. Twin Gate correctly rejected
a too-close placement before succeeding with a valid forward aim. Aion's
Worldline Sever was equipped in the native preset and held with the real Z key.
First-person checks covered that channel, Railbolt, the optical/plasma rays
and orbiting mirrors. The initial Railbolt preview was too bright at the
camera; its final muzzle offset preserves the target endpoint and was verified
with a new native cast. Reduced-particle checks kept Railbolt and ward geometry
visible. Particle settings were restored afterward.

Inspected animation includes continuous trails, counter-rotation, curved
branches, scrolling caustics, oil ignition/expiry and the timed warning/strike
forms. All twelve updated kit articles have current native screenshots; the
gallery identifies staged views and retains per-ability damage metadata.
The original Thoth renderer and cooldown-free 2:1 Mana Crucible remain intact.
The user's **New World** was untouched. Attach/Robot helpers remain outside the
repository and release JAR.

No new renderer exceptions occurred. Existing native model warnings, unavailable
OpenAL audio and the development client's authentication-key fetch failure are
unrelated environment/base-mod limitations. This is a functional software-GPU
smoke check, not a hardware performance benchmark or a shader-pack compatibility
claim. Material batching, filtered masks, distance detail reduction and native
particle-setting decoration limits are implemented.

## Documentation and artifacts

The styled wiki contains **143 articles plus its Main Page**, twelve new native
screenshots and fifty indexed ability entries. Responsive checks cover all
**288 desktop/mobile page views**, image loading, no horizontal overflow,
JavaScript errors, skill search and ability-anchor navigation. Local references,
GitHub Wiki cross-links and offline ZIP contents are verified during packaging.
The separate GitHub Wiki service remains unavailable to this integration;
its generated Markdown edition is committed under `wiki/github`.

The release JAR is byte-identical to `build/libs/tensura-myths-0.15.0.jar`.
All seventeen original new textures and the compiled renderer classes match the
source/build outputs. No attach helpers or downloaded dependency JARs are
bundled. The checked-in texture generator requires Pillow only for regeneration;
players need no Python or shader pack.

Release SHA-256: `7ac6e2a07565d16d3052c01db1a3d2985a254ddb9554c347157e2a4e9dec15a9`.
