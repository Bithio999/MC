# 1.1.4 validation

All **96 standalone native GameTests passed**, along with the normal production build and independent distribution audit.

The standalone distribution implements automatic Chained → Unbound evolution with the same prerequisites and native completion callbacks as Tensura Myths. It includes saved unfinished-entry migration, idempotence, resource/preset/cooldown preservation, rejection of borrowed predecessors, and a live unslotted, untoggled server-tick test.

[Full implementation and validation report](../../../docs/VALIDATION-0.35.4.md).

```sh
gradle build runGameTestServer --console=plain
python3 scripts/verify-distribution.py
```

The distribution audit verifies metadata, asset/tag bytes, all common/client mixin classes and the absence of Myths classes, bundled dependencies and opt-in review fixtures.
