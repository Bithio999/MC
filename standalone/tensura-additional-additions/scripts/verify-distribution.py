#!/usr/bin/env python3
"""Audit the independent addon package and local assets without a running client."""
from pathlib import Path
import json, re, struct, tomllib, zipfile

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
jar = root / 'build/libs/TensuraAdditionalAdditions-1.10.0.jar'
assets = resources / 'assets/heavenlyrestriction'
required_art = [*(f'textures/race/{name}.png' for name in (
    'fettered_human', 'tempered_vessel', 'unshackled', 'heavenly_restricted', 'heavenbreaker')),
    'textures/battlewill/schema.png', 'textures/skill/chained.png', 'textures/battlewill/air_sovereignty.png', 'textures/skill/unbound.png', 'textures/skill/fusion.png', 'textures/skill/prospector.png', 'textures/skill/exponentiation.png', 'textures/skill/loan_shark.png',
    'geo/vital_corolla.geo.json', 'animations/vital_corolla.animation.json',
    'textures/geo/grove.png', 'textures/geo/grove_glowmask.png',
    'textures/item/inverted_spear_of_heaven.png', 'textures/mob_effect/nullification.png']
for name in required_art:
    assert (assets / name).is_file(), name
for name in required_art[:8]:
    data = (assets / name).read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == (64, 64), name
prospector_data=(assets/'textures/skill/prospector.png').read_bytes()
assert struct.unpack('>II',prospector_data[16:24])==(64,64)
spear_data=(assets/'textures/item/inverted_spear_of_heaven.png').read_bytes()
assert struct.unpack('>II',spear_data[16:24])==(64,64), 'Corrected spear sprite must be 64 pixels square'
parent_spear=root.parents[1]/'src/main/resources/assets/wildhunt/textures/item/inverted_spear_of_heaven.png'
if parent_spear.is_file():assert spear_data==parent_spear.read_bytes(), 'Spear art differs between distributions'
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
    assert [(m['modId'], m['version']) for m in mods['mods']] == [('heavenlyrestriction', '1.10.0')]
    assert mods['mods'][0]['displayName'] == 'TensuraAdditionalAdditions'
    language=json.loads(z.read('assets/heavenlyrestriction/lang/en_us.json'))
    for key,label in [('heavenlyrestriction.skill.parallel_world_jumper','Parallel World Jumper'),('heavenlyrestriction.skill.fusion','Fusion'),('heavenlyrestriction.skill.prospector','Prospector'),('heavenlyrestriction.skill.mode.prospector.refine','Refine'),('heavenlyrestriction.skill.mode.fusion.fuse','Fuse'),('heavenlyrestriction.skill.mode.parallel_world_jumper.world_hop','World Jump'),('heavenlyrestriction.skill.mode.parallel_world_jumper.world_select','World Select')]:
        assert language[key]==label, key
    assert any(d['modId'] == 'tensura' and d['type'] == 'required' for d in mods['dependencies']['heavenlyrestriction'])
    assert any(d['modId'] == 'wildhunt' and d['type'] == 'incompatible' for d in mods['dependencies']['heavenlyrestriction'])
    assert not any(n.startswith('dev/wildhunt/') or 'StandaloneReview' in n or 'ChainedTargetingReview' in n or 'ChainedResourceReview' in n or 'UnboundReview' in n or 'AirborneReview' in n or 'NeoOtherworldClientReview' in n or 'NeoOtherworldGameTests' in n or 'NeoOtherworldFixtures' in n or 'SpiritualReviewGameTests' in n or 'TransmissionReview' in n or 'TransmissionClientReview' in n or 'SchemaReview' in n or 'SchemaClientReview' in n or 'GuideClientReview' in n or 'SchemaProgressClientReview' in n or 'WorldHopClientReview' in n or 'WorldHopPreview' in n or 'WorldSelectClientReview' in n or 'WorldLibraryClientReview' in n or 'FusionPreview' in n or 'FusionConsentReview' in n or 'FusionMenuReview' in n or 'FusionControlReview' in n or 'ProspectorReview' in n or 'ExponentiationReview' in n or 'LoanReview' in n or 'HighRealmReview' in n for n in names)
    assert b'ftbteams' not in z.read('dev/heavenlyrestriction/TestFixtures.class'), 'Pack-only fixture leaked into release'
    for name in required_art:
        assert z.read('assets/heavenlyrestriction/' + name) == (assets / name).read_bytes(), name
    assert z.read('data/heavenlyrestriction/tags/manascore_skill/skills/fettered_skill_taking.json') == (resources / 'data/heavenlyrestriction/tags/manascore_skill/skills/fettered_skill_taking.json').read_bytes()
    for name in ['data/heavenlyrestriction/enchantment/nullification.json', 'data/heavenlyrestriction/gear_existence/inverted_spear_of_heaven.json', 'data/heavenlyrestriction/loot_modifiers/ominous_heavenly_spear.json', 'data/neoforge/loot_modifiers/global_loot_modifiers.json', 'data/tensura/tags/enchantment/engraving.json', 'assets/heavenlyrestriction/models/item/inverted_spear_of_heaven.json']:
        assert z.read(name) == (resources / name).read_bytes(), name
    assert z.read('heavenlyrestriction.mixins.json') == (resources / 'heavenlyrestriction.mixins.json').read_bytes()
    for name in config['mixins'] + config.get('client', []):
        assert 'dev/heavenlyrestriction/mixin/' + name.replace('.', '/') + '.class' in names, name
    for name in ['ChainedReincarnation', 'ChainedReincarnationConfig', 'ChainedDash', 'ChainedSkill', 'ChainedState', 'ChainedResources', 'ChainedEvolution', 'UnboundSkill', 'UnboundCritical', 'UnboundCounter', 'RestrictedRace', 'AuraRegenerationSkill', 'AirSovereigntyArt', 'InstantTransmissionArt', 'ParallelWorldJumperSkill', 'FusionSkill', 'LoanSharkSkill', 'ExponentiationSkill', 'ProspectorSkill', 'ProspectorMining', 'ProspectorOres', 'SchemaArt', 'SchemaNetwork', 'HeavenlyArsenal', 'InvertedSpearItem', 'Nullification', 'OminousSpearLoot', 'ChainedWeaponArts', 'UnerringBody']:
        assert 'dev/heavenlyrestriction/' + name + '.class' in names
    assert 'data/heavenlyrestriction_compat/structure/empty.nbt' not in names
    assert 'data/minecraft/worldgen/world_preset/flat.json' not in names
    assert 'dev/heavenlyrestriction/HighRealmGameTests.class' not in names
    battlewill = 'data/tensura/tags/manascore_skill/skills/battlewill.json'
    assert z.read(battlewill) == (resources / battlewill).read_bytes()
    assert 'heavenlyrestriction:instant_transmission' in json.loads(z.read(battlewill))['values']
    assert 'heavenlyrestriction:schema' in json.loads(z.read(battlewill))['values']
    assert 'dev/heavenlyrestriction/client/SchemaScreen.class' in names
    for cls in ['guide/GuideCatalog', 'client/guide/GuideScreen', 'client/guide/GuideClient']:
        assert 'dev/heavenlyrestriction/' + cls + '.class' in names
    guide = json.loads(z.read('assets/heavenlyrestriction/guide/entries.json'))['entries']
    assert len(guide) == 15 and all(not e['id'].startswith('fusion-') for e in guide)
    assert any(e['id']=='schema' for e in guide)
    assert any(e['id']=='parallel-world-jumper' for e in guide)
    unique=json.loads(z.read('data/tensura/tags/manascore_skill/skills/unique_skills.json'))
    assert 'heavenlyrestriction:parallel_world_jumper' in unique['values']
    assert 'heavenlyrestriction:fusion' in unique['values']
    assert 'heavenlyrestriction:prospector' in unique['values']
    assert 'heavenlyrestriction:exponentiation' in unique['values']
    assert 'heavenlyrestriction:loan_shark' in unique['values']
    assert any(e['id']=='loan-shark' for e in guide)
    for cls in ['loans/LoanShark','loans/LoanLedger','loans/LoanNetwork','client/LoanScreen']:assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    assert any(e['id']=='exponentiation' for e in guide)
    assert any(e['id']=='prospector' for e in guide)
    for cls in ['client/prospector/OreSurvey','client/prospector/ProspectorClient','client/prospector/ProspectorMaterial']:assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    path='data/heavenlyrestriction/loot_table/gameplay/prospector_finds.json'
    assert z.read(path)==(resources/path).read_bytes()
    for cls in ['worldhop/WorldHop','worldhop/WorldHopFiles','worldhop/WorldHopIo','worldhop/WorldHopMetadata','worldhop/WorldHopSaveCompat','worldhop/WorldHopNetwork','client/WorldHopClient','worldhop/WorldHopLibrary','worldhop/WorldHopMigration','worldhop/WorldHopJournal','worldhop/WorldSelect','client/WorldSelectScreen','fusion/Fusion','fusion/FusionNetwork','fusion/FusionReturns','client/FusionClient']:
        assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    pause = 'data/heavenlyrestriction/tags/mob_effect/pauses_energy_regeneration.json'
    assert z.read(pause) == (resources / pause).read_bytes()
    deps = mods['dependencies']['heavenlyrestriction']
    assert next(d for d in deps if d['modId']=='tensura')['versionRange'] == '[2.0.1.3,2.0.1.5)'
    assert next(d for d in deps if d['modId']=='geckolib')['versionRange'] == '[4.9.2,5)'
    assert not any(n.endswith('.jar') for n in names), 'Dependency jars must not be redistributed inside the addon'
    for cls in ['HighRealm','HighRealmConfig','GatePattern','RealmData','HighRealmElites']:
        assert 'dev/heavenlyrestriction/highrealm/'+cls+'.class' in names
    for path in (resources/'data/heavenlyrestriction').rglob('*.json'):
        if any(key in path.as_posix() for key in ['/worldgen/','/dimension/','/dimension_type/','high_realm_elites']):
            name=path.relative_to(resources).as_posix();assert z.read(name)==path.read_bytes(),name
    assert any(e['id']=='high-realm' and e['category']=='Dimensions' for e in guide)
print('Independent 1.10.0 JAR, native dependency metadata, all extracted icons/rigs/animations and mixin classes verified; no Myths classes, bundled dependencies or review fixtures.')
