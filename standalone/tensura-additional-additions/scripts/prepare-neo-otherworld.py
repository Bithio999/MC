#!/usr/bin/env python3
"""Prepare an isolated, hash-verified Neo Otherworld 1.1.3a compatibility workspace.

Third-party jars/overrides are downloaded for local testing and never redistributed.
"""
import argparse, concurrent.futures, hashlib, json, shutil, urllib.request, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
LOCK=json.loads((ROOT/'docs/compatibility/tensura-neo-otherworld-1.1.3a.lock.json').read_text())
SERVER_CLIENT_ONLY={'fancytoasts','ymktn','drippyloadingscreen','smart_particles','particle_effects'}
GRADLE_CORE={'tensura','manascore','architectury'}

def download(url,path,expected):
    if path.exists() and hashlib.sha256(path.read_bytes()).hexdigest()==expected:return
    path.parent.mkdir(parents=True,exist_ok=True)
    temporary=path.with_suffix(path.suffix+'.part')
    with urllib.request.urlopen(url,timeout=90) as response,temporary.open('wb') as output:
        shutil.copyfileobj(response,output)
    if hashlib.sha256(temporary.read_bytes()).hexdigest()!=expected:
        temporary.unlink();raise ValueError('SHA-256 mismatch: '+path.name)
    temporary.replace(path)

def prepare(directory,world=None):
    directory=directory.resolve();marker=directory/'heavenlyrestriction-compat.json'
    if directory.exists() and any(directory.iterdir()) and not marker.exists():
        raise ValueError('Choose an empty test directory; an existing instance is never overwritten.')
    directory.mkdir(parents=True,exist_ok=True)
    marker.write_text(json.dumps({'packFileId':LOCK['pack']['fileId'],'purpose':'Disposable compatibility review'},indent=2)+'\n')
    archive=directory/'pack.zip';download(LOCK['pack']['downloadUrl'],archive,LOCK['pack']['sha256'])
    with zipfile.ZipFile(archive) as z:
        for name in z.namelist():
            if not name.startswith('overrides/') or name.endswith('/'):continue
            relative=Path(name).relative_to('overrides')
            if relative.is_absolute() or '..' in relative.parts:raise ValueError('Unsafe archive path')
            for instance in ['client','gametest']:
                destination=directory/instance/relative;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(z.read(name))
    def fetch(mod):
        path=directory/'downloaded-mods'/(mod['slug']+'-'+str(mod['fileID'])+'.jar')
        download(mod['url'],path,mod['sha256']);return mod,path
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        for mod,path in executor.map(fetch,LOCK['mods']):
            ids={m['id'] for m in mod['mods']}
            if ids & GRADLE_CORE:continue
            for instance in ['client','gametest']:
                if instance=='gametest' and ids & SERVER_CLIENT_ONLY:continue
                destination=directory/instance/'mods'/path.name;destination.parent.mkdir(parents=True,exist_ok=True)
                if not destination.exists():shutil.copyfile(path,destination)
    if world:
        shutil.copytree(world,directory/'client/saves/Restriction Validation',dirs_exist_ok=True)
    (directory/'client/options.txt').write_text('lang:en_us\nguiScale:2\nrenderDistance:2\nsimulationDistance:2\nmaxFps:30\n')
    print('Prepared',len(LOCK['mods']),'pinned projects in',directory)
    print('Core Tensura/ManasCore/Architectury are supplied once by the Gradle pack profile.')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory',type=Path)
    parser.add_argument('--world',type=Path,help='Optional disposable flat-world copy for the client review.')
    args=parser.parse_args();prepare(args.directory,args.world)
