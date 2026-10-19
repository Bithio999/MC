# Research: a skill tier above Ultimate

Status: feasibility research and proposed design, **not implemented**. Inspected 2026-10-19 against Minecraft 1.21.1, NeoForge 21.1.209, Tensura: Reincarnated 2.0.1.3 and ManasCore skill module 4.0.0.2. This report belongs outside the gameplay wiki.

## Conclusion

A separate, usable tier above Ultimate can be added to Wild Hunt and integrated into Tensura's current menus. The practical addon approach is to define the new tier in our own metadata and gameplay rules, expose it as a separate category, and retain `SkillType.ULTIMATE` as the native compatibility classification. It can have separate skill IDs, evolution, costs, mastery, abilities and colors; it need not be just a renamed Ultimate.

That approach does **not** add a seventh value to Tensura's `SkillType` enum, nor teach every other addon the new hierarchy automatically. Other mods would see an Ultimate unless they use our tier API/tags. Making a genuinely new native enum value with universal recognition would require changes to Tensura itself and an audit of its consumers, ideally through upstream support. Neither a resource pack nor a datapack alone can do this.

The tier's name, unlock requirements, immunity rules and actual skill kits remain design decisions. “Post-Ultimate” below is a placeholder, not a proposed final player-facing name.

## What the installed code actually does

These findings come from inspection of the installed JARs, including bytecode and locally decompiled methods. No third-party decompiled source is redistributed in this repository.

| Area | Verified behavior in this version | Consequence |
| --- | --- | --- |
| `ability.skill.Skill.SkillType` | Six constants: Resistance, Intrinsic, Common, Extra, Unique, Ultimate. Implements `StringRepresentable`, not `IExtensibleEnum`; exposes a string codec. | A seventh native type is not a normal registration. |
| `Skill.getMaxMastery()` | Explicit handling for Extra, Unique and Ultimate; default is intrinsic mastery. Ultimate defaults to 10,000 in `SkillConfig.Mastery`. | An added native enum could silently receive the wrong default. Define mastery explicitly. |
| `Skill.getDefaultAcquiringMagiculeCost()` | Intrinsic/Common have branches; other types use the Extra default. | A new tier needs an explicit acquisition-cost policy. Higher tier does not supply one automatically. |
| `Skill.createSource()` | Attaches `getType()` to a native ability damage source; `TensuraSkill.createSource()` attaches the skill instance, mode and aura/magicule costs. | Preserve that path; a separate tier/rank policy is additional metadata, not a replacement damage system. |
| `client.screen.AbilityCategoriesScreen.addSkillCategories()` | Explicit loop of six entries, two columns and three rows of 32-pixel buttons. Screen is 254×163. | The new category requires client code and a deliberate layout. |
| `AbilityCategoriesScreen.getSkillCategoryById()` | IDs 0–4 map to lower tiers; the default maps to Ultimate. | Passing category ID 6 would open Ultimate, not a new category. |
| `client.screen.AbilitySelectionScreen` | Learned `Skill` instances feed the list. With an empty search it filters by `getType() == skillFilter`. Search takes a separate branch; filtered abilities refresh during rendering. | A one-time list edit or a renamed category button will not work. Apply the custom filter on every rebuild, including searches. |
| `util.client.ScreenHelper.openAbilityScreen()` | Constructs a native selection screen with a `SkillType` filter. | A custom tier view must be opened explicitly or intercepted. |
| `AbilitySelectionScreen.renderAbility()` | Uses normal skill methods for icon, description, mastery, toggle and mode cooldowns. | Those components can be retained for a subclass of `Skill`. |
| `storage.ability.AbilitySlot` | Presets serialize the skill's registry ID and mode, not a tier ordinal. Native storage supports nine presets and three slots. | No fourth slot or new preset format is required. |
| ManasCore `ManasSkillInstance` and `SkillStorage` | Save/load registered skill instances, mastery and instance state; storage is keyed by the skill ID. | New registered skills can use native persistence. Unlocking a new tier still needs tested state transfer. |
| `network.c2s.RequestAbilityModeChangePacket` | Equip, toggle, mode-change and preset paths operate on learned skill IDs/instances and native eligibility hooks. | Existing operations can be reused. The research did not identify a need to replace their packet format. |
| `TensuraSkillEvents` | Includes `ABILITY_EQUIP`, `SKILL_LEARNING`, and `SKILL_PLUNDER` events. | Prefer supported events for eligibility/plunder rules; check the paths each event actually covers. |

This installed JAR has no classes under `ability/skill/ultimate/`; the enum and UI already expose Ultimate. That observation is limited to this release and does not imply that another addon cannot register Ultimates—Wild Hunt already does.

### NeoForge enum extension is not a shortcut

NeoForge's 1.21–1.21.1 documentation says extensible enums must already implement `IExtensibleEnum`. It explicitly states that adding that interface through mixins/coremods does not work because the enum extender runs earlier. The inspected Tensura enum lacks the interface.

Therefore, an `enumExtensions` JSON file alone will not add this tier. Unsupported enum surgery or an early transformer would introduce class-loading, codec, switch and other-addon compatibility concerns. This is not the recommended addon route. An upstream change to an extensible registry/category API would be preferable to maintaining such a patch.

## Two implementation routes

| Route | Result | Main tradeoff |
| --- | --- | --- |
| **Recommended: addon tier, native Ultimate compatibility** | A real Wild Hunt progression tier with independent skills and rules, a separate menu category, native storage and controls. Native `getType()` remains Ultimate. | Explicit compatibility layer; other mods do not automatically understand our new rank. |
| **Native Tensura tier via upstream change / compatible fork** | A new native type and potentially native category/config/command support. | Must change the enum/category code and audit all equality checks, switches, defaults, tags and consumers. Requires matching dependencies on client and server and a distribution/update plan. |

A new color or a 4× damage multiplier alone would not establish a gameplay hierarchy. The intended advantages over Ultimate need to be implemented explicitly.

## Recommended integration into the existing UI

### Category screen

Keep Tensura's menu tabs, right-hand preset area, three equipped slots, and nine preset buttons. Change only the left skill-category panel.

A concrete layout that fits its current dimensions is **two columns × four rows of 24×24 category buttons**. Relative to the current screen origin, candidate rows are Y=45, 73, 101 and 129; the last button ends at 153, within the 163-pixel screen. Reuse the two current category columns, with proper texture scaling, hover hitboxes and keyboard focus. The seventh slot is the new tier; the eighth can remain unused. Do not merely clip the native 32×32 textures into smaller widgets.

```text
Skills tab                          Existing presets / equipped slots
┌──────────────┬──────────────┐      ┌──────────────────────────────┐
│ Resistance   │ Intrinsic    │      │ Presets 1–9                  │
│ Common       │ Extra        │      │ Equipped slot 1              │
│ Unique       │ Ultimate     │      │ Equipped slot 2              │
│ Post-Ultimate│              │      │ Equipped slot 3              │
└──────────────┴──────────────┘      └──────────────────────────────┘
```

This is a proposed layout, not an in-game screenshot. A category pager that preserves the original six 32-pixel buttons is an alternative if the smaller icons are hard to read. Adding a fourth native-size row without changing layout would exceed the panel's current height.

Implement this through a focused **client-only** hook in `AbilityCategoriesScreen.addSkillCategories()`. A Screen Init event may be sufficient after checking widget access; a narrow mixin gives deterministic replacement of the six native category widgets without replacing the whole menu. Leave magic and battlewill screens alone.

### List and detail view

Create `PostUltimateSelectionScreen extends AbilitySelectionScreen`, initialized with the native Skill/Ultimate filters for compatibility. Reuse its normal detail view, mastery bar, description scrolling, active-mode controls, passive toggle, cooldowns and preset behavior.

Override its list filtering so `super.updateFilteredAbilities()` is followed by our tier predicate **on every refresh**. Handle name searches and `f:` searches, not just the initial open. After filtering, ensure scroll bounds, list start and selection remain valid; clear a selected entry that no longer belongs in the view. A list whose filter is reapplied per frame must not accidentally reset user scrolling every frame.

Also filter native Ultimate results to exclude Post-Ultimates when the user is viewing the ordinary Ultimate category; otherwise the compatibility classification makes them appear in both lists. A narrow client hook at the filtering boundary can distinguish the custom subclass from the native screen. Decide explicitly whether ordinary native name searches retain their existing cross-category search behavior; the custom tier screen must always respect its tier restriction.

Add a translated category label/badge so the new tier is visible in the list/detail UI, not just the category tooltip. The native screen title is the generic ability selection title; it will not display a new tier automatically. Use our tier color in the skill's `getColoredName()` so names also carry through native preset tooltips and chat. Specify names, descriptions and original icons through the existing skill methods.

Returning to categories must recreate the modified category panel correctly. Test opening through normal menus and reopening after resize/GUI-scale changes.

### Persistence and controls

Register each evolved skill under a new stable `wildhunt:` ID. Continue using native ManasCore skill instances and Tensura's slots, toggles, cooldowns and keybinds. Store fixed tier membership in a shared classification/tag/API, not a client-only Boolean or a player's screen state. Any datapack-controlled classification needs verification after server tag sync/reload on the client.

The minimal architecture is:

```text
New registered Skill (native type: ULTIMATE)
    ├─ Wild Hunt tier metadata: Post-Ultimate
    ├─ native ManasCore instance / saving / sync
    ├─ native Tensura modes / toggles / presets / keybinds
    └─ explicit Wild Hunt progression and interaction rules
```

## What “above Ultimate” must mean in gameplay

The inspected code does not expose a universal numeric tier ranking that automatically makes a higher enum ordinal stronger or immune. Important rules use **tags and per-skill logic**.

### Copying and theft

`TensuraSkillTags.ULTIMATE_SKILLS` is explicitly checked by native abilities including Pride, Spearhead, Starved, Unyielding, Chosen One, Summon Daemon and Predator Mist paths. Several also check `NO_PLUNDERING`; plunder events are exposed but should not be assumed to cover every copying mechanic.

For native compatibility, classify the new skills in `tensura:ultimate_skills`, and in our own `wildhunt:post_ultimate_skills`. If they are intended to be unstealable, include the applicable native exclusions such as `no_plundering` and audit each relevant copy path. Decide whether higher-tier skills themselves can steal lower skills and whether equals can interact. Membership in the fallback Ultimate tag supplies existing Ultimate handling, not a complete new hierarchy.

**Existing-addon audit item:** this repository currently provides a native `unique_skills` data tag, but no `ultimate_skills` tag contribution for its four Ultimate IDs. Their Java type is Ultimate; that does not substitute for the explicit tag checks above. Audit/fix this in an implementation pass, with regression checks for the current Ultimates. This research does not change their behavior.

### Anti-Skill and suppression

`AntiSkill.onBeingDamaged()` does not simply compare Unique versus Ultimate. It detects ability-related damage and has explicit bypass cases, including native damage tags and a barrier-bypass threshold. `Skill.isAffectedByStatus()` also checks the native Anti-Skill effect.

Consequently, neither an Ultimate enum value nor a new color makes a skill immune. If Post-Ultimates should defeat Anti-Skill or other suppression, add that as a deliberate, narrowly scoped policy with client/server tests. Do **not** set a broad barrier bypass value simply to evade Anti-Skill: that can also change unrelated defensive interactions. Barrier penetration, resistance penetration, suppression immunity and skill copying are separate design choices.

### Balance and evolution

Define mastery cap, learning/acquisition cost, active aura costs, passive upkeep, mode count and elemental damage explicitly. Set `getMaxMastery()` and acquisition behavior deliberately instead of inheriting whichever native switch default happens to apply.

A candidate progression is “master the matching Ultimate, satisfy the new gate, then evolve into its Post-Ultimate successor.” EP/race/boss requirements and whether the tier consumes aura or magicules remain undecided. Do not add EP damage scaling or change Wild Hunt's current 2/3 aura / 1/3 magicule EP allocation as a side effect.

Evolution must be server-authoritative and idempotent. Learn/validate the destination before removing the source, preserve the source on failure, deactivate its passive modifiers, and decide explicitly how mastery, active effects and cooldowns transfer. Preserve/clamp relevant cooldowns so evolution cannot reset them for free. Remap all nine presets by stable mode IDs, or document intentionally clearing slots. `TensuraSkill.onForgetSkill()` removes the forgotten skill from presets, so a naive forget-and-learn sequence will not preserve equipped layouts automatically. Current `HunterCommands.ascend()` already learns before forgetting, but a new conversion needs the additional preset/state work.

An evolution action can be shown next to the native detail panel using a small addon widget/overlay. Its packet should request a known source/target evolution; the server must re-check ownership, mastery, gates and duplicate ownership. The client must not supply trusted mastery, EP, costs or rank values. A button is optional; command/menu evolution can be separated from the tier browser.

## Proposed implementation units

These are proposed files/components, not files already added by this research:

| Component | Responsibility |
| --- | --- |
| `SkillTier` / `SkillTierResolver` | Namespaced tier identity, native fallback, comparisons and public query API; consistent client/server classification. |
| `PostUltimateSkill` | Native `Skill` subclass with explicit mastery, costs, translated name/color/icon and tier metadata. |
| New native skill registrations | Stable successor IDs; the actual skill kits are a separate design/implementation task. |
| Category-screen client hook | Seven categories with the new layout, tooltip and keyboard focus. |
| `PostUltimateSelectionScreen` + native list hook | Reuse native details/controls; correct custom and ordinary Ultimate filtering. |
| Native tag contributions | `ultimate_skills`, custom tier tag, and selected copy/plunder exclusions. |
| Server evolution service | Gates, transactional conversion, cooldown/passive cleanup and all-preset remapping. |
| Optional evolution payload/widget | Server-validated UI evolution request and result synchronization. |
| Config/translations/original UI art | Tier name, costs, gates, policy choices, category icon and readable tooltips. |

Client screen classes must only load on the client. Shared skill registration, classification, progression and gameplay policies must work on a dedicated server without a GUI.

## Validation required before calling it complete

1. Register one minimal proof-of-concept successor first. Verify dedicated-server startup and native save/sync before designing four complete successor kits.
2. Open the skill category panel at different resolutions and GUI scales; verify all seven categories and unchanged preset buttons, tab navigation and return behavior.
3. Verify tier filtering and selection with an empty list, large lists, name search, `f:` search, live learning/removal, GUI reopening and scrolling. Confirm intended duplicate/global-search behavior.
4. Equip and use the new skill in every slot; exercise all nine presets, keybinds, active mode cycling, passive toggle, descriptions, mastery and cooldown displays.
5. Save/relog, die/respawn, switch dimensions and restart the server. Verify instance data, tier classification, presets, cooldowns and passive state.
6. Test successful evolution and failed/spammed requests, source rollback, duplicate prevention, all-preset remapping and cooldown retention.
7. Test Predator/copy/plunder paths, Anti-Skill damage blocking and status suppression, native barriers/resistances, and chosen higher-tier policies. Include regression tests for existing Uniques/Ultimates.
8. Run with another addon that registers ordinary Ultimate skills. Verify our UI filters do not hide, relabel or alter those skills.
9. Verify config/tag reload synchronization and matching addon/dependency versions on client/server. Treat a mismatched installation as a connection/compatibility case, not a silently different UI rule.

None of these runtime checks have been performed for a new tier yet. This report establishes code-level feasibility; it does not claim a working UI patch or combat hierarchy.

## Evidence and references

Primary evidence is the exact installed dependency version, rather than assuming the latest wiki matches this build:

- Tensura JAR: `tensura-neoforge-2.0.1.3.jar`; SHA-256 `914043ed6d84877a1565daa6164ea489426acb518d797b954386523c1d85a676`.
- ManasCore skill JAR: `manascore-skill-neoforge-4.0.0.2.jar`; SHA-256 `1f4e08ce785f25162baea099a564b24a35714cf54aa7e573b3d8459a0292ce32`.
- Java symbols in the findings table are relative to `io.github.manasmods.tensura`; ManasCore save/load symbols are under `io.github.manasmods.manascore.skill`.
- Existing addon registration/ascension: [WildHunt.java](../../src/main/java/dev/wildhunt/WildHunt.java), [ApexSkill.java](../../src/main/java/dev/wildhunt/ApexSkill.java), [HunterCommands.java](../../src/main/java/dev/wildhunt/HunterCommands.java), [unique skill tag](../../src/main/resources/data/tensura/tags/manascore_skill/skills/unique_skills.json).
- [NeoForge 1.21–1.21.1 extensible enum documentation](https://docs.neoforged.net/docs/1.21.1/advanced/extensibleenums/): interface requirement and transformer-order limitation.
- [NeoForge 1.21–1.21.1 screens documentation](https://docs.neoforged.net/docs/1.21.1/gui/screens/): screen lifecycle and widget integration.
- [Tensura skill overview](https://tensura.wiki.gg/wiki/Abilities/Skills): public context for the existing categories; not evidence of a supported new-tier API.
