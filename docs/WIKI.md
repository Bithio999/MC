# Wiki authoring and publishing

The canonical articles live in `wiki/pages/*.md`; `wiki/README.md` is the GitHub-readable home page. Edit those sources rather than the generated copies.

## Build both editions

Install the optional documentation dependency, then build:

```sh
python3 -m pip install Markdown==3.7
python3 scripts/build-wiki.py
```

This generates:

- `wiki/index.html` and `wiki/site/*.html`: the styled visual edition.
- `wiki/github/*.md`: the actual GitHub Wiki edition, including `Home.md`, all 143 gameplay/reference articles, `_Sidebar.md`, and `_Footer.md`.
- `releases/tensura-myths-wiki-0.15.0.zip`: the self-contained offline edition.

GitHub Wiki uses GitHub's own theme and strips custom CSS and scripts. Its pages mirror the visual edition's structure, original portraits, topic tables, navigation, and article content. The ZIP provides the full custom theme and search.

## Publish to the actual GitHub Wiki

The Wiki is a separate Git repository (`MC.wiki.git`); committing `wiki/` to `MC` alone does not publish it.

If the Wiki has never been enabled, open [repository settings](https://github.com/Bithio999/MC/settings), select **General → Features → Wikis**, then visit [the Wiki tab](https://github.com/Bithio999/MC/wiki), create a page titled **Home**, and save it once. GitHub does not expose a REST endpoint for creating that first Wiki page. Private repository Wikis may also require a supported GitHub plan.

Then publish with Git credentials that can push to the Wiki:

```sh
python3 scripts/export-github-wiki.py --publish
```

The script exports the current sources, clones the Wiki, copies the generated Markdown files, commits changes if needed, and pushes to its existing branch. Other pages and existing Git history are preserved. No tokens are stored in files or remote URLs. Running without `--publish` only prepares the files locally.

The current Codex integration can push the main repository, but GitHub refused its attempt to enable the Wiki with **403 Resource not accessible by integration**. If that remains the case after initialization, publish using an account or Git integration with Wiki access. The prepared pages are in Git for review and manual upload.

## Visual reference

The redesign was checked against these public pages on 2026-10-19:

- [Tensura: Reincarnated Wiki home](https://tensura.wiki.gg/wiki/Tensura:_Reincarnated_Wiki): centered welcome, About, Links and Contribute panels; a three-column topic grid; blue panel boundaries.
- [Great Sage](https://tensura.wiki.gg/wiki/Great_Sage) and [Predator](https://tensura.wiki.gg/wiki/Predator): short introductions, sectioned Information / Evolution / Obtaining infoboxes, Usage divided into Passive and Active, and categorized skill navboxes.
- [Human](https://tensura.wiki.gg/wiki/Races/Human): serif article headings, compact contents, right-hand data infobox with labels and values, evolution references.

The reference's light-theme colors include white content, `#006ee6` borders, `#274690` accents, and `#0645ad` links. This repository implements its own styles and uses only original addon branding and images; no Tensura logos, backgrounds, fonts, or article text are copied.

## Individual article layout

Skill articles use a lead summary, grouped data table, Usage with Passive / Active subsections and labeled arts, Obtaining, Evolution, then strategy and reference notes. Race articles group information, vitality bonuses, evolution and obtaining in their infobox, with Racial Ability, Traits and Evolution in the body. NPC pages put abilities and spawning before the screenshot gallery. The renderer turns the same Markdown data into a sectioned right-hand infobox; GitHub readers retain an ordinary data table. No gameplay values are changed by this layout.

Development history, save migration, and balancing references live separately in [CHANGELOG.md](CHANGELOG.md). Keep gameplay articles in present tense and describe current behavior.

## Topic organization and damage fields

The shared sidebar is defined in `wiki/navigation.json`. `wiki/catalog.json` records article category and skill tier; both editions use these for a consistent hierarchy. Skills, Races, Otherworlders and Reference are the main entry points. The 105 fusion articles remain individually addressable and are indexed by ancestry.

Each active/passive variant is listed in `wiki/abilities.json` and the Ability index. Under each skill's usage label, include an Ability information table with Damage type and Damage ID. Explicitly label utility/healing as None. Separate direct hits from native status damage such as Black Burn; never infer damage from an animation, skill name or resource. Update the ability index and metadata when adding an art. See [damage audit](WIKI-DAMAGE-AUDIT.md) for the inspected native paths.
