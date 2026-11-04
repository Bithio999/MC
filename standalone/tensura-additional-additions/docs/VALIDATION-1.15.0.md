# 1.15.0 — Automatic structure markers in unexplored areas

The visible map area now drives an automatic survey at every zoom level. It is independent of Locate, whose 100,000-block search remains centered on the viewed map as previously requested. Sites OFF cancels the automatic survey; closing the map, losing the skill, changing dimension or stopping the server releases subscriptions and cancels unobserved work.

## Generation and existing worlds

The worker enumerates native random-spread regions, concentric rings and custom-placement candidates, including vanilla and addon structure sets. Minecraft's own `ChunkGenerator.createStructures` runs against disposable, unregistered `ProtoChunk` instances, preserving weighted variants, fallback ordering, frequency/exclusion rules, biome checks, piece generation and bounding boxes. It never registers those chunks with the server, places structure blocks or runs entity/feature generation.

Existing chunk starts are read through the native asynchronous storage worker and decoded with native `StructureStart.loadStaticStart`. Existing saved starts win over regenerated locations. Filesystem checks avoid calling the native read API on nonexistent region files, because that API would otherwise create empty files. No new region files or terrain chunks are needed for the automatic survey.

World-generation locations are saved with a backward-compatible `predicted` flag; older records load as confirmed. Confirmed starts always override predictions. Fresh tile results replace old predictions for their start chunks. Explicit travel prepares real terrain and validates the requested predicted structure before handing the native coordinate menu to the player, preventing a stale marker from authorizing an empty destination.

## Bounds and performance

Viewport bounds and ownership are validated on the server. A structure-bounds gutter covers starts just outside the visible rectangle. Adaptive tiles keep the viewport plan at no more than 384 entries. One worker handles surveys, with eight outstanding shared jobs; watchers reuse matching jobs. Cancellation is checked between candidates and while waiting for storage. The result cache has limits of 512 tiles and 65,536 retained marker records and can repopulate evicted catalogue entries. Oversized individual results stop with an explicit incomplete-survey state. Errors are logged and can be retried by reopening the map.

Exact native height/column samples are memoized only inside a survey scope, with 8,192 height entries and 256 columns. Cached columns return copies, preserving caller mutation semantics. The thread-local scope closes on success, failure and cancellation; ordinary generation and gameplay use uncached native behavior.

Dense packets use counted spatial groups rather than dropping records after the old 2,048-entry limit. Categories remain separate under normal grouping. Client clustering adds represented counts; zooming in reveals the underlying sites. Server saved markers remain bounded by the existing catalogue limit.

## Verification

All **272 required native GameTests** passed. New scenarios cover predicted-marker persistence and confirmed-start precedence, viewport/bounds validation and disabling surveys, completion without distant terrain loads, honoring structures-disabled world settings, and exact memoized sample values with isolation of mutable columns. The native game-test preset disables natural structures; normal-world comparison is also performed in the integrated client review.

JVM coverage checks complete negative-coordinate/gutter coverage, adaptive plans for large viewports, invalid bounds, lossless packet counts and preservation of sparse/category markers. The integrated review is opt-in with `-PmapSurveyOnly=true`, in the guarded disposable normal-generation **Marauders Map Review** world. It pans and zooms through unexplored terrain without invoking Locate, checks automatic markers and unchanged distant region files, then explicitly creates one real native structure start and compares the exact type and bounding box against its marker.

All **45 JVM tests** passed in the clean release build. The baseline artifact targets Tensura 2.0.1.3 / NeoForge 21.1.209; compilation also passed against the optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile. The optional profile is compile coverage, not a full modpack runtime test. The distribution audit verifies standalone contents, dependency metadata, resources and survey mixin registration, excluding review fixtures and bundled dependencies.

The normal-world integrated review completed successfully with **307 automatic markers** in its first unexplored viewport. Independent pan and zoom surveys completed without Locate, distant region-file creation or full terrain loads. A predicted native Tensura goblin-village variant and bounding box matched its actual generated start exactly, and the saved marker upgraded to confirmed. Screenshots of the wide view, pan and zoom are included in the wiki media. The review exited cleanly with `BUILD SUCCESSFUL`.
