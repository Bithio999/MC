# Chained Physical damage and acquisition feedback

Reference: pinned Tensura: Reincarnated **2.0.1.3**, inspected `TensuraDamageHelper`, `MartialMasterSkill`, `TensuraSkill.createSource` and ManasCore 4.0.0.2's `SkillStorage.learnSkill`.

## Physical source and bonus

Tensura has no generic `TensuraDamageTypes.PHYSICAL` key. Martial Master's dash creates `minecraft:mob_attack`; Severance Rush now uses that same source while preserving its Chained ability instance and aura-cost metadata. Aura Slash can also be Physical in Tensura, but the new ID explicitly matches Martial Master's plain Physical attack source.

The classification predicate is **`TensuraDamageHelper.isPhysicalAttack`**. Unlike `isPhysicalOrBattlewill`, it does not require the direct entity to be the attacker, so it includes attributed physical projectiles. It respects native magic/element metadata and explicit Physical conversion. Environmental damage with no Chained attacker is not boosted.

The existing native pre-damage callback applies 3× for Physical damage and independently 5× for Battlewill damage, only while Martial Supremacy is enabled. A qualifying Physical Battlewill hit gets 15× once per hit; other Physical damage gets 3×, non-Physical Battlewills get 5×, and unrelated non-Physical damage gets neither. The dash keeps its established Battlewill classification. Native defenses and other modifiers still apply normally.

The former permanent `chained_combat` modifier on `ATTACK_DAMAGE` is removed during refresh, including on existing saves. Attack-stat-derived formulas therefore read the normal attack attribute; the Physical multiplier applies to outgoing damage instead. Numeric Battlewill buffs retain their separate 3× effectiveness.

## Rejection feedback

The acquisition guard must reject before native unlock callbacks can spend permanent magicules. ManasCore's normal `learnSkill` failure returns before displaying its mutable message, so merely setting the unlock event's message is insufficient.

A shared helper sends a translated, red system message to the affected server player. It includes the skill's component name and allowed categories. The helper is used by blocked learn, update and latent-learning paths; the unlock event also provides the custom reason. Repeated failures for the same owner/skill are limited to one notice per 20 ticks using weak, runtime-only state. Client-side synchronization and non-player owners do not send notices.

The real client received: “Chained blocks Musician. You can only acquire Battlewills, resistances/nullifications, inherited reincarnation skills, and racial aura recovery.” The attempted acquisition neither succeeded nor spent resources.
