#!/usr/bin/env python3
"""Validate the complete editor -> exported runtime asset graph, not just JSON syntax."""
from pathlib import Path
from PIL import Image
import json,re
ROOT=Path(__file__).resolve().parents[1];assets=ROOT/'src/main/resources/assets/wildhunt';source=ROOT/'art/blockbench'
manifest=json.loads((source/'manifest.json').read_text());effects=manifest['effects']
protocol=(ROOT/'src/main/java/dev/wildhunt/ArtVisualEntity.java').read_text()
ids={int(n) for name,n in re.findall(r'([A-Z][A-Z_]+)\s*=\s*(\d+)',protocol)}
assert {e['kind'] for e in effects}==ids and len(effects)==len(ids)==62
routes=(ROOT/'src/main/java/dev/wildhunt/visual/VisualRig.java').read_text()
for e in effects:assert f'new VisualRig({e["kind"]},"{e["asset"]}","{e["theme"]}"' in routes,e
count=cubes=clips=0
for project in sorted(source.glob('*.bbmodel')):
 p=json.loads(project.read_text());name=project.stem
 geo=json.loads((assets/'geo/myths'/f'{name}.geo.json').read_text())['minecraft:geometry'][0]
 anim=json.loads((assets/'animations/myths'/f'{name}.animation.json').read_text())['animations']
 bones={b['name']:b for b in geo['bones']};assert len(bones)==len(geo['bones'])
 if name=='ananke':assert {'head','body','phase_2','phase_3',*[f'wing_{n}' for n in range(6)]}<=set(bones)
 if name=='oil_lash':assert {f'tendon_{n}' for n in range(10)}<=set(bones)
 if name=='reconstructed_crown':assert {f'blade_{n}' for n in range(7)}<=set(bones)
 for b in bones.values():
  seen=set();current=b
  while 'parent' in current:
   assert current['name'] not in seen,(name,b);seen.add(current['name']);current=bones[current['parent']]
  for c in b.get('cubes',[]):
   assert all(v>0 for v in c['size']),(name,c)
   for face in c['uv'].values():
    assert len(face['uv'])==len(face['uv_size'])==2
    assert all(0<=face['uv'][i]+face['uv_size'][i]<=geo['description'][['texture_width','texture_height'][i]] for i in range(2)),(name,face)
 for a in anim.values():
  assert a.get('animation_length',0)>0
  assert set(a.get('bones',{}))<=set(bones),(name,set(a.get('bones',{}))-set(bones))
 assert 'animation.myths.idle' in anim and 'animation.myths.appear' in anim
 assert len(p['elements'])==sum(len(b.get('cubes',[])) for b in bones.values()),name
 assert len({e['uuid'] for e in p['elements']})==len(p['elements'])
 tex=assets/'textures/geo'/p['textures'][0]['name'];image=Image.open(tex)
 assert image.size==(geo['description']['texture_width'],geo['description']['texture_height'])
 glow=Image.open(tex.with_name(tex.stem+'_glowmask.png'));assert glow.size==image.size and glow.getbbox()
 count+=1;cubes+=len(p['elements']);clips+=len(anim)
assert count==67,count
print(f'{len(ids)} protocol effects covered; {count} Blockbench/export pairs, {cubes} cubes and {clips} clips validated.')
for project in sorted((source/'items').glob('*.bbmodel')):
 p=json.loads(project.read_text());m=json.loads((assets/'models/item'/f'{project.stem}.json').read_text())
 assert m['credit']=='Made with Blockbench' and len(m['elements'])==len(p['elements'])
 assert m.get('display')
 for texture in m['textures'].values():
  assert texture.startswith(('wildhunt:item/','wildhunt:block/')), 'Item textures must participate in the block/item atlas'
  assert (assets/'textures'/(texture.split(':',1)[1]+'.png')).exists(),texture
 print(project.stem,': editor-exported Java item model verified')
