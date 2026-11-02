package dev.heavenlyrestriction;
import dev.heavenlyrestriction.client.prospector.*;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.menu.RefiningMenu;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class ProspectorReview {
 private static final Path OUT=Path.of("/workspace/validation-prospector/capture");private static volatile int phase;private static int age,tick;private static volatile String error;
 private static class ToggleMenu extends AbilitySelectionScreen {
  ToggleMenu(){super(AbilityCategoriesScreen.AbilityType.SKILL,Skill.SkillType.UNIQUE,null);}
  void search(){searchField.setValue("Prospector");}
  void choose(){int at=-1;for(int n=0;n<filtered.size();n++)if(filtered.get(n).getSkill()==HeavenlyRestriction.PROSPECTOR.get()){at=n;break;}check(at>=0,"Prospector missing from native Unique menu");mouseClicked(guiLeft+12,guiTop+46+13*(at-listStartIndex)+5,0);check(selectedAbility!=null && selectedAbility.getSkill()==HeavenlyRestriction.PROSPECTOR.get(),"Could not select Prospector");}
  void toggle(){choose();check(mouseClicked(guiLeft+204,guiTop+45,0),"Native toggle click failed");}
 }
 private static void check(boolean ok,String m){if(!ok){error=m;throw new IllegalStateException(m);}}
 private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work){var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->{try{work.accept(s.getPlayerList().getPlayers().getFirst());}catch(Throwable e){error=e.toString();}});}
 private static void shot(String name){try{Files.createDirectories(OUT);try(var im=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){im.writeToFile(OUT.resolve(name+".png"));if(name.startsWith("prospector-ore-sense")){int colored=0;for(int y=140;y<355;y++)for(int x=320;x<960;x++){int c=im.getPixelRGBA(x,y),r=c&255,g=(c>>8)&255,b=(c>>16)&255;if(Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))>20)colored++;}check(colored>20,"Ore markers were not visibly rendered through terrain: "+colored);Files.writeString(OUT.resolve("visible-marker-pixels.txt"),"Colored marker pixels on grayscale wall: "+colored+"\n");}}}catch(Exception e){error=e.toString();}}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;if(++age>4500 || error!=null){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error==null?"Timeout phase "+phase:error);}catch(Exception ignored){}mc.stop();return;}if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null || mc.getOverlay()!=null)return;
  if(phase==0 && mc.player.tickCount>60){phase=1;mc.options.hideGui=false;mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.graphicsMode().set(GraphicsStatus.FANCY);mc.levelRenderer.allChanged();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setScreen(null);try{Files.createDirectories(OUT);Files.deleteIfExists(OUT.resolve("passed.txt"));Files.deleteIfExists(OUT.resolve("failure.txt"));}catch(Exception ex){throw new RuntimeException(ex);}server(p->{check(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Prospector Review"),"Disposable save required");p.removeAllEffects();for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i);RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);p.setGameMode(GameType.SURVIVAL);p.serverLevel().setDayTime(1000);p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);p.serverLevel().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,p.server);p.serverLevel().setWeatherParameters(6000,0,false,false);
   for(var entity:List.copyOf(p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(-55,100,-55,55,210,55))))if(entity!=p)entity.discard();
   for(int x=-38;x<=38;x++)for(int z=-38;z<=38;z++){p.serverLevel().setBlockAndUpdate(new BlockPos(x,158,z),Blocks.STONE.defaultBlockState());for(int y=159;y<=166;y++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
   String[] ores={"coal_ore","iron_ore","copper_ore","gold_ore","redstone_ore","lapis_ore","diamond_ore","emerald_ore","nether_quartz_ore","ancient_debris"};int[][] offsets={{-8,160,-12},{-4,161,-10},{5,160,-9},{4,163,-15},{-10,163,-17},{-7,163,-16},{-3,163,-16},{8,162,-14},{11,160,-17},{9,164,-19}};for(int n=0;n<ores.length;n++){var pos=offsets[n];p.serverLevel().setBlockAndUpdate(new BlockPos(pos[0],pos[1],pos[2]),BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("minecraft:"+ores[n])).defaultBlockState());}
   for(int x=0;x<2;x++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,161,-16),BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("tensura:magic_ore")).defaultBlockState());p.serverLevel().setBlockAndUpdate(new BlockPos(35,161,0),Blocks.DIAMOND_ORE.defaultBlockState());
   for(int x=-20;x<=20;x++)for(int y=159;y<=166;y++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,y,-6),Blocks.STONE.defaultBlockState());
   p.teleportTo(.5,159,.5);p.setYRot(180);p.setYHeadRot(180);p.setXRot(0);var i=HeavenlyRestriction.PROSPECTOR.get().createDefaultInstance();i.setMastery(0);check(SkillAPI.getSkillsFrom(p).learnSkill(i),"Native skill learn failed");io.github.manasmods.tensura.storage.TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,i.getSkill(),0);p.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));phase=2;});tick=0;return;}
  if(phase==2 && ++tick>35){var menu=new ToggleMenu();mc.setScreen(menu);menu.search();phase=3;tick=0;return;}
  if(phase==3 && tick==10 && mc.screen instanceof ToggleMenu menu){menu.choose();}if(phase==3 && tick==15){shot("prospector-skill-menu");}if(phase==3 && ++tick>20 && mc.screen instanceof ToggleMenu menu){menu.toggle();mc.setScreen(null);phase=4;tick=0;return;}
  if(phase==4){mc.player.setYRot(180);mc.player.setXRot(0);var survey=ProspectorClient.survey();check(survey.lastCells()<=OreSurvey.CELL_BUDGET && survey.lastSections()<=OreSurvey.SECTION_BUDGET,"Survey exceeded per-tick budget");if(++tick>80){var families=survey.markers().stream().map(m->m.kind().family()).collect(java.util.stream.Collectors.toSet());check(families.containsAll(Set.of("magic_ore","diamond","iron","gold","copper","coal","emerald","lapis","redstone","quartz","ancient_debris")),"Survey missed ores: "+families);check(survey.markers().stream().allMatch(m->m.pos().distToCenterSqr(mc.player.position())<=1024),"Survey exposed out-of-range ore");check(survey.markers().stream().filter(m->m.kind().family().equals("magic_ore")).anyMatch(m->m.count()==2),"Magic vein was not grouped");shot("prospector-ore-sense");mc.options.graphicsMode().set(GraphicsStatus.FABULOUS);mc.levelRenderer.allChanged();phase=44;tick=0;}return;}
  if(phase==44 && ++tick>40){check(mc.options.graphicsMode().get()==GraphicsStatus.FABULOUS,"Fabulous mode did not activate");shot("prospector-ore-sense-fabulous");phase=5;tick=0;return;}
  if(phase==5 && ++tick>10){var menu=new ToggleMenu();mc.setScreen(menu);menu.search();phase=6;tick=0;return;}
  if(phase==6 && ++tick>12 && mc.screen instanceof ToggleMenu menu){menu.toggle();mc.setScreen(null);phase=7;tick=0;return;}
  if(phase==7 && ++tick>12){check(ProspectorClient.survey().markers().isEmpty(),"Toggle-off retained ore vision");phase=8;tick=0;server(p->{check(ProspectorMining.active(p).isEmpty(),"Native toggle-off failed on server");var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PROSPECTOR.get()).orElseThrow();HeavenlyRestriction.PROSPECTOR.get().onPressed(i,p,0,0);check(p.containerMenu instanceof RefiningMenu,"Native refining menu missing");var menu=(RefiningMenu<?>)p.containerMenu;menu.brewingContainer.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));menu.brewingContainer.setItem(3,new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tensura:hipokute_grass"))));menu.broadcastChanges();});return;}
  if(phase==8 && mc.screen instanceof io.github.manasmods.tensura.client.screen.RefiningScreen){if(++tick==15)shot("prospector-refine");if(tick==20){mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,2);phase=9;tick=0;}return;}
  if(phase==9 && ++tick>140){phase=10;server(p->{var m=(RefiningMenu<?>)p.containerMenu;check(m.brewingContainer.isEmpty() && m.resultContainer.getItem(0).getCount()==1 && m.resultContainer.getItem(0).is(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tensura:high_potion"))),"Native client brew packet/ticks did not refine");});tick=0;return;}
  if(phase==10 && ++tick>10){shot("prospector-refine-result");mc.player.closeContainer();phase=11;tick=0;return;}
  if(phase==11 && ++tick>15){phase=12;server(p->{HeavenlyRestriction.PROSPECTOR.get().onPressed(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PROSPECTOR.get()).orElseThrow(),p,0,0);check(((RefiningMenu<?>)p.containerMenu).resultContainer.getItem(0).getCount()==1,"Reopening lost or duplicated output");});tick=0;return;}
  if(phase==12 && ++tick>20 && mc.screen instanceof io.github.manasmods.tensura.client.screen.RefiningScreen){try{Files.writeString(OUT.resolve("passed.txt"),"PASS: native Unique menu/icon/toggle packets; all 11 native ore families through solid stone; magic vein clustering; 32-block range; bounded section/cell scans; immediate toggle cleanup; native Refine S2C menu, C2S brew button, real ingredient consumption, high potion result, close/reopen persistence while passive off.\n");}catch(Exception ex){throw new RuntimeException(ex);}mc.stop();}
 }
}
