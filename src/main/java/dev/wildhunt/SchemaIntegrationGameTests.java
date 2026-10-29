package dev.wildhunt;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.*;
import static dev.wildhunt.MythicGameTests.*;
import static dev.wildhunt.ChainedGameTests.near;
@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class SchemaIntegrationGameTests {
    private static void regenerative(LivingEntity e){HunterData.learnReward(e,WildHunt.SCHEMA.get());var i=SkillAPI.getSkillsFrom(e).getSkill(WildHunt.SCHEMA.get()).orElseThrow();i.setMastery(2000);i.getOrCreateTag().putIntArray("SchemaTree",new int[]{3,0,0,0,0,0});i.onToggleOn(e);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void manaCrucibleTaxesAuraWithoutCreatingExtraMagicules(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.THOTH);var skill=(MythicSkill)i.getSkill();regenerative(p);var e=TensuraStorages.getExistenceFrom(p);e.setAura(2100);e.setMagicule(0);
        ManaCrucible.begin(i,p);i.getOrCreateTag().putLong("crucible_last_tick",p.level().getGameTime()-1);ManaCrucible.held(skill,i,p);near(h,e.getAura(),0,"Crucible did not use its 2.1 aura price");near(h,e.getMagicule(),1000,"Crucible surcharge changed output or overdrew");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void grovePaysSurchargeOnBaseHealingAndRefundsCanceledRecovery(GameTestHelper h){
        var p=player(h);var target=player(h);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(40);target.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(100);var te=TensuraStorages.getExistenceFrom(target);te.setSpiritualHealth(50);regenerative(p);regenerative(target);var i=learn(p,MythicKind.VERDANT_KEEPER);i.setMastery(0);var e=TensuraStorages.getExistenceFrom(p);e.setAura(100000);
        double hp=Math.min(target.getMaxHealth()*.07,target.getMaxHealth()-40),shp=Math.min(target.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.06,target.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)-50);double cost=(hp*40+shp*60)*1.05;
        GroveHealing.pulse(i,p,java.util.List.of(target));h.assertTrue(Math.abs(target.getHealth()-(40+hp*1.2))<.001,"Grove HP boost");near(h,te.getSpiritualHealth(),50+shp*1.2,"Grove SHP boost");near(h,e.getAura(),100000-cost,"Grove double charged or incorrectly refunded the surcharge");
        target.setHealth(40);te.setSpiritualHealth(target.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));double before=e.getAura();java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingHealEvent> cancel=event->{if(event.getEntity()==target)event.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{GroveHealing.pulse(i,p,java.util.List.of(target));near(h,e.getAura(),before,"Canceled Grove did not refund its full adjusted reservation");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}h.succeed();
    }
}
