# Tensura Myths 0.25.0 validation

Validated 21 October 2026 using Java 21, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2.

## Native gameplay

The full native suite passed **185 required GameTests**, including **14 new boss
checks**, with zero failures. **All 11 JUnit tests** and the release build passed.
The existing player progression, skill, balance, Lugh, Aion and UI packet tests
remain included.

Ananke's focused checks verify:

- Actual **48,000 HP**, **120,000 native SHP**, **15M maximum aura + 15M maximum
  magicules**, Eclipse Seraph ownership, fifteen explicitly mastered skills,
  native Ultimate Moira and Godly ownership through Thoth. Initialization cannot
  refill an already initialized boss. Negative-mastery native learning records
  are not accidentally mastered.
- Default native reincarnation pools exclude the boss race. Moira is absent
  from the starting-skill pool and cannot be slotted by players.
- A paid native cast consumes magicules and starts its actual per-mode cooldown.
  Fatebreaker waits for its warning and hits its fan, leaving targets behind it
  unharmed. Starfall warnings lock independently as their target moves, keep
  earlier positions, and damage at those positions after their warning.
- Eventide Procession leaves its hollow center safe and hits each recipient
  once. Last Dawn waits 75 ticks, damages its dangerous area, and leaves the
  7–12-block refuge and targets outside its 32-block sphere unharmed.
- Long-range starfall render bounds include the saved marker and its 20-block
  sky lance. Boss visuals use their separately registered 192-block tracking
  type; ordinary skill visuals keep their 128-block tracking type.
- Countermand cancels an Ultimate rite and its owned visual without removing
  Godly ownership. Native activation checks remain authoritative.
- Save/load preserves wounds and depleted native resources while cancelling
  pending rites. Health modifiers still affect this boss, other entities retain
  the native 10K health cap, and the boss has no incoming 750-damage hard cap.
- Live AI reaches phase III, casts Last Dawn through the real native skill
  instance, and uses Astral Library. The fixture runs normal server ticks rather
  than calling a mock damage routine.
- A blocked ritual consumes nothing; a successful ritual consumes its writ;
  a duplicate nearby ritual fails without consumption. Native progression boss
  tags are present. The trophy restores its documented resources, consumes one
  item and starts its native item cooldown.
- The native IArenaBoss adapter records its fight ID and excludes unjoined
  players, including through the shared targeting used by auxiliary Myths arts.

A pre-existing manually held Mana Crucible fixture now refreshes its fake
player's hold heartbeat while waiting for the visual to enter the native entity
index. This prevents its own delayed check from expiring the channel; the
production conversion code is unchanged.

## Native client

Opened only the isolated **Myths Validation** save. Checked the original model,
articulated wings, emissive atlas, rotating halo, eased cast pose, all four rite
visuals and both native bossbars. The final bars render at the upper right using
Minecraft's sprites and native progress, clear of Tensura's player HUD.

The gallery contains six 1280×720 PNG captures and an eight-second 1280×720
Last Dawn clip. Warning stills pause actual native cast plans; the clip shows
the running cast from charge through impact and cleanup. Confirmed the gold
refuge remains distinct rather than washing out into a solid white floor.
The final boss-visual entity registration was also checked in the native client.
Temporary instrumentation and helpers remain outside the repository and JAR.

## Documentation and release

- Research compares pinned base Tensura bosses/profiles and inspected Nightmares
  and Mysticism boss implementations. These addons were inspected separately,
  not installed as runtime dependencies or claimed as jointly tested.
- **302 responsive article checks**, **70 ability anchors**, boss infobox/gallery,
  The Last Dawn search and the existing Skill Bank/media checks pass without
  browser page errors.
- **151 HTML views** have **8,191 resolving local references**. All **658 wiki
  files** match the offline ZIP byte for byte. The prepared GitHub Wiki edition
  is included in the main repository; the separate Wiki service is unavailable.
- The release's **127 compiled classes** and all PNG/JSON/TOML resources match
  the final build byte for byte. No third-party dependency JARs or temporary
  graphical helpers are bundled.

## Scope

The encounter targets strength above the inspected base-mod defaults. This is
not a universal ranking against configured servers or every addon boss. Native
combat configuration can change actual damage, recovery and defenses. No full
multiplayer raid, remote dedicated-client connection or shader-pack benchmark
is claimed. The client validation uses local software rendering; its clip has
no audio. Gameplay articles describe current mechanics, with development history
kept in the changelog.

Release JAR SHA-256:
`5141bcac11f7585ed5850300915959dbd620e84262e23765efed9614e5a17e0d`.
