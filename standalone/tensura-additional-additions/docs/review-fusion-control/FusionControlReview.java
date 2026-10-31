package dev.heavenlyrestriction;

import dev.heavenlyrestriction.fusion.Fusion;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Opt-in actual client movement and Jade/Tensura ray-query regression. Never runs in a player save. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class FusionControlReview {
    private static final Path OUT=Path.of("/workspace/validation-fusion/control-review");
    private static volatile int phase;private static int age,ticks,role,hostId,victimId;private static volatile String error;
    private static IronGolem host,victim;private static Vec3 start,release;private static final double[] traveled=new double[2],coast=new double[2];
    private static void check(boolean ok,String message){if(!ok){error=message;throw new IllegalStateException("FUSION_CONTROL_REVIEW: "+message);}}
    private static void server(java.util.function.Consumer<ServerPlayer> action){var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->{try{action.accept(s.getPlayerList().getPlayers().getFirst());}catch(Throwable ex){error=ex.toString();}});}
    private static void aim(net.minecraft.world.entity.LivingEntity p,float yaw){p.setYRot(yaw);p.yRotO=yaw;p.yHeadRot=yaw;p.yHeadRotO=yaw;p.setXRot(0);p.xRotO=0;}
    private static void write(String name,String value){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(name),value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private static void setup(ServerPlayer p){
        check(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Fusion Preview"),"Not disposable review save");
        Fusion.end(p,"Review reset");p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().flying=false;p.getAbilities().mayfly=false;p.onUpdateAbilities();p.setNoGravity(false);p.noPhysics=false;
        for(var e:List.copyOf(p.serverLevel().getEntitiesOfClass(IronGolem.class,new AABB(-100,0,-100,100,300,100))))e.discard();
        for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++){p.serverLevel().setBlockAndUpdate(new BlockPos(x,158,z),Blocks.STONE.defaultBlockState());for(int y=159;y<=163;y++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i);
        SkillAPI.getSkillsFrom(p).learnSkill(HeavenlyRestriction.FUSION.get().createDefaultInstance());p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.1);p.teleportTo(0,159,0);aim(p,0);p.setDeltaMovement(Vec3.ZERO);
        host=new IronGolem(EntityType.IRON_GOLEM,p.level());host.setPos(3,159,0);host.setNoAi(true);p.serverLevel().addFreshEntity(host);TensuraStorages.getExistenceFrom(host).setPermanentOwner(p.getUUID());hostId=host.getId();
        victim=new IronGolem(EntityType.IRON_GOLEM,p.level());victim.setPos(3,159,6);victim.setNoAi(true);p.serverLevel().addFreshEntity(victim);victimId=victim.getId();phase=2;
    }
    private static void targeting() throws Exception {
        var mc=Minecraft.getInstance();var hidden=mc.level.getEntity(hostId);check(hidden instanceof LivingEntity && Fusion.embedded(hidden) && hidden.noPhysics,"Client embedded body is not non-physical");
        check(mc.level.getEntity(hostId)==hidden,"Rendering body unavailable by direct ID");
        check(!mc.level.getEntities(mc.player,mc.player.getBoundingBox().inflate(16),e->true).contains(hidden),"Client untyped query exposes body");
        check(!mc.level.getEntitiesOfClass(LivingEntity.class,mc.player.getBoundingBox().inflate(16),e->true).contains(hidden),"Client typed query exposes body");
        var ray=Class.forName("snownee.jade.overlay.RayTracing");var method=ray.getMethod("getEntityHitResult",net.minecraft.world.level.Level.class,Entity.class,Vec3.class,Vec3.class,AABB.class,Predicate.class);
        for(float yaw:new float[]{0,90,180,-90}){aim(mc.player,yaw);var start=mc.player.getEyePosition();var end=start.add(mc.player.getLookAngle().scale(16));var box=mc.player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1);
            var hit=(EntityHitResult)method.invoke(null,mc.level,mc.player,start,end,box,(Predicate<Entity>)(e->true));check(hit==null || hit.getEntity()!=hidden,"Actual Jade ray picked embedded body at yaw "+yaw);
            var appraisal=ObjectSelectionHelper.getTargetingEntity(mc.player,16,false,true);check(appraisal!=hidden,"Appraisal picked embedded body");
            if(yaw==0){check(hit!=null && hit.getEntity().getId()==victimId,"Jade no longer picks the real target ahead");check(appraisal!=null && appraisal.getId()==victimId,"Appraisal no longer picks real target ahead");}
        }aim(mc.player,0);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(++age>2400 || error!=null){mc.options.keyUp.setDown(false);write("failure.txt",error==null?"Timeout phase "+phase:error);mc.stop();return;}
        if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || mc.getOverlay()!=null)return;
        try{
            if(phase==0 && mc.player.tickCount>60){phase=1;mc.setScreen(null);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);server(FusionControlReview::setup);ticks=0;return;}
            if(phase==2 && ++ticks>40){aim(mc.player,0);start=mc.player.position();mc.options.keyUp.setDown(true);phase=3;ticks=0;return;}
            if(phase==3 && ++ticks>=5){mc.options.keyUp.setDown(false);release=mc.player.position();traveled[role]=release.distanceTo(start);check(traveled[role]>.05,"Movement input did not move player");phase=4;ticks=0;return;}
            if(phase==4 && ++ticks>=30){coast[role]=mc.player.position().distanceTo(release);check(mc.player.getDeltaMovement().horizontalDistance()<.001,"Movement continues sliding after key release");
                if(role==0){phase=5;ticks=0;server(p->{p.teleportTo(3,159,0);p.setDeltaMovement(Vec3.ZERO);aim(p,0);Fusion.press(p);check(Fusion.body(p),"Actual Fuse activation failed");phase=6;});}
                else{phase=7;ticks=0;server(p->{Fusion.end(p,"Control review finished");phase=8;});}return;}
            if(phase==6 && ++ticks>30){targeting();role=1;start=mc.player.position();mc.options.keyUp.setDown(true);phase=3;ticks=0;return;}
            if(phase==8 && ++ticks>30){var original=mc.level.getEntity(hostId);check(original!=null && !Fusion.embedded(original) && !original.noPhysics,"Original client physics not restored");check(mc.level.getEntitiesOfClass(LivingEntity.class,original.getBoundingBox().inflate(.1)).contains(original),"Original client query visibility not restored");
                write("passed.txt","PASS: actual client key movement and release, baseline and fused; client noPhysics; actual Jade permissive ray and native appraisal query at four headings; external targets remain detectable; retained render body and separation restored.\nBaseline travel="+traveled[0]+", coast="+coast[0]+"; fused travel="+traveled[1]+", coast="+coast[1]+"\n");mc.stop();}
        }catch(Throwable ex){error=ex.toString();}
    }
}
