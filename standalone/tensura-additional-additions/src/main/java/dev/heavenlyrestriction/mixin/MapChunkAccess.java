package dev.heavenlyrestriction.mixin;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The public getChunkFuture waits on the main thread; this schedules without managedBlock. */
@Mixin(ServerChunkCache.class)
public interface MapChunkAccess {
  @Invoker("getChunkFutureMainThread")
  CompletableFuture<ChunkResult<ChunkAccess>> additions$mapFuture(
      int x, int z, ChunkStatus status, boolean create);
}
