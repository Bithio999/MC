# 1.18.1 — Native blessing exclusions and engraving reference

The exclusion tag now explicitly includes Tensura’s Vitality, Vigor, Transcendence, Growth and Restoration. Their registry IDs omit “blessing,” and neither supported native version adds a blessing tag, so the previous generic identifier/tag filter did not catch them. The change leaves existing saved items and infusion history intact. No damage, duration, modifier or native engraving behavior is changed.

The new native scenario checks each real blessing is in the base engraving eligibility pool, rejects it through the Orb filter, uses a real anvil to upgrade an ordinary engraving alongside it while retaining its level, and rejects blessing-only gear. Existing synthetic blessing/tag, curse, repeat-use, persistence, repair and crafting scenarios remain enabled.

The wiki reference was checked against both pinned Tensura jars. Their enchantment JSON definitions are identical. Effect calculations were traced through `WeaponBaseAttributeMultiplier`, `SeveranceDamageEntity`, `EnergyStealEntity`, `DeadEndRainbowEntity`, `TensuraDamageHelper`, `DamagingHandler`, `EnergyHelper`, `EffectStorage`, `SlottingHelper`, `SimpleSpellCastItem`, Minecraft’s `CombatRules` and `EnchantmentHelper`, plus the Mysticism enchantment definitions and the addon’s own Nullification handler. Source-backed limitations include the 50% Severance wound-fraction cap, current-resource energy stealing, highest-level protection selection, fixed Nullification/Breathing Support/Boreal Frost behavior, and the absence of four-core native slotting recipes.

Player-facing values use defaults and clearly distinguish weapon-attribute bonuses, raw extra-hit coefficients, resource multipliers and flat armor-mitigation subtraction. No upstream source or dependency jar is bundled.

All **299 required native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209, including the new real-blessing regression. A documentation coverage check matched the 23 eligible native engravings, Additions’ Nullification and both Mysticism engravings against the 26 reference rows.

All **49 JVM tests passed** in the clean production build, and the standalone distribution audit passed. The relevant native effect/helper classes are byte-identical between the two supported Tensura versions, and both native data sets contain zero four-core combination recipes.
