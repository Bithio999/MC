#!/usr/bin/env python3
"""Build the scoped addon wikis and their shared offline portal (Markdown==3.7)."""
from pathlib import Path
from urllib.parse import urlsplit
import html,json,re,shutil,subprocess,sys,zipfile,os
try:import markdown
except ImportError:raise SystemExit('Install the documentation renderer: python3 -m pip install Markdown==3.7')
ROOT=Path(__file__).resolve().parents[1];WIKI=ROOT/'wiki';SITE=WIKI/'site'
MODS={'myths':{'name':'Tensura Myths','version':'0.36.1','home':'Tensura-Myths','mark':'M'},'additions':{'name':'TensuraAdditionalAdditions','version':'1.9.2','home':'TensuraAdditionalAdditions','mark':'A'},'retired_addon':{'name':'Tensura retired_addon','version':'1.2.0','home':'Tensura-retired_addon','mark':'K'}}
subprocess.run([sys.executable,str(ROOT/'scripts/build-ability-index.py')],check=True)
catalog=json.loads((WIKI/'catalog.json').read_text());metadata={e['slug']:e for e in catalog};navigation=json.loads((WIKI/'navigation.json').read_text());abilities=json.loads((WIKI/'abilities.json').read_text())
SITE.mkdir(exist_ok=True)
for source in (ROOT/'src/main/resources/assets/wildhunt/textures').rglob('*.png'):
    relative=source.relative_to(ROOT/'src/main/resources/assets/wildhunt/textures')
    if relative.parts[0] not in ['skill','race','battlewill','item','mob_effect'] and relative.parts[:2]!=('entity','otherworlder'):continue
    destination=WIKI/'assets'/relative;destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,destination)
sources=[WIKI/'README.md']+[WIKI/'pages'/(e['slug']+'.md') for e in catalog]
routes={source.resolve():WIKI/'index.html' if source.name=='README.md' else SITE/(source.stem+'.html') for source in sources}
for obsolete in SITE.glob('*.html'):
    if obsolete.stem not in metadata:obsolete.unlink()
def relative(target,destination):return Path(os.path.relpath(target,destination.parent)).as_posix()
def route(slug,destination):return relative(SITE/(slug+'.html'),destination)
def url(value,source,destination):
    parsed=urlsplit(html.unescape(value))
    if parsed.scheme or parsed.netloc or value.startswith('#'):return value
    target=(source.parent/parsed.path).resolve();fragment='#'+parsed.fragment if parsed.fragment else ''
    if target in routes:return relative(routes[target],destination)+fragment
    if target.is_relative_to(WIKI) and target.is_file():return relative(target,destination)+fragment
    if not target.is_relative_to(ROOT) or not (target.is_file() or target==ROOT/'releases/tensura-addon-wikis.zip'):raise ValueError(f'{source.name}: missing target {value}')
    path=target.relative_to(ROOT).as_posix();raw=target.suffix.lower() in ['.jar','.zip','.mp4','.png','.gif','.webp']
    return ('https://github.com/Bithio999/MC/raw/refs/heads/main/' if raw else 'https://github.com/Bithio999/MC/blob/main/')+path+fragment

def infobox(content,title,category):
    match=re.search(r'<table>.*?</table>',content,re.S)
    if not match or not re.search(r'<th>(?:Skill|Race|Boss|NPC|Engraving|Weapon) data</th>',match[0]):return '',content
    data=match[0];content=content[:match.start()]+content[match.end():];portrait=''
    image=re.search(r'<p><img [^>]*src="[^"]*assets/[^>]+></p>',content)
    if image:portrait='<div class="infobox-portrait">'+image[0]+'</div>';content=content[:image.start()]+content[image.end():]
    if not portrait and title in ['Alma','Gemma']:portrait='<div class="infobox-portrait"><div class="npc-face '+title.lower()+'" role="img" aria-label="'+title+'"></div></div>'
    rows=''.join('<tr><th scope="row">'+k+'</th><td>'+v+'</td></tr>' for k,v in re.findall(r'<tr>\s*<td>(.*?)</td>\s*<td>(.*?)</td>\s*</tr>',data,re.S))
    box=f'<aside class="infobox"><h2>{html.escape(title)}</h2>{portrait}<section class="infobox-section"><h3>{html.escape(category)}</h3><table><tbody>{rows}</tbody></table></section></aside>'
    return box,content

def render(source,destination):
    home=source.name=='README.md';slug='' if home else source.stem;item=metadata.get(slug,{});mod=item.get('mod','all');config=MODS.get(mod);label=config['name'] if config else 'Tensura addon wikis'
    text=source.read_text();title,body=text.split('\n',1);title=title.removeprefix('# ').strip()
    # Breadcrumbs belong to the site frame; they remain in canonical Markdown for GitHub readers.
    body=re.sub(r'^\s*\[All wikis\].*?\n','',body,count=1)
    renderer=markdown.Markdown(extensions=['tables','fenced_code','toc']);content=renderer.convert(body)
    content=re.sub(r'(href|src)="([^"]+)"',lambda m:m[1]+'="'+html.escape(url(m[2],source,destination),quote=True)+'"',content)
    # Keep videos local and playable offline; retain a plain link for download/accessibility.
    def video(m):
        href,name=m.groups()
        posters={'fusion.mp4':'fusion-poster.png','aion-singularity-barrage.mp4':'aion-singularity-formation.png','astral-library.mp4':'astral-library.png','eschaton-aion-anime-preview.mp4':'eschaton-apocalypse.png','ananke-last-dawn.mp4':'ananke-last-dawn.png','counter-eschaton-8m.mp4':'counter-preview-poster.png','world-hop.mp4':'world-hop-preview-poster.png'}
        poster=posters.get(Path(urlsplit(href).path).name)
        attribute=' poster="'+relative(WIKI/'media'/poster,destination)+'"' if poster else ''
        return '<figure class="video"><video controls preload="none" playsinline src="'+href+'"'+attribute+' aria-label="'+html.escape(re.sub('<[^>]+>','',name),quote=True)+'"></video><figcaption><a href="'+href+'">'+name+'</a></figcaption></figure>'

    content=re.sub(r'<p><a href="([^"]+\.mp4)">(.*?)</a></p>',video,content,flags=re.S)
    box,content=infobox(content,title,item.get('category','Reference'))
    intro=''
    lead=re.match(r'\s*(<p>.*?</p>)',content,re.S)
    if lead:intro=lead[1];content=content[lead.end():]
    content=re.sub(r'(<table>.*?</table>)',r'<div class="article-table">\1</div>',content,flags=re.S)
    nav=''
    scopes=[mod] if mod in MODS else list(MODS)
    for scope in scopes:
        if not config:nav+='<h2 class="nav-mod">'+MODS[scope]['name']+'</h2>'
        for heading,entries in (navigation[scope] if config else [('Browse',navigation[scope][0][1]+navigation[scope][1][1][:1])]):
            nav+='<section><h2>'+html.escape(heading)+'</h2>'+''.join('<a href="'+route(key,destination)+'"'+(' aria-current="page"' if key==slug else '')+'>'+html.escape(name)+'</a>' for key,name in entries)+'</section>'
    switches='<a href="'+relative(WIKI/'index.html',destination)+'"'+(' aria-current="page"' if home else '')+'>All wikis</a>'
    for scope,info in MODS.items():switches+='<a href="'+route(info['home'],destination)+'"'+(' aria-current="page"' if scope==mod else '')+'>'+html.escape(info['name'])+'</a>'
    if home:
        cards=''
        for scope,info in MODS.items():
            image='retired_addon-wooden-king.png' if scope=='retired_addon' else 'eschaton-form-one.png' if scope=='myths' else 'additions-world-select-1.5.0.png';desc='Wooden Monkey King, forest arts and Tree Flight.' if scope=='retired_addon' else 'Skills, races, Godly ascension and Myth bosses.' if scope=='myths' else 'Heavenly Restriction, Battlewills and world travel.'
            cards+='<a class="mod-card '+scope+'" href="'+route(info['home'],destination)+'"><img src="'+relative(WIKI/'media'/image,destination)+'" alt="'+html.escape(info['name'])+'"><div><small>VERSION '+info['version']+'</small><h2>'+info['name']+'</h2><p>'+desc+'</p><strong>Open this wiki →</strong></div></a>'
        # The visual cards replace the Markdown comparison table.
        content=re.sub(r'<div class="article-table"><table>.*?</table></div>', '<div class="mod-grid">'+cards+'</div>',content,count=1,flags=re.S)
    toc='<details class="contents"><summary>On this page</summary>'+renderer.toc+'</details>' if len(renderer.toc_tokens)>2 and not home else ''
    version=config['version'] if config else 'Myths 0.36.1 · Additions 1.9.2 · retired_addon 1.2.0';mark=config['mark'] if config else 'T';prefix=relative(WIKI,destination)+'/'
    title_id=markdown.extensions.toc.slugify(title,'-')
    title_attr='' if re.search(r'id="'+re.escape(title_id)+r'"',content) else ' id="'+html.escape(title_id)+'"'
    article_home=route(config['home'],destination) if config else relative(WIKI/'index.html',destination)
    crumbs='<a href="'+relative(WIKI/'index.html',destination)+'">All wikis</a>'+(' / <a href="'+article_home+'">'+label+'</a>' if config else '')
    page=f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="description" content="{html.escape(title,quote=True)} — {html.escape(label,quote=True)} gameplay reference.">
<title>{html.escape(title)} — {html.escape(label)}</title><link rel="stylesheet" href="{relative(SITE/'wiki.css',destination)}">
<script defer src="{relative(SITE/'search-index.js',destination)}"></script><script defer src="{relative(SITE/'search.js',destination)}"></script></head>
<body data-root="{prefix}" data-mod="{mod}"><a class="skip" href="#article">Skip to article</a>
<div class="global-bar"><span>TENSURA ADDON WIKIS</span><div><a href="https://tensura.wiki.gg/wiki/Tensura:_Reincarnated_Wiki">Base mod wiki</a><a href="https://github.com/Bithio999/MC">GitHub</a></div></div>
<header class="masthead"><a class="brand" href="{article_home}"><span class="brand-mark" aria-hidden="true">{mark}</span><span>{label}<small>{version} · Minecraft 1.21.1 · NeoForge</small></span></a>
<div class="search"><label for="wiki-search">Search {label}</label><input id="wiki-search" type="search" placeholder="Search names, abilities or requirements…" autocomplete="off" role="combobox" aria-autocomplete="list" aria-controls="search-results" aria-expanded="false"><div id="search-results" role="listbox" hidden></div><span id="search-status" class="sr-only" aria-live="polite"></span></div></header>
<nav class="mod-switch" aria-label="Choose addon">{switches}</nav>
<div class="layout"><button id="navigation-toggle" aria-controls="wiki-navigation" aria-expanded="false">☰ Wiki navigation</button><nav id="wiki-navigation" class="sidebar" aria-label="{html.escape(label)} navigation">{nav}</nav>
<main id="article" class="{'main-page' if home else 'article-page'}"><div class="page-tabs"><span aria-current="page">{'Choose addon' if home else label}</span><a href="{relative(source,destination)}">Read Markdown</a></div><div class="article-heading"><div class="breadcrumbs">{crumbs}</div><h1{title_attr}>{html.escape(title)}</h1><p class="subtitle">{'Choose the addon you play.' if home else label+' · '+item.get('category','Reference')}</p></div>
<div class="article-content">{intro}{box}{toc}{content}</div><footer>Unofficial addon reference · Default gameplay values · Pictures and videos illustrate their recorded versions.<br>Original addon artwork. Not affiliated with ManasMods or Capcom.</footer></main></div></body></html>'''
    destination.write_text(page)

for source,destination in routes.items():render(source,destination)
search=[]
for item in catalog:
    if item.get('alias'):continue
    search.append({**item,'keywords':(WIKI/'pages'/(item['slug']+'.md')).read_text()})
for a in abilities:search.append({'slug':a['skill'],'mod':a.get('mod','myths'),'anchor':a['anchor'],'title':a['name']+' — '+metadata[a['skill']]['title'],'keywords':a['tier']+' '+a['kind']+' '+a['damage']})
(SITE/'search-index.js').write_text('window.WIKI_PAGES = '+json.dumps(search,ensure_ascii=False)+';\n')
subprocess.run([sys.executable,str(ROOT/'scripts/export-github-wiki.py')],check=True)
# Include the media actually used by entries. Unlinked historical review renders stay in Git.
used_media=set()
for page in [WIKI/'index.html']+list(SITE.glob('*.html')):
    for value in re.findall(r'(?:href|src|poster)="([^"]+)"',page.read_text()):
        parsed=urlsplit(html.unescape(value))
        if not parsed.scheme and not parsed.netloc:
            target=(page.parent/parsed.path).resolve()
            if target.is_relative_to(WIKI/'media'):used_media.add(target)
archive=ROOT/'releases/tensura-addon-wikis.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as bundle:
    for path in sorted(WIKI.rglob('*')):
        if not path.is_file():continue
        if path.is_relative_to(WIKI/'media') and path.resolve() not in used_media:continue
        entry=zipfile.ZipInfo(path.relative_to(WIKI).as_posix(),(2026,1,1,0,0,0));entry.compress_type=zipfile.ZIP_DEFLATED;entry.external_attr=0o644<<16;bundle.writestr(entry,path.read_bytes())
print(f'Built shared portal, {len(catalog)} scoped articles, {len(abilities)} ability links and {archive.name}.')
