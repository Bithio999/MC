package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.splitmind.AutoCastCombat;
import dev.heavenlyrestriction.splitmind.AutoCastWindowTarget;
import java.util.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Independent native hurt windows per automated spell, with no global timer reset. */
@Mixin(LivingEntity.class)
public abstract class AutoCastHurtMixin implements AutoCastWindowTarget {
  @Unique
  private final Map<AutoCastCombat.Origin, AutoCastCombat.Window> additions$windows =
      new LinkedHashMap<>(16, .75f, true);

  @Unique private boolean additions$defensiveWindow;

  @Unique
  private void additions$observeDefense() {
    int time = ((LivingEntity) (Object) this).invulnerableTime;
    if (time > 20) additions$defensiveWindow = true;
    else if (time <= 10) additions$defensiveWindow = false;
  }

  @Inject(method = "baseTick", at = @At("HEAD"))
  private void additions$defenseTick(CallbackInfo ci) {
    additions$observeDefense();
  }

  @Override
  public AutoCastCombat.Window additions$autoWindow(AutoCastCombat.Origin key) {
    additions$observeDefense();
    if (additions$defensiveWindow) return null;
    var window = additions$windows.get(key);
    if (window == null) {
      if (additions$windows.size() >= 64)
        additions$windows.remove(additions$windows.keySet().iterator().next());
      window = new AutoCastCombat.Window();
      additions$windows.put(key, window);
    }
    return window;
  }

  @WrapOperation(
      method = "hurt",
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I",
              opcode = Opcodes.GETFIELD))
  private int additions$readTime(
      LivingEntity target,
      Operation<Integer> original,
      @Local(argsOnly = true) DamageSource source) {
    var window = AutoCastCombat.window(target, source);
    return window == null ? original.call(target) : window.remaining(target);
  }

  @WrapOperation(
      method = "hurt",
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I",
              opcode = Opcodes.PUTFIELD))
  private void additions$writeTime(
      LivingEntity target,
      int value,
      Operation<Void> original,
      @Local(argsOnly = true) DamageSource source) {
    var window = AutoCastCombat.window(target, source);
    if (window == null) original.call(target, value);
    else window.duration(target, value);
  }

  @WrapOperation(
      method = "hurt",
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/world/entity/LivingEntity;lastHurt:F",
              opcode = Opcodes.GETFIELD))
  private float additions$readDamage(
      LivingEntity target, Operation<Float> original, @Local(argsOnly = true) DamageSource source) {
    var window = AutoCastCombat.window(target, source);
    return window == null ? original.call(target) : window.damage();
  }

  @WrapOperation(
      method = "hurt",
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/world/entity/LivingEntity;lastHurt:F",
              opcode = Opcodes.PUTFIELD))
  private void additions$writeDamage(
      LivingEntity target,
      float value,
      Operation<Void> original,
      @Local(argsOnly = true) DamageSource source) {
    var window = AutoCastCombat.window(target, source);
    if (window == null) original.call(target, value);
    else window.damage(value);
  }
}
