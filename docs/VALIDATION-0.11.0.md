# Tensura Myths 0.11.0 validation

Target: Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and Java 21.

## Build and gameplay

`gradle build runGameTestServer` passes **11 JUnit tests and 64 required native GameTests**. The final UI refinement is also built with `gradle build` and inspected in the development client.

New and revised checks cover:

- Aimed Worldline Sever pulses, eight-target cap, magicule billing, native hold/release and exhaustion behavior.
- Event Horizon’s stronger 0.40 pull outside its non-damaging outer area, while retaining the 10-block damaging core and projectile ownership protection.
- Nearby native Poison Nullification rejecting an effect before suppression, admitting poison while suppressed, and protecting again after the aura expires. Saved native toggle flags remain enabled throughout.
- Spatial Nullification admitting another attacker’s damage while the target is suppressed, plus the original native full damage path and attribute-resistance bypass.
- Projectile slowing by proximity, preserving own shots, preventing repeated multiplication, and restoring the aura’s velocity factor on exit.
- Free Reversal recasts at zero magicules, recording resources after the initial 2,400 payment, safe position/HP/SHP recovery, blocked manual returns and lifecycle cancellation.
- Armed Reversal surviving native temporary sleep/suppression and returning automatically with no magicules.
- Automatic Reversal returning once, restoring within reduced current energy caps, and recovering in place when its anchor is blocked.
- Awakening rejecting insufficient lineage-dependent EP, agreeing with its network/menu snapshot, succeeding at the exact gate, applying its energy reward once, and retaining native saving and growth.
- A 25M completed evolution gate producing a 31.25M awakening threshold; earning more EP does not raise that threshold. A native God Slime evolution gate configured to 40M raises awakening to 50M. Existing Duo state without the new last-gate field retains its completed growth requirement.

All earlier hunter, reincarnation, barriers, aura regeneration, otherworlders, Godly classification/presets and Duo gain tests remain enabled. The Reversal position assertions use a real living entity: NeoForge’s fake-player connection intentionally stubs teleports.

## Minecraft client

The native Secondary Race and Awakening tabs show their selected connected frame; Status retains an inactive frame while either is open. Validation also removes residual frame pixels from the formerly selected Status tab. The frame is copied from Tensura’s own background, including its panel bevel.

In an isolated validation world, Sovereign Hunter / God Slime at 25M base EP shows a 31.25M awakening requirement and a disabled action. At exactly 31.25M, the action becomes available and the actual button awakens the player as Aeonic Protean, applying the 25% one-time energy boost.

Worldline Sever renders at the crosshair through the native key press/hold/release path. Its owner is synchronized so only the local caster’s mesh predicts current view direction every frame; hit detection stays on the server. Remote players continue to use the synchronized beam.

A real server player also exercises Reversal through the native skill storage activation entry point. Starting from 1,000,000 magicules, the checkpoint records 997,600 after casting. The player moves ten blocks, magicules are set to zero with aura retained, and a free manual return restores the original position and exactly 997,600 magicules. The timed healthy-player checkpoint also automatically returns after twenty seconds, consumes its snapshot and starts cooldown. A second live automatic checkpoint starts with magicules deliberately depleted after arming and still returns to its recorded position with the final suppression fix. This verifies player teleport synchronization as well as the automated entity checks.

The cloud client has no audio device. Remote multiplayer latency and long-term balance are not covered by these checks.

## Documentation and artifacts

The gameplay wiki documents the updated Reversal resources, all-source defensive suppression, projectile slow and dynamic awakening formula; all 105 fusion articles link to the lineage-dependent requirement. Ability damage types remain explicit.

The styled wiki and GitHub Wiki Markdown mirror are rebuilt. Checks pass for **6,548 local HTML links/assets**, **1,411 GitHub Wiki cross-page links**, and **276 desktop/mobile page checks**, including loaded images, no horizontal overflow, no JavaScript errors, and ability search anchors. The offline ZIP is checked against every generated/source wiki file.

Release: `releases/tensura-myths-0.11.0.jar`, its SHA-256 file and `releases/tensura-myths-wiki-0.11.0.zip`. Registry IDs remain unchanged. Validation helpers and development reload agents stay outside the repository and are not bundled. Replace the previous addon JAR on both client and server.
