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
            press(screen,"Clear");for(int n=0;n<2;n++){press(screen,"Add Regen");press(screen,"Add Mito");press(screen,"Add Str.");}press(screen,"Up");press(screen,"Down");press(screen,"Apply");phase=4;
        }
        if(phase==4 && age%5==0)server.execute(()->{
            if(phase!=4)return;var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();
            if(Arrays.equals(SchemaArt.build(i),new int[]{3,1,2,3,1,2})){
                near(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),3.8125,"Mixed initial recovery");near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack+1.40625,"Mixed initial Strength");near(SchemaArt.damageFactor(p),.9,"Mixed outgoing penalty");near(SchemaArt.damageTakenFactor(p),1.04,"Mixed vulnerability");near(SchemaArt.healingFactor(p),1.225,"Mixed healing factor");near(SchemaArt.costFactor(p),1.1,"Mixed cost factor");
                var energy=TensuraStorages.getExistenceFrom(p);energy.setMagicule(io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(p));double ap=energy.getAura(),mp=energy.getMagicule();check(!io.github.manasmods.tensura.util.EnergyHelper.isOutOfEnergy(p,10,10),"Funded client payment");near(energy.getAura(),ap-11,"Actual aura surcharge");near(energy.getMagicule(),mp-11,"Actual magicule surcharge");
                p.setHealth(p.getMaxHealth()*.5f);double hp=p.getHealth();p.heal(2);near(p.getHealth(),hp+2*1.225,"Actual client HP healing");energy.setSpiritualHealth(10);energy.setSpiritualHealth(20);near(energy.getSpiritualHealth(),22.25,"Actual client SHP healing");
                p.setHealth(p.getMaxHealth());energy.setSpiritualHealth(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));energy.setAura(io.github.manasmods.tensura.util.EnergyHelper.getMaxAura(p));i.setMastery(0);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);SchemaArt.refresh(p);SchemaNetwork.send(p,false,"");((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();phase=5;frames=0;
            }
        });
        if(phase==5 && ++frames==30){shot(mc,"schema-initial-build");phase=6;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();i.setMastery(2000);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);SchemaArt.refresh(p);
            near(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),11,"Mastered mixed rate");near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack+10,"Mastered mixed Strength");near(SchemaArt.healingFactor(p),1.4,"Mastered healing factor");near(SchemaArt.costFactor(p),1.1,"Mastered costs");SchemaNetwork.send(p,false,"");((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();phase=7;frames=0;
        });}
        if(phase==7 && mc.screen instanceof SchemaScreen screen && ++frames==18){shot(mc,"schema-mastered-build");press(screen,"Clear");press(screen,"Close");phase=8;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(Arrays.equals(SchemaArt.build(i),new int[]{3,1,2,3,1,2}),"Closing saved an unapplied draft");i.onPressed(p,0,0);phase=9;frames=0;
        });}
        if(phase==9 && mc.screen instanceof SchemaScreen && ++frames==12){phase=10;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();check(Arrays.equals(SchemaArt.build(i),new int[]{3,1,2,3,1,2}),"Reopen lost mixed build");i.onToggleOff(p);near(p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),1,"Toggle left regen");near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack,"Toggle left Strength");near(SchemaArt.damageTakenFactor(p),1,"Toggle left vulnerability");near(SchemaArt.healingFactor(p),1,"Toggle left healing");near(SchemaArt.costFactor(p),1,"Toggle left costs");
            try{Files.createDirectories(Path.of("schema-review"));Files.writeString(Path.of("schema-review/passed.txt"),"Real Build: six Mito Apply preserved 10.84375x regen / 0.7x damage. Add Regen, Add Mito, Add Str., Up/Down and Apply persisted [3,1,2,3,1,2]. Initial 3.8125x regen / +1.40625 attack / 1.225x healing; mastered 11x regen / +10 attack / 1.4x healing. Both: 0.9x outgoing / 1.04x incoming / 1.1x costs. Actual 10+10 payment withdrew 11 aura and 11 MP, heal(2) restored 2.45 HP, direct SHP +10 restored 12.25. Draft cancellation, reopening and toggle cleanup passed.\n");}catch(java.io.IOException ex){throw new IllegalStateException(ex);}phase=11;frames=0;
        });}
        if(phase==11 && ++frames==10)mc.stop();
    }
}
