# 1.1.2 validation

All **86 native GameTests passed**, along with the normal production build and distribution audit.

Standalone Heavenly Restriction includes the Fettered Human reincarnation blacklist without loading Myths. See the [shared implementation and verification report](../../../docs/VALIDATION-0.35.2.md), [operator configuration](../../../docs/REINCARNATION-BLACKLIST.md) and [default native skill snapshot](../../../docs/research/FETTERED_REINCARNATION_0.35.2.json).

```sh
gradle build runGameTestServer --console=plain
python3 scripts/verify-distribution.py
```

Seven new native tests cover the main/bonus pool, actual reincarnation, inline and configured costs, mastery/toggle variants, overrides, other races, retained skills and empty pools. The distribution audit verifies the new policy/config classes, native dependencies, common/client mixins, final assets, and absence of Myths classes, embedded dependencies and optional review fixtures.
