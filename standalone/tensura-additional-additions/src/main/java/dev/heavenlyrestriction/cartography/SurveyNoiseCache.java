package dev.heavenlyrestriction.cartography;

import java.util.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.*;

/**
 * Per-survey memoization of deterministic native samples. Never active during normal generation.
 */
public final class SurveyNoiseCache implements AutoCloseable {
  private static final ThreadLocal<SurveyNoiseCache> CURRENT = new ThreadLocal<>();

  private record Key(
      Object generator,
      RandomState random,
      int x,
      int z,
      Heightmap.Types type,
      int min,
      int height) {}

  private record Column(int min, BlockState[] states) {
    NoiseColumn copy() {
      return new NoiseColumn(min, states.clone());
    }
  }

  private final SurveyNoiseCache previous;
  private final Map<Key, Integer> heights = bounded(8192);
  private final Map<Key, Column> columns = bounded(256);
  public int heightHits, columnHits;

  private SurveyNoiseCache() {
    previous = CURRENT.get();
    CURRENT.set(this);
  }

  public static SurveyNoiseCache open() {
    return new SurveyNoiseCache();
  }

  private static <K, V> Map<K, V> bounded(int max) {
    return new LinkedHashMap<>(64, .75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > max;
      }
    };
  }

  private static Key key(
      Object generator,
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor height,
      RandomState random) {
    return new Key(generator, random, x, z, type, height.getMinBuildHeight(), height.getHeight());
  }

  public static Integer height(
      Object generator,
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor height,
      RandomState random) {
    var cache = CURRENT.get();
    if (cache == null) return null;
    var value = cache.heights.get(key(generator, x, z, type, height, random));
    if (value != null) cache.heightHits++;
    return value;
  }

  public static void height(
      Object generator,
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor height,
      RandomState random,
      int value) {
    var cache = CURRENT.get();
    if (cache != null) cache.heights.put(key(generator, x, z, type, height, random), value);
  }

  public static NoiseColumn column(
      Object generator, int x, int z, LevelHeightAccessor height, RandomState random) {
    var cache = CURRENT.get();
    if (cache == null) return null;
    var value = cache.columns.get(key(generator, x, z, null, height, random));
    if (value == null) return null;
    cache.columnHits++;
    return value.copy();
  }

  public static void column(
      NoiseBasedChunkGenerator generator,
      int x,
      int z,
      LevelHeightAccessor height,
      RandomState random,
      NoiseColumn value) {
    var cache = CURRENT.get();
    if (cache == null || value == null) return;
    var settings =
        generator.generatorSettings().value().noiseSettings().clampToHeightAccessor(height);
    var states = new BlockState[settings.height()];
    for (int y = 0; y < states.length; y++) states[y] = value.getBlock(settings.minY() + y);
    cache.columns.put(
        key(generator, x, z, null, height, random), new Column(settings.minY(), states));
  }

  @Override
  public void close() {
    if (previous == null) CURRENT.remove();
    else CURRENT.set(previous);
  }
}
