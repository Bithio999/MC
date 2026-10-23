# Editable visual projects

These are original Tensura Myths models, textures and keyframes. Open a `.bbmodel` in **Blockbench 5.2.2**, with **GeckoLib Models & Animations 4.2.5** installed. Textures are embedded in each project. The two projects under `items/` use Blockbench's Java block/item format instead.

The directory contains 62 skill/boss-effect rigs, two impact material variants, Ananke, Alma/Gemma equipment, and two ritual items: **69 projects** in total. `manifest.json` maps the existing network effect IDs to their assets and placement rules. ID 61 remains retired.

The editable projects are the artist-facing source. To export them through the actual editor/plugin, install Playwright and Chromium, then run:

```sh
node scripts/export-blockbench.cjs
python scripts/verify-visual-assets.py
```

Set `CHROMIUM_PATH` if Chromium is installed elsewhere. `MODEL=ananke` restricts an export to matching project names; `CAPTURE=/absolute/existing/directory` saves representative editor screenshots. The exporter uses curl with normal proxy/CA verification to load the official web editor/plugin, and caches those tools in the OS temporary directory. No editor/plugin source is shipped with the addon.

`scripts/prepare-gecko-assets.py` is the deterministic initial authoring recipe (requires Pillow). It **recreates the projects and textures**, overwriting manual edits. Use the exporter directly after editing in Blockbench. Keep bone/animation names referenced by the renderer/controllers, and keep project, runtime export and texture changes together in Git.

The exporter writes geometry/animation/item JSON. If you paint an atlas, export its PNG separately to `textures/geo`, update the matching `_glowmask.png`, and refresh the embedded preview textures in other projects sharing that atlas. Theme atlases are intentionally shared; a palette edit can affect several rigs.

Runtime exports live under `src/main/resources/assets/wildhunt/{geo,animations}/myths`, `textures/geo`, and `models/item`. See [the rendering workflow](../../docs/VISUAL_PIPELINE.md) for placement, animation and custom-optics boundaries.
