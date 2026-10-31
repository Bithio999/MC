# Heavenly Restriction 1.0.5 validation

All **52 required native GameTests passed**, with Tensura Myths absent from the independent runtime.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Aura Sword retains its native damage bonus and no longer receives Chained’s 3× numerical buff bonus. A controlled diamond sword reference with base hit 7 gives `(7 + 7) × 15 = 210` while Chained is active and native 14 when it is off. Applicable outgoing Physical/Battlewill multipliers remain. Other Battlewill buff regressions still pass.

New movement tests cover level and six upward angles, nearest-enemy stopping and damage, body clearance, downward target aiming, allies/off-axis targets and wall occlusion. All earlier race, resource, skill-acquisition, aura recovery, native targeting, Physical classification, cooldown and flight checks pass. Tests wait for native entity tracking and take aura measurements at cast time so passive regeneration does not contaminate the cost check.

The isolated real-player client review also passes the seven air angles, 200-aura payment from 10,000, 1-second cooldown, enemy-stop damage and replicated landing. [Captured stop](media/chained-enemy-stop.png). Run it in **Restriction Validation** with `gradle runClient -I docs/targeting-review/client.init.gradle`. Optional review sources are excluded from normal builds; distribution auditing checks this explicitly. No Myths classes or dependency JARs are bundled.

See [full validation](../../../docs/VALIDATION-0.34.6.md) and [Mysticism Restricted comparison](../../../docs/research/RESTRICTED_DAMAGE_0.34.6.md). This research is static inspection, not an interoperability test.

Release: `releases/tensura-heavenly-restriction-1.0.5.jar` and its checksum in the repository root. Install on client and server instead of Tensura Myths.
