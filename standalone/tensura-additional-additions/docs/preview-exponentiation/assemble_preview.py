#!/usr/bin/env python3
"""Animated reference, not replacement gameplay: every row comes from native runtime getters."""
from pathlib import Path
import json,zipfile,subprocess,math
from PIL import Image,ImageDraw,ImageFont
R=Path(__file__).resolve().parents[4];P=Path(__file__).resolve().parents[2]
rows=json.loads((P/'docs/preview-exponentiation/measurements-1.8.1.json').read_text());rows=[r for r in rows if r['can_change']]
assert len(rows)==55 and all('before' in r and 'after' in r for r in rows)
lang={}
for jar in [Path('/workspace/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar'),*Path('/workspace/.gradle/caches/modules-2/files-2.1/curse.maven/tensura-reincarnated-643695/8793360').rglob('*.jar')]:
 with zipfile.ZipFile(jar) as z:
  for n in z.namelist():
   if n.endswith('/lang/en_us.json'):lang.update(json.loads(z.read(n)))
def label(r):return lang.get(r['name'],r['id'].split(':')[1].replace('generic.','').replace('player.','').replace('_',' ').title())
rows.sort(key=lambda r:r['id'])
# Tested final outputs, distinct from stat formula inputs.
outputs=[('Completed HP / SHP damage',10,100),('HP / SHP healing',5,25),('Aura / magicule recovery',6,36),('Sleep recovery (combined pulse)',16,256),('Learning / mastery award',3,9),('Positive XP award',3,9),('Final drop-stack quantity',5,25),('Native pool award (each EP share)',3,9),('Completed mining speed',8,64)]
slides=[('EXPONENTIATION','Unique • Passive, In Slot • 1.8.1',[],7)]
for i in range(0,len(rows),6):slides.append(('NATIVE ATTRIBUTE MULTIPLIERS',f'Runtime getter measurements • {i+1}–{min(i+6,len(rows))} of {len(rows)}',[(label(r),r['before'],r['after']) for r in rows[i:i+6]],8))
slides += [('COMPLETED OUTPUTS','Tested examples • square once after ordinary modifiers',outputs[:5],9),('GAINS AND REWARDS','Illustrative inputs • native caps still apply',outputs[5:],9),('CAPACITIES ARE UNCHANGED','No bonus to pool size • recovery still benefits',[('Max HP / SHP',100,100),('Max aura / magicules',1000,1000),('Absorption / oxygen / water / lava',3,3)],8),('LIMITS & EXCLUSIONS','Movement • costs • cooldowns • progression requirements',[],9)]
W,H,FPS=1280,720,24
fonts={s:ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans'+('-Bold' if s in [38,70] else '')+'.ttf',s) for s in [16,18,20,23,28,38,70]}
out=R/'wiki/media/exponentiation-multipliers-1.8.1.mp4';out.parent.mkdir(parents=True,exist_ok=True)
proc=subprocess.Popen(['ffmpeg','-hide_banner','-loglevel','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(FPS),'-i','-','-an','-c:v','libx264','-threads','4','-preset','fast','-crf','19','-pix_fmt','yuv420p','-movflags','+faststart',str(out)],stdin=subprocess.PIPE)
for index,(title,subtitle,data,duration) in enumerate(slides):
 base=Image.new('RGB',(W,H),'#0b1020');d=ImageDraw.Draw(base)
 for x in range(0,W,40):d.line((x,0,x,H),fill='#11192c')
 for y in range(0,H,40):d.line((0,y,W,y),fill='#11192c')
 d.rounded_rectangle((32,26,1248,680),radius=18,fill='#111a2b',outline='#394264',width=2)
 d.text((66,53),'TENSURA ADDITIONAL ADDITIONS',font=fonts[16],fill='#d9ba7b');d.text((66,94),title,font=fonts[38],fill='#f1e9d7');d.text((66,150),subtitle,font=fonts[20],fill='#a8b4ca')
 d.text((66,641),'Animated measured-values reference • test inputs are not starting stats',font=fonts[16],fill='#92a2ba');d.text((1150,641),f'{index+1}/{len(slides)}',font=fonts[18],fill='#d9ba7b')
 if not data:
  text= ['x²','Equip the learned skill in your current Tensura preset.','Beneficial values above 1 are squared.','Attack, mining and chant speeds remain included.'] if index==0 else ['NORMAL POOLS. STRONGER OUTPUTS.','Values ≤ 1 stay unchanged. Chances cap at their native maximum.','Drops cap at normal stack size. Fixed skill radii/durations stay normal.','Attribute inputs are not squared again inside completed-output formulas.','Unrelated addon private variables are outside this native preview.']
  for j,t in enumerate(text):d.text((66,230+j*72),t,font=fonts[70 if index==0 and j==0 else 23],fill='#dfc087' if j==0 else '#dce5f4')
 for frame in range(duration*FPS):
  im=base.copy();d=ImageDraw.Draw(im);t=frame/FPS
  if data:
   for j,(name,before,after) in enumerate(data):
    y=215+j*64;progress=max(0,min(1,(t-.15*j)/1.2));ease=1-(1-progress)**3
    d.rounded_rectangle((62,y-4,1218,y+50),radius=7,fill='#19253b')
    d.text((80,y+8),name,font=fonts[20],fill='#e4e9f1')
    d.text((775,y+7),f'{before:g}',font=fonts[23],fill='#9cabc0');d.text((880,y+7),'→',font=fonts[23],fill='#e1bf78');d.text((953,y+7),f'{before+(after-before)*ease:g}' if ease==1 else f'{before+(after-before)*ease:.1f}',font=fonts[28],fill='#85e4d2')
    if after!=before:d.line((80,y+46,80+int(620*ease),y+46),fill='#6572bd',width=2)
  d.rectangle((62,689,62+int(1156*(frame+1)/(duration*FPS)),693),fill='#d9ba7b')
  if index==1 and frame==duration*FPS//2:im.save(out.with_name('exponentiation-preview-poster.png'))
  proc.stdin.write(im.tobytes())
proc.stdin.close();assert proc.wait()==0
print(out)
