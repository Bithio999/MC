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

public final class EschatonContent {
    private EschatonContent(){}
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(WildHunt.ID,Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(WildHunt.ID,Registries.ITEM);
    public static final RegistrySupplier<OriginSovereignRace> RACE=RaceAPI.getRaceRegistry().register(WildHunt.id("origin_sovereign"),OriginSovereignRace::new);
    public static final RegistrySupplier<DemiurgeSkill> DEMIURGE=SkillAPI.getSkillRegistry().register(WildHunt.id("demiurge"),DemiurgeSkill::new);
    public static final RegistrySupplier<EntityType<EschatonEntity>> ESCHATON=TYPES.register("eschaton",()->EntityType.Builder.of(EschatonEntity::new,MobCategory.MONSTER)
        .sized(2.6f,6.2f).eyeHeight(5.5f).clientTrackingRange(20).updateInterval(1).fireImmune().build("wildhunt:eschaton"));
    public static final RegistrySupplier<EntityType<GenesisSealEntity>> SEAL=TYPES.register("genesis_seal",()->EntityType.Builder.of(GenesisSealEntity::new,MobCategory.MISC)
        .sized(2.2f,3.6f).clientTrackingRange(20).updateInterval(1).fireImmune().noSave().noSummon().build("wildhunt:genesis_seal"));
    public static final RegistrySupplier<Item> EGG=ITEMS.register("eschaton_spawn_egg",()->new DeferredSpawnEggItem(ESCHATON,0x111a2b,0x9df7ed,new Item.Properties()));
    public static final RegistrySupplier<Item> INVITATION=ITEMS.register("last_convergence",()->new EschatonItems.Invitation(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> CROWN=ITEMS.register("crown_of_the_last_myth",()->new EschatonItems.Crown(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> FRAGMENT=ITEMS.register("origin_fragment",()->new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static void register(IEventBus bus){
        TYPES.register();ITEMS.register();bus.addListener(EschatonContent::attributes);bus.addListener(EschatonContent::tabs);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(EschatonCommands::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(EschatonItems::restoreCrown);
    }
    private static void attributes(EntityAttributeCreationEvent e){e.put(ESCHATON.get(),EschatonEntity.attributes().build());e.put(SEAL.get(),GenesisSealEntity.attributes().build());}
    private static void tabs(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG.get());if(e.getTabKey()==CreativeModeTabs.COMBAT){e.accept(INVITATION.get());e.accept(CROWN.get());e.accept(FRAGMENT.get());}}
}
