# Heavenly Restriction 1.1.0 validation

All **77 native GameTests passed**, covering the independent runtime with Myths absent. The opt-in real-player review verifies native Ultimate-menu evolution, retained pact data, visible unfinished blocked skills, synchronized Counter state and actual damage negation through a real player.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
# Optional, in the isolated Restriction Validation world:
./gradlew runClient -I docs/unbound-review/client.init.gradle
```

Fourteen new gameplay checks cover Air Walk’s half-cost mastered activation/upkeep; discoverable unfinished skills and blocked final learning; the three evolution gates and loss-of-gate failure; native-menu replacement preserving mastery, toggle, resources, inheritance, dash cooldown and nine presets; exclusive 8× / 5× damage; live 4× numeric Battlewill buffs; 10% / 30% in-slot reduction and native bypass rules; 50% ordinary dodge and projectile/bypass behavior; actual melee/projectile criticals applied once; Counter entry, cancellation, timeout, cleanup, hostile negation, Physical damage/formula and terrain-piercing multi-target hits. Direct native SHP attacks are also checked: they critically hit once and cannot bypass an active Counter. All previous resource, growth, movement, flight, learning and recovery tests remain.

The client observed both 1,024-HP enemies behind glass reduced to approximately **448.75 HP** after Counter, while the real player’s HP/SHP stayed unchanged. The client confirmed the active stance and its cooldown-cancel permission, followed by movement restoration and stance cleanup.

The normal JAR adds exactly **five races, six skills and one non-damaging visual entity**. Metadata, every required mixin class, production classes and 64-pixel icons are checked. No Myths classes, bundled dependencies or optional review fixtures are distributed. [Full report](../../../docs/VALIDATION-0.35.0.md) · [Native integration](../../../docs/research/CHAINED_ULTIMATE_0.35.0.md).

Release: `releases/tensura-heavenly-restriction-1.1.0.jar` and checksum in the repository root. Install on client and server instead of Tensura Myths.
