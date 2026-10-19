# Commands

[Main Page](../README.md) · [Skills](Skills.md) · [Evolution](Evolution.md) · [Mechanics](Mechanics.md) · [Commands](Commands.md)

All commands affect the issuing player. No operator permission is required, and progression gates are still enforced.

| Command | Function |
| --- | --- |
| `/wildhunt` or `/wildhunt guide` | Show the brief gameplay guide |
| `/wildhunt status` | Show race stage, base EP, distinct boss IDs, mastery and learned-art resources |
| `/wildhunt evolve` | Advance one hunter stage if eligible |
| `/wildhunt ascend rey_dau` | Evolve mastered Storm Crown into Raijin |
| `/wildhunt ascend uth_duna` | Evolve mastered Scarlet Deluge into Varuna |
| `/wildhunt ascend nu_udra` | Evolve mastered Black Flame into Agni |
| `/wildhunt ascend jin_dahaad` | Evolve mastered Frostbound Colossus into Ymir |

## Passive status

Each learned art reports its passive name, ON/OFF state and aura upkeep per second. Use the native skill toggle in Tensura’s Ability Selection to change that state. Status does not grant resources or change a toggle.

## Ascension requirements

Be Sovereign Hunter, retain at least two million base EP, own the matching Unique, and master it. The new Ultimate is learned before its source Unique is removed. Failed eligibility checks do not consume your source skill. There is no trial requirement.

---

**Categories:** Tensura Myths

## Myths progression

| Command | Purpose |
| --- | --- |
| `/myths` | Open the Duo Race / Godly progression menu |
| `/myths status` | Show primary and secondary status and open the menu |
| `/myths secondary <family>` | Choose a permanent secondary ancestry after meeting all gates |
| `/myths evolve_secondary <race_id>` | Evolve the secondary through native requirements |
| `/myths awaken` | Perform Convergent Awakening after both races reach final evolution |
| `/myths godly <ultimate_id>` | Ascend the selected mastered Myths Ultimate into Aion |

These commands are available to players and enforce the same server-side requirements as the menu. Valid secondary keys: `beastfolk`, `harpy`, `ogre`, `goblin`, `giant`, `orc`, `lizardman`, `merfolk`, `elf`, `human`, `dwarf`, `slime`, `vampire`, `wight`, `daemon`.

Aion is `wildhunt:aion`. Fused race IDs follow `wildhunt:convergence/<sorted_family_pair>`; the [catalog](Convergent-Awakening.md#race-catalog) links to each exact ID.
