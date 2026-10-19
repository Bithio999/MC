package dev.wildhunt;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = WildHunt.ID, bus = EventBusSubscriber.Bus.MOD)
public final class HunterOtherworlders {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(WildHunt.ID, Registries.ENTITY_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WildHunt.ID, Registries.ITEM);
    public static final RegistrySupplier<EntityType<HunterOtherworlderEntity>> ALMA = type("alma");
    public static final RegistrySupplier<EntityType<HunterOtherworlderEntity>> GEMMA = type("gemma");
    private static RegistrySupplier<EntityType<HunterOtherworlderEntity>> type(String name) {
        var type = TYPES.register(name, () -> EntityType.Builder.<HunterOtherworlderEntity>of(HunterOtherworlderEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10).build(WildHunt.id(name).toString()));
        ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, name.equals("alma") ? 0x8b4936 : 0xc1a653, 0xe4d2ae, new Item.Properties()));
        return type;
    }
    public static void register() { TYPES.register(); ITEMS.register(); }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        var attributes = io.github.manasmods.tensura.entity.template.TensuraTamableEntity.setAttributes()
            .add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ARMOR, 6).add(Attributes.STEP_HEIGHT, 1).build();
        event.put(ALMA.get(), attributes); event.put(GEMMA.get(), attributes);
    }
}
