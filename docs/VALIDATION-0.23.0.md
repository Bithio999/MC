# Tensura Myths 0.23.0 validation

Validated 20–21 October 2026 using Java 21, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2.

## Native gameplay

`gradle build runGameTestServer` completed successfully. **All 167 required native
GameTests passed**, zero failures; the final full run took **2m 40s**. The
**11 JUnit tests** passed as well.

The revised barrage checks cover:

- Twenty distinct seeded airborne origins, continuous curved launch paths,
  matching collision endpoints, finite velocity and volume-preserving deformation.
- Live tracking through final approach, including movement beyond the former
  24-block formation tether; completed impacts remain fixed at their own points.
- Twenty accepted native hits, two ticks apart, against a target moving around
  an eight-block-radius path. Both normal and Horizon-empowered variants retain
  9,600 / 12,000 total raw damage before mastery and shared Godly traits.
- Ordinary Space-elemental hits retain hurt cooldown; individual Verdict
  collisions register even after a normal hit without changing later normal hits.
- Empty targeting spends no MP, cooldown or mastery and preserves Event Horizon.
  Valid casting keeps the 28K-MP / 45-second economy and consumes empowerment.
- Shared cast-clock synchronization, destination-aware culling, completion,
  forgetting Aion and cleanup of owned visuals across loaded dimensions.

The native per-source cooldown opt-in preserves the original Space damage type
and native ability metadata. It does not globally change Space tags, reset hurt
timers or directly subtract health.

## Native client and optics

Opened only the isolated **Myths Validation** development save. Inspected the
actual renderer at 1280×720 and captured a complete live cast, including formation,
accelerating flight and the individual impacts. The updated GIF, MP4 and three
stills are included in the Aion article and offline wiki.

Disks retain fixed seeded world planes independently of core stretch and flight
heading. Broad striated annuli and lensed rear-disk arches follow the Interstellar
production research. The core remains dark; original textures use premultiplied
RGB masks appropriate to native additive blending, avoiding bright square edges.
Collision optics have larger ignition blooms, pressure fronts and plasma streaks.
The outer 10 / 12-block light footprints are cosmetic; damage stays 3.75 / 4.5.

Final client-only optical refinements passed `gradle build` and all 11 JUnit tests.
The packaged **111 compiled classes** and both original singularity textures
match their final build/source files byte for byte. Temporary client
instrumentation stays under `/tmp` and is excluded from the mod.

The clip is silent because this environment has no usable audio device. Visual
inspection uses software OpenGL and does not establish GPU performance or
third-party shader-pack compatibility. The effect needs no shader pack.

[Retrieved media/animation references and design rationale](research/AION_SINGULARITY_ANIMATION.md).

## Release and documentation

The styled and prepared GitHub Wiki editions describe independent damage,
required targeting, uninterrupted homing, faster timing, native damage type and
cosmetic explosion boundaries. Development history stays in the changelog.
The self-contained offline bundle includes the updated gameplay media.

Documentation checks passed: **294 responsive article views**, **66 ability
anchors**, Genesis Verdict search and four Aion media previews; **7,517 local
HTML references**; **1,591 prepared Wiki links**. All **637 wiki files** match
the offline ZIP byte for byte. The separate GitHub Wiki service remains
unavailable; its prepared edition is included in the repository.

Release JAR SHA-256:
`edea2877b92cc8ac9795f57acba37fb3cc0182f30125cf3c0cb79f14c6968105`.
