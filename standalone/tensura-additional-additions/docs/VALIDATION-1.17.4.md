# 1.17.4 — Divine Human

Kael now starts as Tensura's native Divine Human, the terminal human evolution. He retains True Hero status, his existing skill package and the 15M actual EP cap. Divine Ki Release now satisfies the native Divine race requirement; the earlier explicit toggle exception is removed.

Loadout migration version 4 evolves already-initialized Kaels with the native race API. It snapshots current HP, SHP, aura and magicules before the race change and restores them afterwards, preventing the native race setter's full heal. The native API removes the previous race's modifiers and applies the Divine bonuses. Equipment and ownership are retained. Initialization and server-side EP checks retain the existing cap.

The native migration scenario saves a wounded version-3 Saint Kael, reloads and upgrades it, verifies the Divine race and terminal evolution, compares HP/SHP capacities with a fresh Divine Kael, checks actual EP and all wounded pools, confirms True Hero status and unchanged weapon/components, and verifies native eligibility and activation of Divine Ki Release. It repeats initialization and save/reload to check persistence and prevent stacked race bonuses. Existing combat, regeneration, skill loadout, EP cap and rare-spawn regressions remain enabled.

Run the [High Realm native server profile](review-high-realm/README.md). Clean the build before packaging; review classes and the test-only world preset are excluded from production.

All **290 required native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209, including all 15 Kael scenarios and the saved-Saint migration. All **49 JVM tests passed**, without failures or skips. Fresh default Divine Kael measured **10,000 HP**, **37,100 SHP**, **7.5M aura capacity** and **7.5M magicule capacity**.

The optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile compiled successfully. Runtime game tests used the baseline versions above; this compile check is not a full modpack runtime test.

The clean production build passed all 49 JVM tests. The standalone distribution audit passed, including native metadata and mixins, with no review fixtures, test-only world preset, Myths classes or bundled dependencies.
