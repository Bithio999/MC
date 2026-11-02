# 1.9.3 — Exponentiation and skill menus

Exponentiation uses a central ^1.2 transform across the existing supported attributes and terminal outputs. Fractions at or below 1 remain unchanged. Native chance/stack limits, movement and energy-capacity exclusions, raw calculation scopes and single-application guards remain in force. Integer XP/drop quantities round down. Oxygen uses seconds before conversion back to whole ticks. Fusion uses the matching inverse exponent when combining temporary capacities. A new migration marker clamps legacy over-cap current reserves to the reduced capacities without deleting permanent EP gains.

Loan Shark now overrides native mode cycling in both directions. Loan and Report open separate fixed-mode screens; menu tabs and tab-switch requests were removed. The server's Loan list contains only learned permanent lendable instances, excluding unfinished entries, sub-instances, temporary gifts and Loan Shark itself. Report contains only outstanding contracts. Claim closes a contract and removes its report row while keeping the authoritative record for replay protection and earned-income accounting.

Background polling no longer clears confirmation or disables Claim. User actions queue behind an in-flight poll; mutation/search requests disable their controls while awaiting their own response. Confirmation survives timer updates, and the completed Claim result remains visible when the active list becomes empty.

Prospector's RenderGui ore-name box handler was removed. The world-space colored markers, glyphs, loaded-chunk survey and refining behavior remain available. Loan Shark has an image-generated master imported as a 64 × 64 pixel texture using nearest-neighbor sampling.

Baseline checks: **211 native GameTests and 30 JVM unit tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. Native regressions cover the ^1.2 damage, recovery, capacity, EP, mining, learning, loot and oxygen paths, Fusion, owned-skill filtering, native Loan/Report cycling, closed-report removal and both Claim gamerule modes.

Real-client Loan Shark verification passed: native equip and Loan/Report/Loan mode packets, separate menus without tabs, fully learned owned rows, acceptance/mastery, 240 → 180/60 EP sharing, confirmation active through three timer refreshes, and a copied Claim removing its row from the active Report. Both GUI scales were captured; the 64px icon was inspected in the native UI. Claim preview time is deliberately accelerated by the fixture.

The real-client Exponentiation check passed native equip/unequip packets, client/server ^1.2 capacity and attack-speed synchronization, healing/resource gains, unchanged movement, NBT restoration and over-cap cleanup. Current native attribute measurements are included in the attribute audit JSON.

Tensura 2.0.1.4 / NeoForge 21.1.248 API compilation passed, followed by a clean baseline release build. This is compilation coverage, not a complete new Neo Otherworld pack runtime test. The distribution audit excludes the opt-in client fixtures and validates the bundled assets and metadata.
