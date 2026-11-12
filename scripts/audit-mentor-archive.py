#!/usr/bin/env python3
"""Audit the shipped NBT, including its complete envelope on a half-block grid."""
from pathlib import Path
from collections import Counter, deque
from array import array
import gzip, io, json, struct

ROOT=Path(__file__).resolve().parents[1]
path=ROOT/'standalone/tensura-additional-additions/src/main/resources/data/heavenlyrestriction/structure/mentor_archive.nbt'
stream=io.BytesIO(gzip.decompress(path.read_bytes()))
def num(fmt):return struct.unpack('>'+fmt,stream.read(struct.calcsize('>'+fmt)))[0]
def string():return stream.read(num('H')).decode()
def read(t):
 if t in [1,2,3,4,5,6]:return num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
 if t==8:return string()
 if t==9:
  k,n=num('B'),num('i');return [read(k) for _ in range(n)]
 if t==10:
  result={}
  while (k:=num('B')):name=string();result[name]=read(k)
  return result
 raise ValueError(t)
assert num('B')==10
string();data=read(10)
W,H,D=data['size'];pal=data['palette'];blocks={tuple(b['pos']):pal[b['state']] for b in data['blocks']}
air={'Name':'minecraft:air'}
def state(pos):return blocks.get(tuple(pos),air)
def name(pos):return state(pos)['Name'].removeprefix('minecraft:')
def props(pos):return state(pos).get('Properties',{})
def solid(pos):
 n=name(pos)
 return n not in ['air','chain','ladder','lantern','iron_bars','dark_oak_fence','lectern','lightning_rod','cyan_carpet'] and not n.endswith(('_stairs','_slab','_trapdoor'))
errors=[]
def check(ok,what):
 if not ok:errors.append(what)
counts=Counter()
for x in range(W):
 for y in range(H):
  for z in range(D):
   p=(x,y,z);n=name(p);counts[n]+=1
   if n=='ladder':
    dx,dz={'north':(0,1),'south':(0,-1),'east':(-1,0),'west':(1,0)}[props(p)['facing']]
    back=(x+dx,y,z+dz)
    check(solid(back) and 'glass' not in name(back),f'Ladder backing {p}: {name(back)}')
   if n=='lantern':
    if props(p)['hanging']=='true':
     q=(x,y+1,z)
     while name(q)=='chain':q=(q[0],q[1]+1,q[2])
     check(solid(q) or name(q)=='iron_bars',f'Unanchored hanging light {p} -> {q}')
    else:check(solid((x,y-1,z)),f'Unsupported standing lamp {p}')
   if n=='dark_oak_fence':check(solid((x,y-1,z)),f'Floating fence {p}')
   if n.endswith('carpet'):check(solid((x,y-1,z)),f'Floating carpet {p}')
   if n.endswith('trapdoor'):
    check((x,y,z) in [(a,4,b) for a in [12,20] for b in [13,17]],f'Unexpected trapdoor {p}')
    check(name((x,y-1,z))=='dark_oak_fence',f'Unsupported table top {p}')
check(counts['cartography_table']==0,'Unexpected cartography table')
# Check the intended mirror axes rather than demanding an asymmetric building be symmetric.
def mirrored(s,axis):
 s={'Name':s['Name'],'Properties':dict(s.get('Properties',{}))};p=s['Properties']
 if axis=='x':
  if p.get('facing') in ['east','west']:p['facing']={'east':'west','west':'east'}[p['facing']]
  if 'east' in p:p['east'],p['west']=p['west'],p['east']
 else:
  if p.get('facing') in ['north','south']:p['facing']={'north':'south','south':'north'}[p['facing']]
  if 'north' in p:p['north'],p['south']=p['south'],p['north']
 return s
for z in [7,8,30,31,32]:
 for x in range(7,26):
  for y in range(26):
   a=state((x,y,z));b=state((32-x,y,z))
   check(mirrored(a,'x')=={'Name':b['Name'],'Properties':b.get('Properties',{})},f'End elevation asymmetry {(x,y,z)} vs {(32-x,y,z)}')
for x in range(24,31):
 for y in range(12):
  for z in range(19,30):
   a=state((x,y,z));b=state((x,y,48-z))
   check(mirrored(a,'z')=={'Name':b['Name'],'Properties':b.get('Properties',{})},f'Bay asymmetry {(x,y,z)} vs {(x,y,48-z)}')
for x in [7,25]:
 for z in range(7,32):
  for y in range(3):check(solid((x,y,z)),f'Incomplete hall plinth {(x,y,z)}')
for z in [7,31]:
 for x in range(7,26):
  for y in range(3):check(solid((x,y,z)),f'Incomplete end plinth {(x,y,z)}')
for z in range(19,30):
 for y in range(3):check(solid((30,y,z)),f'Incomplete bay plinth {(30,y,z)}')
# All blocks must join the grounded structure, including suspended fittings.
occupied={p for p in blocks if name(p)!='air'}
seen={p for p in occupied if p[1]==0};queue=deque(seen)
steps=[(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]
while queue:
 x,y,z=queue.popleft()
 for dx,dy,dz in steps:
  q=(x+dx,y+dy,z+dz)
  if q in occupied and q not in seen:seen.add(q);queue.append(q)
for p in occupied-seen:check(False,f'Isolated non-air block {p}: {name(p)}')
# Occupancy includes the actual voids in straight stairs and top/bottom slabs.
# Thin decorative fittings deliberately do not seal the building's envelope.
NX,NY,NZ=2*W+2,2*H+2,2*D+2;stride=NX*NZ;size=NX*NY*NZ
sealed=bytearray(size)
def idx(x,y,z):return y*stride+z*NX+x
thin={'air','chain','ladder','lantern','iron_bars','dark_oak_fence','lectern','lightning_rod','cyan_carpet'}
for (x,y,z),s in blocks.items():
 n=name((x,y,z));p=s.get('Properties',{})
 if n in thin or n.endswith('_trapdoor'):continue
 for qx in [0,1]:
  for qy in [0,1]:
   for qz in [0,1]:
    filled=True
    if n.endswith('_slab') and p['type']!='double':filled=qy==(0 if p['type']=='bottom' else 1)
    if n.endswith('_stairs'):
     base=0 if p['half']=='bottom' else 1
     inner={'north':qz==0,'south':qz==1,'east':qx==1,'west':qx==0}[p['facing']]
     filled=qy==base or inner
    if filled:sealed[idx(2*x+1+qx,2*y+1+qy,2*z+1+qz)]=1
# Close only the intentional open entrance for the enclosure audit.
for x in range(15,18):
 for y in range(3,6):
  for qx in [0,1]:
   for qy in [0,1]:
    for qz in [0,1]:sealed[idx(2*x+1+qx,2*y+1+qy,17+qz)]=1
parent=array('i',[-1])*size;parent[0]=0;queue=deque([0])
while queue:
 i=queue.popleft();y,rem=divmod(i,stride);z,x=divmod(rem,NX)
 for delta,valid in [(1,x+1<NX),(-1,x>0),(NX,z+1<NZ),(-NX,z>0),(stride,y+1<NY),(-stride,y>0)]:
  j=i+delta
  if valid and not sealed[j] and parent[j]==-1:parent[j]=i;queue.append(j)
rooms={'hall':(16,4,19),'gallery/attic':(16,15,19),'tower ground':(4,4,15),'tower middle':(4,10,15),'tower top':(4,16,15),'reading bay':(27,4,24)}
for label,(x,y,z) in rooms.items():
 i=idx(2*x+1,2*y+1,2*z+1)
 if parent[i]!=-1:
  trace=[]
  while i and len(trace)<35:
   yy,r=divmod(i,stride);zz,xx=divmod(r,NX);b=((xx-1)//2,(yy-1)//2,(zz-1)//2)
   if not trace or trace[-1]!=b:trace.append(b)
   i=parent[i]
  check(False,f'Exterior air reaches {label}: {trace}')
report={'cells_checked':W*H*D,'authored_blocks':len(blocks),'non_air_blocks':len(occupied),'errors':errors,'half_block_envelope_cells':size,'room_probes':list(rooms),'block_counts':dict(sorted(counts.items()))}
out=ROOT/'standalone/tensura-additional-additions/docs/review-mentor/context-audit.json'
out.write_text(json.dumps(report,indent=2)+'\n')
print(f'Context audit: {report["cells_checked"]} cells, {len(occupied)} non-air blocks, {len(errors)} errors.')
for e in errors[:35]:print(e)
raise SystemExit(bool(errors))
