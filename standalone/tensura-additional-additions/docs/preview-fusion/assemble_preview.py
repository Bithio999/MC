#!/usr/bin/env python3
"""Caption genuine, validated Fusion gameplay; no replacement gameplay frames."""
from pathlib import Path
import json,subprocess,sys
cap=Path(sys.argv[1]);output=Path(sys.argv[2]);output.parent.mkdir(parents=True,exist_ok=True)
m=json.loads((cap/'metrics.json').read_text());assert (cap/'passed.txt').exists() and not (cap/'failure.txt').exists()
assert m['normal_damage']>0 and abs(m['fused_damage']-2*m['normal_damage'])<.1
font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';bold='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def text(name,value,size,x,y,color='0xE5F5F0',extra='',strong=False):
 p=cap/(name+'.txt');p.write_text(value)
 return f'drawtext=fontfile={bold if strong else font}:textfile={p}:fontsize={size}:fontcolor={color}:x={x}:y={y}'+(':'+extra if extra else '')
def card(name,seconds,filters):
 p=cap/(name+'.mp4');subprocess.run(['ffmpeg','-hide_banner','-loglevel','warning','-y','-f','lavfi','-i',f'color=c=0x091a29:s=1280x720:r=30:d={seconds}','-vf',','.join(filters),'-c:v','libx264','-preset','fast','-crf','18','-pix_fmt','yuv420p',str(p)],check=True);return p
intro=card('intro',2.5,['drawbox=x=160:y=230:w=960:h=2:color=0x6FE4CA:t=fill',text('title','FUSION',66,'(w-tw)/2',278,strong=True),text('subtitle','TensuraAdditionalAdditions  |  Unique skill',24,'(w-tw)/2',370,'0xE1BF7C'),text('intro-note','Scripted in-game demonstration',18,'(w-tw)/2',442,'0xA3C4CB'),'fade=t=in:d=0.3','fade=t=out:st=2.25:d=0.25'])
outro=card('outro',6,[text('outro-title','TWO BODIES. ONE FUSED FORM.',35,'(w-tw)/2',155,strong=True),text('outro-ep',f"({m['caster_ep']:,.0f} + {m['subordinate_ep']:,.0f}) × 1.5 = {m['fused_ep']:,.0f} EP",29,'(w-tw)/2',252,'0x81EED8',"enable='gte(t,0.4)'"),text('outro-regen','Aura + magicule regeneration ×10',27,'(w-tw)/2',328,'0xE1BF7C',"enable='gte(t,1.0)'"),text('outro-damage',f"Same test strike: {m['normal_damage']:g} → {m['fused_damage']:g} damage",27,'(w-tw)/2',389,extra="enable='gte(t,1.6)'"),text('outro-duration','Fusion: 3 minutes  |  Post-fusion penalties: 5 minutes',23,'(w-tw)/2',469,'0xA3C4CB',"enable='gte(t,2.2)'"),'fade=t=in:d=0.25','fade=t=out:st=5.7:d=0.3'])
filters=[]
def panel(key,title,lines,start,end):
 enable=f"enable='between(t,{start},{end})'"
 filters.extend([f'drawbox=x=790:y=501:w=472:h=122:color=0x091A29E8:t=fill:{enable}',f'drawbox=x=790:y=501:w=3:h=122:color=0x6FE4CA:t=fill:{enable}',text(key+'-head',title,23,808,516,'0xE1BF7C',enable,True)])
 for n,line in enumerate(lines):filters.append(text(key+str(n),line,19,808,552+25*n,extra=enable))
panel('before','BEFORE FUSION',[f"Caster: {m['caster_ep']:,.0f} EP",f"Subordinate: {m['subordinate_ep']:,.0f} EP"],0,6.8)
panel('merge','FUSED SUBORDINATE',[f"Combined EP ×1.5 = {m['fused_ep']:,.0f}",'One controlled body • temporary skills'],7.1,14)
panel('control','CONTROL THE FUSED FORM',['Movement and borrowed skill slot','Aura + magicule regeneration ×10'],14.1,21.8)
panel('damage','FINAL DAMAGE ×2',[f"Test strike: {m['normal_damage']:g} → {m['fused_damage']:g}",'Original damage type is retained'],22.1,29.6)
panel('split','EARLY SEPARATION',['Original EP and skills restored','Paralysis II + Fragility II: 5 minutes'],30.4,39)
graph=f'[0:v]{",".join(filters)},fade=t=out:st=38.5:d=0.4[g];[1:v]setpts=PTS-STARTPTS[i];[2:v]setpts=PTS-STARTPTS[o];[i][g][o]concat=n=3:v=1:a=0[v]'
subprocess.run(['ffmpeg','-hide_banner','-loglevel','warning','-y','-i',str(cap/'raw.mp4'),'-i',str(intro),'-i',str(outro),'-filter_complex',graph,'-map','[v]','-an','-c:v','libx264','-preset','fast','-crf','19','-pix_fmt','yuv420p','-r','30','-movflags','+faststart',str(output)],check=True)
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-ss','14','-i',str(output),'-frames:v','1',str(output.with_name('fusion-poster.png'))],check=True)
print('Validated gameplay video:',output)
