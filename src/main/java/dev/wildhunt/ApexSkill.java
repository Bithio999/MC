package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Native Tensura skill: slotted modes, aura costs, mastery and per-mode cooldowns. */
public final class ApexSkill extends Skill {
    public final Apex apex;
    public final boolean ultimate;
    public ApexSkill(Apex apex, boolean ultimate) {
        super(ultimate ? SkillType.ULTIMATE : SkillType.UNIQUE);
        this.apex = apex; this.ultimate = ultimate;
    }
    @Override public MutableComponent getName() { return Component.literal(ultimate ? apex.ultimateName : apex.uniqueName); }
    @Override public ResourceLocation getSkillIcon() { return WildHunt.id("textures/skill/" + (ultimate ? apex.ultimateId : apex.uniqueId) + ".png"); }
    @Override public int getModes(ManasSkillInstance instance) { return ultimate ? 4 : 3; }
    @Override public String getModeId(ManasSkillInstance instance, int mode) { return "mode_" + mode; }
    @Override public Component getModeName(ManasSkillInstance instance, int mode) { return Component.literal(modeName(mode + 1)); }
    @Override public int nextMode(LivingEntity entity, ManasSkillInstance instance, int current, boolean reverse) {
        return Math.floorMod(current + (reverse ? -1 : 1), getModes(instance));
    }
    private String modeName(int mode) {
        if (mode == 4) return "Sovereign Domain";
        return switch (apex) {
            case REY_DAU -> switch (mode) { case 1 -> "Railbolt"; case 2 -> "Thunderclap"; default -> "Storm Mantle"; };
            case UTH_DUNA -> switch (mode) { case 1 -> "Crushing Current"; case 2 -> "Scarlet Undertow"; default -> "Water Veil"; };
            case NU_UDRA -> switch (mode) { case 1 -> "Oil Lash"; case 2 -> "Blackflame Ignition"; default -> "Furnace Mantle"; };
            case JIN_DAHAAD -> switch (mode) { case 1 -> "Glacial Lance"; case 2 -> "Freezing Mist"; default -> "Colossus Guard"; };
        };
    }
    @Override public MutableComponent getSkillDescription() {
        return Component.literal("Inspired by " + apex.name().replace('_', ' ') + ". "
            + "1: " + modeName(1) + "; 2: " + modeName(2) + "; 3: " + modeName(3)
            + (ultimate ? "; 4: a ten-second elemental domain." : ". Master this skill and ascend as a Sovereign Hunter to evolve it.")
            + " Consumes aura. Attacks respect teams, PvP settings and line of sight.");
    }
    @Override public double getDefaultAcquiringMagiculeCost() { return ultimate ? 1_000_000 : 25_000; }
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }
    @Override public double getAuraCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return (ultimate ? 600 : 100) * (mode == 3 ? 8 : mode == 1 ? 2 : 1);
    }
    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return false; }
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return instance.getOrCreateTag().getLong("domain_until") > entity.level().getGameTime();
    }
    @Override public void onPressed(ManasSkillInstance instance, LivingEntity caster, int slot, int mode) {
        if (!(caster.level() instanceof ServerLevel level) || mode < 0 || mode >= getModes(instance)) return;
        if (instance.onCoolDown(mode) || !canActivateSkill(instance, caster, mode)) return;
        if (EnergyHelper.isOutOfEnergy(caster, instance, mode)) return;
        int action = mode + 1;
        switch (action) {
            case 1 -> beam(level, caster);
            case 2 -> burst(level, caster, ultimate ? 10 : 6);
            case 3 -> guard(caster);
            case 4 -> {
                instance.getOrCreateTag().putLong("domain_until", level.getGameTime() + 200);
                instance.markDirty();
                burst(level, caster, 12);
            }
            default -> { return; }
        }
        instance.setCoolDown(switch (action) { case 1 -> 80; case 2 -> 200; case 3 -> 400; default -> 1200; }, mode);
        addMasteryPoint(instance, caster);
        level.playSound(null, caster.blockPosition(), switch (apex) {
            case REY_DAU -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case UTH_DUNA -> SoundEvents.GENERIC_SPLASH;
            case NU_UDRA -> SoundEvents.FIRECHARGE_USE;
            case JIN_DAHAAD -> SoundEvents.GLASS_BREAK;
        }, SoundSource.PLAYERS, 0.6f, ultimate ? 0.65f : 1.1f);
    }
    @Override public void onTick(ManasSkillInstance instance, LivingEntity caster) {
        if (caster.level() instanceof ServerLevel level && level.getGameTime() % 20 == 0
                && instance.getOrCreateTag().getLong("domain_until") > level.getGameTime()) {
            burst(level, caster, 12);
        }
    }
    private void beam(ServerLevel level, LivingEntity caster) {
        Vec3 start = caster.getEyePosition();
        Vec3 desired = start.add(caster.getLookAngle().scale(ultimate ? 40 : 24));
        Vec3 end = level.clip(new ClipContext(start, desired, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster)).getLocation();
        Vec3 direction = end.subtract(start);
        int steps = Math.max(1, (int) (direction.length() * 3));
        for (int i = 0; i <= steps; i++) particles(level, start.add(direction.scale((double) i / steps)), 2, 0.08);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1))) {
            if (HunterData.canHarm(caster, target) && caster.hasLineOfSight(target)
                    && target.getBoundingBox().inflate(ultimate ? 0.8 : 0.4).clip(start, end).isPresent()) {
                strike(caster, target, ultimate ? 18 : 8, false);
            }
        }
    }
    private void burst(ServerLevel level, LivingEntity caster, double radius) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius));
        for (LivingEntity target : targets) {
            if (!HunterData.canHarm(caster, target) || caster.distanceToSqr(target) > radius * radius || !caster.hasLineOfSight(target)) continue;
            strike(caster, target, ultimate ? 10 : 5, true);
            Vec3 horizontal = target.position().subtract(caster.position()).multiply(1, 0, 1).normalize();
            if (apex == Apex.UTH_DUNA) horizontal = horizontal.scale(-1);
            target.push(horizontal.x * 0.4, 0.12, horizontal.z * 0.4);
            target.hurtMarked = true;
        }
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48;
            particles(level, caster.position().add(Math.cos(angle) * radius, 0.7, Math.sin(angle) * radius), 2, 0.15);
        }
    }
    private void strike(LivingEntity caster, LivingEntity target, float damage, boolean area) {
        long now = caster.level().getGameTime();
        var data = HunterData.of(target);
        switch (apex) {
            case REY_DAU -> {
                if (target.isInWaterOrRain() || data.getLong("drenched_until") > now) damage *= 1.3f;
            }
            case UTH_DUNA -> {
                data.putLong("drenched_until", now + 200);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, ultimate ? 2 : 0));
            }
            case NU_UDRA -> {
                if (!area) {
                    data.putLong("oil_until", now + 200);
                    data.putUUID("oil_owner", caster.getUUID());
                } else if (data.getLong("oil_until") > now && data.hasUUID("oil_owner") && caster.getUUID().equals(data.getUUID("oil_owner"))) {
                    damage *= 2;
                    data.remove("oil_until"); data.remove("oil_owner");
                    target.igniteForSeconds(ultimate ? 8 : 4);
                }
            }
            case JIN_DAHAAD -> {
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 100));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ultimate ? 100 : 60, ultimate ? 3 : 1));
            }
        }
        var damageType = switch (apex) {
            case REY_DAU -> TensuraDamageTypes.LIGHTNING_ELEMENTAL;
            case UTH_DUNA -> TensuraDamageTypes.WATER_ELEMENTAL;
            case NU_UDRA -> TensuraDamageTypes.FIRE_ELEMENTAL;
            case JIN_DAHAAD -> TensuraDamageTypes.ICE_ELEMENTAL;
        };
        // Tensura's elemental types preserve its resistance system and caster attribution.
        target.hurt(TensuraDamageTypes.getIndirectEntityDamageSource(caster.level(), damageType, caster, caster), damage);
    }
    private void guard(LivingEntity caster) {
        int duration = ultimate ? 240 : 120;
        switch (apex) {
            case REY_DAU -> caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, ultimate ? 2 : 1));
            case UTH_DUNA -> {
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, ultimate ? 3 : 1));
                caster.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration));
            }
            case NU_UDRA -> {
                caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration));
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, ultimate ? 1 : 0));
            }
            case JIN_DAHAAD -> caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, ultimate ? 2 : 0));
        }
    }
    private void particles(ServerLevel level, Vec3 pos, int count, double spread) {
        level.sendParticles(switch (apex) {
            case REY_DAU -> ParticleTypes.ELECTRIC_SPARK;
            case UTH_DUNA -> ParticleTypes.SPLASH;
            case NU_UDRA -> ParticleTypes.FLAME;
            case JIN_DAHAAD -> ParticleTypes.SNOWFLAKE;
        }, pos.x, pos.y, pos.z, count, spread, spread, spread, 0.02);
    }
}
