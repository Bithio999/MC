# Tensura Myths 0.26.0 validation

Validated 21 October 2026 against Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3 and ManasCore 4.0.0.2, using Java 21.

## Native gameplay

The release suite passed **198 required native GameTests**, including thirteen new Ananke support/tactics checks and the existing encounter, player skills, progression and UI coverage. All **11 JUnit tests** and the release build also passed. The final run reported zero required failures.

The new native checks cover:

- Eighteen learned/mastered skills and enabled actual stored support instances. Repeated initialization cannot multiply the Space or perception bonuses. Command-style spawning without `finalizeSpawn` also receives the package.
- A reconstructed 0.25 saved state with disabled support flags and existing ghost modifiers migrates once, keeps wounds/resources and retains the same Space/dodge values through a second reload. The shipped existence profile now provides stats only, avoiding duplicate skill initialization.
- Native Physical, Spiritual, Magic, Holy, Light, Darkness and Spatial Resistance callbacks classify their reference sources and protect while ON; OFF comparisons permit them. Native poison immunity and pain suppression operate.
- Ordinary versus Godly source ownership preserves native defense differences without modifying stored toggles. Actual Myths Ultimate and Godly hit paths damage the repaired boss while leaving its defenses active. These are compatibility probes, not player time-to-kill benchmarks.
- A distant Fatebreaker attempt spends no magicules and starts no cooldown. AI selects a usable ranged rite or waits when none is available.
- Repositioning cannot spend/move into blocked candidates or relocate an active telegraph. A successful shift spends 20K MP and closes to ten blocks. The entity applies a ten-second cooldown and a one-second reaction interval.
- Starfall executes all six phase-one impacts, then releases its movement lock while its visual tail remains. Encounter cleanup removes that tail. The fixture waits for native entity indexing before advancing its clock.
- Renewal waits four seconds after damage and two more seconds after activation before its first pulse; four bounded pulses each restore 1,440 HP / 3,600 SHP on the default boss and each cost 144K MP. A repeated call in the same frame cannot duplicate a pulse. The start costs another 5K MP.
- Accepted damage, SHP-only damage and Countermand stop recovery. Native Frost/healing suppression and insufficient magicules prevent it. Reload cancels the pending action and cannot erase its cooldown.
- Existing live phase-III AI still chooses its ready, in-range Last Dawn and uses Astral Library. Its damaging values, 48K HP, 120K SHP and 30M EP remain unchanged.

## Native client

Opened only the isolated **Myths Validation** save in the native development client. Confirmed Unwritten Renewal's converging scripture, core lights, casting pose and native bossbar label. The gallery capture pauses a real AI-triggered recovery during its windup; it is not an external render. Native full-health attacks and Thoth continue using their established renderers. Also confirmed Spatial Reposition through the live AI, including its destination rings and reaction interval. Both new captures are 1280×720. No third-party shader pack was used.

## Documentation

Both wiki editions and the offline archive are regenerated. Two utility actions are indexed with explicit **None** damage types. Browser validation passed **302 desktop/mobile article checks**, **72 ability anchors**, image loading, search and zero page errors. The final edited home/Ananke/install views were checked again after adding the captures; all **8,200 local HTML references** resolve, and the **660-file** offline archive matches its source files byte for byte.

The 0.25.0 audit is preserved as version-specific evidence; its findings are not rewritten to imply the released version already behaved correctly. Gameplay pages describe the current package without development history.

## Limits

Native source/configuration rules remain authoritative. Godly defense bypass still works. No other addons were installed to benchmark this release, and automated compatibility checks do not establish a universal strongest-boss ranking or replace endgame playtesting.

Release artifact: `releases/tensura-myths-0.26.0.jar`, SHA-256 `af7480d5871f057c8a640749753e357ab9485da608c748307aa953922fc195a0`. The JAR includes the recovery/tactics implementation and excludes the archived audit probe and temporary audit namespace. The [original audit](research/ANANKE_SKILL_PACKAGE_0.25.0.md) records the repaired initialization issue.
