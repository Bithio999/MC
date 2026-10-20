# Tensura 2.0.1.3 integration notes

These notes describe the shipped 0.7.0 implementation. The pinned dependency jar
was inspected locally; no decompiled source or native texture is redistributed.

## Otherworlders

`HunterOtherworlderEntity` extends native `OtherworlderEntity`, which implements
`IOtherworlder`. That preserves subordinate ownership/menus, inventory, native
save data, home handling and configured Unique-skill drops. The AI follows the
native otherworlder pattern: SmartBrainProvider, core movement/look tasks,
prey targeting, neutral target invalidation, owner following and melee. We do
not borrow a named otherworlder's combat subclass or its character-specific attacks.

Native `KyoyaTachinbanaEntity.finalizeSpawn` draws a value in [0,1) and subtracts
`OtherworlderSpawnDistribution.chance` values until an entity is selected. These
are **absolute probabilities, not weights normalized by the engine**. Simply
appending two entries to the existing eight 0.125 entries would leave late
entries unreachable or distort existing chances. This addon supplies ten 0.10
entries using the native datapack registry. The original eight resource IDs are
overridden intentionally; Shizu/Hinata special checks and the overall spawn
chance remain native. `SummonOtherworlderMagic` uses the same distribution.
Pack authors combining other NPC addons should rebalance the registry to total 1.

`EntityExistenceData.CODEC` expects `type`, `spiritualHealth`, `min_magicule`,
`max_magicule`, optional aura bounds and `abilities`. `AttributeHandler` learns
those real registered skills with `NoMagiculeCost` and enables supported toggles.
It also supports entity evolution; that field is not a race ID. We therefore set
Awakened Hunter through `RaceAPI` once during initial spawn, never on load. Native
racial learning grants Hunter's Recovery separately. Seven native abilities per
NPC live in `data/wildhunt/entity_existence/`. Tests spawn actual entities and
check every loaded ability, race and save/load result.

Native player-only menus and manually selected modes do not automatically become
NPC tactics. Passive capabilities and the native humanoid AI remain in charge;
the wiki states this limitation rather than promising autonomous use of every mode.

## Regeneration

The native Self Regeneration effect heals periodically. Native Instant
Regeneration restores missing HP every ten ticks, and its higher amplifier also
restores SHP. Its implementation explicitly spends magicules. Applying that
effect and later refunding MP would cause double payment and ordering problems.
The aura variants instead use registered Extra Skills and an event-driven tick,
native healing/severance checks, native `heal` events, `IExistence` SHP storage
and `EnergyHelper` aura payment. Only the strongest toggled tier runs. Lower
tiers honor native healing suppression; the Infinite equivalent mirrors native
Curse/Frost/Black Burn immunity while still respecting Infinite Imprisonment.
Lethal recovery is aura-funded and excludes invulnerability bypass, high barrier
bypass, complete severance and depleted SHP. It never modifies maximum MP.

## EP and vitality

Native `Alignment.DEFAULT` has aura gain multiplier 2 and magicule multiplier 1.
`DeathHandler.gainEPEntity` applies those multipliers to the existing gain
attributes, then enforces native penalties and gain caps. We retain this pathway.
Hunter evolution energy floors also use exactly two thirds aura and one third
magicules. A native integration test measures actual max-aura/max-MP gains at
all three stages. External gain modifiers or server caps still follow Tensura's rules.

`HumanConfig` defaults: Enlightened Human (100K EP) +80 HP/+300 SHP; Human Saint
(400K EP, four boss kills) +480/+3,000; Divine Human (2M EP) +980/+6,000. Hunter
bonuses are +100/+400, +500/+3,200, +1,000/+6,600 for distinct boss requirements
and final mastery. They replace old stage modifiers; login refreshes caps without
healing current HP/SHP. Sovereign base health is 1,020, below the default 1,024 cap.

## Barriers

Native physical/magic barrier attributes are separate from `MULTILAYER_BARRIER`,
which the shield HUD reads. Multilayer is per-hit capacity and spends native MP,
not a consumable shield-health pool. Native overflow and power interactions can
remove modifiers. Our incoming-damage listener records hits before reduction,
including fully blocked hits. Paid upkeep restores only our missing modifier
after ten seconds without a hit. Off/on never clears that timer. Other sources'
modifiers are not removed by our toggle cleanup. Native MP blocking costs remain
native; aura upkeep and the new recovery skills do not replace those costs.

## Art and skins

The original portrait sheet crossed a quadrant seam. A corrected generated sheet
keeps the wide Rey Dau horns within their own tile; nearest-neighbor extraction
provides four transparent pixels around each icon and enables texture clamping.
`prepare-icons.py` is reproducible asset preparation, not a build dependency.

Character references consulted October 19, 2026 UTC:

- https://monsterhunterwilds.wiki.fextralife.com/Alma — dark tied-back hair, round
  glasses, rust expedition coat, cream hood and field gear.
- https://monsterhunterwilds.wiki.fextralife.com/Gemma — blonde tied-up hair,
  goggles, cropped leather jacket, pale top, work belt and ochre trousers.

Planet Minecraft, Skindex, NovaSkin and NameMC blocked the attempted automated
skin downloads. The bundled skins are original generated interpretations packed
into classic 64×64 Minecraft UVs by `scripts/prepare-skins.py`, not downloaded
community skins. Generated source islands are retained in `art/`. Third-party
reference images were viewed locally and are not included in the repository.
Native regeneration icons are referenced by resource location at runtime.

## 0.7.1 hair correction

The old back/side head crops could sample transparent gaps in the generated
island sheet. Forcing those samples opaque exposed their black RGB values.
The packer now uses opaque head-only atlases for the top, bottom, sides and back,
checks their opacity, and orients side tiles to Minecraft's UV direction. It
preserves the exact original front-face and body pixels. The 0.7.1 release was
built successfully; all 8 existing unit tests passed. Pixel-difference checks
confirmed that changes are confined to the five non-front head faces, and client
views confirmed the repaired rear and side hair. Native NPC/gameplay code was
unchanged from 0.7.0.

## 0.7.2 front hairline and registry initialization

Alma's 8x4 front hairline patch replaces only the upper four rows of the front
head face. The deep triangular skin gap becomes an even hairline above a single
forehead row. Pixel comparison against 0.7.1 confirms that only those 32 pixels
changed: her glasses/eyes/nose/mouth, remaining head faces, body and all Gemma
pixels are identical. Generated patch source and nearest-neighbor packing are
retained in `art/alma-front-hairline.png` and `scripts/prepare-skins.py`.

Client validation also exposed a constructor-time Architectury lookup failure
for `manascore_race:races`. Registration now uses `RaceAPI.getRaceRegistry()` and
`SkillAPI.getSkillRegistry()` directly with explicit `wildhunt` resource IDs,
matching Tensura's own native registration approach. It no longer looks up a
foreign custom registry through a separate DeferredRegister during construction.
All IDs and saved race/skill data are preserved. The build, 8 unit tests and 38
native server GameTests pass with the corrected registration path.

Fresh Minecraft client loading and frontal/angled views also pass in 0.7.2. The
front-view screenshot is in `wiki/media/alma-front-hairline-072.png`.

## Tensura Myths 0.8.0

The display name and archive change; registry namespace `wildhunt` remains stable.
`GodlySkill` stays native `SkillType.ULTIMATE`, with additive
`wildhunt:godly_skills` and explicit native Ultimate tag membership. The client
adds a seventh category and reuses `AbilitySelectionScreen` for skill details,
slots, presets and toggles. The ordinary Ultimate category excludes Godly
entries. Ascension replaces only a successfully learned source, remaps all nine
presets and carries its longest remaining cooldown to every new mode.

Duo state stores independent native `ManasRaceInstance` NBT under the existing
player-persistent addon data. It records both chosen ancestries and survives
ordinary death through a deep clone. Secondary evolution uses the instance's
native next stages and requirement progress, plus a server-side cumulative EP gate measured from selection. Each stage needs another configured 2.5M base EP; the origin and completed count persist across death and reload. Older independent secondary saves begin their next growth interval on the first new progression check. Shared secondary attribute
modifiers have distinct IDs and half the native stage's amount; primary body,
size and energy caps are retained. Secondary intrinsic skills use native
learning. Racial hurt/effect/attack reactions and one-second native ticks are
forwarded to the stored instance. The configurable secondary racial key invokes
native press/hold/release callbacks, activation/release events, server-measured
duration, cooldowns and suppression rules. It starts unbound.

`DuoEnergyMixin` changes only the alignment weight operands inside native
`DeathHandler.gainEPEntity`. It does not award a second kill, replace gain
attributes, or bypass the native caps and PvP penalties. Unfused growth is the
native current alignment with a flat 1.1 multiplier on both weights. Fused growth uses its registered pair profile with the same inherited 1.1 multiplier. No secondary ancestry weights are added. Existing
energy is not redistributed when choosing or evolving a secondary.

Convergence calls native `RaceStorage.setRace` to install one of 105 actual
registered `FusionRace` entries. Each profile has a name, icon, registry ID and
unique EP split. Native tag files combine the final ancestral traits. Fused
attributes are derived from native configured final-stage attributes; original
race instances remain archived for reactions and ticking. The independent
secondary action ends and the native racial action becomes Concordance Pulse.
The one-time energy multiplier runs only after the native race change succeeds.
Both lineages must be terminal, and true awakening and configured base EP are
revalidated on the server. Changing the primary outside this flow clears the
previous Duo life.

`MythsNetwork` accepts bounded intent rather than client-provided race data,
requirements or stat values. All changes run on the server thread; menu data is
a server snapshot. Common registration does not load client screen classes.
The server configuration is `serverconfig/tensura-myths-server.toml` in the world.
Aion transient checkpoints and delayed attacks clear on login, dimension change
and respawn. Reversal preserves energy, inventory and cooldowns while restoring recorded HP/SHP and rejects
an obstructed recorded destination before charging the return cast.

## Native menu integration

The standalone Myths panel is removed. The six native skill category buttons retain their original 32px textures and hover behavior. A tail injection arranges them in three columns and appends a matching original 32px Godly icon. The left panel reuses a clean strip of native blue artwork to fit seven buttons without the old six socket positions showing through. No base-mod artwork is bundled. Godly selection and ascension both use the native
`AbilitySelectionScreen`. Ascension filters owned Myths Ultimates and exposes an
explicit native action; the regular Godly page lists learned Godly skills.

Native `ScreenHelper` right-side slots 5 and 6 open Secondary Race and Convergent Awakening on all screens that use its menu tabs. They use native blank right-tab frames and original 16px glyphs. Evolution header navigation is removed. Status replaces only the race-name rendering with a bounded scrolling `Primary/Secondary` label and full hover text; convergence displays the unified native race name. Read-only silent snapshots refresh Status without opening another screen.
`MythsEvolutionScreen` extends native `EvolutionScreen`, retaining its background,
character renderer, widgets, search, list selection and scrolling behavior. It
removes only the primary evolution/track actions and supplies server-authorized
secondary or convergence actions. Requirements, candidates and completion are
sent by the server, including actual registry IDs for candidate icons. The
`/myths` command remains an optional shortcut to the same native page.

Aion uses separate synchronized visual entities for its gravity domain, anchor,
Verdict telegraph and impacts, plus native Gravity, Space and Recovery circles.
Consuming a Horizon discards its owned geometry and circle immediately. Its pull radius is 24 blocks while damage remains within 10. The visual carries both radii.

Worldline Sever uses native ManasCore press/held/release callbacks. A server-owned six-second window, aim ray, half-second pulse schedule and paid magicule costs prevent continuing damage after release or exhaustion. Cooldown starts when the channel ends. Its visual entity updates position/end vector over native entity synchronization; relog, death, suppression and dimension change stop it. Legacy echo victims are discarded.

Aion costs magicules throughout, including passive upkeep and channel pulses. The native pre-calculation event adds a single 35% outgoing bonus against targets without a Godly-tagged skill. Scoped hooks in `ResistSkill.isResistanceBypass` / `isNullificationBypass` and `DamagingHandler.applyResistanceAttributes` remove resistance protection only for an active Aion caster's attacks. They never remove, retoggle or alter learned defenses or their mastery. Armor, barriers, other authority/skill checks and PvP rules remain native. A short-lived, non-stacking transient movement modifier slows eligible enemies in a 16-block aura by proximity; its ten-tick lease expires after leaving or toggle-off.

## Elemental attributes and Thoth rendering (0.14.0)

`ElementalBonusesMixin` intercepts the cached `AttributeInstance.calculateValue`
only for the eleven registered native boost attributes. It compares each positive
ordinary modifier independently using Minecraft's flat, multiplied-base and
multiplied-total order, retaining the most effective one. All non-positive
modifiers and `wildhunt:thoth_passive` remain in every candidate calculation.
The native attribute sanitizer still applies. Modifiers are never deleted, so
removal/expiry, client/server recalculation and the next-strongest source work
through native attribute dirtiness. Armor, resistance, healing, costs and attack
speed do not enter this hook.

Thoth's dedicated renderer uses full-bright standard entity and additive-eye
passes, original transparent seals, page panels and premultiplied soft glows.
Additive RGB is faded with alpha to avoid opaque white glow rectangles. All mesh
counts are bounded and animated with partial ticks; no shader pack or extra
particle entities are needed. Moving art entities implement packet interpolation;
the client book formation follows the caster's interpolated position. Damage,
chapter timing, target caps and resource costs remain server-owned.

## Native Mana Crucible channel (0.14.1)

ManasCore's `TickingSkill.reachedMaxDuration` explicitly recognizes -1 as
unlimited. Thoth uses that sentinel and its native `onHeld` / `onRelease`
callbacks; conversion is never performed by the generic event tick. A server
game-time guard prevents duplicate transfers. A two-tick heartbeat watchdog
removes abandoned channels without granting catch-up conversion.

The transfer each tick is `min(maxMP × 0.05 / 20, currentAura / 2, missingMP)`.
Actual aura storage is debited even in creative; no cast magicules are charged.
Magic Cost Multiplier does not change the exchange ratio. Mastery is granted
once per twenty successful conversion ticks, without a start/tap reward.

`canIgnoreCoolDown` lets native activation accept an old Crucible cooldown;
login/lifecycle cleanup clears that cooldown, and ascension excludes this one
art from inherited cooldowns. Other Thoth modes keep their cooldown handling.
The same single owned visual is retained and follows the caster while held.
