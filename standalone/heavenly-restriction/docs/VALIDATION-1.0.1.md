# Heavenly Restriction 1.0.1 validation

**All 45 required native GameTests passed** in the independent runtime, with Tensura Myths absent.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Broken Limit is **Passive — True — Mastered**. Owning mastered Chained supplies resistance/nullification degradation, attributable status degradation and dodge bypass regardless of toggle or slots. The native regression checks its pre-mastery boundary, off/unslotted behavior, attribute application, toggle cycling and full cleanup when forgotten. Martial Supremacy's separate attack/damage/buff/training bonuses keep their toggle requirement. Another caster retains normal nullification behavior.

The complete independent suite still covers five races/five skills/one visual entity, native reincarnation and fixed total-EP evolution, starting resources, 90/10 growth, aura capacity and magicule ceiling, inheritance, acquisition costs, recovery tiers, cooldown-free dash and aura-paid flight/cleanup.

The gameplay guide simplifies Heavenly Restriction into effect bullets and updates Broken Limit's activation description. The package audit validates 1.0.1 metadata/assets/mixins and excludes Myths classes, bundled dependency JARs and optional review fixtures. No rendering or layout changes occur; the successful 1.0.0 client review remains documented in its historical validation report and was not repeated for this gameplay patch.

Release: `releases/tensura-heavenly-restriction-1.0.1.jar` and its `.sha256` file in the repository root. Install on both client and server, using this addon instead of Tensura Myths.
