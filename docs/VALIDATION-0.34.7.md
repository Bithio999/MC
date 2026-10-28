# 0.34.7 validation — Exclusive damage bonuses

All **286 required native GameTests** and **11 JUnit tests** passed. The independent Heavenly Restriction runtime passed all **53 required native GameTests**, with Tensura Myths absent.

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

## Native damage pipeline

- Physical Battlewill ability sources receive **5×**, including explicitly Physical-converted sources. Other Physical melee, projectiles, skills and converted damage receive **3×**. No hit receives both bonuses.
- Elemental Battlewill sources retain 5×. Unrelated elemental damage and other attackers receive no Chained bonus. Disabling the toggle removes both damage branches. The attack attribute remains unchanged and the legacy attack-stat modifier is cleaned up.
- Actual native Aura Slash casts with a diamond sword spawn projectiles carrying the native Battlewill ability. Both normal and mastered casts are captured from the entity-join event; their actual 7 / 14 projectile damage is dispatched through the native damage callback and produces **35 / 70**, with Aura Sword active. Toggle-off checks restore the original 7 / 14.
- A native Aura Sword effect still classifies ordinary melee as Battlewill for Tensura’s broader helper, but Chained now checks the source’s actual ability class, like Mysticism. A controlled 7-damage ordinary hit plus the default native 7-damage Sword increment receives **3×**, giving **42**. The added increment is not tripled separately. Aura Sword’s native level/duration and other Battlewill buff behavior are preserved.
- Chained’s dash stays Physical-only and receives 3×. All existing upward/enemy targeting, cost, cooldown, flight, race, growth, learning, inheritance and boss regressions pass.

The [implementation reference](research/CHAINED_DAMAGE_BRANCHES_0.34.7.md) distinguishes the copied Mysticism branch structure/source check from Chained’s retained 3× other-Physical rule. Mysticism’s Physical branch adds attack-stat damage instead and is restricted to direct hits; this update does not copy those unrelated balance rules.

## Packaging and documentation

Release versions: **Tensura Myths 0.34.7** and standalone **Heavenly Restriction 1.0.6**. Both JARs match the compiled production classes and have correct metadata. The independent addon passes its dependency/assets audit and contains no Myths classes, dependency JARs or optional client review fixtures. Checksums accompany both JARs and the offline wiki archive.

The in-game description, wiki, ability information, prepared GitHub mirror and standalone guide explain exclusive branches and the 35/70 Aura Slash and controlled 42-damage melee examples. All 162 HTML pages, 86 ability anchors and 9,640 local references validate; all 789 archive entries match the wiki sources.

This update changes server-side damage branching and documentation. Movement and rendering are unchanged; no additional graphical run was necessary after the prior real-client targeting verification. Install the selected addon on client and server.
