# Wiki authoring and publishing

The player-facing wiki lives in `wiki/pages/*.md`; `wiki/README.md` is the shared addon chooser. Articles cover current gameplay. Research, validation and change history belong in `docs/`.

## Two addon sections

- **Tensura Myths 0.36.1:** home `Tensura-Myths.md`; existing unprefixed article URLs remain valid.
- **TensuraAdditionalAdditions 1.5.1:** home `TensuraAdditionalAdditions.md`; content pages use `Additions-` prefixes.
- `wiki/catalog.json` assigns every article a `mod`: `myths` or `additions`.
- `wiki/navigation.json` contains separate navigation groups. The site and search keep the selected scope; the shared home searches both and labels results by addon.
- Shared content has separate pages. Do not link an Additions article to Myths-only content. Use the correct release/guide when checking values.

Entries should explain what the content does, how to obtain/use it, and necessary damage types, costs, cooldowns and limits. Keep targeting and survival-critical mechanics. Omit registry IDs, implementation details and repeated change notes. Use existing relevant media; label recordings that show an older version.

## Build and check

```sh
python3 -m pip install Markdown==3.7
python3 scripts/build-wiki.py
python3 scripts/verify-wiki.py
```

The build produces the shared `wiki/index.html`, all `wiki/site/*.html`, per-addon ability directories, scoped offline search, `wiki/github/*.md`, and `releases/tensura-addon-wikis.zip`. Original addon icons and local pictures/video are included. Fixed ZIP timestamps and sorted files make the bundle reproducible. Older versioned ZIP releases are retained unchanged.

The checker verifies local links, images/video paths, ability anchors, duplicate IDs, canonical/HTML targets and addon boundaries. Browser review should cover desktop/mobile, search on both addon sections, and video controls. The site uses no remote script, font or search service.

## GitHub Wiki export

GitHub’s Wiki is a separate Git repository, `MC.wiki.git`. Committing `wiki/` to the main repository updates the repository/offline editions, not that separate service.

When the Wiki repository is initialized and accessible to the configured credentials:

```sh
python3 scripts/export-github-wiki.py --publish
```

Without `--publish`, the script only refreshes `wiki/github`. Publishing preserves unrelated remote pages/history and does not change repository settings. GitHub uses its own theme; the downloadable edition provides the custom styling and scoped search.

## In-game guide

The F8 guides are separate release resources and already contain only their installed addon’s content. This wiki reorganization does not alter gameplay, guide resources or release JARs. Do not run `build-ingame-guide.py` as part of a wiki build: it updates mod resources and needs its own release validation.
