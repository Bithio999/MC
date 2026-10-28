#!/usr/bin/env python3
"""Export canonical docs as GitHub Wiki pages; optionally publish an existing wiki.

No GitHub settings are changed. Publishing requires an enabled, initialized wiki.
Only generated pages are copied; unrelated pages and Git history are preserved.
"""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
WIKI = ROOT / 'wiki'
REPO = 'https://github.com/Bithio999/MC'
WIKI_URL = REPO + '/wiki'
CATALOG = json.loads((WIKI / 'catalog.json').read_text())


def export(output):
    output.mkdir(parents=True, exist_ok=True)
    sources = [WIKI / 'README.md'] + [WIKI / 'pages' / (p['slug'] + '.md') for p in CATALOG]
    pages = {source.resolve(): 'Home' if source.name == 'README.md' else source.stem for source in sources}

    def convert(source):
        def url(value):
            parsed = urlsplit(value)
            if parsed.scheme or parsed.netloc or value.startswith('#'):
                return value
            target = (source.parent / parsed.path).resolve()
            fragment = '#' + parsed.fragment if parsed.fragment else ''
            if target in pages:
                return WIKI_URL + '/' + pages[target] + fragment
            relative = target.relative_to(ROOT).as_posix()
            if not target.is_file():
                raise ValueError(f'{source.name}: missing local target {value}')
            return REPO + '/blob/main/' + relative + ('?raw=true' if target.suffix == '.png' else '') + fragment
        text = source.read_text()
        if source.name == 'README.md':
            # The Wiki page describes reading and contributing; publishing mechanics
            # remain in the repository's authoring guide.
            text = text.split('## Open the visual edition')[0]
            text += '\n[Download the styled offline edition](' + REPO + '/blob/main/releases/tensura-myths-wiki-0.34.7.zip)\n'
        text = re.sub(r'(!?\[[^\]]*\]\()([^\s)]+)(\))', lambda m: m[1] + url(m[2]) + m[3], text)
        text = re.sub(r'(src=")([^"]+)(")', lambda m: m[1] + url(m[2]) + m[3], text)
        text = text.replace('improve the Markdown articles in `wiki/pages`', 'improve the [Markdown articles](' + REPO + '/tree/main/wiki/pages)')
        return text

    for source in sources:
        (output / (pages[source.resolve()] + '.md')).write_text(convert(source))
    sidebar = '# Tensura Myths\n\n[Home](' + WIKI_URL + ')\n\n'
    for heading, entries in json.loads((WIKI / 'navigation.json').read_text()):
        sidebar += '### ' + heading + '\n\n'
        for slug, label in entries:
            if not slug:continue
            sidebar += f'- [{label}]({WIKI_URL}/{slug})\n'
        sidebar += '\n'
    (output / '_Sidebar.md').write_text(sidebar)
    (output / '_Footer.md').write_text(f'**Tensura Myths 0.34.7** · Minecraft 1.21.1 · NeoForge\n\n[Home]({WIKI_URL}) · [Source]({REPO}) · [Base mod wiki](https://tensura.wiki.gg/wiki/Tensura:_Reincarnated_Wiki)\n\nUnofficial fan addon. Original documentation and portraits. Not affiliated with Capcom or ManasMods.\n')
    print(f'Exported {len(sources)} GitHub Wiki pages, sidebar, and footer to {output.relative_to(ROOT) if output.is_relative_to(ROOT) else output}.')


def publish(output):
    # Credentials come from Git's configured helper; never put tokens in URLs.
    with tempfile.TemporaryDirectory(prefix='wildhunt-wiki-') as directory:
        checkout = Path(directory) / 'wiki'
        result = subprocess.run(['git', 'clone', REPO + '.wiki.git', str(checkout)], capture_output=True, text=True)
        if result.returncode:
            raise SystemExit('GitHub Wiki is unavailable. Enable Wikis in repository Settings > General > Features, then create its first Home page. The integration may not have access to Wiki Git even with repository access. Exported pages remain ready in wiki/github/.')
        for page in sorted(output.glob('*.md')):
            shutil.copyfile(page, checkout / page.name)
        files = [p.name for p in sorted(output.glob('*.md'))]
        subprocess.run(['git', '-C', str(checkout), 'add', '--', *files], check=True)
        if subprocess.run(['git', '-C', str(checkout), 'diff', '--cached', '--quiet']).returncode == 0:
            print('GitHub Wiki already matches the documentation.'); return
        subprocess.run(['git', '-C', str(checkout), '-c', 'user.name=Tensura Myths Documentation', '-c', 'user.email=docs@users.noreply.github.com', 'commit', '-m', 'Update Tensura Myths wiki documentation and navigation'], check=True)
        subprocess.run(['git', '-C', str(checkout), 'push', 'origin', 'HEAD'], check=True)
        print('Published ' + WIKI_URL)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publish', action='store_true', help='Commit and push the generated pages to the separate GitHub Wiki repository.')
    args = parser.parse_args()
    target = WIKI / 'github'
    export(target)
    if args.publish:
        publish(target)
