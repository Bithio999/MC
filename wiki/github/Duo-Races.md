# Duo Races

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Convergent Awakening](https://github.com/Bithio999/MC/wiki/Convergent-Awakening) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

**Duo Races** let an awakened player develop a second ancestral race alongside their primary race. The primary race retains the body, size and normal racial key. The secondary supplies half-strength shared attributes, its native intrinsic skills, racial reactions and a separate racial action.

## Obtaining

Open **Tensura Myths** from Tensura’s status or evolution screen, or enter `/myths`. All of these conditions are required:

| Requirement | Default |
| --- | --- |
| Primary evolution | Final stage: no next native evolution |
| Awakening | True Hero **or** True Demon Lord |
| Base EP | At least 5,000,000 |
| Secondary slot | Empty |
| Ancestry | Different from the primary family |

A Hero Egg or Demon Lord Seed alone does not qualify. Selection is permanent for the current life; the menu asks for a second click to confirm. Reincarnation or an external change of the primary race resets the Duo state. Ordinary death preserves it.

The supported native families are **Beastfolk, Harpy, Ogre, Goblin, Giant, Orc, Lizardman, Merfolk, Elf, Human, Dwarf, Slime, Vampire and Daemon**, plus **Wight**. Vampire starts as Ghoul, and Daemon starts as Lesser Daemon. The hunter branch belongs to Human. Other addons’ races are not automatically added to the catalog.

## Secondary evolution

The secondary starts at its native base race. Select its next stage in the same menu, inspect the native requirements in the tooltip, and choose **Evolve secondary**. Native EP, awakening, mastery and other conditions are evaluated by the server. Scroll the right column for longer details.

Native branching remains available. If a chosen Goblin branch becomes an Oni, its recorded secondary ancestry remains Goblin for the eventual fusion. Secondary Human can also follow the hunter branch and earn its distinct boss credits.

Evolution replaces the secondary’s half-strength attribute contributions. It does not award a fresh starting energy pool. Shared attributes are HP, SHP, attack damage, armor, toughness, movement and knockback resistance. Energy caps, body size and appearance continue to belong to the primary until Convergent Awakening. Learned intrinsic skills use native skill storage; native learning rules can reject incompatible skills.

Assign **Secondary racial ability** under **Controls → Key Binds → Tensura Myths**. It starts unbound to avoid conflicts. Pressing, holding and releasing invoke the secondary’s native racial action with server-measured duration, native cooldowns and energy costs. The normal Tensura racial key continues to use the primary race.

## EP gains

The formula applies to native kill EP awards before their existing caps and penalties:

**Duo aura weight = current primary aura weight + ½ secondary base aura weight**

**Duo magicule weight = current primary magicule weight + ½ secondary base magicule weight**

These contributions are additive; they are not normalized back to the previous total. The secondary contribution always uses the chosen **base ancestry**, even after it evolves.

| Example | Aura weight | Magicule weight | Gains at a baseline of 3 |
| --- | ---: | ---: | --- |
| Default Human / Hunter | 2 | 1 | 6 aura / 3 magicules |
| Slime base | 1 | 2 | 3 aura / 6 magicules |
| Human / Hunter + secondary Slime | 2.5 | 2 | **7.5 aura / 6 magicules** |
| Holy Divine Human + secondary Slime | 1.5 | 3 | 4.5 aura / 9 magicules |

Advanced native Human stages can have Holy alignment. Their current primary split is preserved; the default Human/Hunter example does not override it. Gaining a secondary does not retroactively redistribute saved energy.

## Awakening

Once **both** races finish evolving, follow [Convergent Awakening](https://github.com/Bithio999/MC/wiki/Convergent-Awakening). The new fused race replaces the Duo growth formula with its own profile.
