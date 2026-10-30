#!/usr/bin/env python3
"""Check canonical/HTML links, anchors, addon boundaries and offline media."""
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote,urlsplit
import json,re,sys
try:import markdown
except ImportError:raise SystemExit('Requires Markdown==3.7, like build-wiki.py')
ROOT=Path(__file__).resolve().parents[1];WIKI=ROOT/'wiki';catalog=json.loads((WIKI/'catalog.json').read_text());meta={e['slug']:e for e in catalog};errors=[]
class Document(HTMLParser):
    def __init__(self,text):self.ids=[];self.urls=[];super().__init__();self.feed(text)
    def handle_starttag(self,tag,attrs):
        attrs=dict(attrs)
        if 'id' in attrs:self.ids.append(attrs['id'])
        for attr in ['href','src','poster']:
            if attrs.get(attr):self.urls.append(attrs[attr])
cache={}
def read(path):
    path=path.resolve()
    if path not in cache:cache[path]=Document(markdown.markdown(path.read_text(),extensions=['tables','toc','fenced_code']) if path.suffix=='.md' else path.read_text())
    return cache[path]
files=[WIKI/'README.md',WIKI/'index.html']+[WIKI/'pages'/(e['slug']+'.md') for e in catalog]+[WIKI/'site'/(e['slug']+'.html') for e in catalog]
for source in files:
    if not source.is_file():errors.append(f'Missing page: {source}');continue
    doc=read(source)
    if len(doc.ids)!=len(set(doc.ids)):errors.append(f'{source.name}: duplicate IDs')
    for value in doc.urls:
        parsed=urlsplit(value)
        if parsed.scheme or parsed.netloc:continue
        target=(source.parent/unquote(parsed.path)).resolve() if parsed.path else source.resolve()
        if not target.exists():errors.append(f'{source.name}: missing {value}');continue
        if parsed.fragment and target.suffix in ['.md','.html'] and unquote(parsed.fragment) not in read(target).ids:errors.append(f'{source.name}: missing anchor {value}')
        if source.parent==WIKI/'pages' and target.parent==source.parent and target.stem in meta:
            if meta[source.stem]['mod']!=meta[target.stem]['mod']:errors.append(f'{source.name}: crosses into other addon: {value}')
for ability in json.loads((WIKI/'abilities.json').read_text()):
    page=WIKI/'pages'/(ability['skill']+'.md')
    if ability['anchor'] not in read(page).ids:errors.append('Ability anchor missing: '+ability['skill']+'#'+ability['anchor'])
    if meta[ability['skill']]['mod']!=ability.get('mod','myths'):errors.append('Ability in wrong addon: '+ability['name'])
    if not ability['damage']:errors.append('Damage classification missing: '+ability['name'])
for mod,groups in json.loads((WIKI/'navigation.json').read_text()).items():
    for _,entries in groups:
        for slug,_ in entries:
            if slug not in meta or meta[slug]['mod']!=mod:errors.append('Navigation crosses addon boundary: '+slug)
for error in sorted(set(errors)):print(error)
if errors:raise SystemExit(f'{len(set(errors))} wiki errors')
print(f'Checked {len(catalog)} articles, canonical/HTML links and anchors, addon boundaries, media and ability targets.')
