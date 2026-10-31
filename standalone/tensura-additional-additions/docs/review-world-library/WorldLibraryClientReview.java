package dev.heavenlyrestriction;

import dev.heavenlyrestriction.client.WorldSelectScreen;
import dev.heavenlyrestriction.worldhop.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;

/** Opt-in native round trip. Refuses to edit anything outside the copied review instance. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class WorldLibraryClientReview {
    private static final Path OUT=Path.of("/workspace/validation-world-library/results");
    private static volatile int phase;private static int age,frames;private static MinecraftServer source;
    private static String idA,idB,nameA;private static long seedA,seedB;private static double ep;
    private static BlockPos buildB;private static Path pathB;private static int diamonds;
    private static Path path(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();}
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("WORLD_LIBRARY_REVIEW: "+message);}
    private static void log(String text){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("results.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    private static void screenshot(String name){try{Files.createDirectories(OUT);var mc=Minecraft.getInstance();try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(name+".png"));}}catch(Exception e){throw new RuntimeException(e);}}
    private static ManasSkillInstance skill(ServerPlayer p){return SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()).orElseThrow();}
    private static void jump(ServerPlayer p){source=p.server;/* Advance past the already-verified cooldown between scripted hops. */skill(p).setCoolDown(0,0);skill(p).onPressed(p,0,0);check(skill(p).getCoolDown(0)==300,"Accepted jump did not start five-minute cooldown");}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.screen!=null && mc.screen.getClass().getSimpleName().equals("LoadingErrorScreen"))for(var child:mc.screen.children())if(child instanceof Button b && b.getMessage().getString().equals("Proceed to main menu")){b.onPress();return;}
        if(mc.screen instanceof WorldSelectScreen screen){screen.children().stream().filter(c->c instanceof Button).forEach(c->((Button)c).setTooltip(null));}
        if(++age>14000){mc.stop();throw new IllegalStateException("World Select review timed out at "+phase);}
        if(phase==13 && ++frames>40){
            try{
                screenshot("singleplayer-before-delete");check(mc.getSingleplayerServer()==null,"Server still open for deletion");
                check(mc.getLevelSource().findLevelCandidates().levels().size()==2,"Hidden worlds appeared as normal saves");
                try(var access=mc.getLevelSource().validateAndCreateAccess(pathB.getFileName().toString())){access.deleteLevel();}
                check(!Files.exists(pathB) && Files.isRegularFile(pathB.getParent().resolve("Unrelated/level.dat")),"Native delete did not remove only the owner and its library");
                check(mc.getLevelSource().findLevelCandidates().levels().size()==1,"Deleted library left visible worlds");
                log("Native Singleplayer discovery shows only active + unrelated; native Delete removes active + entire hidden library, preserving unrelated save. PASS");
                Files.writeString(OUT.resolve("passed.txt"),"PASS: stable slot B → A → B → fresh; legacy migration; latest builds/current character; 128-world native creation cap; hidden save discovery; native owner deletion; unrelated save preserved.\n");
                mc.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new net.minecraft.client.gui.screens.TitleScreen()));phase=14;frames=0;
            }catch(Exception ex){throw new RuntimeException(ex);}return;
        }
        if(phase==14 && ++frames>30){screenshot("singleplayer-after-delete");phase=15;mc.stop();return;}
        var server=mc.getSingleplayerServer();if(server==null || mc.player==null || mc.getOverlay()!=null || mc.player.tickCount<60)return;
        if(phase==0){
            check(path(server).getFileName().toString().equals("World Library Review") && path(server).getParent().getParent().equals(Path.of("/workspace/validation-world-library/client")),"Not a disposable review world");
            phase=1;mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.renderDistance().set(3);server.execute(()->{
                try{
                    var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();var i=skill(p);i.setMastery(0);WorldSelect.select(p,"");var j=WorldSelect.journal(p);check(j.worlds().size()>=2,"Legacy previous world was not imported");
                    idB=j.current();pathB=path(server);seedB=server.overworld().getSeed();var a=j.worlds().stream().filter(e->!e.id().equals(idB)).findFirst().orElseThrow();idA=a.id();nameA=a.name();seedA=a.seed();
                    var jumper=HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get();i.getOrCreateTag().remove(jumper.getModeId(i,1));
                    check(!WorldSelect.select(p,idA),"Unlearned World Select accepted a destination");
                    jumper.learnMode(i,p,1,jumper.getLearningPointRequirement(i,p,1),false);check(jumper.isWorldSelectLearned(i,p),"Native mode learning failed");WorldSelect.select(p,"");
                    check(!WorldSelect.select(p,"../escape") && !WorldSelect.select(p,UUID.randomUUID().toString()) && !WorldSelect.select(p,idB),"Invalid destination accepted");
                    i.setMastery(-1);check(!WorldSelect.select(p,idA) && i.getModes()==1,"Learning gate bypassed");i.setMastery(0);
                    ep=EnergyHelper.getMaxEP(p);diamonds=37;p.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(Items.DIAMOND,diamonds));p.getPersistentData().putString("worldselect_progress","latest");
                    buildB=p.blockPosition().offset(4,0,0);p.serverLevel().setBlockAndUpdate(buildB,Blocks.DIAMOND_BLOCK.defaultBlockState());
                    Files.createDirectories(pathB.resolve("dimensions/review/test/region"));Files.writeString(pathB.resolve("dimensions/review/test/region/worldselect.keep"),"B terrain");
                    log("Legacy imported; learning/forged/current destination guards passed. A="+seedA+" B="+seedB);i.onPressed(p,0,1);phase=2;
                }catch(Exception e){throw new RuntimeException(e);}
            });return;
        }
        if(phase==2 && mc.screen instanceof WorldSelectScreen screen && ++frames>30){
            screenshot("world-select");var search=(EditBox)screen.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();search.setValue("no world matches this");
            check(screen.children().stream().filter(c->c instanceof Button b && b.visible && b.getMessage().getString().equals(nameA)).findAny().isEmpty(),"Search did not hide unmatched worlds");search.setValue(Long.toString(seedA));
            var button=(Button)screen.children().stream().filter(c->c instanceof Button b && b.visible && b.getMessage().getString().equals(nameA)).findFirst().orElseThrow();button.onPress();phase=3;frames=0;return;
        }
        if(phase==3 && mc.screen instanceof WorldSelectScreen screen && ++frames>15){
            screenshot("world-select-search");((EditBox)screen.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow()).setValue("");
            ((Button)screen.children().stream().filter(c->c instanceof Button b && b.getMessage().getString().equals("Set next jump")).findFirst().orElseThrow()).onPress();phase=4;frames=0;return;
        }
        if(phase==4 && ++frames>20){phase=40;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();check(p.getPersistentData().getString(WorldSelect.SELECTION).equals(idA),"Menu did not persist selected destination");log("Native UI search and selection packet passed");phase=5;});return;}
        if(phase==5){screenshot("world-select-selected");mc.options.guiScale().set(3);mc.resizeDisplay();phase=6;frames=0;return;}
        if(phase==6 && ++frames>30){screenshot("world-select-compact");if(Boolean.getBoolean("worldselect.review.menuOnly")){mc.stop();phase=99;return;}source=server;phase=7;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();jump(p);});return;}
        if((phase==7 || phase==9 || phase==11) && server!=source){
            int completing=phase;phase=90;server.execute(()->{
                try{
                    var p=server.getPlayerList().getPlayers().getFirst();if(p.getPersistentData().contains(WorldHopMetadata.MARKER) || WorldHopLibrary.pending(path(server))){phase=completing;return;}
                    check(p.getPersistentData().getString("worldselect_progress").equals("latest") && Math.abs(EnergyHelper.getMaxEP(p)-ep)<.01 && p.getInventory().getItem(1).is(Items.DIAMOND) && p.getInventory().getItem(1).getCount()==diamonds,"Character rolled back");
                    check(skill(p).onCoolDown(0) && skill(p).getCoolDown(0)<=300 && !skill(p).onCoolDown(1),"Cooldown lost across travel or blocked World Select");
                    var cooldown=skill(p).getCoolDown(0);double mastery=skill(p).getMastery();WorldHop.request(p,skill(p));check(skill(p).getCoolDown(0)==cooldown && skill(p).getMastery()==mastery,"Cooldown allowed another jump");
                    check(path(server).equals(pathB),"Active save folder changed");check(!p.getPersistentData().contains(WorldSelect.SELECTION),"Selection not consumed");check(WorldHop.safe(p.serverLevel(),p,p.blockPosition()),"Unsafe landing");var j=WorldSelect.journal(p);
                    if(completing==7){
                        check(server.overworld().getSeed()==seedA && j.current().equals(idA),"Did not return to A");check(WorldHopLibrary.archived(path(server),idB).isPresent(),"B hidden snapshot missing");check(!Files.exists(pathB.getParent().resolve("World Hop Preview")),"Legacy world stayed visible");check(mc.getLevelSource().findLevelCandidates().levels().size()==2,"Migration left additional visible saves");
                        diamonds=41;p.getInventory().getItem(1).setCount(diamonds);p.totalExperience=456;check(WorldSelect.select(p,idB),"Could not select B after returning to A");
                        log("Returned to A with current inventory, skills, EP and safe landing; B snapshot hidden and stable save folder retained");phase=8;
                    }else if(completing==9){
                        check(server.overworld().getSeed()==seedB && j.current().equals(idB) && j.worlds().size()==2,"Return duplicated worlds or changed B seed");
                        check(p.serverLevel().getBlockState(buildB).is(Blocks.DIAMOND_BLOCK) && p.totalExperience==456,"Latest B build or current XP lost");
                        check(Files.readString(path(server).resolve("dimensions/review/test/region/worldselect.keep")).equals("B terrain"),"Modded dimension terrain not retained");
                        check(!WorldSelect.select(p,idB),"Current world accepted");check(WorldSelect.select(p,idA) && WorldSelect.select(p,"") && WorldSelect.destination(p)==null,"New World did not clear destination");
                        log("Returned to latest B: actual diamond-block build and custom-dimension terrain retained; current 41 diamonds/XP intact; no duplicate world IDs");phase=10;
                    }else{
                        check(server.overworld().getSeed()!=seedA && server.overworld().getSeed()!=seedB && j.worlds().size()==3,"Unselected jump did not create new world");
                        check(server.getWorldData().getLevelName().endsWith(" · Parallel 2"),"Library-wide world name reset after revisiting: "+server.getWorldData().getLevelName());
                        check(!Files.exists(path(server).resolve("dimensions/review/test/region/worldselect.keep")),"Random jump retained old terrain");
                        var actual=WorldSelect.journal(p);var entries=new ArrayList<>(actual.worlds());
                        while(entries.size()<128){int n=entries.size();entries.add(new WorldHopJournal.Entry(UUID.randomUUID().toString(),pathB.getFileName().toString(),"Capacity test "+n,n));}
                        WorldHopJournal.write(pathB,new WorldHopJournal.Journal(actual.current(),entries));
                        try{check(WorldSelect.select(p,idA),"Full library rejected return selection");WorldSelect.select(p,"");double capMastery=skill(p).getMastery();skill(p).setCoolDown(0,0);skill(p).onPressed(p,0,0);check(skill(p).getMastery()==capMastery,"Full library allowed native creation");}
                        finally{WorldHopJournal.write(pathB,actual);}
                        log("128-world cap blocks native World Jump creation before mastery/transfer; returning remains selectable");
                        var integrated=(net.minecraft.client.server.IntegratedServer)server;check(integrated.publishServer(net.minecraft.world.level.GameType.SURVIVAL,false,0),"Could not publish test LAN");check(!WorldSelect.select(p,idA) && !WorldSelect.select(p,""),"LAN accepted selection");
                        log("Unselected jump created fresh world; selection used once; LAN selection rejected. PASS");phase=12;frames=0;
                    }
                }catch(Exception e){throw new RuntimeException(e);}
            });return;
        }
        if(phase==8){source=server;phase=9;server.execute(()->jump(server.getPlayerList().getPlayers().getFirst()));return;}
        if(phase==10){source=server;phase=11;server.execute(()->jump(server.getPlayerList().getPlayers().getFirst()));return;}
        if(phase==12 && ++frames>20){mc.level.disconnect();mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen());mc.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new net.minecraft.client.gui.screens.TitleScreen()));phase=13;frames=0;}
    }
}
