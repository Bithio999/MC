# 0.35.3 validation — target-skill blacklist and Creator compatibility

Target: Java 21, Minecraft 1.21.1, NeoForge **21.1.209**, Tensura 2.0.1.3 and ManasCore 4.0.0.2. Independent addon: **1.1.3**. All **431 tests passed**: **327 full-mod native GameTests**, **93 standalone native GameTests** and **11 JUnit tests**. Both normal production builds passed.

## Target-skill policy

Fettered Human’s existing reincarnation filter now applies an independent, enabled-by-default `blockSkillTaking` rule. The public `wildhunt:fettered_skill_taking` tag, or `heavenlyrestriction:fettered_skill_taking` in the standalone, classifies skills that steal, copy, analyze or harvest target skills. The default tag includes Analyst, Chosen One, Degenerate, Gluttony, Gourmet, Great Sage, Predator, Seeker, Spearhead, Starved, Unyielding and Usurper. Pride is explicitly exempt from this automatic classification, even if a datapack adds it to the tag.

The newly excluded default starting skills are **Analyst**, **Great Sage**, **Seeker** and **Spearhead**. Native defaults retain **16 native / six Myths Uniques**, with **38 native / two Myths exclusions**. [Registry-ID snapshot](research/FETTERED_REINCARNATION_0.35.3.json). The native `getReincarnationSkills` return filter still covers both main and bonus pools; other races, intrinsic Chained and already owned skills remain. Explicit blacklist entries take priority; the allowlist can exempt a skill from both automatic rules without adding it to a pool.

Three new policy tests in each distribution verify free skill-taking exclusions, the Pride/Creator exceptions, an actual native birth guaranteed to select Pride over target-skill candidates, independent automatic settings and explicit operator overrides. Earlier magicule-cost, native absorption, pool, inheritance and empty-pool tests remain.

## Creator investigation and fix

Native `CreatorSkill.onPressed` opens `SkillCreationMenu`. Selecting an entry arrives in a later menu packet, outside `ManasSkillInstance.onPressed`’s trusted Chained context. The native menu adds a `Creator` NBT string but no inherited parent, so Chained’s gate previously rejected the generated Unique.

`ChainedCreatorMixin` wraps only the native menu callback for an owned, learned, permanent, non-sub-instance **Creator recorded as a reincarnation skill**, while Chained or Unbound is owned. It restores the existing trusted ability context for the synchronous native transaction and closes that scope on every return. The native menu retains its candidate list, callbacks, costs, temporary grants, slot assignment, cooldown, replacement and mastered renewal behavior. This does not exempt arbitrary skills based on a claimed `Creator` NBT tag.

Four new Creator tests in each distribution exercise the actual native `SkillCreationMenu.clickMenuButton` path:

- A plain generated instance with a `Creator` tag is blocked outside the trusted menu, reproducing the missing handoff. The same reincarnation Creator’s native menu successfully creates temporary Fighter, replaces it with temporary Observer, removes the first instance and renews Observer when mastered. Native timers and cooldowns match their configuration, and maximum magicules stay unchanged.
- A forged metadata tag and a menu without an owned Creator cannot authorize learning.
- A grandfathered Creator that was not recorded at reincarnation, and a temporary Creator, do not acquire a new blanket learning exemption.
- An ordinary Creator without Chained retains native temporary generation. Unrelated Musician gifts remain blocked after creation, proving the context does not leak.

Creator’s own generation is eligible; target-skill stealing/copying is separate. The reincarnation blacklist does **not** filter Creator’s generation menu. Generated abilities retain their own resource costs.

## Build and artifacts

```sh
gradle build runGameTestServer --console=plain
# Repeat in standalone/heavenly-restriction.
python3 standalone/heavenly-restriction/scripts/verify-distribution.py
python3 scripts/verify-visual-assets.py
PYTHONPATH=/workspace/toolchains/wiki-python python3 scripts/build-wiki.py
```

Validation redirected disposable GameTest run directories to `/tmp`, using copies of development configuration. The default pool snapshots from both native servers match the recorded lists. Production package checks cover pinned dependency metadata, the new common mixin, final policy classes and skill-tag data. Review/capture fixtures remain excluded. Both JARs and the offline wiki receive SHA-256 manifests; the current Markdown, HTML and GitHub-export wiki describe the revised rule and Creator compatibility.
