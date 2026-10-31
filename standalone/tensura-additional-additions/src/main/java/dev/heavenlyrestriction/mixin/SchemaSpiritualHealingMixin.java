package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.SchemaArt;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
/** Native SHP regeneration and direct restores share this setter; decreases are untouched. */
@Mixin(ExistenceStorage.class)
public abstract class SchemaSpiritualHealingMixin {
    @Shadow(remap=false) protected abstract LivingEntity getOwner();
    @Shadow(remap=false) public abstract double getSpiritualHealth();
    @WrapMethod(method="setSpiritualHealth",remap=false)
    private void schema$healing(double requested,Operation<Void> original){
        var owner=getOwner();double before=getSpiritualHealth();
        if(owner==null || owner.level().isClientSide() || !owner.isAlive() || !SchemaArt.healingActive(owner) || requested<=before){original.call(requested);return;}
        double factor=SchemaArt.healingFactor(owner);
        if(factor>1 && !SkillUtils.shouldCancelHealing(owner))requested=SchemaArt.heal(before,requested,owner.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),factor);
        original.call(requested);if(factor>1)SchemaArt.trainHealing(owner,getSpiritualHealth()-before);
    }
}
