# 1.17.0 — Kael Veyr, the Starless Wayfarer

Kael is a new native otherworlder boss, not an elite-converted monster. His native Human Saint race, True Hero status and resource pools give exactly **15,000,000 actual EP** (7.5M aura / 7.5M magicules), with **9,900 HP / 34,100 SHP** under the baseline native configuration. Native race/support modifiers explain the difference from his 8,000 / 24,000 base health attributes.

His three native Uniques are mastered and equipped in the real ability preset. Native stored support skills, battlewills, seals, damage handling, magic release callbacks, costs and cooldowns remain active. Tornado Blade creates the native owned wind projectile; Aerial Blade is selected only within its configured range. Custom sword and light attacks use server-authoritative, bounded geometry with visible warnings. Native taming suppresses hostile choreography and boss bars.

The rare spawn is registered in all three High Realm biomes at weight 1, with a configurable 2.5% extra acceptance roll. A successful natural spawn commits a persisted 24,000-tick realm cooldown. Sanctum exclusion, local boss exclusion, dry safe surfaces and dimension/difficulty restrictions apply. Debug commands and spawn eggs remain available. Existing elite spawn rates, caps and EP are unchanged.

The opt-in client review completed against an existing disposable normal-generation world on Tensura 2.0.1.3 / NeoForge 21.1.209. It exercised all four patterns, verified real HP/SHP damage, and captured the original player-UV skin, emissive accents and smooth mesh effects. Eight gameplay screenshots are stored in wiki media. This is a focused existing-world runtime review, not a full modpack playthrough.

All **283 required native GameTests passed**, including eight new boss scenarios: native race/awakening/actual EP and hero-boss classification; learned mastery and preset slots; save/reload without refills; dimension, rarity, sanctuary and persisted spawn cooldown; warned light damage and skill sealing; an owned native Wind Tornado plus close-range Aerial Blade and mana spending; safe solid blink landing with real slash damage; and a traveling sword front with creative-player exclusion. The two immediate-hit fixtures wait for entity indexing before resolving world targets.

The optional Tensura **2.0.1.4 / NeoForge 21.1.248** API profile also compiled successfully. That check covers API compatibility; it is not a full Neo Otherworld modpack runtime test.

The clean baseline production build passed **49 JVM tests** with no failures or skipped tests. The standalone distribution audit verified dependency metadata, the original 128px skin pair, biome entries, spawn egg, loot, advancement and native boss tags, and confirmed that no review fixtures, test preset override, Myths classes or bundled dependency JARs are shipped.

Reproduction instructions and native integration references are in [review-wayfarer/README.md](review-wayfarer/README.md). The production package must be built clean after either review profile so neither review code nor the GameTest flat-preset override is distributed.
