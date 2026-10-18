package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** Native Tensura rank/element damage, with per-tick visual and field scheduling. */
public final class ApexSkill extends Skill {
    public final Apex apex;
    public final boolean ultimate;
    public final ApexBalance balance;
    public ApexSkill(Apex apex, boolean ultimate) {
        super(ultimate ? SkillType.ULTIMATE : SkillType.UNIQUE);
        this.apex = apex; this.ultimate = ultimate; this.balance = ApexBalance.of(apex);
    }
    private String key() { return "wildhunt.skill." + (ultimate ? apex.ultimateId : apex.uniqueId); }
    @Override public MutableComponent getName() {
        return Component.translatableWithFallback(key(), ultimate ? apex.ultimateName : apex.uniqueName);
    }
    @Override public MutableComponent getColoredName() { return getName().withStyle(getType().getChatFormatting()); }
    @Override public ResourceLocation getSkillIcon() {
        return WildHunt.id("textures/skill/" + (ultimate ? apex.ultimateId : apex.uniqueId) + ".png");
    }
    @Override public int getModes(ManasSkillInstance instance) { return ultimate ? 4 : 3; }
    @Override public String getModeId(ManasSkillInstance instance, int mode) { return (ultimate ? apex.ultimateId : apex.uniqueId) + ".mode_" + mode; }
    @Override public Component getModeName(ManasSkillInstance instance, int mode) {
        return Component.translatableWithFallback(key() + ".mode_" + mode, modeName(mode));
    }
    @Override public int nextMode(LivingEntity entity, ManasSkillInstance instance, int current, boolean reverse) {
        return Math.floorMod(current + (reverse ? -1 : 1), getModes(instance));
    }
    public String modeName(int mode) {
        if (mode == 3) return switch (apex) {
            case REY_DAU -> "Heaven's Thunder"; case UTH_DUNA -> "Crimson Ocean";
            case NU_UDRA -> "Black Sun Caldera"; case JIN_DAHAAD -> "Absolute Winter";
        };
        return switch (apex) {
            case REY_DAU -> switch (mode) { case 0 -> "Railbolt"; case 1 -> "Fulgurite Array"; default -> "Storm Mantle"; };
            case UTH_DUNA -> switch (mode) { case 0 -> "Crushing Current"; case 1 -> "Scarlet Undertow"; default -> "Water Veil"; };
            case NU_UDRA -> switch (mode) { case 0 -> "Oil Lash"; case 1 -> "Blackflame Ignition"; default -> "Furnace Mantle"; };
            case JIN_DAHAAD -> switch (mode) { case 0 -> "Glacial Rupture"; case 1 -> "Glacial Rampart"; default -> "Colossus Guard"; };
        };
    }
    public String description() {
        return switch (apex) {
            case REY_DAU -> "Railbolt consumes up to three charges for +25% damage each. Fulgurite rods strike nearby targets to recharge it. Storm Mantle gains charge by moving. Wet targets take 30% more damage.";
            case UTH_DUNA -> "A travelling tidal wave carries and drenches enemies. Aim a persistent whirlpool to gather them. Water Veil retaliates after health damage. Crimson Ocean alternates inward and outward tides.";
            case NU_UDRA -> "Tentacle lashes grab enemies, stack oil and leave up to three oil pools (five as Agni). Ignition burns only your pools. Furnace Mantle coats melee victims in oil. Caldera establishes a burning pool network.";
            case JIN_DAHAAD -> "Ground fissures shatter fully rimed enemies. Glacial Rampart chills enemies crossing a frontal ward and intercepts three hostile projectiles (six as Ymir). Absolute Winter is an expanding frostfront that primes shatter.";
        } + (ultimate ? " Ultimate rank: stronger attacks and defenses, plus a ten-second signature ability." : " Damage grows with EP and mastery. Master this skill to unlock its Ultimate as Sovereign Hunter.");
    }

    @Override public MutableComponent getSkillDescription() { return Component.translatableWithFallback(key() + ".description", description()); }
    @Override public double getDefaultAcquiringMagiculeCost() { return ultimate ? 1_000_000 : 25_000; }
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }
    @Override public double getAuraCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return balance.aura(mode, ultimate); }
    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return false; }
    // ManasCore's onTick runs once per FIVE seconds. Our event handler owns short timers.
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return false; }
    @Override public void onTick(ManasSkillInstance instance, LivingEntity entity) {}
    private float power(ManasSkillInstance instance, LivingEntity caster) {
        return ApexBalance.power(EnergyHelper.getBaseMaxEP(caster), instance.getMastery() / Math.max(1, instance.getMaxMastery()), ultimate);
    }
    @Override public void onPressed(ManasSkillInstance instance, LivingEntity caster, int slot, int mode) {
        if (!(caster.level() instanceof ServerLevel level) || mode < 0 || mode >= getModes(instance)) return;
        if (instance.onCoolDown(mode) || !canActivateSkill(instance, caster, mode) || EnergyHelper.isOutOfEnergy(caster, instance, mode)) return;
        long now = level.getGameTime();
        var tag = instance.getOrCreateTag();
        switch (mode) {
            case 0 -> {
                ApexVisuals.circle(level, caster, apex, caster.position().add(0, 0.15, 0), 2, 16, false);
                ApexArts.primary(this, level, instance, caster);
            }
            case 1 -> ApexArts.secondary(this, level, instance, caster);
            case 2 -> {
                tag.putLong("mantle_until", now + (ultimate ? 600 : 300));
                ApexArts.point(tag, "mantle_last", caster.position());
                applyMantle(caster); mantleEffects(caster); startRipple(instance, caster, 2);
            }
            case 3 -> ApexArts.domain(this, level, instance, caster);
            default -> { return; }
        }
        instance.markDirty();
        HunterData.of(caster).putBoolean("active_skills", true);
        // ManasCore cooldown API is (seconds, zero-based mode).
        instance.setCoolDown(balance.cooldown(mode, ultimate), mode);
        addMasteryPoint(instance, caster);
        level.playSound(null, caster.blockPosition(), switch (apex) {
            case REY_DAU -> mode == 0 ? SoundEvents.TRIDENT_THUNDER.value() : SoundEvents.LIGHTNING_BOLT_THUNDER;
            case UTH_DUNA -> SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE;
            case NU_UDRA -> SoundEvents.BLAZE_SHOOT;
            case JIN_DAHAAD -> SoundEvents.GLASS_BREAK;
        }, SoundSource.PLAYERS, mode == 3 ? 1.1f : 0.75f, ultimate ? 0.7f : 1.0f);
    }
    double radius() { return ultimate ? balance.radius() + 4 : balance.radius(); }
    private void startRipple(ManasSkillInstance instance, LivingEntity caster, double radius) {
        var t = instance.getOrCreateTag(); long now = caster.level().getGameTime();
        t.putLong("ripple_start", now); t.putLong("ripple_until", now + 12); t.putDouble("ripple_radius", radius);
        t.putDouble("ripple_x", caster.getX()); t.putDouble("ripple_y", caster.getY()); t.putDouble("ripple_z", caster.getZ());
    }
    public boolean tickActive(ManasSkillInstance instance, LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        var tag = instance.getOrCreateTag(); long now = level.getGameTime(); boolean active = false;
        if (tag.getLong("mantle_until") > now) {
            active = true;
            if (now % 20 == 0) {
                applyMantle(caster);
                if (apex == Apex.REY_DAU) {
                    double distance = caster.position().distanceTo(ApexArts.point(tag, "mantle_last"));
                    if (distance >= 3 && distance <= 16) ApexArts.charge(instance, 1);
                    ApexArts.point(tag, "mantle_last", caster.position());
                }
            }
            if (now % 4 == 0) ApexVisuals.mantle(level, apex, caster.position(), ultimate);
        } else if (tag.contains("mantle_until")) {
            removeMantle(caster); tag.remove("mantle_until"); instance.markDirty();
        }
        active |= ApexArts.tick(this, level, instance, caster);
        if (tag.getLong("ripple_until") > now) {
            active = true;
            if (now % 2 == 0) {
                Vec3 origin = new Vec3(tag.getDouble("ripple_x"), tag.getDouble("ripple_y"), tag.getDouble("ripple_z"));
                double size = tag.getDouble("ripple_radius") * (now - tag.getLong("ripple_start") + 1) / 12;
                ApexVisuals.ring(level, apex, origin, size, 0.2, ultimate);
            }
        }
        return active;
    }
    void railgun(ServerLevel level, ManasSkillInstance instance, LivingEntity caster, float multiplier, int charges) {
        Vec3 start = caster.getEyePosition();
        Vec3 desired = start.add(caster.getLookAngle().scale(ultimate ? 56 : 36));
        Vec3 end = level.clip(new ClipContext(start, desired, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
        ApexVisuals.beam(level, apex, start, end, ultimate);
        var targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.2));
        targets.sort(Comparator.comparingDouble(caster::distanceToSqr));
        int hits = 0;
        for (LivingEntity target : targets) {
            if (!HunterData.canHarm(caster, target) || !caster.hasLineOfSight(target)
                    || target.getBoundingBox().inflate(ultimate ? 1 : 0.6).clip(start, end).isEmpty()) continue;
            if (strike(level, instance, caster, target, balance.primary() * multiplier, 0, false) && apex == Apex.REY_DAU)
                chain(level, instance, caster, target, charges);
            if (++hits >= (ultimate ? 4 : apex == Apex.REY_DAU ? 2 : 1)) break;
        }
    }
    private void chain(ServerLevel level, ManasSkillInstance instance, LivingEntity caster, LivingEntity origin, int charges) {
        var nearby = level.getEntitiesOfClass(LivingEntity.class, origin.getBoundingBox().inflate(6));
        nearby.sort(Comparator.comparingDouble(origin::distanceToSqr)); int count = 0;
        for (LivingEntity target : nearby) {
            if (target == origin || !HunterData.canHarm(caster, target) || !origin.hasLineOfSight(target) || !caster.hasLineOfSight(target)) continue;
            if (strike(level, instance, caster, target, balance.primary() * 0.55f, 0, true)) {
                ApexVisuals.beam(level, apex, origin.getEyePosition(), target.getEyePosition(), ultimate);
                if (++count >= (ultimate ? 5 : 3) + charges) break;
            }
        }
    }
    void area(ServerLevel level, ManasSkillInstance instance, LivingEntity caster, Vec3 center,
                      double radius, float damage, int mode, boolean pulse) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius))) {
            if (!HunterData.canHarm(caster, target) || target.position().distanceToSqr(center) > radius * radius || !caster.hasLineOfSight(target)) continue;
            if (!strike(level, instance, caster, target, damage, mode, pulse)) continue;
            Vec3 push = target.position().subtract(center).multiply(1, 0, 1).normalize();
            if (apex == Apex.UTH_DUNA) push = push.scale(-1.8);
            ApexArts.push(target, push.scale(0.4).add(0, apex == Apex.JIN_DAHAAD ? 0 : 0.15, 0));
        }
    }
    boolean strike(ServerLevel level, ManasSkillInstance instance, LivingEntity caster, LivingEntity target,
                           float raw, int mode, boolean pulse) {
        long now = level.getGameTime(); var data = HunterData.of(target);
        boolean ownOil = data.hasUUID("oil_owner") && data.getUUID("oil_owner").equals(caster.getUUID()) && data.getLong("oil_until") > now;
        boolean ownRime = data.hasUUID("rime_owner") && data.getUUID("rime_owner").equals(caster.getUUID()) && data.getLong("rime_until") > now;
        boolean shatter = apex == Apex.JIN_DAHAAD && mode == 0 && ownRime && data.getInt("rime_stacks") >= 3;
        float damage = raw;
        if (apex == Apex.REY_DAU && (target.isInWaterOrRain() || data.getLong("drenched_until") > now)) damage *= 1.3f;
        if (apex == Apex.UTH_DUNA && mode == 0 && data.getLong("drenched_until") > now) damage *= 1.5f;
        if (apex == Apex.NU_UDRA && mode == 1 && !pulse && ownOil) damage *= 1 + 0.6f * Math.max(1, data.getInt("oil_stacks"));
        if (shatter) damage += 120;
        var type = switch (apex) {
            case REY_DAU -> TensuraDamageTypes.LIGHTNING_ELEMENTAL;
            case UTH_DUNA -> TensuraDamageTypes.WATER_ELEMENTAL;
            case NU_UDRA -> TensuraDamageTypes.FIRE_ELEMENTAL;
            case JIN_DAHAAD -> TensuraDamageTypes.ICE_ELEMENTAL;
        };
        // This attaches the Unique/Ultimate rank, ability instance, mode and costs,
        // which the old generic elemental source omitted.
        if (!target.hurt(createSource(instance, caster, type, mode), damage * power(instance, caster))) return false;
        switch (apex) {
            case REY_DAU -> {
                if (mode != 0) addEffect(target, instance, mode, caster, MobEffects.MOVEMENT_SLOWDOWN, 40, ultimate ? 3 : 1);
            }
            case UTH_DUNA -> {
                data.putLong("drenched_until", now + 240);
                addEffect(target, instance, mode, caster, MobEffects.MOVEMENT_SLOWDOWN, 100, ultimate ? 3 : 1);
            }
            case NU_UDRA -> {
                if (mode == 0 && !pulse) {
                    int stacks = ownOil ? data.getInt("oil_stacks") : 0;
                    data.putInt("oil_stacks", Math.min(3, stacks + 1));
                    data.putLong("oil_until", now + 300); data.putUUID("oil_owner", caster.getUUID());
                    addEffect(target, instance, mode, caster, MobEffects.MOVEMENT_SLOWDOWN, 80, 1);
                } else {
                    if (!pulse && mode == 1) { data.remove("oil_until"); data.remove("oil_stacks"); }
                    addEffect(target, instance, mode, caster, TensuraMobEffects.getReference(TensuraMobEffects.BLACK_BURN), 200, ultimate ? 2 : 0);
                }
            }
            case JIN_DAHAAD -> {
                if (shatter) { data.remove("rime_until"); data.remove("rime_stacks"); }
                else {
                    data.putInt("rime_stacks", Math.min(3, (ownRime ? data.getInt("rime_stacks") : 0) + 1));
                    data.putLong("rime_until", now + 200); data.putUUID("rime_owner", caster.getUUID());
                }
                addEffect(target, instance, mode, caster, TensuraMobEffects.getReference(TensuraMobEffects.CHILL), 160, ultimate ? 3 : 1);
                addEffect(target, instance, mode, caster, MobEffects.MOVEMENT_SLOWDOWN, 100, ultimate ? 3 : 2);
            }
        }
        ApexVisuals.impact(level, apex, target.position().add(0, target.getBbHeight() * 0.5, 0), ultimate || shatter);
        return true;
    }
    private Holder<Attribute> boost() {
        return switch (apex) {
            case REY_DAU -> TensuraAttributes.LIGHTNING_BOOST;
            case UTH_DUNA -> TensuraAttributes.WATER_BOOST;
            case NU_UDRA -> TensuraAttributes.FLAME_BOOST;
            case JIN_DAHAAD -> TensuraAttributes.WATER_BOOST;
        };
    }
    private ResourceLocation mantleId() { return WildHunt.id((ultimate ? apex.ultimateId : apex.uniqueId) + "_mantle"); }
    private void modify(LivingEntity caster, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        var instance = caster.getAttribute(attribute);
        if (instance != null) instance.addOrUpdateTransientModifier(new AttributeModifier(mantleId(), amount, operation));
    }
    private void applyMantle(LivingEntity caster) {
        var add = AttributeModifier.Operation.ADD_VALUE; var multiply = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        modify(caster, boost(), ultimate ? 1.0 : 0.5, multiply);
        switch (apex) {
            case REY_DAU -> {
                modify(caster, Attributes.MOVEMENT_SPEED, ultimate ? 0.6 : 0.35, multiply);
                modify(caster, Attributes.ATTACK_SPEED, ultimate ? 0.5 : 0.25, multiply);
                modify(caster, Attributes.ARMOR, ultimate ? 12 : 6, add);
            }
            case UTH_DUNA -> {
                modify(caster, Attributes.ARMOR, ultimate ? 25 : 10, add);
                modify(caster, TensuraAttributes.MAGIC_BARRIER, ultimate ? 40 : 20, add);
            }
            case NU_UDRA -> {
                modify(caster, Attributes.ATTACK_DAMAGE, ultimate ? 1 : 0.5, multiply);
                modify(caster, Attributes.ARMOR, ultimate ? 18 : 8, add);
            }
            case JIN_DAHAAD -> {
                modify(caster, Attributes.ARMOR, ultimate ? 40 : 20, add);
                modify(caster, Attributes.KNOCKBACK_RESISTANCE, 1, add);
                modify(caster, TensuraAttributes.MAGIC_BARRIER, ultimate ? 40 : 15, add);
            }
        }
    }
    private void mantleEffects(LivingEntity caster) {
        int duration = ultimate ? 600 : 300;
        if (apex == Apex.UTH_DUNA) {
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.ABSORPTION, duration, ultimate ? 7 : 3));
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WATER_BREATHING, duration));
        } else if (apex == Apex.NU_UDRA) {
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration));
        }
    }
    private void removeMantle(LivingEntity caster) {
        for (Holder<Attribute> attribute : java.util.List.of(boost(), Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED,
                Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE, TensuraAttributes.MAGIC_BARRIER)) {
            var instance = caster.getAttribute(attribute);
            if (instance != null) instance.removeModifier(mantleId());
        }
    }
    @Override public void onForgetSkill(ManasSkillInstance instance, LivingEntity caster) {
        removeMantle(caster);
        for (String key : java.util.List.of("mantle_until", "field_until", "domain_until", "ripple_until", "travel_until")) instance.getOrCreateTag().remove(key);
        instance.getOrCreateTag().remove("oil_pools");
        super.onForgetSkill(instance, caster);
    }
}
