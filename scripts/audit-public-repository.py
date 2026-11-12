#!/usr/bin/env python3
"""Audit public files and optional reachable history without printing matched values."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PUBLIC_NAME = b'Tensura Addons Contributors'
PUBLIC_EMAIL = b'contributors@example.invalid'
TEXT_SUFFIXES = {'.md', '.txt', '.java', '.json', '.toml', '.properties', '.gradle',
                 '.py', '.js', '.css', '.html', '.xml', '.svg', '.yml', '.yaml', '.sh'}
CREDENTIAL = re.compile(rb'gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}'
                        rb'|AKIA[A-Z0-9]{16}|-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----')
EMAIL = re.compile(r'(?<![\w.+%-])[\w.+%-]+@[\w.-]+\.[A-Za-z]{2,}(?![\w.-])')
PRIVATE_PATH = re.compile(r'(?:[A-Z]:\\Users\\|/Users/)[A-Za-z0-9][A-Za-z0-9_.-]*')


def git(*args: str, data: bytes | None = None) -> bytes:
    return subprocess.check_output(['git', *args], cwd=ROOT, input=data)


def inspect(data: bytes, path: str, issues: set[tuple[str, str]], literals: list[bytes]) -> None:
    if any(value in data for value in literals):
        issues.add((path, 'private identity literal'))
    if CREDENTIAL.search(data):
        issues.add((path, 'credential-like value'))
    if b'\x00' in data:
        return
    try:
        text = data.decode('utf-8')
    except UnicodeError:
        return
    for email in EMAIL.findall(text):
        domain = email.rsplit('@', 1)[1].lower()
        if not (domain.endswith('.invalid') or domain in {'example.com', 'example.org',
                'example.net', 'users.noreply.github.com', 'noreply.github.com'}):
            issues.add((path, 'review email address'))
    if PRIVATE_PATH.search(text):
        issues.add((path, 'personal home-directory path'))


def tree(issues: set[tuple[str, str]], literals: list[bytes]) -> int:
    count = 0
    for raw in git('ls-files', '-z').split(b'\x00'):
        if not raw:
            continue
        relative = raw.decode()
        path = ROOT / relative
        if not path.is_file():  # Supports reviewing staged removals.
            continue
        count += 1
        if path.name in {'.env', 'credentials.json', 'launcher_accounts.json',
                         'launcher_profiles.json', 'usercache.json', 'server.properties'}:
            issues.add((relative, 'private local-state filename'))
        if path.suffix.lower() in {'.zip', '.jar'}:
            with zipfile.ZipFile(path) as archive:
                for entry in archive.infolist():
                    if Path(entry.filename).suffix.lower() in TEXT_SUFFIXES:
                        inspect(archive.read(entry), relative + '!' + entry.filename,
                                issues, literals)
        elif path.suffix.lower() in TEXT_SUFFIXES or path.name.startswith('.git'):
            inspect(path.read_bytes(), relative, issues, literals)
    return count


def history(issues: set[tuple[str, str]], literals: list[bytes]) -> int:
    identities = git('log', '--all', '--format=%an%x00%ae%x00%cn%x00%ce').splitlines()
    for fields in (row.split(b'\x00') for row in identities):
        if fields != [PUBLIC_NAME, PUBLIC_EMAIL, PUBLIC_NAME, PUBLIC_EMAIL]:
            issues.add(('Git history', 'non-generic author or committer identity'))
    objects = {}
    for row in git('rev-list', '--objects', '--all').splitlines():
        oid, _, name = row.partition(b' ')
        objects[oid] = name
    rows = git('cat-file', '--batch-check=%(objectname) %(objecttype) %(objectsize)',
               data=b'\n'.join(objects) + b'\n').splitlines()
    candidates = []
    for row in rows:
        oid, kind, size = row.split()
        if kind == b'blob' and int(size) < 16 * 1024 * 1024:
            name = objects[oid].decode(errors='replace')
            if Path(name).suffix.lower() in TEXT_SUFFIXES:
                candidates.append((oid, name))
    process = subprocess.Popen(['git', 'cat-file', '--batch'], cwd=ROOT,
                               stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    try:
        for oid, name in candidates:
            process.stdin.write(oid + b'\n')
            process.stdin.flush()
            header = process.stdout.readline().split()
            data = process.stdout.read(int(header[2]))
            process.stdout.read(1)
            inspect(data, 'history:' + name, issues, literals)
    finally:
        process.stdin.close()
        process.wait()
    return len(candidates)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--history', action='store_true')
    parser.add_argument('--private-patterns-file', type=Path,
                        help='Local JSON list of identity strings; keep it outside Git.')
    args = parser.parse_args()
    literals = ([value.encode() for value in json.loads(args.private_patterns_file.read_text())]
                if args.private_patterns_file else [])
    issues: set[tuple[str, str]] = set()
    files = tree(issues, literals)
    blobs = history(issues, literals) if args.history else 0
    for path, category in sorted(issues):
        print(f'{path}: {category}')
    print(f'Checked {files} tracked files and {blobs} historical text blobs; '
          f'{len(issues)} findings. Matched values are never printed.')
    return 1 if issues else 0


if __name__ == '__main__':
    sys.exit(main())
