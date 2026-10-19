#!/usr/bin/env python3
"""Pack generated character texture islands into Minecraft's classic 64x64 UVs.
Only nearest sampling is used. Base faces are made opaque; unused UVs stay clear.
"""
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
out=root/'src/main/resources/assets/wildhunt/textures/entity/otherworlder'
out.mkdir(parents=True,exist_ok=True)
# Face order: top, bottom, right, front, left, back. Crops describe the original
# art islands, not a generated 3D preview. Explicit packing avoids guessed UVs.
crops={
 'alma':{
  'head':[(168,10,315,154),(320,10,463,154),(8,159,164,374),(164,158,325,374),(326,158,465,374),(467,158,624,374)],
  'body':[(342,405,605,469),(342,716,605,766),(284,470,334,766),(334,470,616,766),(620,470,694,766),(744,470,960,766)],
  'arm':[(88,403,162,467),(89,713,164,764),(29,470,88,766),(89,470,164,766),(164,470,196,766),(183,470,259,766)],
  'leg':[(386,778,475,812),(386,1180,475,1220),(337,780,384,1220),(384,780,476,1220),(478,780,550,1220),(707,780,789,1220)],
 },
 'gemma':{
  'head':[(229,13,397,108),(229,292,397,342),(62,178,219,343),(220,130,401,344),(402,178,559,343),(560,178,618,343)],
  'body':[(430,365,617,402),(430,660,617,705),(347,400,404,708),(405,400,626,708),(627,400,705,708),(733,400,926,708)],
  'arm':[(83,399,169,464),(83,658,169,704),(36,466,81,705),(83,466,169,705),(170,466,204,705),(223,466,306,705)],
  'leg':[(340,875,436,907),(340,1204,436,1240),(328,880,360,1240),(360,880,442,1240),(443,880,518,1240),(697,880,786,1240)],
 }
}
def faces(x,y,w,h,d):
 return [(x+d,y,x+d+w,y+d),(x+d+w,y,x+d+2*w,y+d),
         (x,y+d,x+d,y+d+h),(x+d,y+d,x+d+w,y+d+h),
         (x+d+w,y+d,x+2*d+w,y+d+h),(x+2*d+w,y+d,x+2*d+2*w,y+d+h)]
for name,parts in crops.items():
 source=Image.open(root/f'art/{name}-skin-islands.png').convert('RGBA')
 skin=Image.new('RGBA',(64,64))
 for part,uv in [('head',(0,0,8,8,8)),('body',(16,16,8,12,4)),('arm',(40,16,4,12,4)),('leg',(0,16,4,12,4)),('arm',(32,48,4,12,4)),('leg',(16,48,4,12,4))]:
  for crop,dest in zip(parts[part],faces(*uv)):
   tile=source.crop(crop).resize((dest[2]-dest[0],dest[3]-dest[1]),Image.Resampling.NEAREST)
   # JPEG-like island edge transparency is inappropriate for a base skin face.
   tile.putalpha(255);skin.paste(tile,dest)
 # Head correction atlas: six equal panels, top/bottom/right/front/left/back.
 # Preserve the exact existing front face (glasses, eyes and expression), body,
 # and equipment. Only the five other head faces are replaced.
 head=Image.open(root/f'art/{name}-head-tiles.png').convert('RGBA')
 for index,dest in enumerate(faces(0,0,8,8,8)):
  if index == 3: continue
  column,row=index%3,index//3
  crop=(column*head.width//3,row*head.height//2,
        (column+1)*head.width//3,(row+1)*head.height//2)
  tile=head.crop(crop).resize((8,8),Image.Resampling.NEAREST)
  # UV side faces run in opposite directions around the cube.
  if index in (2,4): tile=tile.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
  if tile.getchannel('A').getextrema()[0] != 255:
   raise ValueError(f'{name}: head atlas contains a transparent cutout')
  skin.paste(tile,dest)
 # The 8x8 face must not be hidden by another full face on the outer layer.
 # Hair is conveyed by the base wrap, with goggles/glasses baked into the face.
 skin.save(out/f'{name}.png')
 (out/f'{name}.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":true}}\n')
 print('Packed',name,skin.size)
