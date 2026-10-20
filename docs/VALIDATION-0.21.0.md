# Tensura Myths 0.21.0 validation

Java 21.0.12.1+1 / Gradle 8.14.3 / Minecraft 1.21.1 / NeoForge 21.1.209 /
Tensura 2.0.1.3 / ManasCore 4.0.0.2, using the pinned repository dependencies.

## Lugh appraisal library

Seven new native GameTests exercise the persistent library and its integration:

- Migrate an old three-entry archive, preserve custom techniques and active slot,
  restore replaced origin techniques, upgrade old origin damage, then round-trip
  the complete library and stable bindings through native skill NBT.
- Accumulate different modes from one skill, retain existing bindings, and refresh
  a known technique without changing its stable ID or duplicating it. Refreshes
  update the slots bound to that record and survive save/load.
- Reject unknown record IDs, invalid slots, malformed/extra arguments and unknown
  actions without changing stored damage. Forgetting Lugh removes permission to
  configure its library; the state payload reports missing ownership.
- Open with Shift + Skill Appraisal while cooling down at zero MP. Selecting and
  binding do not charge or refund resources, reset either mode's cooldown, or
  award mastery. Existing free crouch cycling remains covered by its prior test.
- Observe a native Rune Smith damage source and mode, retain both observed and
  bounded reconstruction damage, and expose source type/name, bindings and mastery
  scale. Modifying a copied state payload cannot modify authoritative records.
- Fill all 256 records without overwriting entries. Refreshing a known technique
  still works at capacity; a new technique is rejected.
- Freeze the three bound records for Grand Synthesis. Reappraising or rebinding
  afterward cannot rewrite the pending attack plan.

The prior real native-hit appraisal fixture now verifies that appraisal appends
an entry, repeated appraisal refreshes it, and the active origin binding remains
unchanged. Existing Countermand, source normalization, attack geometry, timing,
Godly traits, other skills and progression fixtures remain in the suite.

## Client interface

The reference is the pinned Tensura `PrideSkill` and
`SubAbilityModeSelectionScreen`: the same native sub-ability texture, six-row
ability bars, source icons, search widget, scrollbar and detail-panel geometry
are used. The footer selects three Art bindings instead of editing native
Pride presets. The active slot has green text, and the action names its target
slot explicitly.

Graphical checks use the isolated `Myths Validation` world and an 854×480
Minecraft window on software OpenGL. No user save is opened. Checkpoints include
opening the menu at zero MP during cooldown, inspecting initial and observed
records, search, list/detail scrolling, selecting a slot and binding through the
real C2S payload, and retaining the layout in the native small-window bounds.
Temporary validation helpers remain outside the repository and release JAR.

## Packaging and documentation

The release includes the rebuilt addon, SHA-256 checksum, updated Lugh article,
prepared GitHub Wiki pages, styled HTML and self-contained offline wiki ZIP.
The separate GitHub Wiki service remains unavailable; the prepared edition is
committed in the repository. Final build/test and artifact checks are recorded
below.

## Final results

- `gradle runGameTestServer`: **all 159 required native GameTests passed**,
  zero failures. The final server run completed successfully in 1m 49s.
- `gradle build`: **11 JUnit tests passed** and the final addon compiled and
  packaged successfully after the client label/clipping refinements.
- The interactive menu was checked with a real integrated server; search and
  both scrolling areas work, and binding clicks update the saved server loadout.
- The packaged JAR's 104 compiled classes match the build outputs byte for byte.
  It contains no temporary validation helpers or bundled dependency JARs.

- Styled wiki: **294 responsive page checks** at 1440px and 390px, all
  **66 ability anchors**, the Lugh binding instructions and offline search passed.
- **7,510 local HTML references** and **1,591 prepared Wiki links** resolve.
  All **632 wiki files** match their offline ZIP entries byte for byte.
- Release JAR SHA-256: `4372700f1cfeb1355fabdd088332b32308e62199e52c74f3bcb1dfa8ade9ec52`.
