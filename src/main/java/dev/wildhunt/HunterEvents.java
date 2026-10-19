package dev.wildhunt;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class HunterEvents {
    private HunterEvents() {}
    public static final TagKey<EntityType<?>> PROGRESSION_BOSSES =
        TagKey.create(Registries.ENTITY_TYPE, WildHunt.id("progression_bosses"));
    public static void onKill(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !event.getEntity().getType().is(PROGRESSION_BOSSES) || player.isAlliedTo(event.getEntity())) return;
        if (!HunterData.human(player) && HunterData.stage(player) <= 0
                && dev.wildhunt.myths.DuoSystem.secondaryFamily(player).orElse(null) != dev.wildhunt.myths.RaceFamily.HUMAN) return;
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (HunterData.recordBossKill(player, id)) {
            player.sendSystemMessage(Component.literal("New boss defeated: ").append(event.getEntity().getType().getDescription())
                .append(" (" + HunterData.bossKills(player).size() + " distinct bosses)"));
        }
    }
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains("wildhunt")) {
            event.getEntity().getPersistentData().put("wildhunt", HunterData.of(event.getOriginal()).copy());
        }
    }
}
