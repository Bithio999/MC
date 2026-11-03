package dev.heavenlyrestriction.highrealm;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

public final class GatePattern {
  public static BlockPos at(BlockPos core, Direction.Axis axis, int across, int up) {
    return core.offset(
        axis == Direction.Axis.X ? across : 0, up, axis == Direction.Axis.Z ? across : 0);
  }

  public static Block expected(int x, int y) {
    if (Math.abs(x) == 2 && (y == 0 || y == 5)) return Blocks.NETHERITE_BLOCK;
    if (x == 0 && y == 0) return Blocks.LODESTONE;
    if (x == 0 && y == 5) return Blocks.AMETHYST_BLOCK;
    return Blocks.CRYING_OBSIDIAN;
  }

  public static boolean loaded(ServerLevel level, BlockPos core, Direction.Axis axis) {
    return level.hasChunkAt(at(core, axis, -2, 0)) && level.hasChunkAt(at(core, axis, 2, 0));
  }

  public static boolean valid(ServerLevel level, BlockPos core, Direction.Axis axis) {
    if (axis == Direction.Axis.Y) return false;
    for (int x = -2; x <= 2; x++)
      for (int y = 0; y <= 5; y++) {
        var p = at(core, axis, x, y);
        if (!level.hasChunkAt(p)) return false;
        var state = level.getBlockState(p);
        if (Math.abs(x) == 2 || y == 0 || y == 5) {
          if (!state.is(expected(x, y))) return false;
        } else if (!state.getCollisionShape(level, p).isEmpty() || !state.getFluidState().isEmpty())
          return false;
      }
    return true;
  }

  public static AABB interior(BlockPos core, Direction.Axis axis) {
    return AABB.encapsulatingFullBlocks(at(core, axis, -1, 1), at(core, axis, 1, 4))
        .inflate(axis == Direction.Axis.Z ? .65 : 0, 0, axis == Direction.Axis.X ? .65 : 0);
  }

  private GatePattern() {}
}
