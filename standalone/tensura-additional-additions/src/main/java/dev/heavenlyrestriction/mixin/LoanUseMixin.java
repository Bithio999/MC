package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.loans.LoanShark;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ManasSkillInstance.class)
public abstract class LoanUseMixin {
    @WrapMethod(method="onPressed",remap=false)
    private void loan$press(LivingEntity owner,int slot,int mode,Operation<Void> original){original.call(owner,slot,mode);LoanShark.pulse(owner,(ManasSkillInstance)(Object)this,20);}
    @WrapMethod(method="onHeld",remap=false)
    private boolean loan$held(LivingEntity owner,int ticks,int mode,Operation<Boolean> original){boolean result=original.call(owner,ticks,mode);LoanShark.pulse(owner,(ManasSkillInstance)(Object)this,20);return result;}
    @WrapMethod(method="onRelease",remap=false)
    private void loan$release(LivingEntity owner,int ticks,int slot,int mode,Operation<Void> original){original.call(owner,ticks,slot,mode);LoanShark.pulse(owner,(ManasSkillInstance)(Object)this,20);}
}
