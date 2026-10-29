package dev.heavenlyrestriction;
import dev.heavenlyrestriction.client.SchemaScreen;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;
/** Opt-in real screen/save-packet review in a disposable world only. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class SchemaClientReview {
    private static boolean prepared;private static volatile int phase;private static int age,frames;
    private static void check(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private static void press(SchemaScreen screen,String text){var button=screen.children().stream().filter(c->c instanceof Button b && b.getMessage().getString().equals(text)).map(c->(Button)c).findFirst().orElseThrow(()->new IllegalStateException("Missing button "+text));button.onPress();}
    private static void shot(Minecraft mc,String name){try(var pic=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(Path.of("schema-review"));pic.writeToFile(Path.of("schema-review",name+".png"));}catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null || mc.player==null || mc.player.tickCount<100)return;
        check(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Schema Validation"),"Requires disposable Schema Validation world");
        if(++age>2000){mc.stop();throw new IllegalStateException("Schema review timed out at "+phase);}
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);
            for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());
            p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).removeModifiers();p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).setBaseValue(1);
            var energy=TensuraStorages.getExistenceFrom(p);energy.setSleepModeTime(-1);energy.setAura(io.github.manasmods.tensura.util.EnergyHelper.getMaxAura(p));
            SchemaArt.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(i.getMastery()==-1000,"Learning requirement");HeavenlyRestriction.SCHEMA.get().addLearnPoint(i,p,0,1000);check(i.getMastery()>=0,"Native learning completion");
            i.setMastery(0);SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();phase=1;i.onPressed(p,0,0);
        });return;}
        if(phase==1 && mc.screen instanceof SchemaScreen screen){
            mc.gui.getChat().clearMessages(false);mc.options.hideGui=true;
            for(int n=0;n<6;n++)press(screen,"Add");press(screen,"Apply");phase=2;frames=0;
        }
        if(phase==2 && age%5==0)server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();
            if(SchemaArt.count(SchemaArt.build(i))==6){i.onToggleOn(p);check(Math.abs(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER)-10.84375)<.001,"Initial six-stack rate");check(Math.abs(SchemaArt.damageFactor(p)-.4)<.001,"Full stack penalty");phase=3;frames=0;}
        });
        if(phase==3 && ++frames==15){shot(mc,"schema-initial-build");phase=4;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();i.setMastery(2000);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);SchemaArt.refresh(p);check(Math.abs(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER)-31)<.001,"Mastered six-stack rate");SchemaNetwork.send(p,false,"");((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();phase=5;frames=0;
        });}
        if(phase==5 && mc.screen instanceof SchemaScreen screen && ++frames==18){shot(mc,"schema-mastered-build");press(screen,"Clear");press(screen,"Close");phase=6;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(SchemaArt.count(SchemaArt.build(i))==6,"Closing saved an unapplied draft");i.onPressed(p,0,0);phase=7;frames=0;
        });}
        if(phase==7 && mc.screen instanceof SchemaScreen && ++frames==12){phase=8;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(SchemaArt.count(SchemaArt.build(i))==6,"Reopen lost build");i.onToggleOff(p);check(Math.abs(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER)-1)<.001,"Toggle left regen");
            try{Files.createDirectories(Path.of("schema-review"));Files.writeString(Path.of("schema-review/passed.txt"),"Real Schema Build: six Add clicks, Apply packet, 10.84375x regen / 0.4x damage at zero mastery; 31x regen at mastery; unapplied Clear discarded; reopening persisted six slots; toggle off removed bonus.\n");}catch(java.io.IOException ex){throw new IllegalStateException(ex);}phase=9;frames=0;
        });}
        if(phase==9 && ++frames==10)mc.stop();
    }
}
