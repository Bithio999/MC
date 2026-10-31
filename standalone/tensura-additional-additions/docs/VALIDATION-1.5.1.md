# 1.5.1 — Parallel World Jumper fixes

Parallel World Jumper now supplies its native localization keys, English fallback and an explicit Unique-coloured name override. Tensura’s default coloured-name implementation bypassed the previous plain-name override. World Jump / World Select retain their mode IDs and native grey labels.

World Select uses Tensura’s separate mode-learning system. Select its mode and press the skill to gain learning points; after learning, pressing opens the menu. It follows Tensura’s configured requirements, gain and learning cooldown. Owning or mastering the overall skill alone does not unlock the mode. Server-side menu, selection and destination checks enforce this requirement, including existing pending selections. Learning progress survives travel with the current character.

New-world numbering comes from the whole library, so returning to an older world cannot reset the next generated name to Parallel 1. Existing duplicate generated labels are repaired without changing world IDs, seeds or terrain. Return travel applies the journal’s destination name to the staged save metadata.

Temporary file-access failures during copying, directory moves and transaction cleanup receive bounded retries (six attempts, at most 775 ms of backoff per operation). Permanent missing-file/path-collision errors fail immediately. The recovery screen wraps and displays a short underlying error; full details remain in the log. Only individual I/O operations retry, never an entire transfer. The user’s exact intermittent recovery error has not been reproduced, so this is resilience and diagnostics rather than a confirmed diagnosis.

## Verification

- **30 JUnit tests passed:** includes global numbering after returns, duplicate-label repair, sidecar compatibility, temporary/permanent I/O errors, plus existing transfer/rollback, library boundaries and guide checks.
- **157 native GameTests passed** with Tensura 2.0.1.3 / NeoForge 21.1.209. New coverage checks native mode-learning thresholds and copied-instance persistence; name checks include plain, coloured, chat and both mode labels.
- **Standalone live client passed:** B → A → B → fresh through the native menu, with mode learning and current character/build preservation. The fresh world was named `World Hop Preview · Parallel 2`; the 128-world cap, LAN rejection and native owner/library deletion checks also passed.
- **Tensura Neo Otherworld 1.1.3a development-client check:** separate mode learning, actual menu selection and B → A → B returns passed, preserving inventory, EP, current XP, a placed diamond block and custom-dimension terrain. Fresh terrain generation then failed inside Noise Boost with `IllegalAccessError` on the `PalettedContainer.Configuration` constructor. This limits the full-pack result; it does not establish the cause of the user’s earlier intermittent recovery screen.
- Production packaging checks localization, library/menu classes, supported dependencies and exclusion of review fixtures and dependency JARs.

Registry IDs, saved skill IDs and supported dependency ranges remain unchanged. This patch updates only TensuraAdditionalAdditions. [Previous library validation](VALIDATION-1.5.0.md).
