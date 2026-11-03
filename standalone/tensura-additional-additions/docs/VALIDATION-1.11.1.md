# 1.11.1 — Auto-Cast mode selection

Magic Assignment now has an upward-opening mode dropdown, using native names and read-only native mode traversal. It shows locked modes, supports mouse/keyboard selection and scrolling, and lists five spells per page to preserve space for the selector at small GUI sizes. The server revalidates ownership, mastery-gated traversal and any native mode-learning requirements without spending resources or teaching a mode.

The selected index is stored in Split Mind’s NBT. Old saves without the new key use mode 0. Assignment requests carry an explicit mode; malformed, out-of-range and locked selections are rejected. Changing assignment cancels the old cast. If the owner loses the selected mode, Auto-Cast stops.

The runtime propagates the chosen mode through the owned spell instance’s native activation/hold/release lifecycle, resource checks, cooldown checks, virtual preset scope and cleanup. Fire Lance Repeat, Icicle Lance Shot, Icicle Rain Repeat, Water Cutter Repeat and Search Enemy Constant are maintained as native held modes, retaining their own pulse timing/costs and ending when the target or casting conditions fail. Native channel completion retains release/cooldown handling. No new damage or cost formula is introduced.

Three new native scenarios verify locked modes/read-only probing, legacy mode-zero defaults, mode persistence, mode-specific menu/cooldown state, stopping after loss of eligibility, actual Chain Stone Shot staggered projectiles and separate cooldowns, and repeated native Fire Lance pulses with exact per-pulse payment and toggle-off cleanup. The original Split Mind behavior tests remain enabled.

Native delayed projectiles spawned by Auto-Cast bind the selected target in synced entity data. Their existing delay-vector calculation aims at that target until launch, retaining native spread offsets, launch delays, gravity and post-launch flight; normal/manual projectiles are unchanged and no homing is added. The Chain-mode test also checks direction after the owner looks away.

All **249 native GameTests and 31 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209.

The integrated-client review passed: the real dropdown selected Chain Stone Shot, the C2S assignment persisted mode 1, and native Auto-Cast damaged a hostile target while the player faced away. The menu was checked at GUI scales 2 and 4; mouse selection, dropdown dismissal and locked-mode presentation were exercised. Captures are included on the Split Mind wiki page.

Compilation also passed against Tensura 2.0.1.4 / NeoForge 21.1.248 through the optional Neo Otherworld API profile. This is API compatibility coverage, not a fresh full-modpack runtime test.

A clean baseline `build` passed, including the 31 JVM tests. The distribution audit verified version/dependency metadata, new mode/projectile support, shipped mixins and guide assets, and the absence of bundled dependencies, Myths classes and review fixtures. The release JAR is copied to `releases/` with its SHA-256 checksum.
