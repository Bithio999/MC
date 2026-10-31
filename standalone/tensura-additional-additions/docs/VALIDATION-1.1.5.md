# 1.1.5 validation

All **107 standalone native GameTests passed**, plus the final production build and distribution audit. The full Myths mod is absent from this distribution.

Eleven new tests cover Chained/Unbound in-slot status immunity, normal/Sacred/direct Haki, exact Hihiirokane weapon attributes, built-in native engraving, Sharpness and Crushing, custom weapon modifiers, actual dash/Counter weapon damage and Fire Aspect, native melee barrier/status/seal behavior, ranged/boss barrier shattering, unrelated spell exclusion, native skill activation and 40-tick expiry, and actual ominous/ordinary vault loot tables.

[Full implementation and validation report](../../../docs/VALIDATION-0.35.5.md) · [Gameplay guide](GUIDE.md).

```sh
gradle build runGameTestServer --console=plain
python3 scripts/verify-distribution.py
```
