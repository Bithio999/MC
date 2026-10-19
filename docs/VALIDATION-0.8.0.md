# Tensura Myths 0.8.0 validation

Target: Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2,
Java 21. Previous `wildhunt` registry IDs are preserved.

## Build and automated gameplay

`gradle build runGameTestServer` passed **11 JUnit tests** and **50 required native
GameTests**. A final build after client presentation fixes also passed. The suite
retains the 38 earlier gameplay checks and adds 12 Myths checks:

- Registration of all 105 distinct named/profiled fusions in the native race registry, with native attribute metadata.
- Secondary base selection, native Slime → Demon Slime → God Slime requirements and native NBT roundtrip.
- Exact native kill-award weights for a default primary plus half Slime base weights.
- Both-final evolution gates, actual registered Human/Slime fusion, one-time 25% energy reward and native fused-race NBT roundtrip.
- Awakening/EP/same-ancestry selection rejection and prevention of secondary swapping.
- Native Ultimate plus Godly tags, source replacement, ninth-preset remapping and cooldown preservation.
- Rewind position/HP recovery with energy charged and full return cooldown.
- Secondary death clone without shared mutable NBT; preservation of the Holy Divine Human primary's growth split.
- Five-target piercing ray cap, aura cost and cooldown.
- Verdict rejection without a field, consumption of its own Horizon and transient delayed-damage cleanup.
- Passive native barrier allocation, no repair during combat delay, timed recovery and ownership-safe cleanup.
- Secondary Slime native hold/release charging and launch behavior.

The long-range Railbolt fixture now runs away from neighboring GameTest entities;
its twelve-block range assertions and gameplay code are unchanged.

## Actual Minecraft client

A software-rendered Minecraft client loaded the addon and an isolated copy of the
existing development world. Checks included the seven-category native skill
layout, Godly list/title/icon, native skill details and toggle, native slot
assignment, successful Godly ascension through the new networked menu, secondary
Slime progression, and successful Convergent Awakening through the menu. The
fusion changed base EP from 20M to 25M and retained that reward and its own
growth profile after reopening the world.
The new progression panel's duplicated background blur was fixed during this
check. Final-state presentation identifies the two lineages as unified.

The headless environment has no audio device; the client disables its sound
system. This does not prevent the gameplay/UI checks. Simultaneous remote
multiplayer clients and long-term balance are not covered by this validation.

## Wiki and release

Both generated wiki editions contain **128 gameplay articles**, including all
105 fusion race pages. Local HTML links, assets, fragment targets and exported
GitHub Wiki cross-links were checked. Chromium checks covered the Aion, Duo and
Human/Slime race articles at desktop width, the full fusion catalog at mobile
width, image loading and searching for Aeonic Protean, with no page errors or
horizontal page overflow. Gameplay articles contain reference material; changes
are recorded separately in the changelog.

Release: `releases/tensura-myths-0.8.0.jar`. Its adjacent `.sha256` file identifies
the packaged JAR. The offline documentation is
`releases/tensura-myths-wiki-0.8.0.zip`.
