# Tensura Myths 0.14.0 validation

## Automated gameplay

Java 21.0.12.1+1; Gradle 8.14.3; Minecraft 1.21.1; NeoForge 21.1.209;
Tensura 2.0.1.3; ManasCore 4.0.0.2; repository-pinned runtime dependencies.

`gradle build runGameTestServer --console=plain` completed successfully:
**95 required native GameTests passed**, **11 JUnit tests passed**, zero failures.

New native coverage checks:

- Multiple ordinary bonuses across addon/native-style namespaces select the
  strongest; +75% Godly power and negative penalties remain; removing the best
  source reveals the next one. Non-elemental attributes retain normal stacking.
- Flat, multiplied-base and multiplied-total sources are compared correctly;
  innate base values and single powerful sources are not hard-capped.
- Saved incidental shields are removed from Mirrorbound, Demeter, Janus and
  Thoth without removing unrelated barrier modifiers. Their toggles no longer
  create passive Multilayer barriers; unfunded passives still clean attributes.
- Crystal Bulwark actually grants 500 Multilayer capacity and 50 Physical/Magic
  Barrier for 160 ticks through the native attributes and ownership lease.
- New Unique and Ultimate attacks beat their previous damage budgets through
  the real native damage pipeline, against the same controlled target.

Existing tests retain all eight Unique mechanics, three combination Ultimates,
Thoth's typed chapters, native MP payment/conversion, resource/drain exclusions,
reflection ownership, delayed warning behavior, all 21 Vritra combo events,
allied healing, safe gates, timed ward cleanup, Aion's full kit, original apex
barrier recovery, race progression, regeneration, NPCs and all 105 fusions.

The wave sweep fixture now exercises sixteen advances for its new per-tick
cadence; the dash fixture expects its new four-second cooldown. A timed Aion
fixture uses sun-safe Husk targets so daylight cannot change the untouched
victim's HP. The unrelated attack-attribute check uses a standalone native
attribute instance, excluding the test player's existing race modifiers.

## Client and visual scope

Fresh native client launched successfully with the final code. Thoth's Lexicon,
Spellbreak, Mana Crucible and Astral Library are exercised in the isolated
**Myths Validation** world. The user's New World is untouched. Screenshots are
real client captures, with a temporary observation camera for clear views.
Development attach helpers live only in `/tmp`, outside the source and JAR.

Original 1,024px seal and grimoire textures load through standard Minecraft
render types. Additive glow was checked and corrected for premultiplied RGB;
pages and inscription lines remain visible. Library bolts retain distinct
chapter colors and book-aligned origins. Effects use bounded client geometry,
partial-tick animation and no required shader pack.

Automated gameplay verifies behavior and fixed budgets. It does not establish
balance against every modpack boss or performance on every GPU.

## Documentation and packaged artifacts

The styled site and GitHub-ready Markdown were regenerated from the canonical
articles. All 286 page/viewport checks (143 pages at 1440px and 390px) passed,
including image loading, search, ability anchors and no horizontal overflow or
JavaScript errors. All 6,969 local HTML references and 1,465 GitHub cross-page
links resolve; all 145 exported Markdown files pass the local-link checks.

All 588 files in the offline ZIP match the current wiki byte-for-byte. New Thoth
screenshots show all four arts; the nine-second MP4 shows Astral Library's
animated books, inscriptions and autonomous elemental attacks. A normal native
Z-key cast was also inspected in first person with the HUD enabled; the aim
center remains visible. Player movement was exercised with the formation active.

The release JAR matches the successful build byte-for-byte, contains version
0.14.0, the three original effect textures and elemental policy mixin, and
contains no development attach helpers. SHA-256:

`d6e8975644da196e694ff6377d3c796d86a4b184608bf571ec9e6bdd3b124e27`

The separate GitHub Wiki application remains unavailable under the repository's
existing integration permissions. The committed repository wiki, styled offline
ZIP and prepared GitHub Markdown are updated; no separate Wiki-app publication
is claimed.
