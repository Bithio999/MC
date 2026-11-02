# 1.7.0 — Prospector

Prospector is an independently registered **Unique** skill in the native reincarnation pool. The native starting-skill pool is configured separately from the Unique tag, so the existing pool hook also adds Prospector without replacing server entries; native ownership and Truly Unique filters still apply. Its learned toggle grants +50% mining speed, +3 effective Fortune, a rare stone/deepslate find and ore vision. Refine uses Tensura's `IRefining` protocol, native screen/menu, recipes, buttons and ingredient/output serialization.

## Mining

`PlayerEvent.BreakSpeed` applies the 1.5 multiplier alongside other normal mining modifiers. The Fortune hook supplies a copy of the original tool to the native block loot evaluation with three added levels. The original item's durability, components and enchantments remain untouched. Drops are evaluated once; the implementation does not replace an already-evaluated drop list or multiply arbitrary drops. Silk Touch retains its normal precedence in loot tables. Other mods' Fortune-aware block loot tables receive the same effective level.

A separate data-pack loot table, `heavenlyrestriction:gameplay/prospector_finds`, makes one 1% roll for each suitable-tool stone/deepslate break by a survival/adventure player. The extra result is one resource, independently of Fortune. Conditional weights sum to 100: coal 24, raw iron 22, raw copper 20, raw gold 10, redstone 8, lapis 6, quartz 4, diamond 2, emerald 1, magic ore shard 3. No terrain or world-generation change is required. This follows the ordinary cancellable block-drop event; canceled drops are not spawned.

Both bonuses require a living, learned, toggled owner who passes native skill interaction checks. Refine is independent of the toggle and has no additional aura/magicule cost. Its ingredients remain subject to native recipes. Prospector does not copy or steal skills and passes Fettered Human's reincarnation cost filter.

## Ore survey and display

- Client-only survey of terrain already received from the server. No chunk requests, server scans, extra entities or ore-location packets.
- A true 32-block distance limit. Up to four section jobs and 8,192 block cells scanned per client tick; empty/non-ore section palettes are skipped. New surveys prioritize nearby sections.
- Six-neighbor connected ore blocks form deposit markers. At most 8,192 ore cells and 96 nearest deposit markers are retained. A marker's count describes the surveyed portion of its vein, not ore outside range/unloaded terrain.
- Convention ore tags cover vanilla and tagged modded ores; explicit native magic-ore IDs cover Tensura's normal/deepslate magic ore. Resource blocks are excluded. Vanilla families have distinct colors/glyphs; other tagged families receive stable colors and labels.
- Faint corner brackets and small glyphs appear through terrain. Only the deposit near the crosshair gets a compact name/count/distance label. Magic ore uses a violet double diamond. No fullscreen shader or solid X-ray fill is used.
- Scanning/grouping runs on client ticks, not render frames. Toggle-off, losing the skill, death, world replacement and disconnect clear the survey; range checks also apply while drawing. Tag reload refreshes classification.

## Verification

The current baseline has **176 native GameTests** and **30 unit tests**. The real client ran against Tensura 2.0.1.3 / NeoForge 21.1.209. Fancy and Fabulous rendering both passed a framebuffer pixel check for colored markers behind an opaque stone wall. Native menu/toggle/brew packets, all eleven native ore families, range, scan limits, vein clustering, toggle cleanup and refining output persistence passed.

Compilation also passed against Tensura 2.0.1.4 / NeoForge 21.1.248. The shipped build targets the older supported baseline. This is not a complete Neo Otherworld runtime test or a shader-pack compatibility claim.

Native gameplay checks cover effective Fortune VI from a Fortune III tool, original-item preservation, actual diamond loot statistics, Silk Touch, mining toggle/learning gates, the rare loot table, unsuitable/non-stone mining, magic-ore classification, native refining consumption/output persistence and the updated independent registry counts. Existing race, battlewill, world-travel and Fusion regression suites are retained.

The [opt-in client review](review-prospector/README.md) uses the actual Unique menu/toggle packet, ore markers behind a solid stone wall, all eleven vanilla/Tensura ore families, range/budget checks, native Refine menu and brew-button packets, a real high-potion recipe, and close/reopen persistence with the passive off. Screenshots use a prepared disposable world; they are gameplay captures, not mockups.

[Prospector wiki](../../../wiki/pages/Additions-Prospector.md) · [Previous validation](VALIDATION-1.6.2.md)
