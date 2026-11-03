# 1.10.5 — Engorger daemons

Greater Daemons now receive mastered, toggled-on Engorger instead of Great Sage. Saved legacy Lesser Daemon elites use the same replacement. Arch Daemons retain Mathematician and both current tiers retain their native seven/eight spell packages. Engorgement uses Tensura’s native size/combat-stat modifiers and periodic healing for mobs; no player-only active is invoked.

Loadout version 5 removes the Great Sage assigned by version 4 using its toggle-off, preset cleanup and forget lifecycle, then grants Engorger. Migration preserves current HP/SHP/aura/magicules, does not repeat on reload, and excludes owned creatures. Final dimensions are refreshed before fresh-spawn collision checks. The realm still requires 5M actual EP.

Native coverage includes prior-version skill replacement, removal of chant/preset bonuses, retained unrelated preset entries, mastered/enabled Engorger, actual Engorgement scale/attack modifiers and healing, current-resource preservation, replay/reload safety, owned mobs and unchanged Arch Daemon signature. Existing species-signature tests now expect Engorger; autonomous Greater/Arch Daemon spellcasting remains covered. The wiki and searchable in-game guide describe the new signature.

**All 240 native GameTests and 31 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209.

Compatibility compilation passed against Tensura 2.0.1.4 / NeoForge 21.1.248. This checks the optional API profile; a fresh full Neo Otherworld modpack runtime was not run.

The clean baseline release build and distribution audit passed, including version metadata and exclusion of opt-in High Realm review fixtures/preset overrides.
