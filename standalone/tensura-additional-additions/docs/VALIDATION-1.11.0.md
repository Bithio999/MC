# 1.11.0 — Split Mind

Adds a standalone Unique Skill with one passive checkbox for +9 learning/mastery point gain, native Thought Acceleration bonuses and native Chant Annulment. Its movement/attack/chant/dodge modifiers use a separate ID so removing Split Mind does not remove another Thought Acceleration skill. Mastered magic becomes instant under Tensura's existing rule; unmastered magic retains its accelerated cast time. Successful paid/cooldown-producing auto-casts train Split Mind; opening menus and freely switching toggles do not grant mastery.

Magic Assignment is a bounded, paged, searchable native-style menu with aspectual/spiritual filters. The server lists only currently owned, learned magic and validates the choice again on assignment. Learning entries, staff-only unlearned instances and nonmagic abilities are excluded. The saved assignment and independent Auto-Cast switch live in the skill's NBT. Removing the assignment or losing the spell stops automatic casting.

Auto-Cast checks for a new cast every five ticks and targets the nearest visible hostile/combat opponent within a spherical 32-block range. It excludes allies, own subordinates/tames, protected players and passive bystanders; combat opponents can include players when PvP permits. Spells keep their first-mode native damage, costs, cooldowns, cast times and activation restrictions. Manual input takes priority. Runtime holds are canceled on lost targets, ownership changes, death, logout and dimension changes.

The engine uses the real owned spell instance and ManasCore activation/hold/release events. Tensura requires activated magic to occupy an active preset, so a thread-local invocation recognizes exactly the currently owned automatic spell at a reserved virtual key. It neither edits the three actual slots nor admits ordinary/client activation outside that internal scope. The scoped membership also preserves Tensura’s full normal hold duration instead of its 20-tick limit for unequipped abilities. Camera aim is scoped to native callbacks and restored with try/finally. No alternate damage formula or duplicate resource payment is used.

The normal reincarnation pool, Unique tag, custom original icon and searchable in-game guide are registered. Fettered Human's existing magicule-cost starting blacklist includes Split Mind's delegated magic casting; the normal configured allowlist remains available. The wiki includes real game captures.

The integrated-client fixture passed native Unique name/icon selection, real assignment-menu packets, learned-only search/type filtering, persisted Spiritual Fire selection, controls at GUI scales 2 and 4, and native automatic casting of an unequipped, unmastered Fire Ball that damaged a hostile while the player faced away. No cast-step helper was used for that live check.

**All 246 native GameTests and 31 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209, including the revised 14-skill standalone content audit.

Compatibility compilation also passed on Tensura 2.0.1.4 / NeoForge 21.1.248. This is API coverage; a fresh full Neo Otherworld modpack runtime was not run.

The clean baseline release build and distribution audit passed. The shipped JAR contains the icon, 16 guide entries, Unique tag, runtime classes and required mixins, with no opt-in client/review fixtures, test preset override or bundled dependency JARs.
