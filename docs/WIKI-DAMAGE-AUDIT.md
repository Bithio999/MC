# Wiki organization and damage audit

Documentation targets Tensura Myths 0.10.0 with Tensura 2.0.1.3. This revision changes documentation and its generated editions; the mod JAR is unchanged.

## Canonical sources

- `wiki/pages/*.md`: 137 gameplay/reference articles, including all 105 Convergent races.
- `wiki/catalog.json`: titles, categories, skill tiers and search order.
- `wiki/navigation.json`: one sidebar hierarchy shared by styled HTML and the GitHub Wiki export.
- `wiki/abilities.json`: 37 active/passive entries, with parent skill, tier, damage ID, optional status-damage ID and article anchor. Shared racial Concordance Pulse is listed once rather than 105 times.

The main directories are Skills, Races, Otherworlders and Reference. Skill tiers have dedicated entry points; Extra Skills have individual articles. Race pages use acquisition, traits, racial ability, evolution and growth sections as applicable. Ancestry links connect each Convergent article to the complete race directory. Changelog and migration history stay outside gameplay articles.

## Damage evidence

`ApexSkill.strike` selects LIGHTNING_ELEMENTAL, WATER_ELEMENTAL, FIRE_ELEMENTAL or ICE_ELEMENTAL by apex. `ApexArts` routes direct attacks, travelling waves, ignition/pool pulses, icefall and signatures through that method. `ApexArts.onDamage` routes Water Veil's retaliation through the same water strike. Attribute-only passives have no independent damage source.

`GodlySkill.hit` uses SPACE_ELEMENTAL for Worldline Sever, Horizon damage and Verdict. Causal Reversal restores state and does not call damage. Unbroken Continuum's bonus modifies eligible attack amounts; it does not convert their damage types. Its aura slows through a movement modifier.

`AuraRegenerationSkill` heals HP/SHP and handles funded lethal-hit survival, with no attack damage source. `FusionRace.onActivateAbility` heals and pushes; Concordance Pulse does not call `hurt`.

Tensura's native `BlackBurnEffect.applyEffectTick` uses BLACK_FLAME with the Flame element and `hurtSplitElemental`; this is separate from FIRE_ELEMENTAL direct hits. Native Flame Manipulation selects Flame Breath or conditional Black Flame Breath. Their projectiles use FLAME_BREATH and BLACK_FLAME respectively. Its touch hook can apply ordinary burning. NPC descriptions distinguish owning a native skill from autonomous use of every player-controlled mode.

The dependency's `data/minecraft/tags/damage_type/bypasses_armor.json` lists all five addon direct elemental damage IDs. Armor bypass is documented separately from barriers, resistance, authority and resource costs. Native damage tags remain datapack configurable.

## Review checks

Build both editions using `python3 scripts/build-wiki.py` with Markdown 3.7 installed. Check local HTML links/assets/anchors, GitHub Wiki cross-links, all ability anchors and catalog coverage. Browser checks cover every article and the homepage at desktop and mobile widths, including images, overflow and ability-name search. The offline bundle is rebuilt from those same sources. GitHub Wiki publishing availability is documented in `docs/WIKI.md`.

Validation completed: all 137 articles plus the homepage at two viewport widths (276 browser checks), all 37 ability anchors, local links/assets, GitHub Wiki cross-links, and byte-for-byte coverage of the rebuilt offline archive pass. Ability-name search opens Aion directly at `#worldline-sever`. The released mod JAR checksum is unchanged.
