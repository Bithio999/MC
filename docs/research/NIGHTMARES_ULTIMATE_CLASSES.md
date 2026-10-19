# Research: how Nightmares separates Ultimate classes

Research date: 2026-10-19. Status: published documentation and released-code inspection, not an in-game test or a Wild Hunt implementation.

## Finding

Nightmares separates **Lord / King / God classes inside the native Ultimate tier**. Dragon-Class is a special True Dragon branch, not a verified extra rung between King and God. In the inspected NeoForge release, representative skills from every class still use Tensura's `SkillType.ULTIMATE`.

The distinctions are implemented through separate registered skills, per-skill evolution conditions and abilities, mastery configuration, selected God-Class ownership restrictions, and presentation. I did not find a new native `KING`, `DRAGON` or `GOD` skill-type enum, a category-screen replacement for those classes, or a universal Lord-versus-King-versus-God combat comparison.

This provides a concrete precedent for keeping native Ultimate compatibility while implementing more powerful classes within it. A separate top-level category is optional; it is not required to follow Nightmares' model.

## Versions and evidence

Inspected:

- **Nightmares NeoForge 1.21.1:** `trnightmare-1.0.5.2.2.5-neoforge-1.21.1.jar`, CurseForge file **9120824**, published October 10, 2026. SHA-256: `892d77a3169a45c14c41efc277cae8204d2b0786fec6f5416b9ad7bcaa35dd04`.
- Its embedded `META-INF/neoforge.mods.toml` identifies this as a port from 1.19.2 and requires **NeoForge 21.1.220+**, Minecraft 1.21.1, Tensura `[1.9.0,)`, and Text Animator 3.0+ on the client. These are its declared ranges, not a runtime compatibility test against our stack. Wild Hunt currently uses NeoForge 21.1.209, so this release was inspected separately, not installed into the project.
- Nightmares' public wiki, including its version-specific skill landing pages and Ultimate class categories. Some links are empty/legacy, and the public project description is a feature list rather than a definitive implementation matrix. Code findings below apply to the inspected 1.21.1 release; do not silently apply them to 1.19.2.

The released JAR was inspected locally using class metadata, bytecode and decompiled methods. Only this original summary is committed; no third-party binaries or decompiled source are included. The public project pages did not provide a source repository link during this research.

## The advertised classes

| Class | Documented role | Examples | Native type in inspected examples |
| --- | --- | --- | --- |
| Ordinary / Lord-Class | Normal Ultimate class | Raphael, Beelzebuth, Gabriel, Lucifer | Ultimate |
| King-Class | More powerful Ultimate evolutions/combinations | Cthulhu, Astaroth, Cthugha | Ultimate |
| God-Class | Top-end Ultimate class | Azathoth, Nodens, Akashic Records | Ultimate |
| Dragon-Class | Skills representing the True Dragons; separate special classification | Veldora, Velzard, Velgrynd, Velgaia | Ultimate |

The wiki describes the broad progression as **Lord → King → God**, with Dragon skills ranked separately. The wording in a skill's name is not a reliable type detector: Veldora is called “Lord of Storms” but belongs to the Dragon branch. Likewise, “Supreme Ultimate” in some descriptions is not evidence of another native enum value.

## How the code differentiates them

### 1. Shared native type and tags

Nightmares' `main.ultimates.UltimateSkill` extends Tensura's `Skill`. Constructors inspected in `RaphaelSkill`, `CthulhuSkill`, `AstarothSkill`, `AzathothSkill` and `VeldoraLordOfStormsSkill` pass native `SkillType.ULTIMATE`. `NodensSkill` inherits its Ultimate classification through `LuciferSkill`.

The packaged data includes `data/tensura/tags/manascore_skill/skills/ultimate_skills.json`, containing ordinary, King and God examples together. The inspected tier-related native skill tag is Ultimate; no parallel King/God/Dragon skill tags were found in that location.

Therefore the native category filter, equipped slots and most native Ultimate checks treat these as Ultimates. Nightmares supplies the distinctions around that base rather than extending Tensura's enum.

### 2. Mastery settings are configurable, with important exceptions

`NightmareUltSkillConfig.UltMastery` defines these defaults:

| Configuration field | Default mastery points |
| --- | ---: |
| Generic Ultimate | 5,000 |
| Angelic Ultimate | 5,000 |
| Demonic Ultimate | 5,000 |
| Sin Ultimate | 15,000 |
| Virtue Ultimate | 25,000 |
| King Ultimate | 35,000 |
| God Ultimate | 50,000 |

Those fields do not constitute an automatic rank resolver. Individual skills choose which value they use:

- `RaphaelSkill.getMaxMastery()` uses Virtue: **25,000** by default.
- `VeldoraLordOfStormsSkill.getMaxMastery()` uses King: **35,000**. Dragon skills do not have their own mastery field here.
- `AzathothSkill`, `NodensSkill` and `AkashicRecordsSkill` use God: **50,000**.
- **Cthulhu is described as King-Class but its inspected `getMaxMastery()` uses Sin: 15,000.** Do not claim every King-Class skill automatically has 35,000 mastery.
- The common `UltimateSkill` base reads the individual default configuration's Generic mastery field. Inherited implementations can differ from the headline class field.

Mastery points are a learning requirement, not a universal damage multiplier or a conclusive measure of combat strength.

### 3. Evolution conditions and skill packages

The stronger classes use explicit evolution routes instead of an automatic numeric promotion of every Ultimate.

Verified examples:

- **Cthulhu:** its acquisition path checks mastery of both **Gabriel and Leviathan**, the automatic-evolution and Nightmare-Ultimates gamerules, its evolution enable setting, and its configured evolution condition route. The helper receives both prerequisite skills. This is a combination route, not a new native skill type.
- **Azathoth:** checks mastered **Raphael (Wisdom) and Beelzebuth**, Nightmare Ultimates and God Skills rules, evolution enablement, ownership availability, EP and maximum-magicule requirements, plus configured secondary conditions. Its successful acquisition path consumes prerequisites, learns the new skill and grants additional packages such as its spacetime functionality and Void Resistance.
- **Veldora:** the Dragon branch uses `TrueDragonHelper` / `DragonType`, a True Dragon configuration and specific acquisition logic. The wiki describes Dragon Seeds as the acquisition item. This is separate from the God ownership mechanism.

Each successor has its own modes, effects, costs and passive implementation. The class title alone does not supply those mechanics, and exact thresholds must be read from that skill's current configuration.

### 4. God-Class ownership is a real additional server rule

`TrulyGodClassOwnership` is world `SavedData`. It manages a concrete list of skill IDs:

- Akashic Records
- Azathoth
- Nodens
- Tenebrosum
- Yog-Sotohort
- Zehirete

When the `TRULY_GOD_CLASS` rule is enabled, acquisition/claim code consults configurable limits. The inspected defaults are **one owner per managed God skill** and **one managed God skill per player**. Creative acquisition has an explicit exception. The actual implementation is a per-skill owner map plus per-player counts, not simply a single global “only one God skill may exist” flag.

`TrulyGodClassOwnershipHooks` updates ownership on native unlock/removal events and performs login enforcement. Azathoth and Nodens also check ownership during their own acquisition paths. These checks apply to the managed IDs; another addon adding a God-sounding skill is not automatically registered in that list.

### 5. Combat authority is broader than the class names

`UltimateAuthorityHandler` distinguishes **Ultimate owners from non-Ultimate owners** by scanning learned skills for `getType() == ULTIMATE`. It does not compare Lord, King and God classes.

For the inspected handler, when a living attacker owns no Ultimate and the target owns Ultimates:

| Target's Ultimate count | Handler's incoming damage-event multiplier |
| --- | ---: |
| 1 | 0.66 |
| 2 | 0.33 |
| 3 or more | 0 |

It also registers vetoes for energy-drain, spiritual-hurt and possession events across that ownership boundary. These are code paths, not measured final combat outcomes; event ordering, other mechanics and server configuration can affect gameplay.

**A God-Class owner is not granted a blanket victory over a Lord-Class owner by this handler.** If both have an Ultimate, this particular ownership comparison passes. Higher-class advantages come from the individual skills and other specific systems, not a universal class ladder in this handler. I did not find a general Lord/King/God damage multiplier table in the inspected tier-related paths.

This matters for compatibility: Wild Hunt's Ultimates also use native `ULTIMATE`, so they can qualify for this general authority check when Nightmares is installed. Explicit native tag membership still matters for other mechanics that check tags rather than `getType()`.

## How it appears in the UI

### Native skill menu

The inspected Nightmares JAR does not replace `AbilityCategoriesScreen` with separate Lord, King, Dragon and God category buttons. Its skills remain native Ultimates for the normal selection UI.

It adds client mixins for skill icons and animated text in the native ability list, preset/detail rendering, sub-mode screens and HUD. `TextAnimatorCompat` looks up a skill-specific animated translation key and passes supported effect markup to Text Animator. Examples in the shipped language data include:

- Azathoth: a monochrome gradient with pulse/wave effects.
- Nodens: red/black gradient with glitch/wave effects; its icon also uses animation frames.
- Akashic Records: neon/gradient/rainbow effects.

These are per-skill presentation choices, not a consistent tier-color table that defines native skill rank. The client compatibility helper can resolve keys from a skill's namespace; actual support for Wild Hunt would still need validation because our skill class overrides its own colored-name method.

### Evolution UI

Nightmares additionally supplies **its own Ultimate Evolution menu**, opened through its menu helper/keybind. `UltimateEvolutionMenuHelper` builds entries from native Ultimate targets and includes predecessors, condition counts, ready/pending state, magicule requirements and slot availability. `UltimateEvolutionScreen` renders routes, details and actions.

This separates two jobs:

1. Tensura's normal ability menu is used for learned skills and normal controls.
2. Nightmares' own evolution UI explains obtaining/evolving them.

Its “Ultimate slots” system limits **ownership/acquisition capacity**. It is not equivalent to adding more native equipped hotbar slots. Keep those concepts separate when designing Wild Hunt's progression.

## What to take from this for Wild Hunt

The Nightmares model supports the compatibility approach in [the earlier report](POST_ULTIMATE_TIER.md), with a less intrusive UI option:

1. Keep evolved skills as native Ultimates; define our higher class through explicit metadata and stable skill IDs.
2. Give the new class a clearly labeled identity and original presentation in the existing Ultimate list. Add an Ultimate sub-filter if useful; a seventh top-level category is optional.
3. Implement higher-class evolution gates, mastery/costs, and specific abilities. Define copying, Anti-Skill and barrier interactions independently rather than assuming the name changes them.
4. Add an evolution panel/menu if players need to see fusion ingredients or secondary requirements. Preserve native equip/toggle/keybind behavior.
5. Define one authoritative classification system for our own skills, rather than scattering inferred rank across names, config fields and hard-coded ID lists. This makes future compatibility more predictable.
6. If server-unique ownership is desired, expose it as a deliberate, configurable rule. Do not silently copy Nightmares' exclusivity limits or blanket authority behavior into Wild Hunt.
7. Test alongside Nightmares before promising compatibility: it adds broad Ultimate authority and UI hooks that affect skills outside its own namespace. Its current NeoForge minimum also exceeds ours.

No Nightmares dependency, borrowed skill code, gameplay change, native enum modification or UI patch is added by this research.

## Public references

- [Nightmares project](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated-nightmares)
- [Inspected NeoForge release, file 9120824](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated-nightmares/files/9120824)
- [Nightmares wiki 1.21.1 skills](https://tensuranightmares.wiki.gg/wiki/Abilities/1.21.1/Skills)
- [Version-specific Ultimate category landing page](https://tensuranightmares.wiki.gg/wiki/Category:Abilities/1.21.1/Skills/Ultimate_Skills)
- [Lord / ordinary Ultimates](https://tensuranightmares.wiki.gg/wiki/Category:Ultimates)
- [King-Class](https://tensuranightmares.wiki.gg/wiki/Category:King_Ultimates)
- [God-Class](https://tensuranightmares.wiki.gg/wiki/Category:God_Ultimates)
- [Dragon-Class](https://tensuranightmares.wiki.gg/wiki/Category:Dragon_Ultimates)

Wiki descriptions are treated as taxonomy/documentation; implementation claims above are grounded in the named methods from the versioned released JAR. Config values are defaults, not promises about another server's settings.
