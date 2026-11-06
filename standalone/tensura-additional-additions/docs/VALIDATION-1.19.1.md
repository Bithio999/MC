# 1.19.1 — Structure travel and discovery corrections

Teleport preparation previously requested the chunk containing a marker’s bounding-box center, then looked for a native start there only when the marker was predicted. Large structures own their start in a different chunk, so valid predictions could disappear. Old confirmed entries skipped verification. Discovered chunks only added valid starts; they never removed contradicted predictions, and stale Locate/survey references could restore them.

Travel now decodes the native origin chunk from the existing marker key and requests STRUCTURE_STARTS asynchronously. Every destination, predicted or confirmed, must have a valid native start there before its FEATURES-stage landing chunk is prepared. Actual bounds confirm/correct the icon. Preparation rechecks ownership, seals, cooldowns and dimension restrictions, releases the appropriate ticket at each stage, and retains markers on unreadable/failed futures. Missing sites do not open the native teleport menu. Safe landing and native resource spending/confirmation remain unchanged.

Native discovery now reconciles all starts in a chunk, including an empty result. A source-chunk index removes contradicted entries without scanning the whole atlas. Bounded authoritative discovery records protect against late survey results, source-chunk caches retain confirmed empty results, and affected completed survey tiles are invalidated. Final survey results are normalized against current discoveries before being cached. Stale Locate fallbacks are cleared and cannot authorize travel. Locate’s recorded fast path excludes predictions, which continue through native search verification.

## Verification

- **305 required native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. Four new regressions cover a predicted native start three chunks away from its icon, a missing prediction with a stale Locate fallback, a stale confirmed destination, and discovered empty/alternate starts overriding late results and surviving an atlas save/load round trip.
- The previous travel tests now install actual native StructureStart fixtures rather than inventing confirmed markers without corresponding starts. They retain ownership, duplicate-job, free preparation, safe landing, forgotten-skill and session checks.
- **57 JVM tests passed** in the clean baseline release build. The Tensura 2.0.1.4 / NeoForge 21.1.248 API profile also compiled and passed all 57 JVM tests. This is API coverage, not a full Neo Otherworld pack runtime test.
- The guarded normal-world travel profile passed: native stronghold search found a site 76,154 blocks from the viewed search center, both owned-skill menus opened with prefilled coordinates, and native Warp confirmation performed the actual Instant Transmission teleport.

The standalone distribution audit passed: no review fixtures, Myths classes, bundled dependency jars or test-only presets are included.

No registry IDs, saved marker-key formats, pins, terrain caches, map rendering or skill costs change. Existing atlases are corrected as their structure-origin chunks are discovered or travel validation checks them; a new world is not required. Real structures remain marked after discovery.
