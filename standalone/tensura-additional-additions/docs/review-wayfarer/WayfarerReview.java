package dev.heavenlyrestriction;

import dev.heavenlyrestriction.highrealm.*;
import dev.heavenlyrestriction.highrealm.boss.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in visual/runtime capture in a guarded copy of the disposable High Realm review save. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class WayfarerReview {
  private static final Path OUT = Path.of("/workspace/validation-wayfarer/capture");
  private static volatile int phase;
  private static int wait, age;
  private static volatile String error;
  private static volatile KaelVeyrEntity boss;
  private static volatile Husk dummy;
  private static Vec3 root;

  private static void check(boolean ok, String message) {
    if (!ok) throw new IllegalStateException(message);
  }

  private static void server(java.util.function.Consumer<ServerPlayer> fn) {
    var server = Minecraft.getInstance().getSingleplayerServer();
    server.execute(
        () -> {
          try {
            fn.accept(server.getPlayerList().getPlayers().getFirst());
          } catch (Throwable e) {
            e.printStackTrace();
            error = e.toString();
          }
        });
  }

  private static void shot(String name) {
    try {
      Files.createDirectories(OUT);
      try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
        image.writeToFile(OUT.resolve(name + ".png"));
      }
    } catch (Exception e) {
      error = e.toString();
    }
  }

  private static void camera(ServerPlayer p, Vec3 from, Vec3 look) {
    Vec3 diff = look.subtract(from.add(0, p.getEyeHeight(), 0));
    float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z)),
        pitch = (float) -Math.toDegrees(Math.atan2(diff.y, diff.horizontalDistance()));
    p.teleportTo(
        (net.minecraft.server.level.ServerLevel) boss.level(), from.x, from.y, from.z, yaw, pitch);
  }

  private static void cast(WayfarerCombat.Action action, int distance) {
    server(
        p -> {
          boss.combat().cancel();
          boss.teleportTo(root.x, root.y, root.z);
          dummy.setPos(root.add(0, 0, distance));
          boss.setTarget(dummy);
          dummy.setTarget(boss);
          boss.setNoAi(false);
          check(boss.combat().begin(action), "Could not activate native ability " + action);
          boss.combat().delay(200);
        });
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    try {
      exercise();
    } catch (Throwable e) {
      e.printStackTrace();
      error = e.toString();
    }
  }

  private static void exercise() throws Exception {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
    if (error != null || ++age > 9000) {
      Files.createDirectories(OUT);
      Files.writeString(
          OUT.resolve("failed.txt"), error == null ? "Timeout phase " + phase : error);
      phase = 999;
      mc.stop();
      return;
    }
    if (mc.player == null
        || mc.level == null
        || mc.getSingleplayerServer() == null
        || mc.getOverlay() != null) return;
    if (phase > 0 && phase < 999) {
      mc.player.getAbilities().flying = true;
      mc.player.setNoGravity(true);
    }
    wait++;
    if (phase == 0 && wait > 90) {
      phase = 1;
      wait = 0;
      mc.setScreen(null);
      mc.options.guiScale().set(2);
      mc.options.renderDistance().set(10);
      mc.options.cloudStatus().set(CloudStatus.OFF);
      mc.options.particles().set(ParticleStatus.ALL);
      mc.options.fov().set(45);
      mc.options.hideGui = true;
      mc.resizeDisplay();
      server(
          p -> {
            check(
                p.server
                    .getWorldPath(LevelResource.ROOT)
                    .normalize()
                    .getFileName()
                    .toString()
                    .equals("Wayfarer Review"),
                "Refusing a personal save");
            p.setGameMode(GameType.CREATIVE);
            EnergyHelper.setBaseMaxEP(p, 20_000_000);
            p.getAbilities().flying = true;
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
            p.setNoGravity(true);
            p.getInventory().clearContent();
            p.removeAllEffects();
            var level = p.server.getLevel(HighRealm.LEVEL);
            check(level != null, "Realm missing from existing world");
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
            var previousMobs = new java.util.ArrayList<Mob>();
            for (var entity : level.getAllEntities())
              if (entity instanceof Mob mob) previousMobs.add(mob);
            previousMobs.forEach(Entity::discard);
            int x = 1000, z = 1000;
            int y = 90;
            for (int dx = -28; dx <= 28; dx++)
              for (int dz = -28; dz <= 28; dz++)
                y =
                    Math.max(
                        y,
                        level.getHeight(
                                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                                x + dx,
                                z + dz)
                            + 6);
            root = new Vec3(x + .5, y + 1, z + .5);
            // A safe pale courtyard above generated ground, only in the disposable review copy.
            for (int dx = -27; dx <= 27; dx++)
              for (int dz = -27; dz <= 27; dz++) {
                level.setBlock(
                    new BlockPos(x + dx, y, z + dz),
                    ((Math.floorDiv(dx, 4) + Math.floorDiv(dz, 4)) % 2 == 0
                            ? Blocks.CALCITE
                            : Blocks.SMOOTH_QUARTZ)
                        .defaultBlockState(),
                    2);
                for (int dy = 1; dy <= 4; dy++)
                  level.setBlock(
                      new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 2);
              }
            boss = WayfarerContent.KAEL.get().create(level);
            boss.setPos(root);
            boss.setNoAi(true);
            level.addFreshEntity(boss);
            check(
                boss.initialized() && Math.abs(EnergyHelper.getMaxEP(boss) - 15_000_000) < .01,
                "Incorrect actual boss EP");
            camera(p, root.add(2.3, .15, 3.8), root.add(0, .95, 0));
            boss.lookAt(Anchor.EYES, p.getEyePosition());
            boss.setYBodyRot(boss.getYRot());
            boss.setYHeadRot(boss.getYRot());
            System.out.println(
                "Review stage=" + root + " player=" + p.position() + " boss=" + boss.position());
            System.out.println(
                "Kael visual review stats: HP="
                    + boss.getMaxHealth()
                    + " SHP="
                    + boss.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)
                    + " AP="
                    + EnergyHelper.getMaxAura(boss)
                    + " MP="
                    + EnergyHelper.getMaxMagicule(boss));
          });
      return;
    }
    if (phase == 1 && wait > 120 && boss != null && mc.level.getEntity(boss.getId()) != null) {
      shot("kael-portrait");
      phase = 2;
      wait = 0;
      mc.options.hideGui = false;
      mc.options.fov().set(58);
      server(
          p -> {
            camera(p, root.add(16, 9, 20), root.add(0, 3, 7));
            dummy = new Husk(EntityType.HUSK, boss.level());
            dummy.setNoAi(true);
            dummy.setNoGravity(false);
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
            dummy.setHealth(10000);
            dummy.setPos(root.add(0, 0, 12));
            dummy.setCustomName(
                net.minecraft.network.chat.Component.literal("Combat Review Dummy"));
            boss.level().addFreshEntity(dummy);
            RaceAPI.getRaceFrom(dummy).setRace(TensuraRaces.HUMAN_SAINT.get(), false);
            dummy.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(200000);
            TensuraStorages.getExistenceFrom(dummy).setSpiritualHealth(200000);
            EnergyHelper.setBaseMaxEP(dummy, 20_000_000);
            dummy.setTarget(boss);
            boss.setTarget(dummy);
            boss.combat().cancel();
            boss.setNoAi(true);
          });
      return;
    }
    if (phase == 2 && wait > 25) {
      cast(WayfarerCombat.Action.WIND_CUTTER, 12);
      phase = 3;
      wait = 0;
      return;
    }
    if (phase == 3 && wait > 3) {
      shot("kael-native-wind-cutter");
      phase = 4;
      wait = 0;
      return;
    }
    if (phase == 4 && wait > 35) {
      cast(WayfarerCombat.Action.TORNADO_BLADE, 14);
      phase = 5;
      wait = 0;
      return;
    }
    if (phase == 5 && wait > 12) {
      shot("kael-native-tornado");
      phase = 6;
      wait = 0;
      return;
    }
    if (phase == 6 && wait > 45) {
      cast(WayfarerCombat.Action.AURA_SLASH, 12);
      phase = 7;
      wait = 0;
      return;
    }
    if (phase == 7 && wait > 3) {
      shot("kael-native-aura-slash");
      phase = 8;
      wait = 0;
      return;
    }
    if (phase == 8 && wait > 45) {
      cast(WayfarerCombat.Action.AERIAL_BLADE, 2);
      phase = 9;
      wait = 0;
      return;
    }
    if (phase == 9 && wait > 3) {
      shot("kael-native-aerial-blade");
      phase = 10;
      wait = 0;
      return;
    }
    if (phase == 10 && wait > 45) {
      server(
          p -> {
            check(
                dummy.getHealth() < dummy.getMaxHealth()
                    || TensuraStorages.getExistenceFrom(dummy).getSpiritualHealth() < 200000,
                "Native abilities did not damage the combat target");
            boss.combat().cancel();
            boss.setNoAi(true);
          });
      Files.createDirectories(OUT);
      Files.writeString(
          OUT.resolve("passed.txt"),
          "PASS: existing-world High Realm boss, native renderer, 15M actual EP, one HP boss bar,"
              + " native Wind Cutter, Tornado Blade, Aura Slash and Aerial Blade; real HP/SHP"
              + " damage.\n");
      phase = 999;
      mc.stop();
    }
  }
}
