# 0.35.6 / standalone 1.1.7 validation — Spiritual contact and spear silhouette

Targets: Java 21, Minecraft 1.21.1, Tensura 2.0.1.3 / NeoForge 21.1.209 for both normal builds, and Tensura Neo Otherworld 1.1.3a’s Tensura 2.0.1.4 / NeoForge 21.1.248 for standalone integration.

## Spiritual contact

Nullification’s tooltip now includes **“Can attack Spiritual Entities.”** The engraving reuses its existing weapon-source check: main-hand melee and Chained/Unbound weapon arts qualify, while unrelated abilities/projectiles and offhand weapons do not.

A narrow hook returns a full physical input multiplier from native `RaceUtils.getPhysicalAttackInputMultiplier` for qualifying hits. This covers native spiritual bodies such as War Gnome without relabeling damage, changing the attacker’s race or requiring Haki/Anti-Skill. Another hook waives only the spiritual-form contact gate in Tensura’s native damage handler. The target’s stored form stays intact; the remaining damage pipeline handles armor, resistances, blocks, dodge, ally/PvP rules and the engraving’s existing barrier/status/seal behavior.

Four new native tests in each distribution cover source qualification and Physical classification; actual War Gnome hits with a regular weapon versus the engraving, including ordinary armor; spiritual-form immunity versus spear contact and an unrelated held-weapon spell; and both Chained and Unbound weapon-art contacts. War Gnome tests select an unblocked hit through its native pipeline so its independent shield RNG does not distort the reduction comparison.

## Reference-matched art

The earlier narrow three-pronged head is replaced by the supplied reference’s broad asymmetric split blade: one long prong, one shorter prong, a deep open slot, a straight silver guard, dark brown wrapped grip and gold fittings. The image-generated authoring master is retained in `art/items`. The export script uses ImageMagick’s nearest-neighbor sampling and binary alpha to produce the mirrored 64×64 Minecraft sprites. The existing native handheld model renders the new silhouette in inventory and held views.

The generated master and exported sprite were visually inspected. Both distributions’ tooltips, identical item textures and the regenerated Markdown, HTML, GitHub-Wiki export and offline bundle were checked. This release has no new in-game visual capture.

## Validation

All listed tests passed, and the production build/distribution checks completed successfully.

- Full Myths: **345 native GameTests** and **11 JUnit tests**.
- Standalone: **111 native GameTests**.
- Neo Otherworld 1.1.3a: **20 integration GameTests**, including all four new spiritual-contact checks and the existing complete addon starting-skill audit.
- Normal production builds and the standalone distribution audit verify metadata, all mixin classes, item resources, the mirrored sprite and exclusion of optional review code.

All runs use disposable validation worlds. The full Myths release targets its existing Tensura version; use the standalone release for Neo Otherworld.

[Standalone validation](../standalone/heavenly-restriction/docs/VALIDATION-1.1.7.md) · [Nullification wiki](../wiki/pages/Nullification.md) · [Spear wiki](../wiki/pages/Inverted-Spear-of-Heaven.md).
