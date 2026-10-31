# 1.6.1 — Combined Fusion HP/SHP and menu verification

Fusion adds both participants’ **maximum HP and SHP** and **remaining HP and SHP**. These pools use addition; the 1.5× multiplier applies only to EP. Both subordinate and consenting-player control roles use the same combined vitals. On separation, original maximum capacities return and each remaining pool is distributed in proportion to those original capacities. This preserves total remaining health and prevents free healing or duplicated HP/SHP.

The saved recovery marker has a versioned vitals format. Older 1.6.0 orphan markers retain their original recovery interpretation; existing worlds do not need resetting. Three-minute duration, five-minute Paralysis II/Fragility II, damage, resource regeneration and ownership/consent rules remain as before.

## Verification

- **169 native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. New checks cover combined maximum/current HP and SHP, preserving damage across separation, restoring original capacities, and both control roles.
- **30 unit tests passed**, and the production distribution audit excludes optional client review classes.
- **Actual native client menu review passed:** for subordinate control and receiving-player control, borrowed Martial Master appeared under Skills → Unique and borrowed Aura Slash appeared under Battlewills. Actual list/slot mouse handlers sent the native slot packets; the server accepted the selected borrowed skill. After separation, the borrowed entries disappeared from the client’s skill storage.
- The player donor in this review was a server-side test player. The receiver and menu/packet flow used the real client; this was not a two-human-client playtest.

Tensura’s selection screen includes temporary skills and supports `f:temporary`. No replacement menu or menu-filter bypass was needed. Active borrowed abilities can be slotted normally; passive/toggle abilities retain their normal rules.

[Menu review reproduction](preview-fusion/README.md) · [Previous validation](VALIDATION-1.6.0.md).
