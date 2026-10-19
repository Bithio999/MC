#!/usr/bin/env python3
"""Build a self-contained wiki from its GitHub-readable Markdown articles.

Optional authoring dependency: python3 -m pip install Markdown==3.7
Normal mod builds do not need this script or its dependency.
"""
from pathlib import Path
import html
import json
import re

try:
    import markdown
except ImportError:
    raise SystemExit("Install the optional renderer: python3 -m pip install Markdown==3.7")

ROOT = Path(__file__).resolve().parents[1]
WIKI = ROOT / "wiki"
SITE = WIKI / "site"
SITE.mkdir(exist_ok=True)
catalog = json.loads((WIKI / "catalog.json").read_text())


def render(source, destination, home=False):
    text = source.read_text()
    title, body = text.split("\n", 1)
    title = title.removeprefix("# ")
    renderer = markdown.Markdown(extensions=["tables", "fenced_code", "toc"])
    content = renderer.convert(body)

    def href(match):
        value = match.group(1)
        if value.startswith(("http:", "https:", "#")):
            return match.group(0)
        path, sep, fragment = value.partition("#")
        if home and path.startswith("pages/"):
            path = "site/" + path[6:].removesuffix(".md") + ".html"
        elif not home and path == "../README.md":
            path = "../index.html"
        elif not home and "/" not in path and path.endswith(".md"):
            path = path.removesuffix(".md") + ".html"
        return 'href="' + path + (sep + fragment if sep else "") + '"'

    content = re.sub(r'href="([^"]+)"', href, content)
    infobox = ""
    # Each race/skill article begins with its data table. Keep mode tables inline.
    table = re.search(r"<table>.*?</table>", content, flags=re.S)
    if table and ("Skill data" in table[0] or "Race data" in table[0] or "NPC data" in table[0]):
        infobox = table[0]
        content = content[:table.start()] + content[table.end():]
        portrait = re.search(r'<p><img [^>]+></p>', content)
        if portrait:
            infobox = portrait[0] + infobox
            content = content[:portrait.start()] + content[portrait.end():]
        infobox = '<aside class="infobox" aria-label="Page data">' + infobox + '</aside>'
    prefix = "" if home else "../"
    skin = "site/" if home else ""
    navigation = [("Main Page", "index.html"), ("Getting started", "site/Getting-Started.html"),
                  ("Skills", "site/Skills.html"), ("Human evolution", "site/Evolution.html"),
                  ("Combat mechanics", "site/Mechanics.html"), ("Aura regeneration", "site/Aura-Regeneration.html"), ("Alma", "site/Alma.html"), ("Gemma", "site/Gemma.html"), ("Commands", "site/Commands.html"),
                  ("Version history", "site/Version-History.html")]
    links = "".join(f'<a href="{prefix}{url}">{label}</a>' for label, url in navigation)
    toc = '<details class="contents" open><summary>Contents</summary>' + renderer.toc + '</details>' if renderer.toc_tokens else ""
    page = f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)} — Wild Hunt Wiki</title><link rel="stylesheet" href="{skin}wiki.css">
<script defer src="{skin}search-index.js"></script><script defer src="{skin}search.js"></script></head>
<body data-root="{prefix}"><a class="skip" href="#article">Skip to article</a>
<header><a class="brand" href="{prefix}index.html"><span>WILD HUNT</span><small>Tensura: Reincarnated addon wiki</small></a>
<div class="search"><label for="wiki-search">Search the wiki</label><input id="wiki-search" type="search" placeholder="Find a skill, race, or mechanic…" autocomplete="off"><div id="search-results" aria-live="polite"></div></div></header>
<div class="layout"><nav class="sidebar" aria-label="Wiki navigation"><h2>Explore</h2>{links}
<h2>Apex arts</h2>{''.join(f'<a href="{prefix}site/{slug}.html">{label}</a>' for label,slug in [('Storm Crown','Storm-Crown'),('Scarlet Deluge','Scarlet-Deluge'),('Black Flame','Black-Flame'),('Frostbound Colossus','Frostbound-Colossus')])}
<p>Version 0.7.2<br>Minecraft 1.21.1<br>NeoForge</p></nav>
<main id="article"><div class="page-tabs"><span>Article</span><a href="{'README.md' if home else '../pages/' + source.name}">Read Markdown</a></div>
<h1>{html.escape(title)}</h1><p class="subtitle">From the Tensura: Wild Hunt Wiki</p>
{infobox}{toc}{content}
<footer>Unofficial fan addon. Original documentation and portraits. Not affiliated with Capcom or ManasMods. Offline edition; no external services required.</footer></main></div></body></html>'''
    destination.write_text(page)


render(WIKI / "README.md", WIKI / "index.html", True)
for item in catalog:
    render(WIKI / "pages" / (item["slug"] + ".md"), SITE / (item["slug"] + ".html"))
(SITE / "search-index.js").write_text("window.WIKI_PAGES = " + json.dumps(catalog) + ";\n")
print(f"Built Main Page and {len(catalog)} articles in wiki/.")
