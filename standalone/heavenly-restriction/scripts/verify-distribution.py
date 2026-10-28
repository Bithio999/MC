#!/usr/bin/env python3
"""Audit the independent addon package and local assets without a running client."""
from pathlib import Path
import json, re, struct, tomllib, zipfile

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
jar = root / 'build/libs/tensura-heavenly-restriction-1.1.4.jar'
assets = resources / 'assets/heavenlyrestriction'
required_art = [*(f'textures/race/{name}.png' for name in (
    'fettered_human', 'tempered_vessel', 'unshackled', 'heavenly_restricted', 'heavenbreaker')),
    'textures/skill/chained.png', 'textures/battlewill/air_sovereignty.png', 'textures/skill/unbound.png',
    'geo/vital_corolla.geo.json', 'animations/vital_corolla.animation.json',
    'textures/geo/grove.png', 'textures/geo/grove_glowmask.png']
for name in required_art:
    assert (assets / name).is_file(), name
for name in required_art[:8]:
    data = (assets / name).read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == (64, 64), name
assert (resources / 'data/heavenlyrestriction/tags/manascore_skill/skills/fettered_skill_taking.json').is_file()
for path in resources.rglob('*.json'):
    json.loads(path.read_text())
animations = json.loads((assets / 'animations/vital_corolla.animation.json').read_text())['animations']
assert {'animation.restriction.appear', 'animation.restriction.idle'} <= set(animations)
for path in (root / 'src/main/java').rglob('*.java'):
    assert 'dev.wildhunt' not in path.read_text(), path
config = json.loads((resources / 'heavenlyrestriction.mixins.json').read_text())
for name in config['mixins'] + config.get('client', []):
    assert (root / 'src/main/java/dev/heavenlyrestriction/mixin' / (name.replace('.', '/') + '.java')).is_file(), name
with zipfile.ZipFile(jar) as z:
    names = set(z.namelist())
    mods = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
    assert [(m['modId'], m['version']) for m in mods['mods']] == [('heavenlyrestriction', '1.1.4')]
    assert any(d['modId'] == 'tensura' and d['type'] == 'required' for d in mods['dependencies']['heavenlyrestriction'])
    assert any(d['modId'] == 'wildhunt' and d['type'] == 'incompatible' for d in mods['dependencies']['heavenlyrestriction'])
    assert not any(n.startswith('dev/wildhunt/') or 'StandaloneReview' in n or 'ChainedTargetingReview' in n or 'ChainedResourceReview' in n or 'UnboundReview' in n or 'AirborneReview' in n for n in names)
    for name in required_art:
        assert z.read('assets/heavenlyrestriction/' + name) == (assets / name).read_bytes(), name
    assert z.read('data/heavenlyrestriction/tags/manascore_skill/skills/fettered_skill_taking.json') == (resources / 'data/heavenlyrestriction/tags/manascore_skill/skills/fettered_skill_taking.json').read_bytes()
    assert z.read('heavenlyrestriction.mixins.json') == (resources / 'heavenlyrestriction.mixins.json').read_bytes()
    for name in config['mixins'] + config.get('client', []):
        assert 'dev/heavenlyrestriction/mixin/' + name.replace('.', '/') + '.class' in names, name
    for name in ['ChainedReincarnation', 'ChainedReincarnationConfig', 'ChainedDash', 'ChainedSkill', 'ChainedState', 'ChainedResources', 'ChainedEvolution', 'UnboundSkill', 'UnboundCritical', 'UnboundCounter', 'RestrictedRace', 'AuraRegenerationSkill', 'AirSovereigntyArt']:
        assert 'dev/heavenlyrestriction/' + name + '.class' in names
    assert not any(n.endswith('.jar') for n in names), 'Dependency jars must not be redistributed inside the addon'
print('Independent 1.1.4 JAR, native dependency metadata, all extracted icons/rigs/animations and mixin classes verified; no Myths classes, bundled dependencies or review fixtures.')
