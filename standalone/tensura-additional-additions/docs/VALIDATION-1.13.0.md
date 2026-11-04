# 1.13.0 — Unvisited terrain and live players

Marauder’s Map now renders terrain at arbitrary coordinates in the current dimension, including areas outside player exploration and entity/chunk tracking distance. It preserves native skill gating, structure discovery, private pins, dimension changes and world-seed isolation.

## Terrain and detail

A single daemon worker samples immutable native generator/random-state/biome inputs. An 8×8 first pass fills each tile quickly; 32×32 refinement follows. Native height and biome sampling supplies landforms, water depth and biome colors. Preview sampling does not request chunks, change blocks, add chunk tickets, send the world seed, or teleport the player. It checks viewport/session cancellation between rows.

At close zoom, actual native feature-stage chunks supply surface blocks, elevations and biome tints. Native asynchronous scheduling is used directly, without managed blocking or joining a chunk future. Across the server, at most two detail futures are active and new detail generation starts no more often than once per four ticks. Temporary tickets hold only the feature-stage status, never entity-ticking/full-chunk status, and are released on completion, close, logout, dimension change or server shutdown. Native generation can save newly generated terrain; this is progressive viewport refinement, not whole-world pregeneration.

The client samples resource-pack top-face sprites into 4×4 texels per block, adds northwest relief, fine contours and depth-tinted water, and overlays live client surface updates. An existing off-by-one error that sampled one block below the surface was fixed. Native detail includes actual features and existing builds; initial previews provide base terrain/biome estimates until that detail arrives.

Queues and memory are bounded: eight pending tile IDs per session, sixteen queued worker tasks, 512 server preview tiles, and 768 client preview/detail textures, in addition to the existing bounded recorded-terrain atlas. Viewport acknowledgements prevent sending terrain requests against an obsolete server viewport. The detail throttle retains availability until used, so periodic client retries cannot repeatedly miss a single allowed tick. Old-world and old-session responses are discarded.

## Player locations

The server sends the complete online roster to authorized map sessions, including position, facing, identity and dimension, twice per second. Tracking/render distance does not filter this list. Native snapshots also include the same roster. The map renders named cyan markers in the current dimension; the sidebar includes other-dimension players and disables centering them on the wrong map. Search includes dimension names. Departed players disappear with the next roster update. Private pins are unchanged.

## Verification

Native regressions cover unloaded-generator requests and viewport/spacing/overflow validation, exact top-surface block sampling, private pin persistence and gating, native structure starts and discovery, and online roster updates beyond tracking distance and across dimensions. Existing geometry and guide/unit tests also run.

The opt-in integrated-client review exercises the actual skill/menu/network/UI. It moves only the map to unvisited remote terrain (over 100,000 blocks from the character), waits for both a native preview and real feature-stage textured detail, and verifies that the client has not loaded that remote chunk and the player has not moved. It also checks a distant online-roster review player, a review player in the Nether, updates after moving the former 5,000 blocks away, cross-dimension selection safety, broad overview rendering, and all previous pin/search/menu/size controls. Review players are disposable FakePlayers with embedded channels; this is not a test with two additional human clients. Screenshots are actual runtime captures.

All **264 required native GameTests** and **36 JVM tests** passed on Tensura 2.0.1.3 / NeoForge 21.1.209. The final integrated-client review passed with both its original and extended pass markers. Its remote native terrain sample was at **122048, -158976**, while the character stayed near **-800, 160**. It confirmed live out-of-tracking-range positions and a Nether player, fitted controls at GUI scales 2/4, retained pins and structure discovery, and saved the updated screenshots included in the wiki.

Compilation also passed against the optional **Tensura 2.0.1.4 / NeoForge 21.1.248 API profile**. This checks API compatibility; it does not represent a full Neo Otherworld modpack runtime review.

The final clean baseline build, 36 JVM tests and independent-distribution audit passed. The audit checks 1.13.0 metadata, both terrain classes, required mixins, guide/assets, native dependency ranges and the absence of review fixtures, test presets, Myths code or bundled dependencies. The wiki renderer/verifier checks all scoped pages, media, links and ability anchors.
