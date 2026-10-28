# 0.35.5 validation — Heavenly Arsenal

Target: Java 21, Minecraft 1.21.1, NeoForge **21.1.209**, Tensura 2.0.1.3 and ManasCore 4.0.0.2. Independent addon: **1.1.5**. All **459 tests passed**: **341 full-mod native GameTests**, **107 standalone native GameTests** and **11 JUnit tests**. Both final production builds passed.

## Unerring Body

Chained and Unbound gain Fear, Slowness and Burden immunity only while equipped in the active preset; the combat toggle does not gate it. A lowest-priority effect-applicability listener rejects these effects. Server ticks also cleanse pre-existing copies. Native normal/Sacred Haki field effects and the shared Haki forced-movement function are intercepted at their actual entry points. Other damage reduction/dodge values are unchanged.

Native tests exercise both skills, toggle-off immunity, unequipping, pre-existing effects, retention of ordinary Poison, normal and Sacred Haki, and direct Haki push.

## Weapon-bearing arts

Each dash path/arrival hit and Counter adds the main-hand weapon contribution to its existing aura formula. The item modifier API includes native weapon-multiplier engravings, custom modifiers and vanilla attribute operation ordering. Vanilla enchantment damage runs once on the weapon contribution. Successful hits invoke vanilla post-attack effects, both Tensura engraving callback stages, and native player weapon durability handling. Counter resolves the held weapon at release. Empty hands add zero. Physical classification, independent hits and the exclusive Battlewill/Physical damage branches remain.

Tests check the diamond sword's seven damage, Sharpness V's ten damage, Crushing's native weapon modifier, custom additive/total modifiers and empty hands. Actual dash hits deliver distinct path/arrival values and Fire Aspect. An actual Counter uses a sword equipped after stance entry and hits both the attacker and the adjacent target, with the correct weapon contribution and Fire Aspect on each.

## Inverted Spear of Heaven

The new Rare, fire-resistant sword-class item matches native Hihiirokane Sword attack attributes (80 total player attack damage / 1.6 attack speed) and material durability (3,600). Nullification I is a real registered enchantment in Tensura's engraving and inheritance tags, restricted to this item, initialized through native gear data/inventory updates and already present on generated vault rewards. It is not added to random engraving lists. Original item/effect pixel art is reproducible with `scripts/prepare-heavenly-arsenal-art.py`.

Nullification's native pre-calculation handler runs after native targeting/dodge checks and before native barrier calculation. It clears current physical/magic/multilayer barriers and tagged or ability-attributed skill effects, then applies a visible 40-tick skill seal. Native activation events and `TensuraSkill.canActivateSkill` enforce the seal; learned skills, unrelated potion effects and completed passive ownership are retained. Unrelated spells/projectiles cannot borrow the held weapon's engraving. Barrier entity interception covers the base, ranged and boss override implementations; the latter deliberately returns false for ordinary damage and required its own target. Weapon-art corridors can also shatter intersected native barriers while respecting hostility checks.

Tests exercise native player melee piercing a huge multilayer barrier on that hit, clearing all three barrier attributes and both tagged/source-attributed statuses, retaining unrelated Night Vision, applying the exact duration, shattering ranged/boss barrier variants, blocking actual native activation without resource spending, and real 40-tick expiry. An unrelated ability cannot gain Nullification merely from the held spear.

## Ominous loot and packaging

An additive NeoForge global loot modifier uses the exact ominous-vault reward table ID and a 0.02 random-chance condition. An actual native-table test samples 1,024 ominous reward generations and confirms rare engraved spear drops; 128 ordinary-vault generations contain no spear and retain native rewards. No vanilla loot table is replaced.

The independent distribution audit verifies all common/client mixin classes, new item/effect/engraving/gear/loot resources and artwork, pinned metadata, and absence of Myths classes or bundled dependencies. Final JAR source/class/resource consistency and release checksums are audited. The wiki adds weapon and engraving infoboxes, acquisition, damage types, ability weapon terms and standalone guidance, with HTML/GitHub/offline exports.

## Reproduce

```sh
gradle build runGameTestServer --console=plain
# Independently from standalone/heavenly-restriction:
gradle build runGameTestServer --console=plain
python3 scripts/verify-distribution.py
# Repository root; Markdown 3.7 required:
python3 scripts/prepare-heavenly-arsenal-art.py
python3 scripts/build-wiki.py
```

Native tests used disposable `/tmp` worlds. Existing client saves were preserved. New art was visually inspected; no graphical in-game client review was performed for this release. The last packaging-only rebuild included the wrapped tooltip and finalized language resources; server behavior is the tested implementation.
