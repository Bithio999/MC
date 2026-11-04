# 1.16.0 — Dimension atlas, contextual UI and shared exploration

Marker actions now open in a single input-capturing dropdown. Empty map-space right-click offers Add pin here. Layers and Map Tools replace permanent action grids. Atlas buttons use flat slate/gold styling. Pin, structure-search, travel and help dialogs render their own opaque background and never redraw parent text. Dropdown contents clip to their bounds; outside clicks dismiss and are consumed. Other dimensions supports scrolling, typing, arrow keys and Enter.

Vanilla item artwork labels the Overworld, Nether and End tabs, with a selected underline and narrated hover names. Other opens a searchable list of every other live server dimension. Each view retains its camera. Switching rotates the session token, releases tickets/subscriptions and invalidates previous terrain responses. The physical player dimension is validated separately from the viewed dimension. Pin bounds and structure/terrain generation use the viewed ServerLevel. Native travel remains limited to the character’s current dimension, retaining native costs and restrictions.

The server records two already-loaded chunks per tick globally, distributed across all online players, regardless of skill ownership. It never loads or tickets chunks for recording. Actual surfaces use stable block-state palettes and asynchronous atomic compressed files inside the active world’s data directory, separated by world identity, dimension and Nether height layer. A bounded 1,024-chunk detail cache and bounded single-thread I/O queue prevent unbounded working memory. Server shutdown drains pending writes for up to five seconds.

A sparse mip pyramid shares real explored chunk colors at broad zoom levels without scanning all explored terrain per request. Its saved base index is bounded to 262,144 chunk summaries; full detail files remain in the save. Coarse pixels average explored surfaces at their resolution. Visible client tiles refresh periodically; server sampling refreshes gradually. Missing or invalid shared detail drops its positive index entry and permits native terrain fallback. Existing map data loads without migration, and old private pins remain private. Recording is prospective; the server cannot reconstruct historical exploration from another computer’s local map cache.

## Verification

- All **275 required native GameTests** passed, including three new scenarios for remote session isolation, viewed-dimension pins/private ownership, and shared overview persistence with asynchronous stable-palette detail round-tripping.
- All **49 JVM tests** passed, including four new sparse-terrain index cases.
- The normal-world integrated review completed with `BUILD SUCCESSFUL` at GUI scales 2 and 4. It clicked contextual menus, validated/edited pins, opened deletion confirmation, switched all vanilla tabs and the searchable High Realm entry, verified remembered cameras and unchanged player position, and captured the opaque pin/search/help dialogs.
- A distant simulated online player without Marauder’s Map explored a checkerboard build. The recorder captured it automatically and the map received the shared real-surface detail without loading that chunk on the viewing client. The fixture uses an embedded-channel FakePlayer roster, not a second connected human client.

Native/client review sources and screenshots are included. See `docs/review-marauders-map/README.md` for reproduction. Production packages exclude review fixtures, test world presets and bundled dependencies.

The optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile compiled successfully. The release baseline remains Tensura 2.0.1.3 / NeoForge 21.1.209; the optional compile check is not a full modpack runtime test. The map packet protocol is now 5, so client and server should use the same updated addon version.

The first native suite exited with `BUILD SUCCESSFUL`. A final repeat again passed all 275 tests, then exhausted the environment disk while saving its generated Test Level and creating Gradle’s problems report. After shutdown, that generated test world was removed to restore space; the client review saves and screenshots were retained. The production artifact is rebuilt separately without the test preset.

The clean baseline production build completed with `BUILD SUCCESSFUL`; all 49 JVM tests passed with no failures. The standalone distribution audit passed, including the new UI/exploration classes and updated guide.
