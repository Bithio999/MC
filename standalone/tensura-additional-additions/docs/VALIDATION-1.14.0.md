# 1.14.0 — 100,000-block Locate and native spatial travel

## Native integration checked

The implementation references Tensura 2.0.1.3’s `ISpatialMovement`, `SpatialMovementScreen`, `SpatialMotionSkill` and `RequestSpatialActionPacket`, plus Minecraft 1.21.1’s native structure placement, generation context and chunk scheduling. The addon’s Instant Transmission already implements the same native coordinate interface and intercepts its native warp request for aura billing. The new map path preserves that handler.

## Search

Locate now uses an exact 100,000-block radius around the map center, including recorded-marker lookups and final verified results. The old 128-candidate stopping limit is removed.

Random-spread structures are enumerated by placement region, rather than checking every empty chunk in a 12,501×12,501 square. Native concentric-ring positions handle strongholds; custom placement types have a constant-memory chunk-spiral fallback. A single background worker checks native placement/frequency/exclusion rules and native valid generation points, including biome checks. Its input queue holds eight tasks and its candidate handoff holds two positions. No seed is sent to clients. Impossible biome sets are rejected early; rare valid types can take longer and remain cancellable.

The server verifies actual structure starts with native asynchronous scheduling and the existing global start-generation budget. Temporary structure-stage tickets are held while needed and released on result, cancellation, session close, dimension change or shutdown. The worker’s ring-future wait stays off the server tick. Template/height/biome prechecks do not request terrain chunks. The final result comes from a genuine native structure start, including its actual bounding box, within the requested radius.

## Travel

The map lists only owned, learned and currently usable `ISpatialMovement` skills that permit coordinate warping. Cooldown, skill interaction/activation, dimension blacklist and teleport/interaction restrictions are checked. Selection and ownership are rechecked on the server; submitted destination IDs must refer to a known structure in the current dimension. Forged IDs, unowned skills and repeated arrival jobs are rejected.

At most two arrival jobs are active. Native feature-stage scheduling prepares the destination asynchronously under a temporary non-ticking ticket. Landing suggestions use actual terrain, sturdy ground, fluid/hazard checks and two blocks of body clearance; roofed dimensions search below the roof. Collision checks use the prepared chunk, rather than synchronously fetching distant full chunks. Underground structures use a surface approach. Preparing the route does not move the player or spend aura/magicules.

The client then opens a subclass of the actual native `SpatialMovementScreen`, prefilling XYZ and dimension once and retaining edits across resize. Native Warp confirmation sends the original native request packet and uses the chosen skill’s normal billing, charge, mastery, cooldown, capabilities and restrictions. No new map teleport implementation replaces those rules. A successful handoff closes the old atlas session and cancels its pending work.

## Verification

All **268 required native GameTests** passed, including new cases for the 100,000-block radius and outside-range rejection, cancellable background search, learned/owned/cooldown/seal filtering, forged destinations and duplicate jobs, clear native arrival coordinates without resource charges or movement, and cancelling handoffs when a skill is forgotten. Geometry/unit coverage additionally checks complete, nonduplicated center-first spirals, negative coordinates, reduced region counts and overflow bounds.

The integrated client passed the existing terrain/player/pin/structure/search/size review and the new travel review. From map center **70,000 / −70,000**, an actual native stronghold search returned a real start centered at **16,365 / −15,938**, **76,153.88 blocks** away. The native worker checked four candidates; no recorded stronghold shortcut supplied the result.

The review checked both offered skills, the prefilled native **Spatial Motion** and **Instant Transmission** menus, unchanged character position before confirmation, and actual teleportation after activating the native Warp button with Enter. The suggested surface arrival was **16,365.5 / 93 / −15,937.5**. Mastered Instant Transmission used its normal aura-based native request path. This is an integrated-client test with the real server packet handler, not a simulated position change. Screenshots in the wiki are actual runtime captures. The targeted rerun is available with `-PmapTravelOnly=true`; it repeats discovery and both menu handoffs without repeating unrelated review scenes.

Compilation passed against the optional **Tensura 2.0.1.4 / NeoForge 21.1.248 API profile**. This is API compatibility coverage, not a full Neo Otherworld modpack runtime rerun.

The final clean baseline build and **40 JVM tests** passed. The independent-distribution audit verified 1.14.0 metadata, search/travel classes, native dependencies, all required mixins/guide assets, and the exclusion of client review fixtures, test presets, Myths code and embedded dependency jars.
