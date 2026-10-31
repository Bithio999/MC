# 1.1.6 validation

Validated against **Tensura Neo Otherworld Release-1.1.3a**, CurseForge file **9340459**, with its original 263-project manifest and overrides. The standalone contains no Tensura Myths content.

- **107 standalone native GameTests passed** on the existing Tensura 2.0.1.3 / NeoForge 21.1.209 target.
- **16 exact-pack integration GameTests passed** on Tensura 2.0.1.4 / NeoForge 21.1.248, with the pack’s gameplay addons and configs. Checks cover the complete 39-addon-skill Fettered roll, five optional skill-copying classifications, three inline cost adapters, Pride, Creator’s native temporary/permanent creation and inheritance, birth resources, positive EP commands, Air Walk learning, automatic Unbound evolution, learning restrictions, weapon-aware dash/Counter, resistances paid with aura, status immunities, spear stats, actual vault loot, and Mysticism’s regeneration lock/removal.
- The full **263-project client** loaded successfully. A real player completed Chained and Unbound airborne teleports and automatic evolution; native Fettered Human and Ultimate skill screens were captured.
- A production dedicated server reached its ready state with the addon and the server-compatible pack selection. Five client-only visual/UI mods were omitted from dedicated runs and retained in the client review.
- The exact pack API profile compiles against its own GeckoLib 4.9.2 and SmartBrainLib jars. The normal release is built against the older supported Tensura API.
- The production distribution audit verifies version ranges, packaged tag contents, extracted content, and exclusion of review code, pack-only fixtures, third-party jars and Tensura Myths classes.

The pack checks use isolated worlds. Simulated players receive FTB team records and sufficient native Mysticism soul energy for the tested unlocks; production login/configuration is unchanged. Multiplayer balance, every possible addon interaction, and existing player-world migrations are outside these checks.

[Pack compatibility and reproduction](compatibility/NEO-OTHERWORLD-1.1.3a.md) · [Addon starting-skill audit](compatibility/FETTERED-SKILLS.md) · [Gameplay guide](GUIDE.md).
