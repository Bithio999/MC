# Tyche, Lord of Fortune

[All wikis](https://github.com/Bithio999/MC/wiki/Home) · [TensuraAdditionalAdditions](https://github.com/Bithio999/MC/wiki/TensuraAdditionalAdditions) · [Skills](https://github.com/Bithio999/MC/wiki/Additions-Skills)

![Tyche](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/assets/additions/tyche.png)

| Skill data | Value |
| --- | --- |
| Tier | Ultimate |
| Components | Fully mastered Critical Merchant + Tuner |
| Awakening | True Hero / True Demon Lord; awakening complete |
| Achievement | 50 landed compounded criticals and three distinct damage types survived with Tuner |
| Acquisition | 250,000 permanent magicules by default; configurable, affected by mpSkillCost |
| EP requirement | None |

![Tyche synthesis menu](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/tyche-synthesis.png)

![Tyche in Tensura’s Ultimate menu](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/tyche-native-ultimate-menu.png)

Owning either component reveals Tyche in **Skills → Ultimate**. Open the unfinished entry, meet the requirements and confirm synthesis. Awakening bonuses apply before payment. Both parents are replaced by learned, unmastered Tyche. Existing guaranteed hits, their cooldown and Tuner’s current damage-history lockouts transfer. Affected presets point to Guaranteed Dividend.

<a id="loaded-dice"></a>
## Loaded Dice · Passive, Toggle

Criticals begin at **×4**, then use Critical Merchant’s **50% doubling chain** with up to **100 additional successful doublings**. The first failed roll gets **one reroll per chain**. This improves the critical’s payout; it does not itself make every attack critical. Damage retains its original type and normal defenses apply.

<a id="against-all-odds"></a>
## Against All Odds · Passive, Toggle

Below **50% HP**, progressively gain up to **+50% attack damage** and **+25 percentage points of melee/projectile evasion** as HP approaches zero. Recovering HP reduces the bonuses. Uses the same native Ultimate-menu toggle as Loaded Dice. **Damage type:** none directly.

<a id="fate-insurance"></a>
## Fate Insurance · Passive, True

An eligible fatal hit is prevented. Recover **full HP, SHP, magicules and aura**, remove harmful status effects and **reset all other native skill/ability cooldowns**. Capacities remain your current maximums.

The incoming damage type is recorded using Tuner’s native damage-message grouping. The first revival starts a **five-minute history window**. Each new type can trigger once during that window; an already recorded type cannot. Later revivals **do not extend the window**. When it expires, all recorded types become eligible again.

![Fate Insurance history](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/tyche-insurance-history.png)

The reset preserves **Fate Insurance’s own timer and history**. Void, `/kill`, native level-three barrier-bypass attacks and Tuner’s self/owner-attack exclusions remain ineligible. **Damage type:** none.

<a id="guaranteed-dividend"></a>
## Guaranteed Dividend · Active, Press

**5,000 magicules · 20-second cooldown.** Your next **ten landed hits** are guaranteed criticals. Each rolls two Loaded Dice chains and uses the higher result, including when the passive is off. Recasting refreshes to ten; fully blocked, immune or failed hits do not spend a guarantee. Melee, owned projectiles and direct spiritual damage use the shared critical pipeline.

Shift-press opens synthesis or your current insurance history. **Damage type:** the original attack’s type.

<a id="the-house-always-wins"></a>
## The House Always Wins · Active, Press

**20,000 magicules · 90-second cooldown.** For **15 seconds**, a **16-block** domain follows you:

- Nearby enemies’ native critical hits against you become ordinary hits.
- Your attacks bypass enemy dodge inside the domain.
- Incoming attacks have an additional **25%** chance to miss. Native dodge-bypass attacks retain their bypass.

Alliance and PvP rules apply. The field ends on losing the skill, respawning, logging out or changing dimensions. **Damage type:** none directly.

![The House Always Wins](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/tyche-house.png)

Configuration: `tensura-additions-tyche.toml` controls synthesis cost, achievement counts and active magicule costs.
