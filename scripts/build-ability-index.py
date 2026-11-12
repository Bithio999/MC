#!/usr/bin/env python3
"""Build each addon's short ability directory from scoped ability records."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
WIKI=ROOT/'wiki'
records=json.loads((WIKI/'abilities.json').read_text())
metadata={item['slug']:item for item in json.loads((WIKI/'catalog.json').read_text())}
for mod,slug,home,label in [('myths','Abilities','Tensura-Myths','Tensura Myths'),('additions','Additions-Abilities','TensuraAdditionalAdditions','TensuraAdditionalAdditions')]:
    lines=['# Ability directory','',f'[All wikis](../README.md) · [{label}]({home}.md)','','Find a move below, then open its entry for targeting, damage, cost and cooldown. “None” means no direct attack damage.','']
    if mod=='myths':lines[2:2]=['> **Discontinued archive:** Tensura Myths ended at 0.36.1. No further updates are planned.','']
    for tier in ['Unique','Ultimate','Godly','Extra','Battlewill','Racial','Boss']:
        entries=[a for a in records if a.get('mod','myths')==mod and a['tier']==tier]
        if not entries:continue
        lines+=['## '+tier,'','| Ability | Skill / system | Activation | Damage type |','| --- | --- | --- | --- |']
        for a in entries:
            parent=a['skill'];lines.append(f'| [{a["name"]}]({parent}.md#{a["anchor"]}) | [{metadata[parent]["title"]}]({parent}.md) | {a["kind"]} | {a["damage"]} |')
        lines+=['']
    (WIKI/'pages'/(slug+'.md')).write_text('\n'.join(lines))
print(f'Indexed {len(records)} abilities across the addon sections.')
