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
