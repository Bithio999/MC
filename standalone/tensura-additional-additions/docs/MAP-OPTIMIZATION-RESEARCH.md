# Marauder’s Map optimization research

Reviewed public source on 2026-11-05. This implementation uses the architectural patterns below; it introduces no mapping-mod dependency and contains no copied upstream implementation.

| Reference | Relevant approach | Application here |
| --- | --- | --- |
| [VoxelMap — PersistentMap](https://github.com/Updated-VoxelMap/VoxelMap/blob/master/common/src/main/java/com/mamiyaotaru/voxelmap/persistent/PersistentMap.java), [CachedRegion](https://github.com/Updated-VoxelMap/VoxelMap/blob/master/common/src/main/java/com/mamiyaotaru/voxelmap/persistent/CachedRegion.java) | Region reuse, background loading/saving, bounded region lifetime and zoom-aware rendering | Retain preview textures across reopen, cache viewport plans, retain block-pixel region textures at distant zoom and avoid loading empty regions. Use small, area-filtered recorded-region mip textures at distant zooms, with asynchronous disk reuse and a fixed byte budget. |
| [SeedMapper — SeedMapRenderer](https://github.com/xpple/SeedMapper/blob/master/src/main/java/dev/xpple/seedmapper/seedmap/SeedMapRenderer.java), [ChunkStructureData](https://github.com/xpple/SeedMapper/blob/master/src/main/java/dev/xpple/seedmapper/seedmap/ChunkStructureData.java) | Reusable tile/texture layers and cached deterministic generation information | Progressive screen-density terrain layers, per-column native-height reuse across tile boundaries and zoom levels, and shared in-flight preview requests. |
| [SeedMapper — StructureChecks](https://github.com/xpple/SeedMapper/blob/master/src/main/java/dev/xpple/seedmapper/feature/StructureChecks.java) | Stage cheap placement checks before more expensive biome/terrain validation; reuse results | Keep native placement-region enumeration, cache verified starts and empty results per source chunk, reuse them in Locate and memoize native noise samples within each search. |

VoxelMap source tree: `dab1a50a7ae789f2671313b30db9c2662fd8c9b5`. SeedMapper source tree: `3adf2e8be090c23c10aa3e09d7d9fb7bb682071e`.

## Bottlenecks found

- The first preview enlarged an 8×8 color grid to 32×32 with bilinear interpolation. Larger zoom steps amplified that blur. Every full preview queried native height for its entire output grid and border, including a second ocean-floor query under water.
- Preview generation shared one worker, did not combine identical pending requests, and the client discarded all preview textures whenever the map opened.
- Each rendered frame rebuilt and sorted visible tile lists. Below half-scale, recorded block pixels were replaced by chunk-average overview colors even when their detailed region textures were available.
- Any viewport change cancelled its active structure survey and discarded its completion set. Changing survey LOD repeated the same native chunk-start checks. Discoveries waited for a complete tile before being published.
- Each live snapshot scanned every saved marker and rebuilt/transmitted the complete modpack structure catalog.

## Implementation choices and bounds

Terrain first uses inexpensive native-biome layers, then a denser relief layer with independent biome colors per output pixel. Only height is interpolated. Relief uses a shared bounded cache of native surface/ocean-floor samples on an eight-texel grid. This retains sharp biome transitions without paying for every native height column. Textures explicitly use nearest filtering.

Fine preview density follows screen pixels with at most 256 tiles per layer. Initial coverage uses coarser 32×32 layers, not an enlarged/interpolated 8×8 image. Native detail stays capped at 384 chunks per viewport, two active generation futures and one admission per four ticks. Preview generation uses two workers, a 16-job queue, a 1,024-tile result LRU and at most 32,768 cached height samples. Identical requests share work while preserving separate session validation. Overloaded requests receive an explicit retry rather than a silent timeout.

The client retains up to 1,024 preview/detail textures per active world/dimension/layer and 192 full-resolution recorded region textures. Distant recorded regions use 2/4/8-block texel footprints with a separate 32 MiB native-image budget and an 8,192-tile cap; GPU copies use comparable additional memory. Mips are derived asynchronously from existing region PNGs and cached on disk against the source PNG timestamp. Live chunk captures update already visible mips and take precedence over older disk reads. Empty areas allocate no recorded-region textures. The former whole-viewport chunk-average texture is removed, including its per-pan CPU rebuild. World, dimension and ceiling-plane changes isolate the preview context; disconnect clears it. Recently viewed tiles survive a new map session, and shared exploration continues refreshing. Recorded PNG caches and saved world/pin formats stay compatible.

Automatic surveys retain overlapping active/completed tiles, cache up to 4,096 source-chunk results independently of survey LOD, and stream discoveries through a bounded queue. A dimension-separated spatial index limits live marker snapshots to relevant cells. The immutable structure catalog is cached for the session and omitted from routine updates; the client retains the catalog while replacing transient search state normally.

## Accuracy and compatibility

SeedMapper’s vanilla prediction path can use Cubiomes. Marauder’s Map must handle Tensura and other modded structures, so it continues using native generators. Automatic markers still require valid native starts created on disposable, unsaved ProtoChunks, including native weighted variants, frequency, biome and terrain rules. Existing saved starts take precedence. Locate still performs its final native STRUCTURE_STARTS verification; cached placement candidates never become unverified marker positions.

Unexplored terrain remains an estimate of native biome/surface generation. Features, buildings and exact block surfaces come from recorded exploration or the existing bounded native detail stage. Wide preview/survey work does not load distant terrain or write region files. The separate zoomed-in native detail stage can generate feature-stage chunks, as before.

The 100,000-block Locate range and its viewed-map-center origin are unchanged. Dimension tabs, private pins, shared exploration, player tracking, structure search/sorting, cancellation and native teleport handoff remain available.
