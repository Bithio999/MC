package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class HunterEvents {
    private HunterEvents() {}
    public static void onKill(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !(event.getEntity() instanceof Enemy) || player.isAlliedTo(event.getEntity())) return;
        if (!HunterData.human(player) && HunterData.stage(player) <= 0) return;
        var data = HunterData.of(player);
        data.putInt("hunts", Math.min(Integer.MAX_VALUE - 1, data.getInt("hunts")) + 1);
        if (HunterData.stage(player) < 1) return;
        for (Apex apex : Apex.values()) {
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, WildHunt.id("trials/" + apex.id()));
            if (!event.getEntity().getType().is(tag)) continue;
            int count = Math.min(10, data.getInt(apex.id() + "_kills") + 1);
            data.putInt(apex.id() + "_kills", count);
            if (count >= 10 && !data.getBoolean(apex.id() + "_trial")) {
                var skills = SkillAPI.getSkillsFrom(player);
                var unique = WildHunt.UNIQUE.get(apex).get();
                if (skills.getSkill(unique).isPresent() || HunterData.learnReward(player, unique)) {
                    data.putBoolean(apex.id() + "_trial", true);
                    player.sendSystemMessage(Component.literal("Apex trial complete: " + apex.uniqueName + " unlocked!"));
                }
            }
        }
    }
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains("wildhunt")) {
            event.getEntity().getPersistentData().put("wildhunt", HunterData.of(event.getOriginal()).copy());
        }
    }
}
