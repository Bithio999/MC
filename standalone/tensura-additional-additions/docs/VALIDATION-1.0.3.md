# Heavenly Restriction 1.0.3 validation

**All 48 required native GameTests passed**, with Tensura Myths absent from the independent runtime.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Severance Rush emits Physical damage using `minecraft:mob_attack`, like Martial Master. It remains a Battlewill move with the existing formula, targeting, 2% aura cost and 1-second cooldown. An actual incoming-damage event verifies its live source.

The toggle now triples **all outgoing native Physical damage**, covering melee, physical projectiles, Physical skills and converted damage. Native callbacks verify 15× for the Physical Battlewill dash, 5× for non-Physical Battlewills, and no bonus for unrelated elemental damage, other attackers or toggle-off hits. The attack attribute itself is unchanged; the old saved modifier is cleaned up. Aura Sword's numeric buff and damage pipeline are checked together.

Actual learn/update/latent rejection paths provide a named, translated message before resource costs. Repeated identical notices are throttled, allowed skills remain learnable and no rejected ability enters storage. The real client renders the red rejection for Musician without acquiring it or spending resources. [Screenshot](media/chained-block-message.png).

All prior standalone race, resource, inheritance, regeneration, movement, recovery and flight regressions remain covered. The real player's native dash comparison, replicated landing and cooldown/recast review also pass. Current guide damage types, stacked examples and notification behavior are updated. Normal release builds exclude the optional review fixture; the package metadata/assets audit passes.

Release: `releases/tensura-heavenly-restriction-1.0.3.jar` and its checksum in the repository root. Install on client and server instead of Tensura Myths.
