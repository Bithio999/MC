#!/usr/bin/env python3
"""Build the offline wiki. Optional dependency: Markdown==3.7."""
from pathlib import Path
import html
import json
import re
import shutil
import zipfile

try:
    import markdown
except ImportError:
    raise SystemExit("Install the optional renderer: python3 -m pip install Markdown==3.7")

ROOT = Path(__file__).resolve().parents[1]
WIKI = ROOT / "wiki"
SITE = WIKI / "site"
SITE.mkdir(exist_ok=True)
catalog = json.loads((WIKI / "catalog.json").read_text())
titles = {item["slug"]: item["title"] for item in catalog}
lines = [
    ("Rey Dau", "Storm-Crown", "Raijin", "storm_crown", "Lightning · Mobility"),
    ("Uth Duna", "Scarlet-Deluge", "Varuna", "scarlet_deluge", "Water · Barriers"),
    ("Nu Udra", "Black-Flame", "Agni", "black_flame", "Flame · Control"),
    ("Jin Dahaad", "Frostbound-Colossus", "Ymir", "frostbound_colossus", "Ice · Capture"),
]
groups = [
    ("Navigation", [("", "Main Page"), ("Getting-Started", "Getting started"), ("Version-History", "Version history")]),
    ("Gameplay", [("Skills", "Skills"), ("Evolution", "Human evolution"), ("Mechanics", "Combat mechanics"), ("Aura-Regeneration", "Aura regeneration")]),
    ("Apex skills", [(row[1], titles[row[1]]) for row in lines]),
    ("Otherworlders", [("Alma", "Alma"), ("Gemma", "Gemma")]),
    ("Tools", [("Commands", "Commands")]),
]
# Ship only original addon art; the HTML never relies on the source tree.
textures = ROOT / "src/main/resources/assets/wildhunt/textures"
for directory in ("skill", "race", "entity/otherworlder"):
    target = WIKI / "assets" / directory
    target.mkdir(parents=True, exist_ok=True)
    for source in sorted((textures / directory).glob("*.png")):
        shutil.copyfile(source, target / source.name)


def link(slug, prefix=""):
    return prefix + ("site/" + slug + ".html" if slug else "index.html")


def portal():
    cards = ""
    for monster, unique, ultimate, icon, role in lines:
        cards += f'''<article class="apex-card"><a class="portrait-link" href="{link(unique)}"><img src="assets/skill/{icon}.png" width="96" height="96" alt="{monster}"></a><div><span class="eyebrow">{monster}</span><h3><a href="{link(unique)}">{titles[unique]}</a></h3><p>{role}</p><a class="evolution-link" href="{link(ultimate)}">{titles[ultimate]} →</a></div></article>'''
    races = "".join(f'<a class="race-step" href="{link(slug)}"><img src="assets/race/{slug.lower().replace("-", "_")}.png" width="56" height="56" alt=""><span>{titles[slug]}<small>{ep} base EP · {kills} boss type{"s" if kills != 1 else ""}</small></span></a>' for slug, ep, kills in [("Awakened-Hunter", "100K", 1), ("Apex-Hunter", "400K", 3), ("Sovereign-Hunter", "2M", 5)])
    return f'''<section class="welcome"><span class="eyebrow">The hunter's field guide</span><h2>Welcome to the Wild Hunt Wiki</h2><p>A guide to the Monster Hunter inspired addon for <strong>Tensura: Reincarnated</strong>. Explore apex skills, human evolution, aura regeneration, and otherworlders.</p><div class="welcome-links"><a class="button" href="{link('Getting-Started')}">Getting started</a><a href="{link('Skills')}">Browse all skills →</a></div><div class="wiki-stats"><span><strong>{len(catalog)}</strong> articles</span><span><strong>0.7.2</strong> addon version</span><span><strong>1.21.1</strong> Minecraft · NeoForge</span></div></section>
<section class="portal-panel"><h2>Apex skills</h2><p class="panel-intro">Four apex inspirations. Four distinct combat styles. Each Unique Skill has an Ultimate evolution.</p><div class="apex-grid">{cards}</div></section>
<div class="portal-columns"><section class="portal-panel"><h2>Human evolution</h2><p class="panel-intro">A human-looking hunter branch, earned through EP and distinct boss kills.</p><div class="race-path">{races}</div><p class="panel-foot"><a href="{link('Evolution')}">Requirements and race bonuses →</a></p></section>
<section class="portal-panel"><h2>Explore the world</h2><div class="topic"><h3><a href="{link('Aura-Regeneration')}">Aura regeneration</a></h3><p>Three Extra Skills granted through hunter evolution. Recover health using aura.</p></div><div class="topic"><h3>Otherworlders</h3><p>Meet <a href="{link('Alma')}">Alma</a> and <a href="{link('Gemma')}">Gemma</a>, Awakened Hunters with their own native skill sets.</p></div><div class="topic"><h3><a href="{link('Mechanics')}">Combat mechanics</a></h3><p>Learn about barriers, damage, mastery, toggles, and the aura / magicule split.</p></div></section></div>
<section class="portal-panel"><h2>Wiki information</h2><div class="info-columns"><p>This is an unofficial community-style reference for <strong>Tensura: Wild Hunt</strong>. The pages document the addon; the <a href="https://tensura.wiki.gg/wiki/Tensura:_Reincarnated_Wiki">Tensura: Reincarnated Wiki</a> covers the base mod.</p><p><a href="{link('Version-History')}">Version history</a> · <a href="{link('Commands')}">Commands</a><br><a href="https://github.com/Bithio999/MC/blob/main/releases/tensura-wild-hunt-0.7.2.jar">Download the addon</a><br>This edition works offline, including search.</p></div></section>'''


def render(source, destination, home=False):
    title, body = source.read_text().split("\n", 1)
    title = "Main Page" if home else title.removeprefix("# ").strip()
    slug = "" if home else source.stem
    prefix = "" if home else "../"
    skin = "site/" if home else ""
    renderer = markdown.Markdown(extensions=["tables", "fenced_code", "toc"])
    content = renderer.convert(body)

    def href(match):
        value = match[1]
        if value.startswith(("http:", "https:", "#")):
            return match[0]
        path, sep, fragment = value.partition("#")
        if home and path.startswith("pages/"):
            path = "site/" + path[6:].removesuffix(".md") + ".html"
        elif not home and path == "../README.md":
            path = "../index.html"
        elif not home and "/" not in path and path.endswith(".md"):
            path = path.removesuffix(".md") + ".html"
        elif "releases/" in path or path == "../README.md":
            path = "https://github.com/Bithio999/MC/blob/main/" + path.removeprefix("../")
        return 'href="' + path + (sep + fragment if sep else "") + '"'

    content = re.sub(r'href="([^"]+)"', href, content)
    content = content.replace("../../src/main/resources/assets/wildhunt/textures/", "../assets/")
    content = re.sub(r'^\s*<p><a href="../index.html">Main Page</a>.*?</p>', '', content, count=1, flags=re.S)
    content = re.sub(r'<hr\s*/?>\s*<p><strong>Categories:</strong>.*?</p>', '', content, flags=re.S)
    infobox = ""
    category = "Guides"
    table = re.search(r"<table>.*?</table>", content, flags=re.S)
    if table and any(kind + " data" in table[0] for kind in ("Skill", "Race", "NPC")):
        data = table[0]
        category = "Skills" if "Skill data" in data else "Races" if "Race data" in data else "Otherworlders"
        content = content[:table.start()] + content[table.end():]
        portrait = ""
        if category == "Skills":
            match = re.search(r'<p><img [^>]*src="../assets/skill/[^>]+></p>', content)
            if match:
                portrait = '<div class="infobox-portrait">' + match[0] + '</div>'
                content = content[:match.start()] + content[match.end():]
        elif category == "Races":
            portrait = f'<div class="infobox-portrait"><img src="../assets/race/{slug.lower().replace("-", "_")}.png" width="128" height="128" alt="{html.escape(title)} race icon"></div>'
        elif category == "Otherworlders":
            portrait = f'<div class="infobox-portrait"><div class="npc-face {slug.lower()}" role="img" aria-label="{title} in-game face"></div></div>'
        infobox = f'<aside class="infobox" aria-label="{category} data"><div class="infobox-kind">{category}</div><h2>{html.escape(title)}</h2>{portrait}{data}</aside>'
    nav = ""
    for group, entries in groups:
        nav += f'<section><h2>{group}</h2>' + ''.join(f'<a href="{link(key, prefix)}"' + (' aria-current="page"' if key == slug else '') + f'>{label}</a>' for key, label in entries) + '</section>'
    toc = '<details class="contents" open><summary>Contents</summary>' + renderer.toc + '</details>' if renderer.toc_tokens and not home else ""
    if home:
        content = portal()
    related = "".join(f'<a href="{link(item["slug"], prefix)}"' + (' aria-current="page"' if item['slug'] == slug else '') + f'>{html.escape(item["title"])}</a>' for item in catalog if (category == "Skills" and item['slug'] in [v for row in lines for v in row[1:3]] + ["Aura-Regeneration"]) or (category == "Races" and item['slug'].endswith("-Hunter")) or (category == "Otherworlders" and item['slug'] in ("Alma", "Gemma")))
    navbox = f'<nav class="related-box" aria-label="Related {category.lower()}"><strong>Wild Hunt · {category}</strong><div>{related}</div></nav>' if related else ""
    page = f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="description" content="{html.escape(title)} — the unofficial Tensura: Wild Hunt addon reference.">
<title>{html.escape(title)} — Wild Hunt Wiki</title><link rel="stylesheet" href="{skin}wiki.css">
<script defer src="{skin}search-index.js"></script><script defer src="{skin}search.js"></script></head>
<body data-root="{prefix}"><a class="skip" href="#article">Skip to article</a>
<div class="global-bar"><span>TENSURA: WILD HUNT</span><div><a href="https://tensura.wiki.gg/wiki/Tensura:_Reincarnated_Wiki">Base mod wiki</a><a href="https://github.com/Bithio999/MC">GitHub</a></div></div>
<header class="masthead"><a class="brand" href="{prefix}index.html"><span class="brand-mark" aria-hidden="true">W</span><span>WILD HUNT<small>Tensura: Reincarnated addon wiki</small></span></a>
<div class="search"><label for="wiki-search">Search Wild Hunt Wiki</label><input id="wiki-search" type="search" placeholder="Search skills, races, characters…" autocomplete="off" role="combobox" aria-autocomplete="list" aria-controls="search-results" aria-expanded="false"><div id="search-results" role="listbox" aria-label="Search results" hidden></div><span id="search-status" class="sr-only" aria-live="polite"></span></div></header>
<div class="layout"><button id="navigation-toggle" aria-controls="wiki-navigation" aria-expanded="false">☰ Wiki navigation</button><nav id="wiki-navigation" class="sidebar" aria-label="Wiki navigation">{nav}<div class="sidebar-version">Wild Hunt <strong>0.7.2</strong><br>Minecraft 1.21.1 · NeoForge<br><span>Offline reference edition</span></div></nav>
<main id="article" class="{'main-page' if home else 'article-page'}"><div class="page-tabs"><span aria-current="page">{'Main Page' if home else 'Article'}</span><a href="{'README.md' if home else '../pages/' + source.name}">Read Markdown</a><a href="{link('Version-History', prefix)}">Version history</a></div>
<div class="article-heading"><div class="breadcrumbs"><a href="{prefix}index.html">Wild Hunt Wiki</a>{' / ' + category if not home else ''}</div><h1>{html.escape(title)}</h1><p class="subtitle">From the unofficial Tensura: Wild Hunt Wiki</p></div>
<div class="article-content">{infobox}{toc}{content}</div>{navbox}
<div class="categories">Categories: <a href="{prefix}index.html">Tensura: Wild Hunt</a> · {category if not home else 'Main Page'}</div>
<footer>Unofficial fan addon. Original documentation and portraits. Not affiliated with Capcom or ManasMods.<br>Wild Hunt 0.7.2 · Offline edition · No external services required.</footer></main></div></body></html>'''
    destination.write_text(page)


render(WIKI / "README.md", WIKI / "index.html", True)
for item in catalog:
    source = WIKI / "pages" / (item["slug"] + ".md")
    render(source, SITE / (item["slug"] + ".html"))
    item["keywords"] = source.read_text().split("## Overview")[0]
(SITE / "search-index.js").write_text("window.WIKI_PAGES = " + json.dumps(catalog) + ";\n")
# Fixed timestamps and sorted entries keep this documentation bundle reproducible.
archive = ROOT / "releases/wild-hunt-wiki-0.7.2.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as bundle:
    for path in sorted(WIKI.rglob("*")):
        if path.is_file():
            entry = zipfile.ZipInfo(path.relative_to(WIKI).as_posix(), (2026, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = 0o644 << 16
            bundle.writestr(entry, path.read_bytes())
print(f"Built Main Page, {len(catalog)} articles, and {archive.name}.")
