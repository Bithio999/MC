package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

/** Actual native damage, economy and timing checks for the cross-tier rebalance. */
@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class TierBalanceGameTests {
    private static final class CombatTarget extends Husk {
        int attempts, accepted;
        double applied;
        CombatTarget(GameTestHelper h,Vec3 p){
            super(EntityType.HUSK,h.getLevel());var chunk=new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(p));h.getLevel().getChunk(chunk.x,chunk.z);h.getLevel().setChunkForced(chunk.x,chunk.z,true);setPos(p);setNoAi(true);setNoGravity(true);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);setHealth(1024);
            // Protect the test fixture from large hits without bypassing native combat.
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,600,3));h.getLevel().addFreshEntity(this);
        }
        @Override public boolean hurt(DamageSource source,float amount){
            attempts++;float before=getHealth();boolean result=super.hurt(source,amount);
            if(result && before>getHealth()){accepted++;applied+=before-getHealth();}
            // A replenishing practice target: retain all native hit immunity timers.
            if(isAlive())setHealth(getMaxHealth());return result;
        }
        void reset(){attempts=0;accepted=0;applied=0;invulnerableTime=0;HunterData.of(this).remove("drenched_until");}
    }
    private static ManasSkillInstance apex(net.minecraft.world.entity.LivingEntity p,Apex kind){
        var s=WildHunt.UNIQUE.get(kind).get();HunterData.learnReward(p,s);var i=SkillAPI.getSkillsFrom(p).getSkill(s).orElseThrow();i.setMastery(i.getMaxMastery());return i;
    }
    private static void opener(GameTestHelper h,Apex kind,float old){
        var p=player(h);var z=new CombatTarget(h,p.position().add(8,0,0));var i=apex(p,kind);var s=(ApexSkill)i.getSkill();
        afterTracking(h,List.of(z),()->{
            s.strike(h.getLevel(),i,p,z,old*.9f,0,false);double previous=z.applied;z.reset();
            double aura=TensuraStorages.getExistenceFrom(p).getAura();cast(p,i,0);
            for(int n=0;n<16;n++)s.tickActive(i,p);double current=z.applied;
            h.assertTrue(previous>0 && current>=previous*2.5,"Apex opener did not improve in native combat: "+kind+" old="+previous+" current="+current);
            h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura()==aura-s.balance.primaryAura(),"Apex opener charged the wrong aura budget: "+kind);
            z.reset();i.setCoolDown(0,0);EnergyHelper.setBaseMaxEP(p,40_000_000);cast(p,i,0);for(int n=0;n<16;n++)s.tickActive(i,p);
            h.assertTrue(Math.abs(z.applied-current)<.01,"Apex damage changed with EP: "+kind);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100) public static void stormCrownBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){opener(h,Apex.REY_DAU,160);}
    @GameTest(template="empty",timeoutTicks=100) public static void scarletDelugeBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){opener(h,Apex.UTH_DUNA,120);}
    @GameTest(template="empty",timeoutTicks=100) public static void blackFlameBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){opener(h,Apex.NU_UDRA,125);}
    @GameTest(template="empty",timeoutTicks=100) public static void frostboundBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){opener(h,Apex.JIN_DAHAAD,180);}

    private static void mythic(GameTestHelper h,MythicKind kind,float old){
        var p=player(h);var z=new CombatTarget(h,p.position().add(8,0,0));var i=learn(p,kind);i.setMastery(i.getMaxMastery());var s=(MythicSkill)i.getSkill();
        afterTracking(h,List.of(z),()->{
            int mode=kind==MythicKind.RUNE_SMITH || kind==MythicKind.VERDANT_KEEPER?1:0;
            var type=kind==MythicKind.THREADWEAVER?TensuraDamageTypes.SPACE_ELEMENTAL:kind==MythicKind.MIRRORBOUND?TensuraDamageTypes.LIGHT_ELEMENTAL:TensuraDamageTypes.EARTH_ELEMENTAL;
            MythicArts.hit(s,i,p,z,old,mode,type);double previous=z.applied;z.reset();
            double aura=TensuraStorages.getExistenceFrom(p).getAura();if(kind==MythicKind.RUNE_SMITH)cast(p,i,0);cast(p,i,mode);double current=z.applied;
            h.assertTrue(previous>0 && current>=previous*2.3,"Unique art did not improve in native combat: "+kind+" old="+previous+" current="+current);
            double cost=kind.costs[mode]+(kind==MythicKind.RUNE_SMITH?kind.costs[0]:0);
            h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura()==aura-cost,"Unique art charged the wrong aura budget: "+kind);
            z.reset();for(int n=0;n<i.getModes();n++)i.setCoolDown(0,n);EnergyHelper.setBaseMaxEP(p,40_000_000);
            if(kind==MythicKind.RUNE_SMITH)cast(p,i,0);cast(p,i,mode);
            h.assertTrue(Math.abs(z.applied-current)<.01,"Unique damage changed with EP: "+kind);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100) public static void threadweaverBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){mythic(h,MythicKind.THREADWEAVER,160);}
    @GameTest(template="empty",timeoutTicks=100) public static void runeSmithBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){mythic(h,MythicKind.RUNE_SMITH,420);}
    @GameTest(template="empty",timeoutTicks=100) public static void verdantBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){mythic(h,MythicKind.VERDANT_KEEPER,190);}
    @GameTest(template="empty",timeoutTicks=100) public static void mirrorboundBudgetUsesNativeDamageAndFixedEP(GameTestHelper h){mythic(h,MythicKind.MIRRORBOUND,220);}

    @GameTest(template="empty",timeoutTicks=100)
    public static void groveSustainImprovesButStaysBelowOrchard(GameTestHelper h){
        var p=player(h);var grove=learn(p,MythicKind.VERDANT_KEEPER);var orchard=learn(p,MythicKind.DEMETER);var energy=TensuraStorages.getExistenceFrom(p);
        p.setXRot(90);cast(p,grove,0);ApexArts.point(grove.getOrCreateTag(),"grove",p.position());
        double max=p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);p.setHealth(p.getMaxHealth()*.2f);energy.setSpiritualHealth(max*.2);float hp=p.getHealth();tick(p,grove);
        double hpGrove=p.getHealth()-hp,shpGrove=energy.getSpiritualHealth()-max*.2;
        h.assertTrue(Math.abs(hpGrove/p.getMaxHealth()-.07)<.001 && Math.abs(shpGrove/max-.06)<.001,"Grove failed its 7% HP / 6% SHP pulse");
        cast(p,orchard,0);ApexArts.point(orchard.getOrCreateTag(),"orchard",p.position());p.setHealth(p.getMaxHealth()*.2f);energy.setSpiritualHealth(max*.2);hp=p.getHealth();tick(p,orchard);
        h.assertTrue(p.getHealth()-hp>hpGrove && energy.getSpiritualHealth()-max*.2>shpGrove,"Unique support eclipsed the Ultimate orchard");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160)
    public static void aionSustainedBudgetKeepsTwelvePulsesAndNativeImmunity(GameTestHelper h){
        var p=player(h);var z=new CombatTarget(h,p.position().add(8,0,0));HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();var s=WildHunt.GODLY.get();
        afterTracking(h,List.of(z),()->{double mp=TensuraStorages.getExistenceFrom(p).getMagicule(),aura=TensuraStorages.getExistenceFrom(p).getAura();s.onPressed(i,p,0,0);
            for(int elapsed=1;elapsed<=120;elapsed++){final int age=elapsed;h.runAfterDelay(age,()->{
                s.onHeld(i,p,age,0);if(age==120){
                    h.assertTrue(z.attempts==12 && z.accepted==12 && z.applied>2500,"Aion lost sustained native damage or its twelve-pulse limit: "+z.attempts+" / "+z.accepted+" / "+z.applied);
                    h.assertTrue(TensuraStorages.getExistenceFrom(p).getMagicule()==mp-15600 && TensuraStorages.getExistenceFrom(p).getAura()==aura,"Aion channel overbilled or spent aura");
                    h.assertTrue(i.getCoolDown(0)==4 && !i.getOrCreateTag().contains("channel_until"),"Aion failed its channel end/cooldown");h.succeed();
                }
            });}
        });
    }
    @GameTest(template="empty",timeoutTicks=260)
    public static void thothSustainedBudgetAccountsForNativeHitImmunity(GameTestHelper h){
        var p=player(h);var current=new CombatTarget(h,p.position().add(8,0,0));var old=new CombatTarget(h,p.position().add(40,0,0));var i=learn(p,MythicKind.THOTH);var s=(MythicSkill)i.getSkill();
        afterTracking(h,List.of(current,old),()->{double mp=TensuraStorages.getExistenceFrom(p).getMagicule();cast(p,i,3);
            for(int elapsed=0;elapsed<=200;elapsed++){final int age=elapsed;h.runAfterDelay(age,()->{
                tick(p,i);if(age>=10 && age<200 && (age-10)%5==0)MythicArts.hit(s,i,p,old,160,3,ThothElement.at((age-10)/5).damage);
                if(age==200){
                    h.assertTrue(current.attempts==38 && current.accepted==old.accepted && current.accepted>0,"Thoth changed duration, volleys or matched-baseline native hit handling: "+current.attempts+" / "+current.accepted);
                    h.assertTrue(old.applied>0 && current.applied>=old.applied*4.8,"Library strength did not survive native combat handling: "+current.applied+" / "+old.applied);
                    h.assertTrue(TensuraStorages.getExistenceFrom(p).getMagicule()==mp-24000,"Library charged the wrong magicule budget");h.succeed();
                }
            });}
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lughUpgradesOldArchiveWithoutLosingObservationsOrSelection(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);var list=LughArts.archive(i);list.getCompound(0).putFloat("damage",2200);list.getCompound(1).putFloat("damage",2200);list.getCompound(2).putString("source","wildhunt:rune_smith");list.getCompound(2).putString("name","Saved technique");list.getCompound(2).putFloat("damage",2700);i.getOrCreateTag().putInt("selected_art",2);
        var loaded=ManasSkillInstance.fromNBT(i.toNBT());var archive=LughArts.archive(loaded);
        h.assertTrue(archive.getCompound(0).getFloat("damage")==3800 && archive.getCompound(1).getFloat("damage")==3800,"Saved origin arts did not upgrade");
        h.assertTrue(archive.getCompound(2).getFloat("damage")==3500 && archive.getCompound(2).getString("source").equals("wildhunt:rune_smith") && LughArts.selectedName(loaded).equals("Saved technique"),"Archive upgrade lost an observation or selection");h.succeed();
    }
}
