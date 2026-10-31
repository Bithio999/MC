package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.entity.monster.WarGnomeEntity;
import io.github.manasmods.tensura.race.RaceUtils;
import io.github.manasmods.tensura.registry.entity.MonsterEntityTypes;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import static dev.heavenlyrestriction.TestFixtures.*;
import static dev.heavenlyrestriction.ChainedGameTests.near;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class NullificationSpiritualGameTests {
    private static ServerPlayer human(GameTestHelper h){var p=player(h);RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);return p;}
    private static WarGnomeEntity gnome(GameTestHelper h,ServerPlayer p,double offset){
        var target=new WarGnomeEntity(MonsterEntityTypes.WAR_GNOME.get(),h.getLevel());target.setPos(p.position().add(offset,0,0));target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);h.getLevel().addFreshEntity(target);return target;
    }
    /** Select an unblocked native hit; shield RNG is independent of spiritual reduction. */
    private static float unblockedHit(GameTestHelper h,ServerPlayer p,WarGnomeEntity target){
        target.getRandom().setSeed(1);
        for(int attempt=0;attempt<32;attempt++){
            target.invulnerableTime=0;float before=target.getHealth();
            if(target.hurt(p.damageSources().playerAttack(p),100))return before-target.getHealth();
        }
        h.fail("War Gnome blocked every test hit");return 0;
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spiritualContactBelongsOnlyToTheAttackingMainHandWeapon(GameTestHelper h){
        var p=human(h);var source=p.damageSources().playerAttack(p);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));near(h,RaceUtils.getPhysicalAttackInputMultiplier(source),.01,"Ordinary sword gained spiritual contact");
        p.setItemSlot(EquipmentSlot.OFFHAND,InvertedSpearItem.create(p.registryAccess()));near(h,RaceUtils.getPhysicalAttackInputMultiplier(source),.01,"Offhand spear lent its engraving");
        p.setItemSlot(EquipmentSlot.MAINHAND,InvertedSpearItem.create(p.registryAccess()));near(h,RaceUtils.getPhysicalAttackInputMultiplier(source),1,"Nullification left spiritual body reduction");
        h.assertTrue(TensuraDamageHelper.isPhysicalAttack(source),"Engraving changed the damage classification");
        var unrelated=p.damageSources().playerAttack(p);((TensuraDamageSource)unrelated).tensura$setAbilityInstance(UniqueSkills.FIGHTER.get().createDefaultInstance());
        near(h,RaceUtils.getPhysicalAttackInputMultiplier(unrelated),.01,"Unrelated physical ability borrowed the engraving");
        var arrow=new Arrow(EntityType.ARROW,h.getLevel());near(h,RaceUtils.getPhysicalAttackInputMultiplier(p.damageSources().arrow(arrow,p)),.01,"Unrelated projectile borrowed the engraving");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void warGnomeReceivesFullPhysicalWeaponDamageWhileKeepingItsArmor(GameTestHelper h){
        var p=human(h);var plain=gnome(h,p,3);var engraved=gnome(h,p,6);var armored=gnome(h,p,9);armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
        afterTracking(h,List.of(plain,engraved,armored),()->{
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));near(h,unblockedHit(h,p,plain),1,"War Gnome control did not apply native 99% physical reduction");
            p.setItemSlot(EquipmentSlot.MAINHAND,InvertedSpearItem.create(p.registryAccess()));near(h,unblockedHit(h,p,engraved),100,"Nullification failed to bypass War Gnome's spiritual body");
            float received=unblockedHit(h,p,armored);h.assertTrue(received>1 && received<100 && armored.getAttributeValue(Attributes.ARMOR)==20,"Spiritual contact ignored ordinary armor");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spearCanHitSpiritualFormWithoutChangingTheTargetsForm(GameTestHelper h){
        var p=human(h);var plain=target(h,p.position().add(3,0,0));var engraved=target(h,p.position().add(6,0,0));var spell=target(h,p.position().add(9,0,0));
        for(var target:List.of(plain,engraved,spell)){target.getAttribute(Attributes.ARMOR).setBaseValue(0);TensuraStorages.getExistenceFrom(target).setSpiritualForm(true);}
        afterTracking(h,List.of(plain,engraved,spell),()->{
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));h.assertTrue(!plain.hurt(p.damageSources().playerAttack(p),100),"Ordinary weapon gained contact with spiritual form");
            p.setItemSlot(EquipmentSlot.MAINHAND,InvertedSpearItem.create(p.registryAccess()));float before=engraved.getHealth();h.assertTrue(engraved.hurt(p.damageSources().playerAttack(p),100),"Spear could not attack spiritual form");near(h,before-engraved.getHealth(),100,"Spiritual-form spear damage");
            var unrelated=p.damageSources().playerAttack(p);((TensuraDamageSource)unrelated).tensura$setAbilityInstance(UniqueSkills.FIGHTER.get().createDefaultInstance());h.assertTrue(!spell.hurt(unrelated,100),"Unrelated ability borrowed spiritual-form contact");
            h.assertTrue(TensuraStorages.getExistenceFrom(engraved).isSpiritualForm(),"Spear permanently removed the target's spiritual form");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedAndUnboundWeaponArtsCarrySpiritualContact(GameTestHelper h){
        var p=human(h);p.setItemSlot(EquipmentSlot.MAINHAND,InvertedSpearItem.create(p.registryAccess()));var dash=target(h,p.position().add(3,0,0));var counter=target(h,p.position().add(6,0,0));
        for(var target:List.of(dash,counter)){target.getAttribute(Attributes.ARMOR).setBaseValue(0);TensuraStorages.getExistenceFrom(target).setSpiritualForm(true);}
        afterTracking(h,List.of(dash,counter),()->{
            float before=dash.getHealth();h.assertTrue(ChainedWeaponArts.hit(HeavenlyRestriction.CHAINED.get().createDefaultInstance(),p,dash,0,20,100),"Chained weapon art missed spiritual form");near(h,before-dash.getHealth(),100,"Chained physical base plus spear damage");
            before=counter.getHealth();h.assertTrue(ChainedWeaponArts.hit(HeavenlyRestriction.UNBOUND.get().createDefaultInstance(),p,counter,1,20,100),"Unbound weapon art missed spiritual form");near(h,before-counter.getHealth(),100,"Unbound physical base plus spear damage");h.succeed();
        });
    }
}
