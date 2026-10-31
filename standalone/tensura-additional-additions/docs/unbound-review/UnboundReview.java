package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in real-player native menu, Counter synchronization, live damage and visual review. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class UnboundReview {
    private static boolean prepared;private static volatile boolean ready,hitVerified;private static int age;private static boolean slashCaptured;private static Husk enemy,behind;
    private static void near(double a,double b,String why){if(Math.abs(a-b)>.01)throw new IllegalStateException(why+": "+a+" != "+b);}
    private static void shot(Minecraft mc,String name){try(var picture=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var dir=java.nio.file.Path.of("../docs/media");java.nio.file.Files.createDirectories(dir);picture.writeToFile(dir.resolve(name+".png"));}catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
    private static Husk target(net.minecraft.server.level.ServerLevel level,Vec3 at){var e=new Husk(EntityType.HUSK,level);e.setPos(at);e.setNoAi(true);e.setNoGravity(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);e.setHealth(1024);level.addFreshEntity(e);return e;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null || server==null || mc.player.tickCount<100)return;
        if(!server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Restriction Validation"))throw new IllegalStateException("Review is restricted to the isolated validation world");
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            for(int x=-5;x<=24;x++)for(int z=-8;z<=8;z++){level.setBlockAndUpdate(new BlockPos(x,70,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=71;y<=77;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var owned:java.util.List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(owned);
            ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.getLast().get(),false);
            p.closeContainer();TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);TensuraStorages.getPlayerDataFrom(p).markDirty();p.setGameMode(GameType.SURVIVAL);p.teleportTo(level,.5,71,.5,-90,0);p.setOnGround(true);p.getAbilities().flying=false;p.onUpdateAbilities();
            for(var recovery:HeavenlyRestriction.REGENERATION)SkillAPI.getSkillsFrom(p).getSkill(recovery.get()).ifPresent(v->v.onToggleOff(p));p.setHealth(p.getMaxHealth());TensuraStorages.getExistenceFrom(p).setSpiritualHealth(p.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH));
            var skills=SkillAPI.getSkillsFrom(p);var old=skills.getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();old.setMastery(old.getMaxMastery());old.onToggleOff(p);TensuraStorages.getExistenceFrom(p).setTrueHero(true);EnergyHelper.setMaxAura(p,100000);ChainedState.refresh(p);double mp=EnergyHelper.getMaxMagicule(p),aura=EnergyHelper.getMaxAura(p);
            ChainedEvolution.discover(p);var i=skills.getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,i.getSkill(),0);skills.startHoldSkill(i,0,0);
            if(!ChainedState.ultimate(p) || !i.isMastered(p) || skills.getSkill(old.getSkill()).isPresent())throw new IllegalStateException("Real-player native menu evolution failed");near(EnergyHelper.getMaxMagicule(p),mp,"Evolution MP ceiling");near(EnergyHelper.getMaxAura(p),aura,"Evolution aura capacity");
            var latent=ExtraSkills.MAGIC_SENSE.get().createLearningInstance(p);latent.setMastery(-1);if(!skills.learnSkill(latent))throw new IllegalStateException("Unfinished skill was hidden");ExtraSkills.MAGIC_SENSE.get().addLearnPoint(latent,p,0,1000);if(latent.getMastery()!=-1)throw new IllegalStateException("Forbidden skill completed");
            var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);energy.markDirty();enemy=target(level,p.position().add(10,0,0));behind=target(level,p.position().add(13,0,0));
            for(int y=71;y<=73;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(5,y,z),Blocks.GLASS.defaultBlockState());
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,i.getSkill(),1);skills.markDirty();((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();
            System.out.println("UNBOUND_REVIEW: native menu evolution, inherited mastery/resources and visible blocked learning entry passed; Myths absent="+!net.neoforged.fml.ModList.get().isLoaded("wildhunt"));ready=true;
        });}
        if(!ready)return;age++;mc.options.hideGui=false;
        if(age==25)mc.setScreen(new io.github.manasmods.tensura.client.screen.AbilitySelectionScreen(io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen.AbilityType.SKILL,io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE,null));
        if(age==27 && mc.screen instanceof io.github.manasmods.tensura.client.screen.AbilitySelectionScreen screen)screen.selectedAbility=SkillAPI.getSkillsFrom(mc.player).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();
        if(age==30){if(!(mc.screen instanceof io.github.manasmods.tensura.client.screen.AbilitySelectionScreen))throw new IllegalStateException("Native Ultimate screen was replaced");if(SkillAPI.getSkillsFrom(mc.player).getSkill(HeavenlyRestriction.UNBOUND.get()).filter(i->i.isMastered(mc.player)).isEmpty())throw new IllegalStateException("Ultimate client synchronization failed");shot(mc,"unbound-menu");}
        if(age==40){mc.setScreen(null);mc.gui.getChat().clearMessages(true);mc.options.setCameraType(CameraType.FIRST_PERSON);var camera=new net.minecraft.world.entity.decoration.ArmorStand(EntityType.ARMOR_STAND,mc.level);camera.setPos(8,75,16);camera.setYRot(180);camera.setXRot(18);camera.setYHeadRot(180);camera.yHeadRotO=180;camera.setOldPosAndRot();mc.setCameraEntity(camera);}
        if(age==50)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();i.setCoolDown(1,0);TensuraStorages.getExistenceFrom(p).setAura(10000);UnboundCounter.press(i,p);near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),0,"Live Counter root");});
        if(age==60){var i=SkillAPI.getSkillsFrom(mc.player).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();if(!UnboundCounter.active(mc.player) || !i.canIgnoreCoolDown(mc.player,1))throw new IllegalStateException("Client cannot cancel the active stance during cooldown");shot(mc,"unbound-counter-stance");System.out.println("UNBOUND_REVIEW: client stance, root and cooldown-cancel synchronized");}
        if(age==65)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var energy=TensuraStorages.getExistenceFrom(p);float hp=p.getHealth();double shp=energy.getSpiritualHealth();p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(enemy),20);near(p.getHealth(),hp,"Real-player Counter HP");near(energy.getSpiritualHealth(),shp,"Real-player Counter SHP");if(UnboundCounter.active(p) || enemy.getHealth()>=1024 || behind.getHealth()>=1024)throw new IllegalStateException("Live Counter did not negate and cut through glass");System.out.println("UNBOUND_REVIEW: live enemy hit negated, HP/SHP unchanged, terrain-piercing slash hit both enemies; front HP="+enemy.getHealth()+", rear HP="+behind.getHealth());hitVerified=true;});
        if(!slashCaptured && hitVerified)for(var entity:mc.level.entitiesForRendering())if(entity instanceof ArtVisualEntity visual && visual.kind()==ArtVisualEntity.UNBOUND_SLASH && visual.tickCount>=5){shot(mc,"unbound-counter-slash");slashCaptured=true;System.out.println("UNBOUND_REVIEW: captured actual synchronized slash geometry at client age "+visual.tickCount);break;}
        if(age==90){if(!hitVerified || !slashCaptured || UnboundCounter.active(mc.player) || mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED)<=0)throw new IllegalStateException("Live Counter cleanup failed");System.out.println("UNBOUND_REVIEW: complete; native Ultimate menu, visible unfinished skill, server hit and client cleanup passed");mc.stop();}
    }
}
