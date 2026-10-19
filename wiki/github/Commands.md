# Commands

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Reference](https://github.com/Bithio999/MC/wiki/Mechanics) · [Ability index](https://github.com/Bithio999/MC/wiki/Abilities) · [Damage types](https://github.com/Bithio999/MC/wiki/Damage-Types)

All commands affect the issuing player. No operator permission is required, and progression gates are still enforced.

| Command | Function |
| --- | --- |
| `/wildhunt` or `/wildhunt guide` | Show the brief gameplay guide |
| `/wildhunt status` | Show race stage, base EP, distinct boss IDs, mastery and learned-art resources |
| `/wildhunt evolve` | Advance one hunter stage if eligible |
| `/wildhunt ascend vritra` | Combine mastered Storm Crown + Black Flame |
| `/wildhunt ascend demeter` | Combine mastered Verdant Keeper + Frostbound Colossus |
| `/wildhunt ascend janus` | Combine mastered Threadweaver + Mirrorbound |

## Passive status

Each learned art reports its passive name, ON/OFF state and aura upkeep per second. Use the native skill toggle in Tensura’s Ability Selection to change that state. Status does not grant resources or change a toggle.

## Synthesis requirements

Any race can synthesize at **2M base EP** with both specific ingredients mastered. The result is learned before both sources are removed. Invalid requests consume nothing. The same server rules power **Skills → Ultimate → Synthesis**. See [Skill synthesis](https://github.com/Bithio999/MC/wiki/Skill-Synthesis).

## Myths progression

| Command | Purpose |
| --- | --- |
| `/myths` | Open the native secondary evolution page |
| `/myths status` | Show primary and secondary status and open native evolution |
| `/myths secondary <family>` | Choose a permanent secondary ancestry after meeting all gates |
| `/myths evolve_secondary <race_id>` | Evolve the secondary through native requirements |
| `/myths awaken` | Perform Convergent Awakening after both races reach final evolution |
| `/myths synthesize <vritra/demeter/janus>` | Combine the required mastered Unique pair |
| `/myths godly <ultimate_id> [aion/thoth]` | Ascend a mastered combination Ultimate; defaults to Aion. Thoth also combines mastered Rune Smith |

These commands are available to players and enforce the same server-side requirements as the menu. Valid secondary keys: `beastfolk`, `harpy`, `ogre`, `goblin`, `giant`, `orc`, `lizardman`, `merfolk`, `elf`, `human`, `dwarf`, `slime`, `vampire`, `wight`, `daemon`.

Godly IDs are `wildhunt:aion` and `wildhunt:thoth`. Fused race IDs follow `wildhunt:convergence/<sorted_family_pair>`; the [catalog](https://github.com/Bithio999/MC/wiki/Convergent-Awakening#race-catalog) links to each exact ID.

---

**Categories:** Tensura Myths · Reference
