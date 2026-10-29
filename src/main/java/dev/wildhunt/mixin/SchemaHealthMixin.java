package dev.wildhunt.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildhunt.SchemaArt;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.storage.effect.EffectStorage;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
/** One HP writer covers heal(), food/potions and direct skill recovery without boosting twice. */
@Mixin(LivingEntity.class)
public abstract class SchemaHealthMixin {
    @WrapMethod(method="setHealth")
    private void schema$healing(float requested,Operation<Void> original){
        var owner=(LivingEntity)(Object)this;
        // Constructors and dead entities must not query skill storage before attachment.
        if(owner.level().isClientSide() || !owner.isAlive() || !SchemaArt.healingActive(owner) || requested<=owner.getHealth()){original.call(requested);return;}
        double factor=SchemaArt.healingFactor(owner),before=owner.getHealth();
        if(factor>1 && !SkillUtils.shouldCancelHealing(owner))requested=(float)SchemaArt.heal(before,requested,EffectStorage.getSeveranceMaxHealth(owner),factor);
        original.call(requested);if(factor>1)SchemaArt.trainHealing(owner,owner.getHealth()-before);
    }
}
