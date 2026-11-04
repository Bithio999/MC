# 1.11.4 — Unique mastery bonuses

Split Mind discounts the native Magic payment before affordability checks, scoped to the exact owned automatic callback. Its startup preflight and assignment snapshots use the same 0.75 factor. Native damage, aura and manual magic bills remain independent. Existing single, alternate-mode, repeat-pulse and simultaneous-manual-cast payment assertions now include the mastered factor. A new native scenario compares normal and mastered bills, checks an exactly affordable discounted cast and the displayed cost, then verifies a manual cast pays full price.

Loan Shark evaluates the lender's current mastery for an 8-minute (9,600 tick) or normal 10-minute (12,000 tick) idle requirement. The actual claim, report readiness and sender's menu use that same threshold. A new native scenario verifies the 9,599/9,600 boundaries, no bonus while the gift is active, retained stolen/copied mastery and normal gift recall.

Fusion snapshots the initiating caster's mastery before sharing skills. Mastered sessions last 5,400 ticks with a 1.75 multiplier for combined maximum/current aura and magicules. Visual and shared-skill lifetimes use the session duration. HP/SHP addition, damage, regeneration and penalties retain their existing rules. A native scenario checks actual fused resources, expiration before/at the new boundary and restoration without permanent EP changes. Existing scenarios retain baseline 3-minute/1.5× coverage.

Prospector selects a separate datapack-overridable mastered finds table only while its learned passive is active. Its find probability is 0.015; weights sum to 100: coal 12, raw iron 15, raw copper 13, raw gold 14, redstone 10, lapis 10, quartz 8, diamond 7, emerald 4, magic ore shard 7. The normal 1% table is unchanged. A native scenario samples both real registry loot tables with a fixed seed, checks rate/rarity shifts and one-item drops, and verifies toggle gating.

All **257 native GameTests and 31 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. The native suite covers the new mastery scenarios alongside existing baseline, repeated-cast and concurrent manual-cast checks.

Compilation also passed against the optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile. This is API compatibility coverage rather than a fresh full Neo Otherworld modpack runtime test.

The clean baseline build and distribution audit passed. The audit verifies version/dependency metadata, packaged JSON (including the mastered loot table), guide assets and mixins and excludes review fixtures, bundled dependencies and Myths classes. The release includes a SHA-256 checksum.
