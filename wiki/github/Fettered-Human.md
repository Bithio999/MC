# Fettered Human

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Race directory](https://github.com/Bithio999/MC/wiki/Races) · [Chained](https://github.com/Bithio999/MC/wiki/Chained)

**Fettered Human** is the starting race of the **Heavenly Restriction** lineage. A human-looking martial body exchanges magical growth for exceptional physical and aura potential through its intrinsic Unique Skill, **Chained**.

![Fettered Human](https://github.com/Bithio999/MC/blob/main/wiki/assets/race/fettered_human.png?raw=true)

| Race data | Fettered Human |
| --- | --- |
| **Information** | |
| Registry ID | `wildhunt:fettered_human` |
| Ancestry | Human |
| Stage | Starting race |
| Reincarnation difficulty | Hard |
| Starting base aura | 1,500–2,000; unaffected by Chained |
| Starting base magicules | 50–100 |
| Starting EP | 1,550–2,100 before separate reincarnation rewards |
| Intrinsic Unique | [Chained](https://github.com/Bithio999/MC/wiki/Chained) |
| HP racial bonus | +0 |
| SHP racial bonus | +0 |
| Attack bonus | +0 |
| Movement bonus | +0% of base movement |
| Knockback resistance bonus | +0 |
| Previous evolution | None; selected at reincarnation |
| Next evolution | [Tempered Vessel](https://github.com/Bithio999/MC/wiki/Tempered-Vessel) |

## Traits

You keep a human appearance and normal equipment. Each evolution replaces the previous stage’s bonuses.

EP growth splits **90% aura / 10% magicules**. [Chained](https://github.com/Bithio999/MC/wiki/Chained#heavenly-restriction) multiplies later aura-capacity gains by **5×** and blocks magicule-capacity gains; the blocked share is not redirected. A fresh Fettered Human starts with **1,500–2,000 aura** and **50–100 magicules** (**1,550–2,100 EP** before other rewards). Starting aura is not multiplied.

[Chained](https://github.com/Bithio999/MC/wiki/Chained) governs true resource/learning restrictions, the martial toggle, mastered resistance degradation, in-slot dodge and Severance Rush. The same skill and mastery persist through the lineage.

![Fettered Human in the native Status menu](https://github.com/Bithio999/MC/blob/main/wiki/media/chained-status.png?raw=true)

The native-client photograph uses staged resource values for visual review.

## Intrinsic skill

The intrinsic Unique [Chained](https://github.com/Bithio999/MC/wiki/Chained) supplies the lineage’s true restriction and martial abilities. Its active [Severance Rush](https://github.com/Bithio999/MC/wiki/Chained#severance-rush) is an instant teleport that deals native **Physical** damage (`minecraft:mob_attack`). The race’s attribute bonuses are passive traits with no direct damage.

## Reincarnation skill pool

When rolling as Fettered Human, a Unique is excluded if any of its own abilities costs **magicules**, including mastered modes and native passive/upkeep costs. Aura-only and free skills remain eligible. A skill’s acquisition price does not count as an ability cost.

The default native pool leaves **20 eligible Uniques**, including Martial Master, Great Sage, Observer and Pride. All eight aura-based Myths Uniques remain eligible. Both main and bonus pools use the restriction. Intrinsic Chained is guaranteed; already owned skills and their recorded inheritance remain. With `skillBeforeRace`, a roll completed before race selection remains an already owned skill.

Other races keep their normal pools. Server operators can configure the rule in `config/tensura-myths-reincarnation.toml`, or `config/tensura-heavenly-restriction-reincarnation.toml` for the standalone addon. A same-named file in the world’s `serverconfig/` folder can override the default.

```toml
blockMagiculeCosts = true
extraBlacklist = []
allowlist = []
```

`extraBlacklist` adds excluded skill registry IDs and takes priority. `allowlist` exempts registry IDs from automatic cost checks, while keeping native pool and Truly Unique requirements. Restart the server or reopen the world after editing. Third-party skills with hidden internal costs can be added to `extraBlacklist`.

The default native eligible Uniques are Analyst, Berserker, Commander, Cook, Creator, Fighter, Gourmand, Great Sage, Martial Master, Mathematician, Observer, Pride, Reflector, Researcher, Royal Beast, Seeker, Seer, Spearhead, Survivor and Tuner.

## Evolution

![Fettered Human in the native reincarnation menu](https://github.com/Bithio999/MC/blob/main/wiki/media/fettered-human-selection-0.35.1.png?raw=true)

The reincarnation panel shows **Hard** difficulty, starting resource ranges, Chained and the race description.

Select Fettered Human from Tensura’s native reincarnation race menu. It guarantees Chained and still permits the normal reincarnation skill roll. Progression is available in the normal native evolution menu.

Evolution checks **total EP**: effective maximum aura plus effective maximum magicules, as shown in Status. Chained’s bonus aura capacity counts. The milestones remain fixed constants, independent of the player’s EP when the lineage is selected.

| Stage | Fixed total EP | Mastered Battlewills | Chained mastery |
| --- | ---: | ---: | ---: |
| [Fettered Human](https://github.com/Bithio999/MC/wiki/Fettered-Human) | Starting selection | — | — |
| [Tempered Vessel](https://github.com/Bithio999/MC/wiki/Tempered-Vessel) | 100,000 | 1 | 20% |
| [Unshackled](https://github.com/Bithio999/MC/wiki/Unshackled) | 500,000 | 2 | 40% |
| [Heavenly Restricted](https://github.com/Bithio999/MC/wiki/Heavenly-Restricted) | 2,000,000 | 3 | 60% |
| [Heavenbreaker](https://github.com/Bithio999/MC/wiki/Heavenbreaker) | 8,000,000 | 4 | 100% |

---

**Categories:** Tensura Myths · Races
