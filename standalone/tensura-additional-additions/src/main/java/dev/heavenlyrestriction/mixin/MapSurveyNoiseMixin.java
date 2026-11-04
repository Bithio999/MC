package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.cartography.SurveyNoiseCache;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MapSurveyNoiseMixin {
  @Inject(method = "getBaseHeight", at = @At("HEAD"), cancellable = true)
  private void cachedHeight(
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor height,
      RandomState random,
      CallbackInfoReturnable<Integer> cir) {
    var value = SurveyNoiseCache.height(this, x, z, type, height, random);
    if (value != null) cir.setReturnValue(value);
  }

  @Inject(method = "getBaseHeight", at = @At("RETURN"))
  private void saveHeight(
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor height,
      RandomState random,
      CallbackInfoReturnable<Integer> cir) {
    SurveyNoiseCache.height(this, x, z, type, height, random, cir.getReturnValue());
  }

  @Inject(method = "getBaseColumn", at = @At("HEAD"), cancellable = true)
  private void cachedColumn(
      int x,
      int z,
      LevelHeightAccessor height,
      RandomState random,
      CallbackInfoReturnable<NoiseColumn> cir) {
    var value = SurveyNoiseCache.column(this, x, z, height, random);
    if (value != null) cir.setReturnValue(value);
  }

  @Inject(method = "getBaseColumn", at = @At("RETURN"))
  private void saveColumn(
      int x,
      int z,
      LevelHeightAccessor height,
      RandomState random,
      CallbackInfoReturnable<NoiseColumn> cir) {
    SurveyNoiseCache.column(
        (NoiseBasedChunkGenerator) (Object) this, x, z, height, random, cir.getReturnValue());
  }
}
