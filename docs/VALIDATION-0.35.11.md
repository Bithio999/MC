# 0.35.11 / standalone 1.1.12 — Schema mastery interval

Schema trains at most once every **600 game ticks / 30 seconds**, shared across aura recovery, Strength hits and Regenerative HP/SHP recovery. The first qualifying event can train immediately. Native aptitude and Chained/Unbound bonuses retain their existing amount per training tick.

The saved `SchemaLastTraining` timestamp is retained, so updates preserve progress and editing, toggling or reloading cannot reset the interval. Inactive builds and events with no actual recovery still do not train. The in-game description and player guide show the new interval.

Validation targets the changed behavior: the complete Schema suite on both native distributions, plus the exact Neo Otherworld integration suite. The new case checks no gain at 599 ticks, gain at 600 ticks, shared gating across all three paths, editing/toggle protection and persisted timing. Normal production builds, JUnit checks, standalone isolation and artifact hashes are verified before publication.

Passed **25 full-mod Schema GameTests**, **25 standalone Schema GameTests**, **55 exact-pack integration GameTests** and **11 JUnit tests** (116 checks). Both production builds, the standalone distribution audit, release hashes and offline wiki archive passed validation.
