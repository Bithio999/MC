# Tensura Myths 0.24.0 validation

Validated 20 October 2026 using Java 21, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2.

## Native gameplay

`gradle build runGameTestServer` passed **all 171 required native GameTests**,
with zero failures. **All 11 JUnit tests** passed as well.

The Lugh regression coverage checks:

- Flat **+49** additions to both native learning and mastery gain. A native
  Musician instance receives the increased mastery gain, and its native learning
  increment increases by exactly 49 with the same random seed.
- Removal of the former Lugh attack-speed bonus, preservation of another source’s
  speed modifier, and cleanup of both gain modifiers when disabled or forgotten.
- The actual ManasCore cooldown timer update, including native Musician,
  an addon Unique and Aion. Other skills recover twice as fast; Lugh’s
  Reconstructed Art, Countermand and Grand Synthesis retain normal recovery.
  Odd values finish on native whole-second updates and zero never underflows.
- No extra recovery while disabled or when the paid upkeep lease has expired
  with no magicules. The bonus accelerates native recovery rather than resetting
  or repeatedly halving stored cooldowns on toggle.
- Automatic capture from real native damage at the **64-block boundary**,
  rejection beyond it, no capture while off or unfunded, native damage/source
  parameters, bounded reconstruction and stable refresh without duplicates.
- Normal Skill Bank activation at zero magicules, despite a legacy menu cooldown,
  plus the existing free binding, save migration, storage limit, untrusted packet,
  source parameter and frozen Synthesis-plan checks.

The passive watches actual damaging techniques, preserving the reconstruction
system’s existing observation rules. Missed attacks and purely non-damaging
utility moves do not supply a damage reading. The original skill is not granted
or executed, and each owner’s existing bank and bindings persist.

## Native client

Opened only the isolated **Myths Validation** save. Used normal, non-crouching
mode-0 activation with zero magicules and an eight-second legacy menu cooldown.
The native panel opened with its **Skill Bank** title, existing technique entries,
source icon, parameters and Art bindings intact. Its updated 1280×720 screenshot
is included in the Lugh article and offline bundle. Temporary instrumentation
remains outside the repository and release JAR.

## Documentation and release

- **294 responsive article checks**, **66 ability anchors**, Skill Bank search,
  and four Lugh media previews passed without browser page errors.
- **7,517 local HTML references** resolve; all **637 wiki files** match the offline
  ZIP byte for byte. The prepared GitHub Wiki edition remains in the repository;
  the separate GitHub Wiki service is unavailable.
- The release’s **112 compiled classes** match the final build byte for byte.
  No temporary graphical harness or bundled dependency JARs are included.
- Gameplay articles describe current mechanics. Development changes remain in
  the separate changelog.

Release JAR SHA-256:
`11689fbd9c94cb8962ed679f69c487376f47adea2148a5e5ce70e270102a804f`.
