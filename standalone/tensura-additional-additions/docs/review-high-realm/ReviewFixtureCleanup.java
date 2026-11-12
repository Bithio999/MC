package dev.heavenlyrestriction;

import net.minecraft.gametest.framework.GameTestServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Release only the disposable native test server's forced fixture chunks before bulk teardown. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID)
public final class ReviewFixtureCleanup {
  @SubscribeEvent
  public static void stopping(ServerStoppingEvent e) {
    if (!(e.getServer() instanceof GameTestServer)) return;
    int actors = 0;
    for (var level : e.getServer().getAllLevels())
      for (var player : java.util.List.copyOf(level.players())) {
        if (e.getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
          level.removePlayerImmediately(
              player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
          actors++;
        }
      }
    System.out.println("Removed " + actors + " orphaned test player actors.");
    int released = 0;
    for (var level : e.getServer().getAllLevels())
      for (long packed : level.getForcedChunks().toLongArray()) {
        var pos = new net.minecraft.world.level.ChunkPos(packed);
        level.setChunkForced(pos.x, pos.z, false);
        released++;
      }
    System.out.println("Released " + released + " disposable test chunk tickets.");
  }
}
