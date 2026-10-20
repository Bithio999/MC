package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class UniqueCalibrationGameTests {
    private static void near(GameTestHelper h,double actual,double expected,String message){h.assertTrue(Math.abs(actual-expected)<.02,message+": "+actual+" / "+expected);}
    private static void wound(LivingEntity e,double hp,double shp){e.setHealth((float)(e.getMaxHealth()-hp));TensuraStorages.getExistenceFrom(e).setSpiritualHealth(e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)-shp);}
    private static Wolf wolf(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,double size){
        var w=new Wolf(EntityType.WOLF,h.getLevel());w.setPos(p.position().add(2,0,0));w.setNoAi(true);w.setNoGravity(true);w.tame(p);
        w.getAttribute(Attributes.MAX_HEALTH).setBaseValue(size);w.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(size);w.setHealth(w.getMaxHealth());TensuraStorages.getExistenceFrom(w).setSpiritualHealth(size);h.getLevel().addFreshEntity(w);return w;
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveBillsActualHpAndShpAndFullPoolsAreFree(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);energy.setMagicule(1234);wound(p,10,20);
        p.setXRot(90);cast(p,i,0);ApexArts.point(i.getOrCreateTag(),"grove",p.position());double before=energy.getAura();i.setMastery(0);tick(p,i);
        near(h,before,99400,"Grove cast payment");near(h,before-energy.getAura(),1600,"Base pulse did not charge 40 AP/HP + 60 AP/SHP");near(h,p.getHealth(),p.getMaxHealth(),"HP not restored");near(h,energy.getSpiritualHealth(),p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),"SHP not restored");
        before=energy.getAura();i.getOrCreateTag().putLong("grove_next",0);tick(p,i);near(h,energy.getAura(),before,"Full recipients consumed pulse aura");
        i.setMastery(i.getMaxMastery());wound(p,10,20);i.getOrCreateTag().putLong("grove_next",0);tick(p,i);near(h,before-energy.getAura(),1100,"Mastered pulse did not charge 30 AP/HP + 40 AP/SHP");near(h,energy.getMagicule(),1234,"Grove consumed magicules");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveDoesNotRepeatPaymentInTheSameTick(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);wound(p,300,2000);
        cast(p,i,0);ApexArts.point(i.getOrCreateTag(),"grove",p.position());tick(p,i);double after=energy.getAura();float hp=p.getHealth();double shp=energy.getSpiritualHealth();tick(p,i);
        near(h,energy.getAura(),after,"Duplicate tick billed again");near(h,p.getHealth(),hp,"Duplicate tick healed HP");near(h,energy.getSpiritualHealth(),shp,"Duplicate tick healed SHP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveSharesLimitedAuraProportionallyAndStopsAtZero(GameTestHelper h){
        var p=player(h);var w=wolf(h,p,100);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);
        wound(p,p.getMaxHealth()*.5,p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.5);double wmax=w.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);wound(w,w.getMaxHealth()*.5,wmax*.5);
        double max=p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),full=p.getMaxHealth()*.07*40+max*.06*60+w.getMaxHealth()*.07*40+wmax*.06*60;
        energy.setAura(full*.25);float hp=p.getHealth(),whp=w.getHealth();double shp=energy.getSpiritualHealth();
        GroveHealing.pulse(i,p,List.of(p,w));near(h,p.getHealth()-hp,p.getMaxHealth()*.0175,"Owner received unfair HP share");near(h,energy.getSpiritualHealth()-shp,max*.015,"Owner received unfair SHP share");near(h,w.getHealth()-whp,w.getMaxHealth()*.0175,"Pet received unfair HP share");near(h,TensuraStorages.getExistenceFrom(w).getSpiritualHealth(),wmax*.515,"Pet received unfair SHP share");near(h,energy.getAura(),0,"Partial pulse over/undercharged");
        energy.setAura(0);hp=p.getHealth();shp=energy.getSpiritualHealth();GroveHealing.pulse(i,p,List.of(p,w));near(h,p.getHealth(),hp,"Unfunded Grove healed HP");near(h,energy.getSpiritualHealth(),shp,"Unfunded Grove healed SHP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveRefundsCanceledHealingAndPreservesForeignAuraGains(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);wound(p,100,0);float hp=p.getHealth();
        Consumer<LivingHealEvent> cancel=event->{if(event.getEntity()==p){event.setCanceled(true);energy.setAura(energy.getAura()+50);}};
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL,cancel);
        try{GroveHealing.pulse(i,p,List.of(p));}finally{NeoForge.EVENT_BUS.unregister(cancel);}
        near(h,p.getHealth(),hp,"Canceled healing restored HP");near(h,energy.getAura(),100050,"Refund lost unrelated aura or charged canceled HP");
        p.heal(10);near(h,p.getHealth(),hp+10,"Grove quota leaked into unrelated healing");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveAmplificationCannotOverrunTheFundedQuota(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);wound(p,300,0);energy.setAura(100_000);float hp=p.getHealth();
        Consumer<LivingHealEvent> amplify=event->{if(event.getEntity()==p)event.setAmount(event.getAmount()*3);};NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL,amplify);
        try{GroveHealing.pulse(i,p,List.of(p));}finally{NeoForge.EVENT_BUS.unregister(amplify);}
        near(h,p.getHealth()-hp,p.getMaxHealth()*.07,"Native amplification exceeded the funded pulse quota");near(h,100_000-energy.getAura(),(p.getHealth()-hp)*40,"Amplified healing was not funded");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveReducedAndFractionalHealingOnlyPaysForActualPoints(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);wound(p,10,0);energy.setAura(100_000);float hp=p.getHealth();
        Consumer<LivingHealEvent> reduce=event->{if(event.getEntity()==p)event.setAmount(event.getAmount()*.5f);};NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL,reduce);
        try{GroveHealing.pulse(i,p,List.of(p));}finally{NeoForge.EVENT_BUS.unregister(reduce);}
        near(h,p.getHealth()-hp,5,"Native reduction was not honored");near(h,energy.getAura(),99800,"Unused native-reduced healing was not refunded");
        wound(p,.5,.25);double before=energy.getAura();GroveHealing.pulse(i,p,List.of(p));near(h,before-energy.getAura(),35,"Fractional HP/SHP were rounded into free or excess healing");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveHonorsNativeHealingSuppressionWithoutCharging(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);wound(p,100,500);energy.setAura(100_000);float hp=p.getHealth();double shp=energy.getSpiritualHealth();
        for(var learned:SkillAPI.getSkillsFrom(p).getLearnedSkills())if(learned.getSkill() instanceof AuraRegenerationSkill)learned.getSkill().onToggleOff(learned,p);
        var frost=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.FROST);p.addEffect(new MobEffectInstance(frost,100));
        h.assertTrue(p.hasEffect(frost),"Native suppression fixture failed");GroveHealing.pulse(i,p,List.of(p));near(h,p.getHealth(),hp,"Suppressed Grove healed HP");near(h,energy.getSpiritualHealth(),shp,"Suppressed Grove healed SHP");near(h,energy.getAura(),100_000,"Suppressed Grove charged aura");
        for(var learned:SkillAPI.getSkillsFrom(p).getLearnedSkills())if(learned.getSkill() instanceof AuraRegenerationSkill regen && regen.tier==3)regen.onToggleOn(learned,p);
        GroveHealing.pulse(i,p,List.of(p));h.assertTrue(p.getHealth()>hp && energy.getSpiritualHealth()>shp && energy.getAura()<100_000,"Funded Grove lost the existing Infinite Regeneration exception");
        var imprisoned=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.INFINITE_IMPRISONMENT);p.addEffect(new MobEffectInstance(imprisoned,100));h.assertTrue(p.hasEffect(imprisoned),"Imprisonment fixture failed");wound(p,100,500);hp=p.getHealth();shp=energy.getSpiritualHealth();double aura=energy.getAura();GroveHealing.pulse(i,p,List.of(p));near(h,p.getHealth(),hp,"Infinite Imprisonment allowed Grove HP recovery");near(h,energy.getSpiritualHealth(),shp,"Infinite Imprisonment allowed Grove SHP recovery");near(h,energy.getAura(),aura,"Imprisoned Grove charged aura");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveHealingBillScalesWithPoolSizeAndRecipientCount(GameTestHelper h){
        var p=player(h);var small=wolf(h,p,100);var large=wolf(h,p,1000);var i=learn(p,MythicKind.VERDANT_KEEPER);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);
        wound(small,small.getMaxHealth()*.5,small.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.5);GroveHealing.pulse(i,p,List.of(small));double first=100_000-energy.getAura();near(h,first,small.getMaxHealth()*.07*40+small.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.06*60,"Small pool pulse cost");
        wound(large,large.getMaxHealth()*.5,large.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.5);double before=energy.getAura();GroveHealing.pulse(i,p,List.of(large));double second=before-energy.getAura();near(h,second,large.getMaxHealth()*.07*40+large.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.06*60,"Large pools got fixed-cost healing");h.assertTrue(second>first*4,"Increasing the native HP pool did not increase the bill");
        wound(small,small.getMaxHealth()*.5,small.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.5);wound(large,large.getMaxHealth()*.5,large.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.5);before=energy.getAura();GroveHealing.pulse(i,p,List.of(small,large));near(h,before-energy.getAura(),first+second,"Extra recipient healed for free");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeHealerAndGroveRestoreMatchedPointsWithMeasuredPayments(GameTestHelper h){
        var p=player(h);var grove=learn(p,MythicKind.VERDANT_KEEPER);grove.setMastery(grove.getMaxMastery());var healer=UniqueSkills.HEALER.get();HunterData.learnReward(p,healer);var hi=SkillAPI.getSkillsFrom(p).getSkill(healer).orElseThrow();hi.setMastery(hi.getMaxMastery());
        var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);energy.setMagicule(100_000);wound(p,10,20);healer.onPressed(hi,p,0,0);near(h,100_000-energy.getMagicule(),1600,"Native Healer runtime payment diverges from inspected defaults");near(h,energy.getAura(),100_000,"Native Healer spent aura");
        wound(p,10,20);GroveHealing.pulse(grove,p,List.of(p));near(h,100_000-energy.getAura(),1100,"Grove did not fund the matched restoration");near(h,p.getHealth(),p.getMaxHealth(),"Matched Grove HP");near(h,energy.getSpiritualHealth(),p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),"Matched Grove SHP");
        System.out.println("[Unique calibration] Matched 10 HP / 20 SHP: native mastered Healer 1600 MP; mastered Grove 1100 AP, pulse-limited.");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeMusicianAndRailboltCompareActualMasteredCasts(GameTestHelper h){
        var p=player(h);var target=new net.minecraft.world.entity.animal.Cow(EntityType.COW,h.getLevel());target.setPos(p.position().add(8,0,0));target.setNoAi(true);target.setNoGravity(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);target.setHealth(1024);target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,3));h.getLevel().addFreshEntity(target);
        var nativeSkill=UniqueSkills.MUSICIAN.get();HunterData.learnReward(p,nativeSkill);var ni=SkillAPI.getSkillsFrom(p).getSkill(nativeSkill).orElseThrow();ni.setMastery(ni.getMaxMastery());var storm=WildHunt.UNIQUE.get(Apex.REY_DAU).get();HunterData.learnReward(p,storm);var si=SkillAPI.getSkillsFrom(p).getSkill(storm).orElseThrow();si.setMastery(si.getMaxMastery());
        afterTracking(h,List.of(target),()->{
            h.assertTrue(!GodlyPassives.ownsGodly(p) && !si.isToggled(),"External comparison contaminated by Godly ownership or a toggle");var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);energy.setMagicule(100_000);
            nativeSkill.onPressed(ni,p,0,0);double nativeDamage=1024-target.getHealth();near(h,100_000-energy.getMagicule(),50,"Musician native cast cost");target.setHealth(1024);target.invulnerableTime=0;
            storm.onPressed(si,p,0,0);double mythsDamage=1024-target.getHealth();near(h,100_000-energy.getAura(),600,"Railbolt cast cost");
            h.assertTrue(nativeDamage>0 && mythsDamage>0 && mythsDamage/nativeDamage<=3.2,"Railbolt exceeds the three-second versus one-second native attack benchmark: "+mythsDamage+" / "+nativeDamage);
            h.assertTrue(ni.getCoolDown(0)==1 && si.getCoolDown(0)==3,"Native comparison cooldowns changed");
            System.out.println("[Unique calibration] Native mastered Sonic Blast HP="+nativeDamage+" / 1s / 50 MP; mastered untoggled Railbolt HP="+mythsDamage+" / 3s / 600 AP. Same recipient/defenses; no Godly traits.");h.succeed();
        });
    }
}
