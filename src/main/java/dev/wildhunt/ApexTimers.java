package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ApexTimers {
    private ApexTimers() {}
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        // Native race modifiers are permanent, so refresh saved 0.5.0 bonuses too.
        io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(player).getRace().ifPresent(race -> {
            if (race.getRace() instanceof HunterRace) {
                float health = player.getHealth(); race.addAttributeModifiers(player);
                race.getRace().learnIntrinsicSkills(race, player);
                player.setHealth(Math.min(health, player.getMaxHealth()));
            }
        });
        MythicEvolution.migrate(player);
        var storage = SkillAPI.getSkillsFrom(player);
        for (var instance : storage.getLearnedSkills()) {
            if (!(instance.getSkill() instanceof ApexSkill skill)) continue;
            var tag = instance.getOrCreateTag();
            if (tag.getInt("combat_revision") < 5) {
                skill.removePassive(player); instance.setToggled(false);
                // Only the old Water Veil created these particular timed hearts.
                var absorption = player.getEffect(MobEffects.ABSORPTION);
                if (skill.apex == Apex.UTH_DUNA && tag.getLong("mantle_until") > player.level().getGameTime()
                        && absorption != null && absorption.getAmplifier() == (skill.ultimate ? 7 : 3)
                        && absorption.getDuration() <= (skill.ultimate ? 600 : 300)) player.removeEffect(MobEffects.ABSORPTION);
                var old = instance.getCooldownList(); var corrected = new java.util.ArrayList<Integer>();
                for (int mode = 0; mode < instance.getModes(); mode++) {
                    int source = mode == 2 ? 3 : mode;
                    int value = source < old.size() ? old.get(source) : 0;
                    corrected.add(Math.min(value, skill.balance.cooldown(mode, skill.ultimate)));
                }
                instance.setCoolDownList(corrected);
                var abilities = io.github.manasmods.tensura.storage.TensuraStorages.getAbilityFrom(player);
                for (var preset : abilities.getPresets()) for (var slot : preset.getAbilities()) {
                    if (slot.getSkill() != skill) continue;
                    if (slot.getMode() == 3 && skill.ultimate) slot.setMode(2);
                    else if (slot.getMode() >= 2 || slot.getMode() < 0) slot.setMode(0);
                }
                abilities.markDirty();
                for (String key : java.util.List.of("mantle_until", "mantle_last_x", "mantle_last_y", "mantle_last_z",
                        "storm_charge", "ward_budget", "field_until", "domain_until", "domain_hits", "travel_until", "ripple_until")) tag.remove(key);
                tag.putInt("combat_revision", 5); instance.markDirty();
            }
            skill.restorePassive(instance, player);
            storage.checkAndMarkDirty(instance);
        }
    }
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        AuraRegenerationSkill.tick(entity);GodlySkill.tickSlow(entity);MythicArts.tickTarget(entity);
        if(entity.tickCount%20==0)MythicEvolution.migrate(entity);
        var data = entity.getPersistentData().getCompound("wildhunt");
        // Legacy marks no longer affect any attack and can be discarded on load.
        if (data.contains("oil_stacks") || data.contains("rime_stacks"))
            for (String key : java.util.List.of("oil_stacks", "oil_owner", "oil_until", "rime_stacks", "rime_owner", "rime_until")) data.remove(key);
        if (!data.getBoolean("active_skills")) return;
        boolean active = false; var storage = SkillAPI.getSkillsFrom(entity);
        for (var instance : storage.getLearnedSkills()) {
            if (instance.getSkill() instanceof ApexSkill skill) {
                active |= skill.tickActive(instance, entity);
                if (instance.isDirty()) storage.checkAndMarkDirty(instance);
            } else if(instance.getSkill() instanceof MythicSkill skill) {
                active |= skill.tickActive(instance,entity);
                if(instance.isDirty())storage.checkAndMarkDirty(instance);
            } else if (instance.getSkill() instanceof GodlySkill skill) {
                active |= skill.tickActive(instance,entity);
                if(instance.isDirty()) storage.checkAndMarkDirty(instance);
            }
        }
        if (!active) data.remove("active_skills");
    }
}
