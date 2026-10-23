# Final Myth implementation — 0.30.0

Eschaton extends the native integration already researched for [Ananke](MYTH_BOSS_IMPLEMENTATION.md) and its [skill-package audit](ANANKE_SKILL_PACKAGE_0.25.0.md). This release adds original encounter code and assets; it does not copy addon bosses, textures or animations. The external addon comparisons in those reports remain reference-only, rather than a claim that those addons were installed together for this release.

## Native data and API findings

Pinned runtime: Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3 / CurseForge file 8793360, ManasCore 4.0.0.2 and GeckoLib 4.9.3. The native JAR's existence profiles, `EnergyHelper`, `TensuraSkill`, `TensuraAttributes`, native skill APIs, Infinite Regeneration and arena implementation supplied the integration evidence. `javap` inspection was checked against native GameTests.

| Native behavior | Implementation consequence |
| --- | --- |
| Existence data supplies actual AP, MP and SHP | Fixed 500M AP + 500M MP profile and explicit native race/EP initialization; never a cosmetic billion-EP name |
| Native MAX_HEALTH clamps to 10,000; MAX_SPIRITUAL_HEALTH clamps to 2,000,000 | Entity-local raw attribute evaluation retains vanilla modifier ordering for Eschaton's 1M HP / 4M SHP and the seals' 120k HP; no global limit changes |
| Actual skill storage differs from a temporary profile instance | Learn, master and activate the stored native support instances; fill initial resources before enabling funded toggles |
| Native `increaseMaxEP(entity, amount)` adds the argument to both pools | Crown allocates exactly 25M total explicitly to native base AP/MP, normalized to primary/convergent growth weights |
| Fixed `TensuraSkill` art-cost path debits the supplied AP/MP directly | Demiurge's displayed art costs stay fixed; Thoth's spell-efficiency attribute is not advertised as a discount on them |
| Native existence retains a minimum 10 AP + 10 MP | Seals have minimal native 20 EP, no loot/XP, no progression-boss tag; do not assert fake zero EP |
| Newly added entity sections can become query-visible after insertion | Ritual arrivals bridge ten ticks of registration to prevent simultaneous duplicate summons; tests wait for actual native index visibility |
| Native instant regeneration can stop eligible HP and spiritual deaths | Funded Infinite Regeneration gets first refusal; only an exhausted or otherwise unpreventable fatal defeat spends a form |

## Strength and encounter economy

The final Myth has 33⅓ times Ananke's native EP, over 20 times its HP, over 33 times its SHP, and a larger fixed-damage encounter package. This is deliberately above the previous 30M first Myth. EP does not multiply attack damage; its real pools fund native regeneration and skills.

Three Godlies plus 51 native support skills form the 54-skill package. All 44 native resistance/nullification skills operate through the native skill callbacks. Ordinary damage can be rejected, while Godly owners retain the same penetration rules as elsewhere in the addon. No new arbitrary incoming-damage cap or custom barrier was added.

Natural AP/MP regeneration multipliers are zero. Each Genesis Seal subtracts 50M current MP and 25M current AP; all three provide a 150M / 75M drain and an interrupt. Primordial Exchange spends 20M AP up front for 8M MP after its channel. It cannot create an infinite resource loop. Two fatal form changes restore all four native pools; the final defeat gives one victory. Native regeneration remains part of the fight instead of being silently disabled for phase transitions.

## Native integration and counterplay

- `OriginSovereignRace` is a native divine/spiritual lineage, excluded from player acquisition pools and secondary families.
- `DemiurgeSkill` is a real native Ultimate with the addon Godly tag. Its eight modes have actual per-mode energy, cooldowns and source attribution. It cannot be slotted by players.
- Aion and Thoth auxiliary arts activate actual learned instances. Aion's individual barrier toggle stays off. Shared Godly ownership passives remain active and non-stacking.
- `EschatonPattern` supplies the server and renderer with the same locked aim points, fan bands, rotating rays, wave gaps and refuge geometry. Costs and hits run on the server, independently of render frame rate.
- `GenesisSealEntity` is an actual damageable Mob/GeoEntity, bound to owner UUID and cast token. Environmental damage is excluded, participants can damage it, and each death drains once.
- Shared `MythEncounter` targeting extends the existing Ananke arena checks to all owned skill paths. Native eight-player `IArenaBoss` configuration is an optional alternative to the survival ritual.
- Cancelled casts, form transitions, death, removal and reload clean up owned finale warnings and seals. Reload preserves spent forms, wounds and resources, with no untelegraphed cast resumption.
- Four independent native boss events expose HP, SHP, MP and AP. The existing right-aligned HUD handles their insertion markers. Confirmed HP/SHP damage synchronizes a red overlay despite Pain Nullification.

## Art pipeline

Fourteen new editable projects add Eschaton, Genesis Seal, ten effect rigs and two Java item models. Models are exported by the real Blockbench 5.2.2 editor and GeckoLib Models & Animations 4.2.5 plugin. Runtime uses GeckoLib 4.9.3. Existing projects remain intact.

The boss uses four articulated arms, a jointed tail, cloth panels, an unfolding crescent mantle and seven final-form crown blades. New 280° tapered crescent surfaces use cached 96/64/32-segment meshes on animated bones, without cuboid ring outlines. Custom optics supply exact warning footprints, element-specific stars, curtains, a celestial canopy and the lensed world seed. The seed reuses Aion's stable accretion/lensing implementation without changing its twenty-projectile barrage.

The new finale entity type tracks at 20 chunks, renders at 320 blocks and expands culling bounds around saved impact points. Particle settings reduce decoration while preserving primary warnings. This is bounded rendering, not shader-based dynamic lighting or a claim of hardware-independent performance.

## Validation scope

See [the release validation report](../VALIDATION-0.30.0.md) for final native gameplay, asset, client and wiki results. The opt-in [native client fixture](fixtures/visuals-0.30.0/README.md) stages real native casts for screenshots and is excluded from the normal release build. Automated geometry/resource tests and staged client captures do not establish a multiplayer time-to-kill balance target; live endgame loadouts remain the practical tuning reference.
