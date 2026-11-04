# 1.11.3 — Split Mind fixes and High Realm spawn reduction

Auto-Cast previously installed the same movement/range casting modifiers as a manual chant, affecting client FOV and activating the one-chant lock. Manual activation also canceled any automatic hold, and automatic startup waited for all other holds/items.

Automatic native calls now omit only the casting movement/range modifiers. Scoped modifier views in Magic and Mud Spears ignore the competing-chant lock, while their separate anti-magic conditions remain intact. Native skill eligibility, costs, cast times, repeat cadence, damage and cooldowns are preserved. Other manual spells retain their modifiers and can start, advance and release alongside automation. Manual activation of the same owned spell takes priority, preventing shared cooldown/tag/hold collisions. Automation waits while that same spell is manually held.

Two additional native scenarios cover both startup orders with actual Repeat Fire Lance and unmastered Fireball. They check overlapping hold progress, native projectiles from both spells, exact combined costs, manual cooldown, independent release and continued auto pulses, and unchanged automatic movement/range attributes. The final combined baseline suite passed **253 native GameTests and 31 JVM tests** on Tensura 2.0.1.3 / NeoForge 21.1.209, including the High Realm spawn scenario.

The integrated client review passed: real client FOV and movement speed stay at baseline during Auto-Cast, unmastered manual Fireball retains its normal casting zoom and fires alongside Repeat Fire Lance, and manual release restores FOV while automatic pulses continue. Existing native menu/mode-assignment and actual off-camera Chain Stone Shot damage checks also passed.

High Realm natural/chunk-generation spawning now applies a configurable 0.5 acceptance rate once per finalized attempt. A second guard prevents new spawns when 16 living unowned elites already exist within 64 blocks. Only the realm's eligible natural pool uses these guards; saved entities, imports, commands, summons and other dimensions are unaffected. Existing mobs are not deleted. A native scenario checks the seeded acceptance rate, crowded-area denial, resumption below the cap, disabled spawning and command/summon/Overworld exclusions.

The two native casting classes targeted by the new mixins (`Magic` and `MudSpearsMagic`) have identical bytecode in Tensura 2.0.1.3 and 2.0.1.4.

Compilation passed against the optional Tensura 2.0.1.4 / NeoForge 21.1.248 API profile as well. This is API compatibility coverage, not a new full Neo Otherworld modpack runtime test.

A clean baseline release build and the distribution audit passed, verifying version/dependency metadata, all shipped mixins and assets and exclusion of review fixtures, bundled dependencies and Myths classes. The release JAR includes its SHA-256 checksum.
