#!/usr/bin/env python3
"""Assemble actual gameplay, a labeled replay and the measured animated breakdown."""
from pathlib import Path
import argparse,array,json,math,subprocess,wave

def main():
 p=argparse.ArgumentParser();p.add_argument('capture',type=Path);p.add_argument('output',type=Path);a=p.parse_args();m=json.loads((a.capture/'metrics.json').read_text());font='/usr/share/fonts/truetype/open-sans/OpenSans-Bold.ttf'
 rate=48000;duration=15;audio=array.array('f',[0])*(rate*duration)
 for at,freq,length,gain in [(.3,390,.12,.025),(1.1,490,.12,.025),(2.5,550,.12,.025),(3.9,650,.14,.025),(5.0,730,.15,.025),(6.0,880,.42,.035),(7.1,440,.18,.025),(9.2,220,.7,.018),(11.15,660,.5,.035)]:
  for i in range(int(length*rate)):
   t=i/rate;env=min(1,t/.007)*math.exp(-t*7)*min(1,(length-t)/.015);audio[int(at*rate)+i]+=gain*env*(math.sin(math.tau*freq*t)+.3*math.sin(math.tau*freq*1.5*t))
 with wave.open(str(a.capture/'breakdown-cues.wav'),'wb') as w:
  w.setparams((2,2,rate,0,'NONE','not compressed'));data=array.array('h')
  for value in audio:
   sample=int(max(-1,min(1,value))*32767);data.extend((sample,sample))
  w.writeframes(data.tobytes())
 # Retain the windup, labeling the accelerated middle and slow-motion replay.
 cut1=m['cast_at_seconds']+2;cut2=m['counter_at_seconds']-1;end=16.65;replay0=m['counter_at_seconds']-.5;replay1=m['hit_at_seconds']+1.6
 label=lambda t:f"drawtext=fontfile={font}:text='{t}':fontsize=22:fontcolor=0xF0D49B:box=1:boxcolor=0x08111DDD:boxborderw=10:x=w-tw-24:y=h-42"
 offset=m['audio_offset_seconds']+.10
 filters=[
  '[0:v]split=4[g0][g1][g2][r0]',
  f'[g0]trim=start=0.2:end={cut1},setpts=PTS-STARTPTS,fade=t=in:d=0.18[v0]',
  f'[g1]trim=start={cut1}:end={cut2},setpts=(PTS-STARTPTS)/4,fps=60,{label("WINDUP 4x")}[v1]',
  f'[g2]trim=start={cut2}:end={end},setpts=PTS-STARTPTS,fade=t=out:st={end-cut2-.18}:d=0.18[v2]',
  f'[r0]trim=start={replay0}:end={replay1},setpts=2*(PTS-STARTPTS),fps=60,{label("SLOW-MOTION REPLAY 0.5x")},fade=t=in:d=0.18,fade=t=out:st={2*(replay1-replay0)-.2}:d=0.2[v3]',
  '[2:v]setpts=PTS-STARTPTS[v4]',
  f'[1:a]atrim=start={offset}:end={offset+17.066667},asetpts=PTS-STARTPTS,asplit=4[a0][a1][a2][a3]',
  f'[a0]atrim=start=0.2:end={cut1},asetpts=PTS-STARTPTS,afade=t=in:d=0.18[b0]',
  f'[a1]atrim=start={cut1}:end={cut2},asetpts=PTS-STARTPTS,atempo=2,atempo=2[b1]',
  f'[a2]atrim=start={cut2}:end={end},asetpts=PTS-STARTPTS,afade=t=out:st={end-cut2-.18}:d=0.18[b2]',
  f'[a3]atrim=start={replay0}:end={replay1},asetpts=PTS-STARTPTS,atempo=0.5,afade=t=in:d=0.18,afade=t=out:st={2*(replay1-replay0)-.2}:d=0.2[b3]',
  '[3:a]atrim=end=15,asetpts=PTS-STARTPTS[b4]',
  '[v0][b0][v1][b1][v2][b2][v3][b3][v4][b4]concat=n=5:v=1:a=1[v][a]']
 args=['ffmpeg','-hide_banner','-loglevel','warning','-y','-i',str(a.capture/'raw.mp4'),'-i',str(a.capture/'game.wav'),'-i',str(a.capture/'breakdown-final.mp4'),'-i',str(a.capture/'breakdown-cues.wav'),'-filter_complex',';'.join(filters),'-map','[v]','-map','[a]','-c:v','libx264','-preset','slow','-crf','18','-pix_fmt','yuv420p','-r','60','-c:a','aac','-b:a','192k','-movflags','+faststart',str(a.output)]
 subprocess.run(args,check=True)
 print('Gameplay, accelerated windup, replay and animated measured breakdown assembled.')
if __name__=='__main__':main()
