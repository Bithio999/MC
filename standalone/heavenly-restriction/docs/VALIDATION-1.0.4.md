# Heavenly Restriction 1.0.4 validation

All **48 required native GameTests passed**, with Tensura Myths absent from the independent runtime.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Severance Rush is purely **Physical** (`minecraft:mob_attack`). A live incoming-damage event verifies its source is Physical and not Battlewill. The native damage callback now turns a 10-damage dash into **30** while Chained’s combat toggle is enabled, instead of 150. The raw formula, teleport, targeting, 2% current-aura cost and 1-second cooldown are unchanged.

Native Aura Sword and Ogre Guillotine cannot reclassify the dash as Battlewill, while their classification of normal Physical melee remains intact. Genuine Battlewill sources keep their separate 5× bonus and Physical Battlewills retain both bonuses. The dash still passes native Physical handling without a forced Physical-or-Battlewill override. Toggle-off hits, other attackers and unrelated elemental damage retain their prior behavior.

All prior standalone resource, race, inheritance, acquisition-message, recovery and flight regressions pass. The release JAR metadata and asset audit pass and exclude Myths classes, nested dependencies and the optional client fixture. Only version metadata changed after gameplay testing, followed by final packaging. No visual assets changed.

Release: `releases/tensura-heavenly-restriction-1.0.4.jar` and its checksum in the repository root. Install on client and server instead of Tensura Myths.
