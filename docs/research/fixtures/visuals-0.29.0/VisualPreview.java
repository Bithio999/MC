package dev.wildhunt;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
@EventBusSubscriber(modid=WildHunt.ID)
public final class VisualPreview {
 @SubscribeEvent public static void register(RegisterCommandsEvent event){
  event.getDispatcher().register(Commands.literal("vfxclear").requires(s->s.hasPermission(2)).executes(c->{var l=c.getSource().getLevel();for(var e:l.getAllEntities())if(e instanceof ArtVisualEntity)e.discard();return 1;}));
  event.getDispatcher().register(Commands.literal("vfx").requires(s->s.hasPermission(2)).then(Commands.argument("kind",IntegerArgumentType.integer(0,62)).executes(c->{
   var s=c.getSource();int k=IntegerArgumentType.getInteger(c,"kind");if(k==61)return 0;var at=new Vec3(0,70,8);var delta=new Vec3(3,0,0);int life=100;
   if(java.util.Set.of(0,6,11,15,16,29,33,34,38,40,41,44,50,54).contains(k))delta=new Vec3(0,0,12);
   if(k==1)delta=new Vec3(0,4,0);if(k==2)delta=new Vec3(0,-8,0);if(k==3)delta=new Vec3(0,4,10);if(k==7)delta=new Vec3(10,24,0);
   if(k==25)life=70;if(k==26)life=30;if(k==28)life=200;if(k==27)life=200;if(k>=56)life=120;
   if(k==0)at=at.add(0,1.2,0);if(k==2)at=at.add(0,8,0);var v=ArtVisualEntity.spawn(s.getLevel(),k,at,delta,life);
   if(java.util.Set.of(14,20,27,28,37,47,51).contains(k)){v.setCaster(s.getEntity());v.setPos(s.getPosition());if(k==47 || k==51){var t=s.getEntity().getPersistentData().getCompound("wildhunt");var lease=new CompoundTag();lease.putUUID("visual",v.getUUID());lease.putLong("until",s.getLevel().getGameTime()+life+100);t.put(k==47?"perennial_seed":"lugh_seal",lease);s.getEntity().getPersistentData().put("wildhunt",t);}}
   if(k>=56 && k<=59){var t=new CompoundTag();t.putLong("start",s.getLevel().getGameTime());t.putDouble("forward_x",0);t.putDouble("forward_z",1);t.putInt("warned",6);for(int n=0;n<6;n++){double a=n*Math.PI/3;t.putDouble("star_"+n+"_x",Math.cos(a)*7);t.putDouble("star_"+n+"_y",1);t.putDouble("star_"+n+"_z",Math.sin(a)*7);}v.barrage(t);}
   if(k==9){var t=new CompoundTag();t.putLong("start",s.getLevel().getGameTime());t.putInt("seed",91);t.putDouble("aim_x",0);t.putDouble("aim_y",2);t.putDouble("aim_z",10);v.barrage(t);}
   return 1;
  })));
 }
}
