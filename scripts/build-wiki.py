#!/usr/bin/env python3
"""Build the offline wiki. Optional dependency: Markdown==3.7."""
from pathlib import Path
import html
import json
import re
import shutil
import zipfile
import subprocess
import sys

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
    icons = "".join(f'<a href="{link(unique)}"><img src="assets/skill/{icon}.png" width="64" height="64" alt="{monster}"></a>' for monster, unique, ultimate, icon, role in lines)
    links = "".join(f'<a href="{link(slug)}">{label}</a>' for slug, label in [
        ("Skills", "Abilities"), ("Evolution", "Races"), ("Alma", "Otherworlders"),
        ("Aura-Regeneration", "Aura regeneration"), ("Commands", "Commands"), ("Mechanics", "Mechanics"),
        ("Getting-Started", "Getting started"), ("Version-History", "Version history"), ("Gemma", "Gemma")])
    rows = "".join(f'<tr><td><img src="assets/skill/{icon}.png" width="48" height="48" alt=""> {monster}</td><td><a href="{link(unique)}">{titles[unique]}</a></td><td><a href="{link(ultimate)}">{titles[ultimate]}</a></td><td>{role}</td></tr>' for monster, unique, ultimate, icon, role in lines)
    return f'''<section class="welcome portal-panel"><div class="welcome-wordmark" aria-label="Tensura: Wild Hunt"><small>TENSURA: REINCARNATED ADDON</small><strong>WILD HUNT</strong><span>THE HUNTER'S FIELD GUIDE</span></div><h2>Welcome to the Tensura: Wild Hunt Wiki!</h2><div class="panel-body"><p>An unofficial reference for the Monster Hunter inspired addon to <strong>Tensura: Reincarnated</strong>.</p><div class="welcome-portraits">{icons}</div><p>We currently document <strong>{len(catalog)} articles</strong>, covering four apex skill lines, three hunter evolutions, aura recovery, and otherworlders.</p><p><strong>Wild Hunt 0.7.2 · Minecraft 1.21.1 · NeoForge</strong></p></div></section>
<section class="portal-panel"><h2>About the addon</h2><div class="panel-body"><p>Follow a human-looking hunter evolution branch and wield abilities inspired by Rey Dau, Uth Duna, Nu Udra, and Jin Dahaad. Each apex Unique Skill evolves into an Ultimate with its own signature art.</p><p>Build around aura, native barriers, regeneration, and movement. Meet Alma and Gemma through Tensura's otherworlder system. Start with <a href="{link('Getting-Started')}">Getting started</a> for installation and your first steps.</p></div></section>
<section class="portal-panel"><h2>Links</h2><nav class="portal-links" aria-label="Wiki topics">{links}</nav></section>
<section class="portal-panel"><h2>Apex abilities</h2><div class="panel-body table-scroll"><table><thead><tr><th>Inspiration</th><th>Unique Skill</th><th>Ultimate Skill</th><th>Combat identity</th></tr></thead><tbody>{rows}</tbody></table></div></section>
<section class="portal-panel"><h2>Hunter evolution</h2><div class="panel-body"><p class="evolution-path">Human → <a href="{link('Awakened-Hunter')}">Awakened Hunter</a> → <a href="{link('Apex-Hunter')}">Apex Hunter</a> → <a href="{link('Sovereign-Hunter')}">Sovereign Hunter</a></p><p>Progress through 100K / 400K / 2M base EP and 1 / 3 / 5 distinct boss types. Sovereign Hunter also requires mastery of an apex Unique. Each stage grants an <a href="{link('Aura-Regeneration')}">aura regeneration Extra Skill</a>. See <a href="{link('Evolution')}">Human evolution</a> for full requirements and vitality bonuses.</p></div></section>
<section class="portal-panel"><h2>Contribute to the wiki</h2><div class="panel-body"><p>Report missing details, suggest corrections, or improve the Markdown articles in the <a href="https://github.com/Bithio999/MC/tree/main/wiki/pages">repository</a>. Please include the addon version when reporting a gameplay difference.</p><p><a href="https://github.com/Bithio999/MC/wiki">GitHub Wiki</a> · <a href="https://github.com/Bithio999/MC/blob/main/releases/wild-hunt-wiki-0.7.2.zip">Download the offline wiki</a> · <a href="https://tensura.wiki.gg/wiki/Tensura:_Reincarnated_Wiki">Base mod wiki</a></p><p>This is an unofficial fan addon, not the official Tensura or Monster Hunter wiki.</p></div></section>'''


def data_infobox(title, portrait, data, category):
    """Render the article data in the reference wiki's grouped label/value style."""
    groups = []
    heading, rows = "Information", []
    for row in re.findall(r"<tr>(.*?)</tr>", data, re.S):
        cells = re.findall(r"<td[^>]*>(.*?)</td>", row, re.S)
        if len(cells) != 2:
            continue
        label, value = cells
        if re.fullmatch(r"<strong>[^<]+</strong>", label.strip()) and not value.strip():
            if rows:
                groups.append((heading, rows))
            heading, rows = re.sub(r"</?strong>", "", label), []
        else:
            rows.append(f'<tr><th scope="row">{label}</th><td>{value}</td></tr>')
    if rows:
        groups.append((heading, rows))
    sections = "".join('<section class="infobox-section"><h3>' + label + '</h3><table aria-label="' + html.escape(label) + '"><tbody>' + "".join(rows) + '</tbody></table></section>' for label, rows in groups)
    return f'<aside class="infobox" aria-label="{html.escape(title)} {category.lower()} data"><h2>{html.escape(title)}</h2>{portrait}{sections}</aside>'


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
        infobox = data_infobox(title, portrait, data, category)
    nav = ""
    for group, entries in groups:
        nav += f'<section><h2>{group}</h2>' + ''.join(f'<a href="{link(key, prefix)}"' + (' aria-current="page"' if key == slug else '') + f'>{label}</a>' for key, label in entries) + '</section>'
    toc = '<details class="contents" open><summary>Contents</summary>' + renderer.toc + '</details>' if renderer.toc_tokens and not home else ""
    if home:
        content = portal()
    # Put the article introduction before its contents, as on skill/race pages.
    introduction = ""
    if not home:
        lead = re.match(r"\s*(<p>.*?</p>)", content, re.S)
        if lead:
            introduction = '<div class="article-intro">' + lead[1] + '</div>'
            content = content[lead.end():]
        content = re.sub(r'<p>(\[(?:Active|Passive)[^]]*\])', r'<p class="ability-label">\1', content)
        content = re.sub(r"(<table>.*?</table>)", r'<div class="article-table">\1</div>', content, flags=re.S)
    if category == "Skills":
        related_groups = [("Unique Skills", [row[1] for row in lines]), ("Ultimate Skills", [row[2] for row in lines]), ("Extra Skills", ["Aura-Regeneration"])]
    elif category == "Races":
        related_groups = [("Human / Hunter", ["Evolution", "Awakened-Hunter", "Apex-Hunter", "Sovereign-Hunter"])]
    elif category == "Otherworlders":
        related_groups = [("Otherworlders", ["Alma", "Gemma"]), ("Race", ["Awakened-Hunter"])]
    else:
        related_groups = [("Reference", ["Getting-Started", "Skills", "Evolution", "Mechanics", "Commands", "Version-History"])]
    related = ""
    for group, slugs in related_groups:
        anchors = "".join(f'<a href="{link(key, prefix)}"' + (' aria-current="page"' if key == slug else '') + f'>{html.escape(titles[key])}</a>' for key in slugs)
        related += f'<div class="navbox-row"><strong>{group}</strong><div>{anchors}</div></div>'
    navbox = f'<nav class="related-box" aria-label="Related {category.lower()}"><strong>Wild Hunt · {category}</strong>{related}</nav>' if not home else ""
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
<div class="article-content">{introduction}{infobox}{toc}{content}</div>{navbox}
<div class="categories">Categories: <a href="{prefix}index.html">Tensura: Wild Hunt</a> · {category if not home else 'Main Page'}</div>
<footer>Unofficial fan addon. Original documentation and portraits. Not affiliated with Capcom or ManasMods.<br>Wild Hunt 0.7.2 · Offline edition · No external services required.</footer></main></div></body></html>'''
    destination.write_text(page)


render(WIKI / "README.md", WIKI / "index.html", True)
for item in catalog:
    source = WIKI / "pages" / (item["slug"] + ".md")
    render(source, SITE / (item["slug"] + ".html"))
    item["keywords"] = source.read_text().split("## Usage")[0].split("## Racial Ability")[0]
(SITE / "search-index.js").write_text("window.WIKI_PAGES = " + json.dumps(catalog) + ";\n")
subprocess.run([sys.executable, str(ROOT / "scripts/export-github-wiki.py")], check=True)
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
