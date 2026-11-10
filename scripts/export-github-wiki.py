#!/usr/bin/env python3
"""Export addon sections to GitHub Wiki Markdown; optionally publish an initialized wiki."""
import argparse,json,re,shutil,subprocess,tempfile
from pathlib import Path
from urllib.parse import urlsplit
ROOT=Path(__file__).resolve().parents[1];WIKI=ROOT/'wiki';REPO='https://github.com/Bithio999/MC';WIKI_URL=REPO+'/wiki'
MODS={'myths':('Tensura Myths','0.36.1','Tensura-Myths'),'additions':('TensuraAdditionalAdditions','1.23.0','TensuraAdditionalAdditions'),'retired_addon':('Tensura retired_addon','1.2.0','Tensura-retired_addon')}
def export(output):
    catalog=json.loads((WIKI/'catalog.json').read_text());output.mkdir(parents=True,exist_ok=True)
    sources=[WIKI/'README.md']+[WIKI/'pages'/(e['slug']+'.md') for e in catalog];pages={s.resolve():'Home' if s.name=='README.md' else s.stem for s in sources}
    for stale in output.glob('*.md'):
        if stale.stem not in set(pages.values())|{'_Sidebar','_Footer'}:stale.unlink()
    for source in sources:
        def url(value):
            parsed=urlsplit(value)
            if parsed.scheme or parsed.netloc or value.startswith('#'):return value
            target=(source.parent/parsed.path).resolve();fragment='#'+parsed.fragment if parsed.fragment else ''
            if target in pages:return WIKI_URL+'/'+pages[target]+fragment
            if not target.is_relative_to(ROOT) or not (target.is_file() or target==ROOT/'releases/tensura-addon-wikis.zip'):raise ValueError(f'{source.name}: missing target {value}')
            path=target.relative_to(ROOT).as_posix();raw=target.suffix.lower() in ['.png','.webp','.gif','.mp4','.zip','.jar']
            return (REPO+'/raw/refs/heads/main/' if raw else REPO+'/blob/main/')+path+fragment
        text=re.sub(r'(!?\[[^\]]*\]\()([^\s)]+)(\))',lambda m:m[1]+url(m[2])+m[3],source.read_text())
        text=re.sub(r'(src=")([^"]+)(")',lambda m:m[1]+url(m[2])+m[3],text)
        (output/(pages[source.resolve()]+'.md')).write_text(text)
    sidebar='# Tensura addon wikis\n\n[Choose addon]('+WIKI_URL+')\n\n'
    nav=json.loads((WIKI/'navigation.json').read_text())
    for mod,(name,version,home) in MODS.items():
        sidebar+=f'## [{name}]({WIKI_URL}/{home})\n\n'
        for heading,entries in nav[mod]:
            sidebar+='**'+heading+'**\n\n'+'\n'.join(f'- [{label}]({WIKI_URL}/{slug})' for slug,label in entries)+'\n\n'
    (output/'_Sidebar.md').write_text(sidebar)
    (output/'_Footer.md').write_text(f'[All wikis]({WIKI_URL}) · [Tensura Myths 0.36.1]({WIKI_URL}/Tensura-Myths) · [TensuraAdditionalAdditions 1.23.0]({WIKI_URL}/TensuraAdditionalAdditions) · [Tensura retired_addon 1.2.0]({WIKI_URL}/Tensura-retired_addon)\n\nUnofficial addon reference · [Source]({REPO})\n')
    print(f'Exported {len(sources)} GitHub Wiki pages with separate addon sections.')
def publish(output):
    with tempfile.TemporaryDirectory(prefix='tensura-wikis-') as directory:
        checkout=Path(directory)/'wiki';result=subprocess.run(['git','clone',REPO+'.wiki.git',str(checkout)],capture_output=True,text=True)
        if result.returncode:raise SystemExit('The separate GitHub Wiki repository is inaccessible. Repository pages, offline edition and wiki/github export remain complete; no repository settings were changed.')
        for page in sorted(output.glob('*.md')):shutil.copyfile(page,checkout/page.name)
        files=[p.name for p in output.glob('*.md')];subprocess.run(['git','-C',str(checkout),'add','--',*files],check=True)
        if subprocess.run(['git','-C',str(checkout),'diff','--cached','--quiet']).returncode==0:print('GitHub Wiki already matches.');return
        subprocess.run(['git','-C',str(checkout),'-c','user.name=Tensura Documentation','-c','user.email=docs@users.noreply.github.com','commit','-m','Separate addon wikis and simplify gameplay entries'],check=True)
        subprocess.run(['git','-C',str(checkout),'push','origin','HEAD'],check=True)
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--publish',action='store_true');args=parser.parse_args();target=WIKI/'github';export(target)
    if args.publish:publish(target)
