#!/usr/bin/env python3
"""Render measured Counter arithmetic and native regeneration as animated video graphics."""
from pathlib import Path
import argparse, json, math, struct, subprocess
from PIL import Image, ImageDraw, ImageFont

W,H,FPS,DURATION=1280,720,60,15
FONT=Path('/usr/share/fonts/truetype/open-sans')
F={k:ImageFont.truetype(str(FONT/name),size) for k,name,size in [
 ('title','OpenSans-Bold.ttf',34),('big','OpenSans-Bold.ttf',48),('mid','OpenSans-Semibold.ttf',25),
 ('body','OpenSans-Regular.ttf',19),('small','OpenSans-Regular.ttf',16),('label','OpenSans-Semibold.ttf',15),('number','OpenSans-Semibold.ttf',29)]}
WHITE=(236,243,250);MUTED=(151,172,194);GOLD=(239,205,129);CYAN=(126,220,231);GREEN=(146,220,175)
def ease(t): return 1-(1-max(0,min(1,t)))**3
def f32(v): return struct.unpack('f',struct.pack('f',v))[0]
def n(v,d=0): return f'{v:,.{d}f}'
def text(d,xy,value,font='body',color=WHITE): d.text(xy,str(value),font=F[font],fill=color)
def card(image,box,start,t,draw):
 p=ease((t-start)/.45)
 if p<=0:return
 x,y,w,h=box;shift=int((1-p)*24)
 layer=Image.new('RGBA',(w,h));d=ImageDraw.Draw(layer)
 d.rounded_rectangle((0,0,w-1,h-1),radius=13,fill=(17,29,46,249),outline=(42,62,82,255),width=1)
 draw(d,w,h)
 layer.putalpha(layer.getchannel('A').point(lambda a:int(a*p)))
 image.alpha_composite(layer,(x+shift,y))
def bar(d,x,y,w,h,p,color):
 d.rounded_rectangle((x,y,x+w,y+h),radius=h//2,fill=(37,53,70))
 if p>0:d.rounded_rectangle((x,y,x+max(h,int(w*max(0,min(1,p)))),y+h),radius=h//2,fill=color)
def frame(t,m):
 image=Image.new('RGBA',(W,H));d=ImageDraw.Draw(image)
 for y in range(H):
  k=y/H;d.line((0,y,W,y),fill=(int(8+5*k),int(15+7*k),int(27+10*k),255))
 for x in range(-300,1500,150): d.line((x,0,x+480,H),fill=(21,34,50),width=1)
 d.rectangle((48,33,52,102),fill=GOLD)
 text(d,(68,31),'COUNTER / DAMAGE BREAKDOWN','title')
 text(d,(70,78),'THE END OF ALL MYTHS  ·  MEASURED IN-GAME RESULTS','small',MUTED)
 text(d,(1017,47),'TENSURA MYTHS','label',CYAN)
 aura=m['aura_paid'];incoming=m['incoming_damage'];aura_term=6*math.sqrt(aura);base=f32(10*incoming+aura_term)
 total=m['dummy_actual_damage'];healed=m['eschaton_hp_healed_by_infinite_regeneration'];rate=m['infinite_regeneration_magicules_per_hp'];cost=m['eschaton_regeneration_magicule_loss']
 before=m['eschaton_magicules_before_regeneration'];after=m['eschaton_magicules_after_regeneration'];maximum_hp=1_000_000;low_hp=maximum_hp-healed
 assert abs(cost-healed*rate)<.01,(cost,healed*rate)
 assert abs(total-f32(f32(base*5)*1.5))<1,(total,base)
 def conditions(d,w,h):
  text(d,(20,17),'HEAVENBREAKER / UNBOUND','label',GOLD)
  text(d,(20,48),n(m['player_total_ep'])+' EP','number')
  text(d,(20,92),'Aura before  '+n(m['player_aura_before']),'body',MUTED)
  text(d,(20,122),'Entry cost  10% current aura','body',MUTED)
  text(d,(20,153),n(aura,1)+' aura paid','mid',GOLD)
 card(image,(48,130,352,207),.3,t,conditions)
 def row(title,formula,start,y,color=CYAN):
  def paint(d,w,h):
   text(d,(20,13),title,'label',color);text(d,(20,39),formula,'mid')
   p=ease((t-start)/.8);d.rectangle((20,h-4,20+int((w-40)*p),h-3),fill=color)
  card(image,(424,y,808,86),start,t,paint)
 row('01 / THE NEGATED STRIKE',n(incoming)+' × 10 = '+n(incoming*10),1.1,130)
 row('02 / AURA CONTRIBUTION','6 × √'+n(aura,1)+' = '+n(aura_term,1),2.5,228,GOLD)
 row('03 / BASE COUNTER DAMAGE',n(incoming*10)+' + '+n(aura_term,1)+' = '+n(base,1),3.9,326)
 def multipliers(d,w,h):
  text(d,(20,12),'04 / APPLY EACH BONUS ONCE','label',GOLD)
  text(d,(20,36),'Physical ×5     Critical ×1.5','mid')
 card(image,(424,424,808,83),5.0,t,multipliers)
 def result(d,w,h):
  text(d,(20,13),'ADJACENT DUMMY / ACTUAL DAMAGE','label',GREEN)
  value=total*ease((t-6.1)/1.05)
  text(d,(18,35),n(value,1),'big',GOLD)
  if t>7.2:text(d,(520,73),'One hit. Native float rounding.','body',MUTED)
 card(image,(424,522,808,126),6.0,t,result)
 def regeneration(d,w,h):
  text(d,(20,15),'ESCHATON / INFINITE REGENERATION','label',CYAN)
  p=ease((t-7.8)/1.2);hp=low_hp+healed*p
  text(d,(20,45),'HP  '+n(hp)+' / 1,000,000','body')
  bar(d,20,77,w-40,10,hp/maximum_hp,GREEN)
  text(d,(20,99),n(healed)+' HP damage restored','body',GREEN)
  pmp=ease((t-9.2)/1.65);mp=before+(after-before)*pmp
  text(d,(20,130),'Magicules  '+n(mp),'body')
  bar(d,20,160,w-40,10,mp/500_000_000,CYAN)
  if t>9.3:text(d,(20,183),n(before)+' to '+n(after),'small',MUTED)
  if t>10.5:text(d,(20,208),n(healed)+' HP × '+n(rate)+' MP / HP','body',MUTED)
  if t>11.15:text(d,(20,237),'−'+n(cost)+' MP','number',GOLD)
  if t>11.7:text(d,(20,272),'Regen only. Cast: −'+n(m['eschaton_magicules_before_cast']-m['eschaton_magicules_after_cast'])+' MP.','small',MUTED)
 card(image,(48,350,352,298),7.1,t,regeneration)
 d=ImageDraw.Draw(image)
 if t>11.9:
  text(d,(48,668),'Your HP '+n(m['player_hp_before_after'])+' / SHP '+n(m['player_shp_before_after'])+' stayed unchanged.','small',MUTED)
  text(d,(708,668),'Physical branch only. Battlewill ×8 does not stack.','small',MUTED)
 alpha=min(ease(t/.35),ease((DURATION-t)/.35));
 if alpha<1:
  shade=Image.new('RGBA',(W,H),(0,0,0,int((1-alpha)*255)));image.alpha_composite(shade)
 return image.convert('RGB')
def main():
 p=argparse.ArgumentParser();p.add_argument('metrics',type=Path);p.add_argument('output',type=Path);p.add_argument('--still',type=float);a=p.parse_args();m=json.loads(a.metrics.read_text())
 if a.still is not None:frame(a.still,m).save(a.output);return
 cmd=['ffmpeg','-hide_banner','-loglevel','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(FPS),'-i','pipe:0','-an','-c:v','libx264','-preset','fast','-crf','18','-pix_fmt','yuv420p','-movflags','+faststart',str(a.output)]
 process=subprocess.Popen(cmd,stdin=subprocess.PIPE)
 try:
  for i in range(int(DURATION*FPS)):process.stdin.write(frame(i/FPS,m).tobytes())
 finally:process.stdin.close()
 assert process.wait()==0
 print('Rendered',int(DURATION*FPS),'animated breakdown frames.')
if __name__=='__main__':main()
