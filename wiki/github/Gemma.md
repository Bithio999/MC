# Gemma

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Otherworlders](https://github.com/Bithio999/MC/wiki/Otherworlders) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

Gemma is the expedition smith. Her skin follows the blonde hair, goggles, cropped brown jacket, cream top, utility belt and ochre work trousers of her base appearance.

| NPC data | Gemma |
| --- | --- |
| **Information** | |
| Origin | Monster Hunter Wilds |
| Race | [Awakened Hunter](https://github.com/Bithio999/MC/wiki/Awakened-Hunter) |
| **Spawning** | |
| Entity | `wildhunt:gemma` |
| System | Native Tensura Otherworlder |
| Skin | Bundled original 64×64 classic Minecraft skin |


## Spawning

Gemma participates in Tensura's **otherworlder spawn distribution**, including its natural otherworlder replacement and summon selection. Each of the eight regular native entries and these two hunters has a 0.10 weight. Special Shizu/Hinata rolls use Tensura’s native rules. A datapack can override these distributions and loadouts.

Use `/summon wildhunt:gemma` or `/give @s wildhunt:gemma_spawn_egg` for a direct check. Native otherworlder ownership, inventory, home handling, interaction menus and configured Unique-skill drops are inherited. Races, skills and equipment persist through native saving; loading never reinitializes them. Their native entity data supplies 160,000 aura / 80,000 magicules before any applicable modifiers.

## Abilities

| Ability | Registry ID | Damage / effect classification | Character fit |
| --- | --- | --- | --- |
| Godly Craftsman | `tensura:godly_craftsman` | None — crafting (native Unique Skill) | Forge and improve equipment |
| Analytical Appraisal | `tensura:analytical_appraisal` | None — analysis / perception | Judge materials and weapon quality |
| All Seeing Eye | `tensura:all_seeing_eye` | None — analysis / perception | Spot fine defects |
| Strengthen Body | `tensura:strengthen_body` | None directly — physical attribute enhancement | Handle heavy tools |
| Steel Strength | `tensura:steel_strength` | None directly — physical attribute enhancement | Support physical smithing work |
| Flame Manipulation | `tensura:flame_manipulation` | Fire — native `tensura:flame_breath`; conditional black-flame breath / burning | Control forge heat |
| Ultraspeed Regeneration | `tensura:ultraspeed_regeneration` | None — healing | Recover from workshop and field injuries |

Flame Manipulation’s native held action emits Flame Breath; native conditions can select Black Flame Breath instead, and its touch hook can ignite targets. Owning the skill does not guarantee that the NPC AI uses every player-controlled action. Strengthen Body and Steel Strength modify physical capabilities rather than create an independent damage source.

These are real Tensura abilities learned and toggled through native entity-existence data. They use Tensura's effects, passive logic and available NPC behavior. A native humanoid AI handles movement, hostility, melee and subordinate commands; a player-only UI or manual mode of a skill does not gain an invented autonomous AI here. The race also grants Hunter's Recovery as a separate racial Extra Skill.

## Gallery

![Gemma and Alma in the Minecraft client](https://github.com/Bithio999/MC/blob/main/wiki/media/alma-gemma.png?raw=true)

---

**Categories:** Tensura Myths · Otherworlders
