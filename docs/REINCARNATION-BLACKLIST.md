# Fettered Human reincarnation skill blacklist

Fettered Human’s main and bonus Unique rolls exclude a skill when **any of its own abilities consumes magicules**. Checks include mastered abilities and native passive/upkeep, healing, teleport and target-dependent costs. A free first mode does not make a skill eligible if another mode costs magicules.

Skills that only use aura or have no ability cost remain eligible. A permanent magicule price to **acquire** a skill is separate and does not trigger the filter. Magicule **gains** are not costs: for example, Gourmand’s energy-gain modifiers do not blacklist it.

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
extraBlacklist = []
allowlist = []
```

- `blockMagiculeCosts`: enables automatic detection; defaults to `true`.
- `extraBlacklist`: additional registry IDs to exclude, including later edge cases. This list has highest priority.
- `allowlist`: exceptions to automatic detection. An exception must still exist in Tensura’s configured pool and pass its native eligibility rules.

For example, adding `"tensura:creator"` to `extraBlacklist` excludes Creator without changing any other race’s pool. An explicitly allowed skill that is also blacklisted remains excluded.

## Default eligible skills

The pinned native defaults leave **20** eligible starting Uniques: Analyst, Berserker, Commander, Cook, Creator, Fighter, Gourmand, Great Sage, Martial Master, Mathematician, Observer, Pride, Reflector, Researcher, Royal Beast, Seeker, Seer, Spearhead, Survivor and Tuner. **34** native starting Uniques are excluded.

All **eight** aura-based Myths Uniques remain eligible: Storm Crown, Scarlet Deluge, Black Flame, Frostbound Colossus, Threadweaver, Rune Smith, Verdant Keeper and Mirrorbound. The standalone addon does not register these Myths skills. Other installed addons remain subject to their configured pools and exposed costs.

[Measured registry-ID snapshot](research/FETTERED_REINCARNATION_0.35.2.json). This is a reference snapshot; changing native ability prices or the override lists can change eligibility.

## Detection and addon edge cases

The automatic check inspects detached instances at ordinary and maximum mastery, with their toggle flag off and on, querying every declared mode through `TensuraSkill.getMagiculeCost`. It never executes an ability or applies toggle effects. It also reads the pinned native costs charged directly inside effects: Berserk, Chef, Greed, Healer, Infinity Prison, Lust, Reaper, Reverser, Sloth, Suppressor, Traveler and Unyielding.

Third-party skills that spend magicules internally without exposing their costs need an `extraBlacklist` entry until an adapter is added. Copied techniques, derived skills and external spells are not recursively treated as abilities of the rolled skill. A cost query that fails is logged once and excluded conservatively; `allowlist` provides an explicit override.
