package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in isolated client review, excluded from production sources and release JARs. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class ChainedTargetingReview {
    private static boolean prepared;private static volatile boolean ready,finished;private static volatile Vec3 expected;
    private static Husk near,far;private static long castAfter;private static int age;
    private static final Vec3 START=new Vec3(.5,71,.5);
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null || server==null)return;
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            for(int x=-4;x<=24;x++)for(int z=-5;z<=5;z++){level.setBlockAndUpdate(new BlockPos(x,70,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=71;y<=76;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var owned:java.util.List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(owned);
            ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=false;p.onUpdateAbilities();EnergyHelper.setMaxAura(p,100000);
            var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();var energy=TensuraStorages.getExistenceFrom(p);
            var leaves=Blocks.BIRCH_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true);
            for(int x=6;x<=18;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(x,73,z),leaves);
            for(int y=71;y<=75;y++)level.setBlockAndUpdate(new BlockPos(12,y,0),Blocks.BIRCH_LOG.defaultBlockState());
            p.teleportTo(level,START.x,START.y,START.z,-90,0);p.setOnGround(true);i.setCoolDown(0,0);energy.setAura(10000);i.onPressed(p,0,0);
            if(p.getX()<=8.5 || p.getX()>=12 || p.getY()!=71 || Math.abs(energy.getAura()-9800)>.001 || i.getCoolDown(0)!=1 || !level.noCollision(p,p.getBoundingBox()))throw new IllegalStateException("Real-player forest landing failed: "+p.position());
            System.out.println("CHAINED_TARGETING_REVIEW: no-dummy birch canopy/trunk landing passed at "+p.position()+", 200 aura, 1-second cooldown, body clear of leaves");
            for(int x=-4;x<=24;x++)for(int z=-5;z<=5;z++)for(int y=71;y<=76;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            for(int x=-2;x<=20;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(x,73,z),Blocks.STONE.defaultBlockState());
            p.teleportTo(level,START.x,START.y,START.z,-90,-1);p.setOnGround(true);i.setCoolDown(0,0);energy.setAura(10000);i.onPressed(p,0,0);
            if(p.getX()<=10.5 || p.getY()!=71 || Math.abs(energy.getAura()-9800)>.001 || i.getCoolDown(0)!=1)throw new IllegalStateException("Real-player low-ceiling landing failed: "+p.position());
            System.out.println("CHAINED_TARGETING_REVIEW: 1-degree upward aim under two-block ceiling passed at "+p.position());
            for(int x=-2;x<=20;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(x,73,z),Blocks.AIR.defaultBlockState());
            for(var block:new net.minecraft.world.level.block.Block[]{Blocks.STONE_SLAB,Blocks.STONE}){
                for(int x=10;x<=20;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(x,71,z),block.defaultBlockState());
                p.teleportTo(level,START.x,START.y,START.z,-90,0);p.setOnGround(true);i.setCoolDown(0,0);energy.setAura(10000);i.onPressed(p,0,0);
                if(p.getX()<=10.5 || p.getY()!=(block==Blocks.STONE_SLAB?71.5:72) || Math.abs(energy.getAura()-9800)>.001 || i.getCoolDown(0)!=1)throw new IllegalStateException("Real-player terrain support failed: "+p.position());
                System.out.println("CHAINED_TARGETING_REVIEW: "+block+" support landing passed at "+p.position());
            }
            for(int x=10;x<=20;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(x,71,z),Blocks.AIR.defaultBlockState());
            for(int y=71;y<=74;y++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(new BlockPos(1,y,z),Blocks.STONE.defaultBlockState());
            p.teleportTo(level,.6,71,.5,-90,0);p.setOnGround(true);i.setCoolDown(0,0);energy.setAura(10000);i.onPressed(p,0,0);
            if(p.getX()<=.61 || p.getX()>=.71 || p.getY()!=71 || Math.abs(energy.getAura()-9800)>.001 || i.getCoolDown(0)!=1)throw new IllegalStateException("Real-player close wall stop failed: "+p.position());
            System.out.println("CHAINED_TARGETING_REVIEW: close wall short forward cast passed at "+p.position());
            for(int y=71;y<=74;y++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(new BlockPos(1,y,z),Blocks.AIR.defaultBlockState());
            for(float pitch:new float[]{0,-1,-3,-5,-15,-45,-80}){
                p.teleportTo(level,START.x,START.y,START.z,-90,pitch);p.setOnGround(true);i.setCoolDown(0,0);energy.setAura(10000);Vec3 end=START.add(p.getLookAngle().scale(16));
                i.onPressed(p,0,0);if(p.position().distanceToSqr(end)>.00001 || Math.abs(energy.getAura()-9800)>.001 || i.getCoolDown(0)!=1)throw new IllegalStateException("Real-player air aiming failed at pitch "+pitch+": "+p.position());
            }
            System.out.println("CHAINED_TARGETING_REVIEW: real player passed level/upward angles 0, -1, -3, -5, -15, -45, -80; each paid 200 aura and set a 1-second cooldown");
            p.teleportTo(level,START.x,START.y,START.z,-90,0);p.setOnGround(true);i.setCoolDown(0,0);energy.setAura(10000);
            near=target(level,START.add(8,0,0));far=target(level,START.add(14,0,0));castAfter=level.getGameTime()+6;ready=true;
        });}
        if(ready && !finished)server.execute(()->{
            if(finished)return;var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
            if(level.getGameTime()<castAfter || level.getEntity(near.getUUID())!=near || !level.getEntitiesOfClass(Husk.class,near.getBoundingBox()).contains(near))return;
            var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);p.setOnGround(true);i.setCoolDown(0,0);i.onPressed(p,0,0);
            if(p.getX()<7 || p.getX()>=near.getBoundingBox().minX || p.getBoundingBox().intersects(near.getBoundingBox()) || near.getHealth()>=1024 || far.getHealth()!=1024 || Math.abs(energy.getAura()-9800)>.001 || i.getCoolDown(0)!=1)throw new IllegalStateException("Real-player enemy stop/payment/damage failed: "+p.position());
            expected=p.position();finished=true;System.out.println("CHAINED_TARGETING_REVIEW: stopped at "+expected+" before target "+near.position()+"; near HP="+near.getHealth()+", far HP="+far.getHealth()+", aura="+energy.getAura()+", Myths absent="+!net.neoforged.fml.ModList.get().isLoaded("wildhunt"));
        });
        if(!finished)return;age++;if(mc.screen!=null)mc.setScreen(null);mc.options.hideGui=true;
        if(age==20){if(mc.player.position().distanceToSqr(expected)>1)throw new IllegalStateException("Enemy stop did not synchronize to the client: "+mc.player.position());System.out.println("CHAINED_TARGETING_REVIEW: client synchronized at "+mc.player.position());}
        if(age==22){mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);mc.player.setYRot(-140);mc.player.setXRot(18);}
        if(age==30){try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var dir=java.nio.file.Path.of("../docs/media");java.nio.file.Files.createDirectories(dir);screenshot.writeToFile(dir.resolve("chained-enemy-stop.png"));}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        if(age==50)server.execute(()->{near.discard();far.discard();});
        if(age==55){mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.options.hideGui=false;System.out.println("CHAINED_TARGETING_REVIEW: complete");mc.stop();}
    }
    private static Husk target(net.minecraft.server.level.ServerLevel level,Vec3 position){var e=new Husk(EntityType.HUSK,level);e.setPos(position);e.setNoAi(true);e.setNoGravity(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);e.setHealth(1024);level.addFreshEntity(e);return e;}
}
