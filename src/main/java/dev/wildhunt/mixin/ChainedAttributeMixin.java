package dev.wildhunt.mixin;

import dev.wildhunt.*;
import io.github.manasmods.tensura.registry.attribute.*;
import net.minecraft.world.entity.ai.attributes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AttributeInstance.class)
public abstract class ChainedAttributeMixin {
    @ModifyVariable(method="setBaseValue",at=@At("HEAD"),argsOnly=true)
    private double myths$blockMagiculeGrowth(double proposed){
        var a=(AttributeInstance)(Object)this;var e=((TensuraAttributeInstance)this).tensura$getOwner();
        return e!=null && a.getAttribute().equals(TensuraAttributes.MAX_MAGICULE) && proposed>a.getBaseValue() && ChainedState.owned(e) && !ChainedContext.initializing(e)?a.getBaseValue():proposed;
    }
    @Inject(method="setBaseValue",at=@At("RETURN"))
    private void myths$updateStartingExemption(CallbackInfo out){
        var a=(AttributeInstance)(Object)this;var e=((TensuraAttributeInstance)this).tensura$getOwner();
        if(e!=null && a.getAttribute().equals(TensuraAttributes.MAX_AURA) && ChainedState.owned(e))ChainedState.refresh(e);
    }
    @Inject(method="getValue",at=@At("RETURN"),cancellable=true)
    private void myths$capAllMagiculeSources(CallbackInfoReturnable<Double> out){
        var a=(AttributeInstance)(Object)this;if(a.getAttribute().equals(TensuraAttributes.MAX_MAGICULE))out.setReturnValue(ChainedState.magiculeCeiling(((TensuraAttributeInstance)this).tensura$getOwner(),out.getReturnValue()));
    }
    @ModifyVariable(method={"addPermanentModifier","addOrReplacePermanentModifier"},at=@At("HEAD"),argsOnly=true)
    private AttributeModifier myths$permanentBuff(AttributeModifier value){return ChainedBuffs.modifier(((TensuraAttributeInstance)this).tensura$getOwner(),(AttributeInstance)(Object)this,value,true);}
    @ModifyVariable(method={"addTransientModifier","addOrUpdateTransientModifier"},at=@At("HEAD"),argsOnly=true)
    private AttributeModifier myths$transientBuff(AttributeModifier value){return ChainedBuffs.modifier(((TensuraAttributeInstance)this).tensura$getOwner(),(AttributeInstance)(Object)this,value,false);}
}
