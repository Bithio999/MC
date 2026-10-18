package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HunterData {
    private HunterData() {}
    public static CompoundTag of(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains("wildhunt")) root.put("wildhunt", new CompoundTag());
        return root.getCompound("wildhunt");
    }
    public static int stage(LivingEntity entity) {
        return RaceAPI.getRaceFrom(entity).getRace()
            .map(r -> r.getRace() instanceof HunterRace hunter ? hunter.stage : 0).orElse(-1);
    }
    public static boolean human(LivingEntity entity) {
        return RaceAPI.getRaceFrom(entity).getRace()
            .map(r -> r.getRaceId().toString().equals("tensura:human")).orElse(false);
    }
    /** Boss credit is keyed by registry ID, never by an individual spawn's UUID. */
    public static java.util.Set<String> bossKills(LivingEntity entity) {
        var result = new java.util.TreeSet<String>();
        for (var tag : of(entity).getList("boss_kills", net.minecraft.nbt.Tag.TAG_STRING)) {
            result.add(tag.getAsString());
        }
        return java.util.Collections.unmodifiableSet(result);
    }
    public static boolean recordBossKill(LivingEntity entity, net.minecraft.resources.ResourceLocation bossId) {
        if (bossKills(entity).contains(bossId.toString())) return false;
        var kills = of(entity).getList("boss_kills", net.minecraft.nbt.Tag.TAG_STRING);
        kills.add(net.minecraft.nbt.StringTag.valueOf(bossId.toString()));
        of(entity).put("boss_kills", kills);
        return true;
    }
    public static int mastered(LivingEntity entity) {
        int count = 0;
        for (Apex apex : Apex.values()) {
            var skills = SkillAPI.getSkillsFrom(entity);
            if (skills.getSkill(WildHunt.UNIQUE.get(apex).get()).map(s -> s.isMastered(entity)).orElse(false)
                    || skills.getSkill(WildHunt.ULTIMATE.get(apex).get()).isPresent()) count++;
        }
        return count;
    }
    public static boolean learnReward(LivingEntity entity, ApexSkill skill) {
        var instance = skill.createDefaultInstance();
        // Tensura's supported reward flag: granting an ascension reward must not also
        // consume permanent max magicules as if buying the skill from a menu.
        instance.getOrCreateTag().putBoolean("NoMagiculeCost", true);
        return SkillAPI.getSkillsFrom(entity).learnSkill(instance);
    }
    public static boolean canHarm(LivingEntity caster, LivingEntity target) {
        if (caster == target || !target.isAlive() || caster.isAlliedTo(target)) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        if (caster instanceof Player player && target instanceof Player other) {
            if (!player.canHarmPlayer(other)) return false;
            if (caster.getServer() != null && !caster.getServer().isPvpAllowed()) return false;
        }
        if (target instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isOwnedBy(caster)) return false;
        return true;
    }
}
