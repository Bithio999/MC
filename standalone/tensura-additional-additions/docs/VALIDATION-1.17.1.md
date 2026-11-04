# 1.17.1 — Native Wayfarer combat

Kael uses his actual stored Tensura abilities. The former four signature attacks, custom damage geometry and encounter meshes are removed. His AI chooses native Aura Slash, Martial Master, Wind Domination spheres/breath, Wind Cutter, Tornado Blade, close-range Aerial Blade and Seer's Future Vision when wounded. Native callbacks determine damage, weapon contributions, resource spending, spell cast times, cooldowns and effects. Short AI decision intervals prevent skills without native cooldowns from being spammed; they do not change player skill configuration.

Martial Master's exact native dash destination is checked for loaded, dry, solid, unobstructed ground near the enemy before calling the native ability. The AI uses native alternate activation for wind spheres and restores its previous crouch state afterwards. Held wind breath and spells stop when the target becomes invalid or the owner is sealed. Native casting circles are cleaned up on cancellation.

Ultraspeed Regeneration is replaced with mastered, enabled Infinite Regeneration; Wind Manipulation is replaced with mastered, enabled Wind Domination. A versioned migration upgrades already-initialized Kaels, including loaded saved entities, removes the superseded native toggles and their preset references, and preserves current HP, SHP, aura and magicules. It does not reset race, ownership, actual EP or wound state. Repeated initialization does not stack bonuses. Kael retains 15M actual EP and his existing native defenses.

Only the HP boss bar remains. SHP remains a normal combat resource, with native Infinite Regeneration and red damage feedback. The skin, rare spawning, cooldown, drops and advancement remain intact.

Runtime reproduction instructions are in [review-wayfarer/README.md](review-wayfarer/README.md). Review profiles remain opt-in and must be cleaned before packaging.

All **286 required native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. The boss suite verifies actual EP/race/status, mastered skills and presets, wounded save/reload state, rare spawn restrictions, real Aura Slash and wind projectiles with native resource costs, native spell releases and Aerial Blade range, sealed breath cleanup, grounded native Martial Master movement, migration without refills or duplicate wind bonuses (including a superseded saved preset), native Infinite Regeneration HP/SHP healing and magicule spending, a single boss bar, and autonomous native ability use.

Native breath entities are explicitly discarded on channel cancellation and owner removal, before their multipart collision logic can run without an owner. Review fixtures wait for entity indexing before resolving area effects.

The opt-in integrated client review completed successfully in an existing disposable normal-generation world. It verified 15M actual EP, real HP/SHP damage from native abilities and a single HP boss bar. Fresh captures of Wind Domination, Aura Slash and Tornado Blade are included in wiki media.

The optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile compiled successfully. This is API coverage, not a full Neo Otherworld modpack runtime test.

The clean baseline production build passed all **49 JVM tests** with no failures or skipped tests. The standalone distribution audit passed, including native boss resources and exclusion of review fixtures, the test-only world preset and bundled dependencies.
