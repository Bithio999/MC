#!/usr/bin/env python3
"""Audit the independent addon package and local assets without a running client."""
from pathlib import Path
import json, re, struct, tomllib, zipfile

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
jar = root / 'build/libs/TensuraAdditionalAdditions-1.23.0.jar'
assets = resources / 'assets/heavenlyrestriction'
required_art = [*(f'textures/race/{name}.png' for name in (
    'fettered_human', 'tempered_vessel', 'unshackled', 'heavenly_restricted', 'heavenbreaker')),
    'textures/battlewill/schema.png', 'textures/skill/chained.png', 'textures/battlewill/air_sovereignty.png', 'textures/skill/unbound.png', 'textures/skill/fusion.png', 'textures/skill/prospector.png', 'textures/skill/split_mind.png', 'textures/skill/marauders_map.png', 'textures/skill/exponentiation.png', 'textures/skill/loan_shark.png',
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
    assert [(m['modId'], m['version']) for m in mods['mods']] == [('heavenlyrestriction', '1.23.0')]
    assert mods['mods'][0]['displayName'] == 'TensuraAdditionalAdditions'
    language=json.loads(z.read('assets/heavenlyrestriction/lang/en_us.json'))
    for key,label in [('heavenlyrestriction.skill.parallel_world_jumper','Parallel World Jumper'),('heavenlyrestriction.skill.fusion','Fusion'),('heavenlyrestriction.skill.prospector','Prospector'),('heavenlyrestriction.skill.mode.prospector.refine','Refine'),('heavenlyrestriction.skill.mode.fusion.fuse','Fuse'),('heavenlyrestriction.skill.mode.parallel_world_jumper.world_hop','World Jump'),('heavenlyrestriction.skill.mode.parallel_world_jumper.world_select','World Select')]:
        assert language[key]==label, key
    assert any(d['modId'] == 'tensura' and d['type'] == 'required' for d in mods['dependencies']['heavenlyrestriction'])
    assert any(d['modId'] == 'wildhunt' and d['type'] == 'incompatible' for d in mods['dependencies']['heavenlyrestriction'])
    assert not any(n.startswith('dev/wildhunt/') or 'StandaloneReview' in n or 'ChainedTargetingReview' in n or 'ChainedResourceReview' in n or 'UnboundReview' in n or 'AirborneReview' in n or 'NeoOtherworldClientReview' in n or 'NeoOtherworldGameTests' in n or 'NeoOtherworldFixtures' in n or 'SpiritualReviewGameTests' in n or 'TransmissionReview' in n or 'TransmissionClientReview' in n or 'SchemaReview' in n or 'SchemaClientReview' in n or 'GuideClientReview' in n or 'SchemaProgressClientReview' in n or 'WorldHopClientReview' in n or 'WorldHopPreview' in n or 'WorldSelectClientReview' in n or 'WorldLibraryClientReview' in n or 'FusionPreview' in n or 'FusionConsentReview' in n or 'FusionMenuReview' in n or 'FusionControlReview' in n or 'ProspectorReview' in n or 'ExponentiationReview' in n or 'LoanReview' in n or 'WayfarerReview' in n or 'VesperGameTests' in n or 'CriticalMerchantGameTests' in n or 'CriticalMerchantReview' in n or 'HephaestusGameTests' in n or 'HephaestusReview' in n or 'KaelVeyrGameTests' in n or 'MapReview' in n or 'MapPerformanceReview' in n or 'MapClarityReview' in n or 'SplitMindReview' in n or 'HighRealmReview' in n for n in names)
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
    assert not any(n.startswith(('data/vesperreview/','data/merchantreview/','data/forgereview/')) for n in names)
    assert 'data/minecraft/worldgen/world_preset/flat.json' not in names
    assert 'dev/heavenlyrestriction/MetisGameTests.class' not in names
    assert 'dev/heavenlyrestriction/MetisReview.class' not in names
    assert 'data/metisreview/structure/empty.nbt' not in names
    assert 'dev/heavenlyrestriction/HighRealmGameTests.class' not in names
    assert 'dev/heavenlyrestriction/HighRealmLoadoutGameTests.class' not in names
    battlewill = 'data/tensura/tags/manascore_skill/skills/battlewill.json'
    assert z.read(battlewill) == (resources / battlewill).read_bytes()
    assert 'heavenlyrestriction:instant_transmission' in json.loads(z.read(battlewill))['values']
    assert 'heavenlyrestriction:schema' in json.loads(z.read(battlewill))['values']
    assert 'dev/heavenlyrestriction/client/SchemaScreen.class' in names
    for cls in ['guide/GuideCatalog', 'client/guide/GuideScreen', 'client/guide/GuideClient']:
        assert 'dev/heavenlyrestriction/' + cls + '.class' in names
    guide = json.loads(z.read('assets/heavenlyrestriction/guide/entries.json'))['entries']
    assert len(guide) == 22 and all(not e['id'].startswith('fusion-') for e in guide)
    assert any(e['id']=='chromatic-orb' for e in guide)
    assert any(e['id']=='vesper' for e in guide)
    assert any(e['id']=='critical-merchant' for e in guide)
    for cls in ['CriticalMerchantSkill','critical/CriticalMerchant','critical/CriticalPayout','mixin/CriticalMerchantHitMixin','mixin/CriticalMerchantSpiritualMixin','mixin/CriticalMerchantEffectMixin']:
        assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    assert any(e['id']=='hephaestus' for e in guide)
    assert language['heavenlyrestriction.skill.hephaestus']=='Hephaestus, Lord of the Forge'
    for cls in ['HephaestusSkill','forge/ForgeContent','forge/ForgeConfig','forge/ForgeItems','forge/ForgeSynthesis','forge/ForgeLedger','forge/ForgeService','forge/ForgeNetwork','forge/ForgeArsenal','forge/ArsenalBlade','forge/ForgeOffenseContext','forge/ForgeGearGrowth','forge/ForgeEngravingPolicy','mixin/ForgeResearchMixin','mixin/ForgeResearchMenuMixin','mixin/ForgeCurseMixin','MetisSkill','metis/MetisMatrix','metis/MetisIntellect','metis/MetisMagic','metis/MetisCasting','metis/MetisSynthesis','metis/MetisNetwork','client/MetisScreen','mixin/MetisCostMixin','mixin/MetisSelfTargetMixin','mixin/ForgeGearEPMixin','mixin/ForgeGearEPAccess','forge/ArsenalSequence','mixin/ForgeBenchMixin','mixin/ForgeBenchTakeMixin','mixin/ForgeQuickCraftMixin','client/ForgeScreen','client/ArsenalRenderer']:
        assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    forge_icon='assets/heavenlyrestriction/textures/skill/hephaestus.png'
    assert z.read(forge_icon)==(resources/forge_icon).read_bytes()
    assert struct.unpack('>II',z.read(forge_icon)[16:24])==(32,32)
    ultimate=json.loads(z.read('data/tensura/tags/manascore_skill/skills/ultimate_skills.json'))
    assert 'heavenlyrestriction:hephaestus' in ultimate['values']
    assert 'heavenlyrestriction:metis' in ultimate['values']
    assert any(e['id']=='metis' for e in guide)
    assert language['heavenlyrestriction.skill.metis']=='Metis, Lord of Wisdom'
    metis_icon='assets/heavenlyrestriction/textures/skill/metis.png'
    assert z.read(metis_icon)==(resources/metis_icon).read_bytes()
    assert struct.unpack('>II',z.read(metis_icon)[16:24])==(64,64)
    assert not any('heavenlyrestriction:metis' in str(e) for e in json.loads(z.read('data/tensura/tags/manascore_skill/skills/unique_skills.json'))['values'])
    assert not any('heavenlyrestriction:hephaestus' in str(e) for e in json.loads(z.read('data/tensura/tags/manascore_skill/skills/unique_skills.json'))['values'])
    critical_icon='assets/heavenlyrestriction/textures/skill/critical_merchant.png'
    assert z.read(critical_icon)==(resources/critical_icon).read_bytes()
    assert struct.unpack('>II',z.read(critical_icon)[16:24])==(64,64)
    assert language['heavenlyrestriction.skill.critical_merchant']=='Critical Merchant'
    for cls in ['client/VesperTooltip','vesper/VesperItem','vesper/Afterwind','mixin/AfterwindHitMixin','mixin/AfterwindSpiritualHitMixin']:
        assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    for path in ['assets/heavenlyrestriction/textures/item/vesper.png','assets/heavenlyrestriction/models/item/vesper.json','data/heavenlyrestriction/enchantment/afterwind.json','data/heavenlyrestriction/gear_existence/vesper.json','data/heavenlyrestriction/damage_type/afterwind.json']:
        assert z.read(path)==(resources/path).read_bytes()
    assert struct.unpack('>II',z.read('assets/heavenlyrestriction/textures/item/vesper.png')[16:24])==(64,64)
    assert 'dev/heavenlyrestriction/ChromaticGameTests.class' not in names
    assert 'dev/heavenlyrestriction/ChromaticReview.class' not in names
    assert not any('test_blessing' in n or 'test_tagged_benefit' in n for n in names)
    assert 'data/heavenlyrestriction/tags/enchantment/blessings.json' not in names
    for cls in ['ChromaticContent','ChromaticOrb']:
        assert 'dev/heavenlyrestriction/chromatic/'+cls+'.class' in names
    for item in ['chromatic_orb','bulwark_shard','sentinel_shard','skirmisher_shard','starless_core']:
        for suffix in ['models/item/'+item+'.json','textures/item/'+item+'.png']:
            path='assets/heavenlyrestriction/'+suffix
            assert z.read(path)==(resources/path).read_bytes()
        assert language['item.heavenlyrestriction.'+item]
    for path in ['data/heavenlyrestriction/recipe/chromatic_orb.json','data/heavenlyrestriction/advancement/recipes/chromatic_orb.json','data/heavenlyrestriction/tags/enchantment/chromatic_eligible.json','data/heavenlyrestriction/tags/enchantment/chromatic_excluded.json']:
        assert z.read(path)==(resources/path).read_bytes()
    assert any(e['id']=='schema' for e in guide)
    assert any(e['id']=='parallel-world-jumper' for e in guide)
    unique=json.loads(z.read('data/tensura/tags/manascore_skill/skills/unique_skills.json'))
    assert 'heavenlyrestriction:parallel_world_jumper' in unique['values']
    assert 'heavenlyrestriction:fusion' in unique['values']
    assert 'heavenlyrestriction:prospector' in unique['values']
    assert 'heavenlyrestriction:exponentiation' in unique['values']
    assert 'heavenlyrestriction:loan_shark' in unique['values']
    assert 'heavenlyrestriction:critical_merchant' in unique['values']
    assert any(e['id']=='loan-shark' for e in guide)
    for cls in ['loans/LoanShark','loans/LoanLedger','loans/LoanNetwork','client/LoanScreen']:assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    assert any(e['id']=='exponentiation' for e in guide)
    assert any(e['id']=='prospector' for e in guide)
    for cls in ['client/prospector/OreSurvey','client/prospector/ProspectorClient','client/prospector/ProspectorMaterial']:assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    for table in ['prospector_finds','prospector_finds_mastered']:
        path='data/heavenlyrestriction/loot_table/gameplay/'+table+'.json'
        assert z.read(path)==(resources/path).read_bytes()
    for cls in ['worldhop/WorldHop','worldhop/WorldHopFiles','worldhop/WorldHopIo','worldhop/WorldHopMetadata','worldhop/WorldHopSaveCompat','worldhop/WorldHopNetwork','client/WorldHopClient','worldhop/WorldHopLibrary','worldhop/WorldHopMigration','worldhop/WorldHopJournal','worldhop/WorldSelect','client/WorldSelectScreen','fusion/Fusion','fusion/FusionNetwork','fusion/FusionReturns','client/FusionClient']:
        assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    pause = 'data/heavenlyrestriction/tags/mob_effect/pauses_energy_regeneration.json'
    assert z.read(pause) == (resources / pause).read_bytes()
    deps = mods['dependencies']['heavenlyrestriction']
    assert next(d for d in deps if d['modId']=='tensura')['versionRange'] == '[2.0.1.3,2.0.1.5)'
    assert next(d for d in deps if d['modId']=='geckolib')['versionRange'] == '[4.9.2,5)'
    assert not any(n.endswith('.jar') for n in names), 'Dependency jars must not be redistributed inside the addon'
    for cls in ['HighRealm','HighRealmConfig','GatePattern','RealmData','HighRealmElites','HighRealmLoadouts','HighRealmAggro','HighRealmSpawns','HighRealmEvolutions']:
        assert 'dev/heavenlyrestriction/highrealm/'+cls+'.class' in names
    for path in (resources/'data/heavenlyrestriction').rglob('*.json'):
        if any(key in path.as_posix() for key in ['/worldgen/','/dimension/','/dimension_type/','high_realm_elites']):
            name=path.relative_to(resources).as_posix();assert z.read(name)==path.read_bytes(),name
    assert 'heavenlyrestriction:split_mind' in unique['values']
    assert any(e['id']=='split-mind' for e in guide)
    assert any(e['id']=='marauders-map' and any(s['title']=='Dimensions and shared exploration' for s in e['sections']) for e in guide)
    assert 'heavenlyrestriction:marauders_map' in unique['values']
    for cls in ['MaraudersMapSkill','cartography/MapData','cartography/MapColorMip','cartography/MapTileLayout','cartography/StructureMarkerIndex','client/cartography/MapRegionMips','cartography/MapService','cartography/MapTerrain','cartography/MapAutoStructures','cartography/MapExploration','cartography/SharedTerrainIndex','client/cartography/MapUi','client/cartography/MapPopup','client/cartography/MapDimensionTab','cartography/SurveyTiles','cartography/SurveyNoiseCache','cartography/StructureMarkerClusters','cartography/MapStructureSearch','cartography/StructureSearchGrid','cartography/MapTravel','client/cartography/MapTravelScreen','client/cartography/MapWarpScreen','client/cartography/MapTerrainAtlas','client/cartography/MapAtlas','client/cartography/MaraudersMapScreen']:
        assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    for cls in ['SplitMindSkill','splitmind/SplitMindCasting', 'splitmind/SplitMindModes', 'splitmind/SplitMindProjectile','splitmind/SplitMindNetwork','client/SplitMindScreen']:
        assert 'dev/heavenlyrestriction/'+cls+'.class' in names
    assert any(e['id']=='high-realm' and e['category']=='Dimensions' for e in guide)
    pool=json.loads(z.read('data/heavenlyrestriction/tags/entity_type/high_realm_elites.json'))['values'];assert len(pool)==10 and all(t.startswith('tensura:') for t in pool) and 'tensura:arch_daemon' in pool and 'tensura:lesser_daemon' not in pool
    for biome in ['ivory_crags','starwood_grove','mistglass_basin']:
        spawns=json.loads(z.read('data/heavenlyrestriction/worldgen/biome/'+biome+'.json'))['spawners']['monster'];assert all(e['type'] in pool or e['type']=='heavenlyrestriction:kael_veyr' for e in spawns);boss=[e for e in spawns if e['type']=='heavenlyrestriction:kael_veyr'];assert len(boss)==1 and boss[0]['weight']==1 and boss[0]['minCount']==boss[0]['maxCount']==1
    for cls in ['WayfarerContent','WayfarerSpawns','WayfarerLoadout','KaelVeyrEntity','WayfarerCombat']:
        assert 'dev/heavenlyrestriction/highrealm/boss/'+cls+'.class' in names
    assert 'dev/heavenlyrestriction/client/WayfarerRenderer.class' in names
    for name in ['kael_veyr','kael_veyr_emissive']:
        path='assets/heavenlyrestriction/textures/entity/'+name+'.png'
        assert z.read(path)==(resources/path).read_bytes()
        assert struct.unpack('>II',z.read(path)[16:24])==(64,64)
    for path in ['data/heavenlyrestriction/loot_table/entities/kael_veyr.json','data/heavenlyrestriction/advancement/starless_wayfarer.json','data/c/tags/entity_type/bosses.json','data/tensura/tags/entity_type/boss_for_hero.json','assets/heavenlyrestriction/models/item/kael_veyr_spawn_egg.json']:
        assert z.read(path)==(resources/path).read_bytes()
    assert language['entity.heavenlyrestriction.kael_veyr']=='Kael Veyr'
print('Independent 1.23.0 JAR, native dependency metadata, all extracted icons/rigs/animations and mixin classes verified; no Myths classes, bundled dependencies or review fixtures.')
