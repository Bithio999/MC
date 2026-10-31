# 1.6.0 — Fusion

Fusion is a registered Unique with one press ability, Fuse. It accepts native subordinate ownership (including Tensura’s temporary/permanent owner records) or a server-validated player invitation. Invitations expire after 20 seconds and cannot be accepted by another player or replayed. A receiving player controls the body; a subordinate’s fused form is controlled by its caster.

Duration is 3 minutes. Both parties receive native **Paralysis II and Fragility II for 5 minutes**, including on early separation. Fuse can be recast, the absorbed caster can sneak, or either participant can use Pause → Separate Fusion.

Actual maximum aura and magicule capacities are combined and multiplied by 1.5, using transient modifiers rather than rewriting permanent EP. Native positive regeneration pulses are multiplied by 10 after biome/rate calculations. Final HP damage and direct Spiritual damage retain their damage type and receive ×2. Normal skill restrictions, including Chained, remain in effect. Chained retains its flat magicule pulse while converted aura regeneration benefits from Fusion.

The NPC control form keeps the original subordinate entity, ownership and equipment. Its AI is paused; its renderer and stats provide the controlled form, with Tensura’s mob-to-player movement conversion. Vanilla and GeckoLib renderers suppress the embedded original. The caster’s inventory is not copied or replaced. Existing learned skills are retained; temporary loans preserve their origin’s containers/cooldowns, and an existing unlearned entry is restored afterward. Existing temporary abilities are also lent, without extending their original lease. Marked orphan loans and deferred state returns are recovered when their owners load.

Original capacities, flags and game modes return on separation. Remaining resource fractions are divided across the original capacities; HP/SHP remain bounded by their restored maxima. Death, disconnect, dimension change, loss of Fusion and server shutdown end an active session. Stale saved markers recover on load.

## Verification

- **30 unit tests and all 168 native GameTests pass** on Tensura 2.0.1.3 / NeoForge 21.1.209. The native cases include prevented-death/regeneration priority and temporary-ability lease preservation.
- Native integration covers the EP formula/restoration, native regeneration and biome inputs, exact final damage, subordinate ownership, forged/declined/replayed player invitations, receiving-player control persisting after absorption, temporary skill/learning-entry restoration, returned bank/cooldown state, the duration boundary, five-minute level-II penalties, disconnect and stale-marker cleanup.
- **Live subordinate recording:** 2,000,000 + 1,000,000 EP becomes 4,500,000 EP. Physical, magical and direct Spiritual test strikes each changed from 20 to 40. A borrowed Aura Slash was placed in the native slot and cast. Early recast restored the original EP and removed borrowed skills. The preview uses a copied disposable world and genuine gameplay frames; the player separates early rather than waiting three minutes. The video is muted.
- **Live consent UI:** actual server invitation → client Accept button → authenticated reply packet → receiving-player control persists → native pause-menu Separate Fusion button → restored player. The inviter was a server-side test player; this is not a two-human-client playtest. Screenshots document the consent and exit controls.
- Tensura 2.0.1.4 / NeoForge 21.1.248 API compilation also passed. This is an API check, not a full live Neo Otherworld modpack test. Production packaging and wiki checks passed. Review fixtures and dependency JARs are excluded from the distribution.

Registry IDs for existing content remain unchanged. This release updates only TensuraAdditionalAdditions. [Fusion](../../../wiki/pages/Additions-Fusion.md) · [Preview reproduction](preview-fusion/README.md) · [Previous validation](VALIDATION-1.5.2.md).
