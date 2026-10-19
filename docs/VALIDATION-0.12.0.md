# Tensura Myths 0.12.0 validation

Target: Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and Java 21.

## Gameplay coverage

The release runs 11 JUnit tests and 70 native GameTests, including the existing hunter, skill, barrier, Godly, Duo and awakening coverage.

New checks exercise:

- Worldline Sever steering toward a changed aim without an instant snap, matching the same elapsed time at 20 and 60 updates per second, and remaining valid during a half-turn.
- Actual beam damage passing through source water and lava, striking a target behind them and stopping before a target behind a solid wall.
- Causal Reversal preserving improved current HP, SHP, aura and magicules on manual return, and preserving improved reserves on automatic return. Earlier checkpoint-payment, restoration, cap and suppression tests remain enabled.
- Concordance Pulse restoring half of maximum HP and SHP on a large fused player, including healing well over 80 HP, paying 1,500 aura, respecting its 30-second cooldown and stopping at pool maxima.
- Secondary Daemon progression through Greater Daemon, Arch Daemon, Daemon Lord and Devil Lord using the native requirements and added EP gates.
- Real native naming of three eligible mobs leaving the player unnamed and unable to satisfy the player's own naming requirement. Receiving a native name then counts for the secondary race.
- Preserving Daemon Lord's native 50+50+50 weighted alternatives, and requiring the separate physical-body contribution for Devil Lord. The primary hunter race remains intact.
- Auditing **71 native race stages and 65 evolution branches across all 15 ancestral families**. The menu exposes every native option, agrees with native weighted readiness at the added EP gate and rejects the same candidates one EP below that gate.

The native naming fixture uses wolves from Tensura's `can_be_named` entity tag; vanilla zombies are not in that tag. Combat fixtures outside their small structure bounds explicitly retain their target chunks and allow chunk tracking to settle before checking attacks. The Horizon pull expectation accounts for the target's native knockback resistance. These are test-fixture changes, not changes to the older combat abilities.

## Native source audit

`TensuraRace.getEvolutionProgress` sums weighted requirement contributions and caps the displayed total at 100. `DaemonLordRace` assigns 50 points each to awakening, being named and having a physical body; `DevilLordRace` assigns 50, 25 and 25. The addon preserves those weights rather than making all Daemon Lord alternatives mandatory.

`EvolutionRequirement.NamedRequirement` checks `TensuraStorages.getExistenceFrom(player).getName()`. `RequestNamingMenuPacket.name` sets the recipient's native name; naming other entities awards the namer a separate statistic. It does not name the namer. Named state, awakening, body form, skills, boss history and inventory/stat requirements are evaluated from the player, while the native predecessor is the independently stored secondary race instance.

The added cumulative EP gate remains independent of native progress. Selection at 20M requires 22.5M, 25M, 27.5M and 30M for the four secondary Daemon steps, with native conditions still checked. The UI lists met/unmet contributions and the added EP gate, refreshes read-only state every second and preserves scrolling, search and ancestry confirmation.

## Live client and documentation

The native Secondary Race tab shows Arch Daemon → Daemon Lord at 27.5M EP with 50 native points when unnamed and in spiritual form. Receiving a native name updates the still-open menu to 100 points and enables the action without resetting the view. The actual Evolve button successfully creates the secondary Daemon Lord. A screenshot of the live weighted checklist is included in the wiki.

A fused player with 1,020 maximum HP and 9,620 maximum SHP starts at 25% of each pool. Concordance Pulse raises the live values to **765 HP and 7,215 SHP** (75%), restoring **510 HP and 4,810 SHP** in one activation. The live fixture is creative; survival cost and cooldown checks run in the native GameTests.

Worldline Sever is held through native input while the client aim turns 60 degrees. Reading the actual rendered direction shows the remaining angle decay through approximately **19.9°, 6.7°, 2.1°, 0.53° and 0.18°**, rather than snapping to the new aim. The beam remains visible and releases normally. Both sides use the same 60 ms steering response and solid-only ray helper. Automated damage checks cover fluid penetration and solid occlusion.

The software-rendered cloud client has no audio device. Remote multi-client latency and long-term balance are not covered by these checks. The styled wiki, GitHub Wiki Markdown mirror and offline ZIP are rebuilt. Checks pass for **6,551 local HTML links/assets**, **1,411 GitHub Wiki cross-page links**, and **276 desktop/mobile page checks**, plus two targeted checks after adding the live checklist screenshot. Images load, ability search resolves its anchors, and there are no horizontal overflow or JavaScript errors. All **558 offline wiki files** match their source/generated counterparts.

## Release

The addon remains in the `wildhunt` namespace for save compatibility. The 0.12.0 JAR, SHA-256 file, gameplay wiki, GitHub Wiki Markdown mirror and offline ZIP are included in the repository. Update both client and server and remove the older addon JAR. Development inspection helpers stay outside the release.
