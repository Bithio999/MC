package dev.wildhunt;

import com.mojang.serialization.MapCodec;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.*;

public final class HeavenlyArsenal {
    private HeavenlyArsenal(){}
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,WildHunt.ID);
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,WildHunt.ID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT=DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,WildHunt.ID);
    public static final DeferredHolder<Item,InvertedSpearItem> SPEAR=ITEMS.register("inverted_spear_of_heaven",InvertedSpearItem::new);
    public static final DeferredHolder<MobEffect,MobEffect> SKILL_SEAL=EFFECTS.register("nullification",()->new MobEffect(MobEffectCategory.HARMFUL,0xbac8d8){});
    static {LOOT.register("ominous_heavenly_spear",()->OminousSpearLoot.CODEC);}
    public static void register(IEventBus bus){
        ITEMS.register(bus);EFFECTS.register(bus);LOOT.register(bus);
        bus.addListener(HeavenlyArsenal::creative);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,UnerringBody::applicable);
        SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.register(Nullification::beforeDamage);
        SkillEvents.ACTIVATE_SKILL.register(Nullification::activate);
    }
    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event){if(event.getTabKey().equals(CreativeModeTabs.COMBAT))event.accept(SPEAR.get());}
}
