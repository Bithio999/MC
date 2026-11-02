package dev.heavenlyrestriction;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class ExponentiationReview {
 private static final Path OUT=Path.of("/workspace/validation-exponentiation/capture");private static volatile int phase;private static int age,tick;private static volatile String error;
 private static class Menu extends AbilitySelectionScreen {
  Menu(){super(AbilityCategoriesScreen.AbilityType.SKILL,Skill.SkillType.UNIQUE,null);}
  void search(){searchField.setValue("Exponentiation");}
  void choose(){int at=-1;for(int n=0;n<filtered.size();n++)if(filtered.get(n).getSkill()==HeavenlyRestriction.EXPONENTIATION.get()){at=n;break;}check(at>=0,"Missing from native Unique list");mouseClicked(guiLeft+12,guiTop+46+13*(at-listStartIndex)+5,0);check(selectedAbility!=null && selectedAbility.getSkill()==HeavenlyRestriction.EXPONENTIATION.get(),"Native row select failed");}
  void equip(){choose();check(handleClickedSlot(1,0),"Native equip handler failed");}
  void unequip(){check(handleClickedSlot(1,2),"Native unequip handler failed");}
 }
 private static void check(boolean ok,String m){if(!ok){error=m;throw new IllegalStateException(m);}}
 private static void near(double a,double b,String m){check(Math.abs(a-b)<.02,m+": "+a+" != "+b);}
 private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work){var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->{try{work.accept(s.getPlayerList().getPlayers().getFirst());}catch(Throwable e){error=e.toString();}});}
 private static void shot(String name){try{Files.createDirectories(OUT);try(var im=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){im.writeToFile(OUT.resolve(name+".png"));}}catch(Exception e){error=e.toString();}}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
  if(++age>3000 || error!=null){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error==null?"Timeout phase "+phase:error);}catch(Exception ignored){}mc.stop();return;}
  if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || mc.getOverlay()!=null)return;
  if(phase==0 && mc.player.tickCount>60){phase=1;mc.setScreen(null);try{Files.createDirectories(OUT);Files.deleteIfExists(OUT.resolve("passed.txt"));Files.deleteIfExists(OUT.resolve("failure.txt"));}catch(Exception ex){throw new RuntimeException(ex);}server(p->{check(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Exponentiation Review"),"Disposable save required");p.removeAllEffects();p.getInventory().clearContent();for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i);RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);p.setGameMode(GameType.SURVIVAL);for(int slot=0;slot<3;slot++)TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,slot,null,0);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4);p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.1);p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(1000);p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(1000);p.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(100);p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).setBaseValue(0);p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER).setBaseValue(0);p.getAttribute(TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION).setBaseValue(0);try(var reset=ExponentiationContext.raw(p,ExponentiationContext.RESTORE)){p.setHealth(50);var pool=TensuraStorages.getExistenceFrom(p);pool.setAura(500);pool.setMagicule(500);pool.setSpiritualHealth(50);pool.setSleepModeTime(-1);}RestrictionData.learnReward(p,HeavenlyRestriction.EXPONENTIATION.get());near(p.getMaxHealth(),100,"Ownership alone enabled passive");p.teleportTo(.5,159,.5);p.setYRot(180);p.setXRot(0);phase=2;});tick=0;return;}
  if(phase==2 && ++tick>35){var m=new Menu();mc.setScreen(m);m.search();phase=3;tick=0;return;}
  if(phase==3){tick++;if(tick==10 && mc.screen instanceof Menu m)m.choose();if(tick==20)shot("exponentiation-menu");if(tick==25 && mc.screen instanceof Menu m){m.equip();phase=4;tick=0;}return;}
  if(phase==4 && ++tick>25){near(mc.player.getMaxHealth(),100,"Client capacity did not synchronize");near(mc.player.getAttributeValue(Attributes.ATTACK_SPEED),16,"Client attack speed");near(mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED),.1,"Movement changed");shot("exponentiation-equipped");phase=5;tick=0;server(p->{check(Exponentiation.active(p),"Native equip packet failed on server");var pool=TensuraStorages.getExistenceFrom(p);try(var reset=ExponentiationContext.raw(p,ExponentiationContext.RESTORE)){p.setHealth(50);pool.setAura(500);pool.setMagicule(500);pool.setSpiritualHealth(50);}p.heal(5);near(p.getHealth(),75,"Native live-client healing");pool.setAura(505);near(pool.getAura(),525,"Native live-client aura gain");var saved=new net.minecraft.nbt.CompoundTag();p.addAdditionalSaveData(saved);float hp=p.getHealth();p.readAdditionalSaveData(saved);near(p.getHealth(),hp,"Player load squared existing HP");var nbt=new net.minecraft.nbt.CompoundTag();((io.github.manasmods.tensura.storage.ep.ExistenceStorage)pool).save(nbt);double ap=pool.getAura();((io.github.manasmods.tensura.storage.ep.ExistenceStorage)pool).load(nbt);near(pool.getAura(),ap,"Storage load squared current aura");try(var reset=ExponentiationContext.raw(p,ExponentiationContext.RESTORE)){p.setHealth(500);pool.setAura(1000);pool.setMagicule(1000);pool.setSpiritualHealth(100);}phase=6;});return;}
  if(phase==6 && ++tick>15 && mc.screen instanceof Menu m){m.unequip();phase=7;tick=0;return;}
  if(phase==7 && ++tick>25){near(mc.player.getMaxHealth(),100,"Client capacity failed to restore");phase=8;server(p->{check(!Exponentiation.active(p),"Native unequip packet failed");near(p.getHealth(),100,"HP over-cap survived unequip");near(p.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),100,"Saved base changed");check(TensuraStorages.getExistenceFrom(p).getAura()<=1000,"Over-cap aura survived unequip");try{Files.writeString(OUT.resolve("passed.txt"),"PASS: native Unique menu, translated name/icon, C2S equip/unequip, S2C stat synchronization, attack speed squared, movement retained, healing and aura gain squared once, player/storage NBT load without refills, normal capacity restoration and over-cap cleanup.\n");phase=9;}catch(Exception ex){throw new RuntimeException(ex);}});return;}
  if(phase==9)mc.stop();
 }
}
