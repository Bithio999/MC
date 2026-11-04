# 1.12.0 — Marauder’s Map

The Unique and its Open Map mode use native Tensura registration, icon/name/description, menus and reincarnation integration, with learned/seal checks and no resource fee. The skill is eligible for Fettered Human rolls.

The client records visible full chunks at four chunks per tick, using height relief, native map materials and biome grass/foliage/water colors. It prioritizes nearby chunks. A 128-region LRU bounds 256×256 terrain images/textures; distant or very wide views use a chunk-average overview. Cursor-centered zoom and double-precision coordinate conversion keep markers aligned. Dark areas remain unrecorded. A bounded IO queue loads/saves cache files asynchronously with atomic replacement; deferred loads retry under queue pressure and must complete before replacing existing disk data. Native menu blur is disabled so terrain and labels remain crisp.

Caches are partitioned by a server-saved world UUID, dimension and ceiling-plane layer. Native MapData detects a changed world seed and resets its world ID/catalog/pins for a new World Hop continuation; archived worlds retain their own atlas. No seed is sent to clients. The server stores up to 256 private pins per player and 65,536 confirmed structure markers. Session ownership, skill availability, pin identifiers, labels, palette/icon indices, heights, coordinates and world borders are validated. Dimension transitions close old sessions.

Markers come from real structure starts, including addon registry entries. Locate first checks known nearby starts, then scans native placement candidates incrementally within 128 chunks. Per search, scanning is limited to 512 positions per tick and 128 generation candidates; the server schedules at most two new native structure-start futures per tick globally. The private native scheduler is invoked directly because Minecraft’s public getChunkFuture wrapper waits when called on the main thread. No synchronous chunk join or full terrain generation is used. Cancellation drops the job reference without cancelling shared native futures.

Native scenarios cover skill/reincarnation/seal integration, private pin creation/edit/deletion/validation/cap/persistence, genuine native structure-start bounds/deduplication, world-seed partitioning, scan bounds, cancellation and native asynchronous scheduling. Geometry JVM tests cover cursor anchoring, pan conversion at all scales, negative coordinates, bounds and invalid inputs.

The integrated client review uses a disposable normal-generation save and a real village. It tests native menu/name/icon selection and activation, pan/zoom/recenter, help, C2S pin creation/appearance/edit/invalid name/confirmed deletion and NBT/reopen persistence, registry structure search (including Tensura), the real village marker, Go to without teleportation, terrain disk caching and GUI scales 2/4. Screenshots in the wiki are actual in-game captures.

All **261 native GameTests and 36 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. The integrated client review passed with 218 recorded terrain chunks; the final captures show the crisp, unblurred map and menus at both GUI sizes.

A further live pass clears the recorded village catalogue before locating it, verifies that the incremental job checks candidates and discovers the real native start, and confirms Show result. It also checks F-to-search/canvas focus recovery. The opaque map and its dialogs render without Minecraft’s default blur; popup edits keep the map context visible.

Compilation also passed against the optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile. This is API coverage, not a fresh full Neo Otherworld modpack runtime test.

The clean baseline build, 36 JVM tests and distribution audit passed. The audit verifies the new skill/guide/art/classes, native dependencies and all mixins, and excludes the client review, test preset, embedded dependencies and Myths code. The release includes a SHA-256 checksum.
