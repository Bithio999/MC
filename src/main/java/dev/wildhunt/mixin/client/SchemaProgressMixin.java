package dev.wildhunt.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.wildhunt.SchemaArt;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Use Schema's real learning budget in the native bar and show both point totals. */
@Mixin(AbilitySelectionScreen.class)
public abstract class SchemaProgressMixin {
    @Shadow(remap=false) public ManasSkillInstance selectedAbility;
    @Unique private boolean schema$selected(){return selectedAbility!=null && selectedAbility.getSkill() instanceof SchemaArt;}
    @ModifyConstant(method="renderAbility",constant=@Constant(intValue=100),remap=false)
    private int schema$learningTotal(int original){return schema$selected() && selectedAbility.getMastery()<0 ? 1000 : original;}
    @ModifyConstant(method="renderAbility",constant=@Constant(floatValue=100f),remap=false)
    private float schema$learningScale(float original){return schema$selected() && selectedAbility.getMastery()<0 ? 1000f : original;}
    @WrapOperation(method="renderAbility",at=@At(value="INVOKE",target="Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"),remap=false)
    private MutableComponent schema$pointTotals(String key,Object[] args,Operation<MutableComponent> original){
        if(schema$selected()){
            double value=selectedAbility.getMastery();
            if(key.equals("tensura.ability_selection.learning"))args=new Object[]{(int)Math.clamp(1000+value,0,1000)+" / 1,000"};
            else if(key.equals("tensura.ability_selection.mastery"))args=new Object[]{(int)Math.clamp(value,0,2000)+" / 2,000 ("+(int)Math.clamp(value*100/2000,0,100)+"%)"};
        }
        return original.call(key,args);
    }
}
