# Archive art direction

References consulted:

- [Minecraft Wiki — Roof types](https://minecraft.wiki/w/Tutorials/Roof_types): roof hierarchy, gables, lean-to additions, odd-width tower crowns and overhangs.
- [Minecraft Wiki — Adding beauty to constructions](https://minecraft.wiki/w/Tutorials/Adding_beauty_to_constructions): depth around windows, deliberate material palettes, weathering near the plinth, varied room heights and stair brackets between columns and ceilings.

Application: the main reading hall now has a steep slate gable, a lower gabled entrance, a chamfered copper-crowned tower and a low reading bay. Timber bays project beyond inset glazing; sills, shutters, brackets and a contrasting masonry plinth provide depth. Spruce floors and pale plaster keep the interior readable against dark framing. Weathering collects near the ground rather than covering every wall in random noise. The gallery has a real staircase, linked tower floors, connected railings, open circulation and a suspended chandelier.

Elias uses **Kael's existing classic 64×64 otherworlder skin as the UV and shading reference**. His face keeps the same sparse pixel vocabulary and eye placement, with silver hair, a short beard and teal/ivory tailoring. The rejected high-resolution generated atlas is not used or packaged. `scripts/build-elias-skin.py` rebuilds the functional native-sized texture; `scripts/build-mentor-archive.py` authors the structure independently.

## Reproducing the visual review

Use Java 21 and the standalone project's opt-in `docs/review-mentor-art/client.init.gradle` with `runClient`. It requires the disposable `Archive Review 3` save in `/workspace/validation-mentor/client`; it refuses saves without `Archive Review` in their path. The harness chooses level generated terrain, places the actual template and native NPC, waits for the camera to arrive, and captures exterior, interior, gallery and skin views. It does not purchase a lesson or reset the redemption ledger. The fixture's larger LWJGL stack is local to this development run. All review classes are excluded from the production JAR.
