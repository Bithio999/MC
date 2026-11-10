# Additions 1.23.0 — Metis, Lord of Wisdom

Metis is a native Ultimate synthesized from fully mastered Split Mind and Great Sage after completed true awakening and a configurable learned-magic achievement. The acquisition bill defaults to 250,000 permanent magicules, respects mpSkillCost and applies only after awakening. There is no EP gate. Explicit confirmation replaces both components; source storage, assignment and preset modes are preserved. Passive state is captured before native forgetting disables source instances.

Unified Intellect provides +19 learning/mastery points, native Thought Acceleration, Chant Annulment and automatic visible circle analysis within 32 blocks. Circle analysis uses native copiable-magic tags, the plunder event and SkillHelper acquisition checks; self/known spells are skipped, scans are capped and rejected spell retries are bounded. Resistance dossiers are bounded to 32 targets and only read defenses.

The matrix retains up to three distinct real owned magic instances (four at mastery). It uses the existing scoped auto-cast invocation and native hold/release lifecycle so aim and background casting modifiers remain isolated from manual casting. Native modes, cooldowns, aura costs, anti-magic and targeting checks apply. Explicit scheduling priorities never turn harmful spells into self-healing spells: target roles come from native magic types. Healing/support target the owner in a scoped native selection override. Magicule discounts apply once to the native Magic payer or direct EnergyHelper payer, including consumption-based healing; shortfall units remain unchanged. Normal manual bills remain unchanged.

The interface uses cyan Tensura-style beveled controls and a dark panel, separate matrix and synthesis tabs, learned-only search, mode/priority popovers, and an explicit synthesis confirmation. Editing requests require an expiring owner/session match, and the server validates ownership, learning, mode, slot, duplicate spell and mastery limits. Inherited Great Sage utilities retain their actual instance/shared native 20-slot container and original refining offset.

The focused baseline suite passed **10 native Metis cases** and **64 JVM tests** on Tensura 2.0.1.3 / NeoForge 21.1.209. The real client passed explicit synthesis confirmation, a real Assign request, mode selection, mastery/four slots and normal / 320×180 GUI bounds. Captures were inspected for the matrix, overlay and small layout.

The full baseline regression suite passed **383 required native tests**, including the existing Split Mind, forge, equipment, race, combat, world travel, High Realm and map cases. The standalone registration expectation was updated to 18 skills.

The Tensura **2.0.1.4 / NeoForge 21.1.248** compatibility profile passed **10 native Metis tests** and **64 JVM tests**. This is an API/runtime check rather than a whole-modpack playthrough.

Final hardening adds expired-session recovery without mutating assignments, plus native Great Sage appraisal restoration on respawn. The final focused suite contains **11 cases**. Client captures also verify the actual native Ultimate selection menu and centered skill icon.

The final **11-case native suite passed on both supported Tensura profiles** after session-expiry recovery and inherited appraisal respawn handling were added. A clean baseline production build then passed all **64 JVM tests** and the independent distribution audit, verifying native metadata, the Ultimate tag, localized name, 64px icon, required classes and absence of review fixtures or bundled dependencies.
