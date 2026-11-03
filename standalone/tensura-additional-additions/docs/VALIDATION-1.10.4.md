# 1.10.4 — Unique Skills throughout The High Realm

Every natural High Realm species has a mastered native Unique Skill. Existing Berserker (High Orc), Survivor (Armorsaurus), Fighter (Horned Bear) and Seer (Blade Tiger) are retained. New signatures are Shadow Striker (Tempest Star Wolf), Oppressor (Knight Spider), Martial Master (Dragonewt), Survivor (Basilisk), Great Sage (Greater Daemon) and Mathematician (Arch Daemon). Legacy Lesser Daemon elites receive Great Sage. Fighter remains limited to Horned Bears.

Only native mob-compatible passives are configured. Toggleable skills are enabled; Martial Master is equipped alongside the existing Fighter/Berserker preset handling. The wolf does not equip Shadow Striker’s separate execution mode. Player-only menus/techniques are not called by AI. Daemons retain their seven/eight native spells and autonomous casting behavior. Entry stays at 5M actual EP and bosses remain excluded.

Version-4 migration upgrades saved unowned elites without healing, refilling resources or consuming existing EP; stamped loadouts do not replay. Owned creatures remain excluded. The searchable in-game guide and repository/GitHub/static wiki exports describe the signatures.

Two additional native scenarios exercise exact signatures for all ten current species and legacy Lesser Daemons, mastery/toggles/presets, saved vital/EP preservation, no repeated chant multipliers and NBT persistence. Combat probes verify wolf dodge, Arch Daemon resistance degradation, spider arrow immunity, Dragonewt physical multiplication, Basilisk survival immunities and native caster chant-speed improvements. Existing autonomous Greater/Arch Daemon spellcasting tests are retained.

The first full run passed 238 of 239 tests; the pre-existing skill-seal expiry fixture did not wait for its newly force-loaded target to begin simulation. That fixture now waits for the first entity tick before starting the same 40-tick seal and 41-tick expiry assertion. No skill-seal gameplay behavior was changed.

After the fixture correction, **all 239 native GameTests and 31 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209.

Compatibility compilation passed against Tensura 2.0.1.4 / NeoForge 21.1.248. This is API coverage; a fresh full Neo Otherworld modpack runtime was not run.

The clean baseline release build and distribution audit passed. The shipped JAR contains the updated guide and loadouts and excludes opt-in High Realm review classes/preset overrides.
