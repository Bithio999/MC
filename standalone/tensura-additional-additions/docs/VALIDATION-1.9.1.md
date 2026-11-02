# 1.9.1 — Diminishing Loan Shark income

One active gifted skill transfers 25% of its borrower's permanent EP gains. Each additional active gift adds 75% of the previous marginal share: totals are 25%, 43.75%, 57.8125%, 68.359375%, and so on, approaching 100%. This is equivalent to each gift taking 25% of the EP still remaining. The combined cut is split equally among active contracts, including contracts from different lenders. Gift order does not affect entitlement. Each borrower has a separate stack; idle, recalled and otherwise inactive gifts do not contribute.

The debit still uses the actual effective EP award after native caps and modifiers. No change is made to current-resource regeneration, the existing ledger format, already-earned income, offline credit, player consent, Claim timing or the Chained/Ultimate theft exclusions. Existing gifts use the new rate immediately.

Integration checks cover one gift, two different lenders, three gifts to the same borrower, turning gifts off, recalling the last active gift, separate borrowers, exact income/ledger conservation, native kill awards with Exponentiation, Fusion and native storage-cap crossings. The three-gift scenario uses real Schema, Prospector and Hunter's Recovery copies and native permanent-pool writes.

The opt-in [real-client fixture](review-loan/README.md) checks actual gift acceptance, native skill selection, menu packets and Claim. Its one-gift award is now 240 EP → 180 EP retained / 60 EP transferred. Idle time is explicitly accelerated for its Claim preview.

Baseline validation: **207 native GameTests and 30 JVM tests passed**, with a successful build on Tensura 2.0.1.3 / NeoForge 21.1.209 using the final 75%-retained contribution curve.

Real-client verification passed with actual invitation/Accept and gift/Claim packets, retained mastery, 60 EP transferred from a 240 EP award, and refreshed screenshots at both GUI scales. The release audit excludes the opt-in review fixture.

Tensura 2.0.1.4 / NeoForge 21.1.248 API compilation also passed. This is compilation coverage, not a new full Neo Otherworld modpack runtime test.
