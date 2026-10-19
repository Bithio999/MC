# Wild Hunt 0.7.0 validation

Validated against Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3,
ManasCore 4.0.0.2 and the pinned runtime dependencies in `runtime-mods.lock.json`.

- `./gradlew build runGameTestServer`: successful; 8 JUnit tests and 38 required
  native GameTests passed. No disabled or skipped tests. The game tests cover
  native evolution grants, names, HP/SHP migration, real kill-EP gains at all
  stages, the aura/MP split, recovery speed and payment, partial aura recovery,
  healing suppression, funded lethal recovery, native NPC spawning and every
  loadout skill, save/load, normalized spawn probabilities, block-ignoring
  ricochets with a block-clipped initial shot, preserved teleport momentum,
  cursor placement, barrier breakage and delayed automatic recovery.
- The long barrier test waits beyond ten seconds, checks no early repair, adds
  a second hit and confirms that it postpones repair. Its durable creature uses
  valid native max-energy attributes so energy overflow cannot kill the fixture.
- Minecraft development client: both NPCs and their bundled skins render; the
  travelling foam-crested wave and rotating Undertow render in world. Native
  registration, world loading and resource reload succeeded. Unmodified client
  screenshots are included in `wiki/media/`. A fresh client also grants all three
  recovery tiers with their proper displayed names. The fresh client displays
  centered icons and a native 250 combined barrier counter with both Unique
  barrier toggles enabled, without the removed ambient particle effects.
- Original Nu Udra and Jin Dahaad portrait pixels are preserved. Prepared Rey
  Dau and Uth Duna visible bounds have center (32,32) in 64×64 files, with at
  least four clear pixels on each horizontal side; the new atlas isolates horns.
- Offline wiki: 20 articles plus Main Page generated. All 626 local `href` and
  `src` references resolve after packaging. Chromium/Playwright checks desktop
  and mobile layouts, search navigation, image loading and JavaScript errors.
- Release jar checked for the two 64×64 NPC textures, seven native ability IDs
  per NPC, aura recovery classes, healing mixin and native spawn distributions.

This is development-client and automated native-server coverage, not a long
multiplayer playtest. Native elemental defenses, skill ranks, world settings and
other mods can change damage and EP gains. The wiki documents the native MP cost
of blocking with multilayer barriers and the limits of autonomous NPC skills.

Release: `releases/tensura-wild-hunt-0.7.0.jar`

SHA-256: `4e3a95b09fc6c4247074f472eff40f2634d10425c47ce699a335e237b404e61d`
