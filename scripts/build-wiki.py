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
subprocess.run([sys.executable, str(ROOT / "scripts/build-ability-index.py")], check=True)
WIKI = ROOT / "wiki"
SITE = WIKI / "site"
SITE.mkdir(exist_ok=True)
catalog = json.loads((WIKI / "catalog.json").read_text())
titles = {item["slug"]: item["title"] for item in catalog}
lines = [
    ("Rey Dau", "Storm-Crown", "Vritra", "storm_crown", "Lightning · Mobility"),
    ("Uth Duna", "Scarlet-Deluge", "", "scarlet_deluge", "Water · Barriers"),
    ("Nu Udra", "Black-Flame", "Vritra", "black_flame", "Flame · Control"),
    ("Jin Dahaad", "Frostbound-Colossus", "Demeter", "frostbound_colossus", "Ice · Capture"),
]
groups = json.loads((WIKI / "navigation.json").read_text())
metadata = {item["slug"]: item for item in catalog}
abilities = json.loads((WIKI / "abilities.json").read_text())
# Ship only original addon art; the HTML never relies on the source tree.
textures = ROOT / "src/main/resources/assets/wildhunt/textures"
for directory in ("skill", "race", "entity/otherworlder"):
    target = WIKI / "assets" / directory
    target.mkdir(parents=True, exist_ok=True)
    for source in sorted((textures / directory).rglob("*.png")):
        destination = target / source.relative_to(textures / directory)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)


def link(slug, prefix=""):
    return prefix + ("site/" + slug + ".html" if slug else "index.html")


def portal(content):
    icons = "".join(f'<a href="{link(unique)}"><img src="assets/skill/{icon}.png" width="64" height="64" alt="{monster}"></a>' for monster, unique, ultimate, icon, role in lines)
    welcome = f'<section class="welcome portal-panel"><div class="welcome-wordmark"><small>TENSURA: REINCARNATED ADDON</small><strong>TENSURA MYTHS</strong><span>SKILLS · RACES · MYTH BOSSES</span></div><h2>Welcome to the Tensura Myths Wiki!</h2><div class="panel-body"><p>{len(catalog)} articles · {len(abilities)} active/passive ability entries · 105 Convergent races</p><div class="welcome-portraits">{icons}</div></div></section>'
    # Render the same canonical homepage in the styled and Markdown editions.
    sections = re.split(r"(<h2[^>]*>.*?</h2>)", content, flags=re.S)
    cards = []
    for heading, body in zip(sections[1::2], sections[2::2]):
        cards.append('<section class="portal-panel">' + heading + '<div class="panel-body">' + body + '</div></section>')
    return welcome + '<div class="topic-grid">' + "".join(cards) + '</div>'


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
    category = metadata.get(slug, {}).get("category", "Reference")
    table = re.search(r"<table>.*?</table>", content, flags=re.S)
    if table and any(kind + " data" in table[0] for kind in ("Skill", "Race", "NPC", "Boss")):
        data = table[0]
        category = metadata.get(slug, {}).get("category", category)
        content = content[:table.start()] + content[table.end():]
        portrait = ""
        if category == "Skills":
            match = re.search(r'<p><img [^>]*src="../assets/skill/[^>]+></p>', content)
            if match:
                portrait = '<div class="infobox-portrait">' + match[0] + '</div>'
                content = content[:match.start()] + content[match.end():]
        elif category in ("Races", "Convergent races"):
            match = re.search(r'<p><img [^>]*src="../assets/race/[^>]+></p>', content)
            if match:
                portrait = '<div class="infobox-portrait">' + match[0] + '</div>'
                content = content[:match.start()] + content[match.end():]
            else:
                portrait = f'<div class="infobox-portrait"><img src="../assets/race/{slug.lower().replace("-", "_")}.png" width="128" height="128" alt="{html.escape(title)} race icon"></div>'
        elif category == "Bosses":
            match = re.search(r'<p><img [^>]*src="../assets/race/[^>]+></p>', content)
            if match:
                portrait = '<div class="infobox-portrait">' + match[0] + '</div>'
                content = content[:match.start()] + content[match.end():]
        elif category == "Otherworlders":
            portrait = f'<div class="infobox-portrait"><div class="npc-face {slug.lower()}" role="img" aria-label="{title} in-game face"></div></div>'
        infobox = data_infobox(title, portrait, data, category)
    nav = ""
    for group, entries in groups:
        nav += f'<section><h2>{group}</h2>' + ''.join(f'<a href="{link(key, prefix)}"' + (' aria-current="page"' if key == slug else '') + f'>{label}</a>' for key, label in entries) + '</section>'
    toc = '<details class="contents" open><summary>Contents</summary>' + renderer.toc + '</details>' if renderer.toc_tokens and not home else ""
    if home:
        content = portal(content)
    # Put the article introduction before its contents, as on skill/race pages.
    introduction = ""
    if not home:
        lead = re.match(r"\s*(<p>.*?</p>)", content, re.S)
        if lead:
            introduction = '<div class="article-intro">' + lead[1] + '</div>'
            content = content[lead.end():]
        content = re.sub(r'<p>(\[(?:Active|Passive)[^]]*\])', r'<p class="ability-label">\1', content)
        content = re.sub(r"(<table>.*?</table>)", lambda m: '<div class="article-table' + (' ability-data' if 'Ability information' in m[1] else '') + '">' + m[1] + '</div>', content, flags=re.S)
    if category == "Skills":
        related_groups = [("Skill directories", ["Skills", "Unique-Skills", "Ultimate-Skills", "Godly", "Aura-Regeneration"]), ("Related abilities", ["Abilities", "Damage-Types"]), ("Extra Skills", ["Hunters-Recovery", "Apex-Renewal", "Sovereign-Rebirth"])]
    elif category in ("Races", "Convergent races"):
        related_groups = [("Race directory", ["Races"]), ("Race systems", ["Duo-Races", "Convergent-Awakening"]), ("Human / Hunter", ["Evolution", "Awakened-Hunter", "Apex-Hunter", "Sovereign-Hunter"])]
    elif category == "Bosses":
        related_groups = [("Myth bosses", ["Bosses", "Ananke", "Eschaton"]), ("Boss lineage and arts", ["Origin-Sovereign", "Demiurge", "Eclipse-Seraph", "Moira", "Thoth"]), ("Combat reference", ["Abilities", "Damage-Types", "Commands"])]
    elif category == "Otherworlders":
        related_groups = [("Otherworlders", ["Otherworlders", "Alma", "Gemma"]), ("Race", ["Awakened-Hunter"])]
    else:
        related_groups = [("Reference", ["Getting-Started", "Skills", "Abilities", "Damage-Types", "Races", "Otherworlders", "Mechanics", "Commands"])]
    if category == "Convergent races":
        related_groups = [("Race systems", ["Races", "Duo-Races", "Convergent-Awakening"])]
    related = ""
    for group, slugs in related_groups:
        anchors = "".join(f'<a href="{link(key, prefix)}"' + (' aria-current="page"' if key == slug else '') + f'>{html.escape(titles[key])}</a>' for key in slugs)
        related += f'<div class="navbox-row"><strong>{group}</strong><div>{anchors}</div></div>'
    if category == "Convergent races":
        ancestry = " · ".join(f'<a href="{link("Races", prefix)}#{family}-ancestry">{family.title()} ancestry</a>' for family in slug.removeprefix("Fusion-").split("-"))
        related += '<div class="navbox-row"><strong>Ancestries</strong><div>' + ancestry + '</div></div>'
    navbox = f'<nav class="related-box" aria-label="Related {category.lower()}"><strong>Tensura Myths · {category}</strong>{related}</nav>' if not home else ""
    parent = "Skills" if category == "Skills" else "Races" if category in ("Races", "Convergent races") else "Otherworlders" if category == "Otherworlders" else "Bosses" if category == "Bosses" else "Mechanics"
    crumb = f'<a href="{link(parent, prefix)}">{category}</a>'
    if category == "Convergent races":
        crumb = f'<a href="{link("Races", prefix)}">Races</a> / <a href="{link("Convergent-Awakening", prefix)}">Convergent races</a>'
    tier = metadata.get(slug, {}).get("tier")
    if tier:
        tier_page = {"Unique": "Unique-Skills", "Ultimate": "Ultimate-Skills", "Godly": "Godly", "Extra": "Aura-Regeneration"}[tier]
        crumb += f' / <a href="{link(tier_page, prefix)}">{tier} Skills</a>'
    category_link = crumb if not home else 'Main Page'
    page = f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="description" content="{html.escape(title)} — the unofficial Tensura Myths addon reference.">
<title>{html.escape(title)} — Tensura Myths Wiki</title><link rel="stylesheet" href="{skin}wiki.css">
<script defer src="{skin}search-index.js"></script><script defer src="{skin}search.js"></script></head>
<body data-root="{prefix}"><a class="skip" href="#article">Skip to article</a>
<div class="global-bar"><span>TENSURA MYTHS</span><div><a href="https://tensura.wiki.gg/wiki/Tensura:_Reincarnated_Wiki">Base mod wiki</a><a href="https://github.com/Bithio999/MC">GitHub</a></div></div>
<header class="masthead"><a class="brand" href="{prefix}index.html"><span class="brand-mark" aria-hidden="true">M</span><span>TENSURA MYTHS<small>Tensura: Reincarnated addon wiki</small></span></a>
<div class="search"><label for="wiki-search">Search Tensura Myths Wiki</label><input id="wiki-search" type="search" placeholder="Search skills, races, characters…" autocomplete="off" role="combobox" aria-autocomplete="list" aria-controls="search-results" aria-expanded="false"><div id="search-results" role="listbox" aria-label="Search results" hidden></div><span id="search-status" class="sr-only" aria-live="polite"></span></div></header>
<div class="layout"><button id="navigation-toggle" aria-controls="wiki-navigation" aria-expanded="false">☰ Wiki navigation</button><nav id="wiki-navigation" class="sidebar" aria-label="Wiki navigation">{nav}<div class="sidebar-version">Tensura Myths <strong>0.32.0</strong><br>Minecraft 1.21.1 · NeoForge<br><span>Offline reference edition</span></div></nav>
<main id="article" class="{'main-page' if home else 'article-page'}"><div class="page-tabs"><span aria-current="page">{'Main Page' if home else 'Article'}</span><a href="{'README.md' if home else '../pages/' + source.name}">Read Markdown</a><a href="https://github.com/Bithio999/MC/blob/main/docs/CHANGELOG.md">Changelog</a></div>
<div class="article-heading"><div class="breadcrumbs"><a href="{prefix}index.html">Tensura Myths Wiki</a>{' / ' + crumb if not home else ''}</div><h1>{html.escape(title)}</h1><p class="subtitle">From the unofficial Tensura Myths Wiki</p></div>
<div class="article-content">{introduction}{infobox}{toc}{content}</div>{navbox}
<div class="categories">Categories: <a href="{prefix}index.html">Tensura Myths</a> · {category_link}</div>
<footer>Unofficial fan addon. Original documentation and portraits. Not affiliated with Capcom or ManasMods.<br>Tensura Myths 0.32.0 · Offline edition · No external services required.</footer></main></div></body></html>'''
    destination.write_text(page)


render(WIKI / "README.md", WIKI / "index.html", True)
for item in catalog:
    source = WIKI / "pages" / (item["slug"] + ".md")
    render(source, SITE / (item["slug"] + ".html"))
    item["keywords"] = source.read_text().split("## Usage")[0].split("## Racial Ability")[0] + " " + " ".join(a["name"] + " " + a["damage"] for a in abilities if a["skill"] == item["slug"])
search_entries = list(catalog) + [
    {"slug": a["skill"], "anchor": a["anchor"], "title": a["name"] + " — " + titles[a["skill"]],
     "keywords": a["tier"] + " " + a["kind"] + " " + a["damage"]}
    for a in abilities
]
(SITE / "search-index.js").write_text("window.WIKI_PAGES = " + json.dumps(search_entries) + ";\n")
subprocess.run([sys.executable, str(ROOT / "scripts/export-github-wiki.py")], check=True)
# Fixed timestamps and sorted entries keep this documentation bundle reproducible.
archive = ROOT / "releases/tensura-myths-wiki-0.32.0.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as bundle:
    for path in sorted(WIKI.rglob("*")):
        if path.is_file():
            entry = zipfile.ZipInfo(path.relative_to(WIKI).as_posix(), (2026, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = 0o644 << 16
            bundle.writestr(entry, path.read_bytes())
print(f"Built Main Page, {len(catalog)} articles, and {archive.name}.")
