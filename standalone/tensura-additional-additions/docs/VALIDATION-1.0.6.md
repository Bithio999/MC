# Heavenly Restriction 1.0.6 validation

All **53 required native GameTests passed**, with Tensura Myths absent from the independent runtime.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Chained now uses exclusive branches: sources carrying a Battlewill ability receive **5×**; other Physical sources receive **3×**. Physical projectiles and skills remain covered by the latter branch, and the dash stays Physical-only. Ordinary melee under Aura Sword/Ogre Guillotine uses the Physical branch; those buffs alone do not attach a Battlewill ability to its source.

Real native diamond-sword Aura Slash casts, captured at projectile creation, give **35 normally / 70 mastered** through Tensura’s damage callback, including while Aura Sword is active. With Chained off they return to 7 / 14. A controlled ordinary 7-damage hit plus native Aura Sword’s +7 receives 3× for **42**; Sword’s added increment is not independently tripled. Other numeric Battlewill buffs retain their existing enhancement.

Regression coverage includes explicit Physical conversion, elemental Battlewills, unrelated damage, other attackers, disabled toggle, attack-stat cleanup and all prior race, movement, aura recovery, skill acquisition and flight checks. This damage-only update does not change targeting or rendering; the prior client targeting verification remains applicable.

The JAR metadata/assets audit passes and excludes Myths classes, bundled dependencies and optional client fixtures. [Full validation](../../../docs/VALIDATION-0.34.7.md) · [branch reference](../../../docs/research/CHAINED_DAMAGE_BRANCHES_0.34.7.md).

Release: `releases/tensura-heavenly-restriction-1.0.6.jar` and its checksum in the repository root. Install on client and server instead of Tensura Myths.
