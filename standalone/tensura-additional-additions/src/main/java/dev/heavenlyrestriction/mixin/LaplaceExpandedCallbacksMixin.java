package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.laplace.expand.ExpandedRuntime;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ManasSkillInstance.class, remap = false)
public abstract class LaplaceExpandedCallbacksMixin {
  @WrapMethod(method = "onPressed")
  private void laplace$onPressed(LivingEntity owner, int key, int mode, Operation<Void> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, mode)) {
      original.call(owner, key, mode);
    }
  }

  @WrapMethod(method = "onRelease")
  private void laplace$onRelease(
      LivingEntity owner, int held, int key, int mode, Operation<Void> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, mode)) {
      original.call(owner, held, key, mode);
    }
  }

  @WrapMethod(method = "onHeld")
  private boolean laplace$onHeld(
      LivingEntity owner, int held, int mode, Operation<Boolean> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, mode)) {
      return original.call(owner, held, mode);
    }
  }

  @WrapMethod(method = "onTick")
  private void laplace$onTick(LivingEntity owner, Operation<Void> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      original.call(owner);
    }
  }

  @WrapMethod(method = "onToggleOn")
  private void laplace$onToggleOn(LivingEntity owner, Operation<Void> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      original.call(owner);
    }
  }

  @WrapMethod(method = "onBeingDamaged")
  private boolean laplace$onBeingDamaged(
      LivingEntity owner,
      net.minecraft.world.damagesource.DamageSource source,
      float amount,
      Operation<Boolean> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      return original.call(owner, source, amount);
    }
  }

  @WrapMethod(method = "onDamageEntity")
  private boolean laplace$onDamageEntity(
      LivingEntity owner,
      LivingEntity target,
      net.minecraft.world.damagesource.DamageSource source,
      io.github.manasmods.manascore.network.api.util.Changeable<Float> amount,
      Operation<Boolean> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      return original.call(owner, target, source, amount);
    }
  }

  @WrapMethod(method = "onTakenDamage")
  private boolean laplace$onTakenDamage(
      LivingEntity owner,
      net.minecraft.world.damagesource.DamageSource source,
      io.github.manasmods.manascore.network.api.util.Changeable<Float> amount,
      Operation<Boolean> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      return original.call(owner, source, amount);
    }
  }

  @WrapMethod(method = "onDeath")
  private boolean laplace$death(
      LivingEntity owner,
      net.minecraft.world.damagesource.DamageSource source,
      Operation<Boolean> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      return original.call(owner, source);
    }
  }

  @WrapMethod(method = "onEffectAdded")
  private boolean laplace$effect(
      LivingEntity owner,
      net.minecraft.world.entity.Entity source,
      io.github.manasmods.manascore.network.api.util.Changeable<
              net.minecraft.world.effect.MobEffectInstance>
          effect,
      Operation<Boolean> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      return original.call(owner, source, effect);
    }
  }

  @WrapMethod(method = "onTouchEntity")
  private boolean laplace$touch(
      LivingEntity owner,
      LivingEntity target,
      net.minecraft.world.damagesource.DamageSource source,
      io.github.manasmods.manascore.network.api.util.Changeable<Float> amount,
      Operation<Boolean> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      return original.call(owner, target, source, amount);
    }
  }

  @WrapMethod(method = "getAttributeModifierAmplifier")
  private double laplace$attributes(
      LivingEntity owner,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
      io.github.manasmods.manascore.skill.api.ManasSkill.AttributeTemplate template,
      int mode,
      Operation<Double> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      return original.call(owner, attr, template, mode);
    }
  }

  @WrapMethod(method = "onProjectileHit")
  private void laplace$projectile(
      LivingEntity owner,
      net.minecraft.world.phys.EntityHitResult hit,
      net.minecraft.world.entity.projectile.Projectile shot,
      io.github.manasmods.manascore.network.api.util.Changeable<
              net.minecraft.world.entity.projectile.ProjectileDeflection>
          deflection,
      io.github.manasmods.manascore.network.api.util.Changeable<
              io.github.manasmods.manascore.skill.api.EntityEvents.ProjectileHitResult>
          result,
      Operation<Void> original) {
    try (var scope = ExpandedRuntime.open(owner, (ManasSkillInstance) (Object) this, -1)) {
      original.call(owner, hit, shot, deflection, result);
    }
  }
}
