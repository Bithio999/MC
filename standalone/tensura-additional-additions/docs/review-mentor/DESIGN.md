# Archive construction and art direction

References:

- [Minecraft Wiki — Roof types](https://minecraft.wiki/w/Tutorials/Roof_types): overhangs, gables, lean-to additions and tower crowns.
- [Minecraft Wiki — Adding beauty to constructions](https://minecraft.wiki/w/Tutorials/Adding_beauty_to_constructions): recessed windows, consistent palettes, wall relief and deliberate structural framing.

The hall has grounded projecting posts, connected horizontal beams, recessed glazing, straight gable king posts and paired clerestories. A grounded rear chimney passes through the roof and breaks up the rear elevation. No repeating diagonal log pattern or exterior trapdoors are used. Posts and lintels stop beneath the slate roof; the authoring script checks the roof edges for accidental penetrations.

The reading bay is a closed room with fully infilled end walls and a continuous copper lean-to. Its slope alternates full blocks and bottom slabs with a continuous timber underside. The roof meets the hall wall below its eave. A framed three-block-high doorway crosses a continuous floor; shelves are omitted from that route. The tower has three complete floors, a closed ceiling and a supported hanging light on each floor.

Every hanging lantern has a continuous chain to a ceiling or beam. Standing lanterns rest on bookshelves. The central chandelier uses a suspended cross-arm frame with four short lantern chains. Generator checks reject unsupported lights, unanchored chains, exterior trapdoors, missing bay roof/end-wall blocks and framing through the gable roof.

Elias retains Kael's established classic 64×64 otherworlder pixel style. His texture is unchanged by this construction update. The skin and structure have separate authoring scripts.

## Visual review

From the standalone project, use Java 21 and `runClient -I docs/review-mentor-art/client.init.gradle`. This opt-in harness requires the disposable `Archive Review 3` save in `/workspace/validation-mentor/client`, refuses non-review save paths, and leaves the redemption ledger alone. It places the actual template on level terrain, removes trees only in the staging border outside the template, and checks camera arrival before capturing exterior, rear, hall, bay and all three tower rooms. Tower light levels are checked at night. The development LWJGL stack override and all review classes are excluded from production.
