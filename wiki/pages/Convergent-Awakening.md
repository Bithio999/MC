# Convergent Awakening

[Main Page](../README.md) · [Races](Races.md) · [Ability index](Abilities.md) · [Damage types](Damage-Types.md)

**Convergent Awakening** unites two fully evolved lineages into a single registered race. There are **105 unique pairs** among the fifteen supported ancestral families. Every pair has its own name, original pixel icon, registry ID and EP growth profile. Reversing the pair produces the same fused race.

## Obtaining

Open Tensura’s menu and select the **Convergent Awakening tab on the right**, below the Secondary Race tab. Both lineages must be at a final stage with no further native evolution. You must also be a **True Hero or True Demon Lord** and meet the **lineage-dependent base EP requirement** shown in that tab. A base or partially evolved secondary cannot awaken.

The primary becomes the actual fused native race. Its predecessor state and the secondary’s native state are saved for inherited reactions and ticks. The fusion has no next evolution. The independent secondary racial key stops operating; the normal racial key uses **Concordance Pulse** instead.

## EP requirement

By default, the requirement is **125% of the highest EP gate leading up to the awakening**, with a **10,000,000 base EP minimum**. This includes native EP requirements along both current lineage paths and the added cumulative EP gate of the last completed secondary evolution. It uses required EP, not the amount you happened to have when evolving; earning more EP does not move the goalposts.

`Required base EP = max(10,000,000, ceil(highest preceding evolution EP gate × 1.25))`

For example, selecting Slime at 20M EP and completing its two evolutions at the added 22.5M and 25M gates gives a **31.25M EP awakening requirement**, provided neither native lineage has a higher gate. A native evolution requiring 40M raises awakening to **50M** instead. Both races must still reach their final evolution, and the true awakening requirement remains.

Server owners can configure the minimum with `convergenceEP` and the multiplier with `convergenceEvolutionEPFactor`. The menu and server checks use the same calculated threshold. Previously completed Convergent Awakenings remain complete.

## Awakening rewards

- A one-time **25% increase to each base aura and magicule cap**, preserving their existing proportions.
- Full HP, SHP and energy recovery on the successful awakening.
- A new intrinsic list drawn from both native final ancestral races, subject to native learning rules, with previously learned skills retained.
- Native race tags drawn from both final ancestral races.
- A fused attribute profile derived from 65% of both native final stages, plus 80 HP, 1,500 SHP and a pair-dependent attack bonus; the total racial HP bonus is capped at 1,000.
- A distinct future EP growth profile with the inherited **10% EP gain bonus**.

**Damage type:** None — Concordance Pulse heals and applies knockback without an attack damage call.

Shared racial action: **Concordance Pulse** restores **50% of maximum HP and 50% of maximum SHP**, with no fixed healing-amount cap. Neither pool exceeds its current maximum. It also pushes nearby enemies. It costs 1,500 aura, has a 30-second cooldown and renders the pair's ancestral colors. The 105 races share this action; they do not each add a separate combat kit.

Native final-stage attributes depend on Tensura’s server race configuration. These bonuses replace the former primary and secondary race modifiers. They are not stacked on top indefinitely. Appearance retains the player's skin; these are not custom monster models.

## Race catalog

Weights multiply the same baseline used by native aura/magicule gain attributes. Every unboosted profile totals six, twice the default single-race growth budget of three. The inherited 10% bonus multiplies both weights by 1.1 without changing their ratio. This does not bypass native EP caps. The individual pages show an example at a baseline of three, including that bonus.

| Ancestral pair | Convergent race | Aura weight | Magicule weight |
| --- | --- | ---: | ---: |
| Beastfolk + Harpy | [Gryphon Sovereign](Fusion-beastfolk-harpy.md) | 3.74 | 2.26 |
| Beastfolk + Ogre | [Rakshasa](Fusion-beastfolk-ogre.md) | 3.745 | 2.255 |
| Beastfolk + Goblin | [Briar Monarch](Fusion-beastfolk-goblin.md) | 3.75 | 2.25 |
| Beastfolk + Giant | [Fenrir Colossus](Fusion-beastfolk-giant.md) | 3.755 | 2.245 |
| Beastfolk + Orc | [Calydonian King](Fusion-beastfolk-orc.md) | 3.76 | 2.24 |
| Beastfolk + Lizardman | [Chimera Wyrm](Fusion-beastfolk-lizardman.md) | 3.765 | 2.235 |
| Beastfolk + Merfolk | [Leviathan Beast](Fusion-beastfolk-merfolk.md) | 3.77 | 2.23 |
| Beastfolk + Elf | [Sylvan Sphinx](Fusion-beastfolk-elf.md) | 3.775 | 2.225 |
| Beastfolk + Human | [Primal Paragon](Fusion-beastfolk-human.md) | 3.78 | 2.22 |
| Beastfolk + Dwarf | [Adamant Manticore](Fusion-beastfolk-dwarf.md) | 3.785 | 2.215 |
| Beastfolk + Slime | [Amorphous Behemoth](Fusion-beastfolk-slime.md) | 2.79 | 3.21 |
| Beastfolk + Vampire | [Bloodmoon Lycan](Fusion-beastfolk-vampire.md) | 2.795 | 3.205 |
| Beastfolk + Wight | [Ossuary Fenrir](Fusion-beastfolk-wight.md) | 2.8 | 3.2 |
| Beastfolk + Daemon | [Abyssal Chimera](Fusion-beastfolk-daemon.md) | 2.805 | 3.195 |
| Harpy + Ogre | [Garuda Oni](Fusion-harpy-ogre.md) | 3.81 | 2.19 |
| Harpy + Goblin | [Thornwing Archon](Fusion-goblin-harpy.md) | 3.815 | 2.185 |
| Harpy + Giant | [Storm Titan](Fusion-giant-harpy.md) | 3.82 | 2.18 |
| Harpy + Orc | [Tempest Juggernaut](Fusion-harpy-orc.md) | 3.825 | 2.175 |
| Harpy + Lizardman | [Quetzal Sovereign](Fusion-harpy-lizardman.md) | 3.83 | 2.17 |
| Harpy + Merfolk | [Siren Seraph](Fusion-harpy-merfolk.md) | 3.835 | 2.165 |
| Harpy + Elf | [Zephyr Oracle](Fusion-elf-harpy.md) | 3.84 | 2.16 |
| Harpy + Human | [Celestial Valkyrie](Fusion-harpy-human.md) | 3.845 | 2.155 |
| Harpy + Dwarf | [Runewing Artisan](Fusion-dwarf-harpy.md) | 3.85 | 2.15 |
| Harpy + Slime | [Cloudborn Protean](Fusion-harpy-slime.md) | 2.855 | 3.145 |
| Harpy + Vampire | [Nocturne Strix](Fusion-harpy-vampire.md) | 2.86 | 3.14 |
| Harpy + Wight | [Pale Thunderbird](Fusion-harpy-wight.md) | 2.865 | 3.135 |
| Harpy + Daemon | [Fallen Simurgh](Fusion-daemon-harpy.md) | 2.87 | 3.13 |
| Ogre + Goblin | [Thorncrown Asura](Fusion-goblin-ogre.md) | 3.875 | 2.125 |
| Ogre + Giant | [Indra Colossus](Fusion-giant-ogre.md) | 3.88 | 2.12 |
| Ogre + Orc | [Warborn Ravager](Fusion-ogre-orc.md) | 3.885 | 2.115 |
| Ogre + Lizardman | [Ryuujin](Fusion-lizardman-ogre.md) | 3.89 | 2.11 |
| Ogre + Merfolk | [Tidal Susanoo](Fusion-merfolk-ogre.md) | 3.895 | 2.105 |
| Ogre + Elf | [Moonlit Kensei](Fusion-elf-ogre.md) | 3.9 | 2.1 |
| Ogre + Human | [Heavenly Asura](Fusion-human-ogre.md) | 3.905 | 2.095 |
| Ogre + Dwarf | [Vajra Forgemaster](Fusion-dwarf-ogre.md) | 3.91 | 2.09 |
| Ogre + Slime | [Protean Kishin](Fusion-ogre-slime.md) | 2.915 | 3.085 |
| Ogre + Vampire | [Crimson Yaksha](Fusion-ogre-vampire.md) | 2.92 | 3.08 |
| Ogre + Wight | [Gravebound Kishin](Fusion-ogre-wight.md) | 2.925 | 3.075 |
| Ogre + Daemon | [Abyssal Rakshasa](Fusion-daemon-ogre.md) | 2.93 | 3.07 |
| Goblin + Giant | [Bramble Titan](Fusion-giant-goblin.md) | 3.935 | 2.065 |
| Goblin + Orc | [Verdant Warlord](Fusion-goblin-orc.md) | 3.94 | 2.06 |
| Goblin + Lizardman | [Basilisk Regent](Fusion-goblin-lizardman.md) | 3.945 | 2.055 |
| Goblin + Merfolk | [Reef Trickster](Fusion-goblin-merfolk.md) | 3.95 | 2.05 |
| Goblin + Elf | [Thornsong Sovereign](Fusion-elf-goblin.md) | 3.955 | 2.045 |
| Goblin + Human | [Emerald Exemplar](Fusion-goblin-human.md) | 3.96 | 2.04 |
| Goblin + Dwarf | [Gremlin Artificer](Fusion-dwarf-goblin.md) | 3.965 | 2.035 |
| Goblin + Slime | [Briar Protean](Fusion-goblin-slime.md) | 2.97 | 3.03 |
| Goblin + Vampire | [Sanguine Puck](Fusion-goblin-vampire.md) | 2.975 | 3.025 |
| Goblin + Wight | [Ossuary Trickster](Fusion-goblin-wight.md) | 2.98 | 3.02 |
| Goblin + Daemon | [Infernal Goblin King](Fusion-daemon-goblin.md) | 2.985 | 3.015 |
| Giant + Orc | [Worldbreaker](Fusion-giant-orc.md) | 3.99 | 2.01 |
| Giant + Lizardman | [Jormungandr](Fusion-giant-lizardman.md) | 3.995 | 2.005 |
| Giant + Merfolk | [Oceanus](Fusion-giant-merfolk.md) | 4 | 2 |
| Giant + Elf | [Yggdrasil Guardian](Fusion-elf-giant.md) | 4.005 | 1.995 |
| Giant + Human | [Atlas Ascendant](Fusion-giant-human.md) | 4.01 | 1.99 |
| Giant + Dwarf | [Hecatoncheir](Fusion-dwarf-giant.md) | 4.015 | 1.985 |
| Giant + Slime | [Primordial Colossus](Fusion-giant-slime.md) | 3.02 | 2.98 |
| Giant + Vampire | [Bloodforge Titan](Fusion-giant-vampire.md) | 3.025 | 2.975 |
| Giant + Wight | [Ossuary Colossus](Fusion-giant-wight.md) | 3.03 | 2.97 |
| Giant + Daemon | [Typhon](Fusion-daemon-giant.md) | 3.035 | 2.965 |
| Orc + Lizardman | [Ironhide Wyrm](Fusion-lizardman-orc.md) | 4.04 | 1.96 |
| Orc + Merfolk | [Abyssal Cetus](Fusion-merfolk-orc.md) | 4.045 | 1.955 |
| Orc + Elf | [Verdant Juggernaut](Fusion-elf-orc.md) | 4.05 | 1.95 |
| Orc + Human | [Adamant Warlord](Fusion-human-orc.md) | 4.055 | 1.945 |
| Orc + Dwarf | [Vulcan Behemoth](Fusion-dwarf-orc.md) | 4.06 | 1.94 |
| Orc + Slime | [Voracious Protean](Fusion-orc-slime.md) | 3.065 | 2.935 |
| Orc + Vampire | [Sanguine Behemoth](Fusion-orc-vampire.md) | 3.07 | 2.93 |
| Orc + Wight | [Gravehide Colossus](Fusion-orc-wight.md) | 3.075 | 2.925 |
| Orc + Daemon | [Maw of Tartarus](Fusion-daemon-orc.md) | 3.08 | 2.92 |
| Lizardman + Merfolk | [Azure Leviathan](Fusion-lizardman-merfolk.md) | 4.085 | 1.915 |
| Lizardman + Elf | [Elder Starwyrm](Fusion-elf-lizardman.md) | 4.09 | 1.91 |
| Lizardman + Human | [Dragonheart Exemplar](Fusion-human-lizardman.md) | 4.095 | 1.905 |
| Lizardman + Dwarf | [Runebound Fafnir](Fusion-dwarf-lizardman.md) | 4.1 | 1.9 |
| Lizardman + Slime | [Mercurial Ouroboros](Fusion-lizardman-slime.md) | 3.105 | 2.895 |
| Lizardman + Vampire | [Sanguine Wyrm](Fusion-lizardman-vampire.md) | 3.11 | 2.89 |
| Lizardman + Wight | [Ossuary Dragon](Fusion-lizardman-wight.md) | 3.115 | 2.885 |
| Lizardman + Daemon | [Tiamat Scion](Fusion-daemon-lizardman.md) | 3.12 | 2.88 |
| Merfolk + Elf | [Nereid Oracle](Fusion-elf-merfolk.md) | 4.125 | 1.875 |
| Merfolk + Human | [Thalassic Exemplar](Fusion-human-merfolk.md) | 4.13 | 1.87 |
| Merfolk + Dwarf | [Runic Triton](Fusion-dwarf-merfolk.md) | 4.135 | 1.865 |
| Merfolk + Slime | [Abyssal Undine](Fusion-merfolk-slime.md) | 3.14 | 2.86 |
| Merfolk + Vampire | [Sanguine Siren](Fusion-merfolk-vampire.md) | 3.145 | 2.855 |
| Merfolk + Wight | [Ossuary Mariner](Fusion-merfolk-wight.md) | 3.15 | 2.85 |
| Merfolk + Daemon | [Abyssal Scylla](Fusion-daemon-merfolk.md) | 3.155 | 2.845 |
| Elf + Human | [Astral Exemplar](Fusion-elf-human.md) | 4.16 | 1.84 |
| Elf + Dwarf | [Aetherforged Oracle](Fusion-dwarf-elf.md) | 4.165 | 1.835 |
| Elf + Slime | [Sylvan Protean](Fusion-elf-slime.md) | 3.17 | 2.83 |
| Elf + Vampire | [Crimson Sidhe](Fusion-elf-vampire.md) | 3.175 | 2.825 |
| Elf + Wight | [Pale Star Oracle](Fusion-elf-wight.md) | 3.18 | 2.82 |
| Elf + Daemon | [Twilight Seraph](Fusion-daemon-elf.md) | 3.185 | 2.815 |
| Human + Dwarf | [Runic Exemplar](Fusion-dwarf-human.md) | 4.19 | 1.81 |
| Human + Slime | [Aeonic Protean](Fusion-human-slime.md) | 3.195 | 2.805 |
| Human + Vampire | [Eclipse Sovereign](Fusion-human-vampire.md) | 3.2 | 2.8 |
| Human + Wight | [Deathless Exemplar](Fusion-human-wight.md) | 3.205 | 2.795 |
| Human + Daemon | [Empyrean Adversary](Fusion-daemon-human.md) | 3.21 | 2.79 |
| Dwarf + Slime | [Quicksilver Artificer](Fusion-dwarf-slime.md) | 3.215 | 2.785 |
| Dwarf + Vampire | [Bloodrune Patriarch](Fusion-dwarf-vampire.md) | 3.22 | 2.78 |
| Dwarf + Wight | [Ossuary Runesmith](Fusion-dwarf-wight.md) | 3.225 | 2.775 |
| Dwarf + Daemon | [Infernal Forgemaster](Fusion-daemon-dwarf.md) | 3.23 | 2.77 |
| Slime + Vampire | [Sanguine Protean](Fusion-slime-vampire.md) | 2.235 | 3.765 |
| Slime + Wight | [Ossuary Protean](Fusion-slime-wight.md) | 2.24 | 3.76 |
| Slime + Daemon | [Abyssal Progenitor](Fusion-daemon-slime.md) | 2.245 | 3.755 |
| Vampire + Wight | [Nocturne Revenant](Fusion-vampire-wight.md) | 2.25 | 3.75 |
| Vampire + Daemon | [Sanguine Archfiend](Fusion-daemon-vampire.md) | 2.255 | 3.745 |
| Wight + Daemon | [Erebus Sovereign](Fusion-daemon-wight.md) | 2.26 | 3.74 |

## Gallery

![Completed Human/Slime convergence with its own growth profile](../media/convergent-awakening.png)

## Configuration

The world’s `serverconfig/tensura-myths-server.toml` controls `secondaryRaceEP` (5M), `secondaryEvolutionEPStep` (2.5M new EP per stage), `convergenceEP` (10M), `godlyEP` (12M) and `convergenceEnergyMultiplier` (1.25). Server requirements are checked again when a request arrives. Client button state is not authoritative.

Ordinary death and relogging retain the fused race and ancestry data. Reincarnation or an external replacement of the primary race resets the previous Duo life. There is no normal menu action to swap an existing secondary or repeatedly claim the awakening bonus.

---

**Categories:** Tensura Myths · Races
