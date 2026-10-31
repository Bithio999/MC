# Heavenly Restriction 1.0.7 validation

All **58 required native GameTests passed**, with Tensura Myths absent from the independent runtime.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Five new native tests cover the reported no-dummy birch canopy/trunk failure, slight upward aim under a low ceiling, slab/full-block support, short enemy stops and exact close-wall contact. Preferred blocked destinations now retry actual standing heights and closer positions, while body clearance and an unobstructed eye path prevent crossing solid geometry. The quarter-block minimum no longer rejects valid short forward movement.

Successful native downward landings, upward air aim, enemy selection, wall/ally handling, water, truly blocked free rejection, 2% aura payment and 1-second cooldown all pass. The truly blocked fixture now places the body against the wall; a centered body still has room for a legal short dash. Physical classification, exclusive damage bonuses and all earlier resource/race/learning/flight checks remain unchanged and pass.

The real-player client review also passes forest, low-ceiling, slab, full-block and close-wall scenes before its normal air-angle/enemy checks. In the no-dummy forest it lands at `(11.5, 71, 0.5)` with the body clear of leaves, paying 200 aura and setting a 1-second cooldown. Enemy-stop position replication reaches the client correctly. Run with `gradle runClient -I docs/targeting-review/client.init.gradle` in the isolated **Restriction Validation** world.

Normal packaging excludes review fixtures, Myths classes and bundled dependencies; version/assets auditing passes. [Full validation](../../../docs/VALIDATION-0.34.8.md) · [terrain reference](../../../docs/research/CHAINED_TERRAIN_0.34.8.md).

Release: `releases/tensura-heavenly-restriction-1.0.7.jar` and checksum in the repository root. Install on client and server instead of Tensura Myths.
