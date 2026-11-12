#!/usr/bin/env python3
"""Archive of Accord: layered medieval library, authored as a vanilla structure template."""
from pathlib import Path
import gzip,json,struct
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'standalone/tensura-additional-additions/src/main/resources'
blocks={};W,H,D=31,29,33

def b(x,y,z,name,**props):
 if not(0<=x<W and 0<=y<H and 0<=z<D):raise ValueError((x,y,z,name))
 blocks[x,y,z]=('minecraft:'+name if ':' not in name else name,tuple(sorted(props.items())))
def box(x1,y1,z1,x2,y2,z2,name,**props):
 for x in range(x1,x2+1):
  for y in range(y1,y2+1):
   for z in range(z1,z2+1):b(x,y,z,name,**props)
def stair(x,y,z,name,facing,half='bottom'):
 b(x,y,z,name,facing=facing,half=half,shape='straight',waterlogged='false')
def slab(x,y,z,name,kind='bottom'):b(x,y,z,name,type=kind,waterlogged='false')
def lamp(x,y,z,hanging=False):b(x,y,z,'lantern',hanging=str(hanging).lower(),waterlogged='false')
def gable(xl,xr,zf,zb,eave):
 mid=(xl+xr)//2
 for x in range(xl,xr+1):
  yy=eave+min(x-xl,xr-x)
  for z in range(zf,zb+1):
   trim=z in [zf,zb]
   if x==mid:slab(x,yy,z,'polished_deepslate_slab' if trim else 'deepslate_tile_slab')
   else:stair(x,yy,z,'polished_deepslate_stairs' if trim else 'deepslate_tile_stairs','east' if x<mid else 'west')
   if x not in [xl,xr] and yy>eave:b(x,yy-1,z,'dark_oak_planks')
 # Underslung eave brackets read as an overhang rather than a thick solid wedge.
 for z in range(zf+1,zb):
  for x in [xl,xr]:slab(x,eave-1,z,'dark_oak_slab',kind='top')

# Clear the occupied volumes first; every room is then built from floor to roof.
for bounds in [(6,2,6,26,26,32),(0,2,11,8,28,19),(9,2,1,23,17,8),(25,2,19,30,13,28)]:box(*bounds,'air')
box(8,0,8,24,1,30,'stone_bricks');box(8,2,8,24,2,30,'spruce_planks')
# The hall has a masonry ground floor and framed plaster above, not patterned gable logs.
for y in range(3,13):
 for x in range(8,25):
  for z in [8,30]:b(x,y,z,'stone_bricks' if y<8 else 'calcite')
 for z in range(9,30):
  for x in [8,24]:b(x,y,z,'stone_bricks' if y<8 else 'calcite')
for wall,outer in [(8,7),(24,25)]:
 for z in [8,14,20,26,30]:
  box(wall,3,z,wall,12,z,'stripped_dark_oak_log',axis='y')
  if wall==8 and 11<=z<=19 or wall==24 and 20<=z<=27:continue
  box(outer,0,z,outer,2,z,'stone_bricks')
  box(outer,3,z,outer,12,z,'stripped_dark_oak_log',axis='y')
 for z in range(8,31):
  b(wall,8,z,'dark_oak_log',axis='z');b(wall,12,z,'dark_oak_log',axis='z')
  if wall==8 and 11<=z<=19 or wall==24 and 20<=z<=27:continue
  b(outer,8,z,'dark_oak_log',axis='z');slab(outer,2,z,'stone_brick_slab',kind='top')
 for cz in [11,17,23,28]:
  if wall==8 and 11<=cz<=19 or wall==24 and 20<=cz<=27:continue
  box(wall,4,cz-1,wall,6,cz+1,'light_blue_stained_glass')
  for zz in range(cz-1,cz+2):
   slab(outer,3,zz,'stone_brick_slab',kind='top')
   stair(outer,7,zz,'stone_brick_stairs','east' if wall==8 else 'west',half='top')
  box(wall,10,cz-1,wall,11,cz+1,'yellow_stained_glass')
# Each end has a visible outer frame; windows sit a block behind the posts.
for wall,outer in [(8,7),(30,31)]:
 for x in [8,13,19,24]:
  box(x,0,outer,x,2,outer,'stone_bricks')
  box(x,3,outer,x,12,outer,'stripped_dark_oak_log',axis='y')
 for y in [8,12]:box(8,y,outer,24,y,outer,'dark_oak_log',axis='x')
 for cx in [11,21]:
  box(cx-1,4,wall,cx+1,6,wall,'light_blue_stained_glass')
  box(cx-1,10,wall,cx+1,11,wall,'yellow_stained_glass')
  for x in range(cx-1,cx+2):
   slab(x,3,outer,'stone_brick_slab',kind='top')
   stair(x,7,outer,'stone_brick_stairs','south' if wall==8 else 'north',half='top')
   slab(x,9,outer,'dark_oak_slab',kind='top')
# A closed, continuous gable: straight tie beams, king posts and paired windows.
gable(6,26,6,32,13)
for z in [8,30]:
 for x in range(8,25):
  top=13+min(x-6,26-x)
  for y in range(13,top):b(x,y,z,'calcite')
 box(8,13,z,24,13,z,'dark_oak_log',axis='x')
 box(16,14,z,16,22,z,'stripped_dark_oak_log',axis='y')
 if z==8:
  # These clerestories sit above the porch roof intersection.
  for xl,xr in [(14,15),(17,18)]:box(xl,17,z,xr,19,z,'yellow_stained_glass')
  for x in [13,19]:box(x,14,z,x,19,z,'stripped_dark_oak_log',axis='y')
  box(14,20,z,18,20,z,'dark_oak_log',axis='x')
 else:
  for xl,xr in [(13,14),(18,19)]:box(xl,15,z,xr,18,z,'yellow_stained_glass')
  for x in [12,20]:box(x,14,z,x,18,z,'stripped_dark_oak_log',axis='y')
  box(13,19,z,19,19,z,'dark_oak_log',axis='x')
# Real transverse beams carry the roof and gallery brackets.
for z in [9,15,21,29]:
 box(8,12,z,24,12,z,'dark_oak_log',axis='x')
 for x in [9,23]:stair(x,11,z,'dark_oak_stairs','east' if x==9 else 'west',half='top')
# A grounded rear chimney breaks the long flat wall and passes through the roof.
box(15,0,30,17,24,31,'stone_bricks')
for y in [2,8,14,23]:box(15,y,30,17,y,31,'polished_andesite')
for x in range(15,18):
 for z in range(30,33):slab(x,25,z,'stone_brick_slab')
# The overhanging chimney cap is carried by the stack below, with no floating finials.
# Entrance porch: four grounded posts support the tie beams and the smaller gable.
box(10,0,2,22,1,7,'stone_bricks');box(10,2,3,22,2,7,'polished_andesite')
for n,z in enumerate([0,1,2]):
 for x in range(13,20):stair(x,n,z,'stone_brick_stairs','south')
for x in [11,21]:
 for z in [3,7]:
  b(x,2,z,'chiseled_stone_bricks');box(x,3,z,x,7,z,'stripped_dark_oak_log',axis='y')
for z in [3,7]:box(11,8,z,21,8,z,'dark_oak_log',axis='x')
gable(9,23,2,8,9)
for x in range(11,22):
 for y in range(9,9+min(x-9,23-x)):b(x,y,3,'dark_oak_planks')
box(15,11,3,17,12,3,'yellow_stained_glass')
for x in [12,20]:
 box(x,7,4,x,10,4,'chain',axis='y',waterlogged='false');lamp(x,6,4,True)
for x in [10,22]:
 for z in [0,1,2]:b(x,0,z,'mossy_stone_bricks');b(x,1,z,'azalea_leaves',distance='1',persistent='true',waterlogged='false')
# Chamfered tower. Floors at 2/8/14 and a closed ceiling at 21 define three lit rooms.
def tower(dx,dz):return abs(dx)<=3 and abs(dz)<=3 and abs(dx)+abs(dz)<=5
for x in range(1,8):
 for z in range(12,19):
  if not tower(x-4,z-15):continue
  edge=any(not tower(x-4+a,z-15+c) for a,c in [(1,0),(-1,0),(0,1),(0,-1)])
  for y in range(0,22):
   if y in [0,1,2,8,14,21]:b(x,y,z,'stone_bricks' if y<=2 else 'spruce_planks')
   elif edge:b(x,y,z,'stone_bricks' if y<8 else 'calcite')
for face in ['north','west','south']:
 def tpos(u,y,depth=1):
  return (u,y,12-depth) if face=='north' else (1-depth,y,u+11) if face=='west' else (u,y,18+depth)
 for u in [2,6]:
  for y in range(0,21):b(*tpos(u,y),'stone_bricks' if y<8 else 'stripped_dark_oak_log',**({} if y<8 else {'axis':'y'}))
 for y in [8,14,20]:
  for u in range(2,7):b(*tpos(u,y),'dark_oak_log',axis='z' if face=='west' else 'x')
 for base in [3,9,15]:
  for u in [3,4,5]:
   for y in range(base+1,base+4):b(*tpos(u,y,0),'light_blue_stained_glass')
   slab(*tpos(u,base),'stone_brick_slab',kind='top')
   stair(*tpos(u,base+4),'stone_brick_stairs','east' if face=='west' else 'south' if face=='north' else 'north',half='top')
# East upper window; lower openings are the actual links to the hall and gallery.
box(7,16,14,7,18,16,'light_blue_stained_glass')
for x in range(0,9):
 for z in range(11,20):
  dx,dz=x-4,z-15;r=max(abs(dx),abs(dz))
  if abs(dx)+abs(dz)>6:continue
  y=25-r
  if r==0:slab(x,y,z,'waxed_oxidized_cut_copper_slab',kind='double')
  elif abs(dx)==abs(dz):b(x,y,z,'waxed_oxidized_cut_copper')
  else:stair(x,y,z,'waxed_oxidized_cut_copper_stairs','east' if abs(dx)>abs(dz) and dx<0 else 'west' if abs(dx)>abs(dz) else 'south' if dz<0 else 'north')
  if r:b(x,y-1,z,'dark_oak_planks')
b(4,26,15,'lightning_rod',facing='up',powered='false',waterlogged='false')
for y in range(3,21):b(3,y,17,'ladder',facing='north',waterlogged='false')
for ceiling in [8,14,21]:
 b(4,ceiling-1,15,'chain',axis='y',waterlogged='false');lamp(4,ceiling-2,15,True)
box(2,3,13,2,4,16,'bookshelf');b(5,15,15,'cartography_table')
# Reading bay: continuous half-step copper lean-to, fully infilled end walls,
# a closed outer wall, and an explicitly framed doorway on a continuous floor.
box(24,0,20,29,1,27,'stone_bricks');box(24,2,20,29,2,27,'spruce_planks')
for x in range(24,31):
 roof_y=11-(x-24+1)//2
 if x<=29:
  for z in [20,27]:box(x,3,z,x,roof_y-1,z,'stone_bricks' if x==29 else 'calcite')
 for z in range(19,29):
  if (x-24)%2: b(x,roof_y,z,'waxed_weathered_cut_copper')
  else:slab(x,roof_y,z,'waxed_weathered_cut_copper_slab')
  b(x,roof_y-1,z,'dark_oak_planks')
box(29,3,21,29,7,26,'stone_bricks')
box(29,4,22,29,6,25,'light_blue_stained_glass')
for z in [20,27]:
 for x in [24,29]:box(x,3,z,x,(10 if x==24 else 7),z,'stripped_dark_oak_log',axis='y')
 box(26,4,z,27,6,z,'light_blue_stained_glass')
for z in [21,26]:
 box(30,0,z,30,2,z,'stone_bricks');box(30,3,z,30,7,z,'stripped_dark_oak_log',axis='y')
for z in range(22,26):
 slab(30,3,z,'stone_brick_slab',kind='top');stair(30,7,z,'stone_brick_stairs','west',half='top')
for z in [21,26]:box(24,3,z,24,6,z,'stripped_dark_oak_log',axis='y')
box(24,6,21,24,6,26,'dark_oak_log',axis='z')
for z in range(23,26):stair(28,3,z,'spruce_stairs','east')
box(25,3,21,25,4,21,'bookshelf')
b(27,7,24,'chain',axis='y',waterlogged='false');lamp(27,6,24,True)
# Gallery, shelving and a staircase with a clear landing and headroom.
box(9,8,9,23,8,29,'spruce_planks');box(12,8,12,20,8,25,'air')
for x in [11,21]:box(x,9,12,x,9,25,'dark_oak_fence')
for z in [11,26]:box(11,9,z,21,9,z,'dark_oak_fence')
for x in [9,23]:
 for z in [10,16,22,27]:
  if not(x==23 and z==22):box(x,3,z,x,6,min(29,z+2),'bookshelf')
  box(x,9,z,x,11,min(29,z+2),'bookshelf')
 for z in [11,17,23,28]:lamp(x,12,z)
for n in range(6):
 for x in [21,22]:
  box(x,4+n,20+n,x,6+n,20+n,'air');stair(x,3+n,20+n,'spruce_stairs','south')
box(21,9,23,22,11,26,'air')
for x in [10,22]:
 for z in [12,19,26]:
  b(x,7,z,'chain',axis='y',waterlogged='false');lamp(x,6,z,True)
# Desk trapdoors are table tops supported by fence legs; none are used as exterior shutters.
for x in [12,20]:
 for z in [13,20]:
  b(x,3,z,'dark_oak_fence');b(x,4,z,'spruce_trapdoor',facing='north',half='bottom',open='false',powered='false',waterlogged='false')
  stair(x,3,z+1,'spruce_stairs','north')
box(15,3,8,17,3,25,'cyan_carpet')
box(13,3,27,19,3,29,'dark_oak_planks')
for x in range(13,20):stair(x,3,26,'dark_oak_stairs','south')
for x in [13,19]:box(x,4,28,x,6,28,'bookshelf');lamp(x,7,28)
b(16,4,26,'lectern',facing='north',has_book='false',powered='false')
# Cross-arm chandelier. Every lantern has a chain and every chain reaches a beam.
box(16,14,19,16,21,19,'chain',axis='y',waterlogged='false')
for x in range(14,19):b(x,13,19,'dark_oak_fence')
for z in range(17,22):b(16,13,z,'dark_oak_fence')
for x,z in [(14,19),(18,19),(16,17),(16,21)]:
 b(x,12,z,'chain',axis='y',waterlogged='false');lamp(x,11,z,True)
# Openings are carved last, so no shelf, framing or roof trim closes the routes.
box(14,3,8,18,5,8,'air');box(14,6,8,18,6,8,'dark_oak_log',axis='x')
box(7,3,14,9,6,16,'air');box(7,9,14,9,11,16,'air')
box(7,8,14,9,8,16,'spruce_planks');box(23,3,22,24,5,25,'air')
# The threshold is flooring, not a gap or a change in height.
box(23,2,22,25,2,25,'spruce_planks')
# Resolve fence connections explicitly for known-shape jigsaw placement.
for (x,y,z),(name,props) in list(blocks.items()):
 if name!='minecraft:dark_oak_fence':continue
 v=dict(props)
 for direction,dx,dz in [('north',0,-1),('south',0,1),('west',-1,0),('east',1,0)]:v[direction]=str(blocks.get((x+dx,y,z+dz),('',()))[0]==name).lower()
 v['waterlogged']='false';blocks[x,y,z]=(name,tuple(sorted(v.items())))
# Authoring invariants: reject unsupported light fixtures or exterior trapdoors.
for (x,y,z),(name,props) in blocks.items():
 if name=='minecraft:lantern':
  attached=(x,y+1,z) if dict(props).get('hanging')=='true' else (x,y-1,z)
  assert blocks.get(attached,('minecraft:air',()))[0]!='minecraft:air',('unsupported lantern',(x,y,z))
  if dict(props).get('hanging')=='true':
   ax,ay,az=attached
   while blocks.get((ax,ay,az),('minecraft:air',()))[0]=='minecraft:chain':ay+=1
   assert blocks.get((ax,ay,az),('minecraft:air',()))[0]!='minecraft:air',('unanchored chain',(x,y,z))
 if name.endswith('trapdoor'):assert x in [12,20] and z in [13,20] and y==4,('exterior trapdoor',(x,y,z))
for ceiling in [8,14,21]:assert blocks[4,ceiling,15][0]!='minecraft:air'
for z in [8,30]:
 for x in range(6,27):
  if z==30 and 15<=x<=17:continue # Deliberate chimney penetration.
  y=13+min(x-6,26-x)
  assert 'deepslate' in blocks[x,y,z][0],('framing penetrates roof',(x,y,z))
for x in range(25,29):
 roof_y=11-(x-24+1)//2
 for z in [20,27]:
  for y in range(3,roof_y):assert blocks[x,y,z][0]!='minecraft:air',('open bay end wall',(x,y,z))
 for z in range(21,27):assert any(blocks.get((x,y,z),('minecraft:air',()))[0]!='minecraft:air' for y in range(8,12)),('missing bay roof',(x,z))

# Minimal typed NBT writer: structure templates are ordinary vanilla NBT, not runtime geometry.
def I(v):return (3,v)
def L(v):return (4,v)
def F(v):return (5,v)
def Double(v):return (6,v)
def Str(v):return (8,v)
def ListT(kind,values):return (9,(kind,values))
def C(v):return (10,v)
def utf(s):v=s.encode();return struct.pack('>H',len(v))+v
def payload(kind,v):
 if kind==1:return struct.pack('>b',v)
 if kind==3:return struct.pack('>i',v)
 if kind==4:return struct.pack('>q',v)
 if kind==5:return struct.pack('>f',v)
 if kind==6:return struct.pack('>d',v)
 if kind==8:return utf(v)
 if kind==9:
  k,values=v;return bytes([k])+struct.pack('>i',len(values))+b''.join(payload(k,a) for a in values)
 if kind==10:return b''.join(bytes([k])+utf(n)+payload(k,a) for n,(k,a) in v.items())+b'\0'
 raise ValueError(kind)
palette=[];indices={};placed=[]
for pos,state in sorted(blocks.items()):
 if state not in indices:
  indices[state]=len(palette);name,props=state;entry={'Name':Str(name)}
  if props:entry['Properties']=C({k:Str(v) for k,v in props})
  palette.append(entry)
 placed.append({'pos':ListT(3,list(pos)),'state':I(indices[state])})
entity={'pos':ListT(6,[16.5,4.,27.5]),'blockPos':ListT(3,[16,4,27]),'nbt':C({'id':Str('heavenlyrestriction:elias'),'Pos':ListT(6,[16.5,4.,27.5]),'Rotation':ListT(5,[180.,0.]),'PersistenceRequired':(1,1),'Invulnerable':(1,1),'Age':I(0)})}
nbt={'DataVersion':I(3955),'size':ListT(3,[W,H,D]),'palette':ListT(10,palette),'blocks':ListT(10,placed),'entities':ListT(10,[entity])}
path=RES/'data/heavenlyrestriction/structure/mentor_archive.nbt';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(b'\x0a\0\0'+payload(10,nbt),mtime=0))
def data(name,value):
 p=RES/'data'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
data('heavenlyrestriction/worldgen/structure/mentor_archive.json',{'type':'minecraft:jigsaw','biomes':'#heavenlyrestriction:has_structure/mentor_archive','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'heavenlyrestriction:mentor_archive/start','size':1,'start_height':{'absolute':0},'project_start_to_heightmap':'WORLD_SURFACE_WG','max_distance_from_center':80,'use_expansion_hack':False})
data('heavenlyrestriction/worldgen/structure_set/mentor_archive.json',{'structures':[{'structure':'heavenlyrestriction:mentor_archive','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':128,'separation':48,'salt':178943613,'spread_type':'triangular'}})
data('heavenlyrestriction/worldgen/template_pool/mentor_archive/start.json',{'name':'heavenlyrestriction:mentor_archive/start','fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'heavenlyrestriction:mentor_archive','processors':'minecraft:empty','projection':'rigid'}}]})
data('heavenlyrestriction/tags/worldgen/biome/has_structure/mentor_archive.json',{'replace':False,'values':['minecraft:plains','minecraft:sunflower_plains','#minecraft:is_forest','minecraft:meadow','minecraft:cherry_grove','minecraft:savanna']})
print(f'Archive: {len(blocks)} blocks, {len(palette)} states, one mentor; Kael-style skin is built separately.')
