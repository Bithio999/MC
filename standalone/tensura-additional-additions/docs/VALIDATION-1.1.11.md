# 1.1.11 — Regenerative Build

Schema’s new effect gives **+20% HP/SHP healing received** with **+5% aura/magicule costs** per stack. All three effects share the existing six-slot/mastery weights; drawbacks stay full strength. Existing saved trees remain valid.

Passed **145 standalone native GameTests**, **54 exact Neo Otherworld integration GameTests** and the full Myths counterpart’s **381 native GameTests**. Validation covers direct/native HP and SHP recovery, effective caps, canceled healing, native payment affordability and overloads, Chained’s positive bonus, manual dash/Counter bills, training and toggle/forget cleanup. The full client review passed, including the actual 10% resource surcharge and 22.5% HP/SHP healing bonus of a three-effect build. Partial-consuming APIs retain the correct funded fraction, including with empty reserves.

The exact pack and full client retain their original gameplay mods and overrides. The independent JAR excludes optional review fixtures.

[Full validation](../../../docs/VALIDATION-0.35.10.md) · [Player guide](../../../wiki/pages/Schema.md).

The full production build’s 11 JUnit checks passed, for 591 automated checks in total. Independent production builds, the asset/mixin/isolation audit, release hashes and the offline wiki bundle passed.
