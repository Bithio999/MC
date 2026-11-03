package dev.heavenlyrestriction.highrealm;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.*;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class HighRealm {
  public static final ResourceKey<Level> LEVEL =
      ResourceKey.create(
          net.minecraft.core.registries.Registries.DIMENSION, HeavenlyRestriction.id("high_realm"));

  private record Charge(String gate, int ticks) {}

  private static final Map<UUID, Charge> CHARGES = new HashMap<>();

  public static void register(IEventBus bus, ModContainer mod) {
    mod.registerConfig(
        ModConfig.Type.SERVER, HighRealmConfig.SPEC, "tensura-additions-high-realm.toml");
    NeoForge.EVENT_BUS.addListener(HighRealm::interact);
    NeoForge.EVENT_BUS.addListener(HighRealm::tick);
    NeoForge.EVENT_BUS.addListener(HighRealm::travel);
    NeoForge.EVENT_BUS.addListener(
        (BlockEvent.BreakEvent e) -> {
          if (protectedAt(e.getLevel(), e.getPos())) {
            e.setCanceled(true);
            e.getPlayer()
                .displayClientMessage(Component.literal("The return sanctum is protected."), true);
          }
        });
    NeoForge.EVENT_BUS.addListener(
        (BlockEvent.EntityPlaceEvent e) -> {
          if (protectedAt(e.getLevel(), e.getPos())) e.setCanceled(true);
        });
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> CHARGES.clear());
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) ->
            CHARGES.remove(e.getEntity().getUUID()));
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.level.ExplosionEvent.Detonate e) ->
            e.getAffectedBlocks().removeIf(pos -> protectedAt(e.getLevel(), pos)));
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.server.ServerStartedEvent e) ->
            HighRealmConfig.upgradeLegacyDefault(RealmData.get(e.getServer())));
    HighRealmElites.register();
  }

  public static boolean enoughEP(ServerPlayer p) {
    return EnergyHelper.getMaxEP(p) >= HighRealmConfig.ENTRY_EP.get();
  }

  private static void denied(ServerPlayer p) {
    p.displayClientMessage(
        Component.literal(
            String.format(
                Locale.ROOT,
                "The High Realm requires %,.0f EP. Your actual EP: %,.0f.",
                HighRealmConfig.ENTRY_EP.get(),
                EnergyHelper.getMaxEP(p))),
        true);
  }

  private static void travel(EntityTravelToDimensionEvent e) {
    if (e.getDimension().equals(LEVEL)
        && e.getEntity() instanceof ServerPlayer p
        && !p.level().dimension().equals(LEVEL)
        && !enoughEP(p)) {
      e.setCanceled(true);
      denied(p);
    }
  }

  public static boolean activate(ServerPlayer p, BlockPos core, Direction.Axis axis) {
    var level = p.serverLevel();
    if (level.dimension().equals(LEVEL) || !enoughEP(p) || !GatePattern.valid(level, core, axis))
      return false;
    var d = RealmData.get(p.server);
    var g = new RealmData.Gate(level.dimension(), core.immutable(), axis);
    if (d.gates.containsKey(g.key())) return true;
    if (d.gates.size() >= 256) {
      p.displayClientMessage(Component.literal("This world already has 256 awakened gates."), true);
      return false;
    }
    d.add(g);
    level.playSound(null, core, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1, .7f);
    return true;
  }

  private static void interact(PlayerInteractEvent.RightClickBlock e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    var level = p.serverLevel();
    if (level.dimension().equals(LEVEL)) {
      var d = RealmData.get(p.server);
      if (d.sanctuary != null && e.getPos().equals(d.sanctuary)) {
        leave(p);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
      }
      return;
    }
    if (!level.getBlockState(e.getPos()).is(Blocks.LODESTONE)) return;
    var d = RealmData.get(p.server);
    for (var axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
      var g = new RealmData.Gate(level.dimension(), e.getPos(), axis);
      if (d.gates.containsKey(g.key()) && GatePattern.valid(level, e.getPos(), axis)) {
        if (!enoughEP(p)) denied(p);
        else enter(p, g);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        return;
      }
    }
    if (!e.getItemStack().is(Items.NETHER_STAR)) return;
    e.setCanceled(true);
    e.setCancellationResult(InteractionResult.SUCCESS);
    if (!enoughEP(p)) {
      denied(p);
      return;
    }
    for (var axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z})
      if (activate(p, e.getPos(), axis)) {
        if (!p.hasInfiniteMaterials()) e.getItemStack().shrink(1);
        p.displayClientMessage(
            Component.literal(
                "Gate awakened. Walk through the arch, or use its lodestone to enter."),
            true);
        return;
      }
    p.displayClientMessage(
        Component.literal(
            "Incomplete High Realm gate: 5 wide × 6 tall; four netherite corners, lodestone below,"
                + " amethyst above, twelve crying obsidian. Clear the 3 × 4 opening."),
        true);
  }

  private static boolean protectedAt(
      net.minecraft.world.level.LevelAccessor accessor, BlockPos pos) {
    if (!(accessor instanceof ServerLevel level) || !level.dimension().equals(LEVEL)) return false;
    var c = RealmData.get(level.getServer()).sanctuary;
    return c != null
        && Math.abs(pos.getX() - c.getX()) <= 5
        && Math.abs(pos.getZ() - c.getZ()) <= 5
        && pos.getY() >= c.getY() - 1
        && pos.getY() <= c.getY() + 7;
  }

  public static BlockPos sanctuary(ServerLevel level) {
    var d = RealmData.get(level.getServer());
    if (d.sanctuary == null) {
      int y =
          Math.min(
              level.getMaxBuildHeight() - 10,
              Math.max(90, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0) + 3));
      d.sanctuary = new BlockPos(0, y, 0);
      d.setDirty();
    }
    var c = d.sanctuary;
    // Reserve only the small arrival sanctum; the surrounding dimension stays editable.
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++) {
        level.setBlock(c.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState(), 2);
        level.setBlock(
            c.offset(x, 0, z),
            (Math.abs(x) == 4 || Math.abs(z) == 4
                    ? Blocks.CHISELED_QUARTZ_BLOCK
                    : Blocks.SMOOTH_QUARTZ)
                .defaultBlockState(),
            2);
        for (int y = 1; y <= 7; y++)
          level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
    for (int x = -2; x <= 2; x++)
      for (int y = 0; y <= 5; y++)
        if (Math.abs(x) == 2 || y == 0 || y == 5)
          level.setBlock(
              c.offset(x, y, 0),
              (y == 0
                      ? Blocks.BEDROCK
                      : y == 5
                          ? (x == 0 ? Blocks.AMETHYST_BLOCK : Blocks.CHISELED_QUARTZ_BLOCK)
                          : Blocks.QUARTZ_PILLAR)
                  .defaultBlockState(),
              2);
    for (int x : new int[] {-4, 4})
      for (int z : new int[] {-4, 4}) {
        level.setBlock(c.offset(x, 0, z), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
        for (int y = 1; y <= 3; y++)
          level.setBlock(c.offset(x, y, z), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
        level.setBlock(c.offset(x, 4, z), Blocks.END_ROD.defaultBlockState(), 2);
      }
    level.setDefaultSpawnPos(c.offset(0, 1, 3), 180);
    return c;
  }

  public static boolean enter(ServerPlayer p, RealmData.Gate gate) {
    if (!enoughEP(p)) {
      denied(p);
      return false;
    }
    var realm = p.server.getLevel(LEVEL);
    if (realm == null) {
      p.displayClientMessage(
          Component.literal(
              "The High Realm is unavailable; its dimension datapack must be enabled."),
          true);
      return false;
    }
    if (!p.level().dimension().equals(gate.dimension())
        || !GatePattern.valid(p.serverLevel(), gate.core(), gate.axis())) return false;
    var d = RealmData.get(p.server);
    var normal = gate.axis() == Direction.Axis.X ? new Vec3(0, 0, 3) : new Vec3(3, 0, 0);
    d.homes.put(
        p.getUUID(),
        new RealmData.Home(
            p.level().dimension(),
            Vec3.atBottomCenterOf(gate.core().above()).add(normal),
            p.getYRot(),
            p.getXRot()));
    d.setDirty();
    var c = sanctuary(realm);
    p.stopRiding();
    var moved =
        p.changeDimension(
            new DimensionTransition(
                realm,
                Vec3.atBottomCenterOf(c.offset(0, 1, 3)),
                Vec3.ZERO,
                180,
                0,
                DimensionTransition.PLAY_PORTAL_SOUND.then(
                    DimensionTransition.PLACE_PORTAL_TICKET)));
    if (moved == null) return false;
    p.setPortalCooldown();
    p.fallDistance = 0;
    p.displayClientMessage(
        Component.literal(
            "The High Realm. Return through the return arch, or use its base. Leaving needs no"
                + " EP."),
        false);
    return true;
  }

  public static boolean leave(ServerPlayer p) {
    var d = RealmData.get(p.server);
    var home = d.homes.get(p.getUUID());
    ServerLevel level = home == null ? null : p.server.getLevel(home.dimension());
    Vec3 landing = level == null ? null : safeLanding(level, home.pos(), p);
    DimensionTransition destination;
    if (landing != null)
      destination =
          new DimensionTransition(
              level,
              landing,
              Vec3.ZERO,
              home.yaw(),
              home.pitch(),
              DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    else
      destination =
          new DimensionTransition(
              p.server.overworld(),
              p,
              DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    p.stopRiding();
    var moved = p.changeDimension(destination);
    if (moved == null) return false;
    p.setPortalCooldown();
    p.fallDistance = 0;
    return true;
  }

  public static Vec3 safeLanding(ServerLevel level, Vec3 desired, ServerPlayer player) {
    var origin = BlockPos.containing(desired);
    for (int r = 0; r <= 5; r++)
      for (int x = -r; x <= r; x++)
        for (int z = -r; z <= r; z++) {
          if (r > 0 && Math.abs(x) != r && Math.abs(z) != r) continue;
          for (int dy : new int[] {0, 1, 2, -1, 3}) {
            var at = origin.offset(x, dy, z);
            if (at.getY() < level.getMinBuildHeight() + 1
                || at.getY() >= level.getMaxBuildHeight() - 3
                || !level.getWorldBorder().isWithinBounds(at)) continue;
            level.getChunkAt(at);
            var floor = level.getBlockState(at.below());
            if (floor.getCollisionShape(level, at.below()).isEmpty()
                || !floor.getFluidState().isEmpty()
                || floor.is(Blocks.MAGMA_BLOCK)
                || floor.is(Blocks.CACTUS)
                || floor.is(Blocks.CAMPFIRE)
                || floor.is(Blocks.SOUL_CAMPFIRE)) continue;
            var v = Vec3.atBottomCenterOf(at);
            if (level.getBlockState(at).getFluidState().isEmpty()
                && level.getBlockState(at.above()).getFluidState().isEmpty()
                && level.getBlockState(at).isAir()
                && level.noCollision(
                    player, player.getBoundingBox().move(v.subtract(player.position())))) return v;
          }
        }
    return null;
  }

  private static void particles(ServerLevel level, RealmData.Gate g) {
    var random = level.random;
    for (int n = 0; n < 24; n++) {
      double x = (random.nextDouble() - .5) * 2.8, y = 1 + random.nextDouble() * 3.8;
      var p = GatePattern.at(g.core(), g.axis(), 0, 0);
      level.sendParticles(
          ParticleTypes.PORTAL,
          p.getX() + .5 + (g.axis() == Direction.Axis.X ? x : 0),
          p.getY() + y,
          p.getZ() + .5 + (g.axis() == Direction.Axis.Z ? x : 0),
          1,
          .02,
          .02,
          .02,
          .015);
    }
    level.sendParticles(
        ParticleTypes.END_ROD,
        g.core().getX() + .5,
        g.core().getY() + 3,
        g.core().getZ() + .5,
        2,
        .8,
        1.6,
        .8,
        .005);
  }

  private static void tick(ServerTickEvent.Post e) {
    if (e.getServer().getTickCount() % 5 != 0) return;
    var s = e.getServer();
    var d = RealmData.get(s);
    var gates = List.copyOf(d.gates.values());
    var emitted = new HashSet<String>();
    for (var p : s.getPlayerList().getPlayers()) {
      if (!p.isAlive() || p.isSpectator()) {
        CHARGES.remove(p.getUUID());
        continue;
      }
      if (p.level().dimension().equals(LEVEL)) {
        // Command travel and fresh World Hop arrivals also need a usable way home.
        var c = d.sanctuary == null ? sanctuary(p.serverLevel()) : d.sanctuary;
        if (c != null) {
          var g = new RealmData.Gate(LEVEL, c, Direction.Axis.X);
          if (p.distanceToSqr(Vec3.atCenterOf(c)) < 1600
              && s.getTickCount() % 10 == 0
              && emitted.add(g.key())) particles(p.serverLevel(), g);
          if (p.getY() < p.level().getMinBuildHeight() + 8) {
            var at = sanctuary(p.serverLevel());
            p.teleportTo(at.getX() + .5, at.getY() + 1, at.getZ() + 3.5);
            p.fallDistance = 0;
          }
          if (!p.isOnPortalCooldown()
              && GatePattern.interior(c, Direction.Axis.X).intersects(p.getBoundingBox())) leave(p);
        }
        continue;
      }
      RealmData.Gate touching = null;
      for (var g : gates)
        if (g.dimension().equals(p.level().dimension())
            && p.distanceToSqr(Vec3.atCenterOf(g.core())) < 1600) {
          if (!GatePattern.loaded(p.serverLevel(), g.core(), g.axis())) continue;
          if (!GatePattern.valid(p.serverLevel(), g.core(), g.axis())) {
            d.gates.remove(g.key());
            d.setDirty();
            continue;
          }
          if (s.getTickCount() % 10 == 0 && emitted.add(g.key())) particles(p.serverLevel(), g);
          if (GatePattern.interior(g.core(), g.axis()).intersects(p.getBoundingBox())) touching = g;
        }
      if (p.isOnPortalCooldown()) {
        CHARGES.remove(p.getUUID());
        continue;
      }
      if (touching == null) {
        CHARGES.remove(p.getUUID());
        continue;
      }
      if (!enoughEP(p)) {
        CHARGES.remove(p.getUUID());
        if (s.getTickCount() % 40 == 0) denied(p);
        continue;
      }
      var last = CHARGES.get(p.getUUID());
      int ticks = last != null && last.gate.equals(touching.key()) ? last.ticks + 5 : 5;
      CHARGES.put(p.getUUID(), new Charge(touching.key(), ticks));
      if (ticks >= HighRealmConfig.CHARGE_TICKS.get()) {
        CHARGES.remove(p.getUUID());
        enter(p, touching);
      } else p.displayClientMessage(Component.literal("Opening The High Realm…"), true);
    }
  }

  private HighRealm() {}
}
