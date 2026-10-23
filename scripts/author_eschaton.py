"""Original final-Myth art, composed from editable articulated bones and curved surfaces."""
from pathlib import Path
import math,json
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1];ASSET=ROOT/'src/main/resources/assets/wildhunt';SOURCE=ROOT/'art/blockbench'
PROFILES=[(63,'origin_execution','eschaton','origin_edge','finale'),(64,'origin_star_loom','eschaton','origin_loom','finale'),(65,'origin_sevenfold','eschaton','origin_genesis','finale'),(66,'origin_silent_scripture','eschaton','origin_cuts','finale'),(67,'origin_serpent_wake','eschaton','origin_wake','finale'),(68,'origin_exchange','eschaton','origin_exchange','finale'),(69,'origin_thronefall','eschaton','origin_dive','finale'),(70,'origin_apocalypse','eschaton','origin_apocalypse','finale'),(71,'origin_apotheosis','eschaton','origin_ascend','finale'),(72,'origin_star_shatter','eschaton','origin_shatter','finale_shatter')]
def crescent(r,name,parent,at,radius,width=2,thickness=1.2,rot=None,mat=2):
 r.bone(name,parent,at,rot);b=r.surface('crescent',name,at,[radius,width,thickness],mat)
 for n in range(56):
  a=math.radians(-140+(n+.5)*280/56);t=(n+.5)/56;w=width*math.sin(t*math.pi)**.65
  r.box(b,[at[0]+math.cos(a)*radius,at[1],at[2]+math.sin(a)*radius],[max(.05,w),thickness,2*radius*math.tan(math.radians(280/56)/2)],mat,[0,-math.degrees(a),0])
 return name

def effect(Rig,name,theme,kind):
 r=Rig(name,theme)
 if kind in ('origin_edge','origin_dive'):
  r.bone('regalia',pivot=[0,12,0],rotation=[0,0,kind=='origin_edge' and 30 or 0]);r.gem('regalia',[0,20,0],[6,48,3],5);r.box('regalia',[0,21,-1.8],[.55,37,.4],3)
  for s in [-1,1]:r.leaf('regalia',[s*5,2,0],[2,18,2],2,[0,0,-s*55])
  crescent(r,'edge_guard','regalia',[0,2,0],7,1.3,.8,[90,0,0]);r.sway('regalia',2,6)
 elif kind=='origin_genesis':
  for n in range(7):
   r.bone('star_'+str(n),pivot=[0,8,0]);r.gem('star_'+str(n),[0,8,0],[5,16,5],3);crescent(r,'star_rail_'+str(n),'star_'+str(n),[0,8,0],6,.6,.5,[n*23,0,0]);r.spin('star_rail_'+str(n),1,180 if n%2 else -180)
 elif kind in ('origin_loom','origin_apocalypse','origin_ascend'):
  center=[0,12,0];r.bone('origin',pivot=center);r.gem('origin',center,[7,18,7],4 if kind=='origin_apocalypse' else 3)
  for n in range(3):crescent(r,'meridian_'+str(n),'origin',center,14+n*4,1.25,.8,[45+n*43,n*17,20]);r.spin('meridian_'+str(n),1,90 if n%2 else -90)
  if kind=='origin_apocalypse':
   for n in range(3):
    a=n*math.tau/3;r.bone('seal_effigy_'+str(n),pivot=[math.cos(a)*20,0,math.sin(a)*20]);r.gem('seal_effigy_'+str(n),[math.cos(a)*20,7,math.sin(a)*20],[2,10,2],3)
 elif kind=='origin_cuts':
  for n in range(3):
   r.bone('scripture_'+str(n),pivot=[(n-1)*18,15,0]);r.bevel('scripture_'+str(n),[(n-1)*18,15,0],[6,25,1.2],0)
   for y in [5,11,17,23]:r.box('scripture_'+str(n),[(n-1)*18,y,-.8],[3,.5,.4],4)
   r.sway('scripture_'+str(n),1,8,n)
 elif kind=='origin_wake':
  for n in range(9):
   a=n*math.tau/9;at=[math.cos(a)*14,4,math.sin(a)*14];b=r.bone('serpent_rib_'+str(n),pivot=at,rotation=[0,-math.degrees(a),0]);r.leaf(b,[at[0],13,at[2]],[4,24,2],5,[35,0,0]);r.gem(b,[at[0],25,at[2]-5],[2,6,2],3);r.sway(b,0,10,n*.5)
 elif kind=='origin_exchange':
  r.gem('root',[0,18,0],[8,20,8],3);crescent(r,'siphon','root',[0,18,0],13,1.6,1.1,[90,0,0]);r.spin('siphon',2,180)
  for n in range(6):
   a=n*math.tau/6;at=[math.cos(a)*11,5,math.sin(a)*11];r.gem('root',at,[2,12,2],2)
 elif kind=='origin_shatter':
  for n in range(12):
   a=n*2.399963;b=r.bone('fragment_'+str(n),pivot=[0,0,0],rotation=[n*21,n*37,0]);r.gem(b,[0,6+n%3*3,0],[1.5,7,1.5],3 if n%2 else 5);r.sway(b,2,18,n)
 else:raise ValueError(kind)
 return r

def author_boss(Rig):
 r=Rig('eschaton','eschaton');r.bone('body',pivot=[0,57,0]);r.bevel('body',[0,67,0],[17,26,9],0)
 for n in range(5):
  y=77-n*5;r.bevel('body',[0,y,-4.8],[18-n*.7,4.8,2.1],5,[8,0,0]);r.box('body',[0,y,-6],[2.2,3.1,.45],2)
 r.bone('heart','body',[0,72,-6.4]);r.gem('heart',[0,72,-6.4],[5,12,3],4);r.box('body',[0,53,0],[20,4,12],2)
 # Legs, sculpted sabatons and independently moving coat tails.
 for s in [-1,1]:
  b=r.bone('leg_'+str(s),'root',[s*5.5,51,0]);r.box(b,[s*5.5,30,0],[6.5,38,6.5],1);r.bevel(b,[s*5.5,18,0],[7.5,27,8],5);r.bevel(b,[s*5.5,5,-2],[8,8,13],2);r.gem(b,[s*5.5,24,-4.5],[2,12,1.5],3)
 for n in range(9):
  a=(n-4)*.38;x=math.sin(a)*10;z=math.cos(a)*5+3;b=r.bone('mantle_cloth_'+str(n),'body',[x,54,z],rotation=[-10,0,(n-4)*4]);r.leaf(b,[x,29,z+5],[5.5,48-n%2*6,3],0,[10,0,0]);r.leaf(b,[x,24,z+4],[1.2,42-n%2*6,1],2,[10,0,0]);r.sway(b,0,4,n*.4)
 # Four anatomical arms: the lower pair unfolds in the second form.
 r.bone('ruin_arms','body',[0,63,1])
 for tier in range(2):
  for s in [-1,1]:
   parent='body' if tier==0 else 'ruin_arms';y=80-tier*16;x=s*(13-tier);b=r.bone('arm_'+str(tier)+'_'+str(s),parent,[x,y,0],rotation=[tier*12,0,s*(10+tier*12)]);
   r.bevel(b,[x,y,0],[10,9,11],2);r.leaf(b,[x+s*2,y+4,0],[7,16,5],5,[0,0,-s*25]);r.box(b,[x,y-11,0],[5.5,20,5.5],1)
   fore=r.bone('forearm_'+str(tier)+'_'+str(s),b,[x,y-20,0],rotation=[-8,0,0]);r.bevel(fore,[x,y-28,-1],[7.5,17,8],5 if tier==0 else 6);r.gem(fore,[x,y-27,-5.3],[2.5,10,1.5],3 if tier==0 else 4)
   hand=r.bone('hand_'+str(tier)+'_'+str(s),fore,[x,y-37,-1]);r.bevel(hand,[x,y-37,-1],[7,7,6],1)
   for f in range(3):r.leaf(hand,[x+(f-1)*2.2,y-44,-2],[1.5,10,2],5,[-15,0,0])
 # The creator's suspended greatblade; it follows the primary right hand.
 r.bone('worldblade','hand_0_1',[13,37,-1],rotation=[0,0,-12]);r.gem('worldblade',[13,14,-1],[8,49,3.2],5);r.box('worldblade',[13,15,-2.8],[.7,41,.4],3)
 for s in [-1,1]:r.leaf('worldblade',[13+s*6,37,-1],[2,18,2],2,[0,0,-s*65])
 # Crowned, recessed face: no borrowed skin or native boss model.
 r.bone('head','body',[0,84,0]);r.bevel('head',[0,92,0],[12,15,11],5);r.bevel('head',[0,91,-5.7],[9,11,1.1],1)
 for s in [-1,1]:r.box('head',[s*2.4,94,-6.4],[2.7,.8,.5],3,[0,0,s*9]);r.leaf('head',[s*6,101,0],[3.2,23,3],5,[0,0,-s*18]);r.leaf('head',[s*7,97,1],[2.1,18,2.1],2,[0,0,-s*32])
 r.gem('head',[0,101,-3],[2.5,9,2],4);r.leaf('head',[0,84,-5.5],[4,7,1.8],2,[0,0,0]);r.bone('crown','head',[0,104,0])
 for n in range(9):
  a=n*math.tau/9;r.gem('crown',[math.cos(a)*8,107+2*math.cos(a),math.sin(a)*8],[1.7,10+n%2*3,1.7],2 if n%2 else 3)
 r.sway('crown',1,7)
 # Crescent architecture replaces the seraph silhouette; each edge is a continuous tapered mesh.
 r.bone('ruin_mantle','body',[0,74,6])
 for n in range(4):
  s=-1 if n%2==0 else 1;tier=n//2;parent='body' if tier==0 else 'ruin_mantle';at=[s*(17+tier*3),75-tier*15,9+tier*3];name='crescent_'+str(n)
  crescent(r,name,parent,at,27-tier*3,2.1,1.5,[75,s*24,s*(25+tier*35)],2);r.sway(name,2,5,n*.6)
 # A long articulated, curved tail: ten rooted segments, no rectangular body shell.
 parent='root'
 for n in range(10):
  a=n*.21;at=[math.sin(a)*16,21+n*2.1,6+n*2.6];b=r.bone('tail_'+str(n),parent,at,rotation=[-9,0,-n*2]);r.leaf(b,[at[0]+1.3,at[1]+2,at[2]+2],[4.8-n*.32,9-n*.35,4-n*.25],0,[60,0,20]);r.gem(b,[at[0],at[1]+4,at[2]],[1.3,5,1.3],3);r.sway(b,1,3,n*.4);parent=b
 r.bone('last_crown','head',[0,102,0])
 for n in range(7):
  a=n*math.tau/7;b=r.bone('last_blade_'+str(n),'last_crown',[math.cos(a)*14,102,math.sin(a)*14],rotation=[0,0,n*360/7]);r.gem(b,[math.cos(a)*14,115,math.sin(a)*14],[2.2,25,2.2],4)
 r.motion['body']={'position':{'0':[0,0,0],'1.5':[0,1.4,0],'3':[0,0,0]}}
 for tier in range(2):
  for s in [-1,1]:r.sway('arm_'+str(tier)+'_'+str(s),0,3,s*.8+tier)
 r.anim['animation.myths.idle']={'loop':True,'animation_length':3,'bones':r.motion}
 def clip(name,length,bones,loop=True):r.anim['animation.eschaton.'+name]={'loop':loop,'animation_length':length,'bones':bones}
 clip('advance',1.6,{'body':{'rotation':{'0':[6,0,0],'.8':[8,0,0],'1.6':[6,0,0]}},**{'leg_'+str(s):{'rotation':{'0':[s*14,0,0],'.8':[-s*14,0,0],'1.6':[s*14,0,0]}} for s in [-1,1]}})
 clip('conjure',2.4,{'arm_'+str(t)+'_'+str(s):{'rotation':{'0':[0,0,0],'.5':[-65-t*10,s*15,s*25],'1.6':[-63-t*10,s*12,s*26],'2.4':[0,0,0]}} for t in range(2) for s in [-1,1]})
 clip('apocalypse',11.9,{'body':{'position':{'0':[0,0,0],'1.5':[0,5,0],'9.5':[0,5,0],'9.8':[0,2,0],'10':[0,8,0],'10.5':[0,3,0],'11.9':[0,0,0]},'rotation':{'0':[0,0,0],'1.5':[-8,0,0],'9.5':[-8,0,0],'9.8':[12,0,0],'10':[-14,0,0],'10.5':[5,0,0],'11.9':[0,0,0]}},**{'arm_'+str(t)+'_'+str(s):{'rotation':{'0':[-20,0,s*10],'1.2':[-125+t*25,s*20,s*(40+t*10)],'9.4':[-125+t*25,s*20,s*(40+t*10)],'9.8':[-155+t*20,s*10,s*15],'10':[-75,s*15,s*85],'10.5':[-40,s*25,s*45],'11.9':[-20,0,s*10]}} for t in range(2) for s in [-1,1]}})
 clip('edge',3.2,{'body':{'rotation':{'0':[0,0,0],'.9':[-8,-25,-5],'1.2':[8,30,5],'1.7':[-6,35,3],'2':[8,-25,-5],'2.5':[-12,-30,-7],'2.8':[12,35,6],'3.2':[0,0,0]}},'forearm_0_1':{'rotation':{'0':[-8,0,0],'.9':[-55,0,0],'1.2':[8,0,0],'1.7':[-40,0,0],'2':[12,0,0],'2.5':[-60,0,0],'2.8':[15,0,0],'3.2':[-8,0,0]}},'arm_0_1':{'rotation':{'0':[-35,0,-30],'.85':[-100,-25,-80],'1.2':[15,35,70],'1.7':[-65,45,70],'2.0':[10,-35,-65],'2.5':[-110,-15,-70],'2.8':[20,30,70],'3.2':[-35,0,-30]}}})
 clip('thronefall',2.4,{'body':{'rotation':{'0':[-10,0,0],'1.1':[-18,0,0],'1.8':[25,0,0],'2.4':[0,0,0]}},'arm_0_1':{'rotation':{'0':[-140,0,-10],'1.3':[-150,0,-15],'1.8':[20,0,20],'2.4':[0,0,0]}}})
 clip('stagger',2,{'body':{'rotation':{'0':[15,0,0],'1':[17,0,0],'2':[15,0,0]}},'head':{'rotation':{'0':[15,0,0],'1':[18,0,0],'2':[15,0,0]}}})
 clip('unfold',3.2,{'body':{'position':{'0':[0,0,0],'1.2':[0,12,0],'2.3':[0,8,0],'3.2':[0,0,0]}},**{'arm_'+str(t)+'_'+str(s):{'rotation':{'0':[0,0,0],'1.4':[-45,s*20,s*60],'3.2':[0,0,0]}} for t in range(2) for s in [-1,1]}},False)
 boss=r.save()
 r=Rig('genesis_seal','eschaton');r.bone('seed',pivot=[0,24,0]);r.gem('seed',[0,24,0],[11,34,11],3);r.spin('seed',1,90)
 for n in range(3):crescent(r,'ward_'+str(n),'root',[0,24,0],20+n*2,1.4,1,[60+n*37,0,n*22]);r.spin('ward_'+str(n),1,120 if n%2 else -120)
 for n in range(4):
  a=n*math.tau/4;r.gem('root',[math.cos(a)*10,5,math.sin(a)*10],[3,14,3],5)
 r.save();icons();return boss

def icons():
 for folder,name in [('skill','demiurge'),('race','origin_sovereign'),('item','origin_fragment')]:
  im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
  if name=='origin_fragment':d.polygon([(32,4),(45,24),(39,48),(26,60),(19,35)],fill='#223543',outline='#dbffff',width=2);d.polygon([(32,4),(31,35),(26,60),(39,48),(45,24)],fill='#71d8ce');d.line([(32,8),(31,35),(39,46)],fill='#f4ffff',width=2)
  else:
   d.polygon([(8,31),(5,12),(17,20),(20,6),(27,19),(32,2),(37,19),(44,6),(47,20),(59,12),(56,31)],fill='#263345',outline='#c3c6ae',width=2)
   d.polygon([(14,28),(20,52),(32,61),(44,52),(50,28)],fill='#d9e3da',outline='#314657',width=2);d.polygon([(20,32),(44,32),(41,50),(32,55),(23,50)],fill='#111b29');d.rectangle((23,36,28,38),fill='#aaffee');d.rectangle((36,36,41,38),fill='#aaffee');d.polygon([(32,20),(36,27),(32,34),(28,27)],fill='#ff8d9a');d.line((20,29,44,29),fill='#f4e4bb',width=2)
  target=ASSET/'textures'/folder/(name+'.png');target.parent.mkdir(parents=True,exist_ok=True);im.save(target)
 (ASSET/'models/item/origin_fragment.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'wildhunt:item/origin_fragment'}},indent=2)+'\n')

def author_items(Rig):
 for item in ['last_convergence','crown_of_the_last_myth']:
  r=Rig(item,'eschaton')
  if item=='last_convergence':
   r.box('root',[8,8,8],[8,12,2],1);r.box('root',[8,8,6.8],[6,10,.4],5)
   for x in [4,12]:r.box('root',[x,8,8],[1.2,14,3],2)
   for y in [2,14]:r.box('root',[8,y,8],[11,1.5,3],2)
   r.box('root',[8,8,6.4],[3,3,.5],3,[0,0,45]);r.box('root',[8,8,6.1],[1.2,6,.25],4)
  else:
   for z in [4,12]:r.box('root',[8,6,z],[12,3,1.5],2)
   for x in [2,14]:r.box('root',[x,6,8],[1.5,3,8],2)
   for x in [3,6,9,12]:
    for z in [4,12]:r.box('root',[x,9.5+(x%2),z],[1.5,6+(x%2)*2,1.5],5,[0,0,0])
   r.box('root',[8,7,3],[3,3,1],3,[0,0,45])
  r.save();p=SOURCE/f'{item}.bbmodel';d=json.loads(p.read_text());d['meta']['model_format']='java_block';d['animations']=[];d['textures'][0].update(namespace='wildhunt',folder='item',name='origin_material.png');d['display']={'gui':{'rotation':[20,-30,0],'translation':[0,0,0],'scale':[.8,.8,.8]},'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.5,.5,.5]},'firstperson_righthand':{'rotation':[0,-20,0],'translation':[0,2,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.6,.6,.6]}}
  (SOURCE/'items'/p.name).write_text(json.dumps(d,indent=2)+'\n');p.unlink()
  for folder,suffix in [('geo','.geo.json'),('animations','.animation.json')]:(ASSET/folder/'myths'/f'{item}{suffix}').unlink()
 (ASSET/'textures/item/origin_material.png').write_bytes((ASSET/'textures/geo/eschaton.png').read_bytes())
