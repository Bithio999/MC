# Critical Merchant

[All wikis](../README.md) · [TensuraAdditionalAdditions](TensuraAdditionalAdditions.md) · [Skills](Additions-Skills.md)

![Critical Merchant](../assets/additions/critical-merchant.png)

| Skill data | Value |
| --- | --- |
| Tier | Unique |
| Obtaining | Normal reincarnation Unique pool, subject to server settings |
| Role | Critical-hit damage and guaranteed criticals |
| Resource cost | Passive: none · Active: 1,000 magicules |

![Critical Merchant in Tensura’s Unique menu](../media/critical-merchant-menu.png)

*Screenshots were recorded before the 1.26.2 balance change; current payout values are below.*

<a id="compound-interest"></a>

## Compound Interest · Passive, Toggle

Every critical hit receives a guaranteed **×1.5 damage bonus**. Each successful **50% roll** multiplies that bonus by **×1.25**. The chain ends on its **first failure** or after **100 successes**. There is **no separate payout cap**.

**Bonus multiplier = 1.5 × 1.25ⁿ**, where *n* is the number of successful rolls (0–100). This multiplies damage that has already received its normal critical bonus.

Each critical starts a fresh chain. Before defenses, a **100-damage critical** becomes **150**, then **187.5** after one successful roll, then **234.375** after two.

Enable the passive through Tensura’s **Skills → Unique** checkbox. **Damage type:** the original attack’s type. Normal armor, resistances, barriers and other defenses still apply.

<a id="guaranteed-profit"></a>

## Guaranteed Profit · Active, Press

Spend **1,000 magicules** to make your **next ten landed hits** critical hits. **Cooldown: 20 seconds.** Recasting refreshes the count to ten; it does not stack. The native skill mode shows the remaining hits.

This works independently of the passive. Toggle **Compound Interest** on to amplify those criticals. Native melee, owned projectiles and abilities are supported, including direct spiritual damage. Each damaged target counts as a hit. Misses, fully blocked hits and immune targets do not spend a guarantee; Tensura training-dummy damage counts.

Unbound’s existing guaranteed criticals retain one base critical multiplier. Guarantees clear on death or forgetting the skill. **Damage type:** none directly; affected attacks retain their normal types.

![A guaranteed critical on a training dummy](../media/critical-merchant-hit.png)

**Fettered Human:** excluded from its reincarnation rolls because the active costs magicules.

## Ultimate synthesis

Fully mastered Critical Merchant can combine with Tuner into [Tyche, Lord of Fortune](Additions-Tyche.md) after completed awakening and its personal achievements. Pending guaranteed hits and their remaining cooldown transfer.
