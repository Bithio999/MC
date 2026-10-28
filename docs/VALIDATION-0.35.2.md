# 0.35.2 validation — Fettered Human reincarnation blacklist

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3 and ManasCore 4.0.0.2. Independent addon: **1.1.2**.

## Policy and integration

A server-only return injection in native `ReincarnationMenu.getReincarnationSkills` filters Fettered Human’s existing eligible candidates. Both primary and secondary pools, including locked candidates, pass through this method. Native race assignment occurs before the normal post-selection skill roll. Other primary races return the native result unchanged; intrinsic skill grants and previously recorded inheritance are separate.

`ChainedReincarnation` inspects fresh detached instances through Tensura’s cost API at ordinary/max mastery and with toggle flags off/on. It queries all declared modes without learning, casting or applying toggle effects. The pinned native inline charges are read from `ConfigRegistry`’s live `UniqueSkillConfig`; Reaper’s target-EP charge is explicitly recognized. It ignores acquisition prices and magicule-gain modifiers. No eligibility cache hides configuration or player-dependent changes.

[Configuration and scope](REINCARNATION-BLACKLIST.md) · [Measured default pool snapshot](research/FETTERED_REINCARNATION_0.35.2.json). `extraBlacklist` takes priority over `allowlist`; neither adds candidates or bypasses native eligibility. The automatic rule defaults on. Existing skills are retained, including skills rolled before race choice when `skillBeforeRace` is enabled. Hidden third-party internal charges require an explicit exclusion until adapted.

## Native verification

All **416 tests passed**: **319 full-mod native GameTests**, **86 independent standalone native GameTests** and **11 JUnit tests**. Both normal production builds passed.

```sh
gradle build runGameTestServer --console=plain
# Repeat in standalone/heavenly-restriction.
python3 standalone/heavenly-restriction/scripts/verify-distribution.py
python3 scripts/verify-visual-assets.py
PYTHONPATH=/workspace/toolchains/wiki-python python3 scripts/build-wiki.py
```

Seven new native tests in each distribution cover:

- Both configured native pools, free/aura-only eligibility, passive Sniper and inline Reverser/Unyielding/Reaper exclusions, unchanged global pools and read-only inspection.
- Other races and previously owned reincarnation roots, including retained ancestry.
- Live configured costs becoming zero/positive and a magicule cost on a later mode.
- Mastery-only and toggle-only costs through detached native registry-backed instances; no cast or toggle callbacks.
- Explicit exceptions, blacklist precedence and disabled automatic detection without candidate injection.
- Actual native `setRace(..., true, true)` birth with main and bonus rolls, guaranteed Chained and excluded Healer.
- An exhausted filtered pool, with no fallback grant or loss of existing skills.

The measured native defaults retain **20 / 54** native starting Uniques and all **8** aura-based Myths Uniques. The standalone has no Myths registry dependency.

The existing cross-dimension Aion cancellation test now waits for its Nether fixture chunk to become entity-ticking before spawning the remote visual, with a longer test timeout. This makes the test work in a fresh world; Aion gameplay is unchanged. GameTest run directories were placed under `/tmp` with copies of the normal development configs to avoid accumulating disposable regions in the repository workspace.

Normal production JARs contain the final policy/config classes, declared mixins and metadata, and exclude client-review/capture fixtures. Release JARs and the rebuilt offline wiki receive SHA-256 manifests. Current Markdown, HTML and GitHub-export pages document the policy and configuration.
