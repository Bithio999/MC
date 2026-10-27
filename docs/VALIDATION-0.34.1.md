# 0.34.1 validation — Restricted recovery and Chained dash

Target: Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
PYTHONPATH=/workspace/toolchains/wiki-python python scripts/build-wiki.py
```

**BUILD SUCCESSFUL. All 278 required native GameTests and all 11 JUnit tests passed.**

## Gameplay checks

Unshackled grants Hunter’s Recovery, Heavenly Restricted grants Apex Renewal, and Heavenbreaker grants Sovereign Rebirth. Native racial learning retains previous tiers, enables a newly learned tier and avoids permanent magicule acquisition costs. Only the strongest toggled recovery heals.

A narrowly scoped, owner-bound exception allows only eligible aura-recovery tiers during Restricted racial grants. It preserves surrounding reincarnation/initialization context and does not mark ordinary evolution recovery as a reincarnation inheritance root. Other Extra Skills remain blocked by Chained. Fettered Human remains the only race that grants Chained itself.

Existing eligible primary or secondary lineages receive missing recovery tiers on login or the server’s player migration tick. A saved stage marker prevents repeated grants. New reincarnation resets that marker. Native skill learning preserves existing mastery and disabled toggles.

Severance Rush uses 1.5 × √A path damage and 3 × √A arrival damage, where A is the aura paid: exactly half the preceding coefficients. The dash has no cooldown, clears a previous saved cooldown during refresh, and still charges 2% of current aura per successful press. Range, collision checks, separate path/arrival hits, damage type and the 5× toggled Battlewill bonus retain their existing behavior.

Regression coverage includes actual native race learning with Chained owned, all four evolution stages, recovery-tier availability, unchanged magicule capacity, aura-only HP/SHP healing, single strongest-tier billing, existing-save grants, mastery/toggle preservation, secondary migration, unrelated-skill rejection, immediate consecutive teleports and payments, damage formula boundaries, fully blocked movement and no regrant of forgotten Chained.

## Documentation and distribution

- 11 JUnit tests pass with no failures or errors.
- 162 HTML pages, 86 ability anchors and 9,633 local references validate. All 789 archived wiki files match their sources byte-for-byte.
- 16 responsive checks across desktop and mobile cover Chained, all three affected evolutions and the recovery articles. Images, infoboxes and search work without overflow or browser errors.
- JAR metadata and revised classes are verified, with opt-in review fixtures excluded.

Current race/recovery articles, Chained’s formula/examples/cooldown and README describe the final behavior. The styled offline edition and prepared GitHub Wiki mirror are regenerated. No visual assets or menu layouts change; a graphical Minecraft client was not rerun.

Distribution: `releases/tensura-myths-0.34.1.jar`, `releases/tensura-myths-wiki-0.34.1.zip`, and `releases/tensura-myths-0.34.1.sha256`. Install the replacement addon on both client and server, with only one addon version present. Registry IDs remain under `wildhunt`.
