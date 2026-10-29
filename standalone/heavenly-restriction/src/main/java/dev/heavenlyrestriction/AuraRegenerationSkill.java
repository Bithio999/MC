package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.effect.EffectStorage;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Aura-paid counterparts of Self, Ultraspeed and Infinite Regeneration. */
public final class AuraRegenerationSkill extends Skill {
    public final int tier;
    public static final String[] IDS = {"hunters_recovery", "apex_renewal", "sovereign_rebirth"};
    public static final String[] NAMES = {"Hunter's Recovery", "Apex Renewal", "Sovereign Rebirth"};
    public AuraRegenerationSkill(int tier) { super(SkillType.EXTRA); this.tier = tier; }
    @Override public MutableComponent getName() { return Component.translatableWithFallback("heavenlyrestriction.skill." + IDS[tier-1], NAMES[tier-1]); }
    @Override public MutableComponent getColoredName() { return getName().withStyle(getType().getChatFormatting()); }
    @Override public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath("tensura", "textures/skill/" + switch(tier) {
            case 1 -> "common/self_regeneration"; case 2 -> "extra/ultraspeed_regeneration"; default -> "extra/infinite_regeneration";
        } + ".png");
    }
    @Override public MutableComponent getSkillDescription() {
        return Component.literal(switch(tier) {
            case 1 -> "Regenerate 4 HP each second for 20 aura per HP. Mastery doubles healing speed. No cost at full health.";
            case 2 -> "Restore missing HP every half second for 30 aura per HP (20 mastered).";
            default -> "Restore HP and SHP every half second for 30 aura per HP and 10 per SHP (20/5 mastered). Can avert ordinary lethal damage while funded. Severance, healing suppression and high barrier bypass still apply.";
        } + " Only your strongest toggled aura regeneration skill works. Uses aura, never magicules.");
    }
    @Override public boolean checkAcquiringRequirement(Player player, double newEP) { return false; }
    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }
    @Override public boolean canBeSlotted(ManasSkillInstance instance, LivingEntity entity, int mode) { return false; }
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return false; }
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }
    @Override public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) { instance.setToggled(true); }
    @Override public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) { instance.setToggled(false); }
    @Override public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        RestrictionData.of(entity).putBoolean("has_aura_recovery", true);
    }
    static ManasSkillInstance strongest(LivingEntity entity) {
        return SkillAPI.getSkillsFrom(entity).getLearnedSkills().stream()
            .filter(i -> i.isToggled() && i.getSkill() instanceof AuraRegenerationSkill)
            .max(java.util.Comparator.comparingInt(i -> ((AuraRegenerationSkill)i.getSkill()).tier)).orElse(null);
    }
    public static void tick(LivingEntity entity) {
        if (entity.tickCount % 10 != 0 || !entity.isAlive()
                || !entity.getPersistentData().getCompound("heavenlyrestriction").getBoolean("has_aura_recovery")) return;
        var instance = strongest(entity);
        if (instance == null) return;
        var skill = (AuraRegenerationSkill)instance.getSkill();
        if (skill.tier == 1 && entity.tickCount % 20 != 0) return;
        skill.regenerate(instance, entity);
        SkillAPI.getSkillsFrom(entity).checkAndMarkDirty(instance);
    }
    void regenerate(ManasSkillInstance instance, LivingEntity entity) {
        if (!canInteractSkill(instance, entity) || healingBlocked(entity) || SkillUtils.shouldCancelInteraction(entity)) return;
        double maxHP = EffectStorage.getSeveranceMaxHealth(entity);
        double missing = Math.max(0, maxHP - entity.getHealth());
        if (tier == 1) missing = Math.min(missing, instance.isMastered(entity) ? 8 : 4);
        boolean mastered = instance.isMastered(entity);
        double hpCost = tier == 1 ? 20 : mastered ? 20 : 30;
        double shpCost = mastered ? 5 : 10;
        var energy = TensuraStorages.getExistenceFrom(entity);
        double shp = tier == 3 ? Math.max(0, entity.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH) - energy.getSpiritualHealth()) : 0;
        if (missing == 0 && shp == 0) return;
        double budget = Math.max(0, energy.getAura()) / SchemaArt.costFactor(entity);
        double hp = Math.min(missing, budget / hpCost); budget -= hp * hpCost;
        double spirit = Math.min(shp, budget / shpCost);
        double cost = hp * hpCost + spirit * shpCost;
        if (cost <= 0) return;
        if (EnergyHelper.isOutOfEnergy(entity, cost, 0)) return;
        double beforeHP=entity.getHealth(),beforeSHP=energy.getSpiritualHealth();
        if (hp > 0) entity.heal((float)hp);
        if (spirit > 0) { energy.setSpiritualHealth(energy.getSpiritualHealth() + spirit); energy.markDirty(); }
        if(entity.level() instanceof net.minecraft.server.level.ServerLevel level && (entity.getHealth()>beforeHP || energy.getSpiritualHealth()>beforeSHP))
            ArtVisualEntity.spawn(level,ArtVisualEntity.HEALING_PULSE,entity.position(),new net.minecraft.world.phys.Vec3(.8+tier*.15,0,0),10).style(tier-1).setCaster(entity);
        addMasteryPoint(instance, entity);
    }
    /** Match native Infinite Regeneration's healing immunity without its MP drain. */
    public static boolean allowsInfiniteRecovery(LivingEntity entity) {
        if (!entity.getPersistentData().getCompound("heavenlyrestriction").getBoolean("has_aura_recovery")) return false;
        var instance = strongest(entity);
        return instance != null && ((AuraRegenerationSkill)instance.getSkill()).tier == 3
            && !SkillUtils.shouldCancelInteraction(entity)
            && !entity.hasEffect(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(
                io.github.manasmods.tensura.registry.effect.TensuraMobEffects.INFINITE_IMPRISONMENT));
    }
    private boolean healingBlocked(LivingEntity entity) {
        // Native Infinite Regeneration bypasses Curse, Frost and Black Burn II,
        // while Infinite Imprisonment still prevents recovery.
        return tier < 3 ? SkillUtils.shouldCancelHealing(entity) : entity.hasEffect(
            io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(
                io.github.manasmods.tensura.registry.effect.TensuraMobEffects.INFINITE_IMPRISONMENT));
    }
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide() || event.isCanceled()) return;
        var instance = strongest(entity);
        if (instance == null || ((AuraRegenerationSkill)instance.getSkill()).tier < 3
                || !instance.canInteractSkill(entity) || ((AuraRegenerationSkill)instance.getSkill()).healingBlocked(entity)
                || SkillUtils.shouldCancelInteraction(entity)
                || TensuraStorages.getExistenceFrom(entity).getSpiritualHealth() <= 0
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || ((io.github.manasmods.tensura.damage.TensuraDamageSource)event.getSource()).tensura$getBarrierBypassLevel() >= 1.75
                || EffectStorage.getSeveranceMaxHealth(entity) <= 0) return;
        double cost = instance.isMastered(entity) ? 20 : 30;
        if (EnergyHelper.isOutOfEnergy(entity, cost, 0)) return;
        entity.setHealth(1); event.setCanceled(true);
        if(entity.level() instanceof net.minecraft.server.level.ServerLevel level)ArtVisualEntity.spawn(level,ArtVisualEntity.HEALING_PULSE,entity.position(),new net.minecraft.world.phys.Vec3(1.5,0,0),18).style(2).setCaster(entity);
    }
}
