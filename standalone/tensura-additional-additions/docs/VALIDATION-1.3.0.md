# 1.3.0 — Parallel World Jumper

A standalone-only Unique skill. World Hop uses a fresh-seed continuation: save/close the integrated server through Minecraft’s normal disconnect sequence, stage a new save without any dimension terrain, rewrite its world seed and player landing marker, then open it through vanilla world-loading flows. The preceding world remains untouched as the recovery copy. Its internal mod/registry IDs remain stable for existing standalone saves.

Character inventory/components, race, EP, skills and persistent progress carry over. Global saved data, player files, advancements, stats, datapacks and server configs are retained. Terrain/entities/POI and dimension-local world state regenerate. Stale map/raid/forced-chunk/random-sequence state is excluded, old bed bindings clear, and End fight state resets using the native codec. Generator presets and datapacks remain unchanged; flat generators remain flat. A saved sidecar keeps continuation names numbered without repeatedly appending the old suffix; native world serialization drops unknown level.dat fields.

Landing searches loaded chunks incrementally for nearby safe ground, rejecting water, hazardous supports and insufficient headroom. Nether searches stay below its roof. If nearby ground is unavailable, search around the new Overworld spawn; if that also fails, offer recovery to the preceding world. Temporary landing protection is restored on completion/logout. Saved landing markers support resuming after interruption and identify the preceding world for recovery after a restart.

## Restrictions and compatibility

- Only the private integrated-server owner with no other players can request a hop. Dedicated servers, published LAN worlds, guests and extra-player sessions are rejected.
- Multiplayer reincarnation rolls, acquisition, unfinished learning and copy/update paths reject this skill. Already carried skills remain inert in multiplayer.
- The client accepts only a one-use ticket from its own integrated server. Remote packets cannot nominate save paths or regenerate files.
- Neo Otherworld’s Starter Kit tracks single-player first joins by save-folder name. Carry an already-claimed kit record to the continuation name, preventing a new starter kit from replacing inventory. Unclaimed kit state and multiplayer records are left unchanged; no pack settings are changed.
- File-copy failures remove only the staging directory and leave the source intact. Linked/unsupported save files fail safely rather than copying outside the save.

## Validation

- **16 production JUnit tests passed**, including file-copy/failure/recovery, Starter Kit history and searchable guide content.
- **152 native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209, including independent registration of five races, nine skills and one visual entity.
- **61 exact-pack integration tests passed** on Neo Otherworld 1.1.3a / Tensura 2.0.1.4 / NeoForge 21.1.248. The existing audit of 39 additional third-party reincarnation skills still passes; the standalone’s own new skill is checked separately.
- The actual modpack client completed two consecutive hops: seed `0` → `4951447663744991757` → `8325000690645357723`. Both retained 1,000,000 EP, race, permanent skills/mastery, named sword, 37 diamonds and foreign saved progress, with safe landings and both preceding worlds present. Published LAN rejected acquisition/learning/rolling/transfer.
- The final code completed another real hop: seed `0` → `3464544689503797360`, preserving the same character state and landing at `(24.5, 72, 32.5)`. The final native suite also passed again; three focused World Hop native checks passed after the final history/naming update. Tests used copied disposable noise-generator worlds, not a player save.
- A clean production build and distribution audit verify metadata, nine registered skills, the ten-page guide, unique classification, assets, both supported dependency ranges and exclusion of optional review code.

The skill uses Tensura’s native Spatial Motion icon. It does no damage, has no portal mode and spends no aura or magicules. The full Tensura Myths release is unchanged.
