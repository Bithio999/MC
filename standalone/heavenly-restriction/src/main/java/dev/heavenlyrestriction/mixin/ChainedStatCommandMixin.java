package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.RestrictedRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.command.edit.StatCommand;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native positive max-EP command awards use the lineage split before capacity modifiers. */
@Mixin(StatCommand.class)
public abstract class ChainedStatCommandMixin {
    @WrapOperation(method={"addMaxEp(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/commands/arguments/selector/EntitySelector;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;D)Z","addMaxEp(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/commands/arguments/selector/EntitySelector;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DZ)Z"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;setBaseValue(D)V"),remap=false)
    private void restriction$splitGain(AttributeInstance attribute,double proposed,Operation<Void> original,@Local LivingEntity owner){
        double delta=proposed-attribute.getBaseValue();
        if(delta>0 && RaceAPI.getRaceFrom(owner).getRace().filter(i->i.getRace() instanceof RestrictedRace).isPresent()){
            var gains=dev.heavenlyrestriction.RestrictionData.growth(owner,.5,.5);
            if(attribute.getAttribute().equals(TensuraAttributes.MAX_AURA))proposed=attribute.getBaseValue()+delta*2*gains.aura();
            else if(attribute.getAttribute().equals(TensuraAttributes.MAX_MAGICULE))proposed=attribute.getBaseValue()+delta*2*gains.magicules();
        }
        original.call(attribute,proposed);
    }
}
