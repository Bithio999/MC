package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.effect.template.*;
import io.github.manasmods.tensura.entity.magic.barrier.*;
import io.github.manasmods.tensura.entity.magic.field.haki.*;
import io.github.manasmods.tensura.registry.entity.MiscEntityTypes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.item.TensuraToolItems;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.heavenlyrestriction.TestFixtures.*;
import static dev.heavenlyrestriction.ChainedGameTests.near;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class HeavenlyArsenalGameTests {
    private static ManasSkillInstance learn(LivingEntity e,ManasSkill skill){RestrictionData.learnReward(e,skill);return SkillAPI.getSkillsFrom(e).getSkill(skill).orElseThrow();}
    private static void enchant(LivingEntity owner,ItemStack stack,String id,int level){stack.enchant(owner.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.parse(id))),level);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void bothBodiesBlockOnlyTheRequestedEffectsWhileEquipped(GameTestHelper h){
        for(var skill:List.of(HeavenlyRestriction.CHAINED.get(),HeavenlyRestriction.UNBOUND.get())){
            var p=player(h);var i=learn(p,skill);i.onToggleOff(p);var slots=TensuraStorages.getAbilityFrom(p);slots.setAbilitySlot(0,0,skill,0);
            for(var effect:List.of(MobEffects.MOVEMENT_SLOWDOWN,TensuraMobEffects.getReference(TensuraMobEffects.FEAR),TensuraMobEffects.getReference(TensuraMobEffects.BURDEN)))h.assertTrue(!p.addEffect(new MobEffectInstance(effect,100,3)),"In-slot body accepted "+effect);
            h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.POISON,100)),"Body gained unrequested poison immunity");
            slots.setAbilitySlot(0,0,null,0);h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100)),"Body immunity remained unequipped");
            slots.setAbilitySlot(0,0,skill,0);UnerringBody.tick(p);h.assertTrue(!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && p.hasEffect(MobEffects.POISON),"Equip failed to cleanse an existing slow or removed poison");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void bodyRejectsNormalAndSacredHakiAndDirectHakiPush(GameTestHelper h){
        var p=player(h);var attacker=target(h,p.position().add(4,0,0));var i=learn(p,HeavenlyRestriction.CHAINED.get());TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,i.getSkill(),0);
        var haki=ExtraSkills.HAKI.get();var instance=haki.createDefaultInstance();
        for(var field:List.of(new HakiField(h.getLevel(),attacker),new SacredHakiField(MiscEntityTypes.HAKI_FIELD.get(),h.getLevel()))){
            field.setOwner(attacker);field.setSkill(attacker,instance,haki,0);field.setEp(1e15);field.setEpDifferenceMultiplier(1);field.setFearDuration(100);
            var motion=p.getDeltaMovement();field.applyEffect(p,true);h.assertTrue(!p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.FEAR)) && p.getDeltaMovement().equals(motion),"Haki affected an equipped body");
        }
        var motion=p.getDeltaMovement();io.github.manasmods.tensura.ability.skill.extra.HakiSkill.hakiPush(p,attacker,instance,100);h.assertTrue(p.getDeltaMovement().equals(motion),"Direct Haki push bypassed body immunity");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void weaponStatsMatchHihiirokaneAndBuiltinEngravingIsNative(GameTestHelper h){
        var p=player(h);var spear=InvertedSpearItem.create(p.registryAccess());var sword=new ItemStack(TensuraToolItems.HIHIIROKANE_SWORD.get());
        h.assertTrue(spear.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(sword.get(DataComponents.ATTRIBUTE_MODIFIERS)),"Spear attack damage/speed differ from Hihiirokane Sword");
        h.assertTrue(spear.getMaxDamage()==sword.getMaxDamage(),"Spear durability differs from its material");
        var holder=p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Nullification.ENGRAVING);
        h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(holder,spear)==1 && holder.is(io.github.manasmods.tensura.data.TensuraTags.Enchantments.ENGRAVING),"Nullification is missing or not classified as a native engraving");
        var given=new ItemStack(HeavenlyArsenal.SPEAR.get());given.inventoryTick(h.getLevel(),p,0,true);h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(holder,given)==1,"A plain give/creative stack missed its built-in engraving");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void weaponDamageIncludesSharpnessCustomModifiersAndCrushingEngraving(GameTestHelper h){
        var p=player(h);var victim=target(h,p.position().add(4,0,0));var source=p.damageSources().playerAttack(p);var sword=new ItemStack(Items.DIAMOND_SWORD);p.setItemSlot(EquipmentSlot.MAINHAND,sword);
        near(h,ChainedWeaponArts.weaponDamage(p,victim,source),7,"Diamond sword contribution");enchant(p,sword,"minecraft:sharpness",5);near(h,ChainedWeaponArts.weaponDamage(p,victim,source),10,"Sharpness applied incorrectly");
        enchant(p,sword,"tensura:crushing",1);h.assertTrue(ChainedWeaponArts.weaponDamage(p,victim,source)>10,"Native weapon-multiplier engraving was ignored");
        var custom=new ItemStack(Items.DIAMOND_SWORD);custom.set(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(HeavenlyRestriction.id("test_add"),6,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_DAMAGE,new AttributeModifier(HeavenlyRestriction.id("test_scale"),.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),EquipmentSlotGroup.MAINHAND).build());p.setItemSlot(EquipmentSlot.MAINHAND,custom);
        near(h,ChainedWeaponArts.weaponDamage(p,victim,source),10.5,"Custom attribute operation ordering");p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);near(h,ChainedWeaponArts.weaponDamage(p,victim,source),0,"Unarmed ability gained an unwanted weapon term");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualDashAppliesWeaponDamageAndFireAspectToBothHits(GameTestHelper h){
        var anchor=player(h);var owner=target(h,anchor.position().add(0,30,0));var victim=target(h,owner.position().add(8,0,0));var i=learn(owner,HeavenlyRestriction.CHAINED.get());i.onToggleOff(owner);EnergyHelper.setBaseMaxEP(owner,100000);
        var sword=new ItemStack(Items.DIAMOND_SWORD);enchant(owner,sword,"minecraft:sharpness",5);enchant(owner,sword,"minecraft:fire_aspect",1);owner.setItemSlot(EquipmentSlot.MAINHAND,sword);
        afterTracking(h,List.of(owner,victim),()->{
            owner.setYRot(-90);owner.setXRot(0);owner.setOnGround(false);TensuraStorages.getExistenceFrom(owner).setAura(10000);var seen=new ArrayList<Float>();
            java.util.function.Consumer<LivingIncomingDamageEvent> listener=e->{if(e.getEntity()==victim)seen.add(e.getAmount());};NeoForge.EVENT_BUS.addListener(listener);
            try{HeavenlyRestriction.CHAINED.get().onPressed(i,owner,0,0);}finally{NeoForge.EVENT_BUS.unregister(listener);}
            h.assertTrue(seen.size()==2,"Dash did not deliver separate path/arrival hits");near(h,seen.get(0),ChainedSkill.pathDamage(200)+10,"Dash path weapon damage");near(h,seen.get(1),ChainedSkill.arrivalDamage(200)+10,"Dash arrival weapon damage");h.assertTrue(victim.isOnFire(),"Dash omitted Fire Aspect post-hit callback");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualCounterUsesReleaseWeaponAndHitsTheAdjacentTarget(GameTestHelper h){
        var p=player(h);var i=learn(p,HeavenlyRestriction.UNBOUND.get());i.onToggleOff(p);i.setMastery(0);var attacker=target(h,p.position().add(8,0,0));var adjacent=target(h,p.position().add(9,0,1));TensuraStorages.getExistenceFrom(p).setAura(10000);
        afterTracking(h,List.of(attacker,adjacent),()->{
            UnboundCounter.press(i,p);var sword=new ItemStack(Items.DIAMOND_SWORD);enchant(p,sword,"minecraft:sharpness",5);enchant(p,sword,"minecraft:fire_aspect",1);p.setItemSlot(EquipmentSlot.MAINHAND,sword);var seen=new ArrayList<Float>();
            java.util.function.Consumer<LivingIncomingDamageEvent> listener=e->{if(e.getEntity()==attacker || e.getEntity()==adjacent)seen.add(e.getAmount());};NeoForge.EVENT_BUS.addListener(listener);
            try{h.assertTrue(UnboundCounter.intercept(p,p.damageSources().mobAttack(attacker),20),"Counter failed to intercept enemy");}finally{NeoForge.EVENT_BUS.unregister(listener);}
            h.assertTrue(seen.size()==2,"Weapon counter missed an adjacent target");for(float amount:seen)near(h,amount,UnboundCounter.damage(20,1000)+10,"Counter weapon contribution");h.assertTrue(attacker.isOnFire() && adjacent.isOnFire(),"Counter omitted enchantment callbacks");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeMeleeShattersBarriersPurgesSkillEffectsAndAppliesSeal(GameTestHelper h){
        var p=player(h);p.setItemSlot(EquipmentSlot.MAINHAND,InvertedSpearItem.create(p.registryAccess()));var victim=target(h,p.position().add(3,0,0));
        victim.getAttribute(TensuraAttributes.MULTILAYER_BARRIER).setBaseValue(100000);victim.getAttribute(TensuraAttributes.PHYSICAL_BARRIER).setBaseValue(100);victim.getAttribute(TensuraAttributes.MAGIC_BARRIER).setBaseValue(100);
        victim.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.AURA_SWORD),200));
        var attributed=new MobEffectInstance(MobEffects.DIG_SPEED,200);((TensuraMobEffectInstance)attributed).tensura$setSourceAbility(ExtraSkills.HAKI.get(),0);victim.addEffect(attributed);victim.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,200));
        afterTracking(h,List.of(victim),()->{
            p.attack(victim);
            for(var attr:List.of(TensuraAttributes.MULTILAYER_BARRIER,TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER))near(h,victim.getAttributeValue(attr),0,"Nullification left a native barrier");
            h.assertTrue(victim.getHealth()<1024 && !victim.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.AURA_SWORD)) && !victim.hasEffect(MobEffects.DIG_SPEED),"Spear did not pierce the current barrier or clear skill effects");
            h.assertTrue(victim.hasEffect(MobEffects.NIGHT_VISION) && Nullification.sealed(victim) && victim.getEffect(HeavenlyArsenal.SKILL_SEAL).getDuration()==40,"Spear removed ordinary potion effects or failed the two-second seal");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void bladeBreaksAllNativeBarrierEntityVariants(GameTestHelper h){
        var p=player(h);p.setItemSlot(EquipmentSlot.MAINHAND,InvertedSpearItem.create(p.registryAccess()));
        for(var barrier:List.of(new RangedBarrierEntity(MiscEntityTypes.RANGED_BARRIER.get(),h.getLevel()),new BossBarrierEntity(h.getLevel()))){barrier.setHealth(1000000);barrier.setPos(p.position().add(3,0,0));h.getLevel().addFreshEntity(barrier);h.assertTrue(barrier.hurt(p.damageSources().playerAttack(p),1) && barrier.isRemoved(),"Weapon failed to shatter "+barrier.getClass().getSimpleName());}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void heldSpearDoesNotNullifyUnrelatedSpells(GameTestHelper h){
        var p=player(h);p.setItemSlot(EquipmentSlot.MAINHAND,InvertedSpearItem.create(p.registryAccess()));var victim=target(h,p.position().add(3,0,0));victim.getAttribute(TensuraAttributes.MULTILAYER_BARRIER).setBaseValue(1000);
        var other=ExtraSkills.HAKI.get().createDefaultInstance();var source=ExtraSkills.HAKI.get().createSource(other,p,net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK,0);
        Nullification.beforeDamage(SkillAPI.getSkillsFrom(victim),victim,source,io.github.manasmods.manascore.network.api.util.Changeable.of(10f));
        near(h,victim.getAttributeValue(TensuraAttributes.MULTILAYER_BARRIER),1000,"Unrelated ability borrowed weapon engraving");h.assertTrue(!Nullification.sealed(victim),"Holding a spear sealed an unrelated spell target");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeActivationIsBlockedForFortyTicksWithoutSpendingResources(GameTestHelper h){
        var p=player(h);var owner=target(h,p.position().add(0,25,0));var i=learn(owner,UniqueSkills.FIGHTER.get());
        // A freshly force-loaded arena may start ticking entities after the test clock starts.
        h.startSequence().thenWaitUntil(()->h.assertTrue(owner.tickCount>0,"Waiting for entity simulation"))
            .thenExecute(()->{
                owner.addEffect(new MobEffectInstance(HeavenlyArsenal.SKILL_SEAL,40));var skills=SkillAPI.getSkillsFrom(owner);double ap=TensuraStorages.getExistenceFrom(owner).getAura(),mp=TensuraStorages.getExistenceFrom(owner).getMagicule();
                h.assertTrue(!i.getSkill().canActivateSkill(i,owner,0) && !skills.startHoldSkill(i,0,0),"Sealed native skill activated");near(h,TensuraStorages.getExistenceFrom(owner).getAura(),ap,"Blocked activation spent aura");near(h,TensuraStorages.getExistenceFrom(owner).getMagicule(),mp,"Blocked activation spent MP");
                h.runAfterDelay(41,()->{h.assertTrue(!Nullification.sealed(owner) && i.getSkill().canActivateSkill(i,owner,0),"Seal did not expire after two seconds; entity ticks="+owner.tickCount);h.succeed();});
            });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualOminousVaultLootRollsRareEngravedSpearsAndNormalVaultDoesNot(GameTestHelper h){
        var level=h.getLevel();var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO)).create(LootContextParamSets.CHEST);
        var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous")));int count=0;
        for(int n=1;n<=1024;n++)for(var stack:table.getRandomItems(params,net.minecraft.util.RandomSource.create(n))){if(stack.is(HeavenlyArsenal.SPEAR.get())){count++;h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Nullification.ENGRAVING),stack)==1,"Vault spear lacked its engraving");}}
        h.assertTrue(count>=5 && count<=40,"Ominous vault rarity differs from 2% or modifier was not loaded: "+count);
        var normal=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward")));
        for(int n=1;n<=128;n++){var items=normal.getRandomItems(params,net.minecraft.util.RandomSource.create(n));h.assertTrue(items.stream().noneMatch(s->s.is(HeavenlyArsenal.SPEAR.get())) && !items.isEmpty(),"Spear leaked into normal vaults or replaced their rewards");}h.succeed();
    }
}
