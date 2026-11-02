package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.exponentiation.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Player.class)
public abstract class ExponentiationPlayerMixin {
    @WrapMethod(method="attack")
    private void exponentiation$attack(Entity target,Operation<Void> original){try(var scope=ExponentiationContext.raw((Player)(Object)this,ExponentiationContext.DAMAGE)){original.call(target);}}
    @WrapMethod(method="getDigSpeed")
    private float exponentiation$mining(BlockState state,BlockPos pos,Operation<Float> original){var owner=(Player)(Object)this;float speed;try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.MINING)){speed=original.call(state,pos);}return Exponentiation.active(owner)?Exponentiation.squareFloat(speed):speed;}
}
