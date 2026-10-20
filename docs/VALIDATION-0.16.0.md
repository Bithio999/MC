# Tensura Myths 0.16.0 validation

Java 21.0.12.1+1, Gradle 8.14.3, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2; repository-pinned dependencies.

`gradle build runGameTestServer --console=plain` passed **108 required native
GameTests** and **11 JUnit tests**, zero failures. Final client-only presentation
polish was followed by another successful build, all eleven unit tests and a
native client replay. Server skill behavior did not change after the 108-test
run.

## Elemental execution

The spectrum is exactly **Fire, Water, Lightning, Earth, Wind, Light, Space**.
It uses the seven boost families in Arcane Sovereignty, with Water representing
the shared Water/Ice slot. Ice, Darkness and Gravity are excluded from Thoth's
new cycles. Spellbreak, Mana Crucible and the passive retain their behavior.

The revised Lexicon fixture verifies seven scheduled chapters at ticks
**12, 18, 25, 32, 38, 45, 52**, its **70-tick** visual, unchanged **5,000 MP** cost,
**12-second** cooldown and no aura payment. Its initial visual lookup needed
the established fixture wait for the fresh arena's entity manager to track
new entities; the final fixture retains all assertions.

Four new native tests verify actual scheduled damage-source order and timing,
all seven synchronized bolt styles, **38 Library volleys** at ticks 10–195 in
five-tick steps, the unchanged **200-tick** Library duration and **35-second**
cooldown, bounded visual style values and Demeter's actual retained Ice damage.
Source-observation recipients deliberately record incoming calls without taking
damage, so native hit immunity cannot hide which element the skill attempts.
The separate real-combat fixture keeps native immunity timers intact and
compares full-cast damage against the former cadence on ordinary recipients;
both Lexicon and Library remain within 20% of that baseline. It refills health
to prevent early recipient death, without resetting immunity. No production
code resets native hit immunity to force the faster chapters through defenses.
Repeat runs exposed fixture assumptions: an out-of-range baseline recipient
was outside the forced ticking chunks, and a pre-existing Mirror projectile
fixture could begin before its projectiles were tracked. The baseline now forces
its own chunks and verifies ticking readiness; the projectile fixture waits for
tracking with stationary shots before applying its measured velocities. Combat
assertions and production reflection behavior remain unchanged. The shared
recipient wait also verifies the UUID index used by Threadweaver’s tether,
rather than treating presence in the spatial query index alone as readiness.

Existing targeting limits, ownership, movement, resource conservation,
Mana Crucible's held lifecycle, passives, barriers, progression and migration
coverage remain passing. Lexicon uses 375 raw damage per chapter and Library
160 per bolt; native defenses and hit immunity still determine actual loss.

## Client visuals

The native development client was exercised in the isolated **Myths Validation**
world at 1280 × 800 with software OpenGL. Actual casts through learned/equipped
Thoth instances produced seven Lexicon captures and seven Library captures,
using a temporary durable recipient for sustained Library fire. Capture polling
used native visual age for Lexicon and the server-authored volley plus actual
bolt style for Library, rather than assuming wall-clock frame timing.

The client review covers the matching native elemental circles, flame crowns,
caustic Water ribbons, branching lightning, stone tablets, wind helices,
radiant spokes and spatial fractures; rotating book emitters and their
seven-element bolt/impact styles, with book colours following the actual server
volley instead of guessing its phase from client age; unchanged spell durations and Library
cleanup. Library flashes now fade before the next volley to avoid the previous
element's bright trail obscuring the current one. The Thoth renderer also uses
the shared material batch for its additional geometry. The existing seals,
animated books, Spellbreak and held Mana Crucible formation remain.

The temporary recipient was removed and camera/HUD state restored. The user's
**New World** was untouched. Helpers remain in `/tmp`, outside the release.
There were no new renderer exceptions. Existing base-mod model warnings,
unavailable OpenAL and development authentication-key failures are unrelated.
This is a functional software-GPU check, not a hardware performance benchmark.

## Wiki and release

The Thoth article, ability index, damage reference and visual gallery describe
the seven-element cycle, exact chapter times, new bolt cadence, native damage
IDs and unchanged durations. Current native screenshots replace the prior
Lexicon image and add Space/Library views. Both generated wiki editions and
the offline bundle are updated; release history remains in the changelog.

Packaging verifies all 288 desktop/mobile page views, local references, image
loading, search/ability anchors and exact offline ZIP contents. The release
JAR matches the build byte-for-byte, includes the common spectrum class and
updated combat/renderer classes, and excludes attach helpers/dependency JARs.
The separate GitHub Wiki remains unavailable to this integration; its prepared
Markdown edition is committed under `wiki/github`.

Release SHA-256: `916f9f6e7bed3a647a1cd939735e4a5712d7ce523c7ad2baf30a01f17cfc1ac8`.
