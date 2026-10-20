# Tensura Myths 0.14.1 validation

Java 21.0.12.1+1, Gradle 8.14.3, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2; repository-pinned dependencies.

`gradle build runGameTestServer --console=plain` passed with **99 required native
GameTests** and **11 JUnit tests**, zero failures.

Mana Crucible coverage checks native skill-storage activation, old cooldown
bypass and cleanup, the -1 unlimited-hold sentinel, 5% maximum MP per second,
exact 2:1 conservation, no activation payment, no duplicate transfer in one
game tick, conversion beyond the former 20,000 cap, current-pool bounds,
fractional final aura, no-cost invalid starts, immediate restart, owned visual
following/cleanup, release, lost-heartbeat cancellation, lifecycle/forgetting,
and mastery awarded only for sustained successful conversion. Thoth ascension
retains other modes' inherited cooldowns while leaving Crucible cooldown-free.
The native activation fixture equips the channel in the active preset, as
required by Tensura's activation event; direct method calls alone do not verify
that native requirement. Existing combat and progression coverage still passes.

The fresh client was tested in survival in the isolated **Myths Validation**
world with the native Z skill key. Held conversion increased MP and reduced
aura at the expected 2:1 ratio, apart from small native natural regeneration.
After release, the channel tag and visual were removed and cooldown was zero.
The visual remains active beyond its former two-second duration while holding.
Native exhaustion sleep and suppression remain activation restrictions; no
exhaustion bypass is added. The user's New World is untouched. Attach/Robot
helpers remain in `/tmp` and are excluded from the release.

Release SHA-256: `5a80deb5b42f9b7543e79cea5788cf3d8c6f8504ff6bdcd002f14ecc2d23d3e7`.

The wiki was regenerated with the held controls, conversion formula, natural
capacity limits and the ascension cooldown exception. All 286 responsive page
checks pass, with no overflow, broken images or JavaScript errors; 6,969 local
HTML references and 1,465 exported cross-page links resolve. All 588 offline
ZIP files match the current wiki. The release JAR matches the tested build
byte-for-byte, contains the new channel class, and excludes attach helpers.
