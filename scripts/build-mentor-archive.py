#!/usr/bin/env python3
"""Author the Archive of Accord template and Elias's original classic 64px skin."""
from pathlib import Path
import gzip,json,struct
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'standalone/tensura-additional-additions/src/main/resources'
blocks={};W,H,D=31,20,33

def b(x,y,z,name,**props):
 if not (0<=x<W and 0<=y<H and 0<=z<D):raise ValueError((x,y,z))
 blocks[x,y,z]=(name if ':' in name else 'minecraft:'+name,tuple(sorted(props.items())))
def box(x1,y1,z1,x2,y2,z2,name,**props):
 for x in range(x1,x2+1):
  for y in range(y1,y2+1):
   for z in range(z1,z2+1):b(x,y,z,name,**props)
# Clear only the occupied volume. Surrounding terrain is left to beard_thin blending.
box(4,0,5,26,16,28,'air')
box(10,0,0,20,7,4,'air')
box(4,0,5,26,0,28,'deepslate_bricks')
box(5,1,6,25,1,27,'polished_andesite')
box(6,1,7,24,1,26,'dark_oak_planks')
# Stone walls, weathered base and oak structural bays.
for y in range(1,11):
 for x in range(4,27):
  for z in [5,28]:b(x,y,z,'stone_bricks' if y<8 else 'calcite')
 for z in range(6,28):
  for x in [4,26]:b(x,y,z,'stone_bricks' if y<8 else 'calcite')
for x in [4,9,15,21,26]:
 for z in [5,28]:box(x,1,z,x,11,z,'dark_oak_log',axis='y')
for z in [5,11,17,23,28]:
 for x in [4,26]:box(x,1,z,x,11,z,'dark_oak_log',axis='y')
for x in [4,26]:box(x,11,6,x,12,27,'dark_oak_planks')
# Tall leaded glass and lantern corbels.
for z in [8,14,20,25]:
 for x in [4,26]:
  box(x,3,z-1,x,6,z+1,'light_blue_stained_glass')
  box(x,9,z,x,10,z,'yellow_stained_glass')
for x in [7,12,18,23]:
 box(x,3,28,x,6,28,'light_blue_stained_glass')
 box(x,9,28,x,10,28,'yellow_stained_glass')
# Central carpet, aisle and sunburst compass inlay.
box(14,2,5,16,2,21,'cyan_carpet')
for x,z in [(15,14),(14,14),(16,14),(15,13),(15,15)]:b(x,1,z,'gold_block')
for x,z in [(13,14),(17,14),(15,12),(15,16)]:b(x,1,z,'chiseled_stone_bricks')
# Lower alcove shelves with open aisles; upper galleries have continuous walking space.
for x in [6,24]:
 for z in range(8,26):
  if z in [11,17,23]:continue
  box(x,2,z,x,5,z,'bookshelf')
for x in [10,20]:
 for z in [10,15,23]:box(x,2,z,x,4,z+2,'bookshelf')
# Gallery ring, rail and transverse beams.
box(5,7,6,25,7,27,'dark_oak_planks')
box(10,7,10,20,7,23,'air')
for x in [9,21]:box(x,8,10,x,8,23,'dark_oak_fence')
for z in [9,24]:box(9,8,z,21,8,z,'dark_oak_fence')
for x in [6,24]:
 for z in range(8,27):
  if z%6 in [0,1,2,3]:box(x,8,z,x,10,z,'bookshelf')
# Two stairways rise toward the rear gallery. Adequate headroom on every step.
for side in [7,22]:
 for step in range(6):
  box(side,2+step,17+step,side+1,4+step,17+step,'air')
  for x in range(side,side+2):b(x,2+step,17+step,'dark_oak_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
# Carved columns, tie beams and hanging lighting.
for x in [9,21]:
 for z in [9,18,24]:
  box(x,2,z,x,6,z,'stripped_dark_oak_log',axis='y')
  b(x,6,z,'chiseled_stone_bricks')
for z in [6,12,18,24,27]:
 box(4,11,z,26,11,z,'dark_oak_log',axis='x')
 for x in [11,19]:
  box(x,9,z,x,10,z,'chain',axis='y',waterlogged='false')
  b(x,8,z,'lantern',hanging='true',waterlogged='false')
# Steep gabled slate roof with stepped cornices and a gold ridge.
for x in range(2,29):
 roof_y=12+min(x-2,28-x)//2
 for z in range(3,31):
  b(x,roof_y,z,'deepslate_tiles')
  if z in [3,30]:b(x,roof_y,z,'polished_deepslate')
for z in range(4,30):b(15,18,z,'dark_oak_log',axis='z')
for z in [4,15,29]:b(15,19,z,'gilded_blackstone')
# Triangular gable infill and circular-looking rose windows.
for x in range(5,26):
 for y in range(12,12+min(x-2,28-x)//2):
  for z in [5,28]:b(x,y,z,'dark_oak_planks')
for z in [5,28]:
 for dx,dy in [(0,0),(-1,0),(1,0),(0,-1),(0,1),(-1,-1),(1,-1),(-1,1),(1,1),(0,2),(0,-2),(-2,0),(2,0)]:
  b(15+dx,14+dy,z,'yellow_stained_glass' if abs(dx)+abs(dy)>1 else 'cyan_stained_glass')
# Teaching dais, lectern, flanking shelves and warm lamps.
box(12,2,24,18,2,26,'dark_oak_planks')
for x in range(12,19):b(x,2,23,'dark_oak_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
for x in [12,18]:
 box(x,3,25,x,5,25,'bookshelf');b(x,6,25,'lantern',hanging='false',waterlogged='false')
b(15,3,24,'lectern',facing='north',has_book='false',powered='false')
# Reading tables, benches, and writing desks.
for x in [12,18]:
 for z in [10,19]:
  b(x,2,z,'dark_oak_fence');b(x,3,z,'dark_oak_pressure_plate',powered='false')
  b(x,2,z+1,'dark_oak_stairs',facing='north',half='bottom',shape='straight',waterlogged='false')
# Entrance portico and open arch (no door AI obstruction).
box(12,0,0,18,0,4,'stone_bricks')
box(12,1,2,18,1,4,'polished_andesite')
for x in range(12,19):b(x,0,0,'stone_brick_stairs',facing='south',half='bottom',shape='straight',waterlogged='false');b(x,1,1,'stone_brick_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
box(14,2,5,16,5,5,'air')
for x in [11,19]:
 box(x,1,2,x,5,2,'stone_brick_wall',up='true',north='none',south='none',east='none',west='none',waterlogged='false')
 b(x,6,2,'chiseled_stone_bricks')
for x in range(10,21):box(x,7,1,x,7,4,'dark_oak_slab',type='bottom',waterlogged='false')
for x in [12,18]:b(x,6,3,'lantern',hanging='true',waterlogged='false')
# Empty side garden courts make the archive silhouette feel intentionally placed.
for x in [1,29]:
 for z in [8,15,23,29]:
  b(x,0,z,'mossy_stone_bricks');b(x,1,z,'stone_brick_wall',up='true',north='none',south='none',east='none',west='none',waterlogged='false');b(x,2,z,'lantern',hanging='false',waterlogged='false')
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
entity={'pos':ListT(6,[15.5,3.,25.5]),'blockPos':ListT(3,[15,3,25]),'nbt':C({'id':Str('heavenlyrestriction:elias'),'Pos':ListT(6,[15.5,3.,25.5]),'Rotation':ListT(5,[180.,0.]),'PersistenceRequired':(1,1),'Invulnerable':(1,1),'Age':I(0)})}
nbt={'DataVersion':I(3955),'size':ListT(3,[W,H,D]),'palette':ListT(10,palette),'blocks':ListT(10,placed),'entities':ListT(10,[entity])}
path=RES/'data/heavenlyrestriction/structure/mentor_archive.nbt';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(b'\x0a\0\0'+payload(10,nbt),mtime=0))
def data(name,value):
 p=RES/'data'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
data('heavenlyrestriction/worldgen/structure/mentor_archive.json',{'type':'minecraft:jigsaw','biomes':'#heavenlyrestriction:has_structure/mentor_archive','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'heavenlyrestriction:mentor_archive/start','size':1,'start_height':{'absolute':0},'project_start_to_heightmap':'WORLD_SURFACE_WG','max_distance_from_center':80,'use_expansion_hack':False})
data('heavenlyrestriction/worldgen/structure_set/mentor_archive.json',{'structures':[{'structure':'heavenlyrestriction:mentor_archive','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':128,'separation':48,'salt':178943613,'spread_type':'triangular'}})
data('heavenlyrestriction/worldgen/template_pool/mentor_archive/start.json',{'name':'heavenlyrestriction:mentor_archive/start','fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'heavenlyrestriction:mentor_archive','processors':'minecraft:empty','projection':'rigid'}}]})
data('heavenlyrestriction/tags/worldgen/biome/has_structure/mentor_archive.json',{'replace':False,'values':['minecraft:plains','minecraft:sunflower_plains','#minecraft:is_forest','minecraft:meadow','minecraft:cherry_grove','minecraft:savanna']})
# Original classic skin: grey swept hair, half-moon spectacles, teal/gold scholar's coat.
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
def faces(x,y,w,h,depth,colors):
 boxes=[(x+depth,y,w,depth),(x+depth+w,y,w,depth),(x,y+depth,depth,h),(x+depth,y+depth,w,h),(x+depth+w,y+depth,depth,h),(x+2*depth+w,y+depth,w,h)]
 for (px,py,pw,ph),color in zip(boxes,colors):
  d.rectangle((px,py,px+pw-1,py+ph-1),fill=color)
  for yy in range(py,py+ph):
   for xx in range(px,px+pw):
    if (xx*7+yy*3)%11==0:
     c=tuple(min(255,v+7) for v in color[:3]);d.point((xx,yy),fill=c+(255,))
faces(0,0,8,8,8,[(139,149,155,255),(188,153,125,255),(155,158,154,255),(204,170,143,255),(155,158,154,255),(134,146,152,255)])
d.rectangle((8,8,15,9),fill='#d7dddc');d.rectangle((8,10,9,11),fill='#c7d0ce');d.rectangle((14,9,15,11),fill='#aab9bb')
d.point((11,11),fill='#546068');d.point((14,11),fill='#546068');d.rectangle((10,12,11,12),fill='#dfc58b');d.rectangle((13,12,14,12),fill='#dfc58b');d.point((12,12),fill='#806b50')
d.rectangle((10,14,14,15),fill='#b8bfba');d.point((12,14),fill='#91765e')
faces(16,16,8,12,4,[(29,53,65,255),(21,35,49,255),(33,51,65,255),(32,61,73,255),(33,51,65,255),(27,45,62,255)])
for x in [21,26]:d.line((x,21,x,30),fill='#d8bc78')
d.polygon([(22,20),(25,20),(24,25),(23,25)],fill='#e8e0c5');d.rectangle((20,29,27,30),fill='#735646');d.rectangle((23,29,24,30),fill='#d8bc78')
# Embroidered small open-book emblem on the back.
d.line((33,24,35,25,37,24),fill='#dec78a',width=1);d.line((35,22,35,25),fill='#dec78a')
for x,y in [(40,16),(32,48)]:
 faces(x,y,4,12,4,[(41,64,78,255),(192,157,129,255),(31,52,67,255),(36,60,73,255),(31,52,67,255),(29,47,63,255)])
 for fx in [x,x+4,x+8,x+12]:
  d.rectangle((fx,y+12,fx+3,y+12),fill='#d5b775');d.rectangle((fx,y+13,fx+3,y+15),fill='#c7a181')
for x,y in [(0,16),(16,48)]:
 faces(x,y,4,12,4,[(31,42,54,255),(27,29,37,255),(29,38,49,255),(30,42,54,255),(29,38,49,255),(26,35,47,255)])
 for fx in [x,x+4,x+8,x+12]:d.rectangle((fx,y+12,fx+3,y+15),fill='#42372f')
# Sparse outer hair wrap adds volume without covering the face.
for px,py,pw,ph in [(40,0,8,8),(32,8,8,4),(48,8,8,4),(56,8,8,7)]:d.rectangle((px,py,px+pw-1,py+ph-1),fill='#b2bebb')
path=RES/'assets/heavenlyrestriction/textures/entity/elias.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
path.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":true}}\n')
print(f'Archive: {len(blocks)} blocks, {len(palette)} states, one mentor; original 64x64 skin.')
