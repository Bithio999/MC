package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ApexTimers {
    private ApexTimers() {}
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        var storage = SkillAPI.getSkillsFrom(event.getEntity());
        for (var instance : storage.getLearnedSkills()) {
            if (!(instance.getSkill() instanceof ApexSkill skill) || instance.getOrCreateTag().getInt("combat_revision") >= 4) continue;
            // Existing saves retain the erroneous 80/200/400/1200-second values.
            for (int mode = 0; mode < instance.getModes(); mode++) {
                instance.setCoolDown(Math.min(instance.getCoolDown(mode), skill.balance.cooldown(mode, skill.ultimate)), mode);
            }
            for (String timer : java.util.List.of("field_until", "domain_until", "travel_until")) instance.getOrCreateTag().remove(timer);
            instance.getOrCreateTag().putInt("combat_revision", 4);
            instance.markDirty(); storage.checkAndMarkDirty(instance);
        }
    }
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()
                || !entity.getPersistentData().getCompound("wildhunt").getBoolean("active_skills")) return;
        boolean active = false;
        for (var instance : SkillAPI.getSkillsFrom(entity).getLearnedSkills()) {
            if (instance.getSkill() instanceof ApexSkill skill) active |= skill.tickActive(instance, entity);
        }
        if (!active) HunterData.of(entity).remove("active_skills");
    }
}
