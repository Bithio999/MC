# Tensura Myths 0.22.0 validation

Java 21.0.12.1+1 / Gradle 8.14.3 / Minecraft 1.21.1 / NeoForge 21.1.209 /
Tensura 2.0.1.3 / ManasCore 4.0.0.2, using pinned repository dependencies.

## Native behavior

Six new native GameTests cover Aion's singularity barrage:

- Twenty distinct, deterministic airborne origins, curved endpoints, continuous
  launch positions, finite velocities and volume-preserving deformation ratios.
- A real aimed target, unfrozen tracking, per-salvo aim locks, later salvos retaining
  tracking, and the synchronized cast clock rather than an entity's spawn age.
- The last valid destination remaining fixed when a target leaves the 24-block
  formation tether.
- Standalone and Horizon-empowered attacks through the actual native damage
  pipeline: five accepted hits, twelve ticks apart, without resetting hurt timers;
  9,600 / 12,000 raw budgets before mastery, 28K MP payment, no AP payment and the
  existing 45-second cooldown. A replenished Resistance-IV practice recipient
  retains native hit immunity between strikes.
- Natural completion, forgetting Aion and lifecycle cleanup, including locating
  an owned visual in another loaded dimension. The fixture waits for real entity
  tracking and loads its remote test chunk before checking cleanup.

The existing appraisal, all-skill combat/balance, progression, Godly authority,
Causal Reversal, field consumption and ownership protections remain in the suite.
An initial fixture exposed a same-tick entity-index delay; the barrage now allows
its freshly spawned visual to enter tracking before treating absence as removal.

## Rendering and animation

Graphical validation uses only the isolated `Myths Validation` world. The visual
has been inspected at formation, accelerated flight and impact stages in a real
Minecraft client using software OpenGL. The checks include the normal play view,
a closer view, visible deformation, separate disk/ring layers, curved wakes and
readable dark cores. Launch headings align with the departure tangent during
anticipation, so near-zero eased speed does not produce an orientation snap. The original periodic accretion texture was adjusted to
reduce high-frequency shimmer during movement.

The core meshes are cached, emit directly into a reused material batch and use
lower tessellation for distant nodes. Individual node distance determines detail;
all twenty remain visible even with decorative particles reduced. The image and
video assets are captured in-game. Temporary instrumentation stays under `/tmp`
and is excluded from the packaged mod.

This checks native rendering and behavior in the provided environment, rather
than claiming performance parity across GPUs or third-party shader packs. The
renderer uses Minecraft's native shaders and does not require a shader pack.

[Animation/optics references and implementation rationale](research/AION_SINGULARITY_ANIMATION.md).

## Final checks

- **All 165 required native GameTests passed**, zero failures; final full run
  `gradle build runGameTestServer` completed successfully in **2m 4s**.
- The final client-only optical/performance refinements also passed `gradle build`
  and **all 11 JUnit tests**. The packaged **109 compiled classes** and original
  effect texture match their build/source files byte for byte.
- Native 720p / 30-frame-per-second capture includes formation, flight and all
  five impact salvos. Its GIF and stills are included in the Aion article and
  visual-effects gallery. The validation clip is silent.
- **294 responsive article checks**, **66 ability anchors**, offline search,
  **7,517 local HTML references** and **1,591 prepared Wiki links** passed.
- All **637 wiki files** match the offline ZIP entries. The separate GitHub
  Wiki service remains unavailable; the prepared Wiki edition is in the repository.
- No temporary validation instrumentation or bundled dependency JARs are in the mod.
- Release JAR SHA-256: `e6e09597466c6b45e7b2189957833aa30e21767ab3953f40f651b1d2e370bd15`.
