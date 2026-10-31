package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.enchantment.TensuraEnchantmentHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Native item modifiers, vanilla enchantment damage and both Tensura engraving callbacks. */
public final class ChainedWeaponArts {
    private ChainedWeaponArts(){}
    public static float weaponDamage(LivingEntity owner,LivingEntity target,DamageSource source){
        var weapon=owner.getMainHandItem();if(weapon.isEmpty() || !(owner.level() instanceof ServerLevel level))return 0;
        // ItemStack.forEachModifier includes native WEAPON_MULTIPLIER engravings and
        // every custom item modifier. Preserve vanilla attribute operation ordering.
        double[] modifiers={0,0,1};
        weapon.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{
            if(!attribute.equals(Attributes.ATTACK_DAMAGE))return;
            switch(modifier.operation()){
                case ADD_VALUE -> modifiers[0]+=modifier.amount();
                case ADD_MULTIPLIED_BASE -> modifiers[1]+=modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> modifiers[2]*=1+modifier.amount();
            }
        });
        float raw=(float)Math.max(0,(1+modifiers[0])*(1+modifiers[1])*modifiers[2]);
        return Math.max(0,EnchantmentHelper.modifyDamage(level,weapon,target,source,raw));
    }
    public static boolean hit(ManasSkillInstance instance,LivingEntity owner,LivingEntity target,int mode,float base,double spent){
        if(!(owner.level() instanceof ServerLevel level))return false;
        var source=((ChainedSkill)instance.getSkill()).createSource(instance,owner,DamageTypes.MOB_ATTACK,mode);
        ((TensuraDamageSource)source).tensura$setAuraCost(spent);((IndependentHitSource)source).restriction$independentHit();
        var weapon=owner.getMainHandItem();float damage=base+weaponDamage(owner,target,source);
        boolean hit=target.hurt(source,damage);
        if(hit && !weapon.isEmpty()){
            TensuraEnchantmentHelper.doAdditionalAfterAttack(level,target,owner,source,weapon,damage);
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level,target,source,weapon);
            TensuraEnchantmentHelper.doAdditionalAfterDamage(level,target,owner,source,weapon,damage);
            if(owner instanceof Player player && weapon.hurtEnemy(target,player))weapon.postHurtEnemy(target,player);
        }
        return hit;
    }
}
