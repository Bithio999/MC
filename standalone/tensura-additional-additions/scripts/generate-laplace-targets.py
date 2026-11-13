#!/usr/bin/env python3
"""Build optional, stack-neutral parameter hook targets from the two supported native APIs."""
from pathlib import Path
import zipfile,json,re,struct,os
root=Path(__file__).resolve().parents[1]
names=set()
def class_flags(data):
 pos=10;index=1;count=struct.unpack_from('>H',data,8)[0]
 while index<count:
  tag=data[pos];pos+=1
  if tag==1:size=struct.unpack_from('>H',data,pos)[0];pos+=2+size
  elif tag in (3,4,9,10,11,12,17,18):pos+=4
  elif tag in (5,6):pos+=8;index+=1
  elif tag in (7,8,16,19,20):pos+=2
  elif tag==15:pos+=3
  else:raise ValueError(tag)
  index+=1
 return struct.unpack_from('>H',data,pos)[0]

cache=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/modules-2/files-2.1/curse.maven/tensura-reincarnated-643695'
for version in ('8793360','9289888'):
 for jar in (cache/version).rglob('*.jar'):
  with zipfile.ZipFile(jar) as z:
   names.update(n[:-6].replace('/','.') for n in z.namelist() if n.endswith('.class') and n.startswith('io/github/manasmods/tensura/') and not n.startswith('io/github/manasmods/tensura/config/') and b'io/github/manasmods/tensura/config/ability/' in z.read(n) and not class_flags(z.read(n))&0x200)
for path in (root/'src/main/java/dev/heavenlyrestriction').rglob('*.java'):
 rel=path.relative_to(root/'src/main/java').as_posix()
 if not re.search(r'\binterface\s+'+re.escape(path.stem)+r'\b',path.read_text()) and not any('/'+s+'/' in '/'+rel for s in ('mixin','client','laplace','guide','cartography','worldhop')):names.add(rel[:-5].replace('/','.'))
path=root/'src/main/java/dev/heavenlyrestriction/mixin/ParameterReadMixin.java'
path.write_text('''package dev.heavenlyrestriction.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
/** Generated target inventory; optional classes are ignored when absent from a native API. */
@Pseudo
@Mixin(targets={\n'''+',\n'.join('  '+json.dumps(n) for n in sorted(names))+'''},remap=false)
public abstract class ParameterReadMixin {}
''')
print(f'Generated {len(names)} optional parameter-read targets.')
