# 0.35.4 validation — automatic Unbound evolution

Target: Java 21, Minecraft 1.21.1, NeoForge **21.1.209**, Tensura 2.0.1.3 and ManasCore 4.0.0.2. Independent addon: **1.1.4**. All **437 tests passed**: **330 full-mod native GameTests**, **96 standalone native GameTests** and **11 JUnit tests**. Both production builds passed.

## Implementation

The existing server-side Chained skill tick and periodic living-entity check now complete Unbound learning automatically once the owner is Heavenbreaker, owns permanent, independent, fully mastered Chained, and has True Hero or True Demon Lord status. Equipping, toggling, pressing and manual learning are unnecessary.

Completion uses the actual stored instance and native `addLearnPoint` / unlock callbacks, with the one-use no-magicule-cost flag. The existing reward-first replacement preserves mastery, toggle, recorded ancestry, all 27 slots across nine presets, dash cooldown, magicule ceiling and starting-aura exemptions. Resource pools and capacity are unchanged. Existing unfinished Unbound entries are completed in place only after the same gates pass. Temporary or derived Chained cannot trigger permanent evolution; finished Unbound is not reset by later checks.

## Native coverage

Both distributions exercise:

- Every prerequisite independently, with Chained retained when a requirement is missing; True Demon Lord and True Hero both qualify.
- Automatic completion through the skill tick with no activation or learning input, including zero maximum magicules and preservation of current aura/magicules, mastery, toggle, cooldown, inherited ancestry and every preset slot.
- In-place completion of saved unfinished Unbound entries, including entries with more than one missing learning point; no completion before awakening status is acquired.
- Rejection of temporary and derived Chained.
- A tracked living owner evolving through actual server ticks while the skill is unslotted and untoggled.
- Idempotent checks after completion, active numeric Battlewill buff upgrades, and all existing Chained/Unbound combat, resource, acquisition and reincarnation regressions.

## Commands

```sh
gradle build runGameTestServer --console=plain
# In standalone/tensura-additional-additions:
gradle build runGameTestServer --console=plain
python3 scripts/verify-distribution.py
# From the repository root:
python3 scripts/build-wiki.py
```

Native test runs use disposable directories under `/tmp`, preserving development/client saves. Distribution audits verify metadata, pinned NeoForge requirements, matching source/JAR policy resources and mixin classes, and standalone isolation. The wiki source, generated HTML, GitHub export and offline archive document automatic acquisition. No graphical client review was needed for this server-side change.
