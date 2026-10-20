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
import net.minecraft.world.phys.Vec3;

/** Two active arts, a native skill toggle, and an Ultimate-only third active. */
public class ApexSkill extends Skill {
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
    @Override public int getModes(ManasSkillInstance instance) { return ultimate ? 3 : 2; }
    @Override public String getModeId(ManasSkillInstance instance, int mode) { return (ultimate ? apex.ultimateId : apex.uniqueId) + ".mode_" + mode; }
    @Override public Component getModeName(ManasSkillInstance instance, int mode) {
        return Component.translatableWithFallback(key() + ".mode_" + mode, modeName(mode));
    }
    @Override public int nextMode(LivingEntity entity, ManasSkillInstance instance, int current, boolean reverse) {
        return Math.floorMod(current + (reverse ? -1 : 1), getModes(instance));
    }
    public String modeName(int mode) {
        if (mode == 2) return switch (apex) {
            case REY_DAU -> "Heaven's Thunder"; case UTH_DUNA -> "Crimson Ocean";
            case NU_UDRA -> "Black Sun Caldera"; case JIN_DAHAAD -> "Heavenfall Glacier";
        };
        return switch (apex) {
            case REY_DAU -> mode == 0 ? "Railbolt" : "Thunder Step";
            case UTH_DUNA -> mode == 0 ? "Crushing Current" : "Scarlet Undertow";
            case NU_UDRA -> mode == 0 ? "Oil Lash" : "Blackflame Ignition";
            case JIN_DAHAAD -> mode == 0 ? "Avalanche Crash" : "Glacial Prison";
        };
    }
    public String passiveName() {
        return switch (apex) {
            case REY_DAU -> "Storm Mantle"; case UTH_DUNA -> "Water Veil";
            case NU_UDRA -> "Furnace Mantle"; case JIN_DAHAAD -> "Colossus Carapace";
        };
    }
    public String description() {
        return switch (apex) {
            case REY_DAU -> "Railbolt ricochets between enemies within twelve blocks through walls without revisiting a target. The initial shot needs a clear path. Thunder Step travels 16 blocks (24 Ultimate) along your aim, preserving momentum through a clear 3D path and shocks enemies along it. Toggle Storm Mantle for movement, attack speed and lightning power.";
            case UTH_DUNA -> "A travelling tidal wave carries and drenches enemies. A whirlpool placed at your crosshair within 24 blocks (36 Ultimate) gathers them. Toggle Water Veil for native physical, magic and HUD multilayer barriers and retaliation after health damage.";
            case NU_UDRA -> "Rendered tentacles lash and pull enemies, leaving owned oil pools. Ignition burns those locations directly, without oil stacks. Toggle Furnace Mantle for melee and flame power.";
            case JIN_DAHAAD -> "Avalanche Crash erupts at your target and launches nearby enemies. Glacial Prison captures one target in native Frost. Toggle Colossus Carapace for physical, magic and HUD multilayer barriers and stability.";
        } + (ultimate ? " Ultimate: triple base damage and a third signature art." : " Damage improves with mastery, never with EP. Master Unique skills to unlock combination Ultimates through Synthesis.");
    }
    @Override public MutableComponent getSkillDescription() { return Component.translatableWithFallback(key() + ".description", description()); }
    @Override public double getDefaultAcquiringMagiculeCost() { return ultimate ? 1_000_000 : 25_000; }
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }
    @Override public double getAuraCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return balance.aura(mode, ultimate); }
    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }
    // ManasCore ticks every five seconds; the event handler owns one-second upkeep.
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return false; }
    @Override public void onTick(ManasSkillInstance instance, LivingEntity entity) {}
    @Override public void onLearnSkill(ManasSkillInstance instance, LivingEntity caster) {
        super.onLearnSkill(instance, caster);
        instance.getOrCreateTag().putInt("combat_revision", 5); instance.markDirty();
    }
    @Override public void onToggleOn(ManasSkillInstance instance, LivingEntity caster) {
        if (caster.level().isClientSide()) return;
        if (!canInteractSkill(instance, caster) || !canActivateSkill(instance, caster, 0)
                || EnergyHelper.isOutOfEnergy(caster, balance.upkeep(ultimate), 0)) {
            onToggleOff(instance, caster); return;
        }
        instance.setToggled(true);
        instance.getOrCreateTag().putLong("passive_next", caster.level().getGameTime() + 20);
        restorePassive(instance, caster);
    }
    @Override public void onToggleOff(ManasSkillInstance instance, LivingEntity caster) {
        if (caster.level().isClientSide()) return;
        instance.setToggled(false); removePassive(caster);
        instance.getOrCreateTag().remove("passive_next");
        instance.getOrCreateTag().remove("hud_barrier_initialized"); instance.markDirty();
    }
    void restorePassive(ManasSkillInstance instance, LivingEntity caster) {
        if (!instance.isToggled()) return;
        applyPassive(caster);
        if (barrierCapacity() > 0) {
            var barrier = caster.getAttribute(TensuraAttributes.MULTILAYER_BARRIER);
            if (barrier != null && (caster.level().getGameTime() >= instance.getOrCreateTag().getLong("barrier_ready")
                    && (barrier.getModifier(passiveId()) == null || !instance.getOrCreateTag().getBoolean("hud_barrier_initialized")))) {
                // The native HUD and damage handler both read this attribute.
                // Permanent modifiers persist a surviving barrier across relogs;
                // native breaks remove it. Rebuild only after ten seconds without a hit.
                barrier.addOrReplacePermanentModifier(new AttributeModifier(passiveId(), barrierCapacity(), AttributeModifier.Operation.ADD_VALUE));
                instance.getOrCreateTag().putBoolean("hud_barrier_initialized", true); instance.markDirty();
            }
        }
        HunterData.of(caster).putBoolean("active_skills", true);
    }
    public double barrierCapacity() {
        return switch (apex) {
            case UTH_DUNA -> ultimate ? 250 : 100;
            case JIN_DAHAAD -> ultimate ? 350 : 150;
            default -> 0;
        };
    }
    @Override public void onPressed(ManasSkillInstance instance, LivingEntity caster, int slot, int mode) {
        if (!(caster.level() instanceof ServerLevel level) || mode < 0 || mode >= getModes(instance)) return;
        if (instance.onCoolDown(mode) || !canActivateSkill(instance, caster, mode) || EnergyHelper.isOutOfEnergy(caster, instance, mode)) return;
        switch (mode) {
            case 0 -> ApexArts.primary(this, level, instance, caster);
            case 1 -> ApexArts.secondary(this, level, instance, caster);
            case 2 -> ApexArts.domain(this, level, instance, caster);
            default -> { return; }
        }
        instance.markDirty(); HunterData.of(caster).putBoolean("active_skills", true);
        instance.setCoolDown(balance.cooldown(mode, ultimate), mode);
        addMasteryPoint(instance, caster);
        level.playSound(null, caster.blockPosition(), switch (apex) {
            case REY_DAU -> mode == 0 ? SoundEvents.TRIDENT_THUNDER.value() : SoundEvents.LIGHTNING_BOLT_THUNDER;
            case UTH_DUNA -> SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE;
            case NU_UDRA -> SoundEvents.BLAZE_SHOOT;
            case JIN_DAHAAD -> SoundEvents.GLASS_BREAK;
        }, SoundSource.PLAYERS, mode == 2 ? 1.1f : 0.75f, ultimate ? 0.7f : 1.0f);
    }
    double radius() { return ultimate ? balance.radius() + 4 : balance.radius(); }
    public boolean tickActive(ManasSkillInstance instance, LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        var tag = instance.getOrCreateTag(); long now = level.getGameTime();
        if (instance.isToggled()) {
            if (!caster.isAlive() || !canInteractSkill(instance, caster) || !canActivateSkill(instance, caster, 0)) onToggleOff(instance, caster);
            else if (now >= tag.getLong("passive_next")) {
                if (EnergyHelper.isOutOfEnergy(caster, balance.upkeep(ultimate), 0)) onToggleOff(instance, caster);
                else { restorePassive(instance, caster); tag.putLong("passive_next", now + 20); instance.markDirty(); }
            }
            if (instance.isToggled() && apex != Apex.UTH_DUNA && apex != Apex.JIN_DAHAAD && now % 8 == 0) ApexVisuals.mantle(level, apex, caster.position(), ultimate);
        }
        return ApexArts.tick(this, level, instance, caster) | instance.isToggled();
    }
    void area(ServerLevel level, ManasSkillInstance instance, LivingEntity caster, Vec3 center,
              double radius, float damage, int mode, boolean pulse) {
        for (var target : ApexArts.enemies(level, caster, center, radius)) {
            if (!strike(level, instance, caster, target, damage, mode, pulse)) continue;
            if (apex == Apex.UTH_DUNA) ApexArts.push(target, center.subtract(target.position()).multiply(1, 0, 1).normalize().scale(0.72));
        }
    }
    boolean strike(ServerLevel level, ManasSkillInstance instance, LivingEntity caster, LivingEntity target,
                   float raw, int mode, boolean pulse) {
        float damage = raw * 0.9f; var data = HunterData.of(target); long now = level.getGameTime();
        if (apex == Apex.REY_DAU && (target.isInWaterOrRain() || data.getLong("drenched_until") > now)) damage *= 1.3f;
        if (apex == Apex.UTH_DUNA && mode == 0 && data.getLong("drenched_until") > now) damage *= 1.5f;
        var type = switch (apex) {
            case REY_DAU -> TensuraDamageTypes.LIGHTNING_ELEMENTAL;
            case UTH_DUNA -> TensuraDamageTypes.WATER_ELEMENTAL;
            case NU_UDRA -> TensuraDamageTypes.FIRE_ELEMENTAL;
            case JIN_DAHAAD -> TensuraDamageTypes.ICE_ELEMENTAL;
        };
        float power = ApexBalance.power(instance.getMastery() / Math.max(1, instance.getMaxMastery()), ultimate);
        if (!target.hurt(createSource(instance, caster, type, mode), damage * power)) return false;
        switch (apex) {
            case REY_DAU -> { if (mode != 0) addEffect(target, instance, mode, caster, MobEffects.MOVEMENT_SLOWDOWN, 40, ultimate ? 3 : 1); }
            case UTH_DUNA -> {
                data.putLong("drenched_until", now + 240);
                addEffect(target, instance, mode, caster, MobEffects.MOVEMENT_SLOWDOWN, 100, ultimate ? 3 : 1);
            }
            case NU_UDRA -> {
                if (mode == 0) addEffect(target, instance, mode, caster, MobEffects.MOVEMENT_SLOWDOWN, 80, 1);
                else addEffect(target, instance, mode, caster, TensuraMobEffects.getReference(TensuraMobEffects.BLACK_BURN), 200, ultimate ? 2 : 0);
            }
            case JIN_DAHAAD -> addEffect(target, instance, mode, caster, TensuraMobEffects.getReference(TensuraMobEffects.CHILL), 120, ultimate ? 2 : 0);
        }
        ApexVisuals.impact(level, apex, target.position().add(0, target.getBbHeight() * 0.5, 0), ultimate);
        return true;
    }
    private Holder<Attribute> boost() {
        return switch (apex) {
            case REY_DAU -> TensuraAttributes.LIGHTNING_BOOST;
            case UTH_DUNA, JIN_DAHAAD -> TensuraAttributes.WATER_BOOST;
            case NU_UDRA -> TensuraAttributes.FLAME_BOOST;
        };
    }
    // Keep the old modifier ID so an upgrade can remove every legacy contribution.
    private ResourceLocation passiveId() { return WildHunt.id((ultimate ? apex.ultimateId : apex.uniqueId) + "_mantle"); }
    private void modify(LivingEntity caster, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        var value = caster.getAttribute(attribute);
        if (value != null) value.addOrUpdateTransientModifier(new AttributeModifier(passiveId(), amount, operation));
    }
    private void applyPassive(LivingEntity caster) {
        var add = AttributeModifier.Operation.ADD_VALUE; var multiply = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        modify(caster, boost(), ultimate ? 1 : 0.5, multiply);
        switch (apex) {
            case REY_DAU -> {
                modify(caster, Attributes.MOVEMENT_SPEED, ultimate ? 0.6 : 0.35, multiply);
                modify(caster, Attributes.ATTACK_SPEED, ultimate ? 0.5 : 0.25, multiply);
                modify(caster, Attributes.ARMOR, ultimate ? 12 : 6, add);
            }
            case UTH_DUNA -> {
                modify(caster, Attributes.ARMOR, ultimate ? 25 : 10, add);
                modify(caster, TensuraAttributes.PHYSICAL_BARRIER, ultimate ? 36 : 18, add);
                modify(caster, TensuraAttributes.MAGIC_BARRIER, ultimate ? 36 : 18, add);
            }
            case NU_UDRA -> {
                modify(caster, Attributes.ATTACK_DAMAGE, ultimate ? 1 : 0.5, multiply);
                modify(caster, Attributes.ARMOR, ultimate ? 18 : 8, add);
            }
            case JIN_DAHAAD -> {
                modify(caster, Attributes.ARMOR, ultimate ? 40 : 20, add);
                modify(caster, Attributes.KNOCKBACK_RESISTANCE, 1, add);
                modify(caster, TensuraAttributes.PHYSICAL_BARRIER, ultimate ? 72 : 36, add);
                modify(caster, TensuraAttributes.MAGIC_BARRIER, ultimate ? 36 : 13.5, add);
            }
        }
    }
    void removePassive(LivingEntity caster) {
        for (Holder<Attribute> attribute : java.util.List.of(boost(), Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED,
                Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE,
                TensuraAttributes.MAGIC_BARRIER, TensuraAttributes.PHYSICAL_BARRIER, TensuraAttributes.MULTILAYER_BARRIER)) {
            var value = caster.getAttribute(attribute);
            if (value != null) value.removeModifier(passiveId());
        }
    }
    @Override public void onForgetSkill(ManasSkillInstance instance, LivingEntity caster) {
        onToggleOff(instance, caster);ApexArts.clearPoolVisuals(instance,caster);
        for (String key : java.util.List.of("field_until", "domain_until", "travel_until", "prison_until", "oil_pools", "icefalls")) instance.getOrCreateTag().remove(key);
        super.onForgetSkill(instance, caster);
    }
}
