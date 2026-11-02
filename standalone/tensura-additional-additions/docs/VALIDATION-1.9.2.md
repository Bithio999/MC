# 1.9.2 — Claim copying

Claim always recalls its marked gift. Once the borrower has left the gift unused for ten uninterrupted minutes, an additional eligible skill is awarded with retained mastery. Claim reads Tensura’s live `skillSteal` gamerule from the borrower’s level: true removes the original; false leaves it untouched and grants a fresh permanent copy. Acquisition restrictions, Chained/Ultimate exclusions, temporary-skill exclusions and already-owned filtering apply in both modes. Existing contracts need no migration.

Copied rewards use a **Copied** history status; the existing reward identifier field remains compatible with older ledgers. Lender confirmations distinguish copying from stealing. The server rechecks the gamerule on Claim, independently of client text or the setting when a gift was issued. Recipient invitations remain unchanged.

New native regression scenarios cover a player accepting a gift, changing the rule after issuance, recalling at exactly 12,000 idle ticks, preserving the donor’s original object/mastery, retaining mastery in the new permanent copy, stripping private/loan data, saving the copied history and rejecting replay. Separate copy-mode checks cover early and active recalls plus Chained and all registered Ultimate exclusions. Existing steal-mode tests still verify donor removal.

The optional real-client fixture supports `-PloanReviewCopy=true` to exercise actual Claim packets with the gamerule off, verify both owners retain the reward and capture the copied report.

Baseline validation passed: **209 native GameTests and 30 JVM tests**, with a successful clean build on Tensura 2.0.1.3 / NeoForge 21.1.209.

Real-client verification passed with the actual invitation/Accept, Loan and two-step Claim packets under `skillSteal false`. The new owner receives Hunter’s Recovery at mastery 321, the donor retains theirs, the report shows copy eligibility at both GUI scales, and the completed record shows **Copied**. The 240 EP award still splits 180 retained / 60 transferred. Screenshots were visually reviewed.

Tensura 2.0.1.4 / NeoForge 21.1.248 API compilation passed, followed by a clean baseline release build. This is API compilation coverage, not a new full Neo Otherworld runtime test. The distribution audit verifies the release metadata/assets and excludes the opt-in client fixture.
