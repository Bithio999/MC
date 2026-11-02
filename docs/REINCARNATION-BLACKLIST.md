# Fettered Human reincarnation skill blacklist

Fettered Human’s main and bonus Unique rolls exclude a skill when **any of its own abilities consumes magicules**, or it **steals, copies or harvests skills from a target or its projectiles**. **Pride is explicitly exempt from the skill-taking rule.** Checks include mastered abilities and native passive/upkeep, healing, teleport and target-dependent costs. A free first mode does not make a skill eligible if another mode costs magicules.

Skills that only use aura or have no ability cost remain eligible if they do not take target skills. A permanent magicule price to **acquire** a skill is separate and does not trigger the filter. Magicule **gains** are not costs: for example, Gourmand’s energy-gain modifiers do not blacklist it.

## Scope

The rule applies when the primary race is **Fettered Human at roll time**. It filters Tensura’s existing eligible lists, including the secondary pool and locked-skill candidates, while preserving native roll counts, chances and Truly Unique checks. It does not add candidates to a pool, remove skills already owned, change recorded reincarnation inheritance or block intrinsic Chained. Other races retain their existing pools, even if they own Chained.

With `skillBeforeRace` enabled, skills already rolled before selecting Fettered Human are already owned and are retained. The filter does not predict a future race choice or replace existing skills.

If filtering leaves no primary candidates, native reincarnation grants no random Unique; intrinsic Chained remains.

## Server configuration

The files are generated after loading a world or starting a server with the addon:

| Distribution | Default file |
| --- | --- |
| Tensura Myths | `config/tensura-myths-reincarnation.toml` |
| Standalone Heavenly Restriction | `config/tensura-heavenly-restriction-reincarnation.toml` |

A world’s `serverconfig/` file of the same name can override the instance default. Restart the server or reopen the world after editing.

```toml
blockMagiculeCosts = true
blockSkillTaking = true
extraBlacklist = []
allowlist = []
```

- `blockMagiculeCosts`: enables automatic detection; defaults to `true`.
- `blockSkillTaking`: excludes the known skill-taking abilities classified by the addon’s `fettered_skill_taking` skill tag; defaults to `true`. Pride remains exempt from this automatic rule.
- `extraBlacklist`: additional registry IDs to exclude, including later edge cases. This list has highest priority.
- `allowlist`: exceptions to both automatic rules. An exception must still exist in Tensura’s configured pool and pass its native eligibility rules.

For example, adding `"tensura:creator"` to `extraBlacklist` excludes Creator without changing any other race’s pool. An explicitly allowed skill that is also blacklisted remains excluded.

Prospector in TensuraAdditionalAdditions has no magicule ability cost and does not take target skills, so it remains eligible for Fettered Human. Its Refine menu consumes recipe ingredients.

## Default eligible skills

The pinned native defaults leave **16** eligible starting Uniques: Berserker, Commander, Cook, Creator, Fighter, Gourmand, Martial Master, Mathematician, Observer, Pride, Reflector, Researcher, Royal Beast, Seer, Survivor and Tuner. **38** native starting Uniques are excluded. Analyst and Seeker learn projectile skills, Great Sage copies target/projectile skills, and Spearhead harvests skills from fallen subordinates.

**Six** Myths Uniques remain eligible: Storm Crown, Black Flame, Threadweaver, Rune Smith, Verdant Keeper and Mirrorbound. **Scarlet Deluge and Frostbound Colossus are excluded** because their multilayer barriers spend native magicules when absorbing damage, despite aura-only activation. If the native multilayer absorption price is configured to zero, that exclusion no longer applies. The standalone addon does not register these Myths skills. Other installed addons remain subject to their configured pools and exposed costs.

[Measured registry-ID snapshot](research/FETTERED_REINCARNATION_0.35.3.json). This is a reference snapshot; changing native ability prices or the override lists can change eligibility.

## Creator and Chained

Creator remains eligible because it generates its own skills rather than taking them from targets. A permanent, recorded reincarnation Creator can use the native Skill Creation menu under Chained or Unbound. Generated skills retain native temporary duration, replacement, renewal and cooldown behavior; normal resource costs of their abilities still apply.

The reincarnation blacklist filters random rolls. It does not filter Creator’s configured generation menu. Creator’s scope does not permit unrelated gifts, and a forged `Creator` metadata tag cannot authorize learning. A Creator owned before gaining Chained but not recorded as a reincarnation skill does not gain a new exemption.

## Detection and addon edge cases

The automatic check inspects detached instances at ordinary and maximum mastery, with their toggle flag off and on, querying every declared mode through `TensuraSkill.getMagiculeCost`. It never executes an ability or applies toggle effects. For Myths, it checks native absorption costs on Apex skills with multilayer barriers. It also reads the pinned native costs charged directly inside effects: Berserk, Chef, Greed, Healer, Infinity Prison, Lust, Reaper, Reverser, Sloth, Suppressor, Traveler and Unyielding.

Third-party skills that spend magicules internally without exposing their costs need an `extraBlacklist` entry until an adapter is added. Third-party skill-taking abilities can be added to `extraBlacklist` or the distribution’s `fettered_skill_taking` skill tag. The built-in tag covers Analyst, Chosen One, Degenerate, Gluttony, Gourmet, Great Sage, Predator, Seeker, Spearhead, Starved, Unyielding and Usurper. Pride is explicitly excluded from that classification even if another datapack adds it to the tag. Copied techniques, derived skills and external spells are not recursively treated as abilities of the rolled skill. A cost query that fails is logged once and excluded conservatively; `allowlist` provides an explicit override.
