# 0.34.5 validation — Purely Physical Severance Rush

All **281 required native GameTests** and **11 JUnit tests** passed. The independent Heavenly Restriction runtime also passed all **48 required native GameTests**, with Tensura Myths absent.

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

## Damage classification

- The actual incoming-damage event for a live dash reports `minecraft:mob_attack`, native Physical classification and no Battlewill classification.
- Native damage callbacks give a 10-damage dash **30 damage** with Chained’s toggle enabled, rather than 150. Toggle-off damage stays 10. Other Physical melee, projectiles, skills and converted damage retain the 3× bonus.
- Aura Sword and Ogre Guillotine cannot reclassify the dash as Battlewill, whether the helper receives its caster or a null attacker. Both buffs still classify ordinary Physical melee normally.
- Genuine Physical Battlewill sources retain 15× combined bonuses; non-Physical Battlewills retain 5×. The native Physical-or-Battlewill helper still accepts the dash through its ordinary Physical branch.
- The explicit dash exception in Chained’s 5× damage callback is removed. Its targeted native `isBattlewill` hook now returns false for the dash. The old forced `isPhysicalOrBattlewill` hook is removed so native Physical handling applies.
- Movement, aura cost, cooldown, base damage formula, regeneration, acquisition notices and all other prior regressions pass.

## Distribution

Release versions are **Tensura Myths 0.34.5** and **Heavenly Restriction 1.0.4**. Normal JAR builds and metadata/assets audits pass; the standalone JAR contains no Myths classes, bundled dependencies or optional client-review fixtures. Only version metadata changed after gameplay tests, followed by final packaging.

The wiki, ability directory and standalone guide show purely Physical damage and corrected examples: spending 2,000 aura deals about 67.08 path / 134.16 arrival base damage, or **201.25 / 402.49** with the 3× Physical toggle, before other native modifiers and defenses. All 162 HTML pages, 86 ability anchors and 9,640 local references validate; all 789 archive entries match their wiki sources. No visual assets changed and no graphical review was needed for this classification-only update.

Install the selected addon on both client and server. Release JARs, the offline wiki archive and SHA-256 checksums are included in `releases/`.
