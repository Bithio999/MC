# 0.35.0 validation — Unbound and Air Walk

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

All **398 tests passed**: **310 full-mod native GameTests**, **11 JUnit tests**, and **77 independent standalone native GameTests**. The opt-in real-player client review runs with Myths absent. [Implementation references](research/CHAINED_ULTIMATE_0.35.0.md).

```sh
gradle build runGameTestServer --console=plain
# Also run build/runGameTestServer from standalone/heavenly-restriction.
python standalone/heavenly-restriction/scripts/verify-distribution.py
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

## Gameplay coverage

- Air Walk retains its native Battlewill tag, permanent Air Flight prerequisite, learning progression and saved registry identity. Native activation and one-second flying upkeep both pay half aura once mastered. Stopping remains free.
- Unfinished restricted skills enter native storage and can gain learning points. A blocked final learning step retains the unfinished entry and prior progress without billing permanent acquisition MP. Updating an unfinished entry to a learned one cannot bypass the gate. Concise notices retain the blocked name’s native tier color and spam throttle. Existing inherited, Battlewill, resistance/nullification and racial recovery grants remain functional.
- Unbound discovery checks Heavenbreaker, mastered permanent Chained and both alternative awakening statuses. Removing a gate after discovery prevents completion and leaves Chained intact. Activation through native `startHoldSkill` replaces Chained and preserves completed mastery, toggle, all nine presets, dash cooldown, resource ceiling, birth exemption and inheritance, including an owner whose maximum magicules have fallen to zero.
- Exclusive damage branches upgrade to 8× Battlewill / 5× other Physical. Live tracked native Battlewill buffs change from 3× to 4× without compounding, restoring normal values when disabled. Aura Sword stays excluded from numeric buff amplification.
- Unerring Body supplies 10% / 30% in-slot reduction, with native Strengthen Body bypass rules. Ultimate dodge is checked at 50% ordinary damage while preserving guaranteed projectile dodge and explicit bypasses. Unfinished or unequipped entries do not grant defenses.
- Native `Player.attack` and owned projectile damage verify exactly one critical multiplier. Scoped native-melee handling does not leak into later sources, and true criticals work with Martial Supremacy off.
- Counter entry pays 10% current aura once, roots speed, starts its 10-second cooldown and permits a free early cancellation. Three-second timeout and forgetting restore movement. Environmental, self and operator/void sources do not trigger it. A native enemy damage event is negated; the Physical slash hits multiple enemies through terrain once each with the recorded aura/formula and independent hit handling. Out-of-radius targets stay untouched.
- Direct native SHP attacks critically hit once and trigger Counter without leaking spiritual damage.
- Prior resource, terrain dash, flight, race, secondary progression, boss and skill regressions remain in the suite.

Tests use isolated native arenas. Bypass checks create a fresh damage source rather than modifying Minecraft’s shared cached generic source, keeping later barrier/regeneration tests independent.

## Real client

The optional [Unbound review](../standalone/heavenly-restriction/docs/unbound-review/UnboundReview.java) runs in **Restriction Validation**, with no Myths runtime. It exercises native learning/evolution, the Ultimate list and description, a visible blocked learning entry, server/client stance state and free cancellation availability during cooldown.

A real player takes an actual 20-damage hostile hit while in Counter. HP and SHP stay unchanged; both 1,024-HP enemies behind a glass wall take the slash, reaching about **448.75 HP** with the review’s disabled Martial Supremacy toggle and normal native armor/critical handling. The client then sees the stance end and movement return. This supplements the server fake-player tests, whose native invulnerability otherwise prevents real incoming hits.

```sh
# From standalone/heavenly-restriction with a graphical display:
gradle runClient -I docs/unbound-review/client.init.gradle
```

[Native Ultimate menu](../standalone/heavenly-restriction/docs/media/unbound-menu.png) · [Counter stance](../standalone/heavenly-restriction/docs/media/unbound-counter-stance.png) · [Counter slash and hit feedback](../standalone/heavenly-restriction/docs/media/unbound-counter-slash.png).

The review fixture is excluded from production packages. Normal builds afterward verify production-class bytes, native metadata, declared mixin classes, assets and fixture exclusions. Both JARs and the offline wiki receive SHA-256 files. The current wiki includes Unbound’s per-ability damage type and formula, Air Walk’s mastery costs, and the concise acquisition behavior.
