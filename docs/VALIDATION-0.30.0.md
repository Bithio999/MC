# 0.30.0 validation — Eschaton, the Last Myth

## Build and native gameplay

Validated on Java 21, Minecraft 1.21.1, NeoForge 21.1.209, Tensura 2.0.1.3, ManasCore 4.0.0.2 and GeckoLib 4.9.3.

```sh
gradle build runGameTestServer --console=plain
python scripts/verify-visual-assets.py
```

**Result: BUILD SUCCESSFUL; all 230 required native GameTests and 11 JUnit tests passed.** This includes 24 new final-Myth tests and the 206 existing regression tests.

The new native checks cover:

- Actual **1,000,000,000 base and maximum EP**, 500M AP/500M MP, 1M HP/4M SHP, mastered/enabled stored support and Godly/Ultimate compatibility.
- Exclusion of the boss race and signature skill from player reincarnation/slots; local HP/SHP handling without raising global limits; real attribute modifiers.
- Funded native Infinite Regeneration before fatal forms; exhausted HP and spiritual deaths; exactly two complete four-pool revivals; final boss credit; red feedback only for confirmed damage.
- Paid costs/cooldowns, warning timing and real hit geometry for all eight modes: three fan bands, rotating rays, seven locked elemental impacts, three spatial corridors, gapped/deduplicated fronts, one-time paid conversion, locked safe landing and the 56-block apocalypse.
- Three damageable seals, native minimum existence, once-only reserve drains, real refuges, complete interruption, stagger, and removal of owned warnings/seals.
- Saved forms, wounds and resources without a free refill or untelegraphed resumed attack; native arena participant protection in both directions.
- One-time native 25M-EP crown reward, non-stacking permanent HP/SHP modifiers, safe blocked/duplicate rituals and operator removal.
- Actual live AI activation through native skill storage, energy-aware selection when exhausted, and long-range warning networking/culling bounds.

The test fixture explicitly loads the complete seal-placement area and waits for actual entity-index and native healing state. This avoids treating delayed chunk promotion as an attack or regeneration failure. The existing Ananke migration check likewise waits for migration to occur rather than assuming a two-tick load deadline. Combat assertions remain intact.

## Assets and native client

The real Blockbench/GeckoLib export and graph validator passed for **83 editable projects**, comprising **79 animated geometry exports**, **171 clips**, **1,176 continuous/faceted surface anchors**, and **four Java item models**. All 72 active effect IDs are covered; ID 61 remains retired. The validator checks Eschaton's form bones, seven crown blades, articulated tail, seven specialized clips and tapered crescents, plus Genesis Seal crescents.

The isolated **Myths Validation** client world was used to review the actual animated boss, both form changes, all eight real native casts, creation seals and a broken seal's golden refuge, four resource bars, the celestial canopy/lensed world seed and confirmed red hit feedback. Screenshots are committed in `wiki/media`; the full gallery has 72 native formation previews.

The opt-in [review fixture](research/fixtures/visuals-0.30.0/README.md) was excluded from the normal release build. The packaged JAR was checked for the boss's code, geometry, item model and loot table and for absence of both `OriginReview.class` and `VisualPreview.class`.

The client used a software-rendered headless X display. No shader pack was required. OpenAL was unavailable, so audio playback was not auditioned; the encounter uses registered native sound events. These staged views and automated hits are not a multiplayer time-to-kill or hardware-performance benchmark. Third-party addons inspected in previous research were not installed for this validation.

## Wiki and distribution

- **153 articles plus the Main Page**, with **80 valid ability anchors** and **8,810 checked local HTML references**.
- Twelve responsive article checks across 1440px desktop and 390px mobile, including Eschaton, Demiurge and Origin Sovereign; no horizontal overflow, missing images or page errors.
- All **72 formation previews** load on desktop and mobile. Offline search finds the new apocalypse by ability name.
- The offline archive matches every wiki file byte-for-byte: **751 entries**. The GitHub Wiki edition includes Home, every article, sidebar and footer in the repository's prepared mirror.

Distribution: `releases/tensura-myths-0.30.0.jar`, `releases/tensura-myths-wiki-0.30.0.zip` and `releases/tensura-myths-0.30.0.sha256`. Install only this addon version, alongside required dependencies, on both client and server. The `wildhunt` namespace and all existing content remain compatible with existing registrations.
