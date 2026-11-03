# 1.9.4 — Dead borrower cleanup

Confirmed non-player deaths retire every outstanding contract for the borrower's UUID. End-of-tick/Report reconciliation handles loaded corpses; entity-removal handling catches killed entities. Closure removes active/borrower indexes and use pulses, while preserving loan history, earned income and queued credit. Player death is excluded, and healthy chunk unloads do not imply death. Report reads the active index independently of closed history size.

An unavailable record can be closed without an extra skill. The server closes the contract and revokes its marked skill copies if the borrower later loads or logs in. The explicit close-only action remains close-only even if that borrower returns before confirmation reaches the server. An unavailable UUID from an older release cannot safely be classified as dead, so those rows require this manual close.

Baseline validation: **216 native GameTests and 30 JVM tests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. New scenarios cover actual subordinate death, multiple loans, retained income/queued credit, restart persistence and replay protection, healthy unload, close followed by native entity NBT reload and marked-only recall, player-death preservation, totem recovery and close-only collateral protection.

The real-client flow passed actual subordinate death/removal in the open Report, retention of an unloaded living borrower, Close record/Confirm close packets, a borrower returning during confirmation, three refreshes without changing the close-only intent, marked-gift recall and absence of collateral. Screenshots were visually reviewed. The standalone scenario resets native post-acquisition sleep before beginning this independent flow.

Tensura 2.0.1.4 / NeoForge 21.1.248 API compilation passed, followed by a clean baseline release build. This is compilation coverage, not a new full Neo Otherworld pack runtime test. Distribution metadata/assets and exclusion of the opt-in review fixture were verified.
