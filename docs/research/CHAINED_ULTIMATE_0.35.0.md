# Chained, Unbound and native integration

Pinned references: Tensura 2.0.1.3, ManasCore 4.0.0.2, NeoForge 21.1.209; static comparison with Mysticism 2.2.1. Dependency source inspection is not redistributed.

## Learning and notices

Mysticism’s `RestrictedHandler` permits unfinished unlock entries, blocks learned acquisitions, and uses native `getChatDisplayName(true)` components for tier-colored skill names. Its learning event blocks ordinary restricted training. This implementation follows the user’s requested variation: unfinished entries remain visible and can train, but forbidden final completion is rejected.

`ChainedLearningGateMixin` permits negative-mastery instances in `learnSkill` and `updateSkill`. It rejects fully learned first acquisitions before native callbacks can bill permanent capacity. Updating an existing unfinished entry to a fully learned blocked instance is rejected too; already learned skills stay usable and updateable. `ChainedLearningMixin` intercepts the native final unlock dispatch in `TensuraSkill.addLearnPoint`, before acquisition callbacks spend permanent MP. Native rollback restores the previous negative mastery. Ordinary training activation costs retain their native behavior. Direct grants still use the same acquisition gate.

The notice is simply `Cannot acquire [skill].`, with the name’s native tier color and a one-second per-skill spam guard. There is no explanation appended. Resistance/nullification aura acquisition, scoped racial recovery and recorded reincarnation-derived exemptions remain intact.

## Body defense

Native `StrengthenBodySkill.onTakenDamage` multiplies incoming damage unless the source bypasses invulnerability or has barrier bypass at least 2. Chained’s in-slot Unerring Body uses the same callback and exceptions, with a 0.9 multiplier. Unbound uses 0.7. Dodge remains a separate `onBeingDamaged` callback: 100% projectile / 25% other for Chained, 100% / 50% for Unbound. Unfinished entries never grant defenses.

## Native-menu evolution

The primary native race must be stage-four Heavenbreaker; the permanent independent Chained instance must be mastered; native existence data must report True Hero or True Demon Lord. The addon discovers a normal unfinished Ultimate entry, not a sub-instance. Equipping and activating it completes a one-point native learning step. Gates are rechecked at completion. This adds no dynamic EP threshold, payment or separate UI. The offered entry carries the native one-use `NoMagiculeCost` flag, so Tensura’s zero-cost acquisition check cannot require a positive MP reserve after capacity has fallen to zero.

The replacement learns first and removes the original only afterward. It copies the full pact tag, completed mastery, toggle, dash cooldown and inheritance; all nine native preset slots referencing Chained become Unbound dash slots. Existing birth exemption and maximum-MP ceiling are preserved. All ownership hooks resolve learned Unbound first, otherwise Chained, so aura growth, regeneration, learning, Broken Limit and tracked buffs continue using one set of modifier IDs.

## Automatic criticals

ManasCore applies player melee criticals through NeoForge’s `CriticalHitEvent`; its native non-player critical handler does not cover player-owned projectiles. Unbound forces the native player melee event to critical, preserving stronger existing multipliers. A scoped `Player.attack` wrapper records the primary target so the global damage callback does not apply another critical multiplier to that melee hit. The scope restores itself in `finally`, including nested attacks.

Owned projectile and ability sources get one native critical-damage attribute multiplier and native critical feedback. This true mastered trait is independent of the combat toggle. Actual native Player.attack and owned Arrow damage tests verify one multiplier per hit. The normal critical multiplier is 1.5, but the native attribute remains authoritative. Environmental/unattributed damage is unaffected. A targeted wrapper on the native five-argument `directSpiritualHurt` path covers direct SHP attacks, which skip the ordinary damage event. It preserves native immunity handling, permits Counter to intercept, and supplies the one critical multiplier for attributable player attacks.

## Counter

Entry consumes 10% current aura and starts a 10-second cooldown. A transient -1 total movement-speed modifier roots normal movement for 60 server ticks. The active stance’s deadline/dimension are synchronized through the skill tag so native client cooldown checks permit a free cancellation press. Authoritative state remains in the server stance map.

At HIGHEST incoming-damage priority, the first finite positive eligible enemy hit is canceled before dodge, HP or SHP damage. Environmental, self, allied, PvP-protected and operator/void sources do not trigger it. The stance is cleared before releasing the slash, preventing recursive reuse. The slash instantly sweeps a three-block-radius corridor toward the attacker, at least 16 blocks, ending four beyond the attacker up to 64 blocks. Each eligible target gets one independent Physical hit. It does not raycast against blocks or edit terrain.

Base damage is `10 * incoming + 6 * sqrt(auraPaidAtEntry)`. This is at least ten times the negated pre-defense incoming amount; native outgoing bonuses, Unbound’s exclusive Physical branch and its one critical multiplier follow normally. Both Severance Rush and Counter are excluded from native Battlewill fallback classification, including while Aura Sword is active.

Timeout, early cancellation, suppression, loss of ownership, death, logout, dimension changes and respawn clear the stance and restore movement. Cancellation is free and retains the original cooldown; there is no refund or timeout attack. Standalone and full Myths use the same implementation with separate namespaces.

## Visuals and saved IDs

Air Walk retains the existing `air_sovereignty` registry ID, icon and saved flight lease keys. Its displayed skill/mode name changes, and its authoritative aura cost halves at mastery for both activation and real flying upkeep. No registry migration or duplicate skill is introduced.

Counter uses synchronized, non-damaging ArtVisual entities: a narrow stance pressure mark and a broad layered slash with a dark lip, white core, expanding flash and pressure wake. World-space custom geometry supplies continuous curves. The original Chained icon is retained for the evolved lineage skill. Native menu widgets, background, Ultimate tier color and input remain unchanged.
