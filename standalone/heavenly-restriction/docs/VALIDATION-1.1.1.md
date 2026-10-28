# 1.1.1 validation

The independent Heavenly Restriction addon includes the same airborne dash, gold Unbound icon and native Fettered Human reincarnation panel as Myths 0.35.1. It requires no Myths classes or runtime. All **79 native GameTests passed**, along with the production build, distribution audit and real-client review.

See the [shared implementation and validation report](../../../docs/VALIDATION-0.35.1.md), [opt-in actual-player review](airborne-review/README.md), [race selection screenshot](media/fettered-human-selection-1.1.1.png) and [Ultimate menu screenshot](media/unbound-icon-1.1.1.png).

```sh
gradle build runGameTestServer --console=plain
python3 scripts/verify-distribution.py
```

The distribution audit verifies 64 × 64 icons, native metadata, common and client mixin classes, runtime asset bytes, and the absence of Myths, bundled dependencies and opt-in review fixtures. Use either this addon or Tensura Myths in an instance.
