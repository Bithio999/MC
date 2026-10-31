# 1.1.3 validation

All **93 standalone native GameTests passed**, plus the normal production build. Myths is absent from this distribution.

The addon independently registers the new skill-taking tag and configuration and the native Creator-menu mixin. Seven new tests cover the Pride exception, target-skill copying/harvesting, operator overrides, actual reincarnation and Creator’s native temporary generation/replacement/renewal, with negative authorization and context-leak checks.

[Full implementation and validation report](../../../docs/VALIDATION-0.35.3.md) · [Policy and configuration guide](../../../docs/REINCARNATION-BLACKLIST.md) · [Default skill snapshot](../../../docs/research/FETTERED_REINCARNATION_0.35.3.json).

```sh
gradle build runGameTestServer --console=plain
python3 scripts/verify-distribution.py
```

The distribution audit verifies metadata, final asset/tag bytes, all common/client mixin classes, and the absence of Myths classes, bundled dependencies and opt-in review fixtures.
