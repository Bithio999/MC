package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.ProspectorMining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import java.util.List;

@Mixin(Block.class)
public abstract class ProspectorLootMixin {
    @WrapMethod(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> prospector$fortune(BlockState state,ServerLevel level,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemStack tool,Operation<List<ItemStack>> original){
        return original.call(state,level,pos,blockEntity,breaker,breaker instanceof LivingEntity miner?ProspectorMining.lootTool(miner,tool):tool);
    }
}
