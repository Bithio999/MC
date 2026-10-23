#!/usr/bin/env python3
"""Original, editable Blockbench rigs. Run export-blockbench.cjs for the editor's exports.
All coordinates are authored in Blockbench pixels (16 = one Minecraft block).
"""
from pathlib import Path
from PIL import Image,ImageDraw
import math,json,uuid,base64,io,re,random
ROOT=Path(__file__).resolve().parents[1];ASSET=ROOT/'src/main/resources/assets/wildhunt';SOURCE=ROOT/'art/blockbench'
for p in [SOURCE,ASSET/'geo/myths',ASSET/'animations/myths',ASSET/'textures/geo'] :p.mkdir(parents=True,exist_ok=True)
def write(p,d):p.write_text(json.dumps(d,indent=2)+'\n')
def uid(s):return str(uuid.uuid5(uuid.NAMESPACE_URL,'tensura-myths/'+s))
def polar(a,r,y=0):return [math.cos(a)*r,y,math.sin(a)*r]
THEMES={
 'storm':['292b43','182035','d9ac4d','c9f8ff','76648e','f2d293','586177','92723f'],
 'tide':['24485e','142939','cba98d','b5fff5','d88d9f','e0f2e7','49969e','e4c3b7'],
 'volcanic':['342436','140f20','b17957','ff9e4a','9e54ce','ded0bd','69445d','de6e3d'],
 'winter':['54859b','1e374d','9ddedc','efffff','80b8ec','dae7ef','6aa4b0','bdd6da'],
 'causal':['332a4a','0d1022','cda968','ffecb9','a78bff','e9dfce','5c4d88','aa8153'],
 'silk':['58406a','241b37','d1a478','f4c9ef','b58cdd','f3e9e2','936883','e4b6d4'],
 'rune':['6f4b37','262a33','d9b56d','ffe7aa','d1834f','ded9bb','736958','9a7547'],
 'grove':['456454','162e2e','bea069','d1ffd8','76b99a','eee9cc','689771','b2c584'],
 'mirror':['395778','192e49','becddc','e3f9ff','a4c1ee','dde2f4','719eab','c6b1dc'],
 'gate':['494164','18182e','b99a61','e6dcff','a18fdf','e4dfd6','626482','d9b37e'],
 'thoth':['254a5c','122737','c8a56c','a2f7ff','6b9fac','f1e5c7','51867d','dbc995'],
 'lugh':['40645b','182d31','cca565','fff0c5','a3dcca','eee6cb','6f9183','d1b47b'],
 'eschaton':['172434','0a101e','bdaf83','a5fff1','ff919c','e8eee3','3c6873','c6d9d7'],
 'ananke':['cdc7b9','161627','ac7e42','f3c477','9273be','e5ddca','77798b','d7b578'],
}
# Atlas tiles: dark enamel, recessed metal, engraved trim, luminous inlay,
# secondary luminous inlay, ivory/pages, brushed structural metal and edge bevel.
for theme,palette in THEMES.items():
 im=Image.new('RGBA',(256,128));em=Image.new('RGBA',im.size);rng=random.Random(theme)
 for tile,h in enumerate(palette):
  base=tuple(bytes.fromhex(h));ox=tile*32
  for y in range(128):
   for x in range(32):
    bevel=min(x,31-x,y%32,31-y%32);v=(.66 if bevel==0 else .86 if bevel<3 else 1)+rng.uniform(-.025,.025)+.035*math.sin(y*.8)
    rgb=tuple(max(0,min(255,int(c*v))) for c in base);im.putpixel((ox+x,y),(*rgb,165 if tile==0 and theme in ('winter','mirror') else 255))
    if tile in (3,4):em.putpixel((ox+x,y),(*rgb,255))
 d=ImageDraw.Draw(im)
 for tile in range(8):
  ox=tile*32;bright=tuple(min(255,int(c*1.17+9)) for c in bytes.fromhex(palette[tile]))
  for n in range(4):
   oy=n*32;d.rectangle((ox+3,oy+3,ox+28,oy+28),outline=bright)
   if tile in (2,3,4):
    d.line([(ox+8,oy+8),(ox+24,oy+8),(ox+24,oy+15),(ox+15,oy+15),(ox+15,oy+24),(ox+8,oy+24)],fill=bright,width=1)
    d.rectangle((ox+5,oy+13,ox+7,oy+18),fill=bright)
   elif tile==5:
    for line in range(7,27,4):d.line((ox+6,oy+line,ox+25-(line%3)*2,oy+line),fill=tuple(int(c*.75) for c in bright))
 im.save(ASSET/'textures/geo'/f'{theme}.png');em.save(ASSET/'textures/geo'/f'{theme}_glowmask.png')

class Rig:
 def __init__(self,name,theme):self.name=name;self.theme=theme;self.bones=[];self.anim={};self.motion={};self.bone('root')
 def bone(self,name,parent='root',pivot=None,rotation=None):
  b={'name':name,'pivot':pivot or [0,0,0]}
  if name!='root':b['parent']=parent
  if rotation:b['rotation']=rotation
  self.bones.append(b);return name
 def box(self,bone,at,size,mat=0,rot=None,pivot=None):
  c={'origin':[round(at[k]-size[k]/2,4) for k in range(3)],'size':list(size),'uv':{f:{'uv':[mat*32,0],'uv_size':[32,32]} for f in ['north','east','south','west','up','down']}}
  if rot:c.update(rotation=rot,pivot=pivot or list(at))
  next(b for b in self.bones if b['name']==bone).setdefault('cubes',[]).append(c)
 def surface(self,shape,parent,at,size,mat,rot=None):
  # The self-describing child bone is an animation anchor for SmoothGeoGeometry.
  # Its cubes are an editor preview, replaced by a continuous mesh at runtime.
  dims='_'.join(str(round(v*100)) for v in size)
  b=self.bone(f'surface_{shape}_{len(self.bones)}_{dims}_{mat}',parent,list(at),rot)
  return b
 def gem(self,bone,at,size,mat=3):
  b=self.surface('gem',bone,at,size,mat)
  for i in range(7):
   t=(i+.5)/7;f=max(.05,1-abs(t-.4)/.6)
   self.box(b,[at[0],at[1]+(t-.5)*size[1],at[2]],[size[0]*f,size[1]/7,size[2]*f],mat,[0,45,0])
 def leaf(self,bone,at,size,mat=6,rot=None):
  b=self.surface('leaf',bone,at,size,mat,rot)
  for i in range(8):
   t=(i+.5)/8;f=math.sin(t*math.pi)**.65
   self.box(b,[at[0],at[1]+(t-.5)*size[1],at[2]+math.sin(t*math.pi)*size[2]*.25],[size[0]*f,size[1]/8,size[2]*.18],mat)
 def bevel(self,bone,at,size,mat=0,rot=None):
  b=self.surface('box',bone,at,size,mat,rot);edge=min(size)*.16
  self.box(b,at,[size[0]-edge,size[1],size[2]-edge],mat)
  self.box(b,at,[size[0],size[1]-edge,size[2]-edge],mat)
  self.box(b,at,[size[0]-edge,size[1]-edge,size[2]],mat)
 def ring(self,name,r,y=0,segments=12,mat=2,parent='root',tilt=None,width=1.2):
  self.bone(name,parent,[0,y,0],tilt)
  b=self.surface('ring',name,[0,y,0],[r,width,width*.64],mat)
  for n in range(64):
   a=n*math.tau/64;self.box(b,polar(a,r,y),[width,width*.64,2*r*math.tan(math.pi/64)],mat,[0,-math.degrees(a),0])
  return name
 def spin(self,bone,axis=1,degrees=360):self.motion[bone]={'rotation':{'0.0':[0,0,0],'3.0':[degrees if axis==0 else 0,degrees if axis==1 else 0,degrees if axis==2 else 0]}}
 def sway(self,bone,axis=2,amount=8,phase=0):
  vals={}
  for t in [0,.75,1.5,2.25,3]:
   v=[0,0,0];v[axis]=round(math.sin(t/3*math.tau+phase)*amount,3);vals[str(t)]={'vector':v,'easing':'easeInOutSine'}
  self.motion[bone]={'rotation':vals}
 def save(self):
  geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+self.name,'texture_width':256,'texture_height':128,'visible_bounds_width':128,'visible_bounds_height':64,'visible_bounds_offset':[0,8,0]},'bones':self.bones}]}
  self.anim.setdefault('animation.myths.idle',{'loop':True,'animation_length':3,'bones':self.motion})
  moving=[b['name'] for b in self.bones if b['name']!='root' and not b['name'].startswith('surface_')][:8]
  self.anim.setdefault('animation.myths.appear',{'animation_length':.4,'bones':{b:{'scale':{'0':[.05,.05,.05],'.25':[1.08,1.08,1.08],'.4':[1,1,1]}} for b in moving}})
  write(ASSET/'geo/myths'/f'{self.name}.geo.json',geo);write(ASSET/'animations/myths'/f'{self.name}.animation.json',{'format_version':'1.8.0','geckolib_format_version':2,'animations':self.anim})
  # Native Blockbench project, per-face UVs, embedded original texture and bone keyframes.
  elements=[];groups={};texture=(ASSET/'textures/geo'/f'{self.theme}.png').read_bytes()
  for b in self.bones:
   group={'name':b['name'],'origin':b['pivot'],'rotation':b.get('rotation',[0,0,0]),'uuid':uid(self.name+'/'+b['name']),'children':[],'export':True,'isOpen':False}
   groups[b['name']]=group
   for i,c in enumerate(b.get('cubes',[])):
    id=uid(self.name+'/'+b['name']+'/'+str(i));group['children'].append(id)
    el={'name':b['name']+'_'+str(i),'uuid':id,'type':'cube','from':c['origin'],'to':[round(c['origin'][k]+c['size'][k],4) for k in range(3)],'origin':c.get('pivot',b['pivot']),'rotation':c.get('rotation',[0,0,0]),'box_uv':False,'autouv':0,'faces':{}}
    for f,u in c['uv'].items():el['faces'][f]={'uv':u['uv']+[u['uv'][0]+u['uv_size'][0],u['uv'][1]+u['uv_size'][1]],'texture':0}
    elements.append(el)
  for b in self.bones:
   if b.get('parent'):groups[b['parent']]['children'].append(groups[b['name']])
  animations=[]
  for name,a in self.anim.items():
   animators={}
   for bone,channels in a['bones'].items():
    keys=[]
    for channel,values in channels.items():
     for t,v in values.items():
      v=v.get('vector',v) if isinstance(v,dict) else v
      keys.append({'channel':channel,'data_points':[dict(zip(['x','y','z'],v))],'uuid':uid(self.name+name+bone+channel+t),'time':float(t),'interpolation':'catmullrom' if len(values)>2 else 'linear'})
    animators[groups[bone]['uuid']]={'name':bone,'type':'bone','keyframes':keys}
   animations.append({'name':name,'uuid':uid(self.name+name),'loop':'loop' if a.get('loop') else 'once','length':a['animation_length'],'snapping':40,'animators':animators})
  project={'meta':{'format_version':'5.0','model_format':'geckolib_model','box_uv':False},'name':self.name,'model_identifier':self.name,'geckolib_model_type':'Entity','resolution':{'width':256,'height':128},'elements':elements,'outliner':[groups['root']],'textures':[{'name':self.theme+'.png','uuid':uid('texture/'+self.theme),'id':'0','width':256,'height':128,'uv_width':256,'uv_height':128,'source':'data:image/png;base64,'+base64.b64encode(texture).decode()}],'animations':animations}
  write(SOURCE/f'{self.name}.bbmodel',project)
  return {'asset':self.name,'theme':self.theme,'bones':len(self.bones),'cubes':len(elements)}

def blossom(r,ice=False):
 for n in range(8):
  a=n*math.tau/8;b=r.bone('petal_'+str(n),pivot=[0,2,0],rotation=[0,-math.degrees(a),0]);r.leaf(b,[0,9,7],[4,17,2.8],6,[20,0,0]);r.leaf(b,[0,16,10],[5,10,2],5 if not ice else 4,[30,0,0]);r.gem(b,[0,19,11],[2,5,1.5],3);r.sway(b,0,12,n*.4)
 r.gem('root',[0,5,0],[4,9,4],3)
def loom(r):
 r.gem('root',[0,8,0],[4,12,4],4)
 for n in range(8):
  a=n*math.tau/8;r.bone('leg_'+str(n),pivot=[0,7,0],rotation=[0,-math.degrees(a),0]);b='leg_'+str(n);r.box(b,[0,8,7],[1.2,1.2,13],2,[25,0,0]);r.box(b,[0,5,13],[1,9,1],6,[30,0,0]);r.gem(b,[0,10,7],[2,3,2]);r.sway(b,2,5,n)
def book(r,name='book',at=None,parent='root'):
 at=at or [0,0,0];r.bone(name,parent,at)
 for s in [-1,1]:
  b=r.bone(name+('_left' if s<0 else '_right'),name,at,[0,s*23,0]);r.bevel(b,[at[0]+s*5,at[1],at[2]],[9.5,13,1],2);r.bevel(b,[at[0]+s*4.8,at[1],at[2]-.8],[8.5,11.5,.8],5);
  for line in range(4):r.box(b,[at[0]+s*4.8,at[1]+4-line*2.5,at[2]-1.4],[6-line%2*1.5,.25,.15],3)
  r.sway(b,1,4,s)
 r.box(name,at,[1,14,2],2)
def spear(r,name='spear',parent='root',at=None):
 at=at or [0,0,0];r.bone(name,parent,at)
 r.bevel(name,at,[1.4,20,1.4],2);r.gem(name,[at[0],at[1]+16,at[2]],[4,16,2],5)
 r.box(name,[at[0],at[1]+12,at[2]],[.7,9,.7],3)
 for s in [-1,1]:r.leaf(name,[at[0]+s*3,at[1]+10,at[2]],[1.6,8,1.4],4,[0,0,-s*25])
def tablet(r,name='tablet',at=None,parent='root'):
 at=at or [0,0,0];r.bone(name,parent,at);r.bevel(name,at,[14,18,1.8],2);r.bevel(name,[at[0],at[1],at[2]-1],[12,16,.4],1)
 for n in range(4):r.box(name,[at[0]-1+(n%2),at[1]+5-n*3,at[2]-1.4],[8-n%2*2,.5,.3],3)
 for s in [-1,1]:r.gem(name,[at[0]+s*6,at[1]+8,at[2]],[2,4,2],4)
 r.sway(name,2,4)

from author_eschaton import PROFILES as FINAL_PROFILES, effect as final_effect, author_boss, author_items

def effect(name,theme,kind):
 if kind.startswith('origin_'):return final_effect(Rig,name,theme,kind)
 r=Rig(name,theme)
 if kind in ['crystal','icefall','bulwark','bloom','garden','icegarden','briar','seed','rebirth','heal']:
  if kind in ['crystal','icefall']:
   for n in range(5):a=n*math.tau/5;at=polar(a,3,8);r.gem('root',at,[3,20-n%2*6,3],0 if n%2 else 3)
  elif kind in ['garden','icegarden']:
   for n in range(5):
    at=polar(n*math.tau/5,11,0);b=r.bone('tree_'+str(n),pivot=at);r.box(b,[at[0],12,at[2]],[2,24,2],6)
    for t in range(3):
     for leaf in range(4):r.leaf(b,[at[0]+math.cos(leaf*math.tau/4+t)*3,17+t*4,at[2]+math.sin(leaf*math.tau/4+t)*3],[4.5-t*.5,9-t,1.5],0,[30,leaf*90+t*57,35])
    r.gem(b,[at[0],29,at[2]],[3,7,3],3);r.sway(b,2,3,n)
  elif kind=='bulwark':
   for n in range(6):
    at=polar(n*math.tau/6,14,13);b=r.bone('shield_'+str(n),pivot=at,rotation=[0,-n*60,0]);r.bevel(b,at,[1.5,25,10],0);r.box(b,[at[0]+.9,at[1],at[2]],[.4,23,1],3);r.gem(b,[at[0],at[1]+13,at[2]],[2,8,4],4);r.sway(b,0,4,n)
  else:blossom(r,kind=='bloom')
 elif kind in ['mirror','shards','mirrorsea','lens']:
  count=3 if kind=='shards' else 6
  for n in range(count):
   at=polar(n*math.tau/count,13,4 if kind!='mirrorsea' else 18);b=r.bone('glass_'+str(n),pivot=at,rotation=[0,-n*360/count,0]);r.gem(b,at,[7,15,.6],0);r.box(b,[at[0],at[1],at[2]-.4],[.6,14,.4],3,[0,0,45]);r.sway(b,1,9,n)
 elif kind in ['book','library','lexicon']:
  r.ring('meridian',13,0,16,2,tilt=[48,0,0]);r.spin('meridian',1,90)
  for n in range(3 if kind!='book' else 1):
   at=polar(n*math.tau/3,13 if kind!='book' else 0,12 if kind!='lexicon' else 35);book(r,'volume_'+str(n),at)
 elif kind in ['tablet','synthesis','seal']:
  for n in range(3 if kind=='synthesis' else 1):tablet(r,'codex_'+str(n),polar(n*math.tau/3,10 if kind=='synthesis' else 0,20))
  r.ring('registry',14,5,12,2);r.spin('registry',1,-160)
 elif kind in ['spider','spindle','thread','loomstep']:
  loom(r)
  if kind!='spider':r.ring('bobbin',6,8,10,2,tilt=[90,0,0]);r.spin('bobbin',2)
 elif kind in ['spear','lance','crown','blade','sever','parallax','checkmate']:
  if kind in ['crown','checkmate']:
   for n in range(7 if kind=='crown' else 4):spear(r,'blade_'+str(n),at=polar(n*math.tau/(7 if kind=='crown' else 4),12,16))
   if kind=='crown':
    for b in r.bones:
     if b['name'].startswith('blade_'):b['rotation']=[180,0,0]
  elif kind=='parallax':
   for s in [-1,1]:spear(r,'verdict_'+str(s),at=[s*12,12,0])
  else:spear(r)
  r.ring('armature',5,0,8,2);r.spin('armature',1,180)
 elif kind in ['gate','crossroads','transit','shift']:
  count=4 if kind=='crossroads' else 1
  for n in range(count):
   at=polar(n*math.tau/count,12 if count>1 else 0,18);b=r.bone('gate_'+str(n),pivot=at,rotation=[90,0,n*90]);
   rim=r.surface('ring',b,at,[13,1.4,.9],2)
   for k in range(64):
    a=k*math.tau/64;r.box(rim,[at[0]+math.cos(a)*13,at[1],at[2]+math.sin(a)*13],[1.4,.9,2*13*math.tan(math.pi/64)],2,[0,-math.degrees(a),0])
   for k in range(8):
    a=k*math.tau/8;r.gem(b,[at[0]+math.cos(a)*13.5,at[1],at[2]+math.sin(a)*13.5],[1.4,2.8,1.4],4)
   r.sway(b,2,3,n)
 elif kind in ['hourglass','astrolabe','orrery','chronoburst','singularity','dawn','ascend']:
  if kind=='hourglass':
   for y in [2,26]:r.ring('rim_'+str(y),6,y,10,2)
   for s in [-1,1]:r.box('root',[s*5,14,0],[1,24,1],2)
   for y in [7,21]:r.gem('root',[0,y,0],[5,9,5],4)
  else:
   for n in range(3):r.ring('orbit_'+str(n),9+n*3,12,16,2 if n%2==0 else 4,tilt=[n*54,0,n*23]);r.spin('orbit_'+str(n),1,120 if n%2==0 else -120)
   if kind!='singularity':r.gem('root',[0,12,0],[5,13,5],1 if kind=='dawn' else 3)
 elif kind in ['wave','whirl','breaker','oil']:
  for n in range(8):
   a=n*math.tau/8;at=polar(a,13,3);b=r.bone('crest_'+str(n),pivot=at,rotation=[0,-math.degrees(a),0]);r.leaf(b,[at[0],7,at[2]-1],[3,14,4],0,[20,0,0]);r.leaf(b,[at[0],11,at[2]-3],[2,9,2],5 if kind!='oil' else 4,[35,0,0]);r.gem(b,[at[0],14,at[2]-5],[1.5,4,1.5],3);r.sway(b,0,12,n*.7)
 elif kind in ['lash']:
  parent='root'
  for n in range(10):
   at=[0,0,n*2];b=r.bone('tendon_'+str(n),parent,at);r.box(b,[0,3,n*2+1],[.6,1,1.8],3);r.gem(b,[0,-2,n*2+1],[1,3,1],4);r.sway(b,1,4,n*.55)
 elif kind in ['rail','plasma','step','prism','muzzle']:
  for n in range(3):r.ring('conductor_'+str(n),4+n*1.2,n*3,10,2 if n%2 else 3);r.spin('conductor_'+str(n),1,240 if n%2 else -240)
  for n in range(4):
   b=r.bone('fin_'+str(n),rotation=[0,n*90,0]);r.bevel(b,[5,6,0],[1.5,14,2],0);r.box(b,[5.8,6,0],[.4,11,1],3)
 elif kind in ['mine','burst','eruption','stormcloud','crucible','spellbreak','impact']:
  r.ring('base',12,1,12,2);r.spin('base',1,90)
  for n in range(6):
   at=polar(n*math.tau/6,10,6);b=r.bone('pylon_'+str(n),pivot=at);r.bevel(b,at,[3,10,3],0);r.gem(b,[at[0],13,at[2]],[2,7,2],3);r.sway(b,2,5,n)
  if kind=='crucible':r.ring('collector',8,27,12,4);r.spin('collector',1,-240)
 else:raise ValueError(kind)
 return r

# Each family has its own silhouette and articulated rig, not a single recolored effect mesh.
PROFILES=[
(0,'oil_lash','volcanic','lash','lash'),(1,'glacial_spire','winter','crystal','crystal'),(2,'falling_icicle','winter','icefall','fall'),(3,'tidal_crest','tide','wave','wave'),(4,'coral_vortex','tide','whirl','radial'),
(5,'causal_astrolabe','causal','astrolabe','radial'),(6,'worldline_blade','causal','sever','beam'),(7,'event_horizon_orrery','causal','orrery','horizon'),(8,'causal_hourglass','causal','hourglass','anchor'),(9,'singularity_armature','causal','singularity','singularity'),(10,'chronofracture','causal','chronoburst','radial'),
(11,'silken_spindle','silk','spindle','beam'),(12,'runic_igniter','rune','mine','radial'),(13,'living_grove','grove','garden','radial'),(14,'prism_guard','mirror','mirror','follow'),(15,'prismatic_emitter','mirror','prism','beam'),(16,'plasma_capacitor','volcanic','plasma','beam'),(17,'storm_engine','volcanic','stormcloud','sky'),(18,'volcanic_fault','volcanic','eruption','radial'),(19,'winter_orchard','winter','icegarden','radial'),(20,'crystal_aegis','winter','bulwark','follow'),(21,'glacial_blossom','winter','bloom','radial'),(22,'threshold_gate','gate','gate','gate'),(23,'parallax_judges','gate','parallax','radial'),(24,'crossroads','gate','crossroads','radial'),
(25,'lexicon_volumes','thoth','lexicon','radial'),(26,'spellbreak_obelisks','thoth','spellbreak','radial'),(27,'mana_alembic','thoth','crucible','follow'),(28,'astral_volumes','thoth','library','library'),(29,'spectrum_emitter','thoth','muzzle','beam'),(30,'briar_corolla','grove','briar','radial'),(31,'runic_detonator','rune','burst','radial'),(32,'mirror_splinters','mirror','shards','radial'),(33,'fulgur_conductor','storm','rail','beam'),(34,'thunderstep_armature','storm','step','beam'),(35,'elemental_detonation','storm','impact','radial'),(36,'obsidian_vents','volcanic','oil','radial'),(37,'vital_corolla','grove','heal','follow'),(38,'gate_wake','gate','transit','beam'),
(39,'fate_loom','silk','spider','anchor'),(40,'fate_spindle','silk','thread','beam'),(41,'loom_wake','silk','loomstep','beam'),(42,'checkmate_blades','silk','checkmate','edge'),(43,'mirrorsea_lenses','tide','mirrorsea','radial'),(44,'refracted_harpoon','tide','spear','tip'),(45,'sovereign_crest','tide','breaker','breaker'),(46,'refraction_lenses','tide','lens','radial'),(47,'perennial_corolla','grove','seed','marker'),(48,'rebirth_lotus','grove','rebirth','radial'),(49,'skill_bank_codex','lugh','tablet','radial'),(50,'artisan_spindle','lugh','thread','beam'),(51,'countermand_tablet','lugh','seal','marker'),(52,'grand_synthesis_codices','lugh','synthesis','radial'),(53,'reconstructed_sword','lugh','blade','blade'),(54,'reconstructed_lance','lugh','lance','tip'),(55,'reconstructed_crown','lugh','crown','radial'),
(56,'fatebreaker_armature','ananke','blade','bossfan'),(57,'starfall_armature','ananke','spear','stars'),(58,'procession_censer','ananke','crucible','procession'),(59,'last_dawn_orrery','ananke','dawn','dawn'),(60,'seraph_ascension','ananke','ascend','ascend'),(62,'seraph_threshold','ananke','shift','anchor')]
PROFILES += FINAL_PROFILES
manifest=[]
for id,name,theme,shape,placement in PROFILES:
 row=effect(name,theme,shape).save();row.update(kind=id,shape=shape,placement=placement);manifest.append(row)
# Style-specific impact palettes are distinct native damage families.
for theme in ['tide','volcanic']:effect('elemental_detonation_'+theme,theme,'impact').save()

# Six-winged original Eclipse Seraph: layered armor, articulated flight feathers,
# recessed faceplate, suspended tabards and phase-specific crown vanes.
r=Rig('ananke','ananke');r.bone('body',pivot=[0,31,0]);r.box('body',[0,35,0],[13,17,6],1)
for n in range(4):r.bevel('body',[0,40-n*4,-3.4],[14-n*.7,4,2],0,[8,0,0]);r.box('body',[0,40-n*4,-4.5],[2,3,.6],2)
r.gem('body',[0,37,-5.1],[3,8,1.4],4);r.box('body',[0,27,0],[15,3,8],2)
for s in [-1,1]:
 b=r.bone('arm_'+str(s),'body',[s*9,41,0]);r.bevel(b,[s*10,40,0],[7,6,8],2,[0,0,s*10]);r.box(b,[s*11,34,0],[4,10,4],1);r.bevel(b,[s*11,29,-.3],[5,7,5],0);r.box(b,[s*11,26,-.3],[4,3,4],2);r.gem(b,[s*11,31,-3],[2,5,1],4)
 r.bone('leg_'+str(s),'root',[s*4,25,0]);b='leg_'+str(s);r.box(b,[s*4,14,0],[5,22,5],1);r.bevel(b,[s*4,7,0],[5.5,12,6],0);r.bevel(b,[s*4,2,-1.4],[6,4,8],2)
for n in range(7):
 b=r.bone('tabard_'+str(n),'body',[(n-3)*2.4,28,4],rotation=[-8,0,(n-3)*7]);r.box(b,[(n-3)*2.4,17,4],[2.8,22-n%2*3,.9],0);r.box(b,[(n-3)*2.4,7+n%2*1.5,4],[3,2,1.3],2);r.sway(b,0,4,n*.5)
r.bone('head','body',[0,46,0]);r.bevel('head',[0,50,0],[9,11,8],0);r.box('head',[0,49,-4.3],[7,8,1],1);r.box('head',[0,46,-4.8],[4,2,1.2],2)
for s in [-1,1]:r.box('head',[s*1.8,51,-5],[2.4,.65,.5],4,[0,0,s*8]);r.box('head',[s*4,53,-.5],[2,6,9],2);r.box('head',[s*4,58,-.5],[1.5,6,2],2,[0,0,-s*14])
r.gem('head',[0,56,-3],[2,6,1.5],3);r.ring('crown_halo',12,58,24,2,'body',tilt=[75,0,0],width=.7);r.spin('crown_halo',1,45)
for tier in range(3):
 for s in [-1,1]:
  n=tier*2+(s==1);b=r.bone('wing_'+str(n),'body',[s*5,40-tier*5,3],rotation=[-10,s*10,s*(tier-1)*34]);r.box(b,[s*18,40-tier*5,4],[27,2,3],2)
  for f in range(9):
   x=s*(9+f*3);y=40-tier*5;fb=r.bone('feather_'+str(n)+'_'+str(f),b,[x,y,4],rotation=[0,0,s*(22+f*.8)]);length=19-f*.85;r.leaf(fb,[x,y-length/2,4],[2.5,length,1.2],0);r.box(fb,[x,y-length/2,3.2],[.65,length+.8,.6],2);r.gem(fb,[x,y-length+1,3.5],[1.3,4,1],4)
  r.sway(b,2,6,tier*.6);r.motion[b]['position']={'0':[0,0,0],'1.5':[0,1.2,0],'3':[0,0,0]}
for phase in [2,3]:
 b=r.bone('phase_'+str(phase),'head',[0,57,0]);
 for s in [-1,1]:r.box(b,[s*(phase+2),59+(phase-2)*3,1],[1.5,9,2],4,[0,0,-s*20])
r.motion['body']={'position':{'0':[0,0,0],'1.5':[0,1,0],'3':[0,0,0]}};r.anim['animation.myths.idle']={'loop':True,'animation_length':3,'bones':r.motion}
for mode in range(1,5):
 bones={}
 for s in [-1,1]:bones['arm_'+str(s)]={'rotation':{'0':[0,0,0],'.35':{'vector':[-35-mode*18,s*8,s*(12+mode*5)],'easing':'easeOutCubic'},'1.3':[-32-mode*18,s*6,s*(12+mode*5)],'1.8':[0,0,0]}}
 r.anim['animation.ananke.cast_'+str(mode)]={'loop':True,'animation_length':1.8,'bones':bones}
r.anim['animation.ananke.flight']={'loop':True,'animation_length':1.5,'bones':{'wing_'+str(n):{'rotation':{'0':[0,0,-10 if n%2==0 else 10],'.75':[0,0,12 if n%2==0 else -12],'1.5':[0,0,-10 if n%2==0 else 10]}} for n in range(6)}}
r.anim['animation.ananke.ascend']={'animation_length':1.8,'bones':{'body':{'position':{'0':[0,0,0],'.7':[0,6,0],'1.8':[0,0,0]}},**{'wing_'+str(n):{'rotation':{'0':[0,0,0],'.5':[0,0,-24 if n%2==0 else 24],'1.8':[0,0,0]}} for n in range(6)}}}
r.save()
# Native NPC bodies/equipment remain native; new animated original accessories.
for who,theme in [('alma','rune'),('gemma','storm')]:
 r=Rig(who+'_equipment',theme);r.bone('pack',pivot=[0,20,3]);r.bevel('pack',[0,19,4.8],[7,9,3],0);r.box('pack',[0,23,6.5],[8,2,1],2);r.box('pack',[0,18,6.5],[.7,7,.5],2)
 if who=='alma':
  book(r,'journal',[5,18,3],'pack');r.box('pack',[-5,18,3],[2,10,2],5);r.box('pack',[-5,23,3],[2.5,1,2.5],2)
 else:
  for n in range(3):r.box('pack',[n*2-2,24,4],[1,8,1],2);r.box('pack',[n*2-2,28,4],[2.5,1,1.5],6)
 r.sway('pack',2,2);r.save()
author_boss(Rig)
write(SOURCE/'manifest.json',{'schema':2,'effects':manifest,'boss':'ananke','final_boss':'eschaton','weak_points':['genesis_seal'],'otherworlders':['alma_equipment','gemma_equipment'],'custom_optics':['bone-attached continuous rings, facets, curved leaves and beveled solids','singularity horizons and stable disks','fluid sheets and caustics','beams and trails','exact boss damage warnings']})
print(f'Authored {len(PROFILES)} effect rigs, 2 style variants, 2 bosses, 1 weak point and 2 NPC accessories.')

rows=[f'        new VisualRig({id},"{name}","{theme}",Placement.{placement.upper()})' for id,name,theme,shape,placement in PROFILES]
java='package dev.wildhunt.visual;\n\n/** Authored rig routing. IDs match the existing synchronized visual protocol. */\npublic record VisualRig(int kind,String asset,String theme,Placement placement) {\n    public enum Placement {'+','.join(sorted({p[4].upper() for p in PROFILES}))+'}\n    private static final VisualRig[] ALL={\n'+',\n'.join(rows)+'\n    };\n    public static VisualRig forKind(int kind){for(var rig:ALL)if(rig.kind==kind)return rig;return null;}\n    public static java.util.List<VisualRig> all(){return java.util.List.of(ALL);}\n}\n'
(ROOT/'src/main/java/dev/wildhunt/visual/VisualRig.java').write_text(java)

# Static ritual items use Blockbench's Java-item exporter; no animation controller
# is needed for an ordinary held scroll/book. Spawn eggs retain vanilla rendering.
(SOURCE/'items').mkdir(exist_ok=True)
for item in ['writ_of_last_dawn','dawn_testament']:
 r=Rig(item,'ananke')
 if item=='writ_of_last_dawn':
  r.box('root',[8,8,8],[6,11,1.2],5)
  for y in [2.5,13.5]:r.box('root',[8,y,8],[8,1.5,2],2);r.box('root',[8,y,8],[6,.7,2.5],0)
  r.box('root',[8,8,7.2],[1,6,.4],4)
  for x in [6,10]:r.box('root',[x,8,7.1],[.3,6,.3],2)
 else:
  r.box('root',[8,8,8],[9,12,3],5)
  for z in [6.3,9.7]:r.box('root',[8,8,z],[10,13,.6],1);r.box('root',[8,8,z],[8.5,11.5,.75],2)
  r.box('root',[3.2,8,8],[1,13,4],2);r.box('root',[8,8,5.7],[3.5,3.5,.6],4,[0,0,45])
 r.save();p=SOURCE/f'{item}.bbmodel';d=json.loads(p.read_text());d['meta']['model_format']='java_block';d['animations']=[];d['textures'][0].update(namespace='wildhunt',folder='item',name='myths_material.png')
 d['display']={'gui':{'rotation':[20,-30,0],'translation':[0,0,0],'scale':[.9,.9,.9]},'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.65,.65,.65]},'firstperson_righthand':{'rotation':[0,-20,0],'translation':[0,2,0],'scale':[.65,.65,.65]}}
 write(SOURCE/'items'/p.name,d);p.unlink()
 for folder,suffix in [('geo','.geo.json'),('animations','.animation.json')]: (ASSET/folder/'myths'/f'{item}{suffix}').unlink()

(ASSET/'textures/item/myths_material.png').write_bytes((ASSET/'textures/geo/ananke.png').read_bytes())

author_items(Rig)
