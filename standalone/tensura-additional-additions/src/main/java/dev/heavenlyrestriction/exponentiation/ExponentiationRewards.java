package dev.heavenlyrestriction.exponentiation;
import dev.heavenlyrestriction.RestrictionData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
/** Final item quantities respect native stack sizes; XP changes create no extra orb entities. */
public final class ExponentiationRewards {
    private static void stack(ItemStack stack){if(stack.getCount()>1)stack.setCount((int)Math.min(stack.getMaxStackSize(),Exponentiation.square(stack.getCount())));}
    public static void block(BlockDropsEvent event){if(event.getBreaker() instanceof net.minecraft.world.entity.LivingEntity owner && Exponentiation.active(owner))for(var drop:event.getDrops())stack(drop.getItem());}
    public static void mob(LivingDropsEvent event){if(Exponentiation.active(RestrictionData.attacker(event.getSource())))for(var drop:event.getDrops())stack(drop.getItem());}
    public static void xp(PlayerXpEvent.XpChange event){if(event.getAmount()>0 && Exponentiation.active(event.getEntity()))event.setAmount((int)Math.min(Integer.MAX_VALUE,Exponentiation.square(event.getAmount())));}
    private ExponentiationRewards(){}
}
