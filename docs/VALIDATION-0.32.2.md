# 0.32.2 validation — Unmultiplied starting aura

Validated with Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
python scripts/verify-visual-assets.py
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

**BUILD SUCCESSFUL. All 259 required native GameTests and all 11 JUnit tests passed.** The native suite includes 29 Chained checks and the existing 230 gameplay regressions.

## Starting pools and growth

- The native starting range is **1,500–2,000 base aura** and **50–100 base magicules**. Fresh native reincarnations initialize current aura to the actual aura roll and preserve the rolled magicule capacity as Chained’s ceiling.
- Chained leaves the starting aura unmultiplied. Without other modifiers, starting effective EP equals raw aura plus raw magicules: **1,550–2,100** before separate reincarnation rewards. The race roll is captured before the native menu grants those rewards.
- With no other capacity multipliers, effective maximum aura is **starting aura + 5 × subsequent raw aura-capacity gains**. Tests verify helper/base growth, equipment additions, removal, foreign multipliers, native kill EP and the existing secondary-race bonus.
- The starting exemption persists through four native evolutions, skill NBT reload, toggle/refresh, and forgetting/relearning. It is stored in persistent player data and copied into the skill’s saved tag. Forgetting removes both Chained capacity modifiers without removing earned raw capacity.
- Eight repeated native births produce multiple valid rolls and replace the starting exemption and magicule ceiling, including while Chained is already owned. Ordinary evolution does not reroll either resource.
- Capacity loss below the starting pool and subsequent recovery leave the remaining/recovered starting amount unmultiplied. Growth beyond the starting pool resumes 5× scaling immediately. Chained’s own modifiers are excluded from Battlewill buff amplification when a Battlewill callback changes capacity.
- Older Chained owners exclude their historical fixed 20,000 starting aura while retaining earned raw capacity and mastery. Migration also works after a race change and when losses have reduced raw aura below the historical starting pool. Repeated refreshes do not compound it. Existing characters are not rerolled into the new ranges.
- All previous 90/10 EP allocation, fixed evolution gates, acquisition-cost, inheritance, regeneration, combat, defense, dodge, dash and synthesis checks continue to pass.

## Documentation and distribution

Updated Chained, the five race articles, starting-pool infobox fields, README and release references. Development history remains in the changelog and validation reports.

The offline wiki passes **160 HTML pages**, **9,342 local references**, **85 ability anchors** and **782 byte-identical archive entries**. Sixteen desktop/mobile page checks confirm infoboxes, portraits, ability search and the existing formation gallery without missing images, overflow or browser errors. The prepared GitHub Wiki mirror is regenerated; the separate GitHub Wiki remains disabled.

The existing visual asset graph passes for 73 protocol effects, 79 editor/export pairs and 171 animation clips. Visuals are unchanged; a graphical Minecraft client was not rerun for this resource change. The release JAR reports version 0.32.2, contains the updated gameplay classes and excludes opt-in client review fixtures.

Distribution: `releases/tensura-myths-0.32.2.jar`, `releases/tensura-myths-wiki-0.32.2.zip` and `releases/tensura-myths-0.32.2.sha256`. Replace the addon on both client and server and install only one version. Existing registry IDs retain the `wildhunt` namespace.
