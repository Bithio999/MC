package dev.wildhunt;

import dev.architectury.registry.registries.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import java.util.List;

public final class MythBosses {
    private MythBosses(){}
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(WildHunt.ID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(WildHunt.ID,Registries.ITEM);
    public static final RegistrySupplier<EclipseSeraphRace> RACE=RaceAPI.getRaceRegistry().register(WildHunt.id("eclipse_seraph"),EclipseSeraphRace::new);
    public static final RegistrySupplier<MoiraSkill> MOIRA=SkillAPI.getSkillRegistry().register(WildHunt.id("moira"),MoiraSkill::new);
    public static final RegistrySupplier<EntityType<AnankeEntity>> ANANKE=TYPES.register("ananke",()->EntityType.Builder.of(AnankeEntity::new,MobCategory.MONSTER)
        .sized(1.6f,4.1f).eyeHeight(3.5f).clientTrackingRange(12).updateInterval(1).fireImmune().build("wildhunt:ananke"));
    public static final RegistrySupplier<Item> EGG=ITEMS.register("ananke_spawn_egg",()->new DeferredSpawnEggItem(ANANKE,0x171125,0xF2C36D,new Item.Properties()));
    public static final RegistrySupplier<Item> WRIT=ITEMS.register("writ_of_last_dawn",()->new AnankeItems.Writ(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> TESTAMENT=ITEMS.register("dawn_testament",()->new AnankeItems.Testament(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final List<String> NATIVE_SKILLS=List.of("tensura:mathematician","tensura:observer","tensura:universal_perception","tensura:thought_acceleration",
        "tensura:chant_annulment","tensura:spatial_domination","tensura:self_regeneration","tensura:physical_attack_resistance",
        "tensura:spiritual_attack_resistance","tensura:magic_resistance","tensura:abnormal_condition_nullification","tensura:pain_nullification","tensura:holy_attack_resistance",
        "tensura:light_attack_resistance","tensura:darkness_attack_resistance","tensura:spatial_attack_resistance");
    public static void register(IEventBus bus){TYPES.register();ITEMS.register();bus.addListener(MythBosses::attributes);bus.addListener(MythBosses::tabs);}
    private static void attributes(EntityAttributeCreationEvent event){event.put(ANANKE.get(),AnankeEntity.attributes().build());}
    private static void tabs(BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS)event.accept(EGG.get());
        if(event.getTabKey()==CreativeModeTabs.COMBAT){event.accept(WRIT.get());event.accept(TESTAMENT.get());}
    }
}
