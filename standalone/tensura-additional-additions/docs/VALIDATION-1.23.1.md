# Additions 1.23.1 — Auto-cast gestures and independent native hit windows

Native spells call LivingEntity.swing during their held/release callbacks. The shared owned auto-cast scope now suppresses that call before arm state changes or animation packets are sent. Manual input is outside that scope and remains unaffected, including a manual swing already in progress.

Automatic damage uses bounded, transient per-target hurt windows keyed by caster UUID, spell ID and mode. Reads/writes of the native hurt timer and last-hit amount in LivingEntity.hurt use that spell’s window, rather than changing global manual hit state. Window aging uses the target’s tick count; ordinary damage still uses vanilla state. Existing native cooldown-bypass flags remain native. Longer defensive invulnerability is observed and retained through its protected tail. Ordinary invulnerable entities, resistance checks and damage processing remain native.

Native TensuraSkill.createSource records provenance for direct automatic spell hits. Spawned owned projectiles/clouds retain provenance in entity persistent data so delayed and saved impacts can be attributed after the callback ends. Native source copies preserve it. Owning Metis does not mark manual spells or weapons as automated.

TensuraDamageHelper.hurtDouble/hurtSplit intentionally reset the hurt timer between paired components. Their timer access uses the same automatic spell window, preserving a spell’s native multi-component behavior instead of making every sustained pulse hit without cooldown. No global spell rebalance is applied.

The native spell comparison reproduced unmodified Wind Cutter hits of 40 then 30, Fire hits of 30 then 20, and Water Cutter hits of 40 then 0 with default configuration and no extra target defenses. Both supported Tensura binaries use the same paired-hit routine and default values. The routine applies secondary magic first, conditionally clears the timer, then applies elemental damage. The final component becomes the last-hit comparison for the next spell. A pure magic Water Cutter has no second component to perform that reset. These figures can change with combat modifiers and intervening hits.

The 17-case focused suite includes six new combat cases covering real native hurt calls, source/copy/reload behavior, native pairing and real projectile damage. The review fixtures stay outside the production JAR. The native spell behavior is also documented separately from the Metis fix in the damage-type wiki.

Validation passed: **390 required native tests** on Tensura **2.0.1.3 / NeoForge 21.1.209**; **17 focused native tests** on **2.0.1.4 / 21.1.248**; and **64 JVM tests** in the final clean baseline production build. The independent distribution audit verified the 1.23.1 JAR, all required new combat classes/mixins, native dependency metadata and assets, with no review fixtures or bundled dependencies. Both native profiles reproduced Wind [40, 30], Fire [30, 20], and Water [40, 0].
