#!/usr/bin/env python3
"""Render the gameplay ability directory from the same records used by offline search."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
WIKI=ROOT/'wiki'
records=json.loads((WIKI/'abilities.json').read_text())
titles={item['slug']:item['title'] for item in json.loads((WIKI/'catalog.json').read_text())}
lines=['# Ability index','','[Main Page](../README.md) · [Skills](Skills.md) · [Damage types](Damage-Types.md)','','**Ability index** lists active arts, toggleable passives, classification passives and shared racial actions. Each entry links to its exact resource costs, targeting and damage-type information. Reflected projectiles retain their original attack type.','']
for tier,label in [('Unique','Unique abilities'),('Ultimate','Ultimate abilities'),('Godly','Godly abilities'),('Extra','Extra abilities'),('Racial','Racial abilities')]:
    entries=[a for a in records if a['tier']==tier]
    if not entries:continue
    lines+=['## '+label,'']
    if tier=='Godly':lines+=['The three Godly classification passives are always active, free and non-stacking. Individual skill toggles retain their own upkeep.','']
    lines+=['| Ability | Parent skill / system | Activation | Damage type |','| --- | --- | --- | --- |']
    for a in entries:
        slug=a['skill'];damage=a['damage']
        if slug=='Lugh' and a['name'] in ('Reconstructed Art','Grand Synthesis'):damage+=' — see the linked archive/source reference'
        else:
            ids=[v for v in [a.get('damage_id'),a.get('secondary_damage_id')] if v]
            if ids:damage+=' — '+', '.join('`'+v+'`' for v in dict.fromkeys(ids))
        activation='Always active' if slug=='Godly' else a['kind']
        lines.append(f'| [{a["name"]}]({slug}.md#{a["anchor"]}) | [{titles[slug]}]({slug}.md) | {activation} | {damage} |')
    lines+=['']
lines+=['---','','**Categories:** Tensura Myths · Skills','']
(WIKI/'pages/Abilities.md').write_text('\n'.join(lines))
print(f'Indexed {len(records)} abilities.')
