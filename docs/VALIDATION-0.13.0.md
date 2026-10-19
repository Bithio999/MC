# Tensura Myths 0.13.0 validation

## Build and automated gameplay

Java 21.0.12.1+1; Gradle 8.14.3; Minecraft 1.21.1; NeoForge 21.1.209;
Tensura 2.0.1.3; ManasCore 4.0.0.2. Builds use the repository's pinned dependencies.

`gradle build runGameTestServer --console=plain` completed successfully with
**90 required native GameTests passed**. `gradle build --console=plain` passed
again after the final client title/book-mesh adjustments, including all **11 JUnit
tests** (5 progression, 3 balance, 3 Duo; zero failures).

The new suite covers:

- Eight new native registrations, translated names, native mode counts, exact
  Unique/Godly/Ultimate tags, reincarnation inclusion and retired-ID exclusion.
- Both mastered ingredients and fixed EP for synthesis; unrelated skills, all nine
  presets, inherited cooldowns and failure without consuming ingredients.
- Old Ultimate migration, retained mastery fraction, cleanup of old barrier
  modifiers, preset references and idempotence.
- Thread leash radius/pull/sever, mine arming and one-shot activation, no phantom
  remote detonation costs, ally/owned-pet healing, light splitting and native Webbed.
- Incoming projectile reflection and owner transfer; friendly outgoing shots
  remain unchanged.
- Piercing plasma and delayed fuses, all 21 delayed explosions from a complete
  Vritra combo, bounded cataclysm telegraphs, orchard
  consumption, dodgeable fixed crystal blooms and owner-specific ward expiry.
- Safe gate destinations and preserved momentum, mirrored enemy displacement,
  and the warning interval before spatial damage.
- Thoth's additional mastered Rune Smith requirement, three typed elemental
  chapters, fixed magicule costs, capped two-aura-to-one-magicule conversion,
  non-Godly drain/disruption, Godly exclusions, mobile library target cap and
  one-pulse scheduling.
- All new native toggles, actual attribute cleanup when unfunded, delayed native
  barrier recovery, and cancellation of autonomous/delayed attacks and owned
  visuals on forgetting or lifecycle cleanup.

Existing coverage retains Aion, the four original Uniques, hunter races,
regeneration, NPCs, native barrier behavior, primary/secondary EP split, native
race requirements and all 105 registered fusions. Obsolete tests of the removed
Heaven's Thunder and Heavenfall signatures were replaced by the new combat suite.

## Fixed race requirements

The secondary origin and recorded previous EP are not used in either formula.
Tests inject old origin/last-evolution values up to 999M while confirming a fixed
12.5M default Human/Hunter + final Slime awakening gate. Changing current EP does
not change the result. A native preceding evolution configured to require 40M
still raises awakening to 50M.

The native secondary-branch audit still traverses **71 stages / 65 branches**
across all 15 supported families. It checks shared server/menu readiness and
native weighted requirements. The live naming distinction (receiving a native
name, not mass-naming other entities) is unchanged.

## Test fixture corrections

New combat arenas follow the runner’s fresh test region and keep target chunks
forced. They wait until all fixtures are accessible in the entity manager,
preventing asynchronous loading or mobs saved by an earlier run from changing
target-cap assertions. Resource-drain checks set exact reserves after that wait,
so native regeneration during setup cannot contaminate the measured cast. The existing
proximity-slow test now schedules a strictly future five-tick aura boundary,
rather than accidentally scheduling its callback for the tick already being
processed. Its mob fixtures keep their chunks active. Gate unit simulation uses
an ordinary living caster because NeoForge's FakePlayer connection intentionally
does not apply the teleport; a real connected player was also checked below.
These changes preserve behavioral assertions instead of weakening them.

## Real client

A software-rendered Minecraft client ran the complete addon in the isolated
**Myths Validation** world. The separate existing **New World** was untouched.

- Used the real native Ultimate → Synthesis control and Combine buttons to obtain
  Vritra, Demeter and Janus from both mastered ingredients.
- Used the real Godly Ascension menu to obtain Aion and then Thoth; both appeared
  in the Godly list with correctly centered icons and native skill controls.
- Cast Astral Library using the normal native ability key; its cooldown and
  orbiting geometry appeared. Upright open-book meshes and header title spacing
  were refined after the visual inspection and checked in a fresh client.
- Real-player Twin Gates moved from `(20.5, -60, 20.5)` to its saved exit near
  `(56.464, -58.382, 20.5)`, preserving velocity `(0.3, 0.2, -0.1)`. A native entity
  motion packet synchronizes that preserved momentum to the client.

Development attach helpers live only in `/tmp`; they are not shipped in source
or the release JAR. Expected cloud limitations are the missing audio device and
Mojang public-key lookup in offline development, not addon failures.

## Wiki and artifacts

The gameplay wiki contains **142 articles / 143 pages including Home**, and
**50 ability entries**. It includes the eight new skill infoboxes, exact native
damage IDs for each ability, combination recipes, fixed EP formulas and current
menu instructions. Development history and migration details stay in the
repository README/changelog rather than the gameplay articles.

The static browser pass checks every page at desktop (1440px) and phone (390px)
widths: **286 responsive checks**, loaded images, no horizontal overflow and no
JavaScript errors. Search covers skills and individual ability anchors.
Local HTML and prepared GitHub-Wiki links are checked after regeneration. All
nine documented damage IDs are verified against Tensura’s actual registry data
(including the generic magic source `tensura:magic`).

Release artifacts: `releases/tensura-myths-0.13.0.jar`, its SHA-256 file and the
complete offline wiki ZIP. Normal build output is compared with the released JAR;
the archive is checked against current wiki sources. Previous releases remain
available. The separate GitHub Wiki service is still unavailable to this
integration; repository wiki sources, prepared GitHub Markdown and the offline
styled edition are updated together.
