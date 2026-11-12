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
def timber(x,y,z,axis='y'):b(x,y,z,'stripped_dark_oak_log',axis=axis)
def masonry(x,y,z):
 # Weathering collects at the plinth; intact masonry dominates upper walls.
 n=(x*17+z*13+y*7)%31
 return 'mossy_stone_bricks' if y<=1 and n<5 else 'cracked_stone_bricks' if y<=3 and n<3 else 'stone_bricks'
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

def rose(cx,cy,z,front):
 for dx in range(-3,4):
  for dy in range(-3,4):
   r=dx*dx+dy*dy
   if r<=10:
    b(cx+dx,cy+dy,z,'smooth_quartz' if r>=6 else 'yellow_stained_glass' if dx==0 or dy==0 else 'light_blue_stained_glass')
 b(cx,cy,z,'sea_lantern')
 # A thin sill projects in front of the inset glass.
 for dx in range(-2,3):slab(cx+dx,cy-3,z+front,'stone_brick_slab',kind='top')

# Distinct connected masses; terrain is cleared above the occupied volume only.
for bounds in [(7,2,7,25,24,31),(0,2,11,8,28,19),(9,2,1,23,16,8),(24,2,19,30,14,28)]:box(*bounds,'air')
# Main hall plinth and floor.
box(8,0,8,24,1,30,'stone_bricks');box(8,2,8,24,2,30,'spruce_planks')
for x in range(8,25):
 for z in range(8,31):
  if x in [8,24] or z in [8,30]:b(x,2,z,'polished_andesite')
for y in range(3,13):
 for x in range(8,25):
  for z in [8,30]:b(x,y,z,masonry(x,y,z) if y<8 else 'calcite')
 for z in range(9,30):
  for x in [8,24]:b(x,y,z,masonry(x,y,z) if y<8 else 'calcite')
# Structural bays protrude from the plaster and the windows sit one block behind them.
for x in [8,24]:
 for z in [8,14,20,26,30]:
  box(x,3,z,x,12,z,'stripped_dark_oak_log',axis='y')
  outside=x-1 if x==8 else x+1
  box(outside,2,z,outside,3,z,'stone_bricks')
  stair(outside,11,z,'dark_oak_stairs','east' if x==8 else 'west',half='top')
for z in [8,30]:
 for x in [8,13,19,24]:box(x,3,z,x,12,z,'stripped_dark_oak_log',axis='y')
for z in [8,30]:box(8,8,z,24,8,z,'dark_oak_log',axis='x')
for x in [8,24]:box(x,8,8,x,8,30,'dark_oak_log',axis='z');box(x,12,8,x,13,30,'dark_oak_planks')
for x in [8,24]:
 outer=x-1 if x==8 else x+1
 for z in [11,17,23,28]:
  box(x,4,z-1,x,6,z+1,'light_blue_stained_glass')
  for zz in range(z-1,z+2):slab(outer,3,zz,'stone_brick_slab',kind='top');stair(outer,7,zz,'stone_brick_stairs','east' if x==8 else 'west',half='top')
  for zz in [z-2,z+2]:
   for yy in [4,5,6]:b(outer,yy,zz,'spruce_trapdoor',facing='east' if x==8 else 'west',half='bottom',open='true',powered='false',waterlogged='false')
  box(x,10,z,x,11,z,'yellow_stained_glass')
for z in [8,30]:
 for x in [11,21]:box(x-1,4,z,x+1,6,z,'light_blue_stained_glass')
# Main steep gable, exposed trusses and a framed rose window in each end.
gable(6,26,6,32,13)
for z in [8,30]:
 for x in range(8,25):
  top=13+min(x-6,26-x)
  for yy in range(13,top):b(x,yy,z,'calcite' if (x+yy)%6 else 'stripped_dark_oak_log',**({'axis':'y'} if (x+yy)%6==0 else {}))
 rose(16,18,z,-1 if z==8 else 1)
for z in [9,16,23,29]:
 box(8,12,z,24,12,z,'dark_oak_log',axis='x')
 for x in [9,23]:stair(x,11,z,'dark_oak_stairs','east' if x==9 else 'west',half='top')
for z in [7,31]:
 slab(16,23,z,'polished_deepslate_slab',kind='double')
 b(16,24,z,'polished_deepslate_wall',up='true',north='none',south='none',east='none',west='none',waterlogged='false')
# Portico: a lower gable, carved arch and three wide steps make a clear entrance.
box(10,0,2,22,1,7,'stone_bricks');box(10,2,3,22,2,7,'polished_andesite')
for n,z in enumerate([0,1,2]):
 for x in range(13,20):stair(x,n,z,'stone_brick_stairs','south')
for x in [11,21]:
 for z in [3,7]:
  b(x,2,z,'chiseled_stone_bricks');box(x,3,z,x,7,z,'stripped_dark_oak_log',axis='y');b(x,8,z,'chiseled_stone_bricks')
box(11,8,3,21,8,3,'dark_oak_log',axis='x');gable(9,23,2,8,9)
for x in range(11,22):
 for yy in range(10,9+min(x-9,23-x)):b(x,yy,3,'dark_oak_planks')
for dx,dy in [(0,0),(-1,0),(1,0),(0,-1),(0,1)]:b(16+dx,12+dy,3,'yellow_stained_glass')
box(14,3,8,18,5,8,'air');box(15,6,8,17,6,8,'air');b(16,7,8,'air')
for x in [14,18]:stair(x,6,7,'stone_brick_stairs','east' if x==14 else 'west',half='top')
for x in [12,20]:b(x,7,4,'chain',axis='y',waterlogged='false');lamp(x,6,4,True)
# Stone path and low planting pockets stay subordinate to the facade.
for x in [10,22]:
 for z in [0,1,2]:b(x,0,z,'mossy_stone_bricks');b(x,1,z,'azalea_leaves',distance='1',persistent='true',waterlogged='false')
# Reading bay: a low lean-to roof and deep glazed corners break up the hall's east side.
box(24,0,20,29,1,27,'stone_bricks');box(24,2,20,29,2,27,'spruce_planks')
for yy in range(3,9):
 for x in range(24,30):
  for z in [20,27]:b(x,yy,z,'calcite' if yy>=7 else 'stone_bricks')
 for z in range(21,27):b(29,yy,z,'calcite' if yy>=7 else 'stone_bricks')
for x,z in [(24,20),(29,20),(29,27),(24,27)]:box(x,3,z,x,9,z,'dark_oak_log',axis='y')
box(29,4,22,29,6,25,'light_blue_stained_glass')
for z in [20,27]:box(26,4,z,27,6,z,'light_blue_stained_glass')
box(24,3,22,24,6,25,'air')
for x in range(24,31):
 yy=14-(x-24)//2
 for z in range(19,29):
  stair(x,yy,z,'waxed_weathered_cut_copper_stairs','west')
  b(x,yy-1,z,'dark_oak_planks')
box(28,3,22,28,3,25,'spruce_stairs',facing='west',half='bottom',shape='straight',waterlogged='false')
b(27,3,21,'bookshelf');lamp(27,4,21)
# Chamfered archive tower with three connected levels and a copper crown.
def tower(dx,dz,r=3):return abs(dx)<=r and abs(dz)<=r and abs(dx)+abs(dz)<=r*2-1
for x in range(1,8):
 for z in range(12,19):
  dx,dz=x-4,z-15
  if not tower(dx,dz):continue
  edge=any(not tower(dx+a,dz+c) for a,c in [(1,0),(-1,0),(0,1),(0,-1)])
  for yy in range(0,22):
   if yy in [0,1,2,8,14]:b(x,yy,z,'polished_andesite' if yy<3 else 'spruce_planks')
   elif edge:b(x,yy,z,'stone_bricks' if yy<14 else 'calcite')
for yy in [3,9,15]:
 for x,z in [(1,15),(7,15),(4,12),(4,18)]:box(x,yy+1,z,x,yy+3,z,'light_blue_stained_glass')
for yy in [7,13,20]:
 for x in range(1,8):
  for z in range(12,19):
   if tower(x-4,z-15) and any(not tower(x-4+a,z-15+c) for a,c in [(1,0),(-1,0),(0,1),(0,-1)]):b(x,yy,z,'chiseled_stone_bricks' if yy==20 else 'dark_oak_log',**({'axis':'y'} if yy!=20 else {}))
# Open links join the tower to the main hall and gallery.
box(7,3,14,8,6,16,'air');box(7,9,14,8,11,16,'air');box(7,8,14,9,8,16,'spruce_planks')
for yy in range(3,20):
 b(3,yy,17,'ladder',facing='north',waterlogged='false')

for x,z in [(2,14),(2,16)]:box(x,3,z,x,5,z,'bookshelf')
b(5,15,15,'cartography_table');lamp(4,19,15)
for x in range(0,9):
 for z in range(11,20):
  dx,dz=x-4,z-15;r=max(abs(dx),abs(dz))
  if abs(dx)+abs(dz)>6:continue
  yy=25-r
  if r==0:slab(x,yy,z,'waxed_oxidized_cut_copper_slab',kind='double')
  elif abs(dx)==abs(dz):b(x,yy,z,'waxed_oxidized_cut_copper')
  else:stair(x,yy,z,'waxed_oxidized_cut_copper_stairs','east' if abs(dx)>abs(dz) and dx<0 else 'west' if abs(dx)>abs(dz) else 'south' if dz<0 else 'north')
  if r:b(x,yy-1,z,'dark_oak_planks')
b(4,26,15,'lightning_rod',facing='up',powered='false',waterlogged='false')
# Gallery, coherent shelf bays and unobstructed central aisle.
box(9,8,9,23,8,29,'spruce_planks');box(12,8,12,20,8,25,'air')
for x in [11,21]:box(x,9,12,x,9,25,'dark_oak_fence')
for z in [11,26]:box(11,9,z,21,9,z,'dark_oak_fence')
for x in [9,23]:
 for z in [10,16,22,27]:
  box(x,3,z,x,6,min(28,z+2),'bookshelf');box(x,9,z,x,11,min(28,z+2),'bookshelf')
# Reopen tower and reading-bay links after fitting the shelves.
box(9,3,14,9,6,16,'air');box(9,9,14,9,11,16,'air');box(23,3,22,23,6,25,'air')
for yy in [3,9]:
 for z in [12,19,26]:
  for x in [10,22]:lamp(x,yy+3,z,True)
box(15,3,8,17,3,25,'cyan_carpet')
for x,z in [(15,19),(16,18),(17,19),(16,20)]:b(x,2,z,'waxed_weathered_cut_copper')
b(16,2,19,'chiseled_stone_bricks')
# A real six-step gallery staircase; its landing and overhead space are clear.
for n in range(6):
 for x in [21,22]:
  box(x,4+n,20+n,x,6+n,20+n,'air');stair(x,3+n,20+n,'spruce_stairs','south')
box(21,9,23,22,11,26,'air')
# Reading desks face the centre, and the bay provides a quieter bench.
for x in [12,20]:
 for z in [13,20]:
  b(x,3,z,'dark_oak_fence');b(x,4,z,'spruce_trapdoor',facing='north',half='bottom',open='false',powered='false',waterlogged='false')
  stair(x,3,z+1,'spruce_stairs','north')
# Teaching dais: the mentor stands one level above the main hall.
box(13,3,27,19,3,29,'dark_oak_planks')
for x in range(13,20):stair(x,3,26,'dark_oak_stairs','south')
for x in [13,19]:box(x,4,28,x,6,28,'bookshelf');lamp(x,7,28)
b(16,4,26,'lectern',facing='north',has_book='false',powered='false')
# Central chandelier is suspended from the ridge, not from empty air.
box(16,14,19,16,22,19,'chain',axis='y',waterlogged='false');b(16,13,19,'gilded_blackstone')
for x in range(14,19):
 for z in [17,21]:b(x,12,z,'iron_bars')
for z in range(18,21):
 for x in [14,18]:b(x,12,z,'iron_bars')
for x,z in [(14,17),(18,17),(14,21),(18,21)]:lamp(x,11,z,True)
for x in range(14,19):b(x,13,19,'iron_bars')
for z in range(17,22):
 for x in [14,18]:b(x,13,z,'iron_bars')
b(16,13,19,'gilded_blackstone')
for x,z in [(13,18),(19,18),(13,20),(19,20)]:b(x,13,z,'iron_bars')
# Encode connections: jigsaw placement suppresses ordinary neighbour-shape updates.
for (x,y,z),(name,props) in list(blocks.items()):
 if name not in ['minecraft:dark_oak_fence','minecraft:iron_bars']:continue
 v=dict(props)
 for direction,dx,dz in [('north',0,-1),('south',0,1),('west',-1,0),('east',1,0)]:v[direction]=str(blocks.get((x+dx,y,z+dz),('',()))[0]==name).lower()
 v['waterlogged']='false';blocks[x,y,z]=(name,tuple(sorted(v.items())))
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
