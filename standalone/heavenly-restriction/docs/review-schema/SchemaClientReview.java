package dev.heavenlyrestriction;
import dev.heavenlyrestriction.client.SchemaScreen;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    private static boolean prepared;private static volatile int phase;private static int age,frames;private static double baseAttack;
    private static void check(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private static void near(double actual,double expected,String why){check(Math.abs(actual-expected)<.001,why+": "+actual+" expected "+expected);}
    private static void press(SchemaScreen screen,String text){var button=screen.children().stream().filter(c->c instanceof Button b && b.getMessage().getString().equals(text)).map(c->(Button)c).findFirst().orElseThrow(()->new IllegalStateException("Missing button "+text));button.onPress();}
    private static void shot(Minecraft mc,String name){try(var pic=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(Path.of("schema-review"));pic.writeToFile(Path.of("schema-review",name+".png"));}catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null || mc.player==null || mc.player.tickCount<100)return;
        check(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Schema Validation"),"Requires disposable Schema Validation world");
        if(++age>2000){mc.stop();throw new IllegalStateException("Schema review timed out at "+phase);}
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);
            for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());
            p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).removeModifiers();p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).setBaseValue(1);baseAttack=p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            var energy=TensuraStorages.getExistenceFrom(p);energy.setSleepModeTime(-1);energy.setAura(io.github.manasmods.tensura.util.EnergyHelper.getMaxAura(p));
            SchemaArt.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(i.getMastery()==-1000,"Learning requirement");HeavenlyRestriction.SCHEMA.get().addLearnPoint(i,p,0,1000);check(i.getMastery()>=0,"Native learning completion");
            i.setMastery(0);SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();phase=1;i.onPressed(p,0,0);
        });return;}
        if(phase==1 && mc.screen instanceof SchemaScreen screen){
            mc.gui.getChat().clearMessages(false);mc.options.hideGui=true;
            for(int n=0;n<6;n++)press(screen,"Add Mito");press(screen,"Apply");phase=2;frames=0;
        }
        if(phase==2 && age%5==0)server.execute(()->{
            if(phase!=2)return;var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();
            if(SchemaArt.count(SchemaArt.build(i),SchemaArt.MITOCHONDRIAL)==6){i.onToggleOn(p);near(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),10.84375,"Initial six Mito rate");near(SchemaArt.damageFactor(p),.7,"Reduced six Mito penalty");phase=3;frames=0;}
        });
        if(phase==3 && mc.screen instanceof SchemaScreen screen && ++frames==10){
            press(screen,"Clear");for(int n=0;n<3;n++){press(screen,"Add Str.");press(screen,"Add Mito");}press(screen,"Up");press(screen,"Down");press(screen,"Apply");phase=4;
        }
        if(phase==4 && age%5==0)server.execute(()->{
            if(phase!=4)return;var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();
            if(Arrays.equals(SchemaArt.build(i),new int[]{2,1,2,1,2,1})){
                near(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),4.28125,"Mixed initial recovery");near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack+6.5625,"Mixed initial Strength");near(SchemaArt.damageFactor(p),.85,"Mixed outgoing penalty");near(SchemaArt.damageTakenFactor(p),1.06,"Mixed vulnerability");phase=5;frames=0;
            }
        });
        if(phase==5 && ++frames==15){shot(mc,"schema-initial-build");phase=6;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();i.setMastery(2000);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);SchemaArt.refresh(p);
            near(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),16,"Mastered mixed rate");near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack+15,"Mastered mixed Strength");SchemaNetwork.send(p,false,"");((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();phase=7;frames=0;
        });}
        if(phase==7 && mc.screen instanceof SchemaScreen screen && ++frames==18){shot(mc,"schema-mastered-build");press(screen,"Clear");press(screen,"Close");phase=8;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(Arrays.equals(SchemaArt.build(i),new int[]{2,1,2,1,2,1}),"Closing saved an unapplied draft");i.onPressed(p,0,0);phase=9;frames=0;
        });}
        if(phase==9 && mc.screen instanceof SchemaScreen && ++frames==12){phase=10;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(Arrays.equals(SchemaArt.build(i),new int[]{2,1,2,1,2,1}),"Reopen lost mixed build");i.onToggleOff(p);near(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),1,"Toggle left regen");near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack,"Toggle left Strength");near(SchemaArt.damageTakenFactor(p),1,"Toggle left vulnerability");
            try{Files.createDirectories(Path.of("schema-review"));Files.writeString(Path.of("schema-review/passed.txt"),"Real Build: six Mito Add clicks and Apply: 10.84375x regen / 0.7x damage. Mixed Add Str./Add Mito, Up/Down and Apply persisted [2,1,2,1,2,1]. Initial 4.28125x regen / +6.5625 attack; mastered 16x regen / +15 attack; 0.85x damage dealt / 1.06x taken. Draft Clear discarded; reopening retained mixed tree; toggle off removed all bonuses and drawbacks.\n");}catch(java.io.IOException ex){throw new IllegalStateException(ex);}phase=11;frames=0;
        });}
        if(phase==11 && ++frames==10)mc.stop();
    }
}
