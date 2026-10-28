# 0.34.6 validation — Aura Sword and Chained dash targeting

All **285 required native GameTests** and **11 JUnit tests** passed. The independent Heavenly Restriction runtime passed all **52 required native GameTests**, with Tensura Myths absent.

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

## Aura Sword

Chained no longer triples Aura Sword’s numeric damage increment as a Battlewill buff. A native callback test equips the diamond sword and applies its actual item modifiers to the simulated player, then checks its 7-damage weapon reference. Aura Sword retains the native configured increment; applicable outgoing Physical/Battlewill multipliers remain. With a 7-damage starting hit and default native Aura Sword coefficient 1, the callback gives `(7 + 7) × 15 = 210`, rather than `(7 + 3 × 7) × 15 = 420`. Toggle-off restores the native 14 for this controlled reference. These examples exclude racial attack bonuses and other modifiers.

The effect’s native level and duration are preserved. Existing Diamond Path tests verify other numeric Battlewill buffs still receive their 3× bonus and do not compound during refresh.

## Dash

- Seven level/upward air angles (0°, 1°, 3°, 5°, 15°, 45° and 80° upward) cast successfully at the full 16-block look displacement, each paying 2% current aura once and setting the 1-second cooldown.
- Live dash tests select the nearest enemy on the eye ray, stop before its body without overlap, apply path/arrival damage to that target and leave the more distant enemy outside the truncated route unharmed.
- Downward enemy aim lands at the target’s feet instead of inside the floor. Allies and off-axis enemies do not capture aim; block geometry occludes targets behind walls.
- Native Martial Master comparisons at downward block-hit angles still match. Prior fluid, blocked destination, airborne, cost/cooldown, Physical-only damage and repeat-cast tests pass.

The issue came from applying a block face offset to a ray MISS. Air aim now uses the look displacement relative to the caster’s feet. Actual block hits retain native adjacent-cell logic. Enemy stops use body clearance and a safe landing check; air/enemy landings can back off by up to one block for tight corners, never backward through the caster’s origin. Unsafe landings remain free.

## Real client

The [opt-in targeting fixture](../standalone/heavenly-restriction/docs/targeting-review/ChainedTargetingReview.java) runs only in the isolated **Restriction Validation** world. It is excluded from production source sets and JARs.

```sh
# From standalone/heavenly-restriction, on an available graphical display:
gradle runClient -I docs/targeting-review/client.init.gradle
```

The actual server player passed all seven air angles, then stopped at `(7.75, 71, 0.5)` before a target at `(8.5, 71, 0.5)`. The caster’s body did not overlap the victim. The near target lost HP, the far target retained 1,024 HP, aura became exactly 9,800 from 10,000, and cooldown was 1 second. Client position replication matched the server landing. Myths was absent from this independent run. The fixture removes its two test mobs before exit. [Captured landing](../standalone/heavenly-restriction/docs/media/chained-enemy-stop.png).

## Mysticism research and packages

[Restricted damage research](research/RESTRICTED_DAMAGE_0.34.6.md) records inspected Mysticism **2.2.1** code and defaults. Its `if / else if` branches do not multiply Battlewill and Physical bonuses together. Chained’s separate 5× Battlewill and 3× all-Physical bonuses are retained; this update removes only Aura Sword’s extra buff multiplier. Mysticism was inspected, not installed for interoperability testing.

Normal release JARs, dependency/version metadata, extracted assets and exclusion of all optional client fixtures are audited. The wiki, prepared GitHub mirror, offline archive and standalone guide are updated; all local references and ability anchors validate. SHA-256 checksums accompany Myths **0.34.6** and standalone **1.0.5**. Install the chosen addon on client and server.
