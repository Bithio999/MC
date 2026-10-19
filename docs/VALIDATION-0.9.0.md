# Tensura Myths 0.9.0 validation

Target: Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3,
ManasCore 4.0.0.2 and Java 21.

## Build and gameplay

The build passes 11 JUnit tests and **54 required native GameTests**. The previous
race, skill, NPC, barrier and Duo coverage remains enabled. Aion checks now cover:

- Eight-target cleave limit, its aura cost/cooldown and bounded echo victims.
- Both delayed echoes dealing damage and expiring without additional aura cost.
- Standalone Verdict and empowerment from consuming an existing Horizon.
- Immediate removal of consumed Horizon geometry and its native magic circle.
- Forgetting Aion clearing its passive and pending damage/visuals.
- Checkpoint return restoring recorded SHP; blocked returns preserve the pending
  checkpoint without charging aura, and safe returns charge the correct cost.
- Gravity affecting hostile/unowned projectiles while preserving the caster's shots.
- Native barrier allocation, combat recovery delay and ownership-safe cleanup.
- Native Godly/Ultimate tags, preset/cooldown preservation and the existing
  both-final, EP, awakening, persistence and one-time fusion gates.

The delayed-echo fixture stays within the GameTest world's forced chunks, so its
victim remains loaded for the delayed hits.

## Minecraft client

A real software-rendered development client checks the restored six native skill
categories, plain Godly entry, native Godly/Ascension skill pages and native
Evolution navigation. An isolated development world is used for secondary
selection with confirmation, native secondary evolution and Convergent Awakening. Godly ascension from a mastered Raijin succeeds
through the native Ascend control and returns to the native Godly skill list.
The native character preview, search, requirement scrolling and server-driven
progression remain available. Final layout checks remove an unnecessary candidate
icon over the progress column and prevent the lineage name from overlapping the
native character name.

Aion's gravity core, orbital bands and native Gravity circle render in the actual
client. Updated screenshots are included in the wiki. Headless audio is unavailable
and the client disables its sound device. Remote multi-client gameplay and
long-term balance are not covered by these checks.

## Release and docs

The current gameplay articles describe 0.9.0. Changes remain in the changelog.
Both generated wiki editions, their local links/assets and the offline bundle
are rebuilt. Release: `releases/tensura-myths-0.9.0.jar`, with an adjacent SHA-256
file. Replace the earlier addon JAR on both sides; registry IDs remain unchanged.
