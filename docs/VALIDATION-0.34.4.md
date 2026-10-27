# 0.34.4 validation — Physical damage and Chained rejection messages

**All 281 required native GameTests and all 11 JUnit tests passed.** The independent standalone build also passed all **48 required native GameTests**, with Tensura Myths absent.

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

The [implementation reference](research/CHAINED_PHYSICAL_0.34.4.md) records native damage classification and why rejected learning needs an explicit notification.

## Gameplay

- An actual Severance Rush is observed through the native incoming-damage event and emits `minecraft:mob_attack`, classified as Physical. Its existing Battlewill classification, targeting, cost, cooldown and formula remain covered.
- Native damage callbacks verify 3× outgoing Physical melee, projectiles, Physical skills and explicitly Physical-converted damage; 15× on the Physical Battlewill dash; 5× on non-Physical Battlewills; and no bonus for unrelated elemental damage or another attacker. Toggle-off cleanup is checked.
- A simulated old attack-stat modifier is removed without changing the underlying attack attribute. Native Aura Sword regression verifies its separate numeric buff, Battlewill bonus and Physical bonus together.
- A recording native player verifies named notices through actual learn/update/latent rejection paths, throttled identical attempts, the unlock event reason, absence of resource charges and preserved allowed acquisition.
- All prior race, growth, mastery, regeneration, inheritance, movement, flight and boss regressions pass.

## Client and documentation

The isolated standalone client successfully receives and renders the translated red **“Chained blocks Musician…”** message. The forbidden skill stays unlearned with unchanged aura and maximum magicules. Its real-player dash, client landing synchronization and cooldown/recast checks also pass. [Client screenshot](../standalone/heavenly-restriction/docs/media/chained-block-message.png).

The wiki/guide now show Physical damage, both stacked bonuses, corrected examples and unmultiplied racial attack-stat values. The styled edition and prepared GitHub Wiki mirror are rebuilt. Rendering geometry is unchanged. The normal standalone JAR audit excludes the optional review fixture and dependency JARs.

Release: `releases/tensura-myths-0.34.4.jar`, `releases/tensura-myths-wiki-0.34.4.zip` and checksums; standalone `releases/tensura-heavenly-restriction-1.0.3.jar` and checksum. Install the appropriate addon on client and server.

Distribution checks: 162 HTML pages, 86 ability anchors, 9,640 valid local references and all 789 archive entries verified. Both JARs have correct version metadata, Physical source/classification code, localized rejection text and no optional client review fixture. Checksums accompany both distributions.
