package dev.wildhunt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.*;

/** Additive reward: preserves vanilla and other addons' loot tables. */
public final class OminousSpearLoot extends LootModifier {
    public static final MapCodec<OminousSpearLoot> CODEC=RecordCodecBuilder.mapCodec(instance->codecStart(instance).apply(instance,OminousSpearLoot::new));
    public OminousSpearLoot(LootItemCondition[] conditions){super(conditions);}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generated,LootContext context){generated.add(InvertedSpearItem.create(context.getLevel().registryAccess()));return generated;}
    @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
