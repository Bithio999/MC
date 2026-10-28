package dev.wildhunt;

import io.github.manasmods.tensura.item.TensuraToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class InvertedSpearItem extends SwordItem {
    public InvertedSpearItem(){super(TensuraToolTiers.HIHIIROKANE,new Item.Properties().fireResistant().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(TensuraToolTiers.HIHIIROKANE,3,-2.4f)));}
    public static void engrave(ItemStack stack,RegistryAccess access){
        var enchantment=access.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Nullification.ENGRAVING);
        if(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment,stack)<1)stack.enchant(enchantment,1);
    }
    public static ItemStack create(RegistryAccess access){var stack=new ItemStack(HeavenlyArsenal.SPEAR.get());engrave(stack,access);return stack;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity owner,int slot,boolean selected){super.inventoryTick(stack,level,owner,slot,selected);if(!level.isClientSide())engrave(stack,level.registryAccess());}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        for(int part=1;part<=4;part++)lines.add(Component.translatable("item."+WildHunt.ID+".inverted_spear_of_heaven.desc."+part).withStyle(ChatFormatting.GRAY));
    }
}
