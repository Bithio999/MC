# 0.34.2 validation — Broken Limit true passive

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

**All 278 required native GameTests and 11 JUnit tests passed.** The independent standalone build also passed its 45 required native tests.

Broken Limit now follows ownership of mastered Chained. It applies resistance/nullification degradation, attributable status degradation and dodge bypass with the skill toggled off and unequipped. Its native degradation/dodge-negation attributes use that same condition. Martial Supremacy's damage, numerical buff, attack and training bonuses still depend on its combat toggle.

The updated native regressions verify that Broken Limit is absent before mastery, works while off and unslotted, survives an on/off toggle cycle, preserves nullification's lower resistance tier, and removes both live bypasses and attributes when Chained is forgotten. Status tests verify the mastered off-toggle caster while preserving another caster's normal interaction with nullification. Existing combat, growth, race, recovery and flight checks retain their coverage.

The in-game description, wiki ability classification and standalone guide now say **Passive — True — Mastered**. Heavenly Restriction's wiki description is condensed into five effect bullets plus short starting-resource/EP notes. Repeated lineage trait descriptions are shortened. The styled offline wiki and prepared GitHub Wiki mirror are regenerated. No client geometry, textures or menu layouts change, so the graphical client was not rerun for this patch.

Release: `releases/tensura-myths-0.34.2.jar`, `releases/tensura-myths-wiki-0.34.2.zip`, and `releases/tensura-myths-0.34.2.sha256`.

Wiki verification: 162 HTML pages, 86 ability anchors and 9,638 valid local references; all 789 archived files match the wiki. Broken Limit’s index/search classification is explicitly verified as `Passive - True - Mastered`. Both release JARs report their correct versions and revised skill descriptions.
