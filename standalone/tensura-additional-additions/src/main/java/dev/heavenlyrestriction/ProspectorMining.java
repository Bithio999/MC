package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import java.util.Optional;

public final class ProspectorMining {
    public static final ResourceKey<LootTable> FINDS=ResourceKey.create(Registries.LOOT_TABLE,HeavenlyRestriction.id("gameplay/prospector_finds"));
    public static final ResourceKey<LootTable> MASTERED_FINDS=ResourceKey.create(Registries.LOOT_TABLE,HeavenlyRestriction.id("gameplay/prospector_finds_mastered"));
    public static ResourceKey<LootTable> findsTable(LivingEntity owner){return active(owner).filter(i->i.isMastered(owner)).isPresent()?MASTERED_FINDS:FINDS;}
    public static Optional<ManasSkillInstance> active(LivingEntity owner){
        if(owner==null || !owner.isAlive() || owner.isSpectator())return Optional.empty();
        return SkillAPI.getSkillsFrom(owner).getSkill(HeavenlyRestriction.PROSPECTOR.get()).filter(i->i.getMastery()>=0 && i.isToggled() && i.canInteractSkill(owner));
    }
    public static void speed(PlayerEvent.BreakSpeed e){if(active(e.getEntity()).isPresent())e.setNewSpeed(e.getNewSpeed()*1.5f);}
    /** A loot-only copy preserves the actual item's enchants, durability and components. No reroll/double loot evaluation. */
    public static ItemStack lootTool(LivingEntity miner,ItemStack tool){
        if(tool.isEmpty() || active(miner).isEmpty())return tool;
        var fortune=miner.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE);
        var copy=tool.copy();copy.enchant(fortune,Math.min(255,EnchantmentHelper.getItemEnchantmentLevel(fortune,tool)+3));return copy;
    }
    public static void finds(BlockDropsEvent e){
        if(!(e.getBreaker() instanceof ServerPlayer p) || p.isCreative() || active(p).isEmpty() || !(e.getState().is(Blocks.STONE) || e.getState().is(Blocks.DEEPSLATE)) || !e.getTool().isCorrectToolForDrops(e.getState()))return;
        var params=new LootParams.Builder(e.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(e.getPos())).withParameter(LootContextParams.BLOCK_STATE,e.getState()).withParameter(LootContextParams.TOOL,e.getTool()).withParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.BLOCK);
        for(var stack:e.getLevel().getServer().reloadableRegistries().getLootTable(findsTable(p)).getRandomItems(params))e.getDrops().add(new ItemEntity(e.getLevel(),e.getPos().getX()+.5,e.getPos().getY()+.5,e.getPos().getZ()+.5,stack));
    }
    private ProspectorMining(){}
}
