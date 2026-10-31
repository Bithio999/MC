#!/usr/bin/env python3
"""Edit genuine World Hop footage; captions use the captured/verified metrics."""
from pathlib import Path
import argparse,json,subprocess,math
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def main():
 p=argparse.ArgumentParser();p.add_argument('capture',type=Path);p.add_argument('output',type=Path);a=p.parse_args();a.output.parent.mkdir(parents=True,exist_ok=True);m=json.loads((a.capture/'metrics.json').read_text())
 def txt(name,value):
  f=a.capture/(name+'.txt');f.write_text(value);return str(f)
 def text(name,value,size,x,y,color='white',bold=False,extra=''):
  return f"drawtext=fontfile={BOLD if bold else FONT}:textfile={txt(name,value)}:fontsize={size}:fontcolor={color}:x={x}:y={y}"+(f':{extra}' if extra else '')
 def encode_card(name,duration,filters):
  out=a.capture/(name+'.mp4');subprocess.run(['ffmpeg','-hide_banner','-loglevel','warning','-y','-f','lavfi','-i',f'color=c=0x091526:s=1280x720:r=30:d={duration}','-vf',','.join(filters),'-an','-c:v','libx264','-preset','fast','-crf','18','-pix_fmt','yuv420p',str(out)],check=True);return out
 intro=2.6;outro=5.6
 introfile=encode_card('intro',intro,['drawbox=x=160:y=240:w=960:h=2:color=0xE0C186:t=fill',text('intro-a','PARALLEL WORLD JUMPER',44,'(w-tw)/2',285,bold=True),text('intro-b','WORLD HOP',27,'(w-tw)/2',353,'0xE0C186'),text('intro-c','TensuraAdditionalAdditions 1.3.0  |  In-game preview',18,'(w-tw)/2',428,'0xA7B9CD'),'fade=t=in:d=0.25','fade=t=out:st=2.35:d=0.25'])
 seed_before=str(m['seed_before']);seed_after=str(m['seed_after']);ep=f"{m['ep_after']:,.0f}"
 outrofile=encode_card('outro',outro,[text('end-a','A NEW SEED. YOUR CHARACTER.',35,'(w-tw)/2',160,bold=True),text('end-b',seed_before+'  →  '+seed_after,21,'(w-tw)/2',236,'0xA7B9CD'),text('end-c','Every dimension regenerated',27,'(w-tw)/2',330,'0xE0C186',extra="enable='gte(t,0.4)'"),text('end-d',ep+' EP  •  named sword  •  37 diamonds retained',23,'(w-tw)/2',392,extra="enable='gte(t,1.1)'"),text('end-e','Previous world preserved as a recovery copy',22,'(w-tw)/2',452,'0xA7B9CD',extra="enable='gte(t,1.8)'"),text('end-f','PRIVATE SINGLE-PLAYER ONLY',17,'(w-tw)/2',550,'0xE0C186',extra="enable='gte(t,2.3)'"),'fade=t=in:d=0.25',f'fade=t=out:st={outro-.3}:d=0.3'])
 # A stopped filming controller can leave an idle gap; keep only the genuine
 # reload and the later rendered landscape, never stretching that idle into loading.
 first=m['client_return_at'];firstfile=a.capture/'first-return.json'
 if firstfile.exists():first=json.loads(firstfile.read_text())['first_return_at']
 b0=.35;b1=max(1,m['cast_at']-.2);t0=b1;t1=m['cast_at']+2.0;l0=t1;l1=min(m['terrain_visible_at'],first+4);post0=m['terrain_visible_at']+.15;post1=m['duration']-.35
 speed=max(1,(l1-l0)/6);loading=(l1-l0)/speed
 assert b1>b0 and t1>t0 and l1>l0 and post1>post0
 def panel(title,seed,stem):
  return ','.join(['drawbox=x=807:y=25:w=448:h=115:color=0x091526DD:t=fill','drawbox=x=807:y=25:w=3:h=115:color=0xE0C186:t=fill',text(stem+'-title',title,21,826,42,bold=True),text(stem+'-seed','Seed  '+seed,18,826,78,'0xB7C7D9'),text(stem+'-ep',ep+' EP  •  37 diamonds',18,826,108,'0xB7C7D9')])
 loading_label=text('loading-note',f'WORLD RELOADING  ·  {speed:.1f}× SPEED',22,'w-tw-24','h-50','0xE0C186',bold=True,extra='box=1:boxcolor=0x091526DD:boxborderw=12')
 graph=['[0:v]split=4[vb][vt][vl][va]',f'[vb]trim=start={b0}:end={b1},setpts=PTS-STARTPTS,{panel("BEFORE WORLD HOP",seed_before,"before")}[b]',f'[vt]trim=start={t0}:end={t1},setpts=PTS-STARTPTS[t]',f'[vl]trim=start={l0}:end={l1},setpts=(PTS-STARTPTS)/{speed},fps=30,{loading_label}[l]',f'[va]trim=start={post0}:end={post1},setpts=PTS-STARTPTS,{panel("AFTER WORLD HOP",seed_after,"after")},fade=t=out:st={post1-post0-.25}:d=0.25[a]','[1:v]setpts=PTS-STARTPTS[i]','[2:v]setpts=PTS-STARTPTS[o]','[i][b][t][l][a][o]concat=n=6:v=1:a=0[v]']
 # Quiet original game audio; no invented portal/casting sound effects.
 offset=max(0,m.get('audio_offset',0));audio=a.capture/'game.wav';args=['ffmpeg','-hide_banner','-loglevel','warning','-y','-i',str(a.capture/'raw.mp4'),'-i',str(introfile),'-i',str(outrofile)]
 if audio.exists():
  args+=['-i',str(audio)];graph += [f'[3:a]aresample=48000,asplit=4[ab][at][al][aa]',f'[ab]atrim=start={offset+b0}:end={offset+b1},asetpts=PTS-STARTPTS[ba]',f'[at]atrim=start={offset+t0}:end={offset+t1},asetpts=PTS-STARTPTS[ta]',f'[al]atrim=start={offset+l0}:end={offset+l1},asetpts=PTS-STARTPTS,atempo={speed},volume=0[la]',f'[aa]atrim=start={offset+post0}:end={offset+post1},asetpts=PTS-STARTPTS,afade=t=out:st={post1-post0-.25}:d=0.25[pa]',f'anullsrc=r=48000:cl=stereo,atrim=duration={intro}[ia]',f'anullsrc=r=48000:cl=stereo,atrim=duration={outro}[oa]','[ia][ba][ta][la][pa][oa]concat=n=6:v=0:a=1[sound]']
 args+=['-filter_complex',';'.join(graph),'-map','[v]']
 if audio.exists():args+=['-map','[sound]','-c:a','aac','-b:a','160k']
 args+=['-c:v','libx264','-preset','slow','-crf','18','-pix_fmt','yuv420p','-r','30','-movflags','+faststart',str(a.output)];subprocess.run(args,check=True)
 timeline={'intro':intro,'before':b1-b0,'transition':t1-t0,'loading':loading,'loading_speed':speed,'after':post1-post0,'outro':outro,'removed_idle_seconds':max(0,post0-l1)}
 (a.output.parent/'edit-timeline.json').write_text(json.dumps(timeline,indent=2)+'\n');(a.output.parent/'capture-metrics.json').write_text(json.dumps(m,indent=2)+'\n');print(json.dumps(timeline))
if __name__=='__main__':main()
