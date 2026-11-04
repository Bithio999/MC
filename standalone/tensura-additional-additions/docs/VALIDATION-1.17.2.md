# 1.17.2 — Magic and battlewill combat

Kael's AI no longer selects either Wind Domination active: breath and wind sphere. Wind Domination remains mastered and toggled for its native wind boost and resistance degradation. Offensive choices are native Aura Slash, Wind Cutter, Tornado Blade, Aerial Blade and Martial Master's dash, alongside ordinary sword attacks. No loadout or resource migration is needed for saved Kaels.

Seer's Future Vision can be activated during combat at full health. The old 75% HP gate was ineffective alongside Infinite Regeneration. The AI prioritizes Future Vision when valid, respects its native cooldown and does not press it again while its effect is active. Martial Master retains its native dash and in-slot damage/passive bonuses. Mathematician contributes native toggle modifiers; its appraisal activation explicitly requires a ServerPlayer and is not an NPC attack.

The native boss scenario checks actual full-health autonomous Future Vision activation, native cooldown state, prevention of accidental uncasting/recasting, the passive modifier IDs for all three Uniques, Mathematician's resistance degradation, and retained Wind Domination mastery/toggle with no active attack choice. Other scenarios retain actual native magic/battlewill projectiles, resource costs, seals, safe native dash, save/reload, regeneration and spawn checks.

The opt-in client review is updated to activate Wind Cutter, Tornado Blade, Aura Slash and Aerial Blade in a disposable existing High Realm save. Review instructions remain in [review-wayfarer/README.md](review-wayfarer/README.md). Review fixtures and the GameTest world preset must remain outside the production package.

All **286 required native GameTests passed** against Tensura 2.0.1.3 / NeoForge 21.1.209, including the revised autonomous Future Vision and Unique-passive scenario.

The integrated client review also completed successfully in an existing disposable normal-generation world. It captured the four native magic/battlewill attacks, verified real HP/SHP damage and retained the single HP boss bar.

The clean production build passed all **49 JVM tests** with no failures or skips, and the standalone distribution audit passed. No review fixture, test-only preset or bundled dependency is included.
