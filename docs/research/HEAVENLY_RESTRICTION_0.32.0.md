# Heavenly Restriction integration notes

Target: Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2. The implementation follows the user’s five-stage choice: one starting race plus four evolutions. Existing magicule capacity is retained, and configured defense acquisition bills use current aura.

## Native entry points

- `ReincarnationMenu.getRacePool/getRandomRacePool`: add only the starting Fettered Human. Advanced stages use `ManasRaceInstance.getNextEvolutions` and native weighted `EvolutionRequirement` progress. The fixed base-EP milestones are 100K / 500K / 2M / 8M, plus one through four mastered Battlewills and 20% / 40% / 60% / 100% Chained mastery.
- `TensuraRace.getIntrinsicSkills`: all five stages grant the same Unique Chained. Attribute IDs are shared across stages, so native race changes replace the racial bonuses. Human ancestry integrates with the existing primary-lineage classification without adding a sixteenth fusion family.
- `TensuraAttributeInstance.tensura$getOwner`: scope resource and Battlewill modifier handling to the actual owner. A single +99 total aura multiplier covers base values and all ordinary capacity modifiers. Positive base-MP changes are refused and effective MP reads are capped to the saved pre-vow ceiling, including an existing spiritual limit. Current-resource refills, native hard limits and legitimate losses retain their roles.
- `ExistenceStorage.handleMagiculeRegen`: redirect the natural regeneration amount, using the actual area and regeneration multiplier, into current aura. Do this even at full MP; keep spiritual upkeep and excess-energy decay on their original paths.

## Inheritance and acquisition

`SkillStorage.learnSkill` is checked before native unlock callbacks can spend permanent magicules or fire learning side effects. Its update API also checks new additions, while normal loading and synchronization preserve already-owned skills. `TensuraSkill.addLearnPoint` prevents completing a prohibited previously latent ability through another entry point.

Reincarnation race grants, ordinary Unique grants and the special Rimuru path run inside exception-safe provenance scopes. Native learned/mastered/tick/activation/damage callbacks inherit those scopes only for the same owner. Successful nested grants and inherited parent-skill links are recorded in persistent player data. This permits Martial Master’s actual Heavenly Eye grant without broadly whitelisting unrelated Extra skills. Tracking also runs before a player acquires Chained, covering skill-before-race ordering. The existing clone handler copies the persistent ancestry.

Myths synthesis and Godly ascension authorize a derived result when every consumed ingredient has recorded inheritance, preserving the same rule through local recipe-based evolution.

Historical grants cannot be attributed reliably after the fact. `/myths chained inherit <player> <skill>` lets a permission-level-2 operator certify an already-owned historical root. It grants no skill. No client payload can claim ancestry directly.

Tensura’s pinned `AbilityHandler.lambda$init$0` is the native unlock/payment callback. The scoped acquisition wrapper substitutes **current aura** for its configured permanent-MP bill only for a Chained owner acquiring Resistance/Nullification. The acquisition-cost gamerule remains effective, native free reward flags remain free, and latent completion/upgrading use the same rule. An unfunded completion rolls back through the native learning result. The callback binding is version-specific and must be revisited when upgrading Tensura.

## Martial effects

Battlewill is a separate native ability class, not the `Skill` class. It remains learnable. Default Battlewill learning and mastery increments receive +9; the resistance class’s direct learning path receives +19. Unrelated abilities do not inherit these bonuses.

Battlewill callbacks and their beneficial effects carry source provenance. Native attribute modifiers retain an original/applied ledger; refresh/toggle/save operations adjust them from their original amounts, avoiding repeated tripling. Damage-action buffs such as Aura Sword triple their numeric increment through the native damage callback. Durations, conversion rates and costs are not multiplied. Chained’s separate attack modifier and 8× Battlewill damage then follow native combat handling.

At mastery, source-level resistance bypass is at least one, not two. Native Nullification therefore uses its actual Resistance fallback. Corresponding status immunity follows the lower-tier resistance’s own immunity list, scoped to the caster. Numeric resistance attributes are bypassed while barriers and interference remain separate. Live source-level dodge bypass and the native dodge-negation attribute cover the owner’s hits. Slotted Chained also implements its own universal projectile/other-damage dodge roll through ManasCore’s damage dispatch.

## Instant teleport and visuals

Severance Rush resolves its collision-limited endpoint and both damage regions in one activation, then uses native `teleportTo`, just as Martial Master’s art completes immediately. It is not an interpolated physical dash. Sampling checks the body against terrain, unloaded chunks, the world border and build height. Targets can receive one path hit and one separate arrival hit; source-scoped independent hits prevent vanilla hurt cooldown from swallowing the arrival damage. A native helper classifies the Unique’s own source as Battlewill.

Cost is 2% of current aura. Raw path damage is `2 * sqrt(auraPaid)` and arrival damage is twice that; there is no free base-damage floor, extra EP coefficient or additional mastery damage multiplier. Native defenses and allowed damage bonuses still apply.

One synchronized procedural effect draws a short separating chain wake and arrival pressure burst from the actual journey. It adds no authored model and leaves the existing Blockbench/GeckoLib assets unchanged. The actual client reviewed the real-player teleport (16.0 blocks, 1,000 aura from a 50,000-aura staged pool), the native Status display and the rendered wake. The fixture is opt-in and excluded from release builds. The native-client acquisition chat revealed the missing colored-name fallback during development; the release supplies a literal colored name and matching locale entries.
