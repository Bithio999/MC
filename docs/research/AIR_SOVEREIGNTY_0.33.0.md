# Air Sovereignty — native integration reference

Pinned dependencies: Tensura 2.0.1.3, ManasCore skill 4.0.0.2, Minecraft 1.21.1 and NeoForge 21.1.209. Research used the installed dependency JARs; decompiled dependency sources are not distributed with this addon.

## Movement and progression

The requested predecessor is registered as `tensura:air_flight`, implemented by `AirFlightArt` and displayed as **Air Flight** by the pinned English locale. Air Float is the player-facing alias used in the request. Its held art calls `pushForwardAtStaticSpeed` and sets no-gravity; this differs from Gravity Manipulation’s active.

`GravityManipulationSkill.onPressed` grants `Player.getAbilities().mayfly` and `flying`, then sends `onUpdateAbilities()`. The vanilla client supplies free directional movement, ascent/descent and hovering. The new art uses those same flags and packets rather than simulating flight through velocity pushes. It does not inherit Gravity Manipulation’s elemental toggle or smash-attack bonus.

Native `FormhideArt.onSkillMastered` unlocks a learning instance of Haze through `SkillHelper.learnSkill`. Air Sovereignty follows this pattern: keep mastered Air Flight and reveal a learnable Battlewill. The hook targets `ManasSkillInstance.onSkillMastered` because Air Flight inherits the underlying skill callback. A low-frequency discovery check and login check cover players who already mastered the predecessor. Both require the actual owned, permanent, independent parent instance. Native learning, acquisition handling and Battlewill mastery remain intact.

## Cost and permissions

Air Flight defaults to 15 aura + 15 magicules per payment. The new art converts their configured sum to aura: 30 aura at activation and per full second actually flying by default. Upkeep counts airborne ticks, not wall-clock time or time standing with flight permission. Native energy helpers handle payment. Deactivation is free. No flight-speed or other numerical buff is added; Chained applies its normal Battlewill training bonuses.

Native `SkillUtils.canFlyLegit` delegates to `canFlyWithSkills` after checking creative/spectator, armor and transformations. The new mixin adds an entitlement only for a learned, usable art whose flight has actually been enabled. Server authorization also requires its persistent lease. Ownership alone grants no flight permission.

Cleanup clears the art’s enabled state and lease before asking the native method whether another source permits flight. This prevents self-authorization from preserving stale permissions and preserves native race/skill/equipment permissions. Entity ticking runs outside native passive-skill ticking, so suppression cannot strand the permission. Actual airborne upkeep, interference, movement and world/boss restriction checks run on the server. Login can restore a valid saved lease; respawn ends it. The clone listener runs after the persistent-data copy. Detached/copied skill state cannot retain unbilled flight without the matching lease.

The release adds the new registry ID to Tensura’s `battlewill` skill tag and overrides its icon and literal colored name. It stays out of the Unique roll pool. Existing native Air Flight behavior and configured manual lists are unchanged.

## Related balance change

Chained’s Severance Rush changes from 4√A / 8√A to **3.5√A / 7√A**, a 12.5% reduction at a fixed aura payment. Its payment, instant teleport, collision checks, travel/arrival radii, cooldown and Battlewill handling retain their existing behavior.
