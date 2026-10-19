# Ymir, Lord of Winter

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

**Ymir, Lord of Winter** is a Ultimate Skill inspired by **Jin Dahaad**. It combines avalanche impacts, ice capture, and a defensive carapace.

![Ymir, Lord of Winter](https://github.com/Bithio999/MC/blob/main/src/main/resources/assets/wildhunt/textures/skill/ymir.png?raw=true)

| Skill data | Ymir, Lord of Winter |
| --- | --- |
| **Information** | |
| Type | Ultimate Skill |
| Inspiration | Jin Dahaad |
| Damage type | Ice elemental |
| Damage ID | `tensura:ice_elemental` |
| Combat role | Avalanche impact / capture / ice siege |
| Active modes | 3 |
| Toggleable passive | Colossus Carapace |
| Passive upkeep | 80 aura / second |
| Registry ID | `wildhunt:ymir` |
| **Evolution** | |
| Previous | [Frostbound Colossus](https://github.com/Bithio999/MC/wiki/Frostbound-Colossus) |
| Evolution | [Aion, God of Causality](https://github.com/Bithio999/MC/wiki/Aion) |
| **Obtaining** | |
| Method | Ascend the matching mastered Unique |

All damaging arts in this skill use **ice elemental damage**, including its area hits and pulses.

## Usage

### Passive

[Passive - Toggle] — **Colossus Carapace**

While toggled on, gain 40 armor, 72 physical barrier, 36 magic barrier, one knockback resistance and 100% water-element boost. Tensura classifies ice under water for this boost. This uses the native barrier layer and damage rules, not absorption or the former projectile-deletion ward.

The passive also arms a **350-capacity native Multilayer Barrier**, which appears in Tensura’s shield HUD counter when its Barrier decoration is enabled. The counter is a per-hit blocking capacity, not absorption hearts or a custom health bar. Native rules spend magicules on blocked damage and can break the barrier on overflow, insufficient magicules or stronger attacks. While still toggled on, a broken barrier automatically rearms on the first paid upkeep after **ten seconds without an incoming hit**. Any new hit restarts the delay, including hits completely blocked by a surviving shield. Retoggling does not bypass the delay. Surviving shields persist across relogs.

Enable or disable this through Tensura’s native **Ability Selection** skill toggle. It is separate from active mode cycling and has no fifteen/thirty-second duration. Upkeep is **80 aura per second**, with the first second paid on enabling. It turns off when aura is insufficient or activation requirements fail. Turning it off removes its own attribute modifiers immediately.

The toggle uses native barrier rendering and the shield HUD.

### Active

| Mode | Aura | Cooldown | Raw opening damage |
| --- | ---: | ---: | ---: |
| 1. Avalanche Crash | 800 | 3 s | 300 |
| 2. Glacial Prison | 2,600 | 7 s | 165 |
| 3. Heavenfall Glacier | 6,000 | 60 s | See signature below |

**Damage calculation:** multiply the listed base values by **0.90**, then by the mastery multiplier (up to **1.50**). Ultimate table values already include their 3× base multiplier. Elemental boosts, defenses, barriers, and situational modifiers also apply. EP does not multiply damage.

#### Avalanche Crash

[Active - Press] — **Avalanche Crash**

Aim at a visible enemy up to 24 blocks away, or at the endpoint if no enemy is selected. Erupt eight rendered ice spires around the impact. Damage enemies in a 6-block radius and launch them upward and outward, respecting knockback resistance.

#### Glacial Prison

[Active - Press] — **Glacial Prison**

Aim at one visible hostile within 28 blocks. Deal the listed hit damage, then apply Tensura’s native Frost effect for up to 6 seconds, subject to its rank and resistance checks. Six rendered spires and an Ice magic circle mark the prison. The visual cage places no blocks and adds no separate collision wall. There is no projectile-interception budget.

#### Heavenfall Glacier

[Active - Press] — **Heavenfall Glacier**

Anchor an ice siege at the selected enemy’s position within twenty-four blocks, or at the aim endpoint. For ten seconds, select up to three enemies within ten blocks of that anchor each second. Mark their current positions with circles and falling ice. Each mark explodes after ten ticks (half a second), striking a 2.5-block radius. Opening raw damage is 300 per impact; later impacts use 135, before mastery and native modifiers. Overlapping marks cannot strike one victim twice in the same resolution tick. Enemies can leave the marked locations before impact.

## Obtaining

Evolve [Frostbound Colossus](https://github.com/Bithio999/MC/wiki/Frostbound-Colossus) as Sovereign Hunter with 2,000,000 base EP and mastery of that Unique. Use `/wildhunt ascend jin_dahaad`. It replaces only the matching Unique; the Ultimate starts with its own mastery.

## Evolution

| Unique Skill | Ultimate Skill | Requirements |
| --- | --- | --- |
| [Frostbound Colossus](https://github.com/Bithio999/MC/wiki/Frostbound-Colossus) | Ymir, Lord of Winter | Sovereign Hunter; 2,000,000 base EP; matching Unique mastered |

Ascension replaces only the matching Unique. The Ultimate starts with its own mastery.

## Suggested combo

Capture an enemy with Glacial Prison → aim Avalanche Crash at the captive and nearby foes. As Ymir, use the prison to hold a target within Heavenfall Glacier’s warning circles.

## Strengths and limitations

Prison uses native Frost, so immunity, skill rank and stronger defensive effects can affect it. The cage mesh is visual. Icefall marks stay where they were placed and can be dodged. Neither spires nor falling ice change terrain. There are no rime stacks, shatter preparation, finite projectile wards or expanding frostfronts.

## In-game preview

![Glacial Prison](https://github.com/Bithio999/MC/blob/main/wiki/media/glacial-prison.png?raw=true)

In-game example of the active art.

## Native barrier HUD

![Native shield HUD counter](https://github.com/Bithio999/MC/blob/main/wiki/media/colossus-carapace-hud.png?raw=true)

The Unique passive’s native shield counter. Its Ultimate uses a higher capacity.

## Related pages

[Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

---

**Categories:** Tensura Myths

## Godly evolution

At mastery, True Hero or True Demon Lord awakening, and 12M base EP, use **Skills → Godly → Ascension** to evolve this Ultimate into [Aion, God of Causality](https://github.com/Bithio999/MC/wiki/Aion). It replaces the selected Ultimate while preserving presets and remaining cooldowns.
