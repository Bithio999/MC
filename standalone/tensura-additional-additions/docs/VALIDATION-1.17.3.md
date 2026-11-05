# 1.17.3 — Hero skill package

Kael retains his native Human Saint race and True Hero status. His mastered package now includes Law Manipulation, Magic Wind Transform, Divine Ki Release, Eye of Truth and Unpredictability. True Hero status is set before initializing the hero-dependent toggles. Divine Ki Release is an explicit boss grant: native acquisition normally requires the later Divine race tag. Its own native toggle callback applies the effect; global race or skill requirements are unchanged.

Combat opens with native Magic Wind Transform, paying native magicules and respecting native duration and cooldown. The AI checks the active elemental effect before activation so it cannot accidentally remove the transformation. It then prioritizes Law's native Self-Cleanse when Severance, Cook's maximum-HP penalty or a native cleanseable effect is present. Magic Takeover selects eligible hostile native spells, respects Law's range and owner EP protection, and verifies the exact native targeting ray before invoking mode 1. This interrupts casting circles, closes eligible portals or transfers native projectile ownership as the native skill specifies. Wind Domination remains passive only.

Actual EP growth is clamped to 15,000,000 before native positive-growth events, preventing rejected kill EP from granting native stat growth or kill healing. A post-reward cap and existing-entity initialization/server-tick checks also correct over-cap capacity modifiers and legacy saves. The cap accounts for capacity modifiers, retains the current capacity split, and only clamps current aura/magicules if they exceed the reduced capacities. It never refills HP, SHP or current resources. Native reward processing is retained; the reward hook affects only Kael. Versioned loadout migration adds the new skills to saved Kaels without resetting equipment, ownership or wounded state.

The native regressions cover mastered/enabled intrinsic modifiers; the real transformation opener and its cost; avoiding accidental uncasting; autonomous Self-Cleanse removing Severance and Cook without healing; autonomous Magic Takeover destroying a hostile native casting circle and setting the interrupted spell's cooldown; repeated native player-kill awards; actual-cap accounting with flat/multiplicative capacity modifiers; and an over-cap version-2 saved Kael migrating without refilling HP, SHP, aura or magicules. Existing native ability, regeneration, rare spawn and persistence scenarios remain enabled.

Reproduction uses the opt-in [High Realm server profile](review-high-realm/README.md) and guarded [existing-world client review](review-wayfarer/README.md). Production builds are cleaned afterwards, and the distribution audit excludes review fixtures and the test-only world preset.

All **289 required native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209, including all 14 Kael scenarios. All **49 JVM tests passed**, with no failures or skips.

The integrated client review passed in an existing disposable normal-generation save, checking the active native wind transformation and real damage from Wind Cutter, Tornado Blade, Aura Slash and Aerial Blade. The one HP boss bar and native renderer remained intact. A gameplay capture of Magic Wind Transform is included in the encounter wiki.

The optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile compiled successfully. This is API coverage; the native game and client checks above used Tensura 2.0.1.3 / NeoForge 21.1.209.

The clean production build passed all 49 JVM tests. The standalone distribution audit passed, including all registered mixin classes and exclusion of review fixtures, the isolated diagnostic namespace, the test-only world preset and bundled dependencies.
