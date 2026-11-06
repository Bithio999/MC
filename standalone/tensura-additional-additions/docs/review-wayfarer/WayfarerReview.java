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
  private static volatile int vesperWind, vesperCuts;
  private static java.util.function.Consumer<
          net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent>
      vesperObserver;

  private static final class VesperScreen extends net.minecraft.client.gui.screens.Screen {
    VesperScreen() {
      super(net.minecraft.network.chat.Component.literal("Vesper"));
    }

    @Override
    public boolean isPauseScreen() {
      return false;
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int x, int y, float partial) {
      g.fill(0, 0, width, height, 0xff101b29);
      var stack = Minecraft.getInstance().player.getMainHandItem();
      g.drawCenteredString(
          font,
          dev.heavenlyrestriction.chromatic.ChromaticOrb.infused(stack)
              ? "Vesper · Chromatic Orb upgrade"
              : "Vesper",
          width / 2,
          30,
          0xffe1c78b);
      g.pose().pushPose();
      g.pose().translate(width - 170, height / 2 - 65, 0);
      g.pose().scale(7, 7, 1);
      g.renderItem(stack, 0, 0);
      g.pose().popPose();
      g.renderTooltip(font, stack, 16, height / 2 - 70);
    }
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new IllegalStateException(message);
  }

  private static void checkVesperTooltip(boolean upgraded) {
    var mc = Minecraft.getInstance();
    var lines =
        mc
            .player
            .getMainHandItem()
            .getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(mc.level),
                mc.player,
                net.minecraft.world.item.TooltipFlag.NORMAL)
            .stream()
            .map(net.minecraft.network.chat.Component::getString)
            .toList();
    String heading = upgraded ? "Afterwind II" : "Afterwind";
    int name = lines.indexOf(heading);
    int effect = -1;
    for (int n = 0; n < lines.size(); n++)
      if (lines.get(n).contains((upgraded ? "75%" : "30%") + " raw attack")) effect = n;
    check(name >= 0 && effect > name, "Wrong engraving heading/effect order: " + lines);
    check(
        lines.stream().anyMatch(l -> l.contains("105 Attack Damage")),
        "Native attack attribute missing/wrong: " + lines);
    check(
        lines.stream()
            .anyMatch(l -> l.contains("0.2s") && l.contains("3s") && l.contains("12 blocks")),
        "Live Afterwind timing missing: " + lines);
    check(
        lines.stream().filter(l -> l.contains("EP:")).count() == 1
            && lines.stream().anyMatch(l -> l.contains("1,000,000/1,000,000")),
        "EP tooltip duplicated/unformatted: " + lines);
    System.out.println("[Vesper tooltip " + (upgraded ? "II" : "I") + "] " + lines);
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
            boolean vesperReview = Boolean.getBoolean("heavenlyrestriction.review.vesperOnly");
            if (vesperReview) y = 160;
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
                for (int dy = 1;
                    dy <= (vesperReview ? level.getMaxBuildHeight() - y - 1 : 4);
                    dy++) {
                  var clear = new BlockPos(x + dx, y + dy, z + dz);
                  if (!level.getBlockState(clear).isAir())
                    level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
                }
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
      shot(
          Boolean.getBoolean("heavenlyrestriction.review.vesperOnly")
              ? "kael-vesper"
              : "kael-portrait");
      if (Boolean.getBoolean("heavenlyrestriction.review.kaelSkinOnly")) {
        phase = 800;
        wait = 0;
        server(
            p -> {
              camera(p, root.add(-3, .3, -4.8), root.add(0, 1, 0));
              boss.setYRot(0);
              boss.setYBodyRot(0);
              boss.setYHeadRot(0);
            });
        return;
      }
      phase = Boolean.getBoolean("heavenlyrestriction.review.vesperOnly") ? 900 : 2;
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
            if (!Boolean.getBoolean("heavenlyrestriction.review.vesperOnly")) {
              RaceAPI.getRaceFrom(dummy).setRace(TensuraRaces.HUMAN_SAINT.get(), false);
              dummy.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(200000);
              TensuraStorages.getExistenceFrom(dummy).setSpiritualHealth(200000);
              EnergyHelper.setBaseMaxEP(dummy, 20_000_000);
            }
            dummy.setTarget(boss);
            boss.setTarget(dummy);
            boss.combat().cancel();
            boss.setNoAi(true);
          });
      return;
    }
    if (phase == 800 && wait > 35) {
      shot("kael-skin-back");
      phase = 801;
      wait = 0;
      server(p -> camera(p, root.add(-4, .15, 2.4), root.add(0, 1, 0)));
      return;
    }
    if (phase == 801 && wait > 35) {
      shot("kael-skin-side");
      phase = 802;
      wait = 0;
      server(p -> camera(p, root.add(.4, .15, 3.5), root.add(0, 1.25, 0)));
      return;
    }
    if (phase == 802 && wait > 35) {
      shot("kael-skin-front");
      Files.writeString(
          OUT.resolve("passed-skin.txt"),
          "PASS: native 64x64 Kael skin and registered subtle star mask; front/back/side views,"
              + " native player geometry and held weapon.\n");
      mc.stop();
      return;
    }
    if (phase == 900 && wait > 25 && dummy != null) {
      server(
          p -> {
            check(
                boss.getMainHandItem().getItem()
                    instanceof dev.heavenlyrestriction.vesper.VesperItem,
                "Boss does not wield Vesper");
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);
            dummy.setHealth(100000);
            vesperObserver =
                e -> {
                  if (e.getEntity() == dummy
                      && dev.heavenlyrestriction.RestrictionData.attacker(e.getSource()) == boss) {
                    System.out.println(
                        "[Vesper hit] type="
                            + e.getSource().typeHolder().unwrapKey()
                            + " element="
                            + ((io.github.manasmods.tensura.damage.TensuraDamageSource)
                                    e.getSource())
                                .tensura$getElement()
                            + " ability="
                            + ((io.github.manasmods.tensura.damage.TensuraDamageSource)
                                    e.getSource())
                                .tensura$getAbilityInstance()
                            + " raw="
                            + e.getAmount()
                            + " hp="
                            + dummy.getHealth()
                            + " shp="
                            + TensuraStorages.getExistenceFrom(dummy).getSpiritualHealth());
                    if (e.getSource().is(dev.heavenlyrestriction.vesper.Afterwind.DAMAGE))
                      vesperCuts++;
                    else if (((io.github.manasmods.tensura.damage.TensuraDamageSource)
                                    e.getSource())
                                .tensura$getElement()
                            == io.github.manasmods.tensura.ability.magic.Element.WIND
                        || e.getSource()
                            .typeHolder()
                            .unwrapKey()
                            .orElseThrow()
                            .location()
                            .getPath()
                            .contains("wind")) vesperWind++;
                  }
                };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(vesperObserver);
          });
      cast(WayfarerCombat.Action.WIND_CUTTER, 4);
      phase = 901;
      wait = 0;
      return;
    }
    if (phase == 901 && wait > 15) {
      check(wait < 300, "Native Wind Cutter never reached the review enemy");
      if (vesperWind == 0) return;
      server(
          p -> {
            boss.combat().cancel();
            boss.setNoAi(true);
            boss.teleportTo(dummy.getX(), dummy.getY(), dummy.getZ() - 1.8);
            boss.setYRot(0);
            boss.setYBodyRot(0);
            boss.setYHeadRot(0);
            dummy.invulnerableTime = 0;
            camera(p, root.add(6, .7, 10), dummy.position().add(0, 1, 0));
            System.out.println(
                "[Vesper melee] held="
                    + boss.getMainHandItem()
                    + " accepted="
                    + boss.doHurtTarget(dummy)
                    + " posthp="
                    + dummy.getHealth()
                    + " postshp="
                    + TensuraStorages.getExistenceFrom(dummy).getSpiritualHealth());
          });
      phase = 902;
      wait = 0;
      return;
    }
    if (phase == 902 && wait > 6) {
      check(
          vesperCuts == 1,
          "Native wind/sword combo did not produce exactly one Afterwind cut: wind="
              + vesperWind
              + " cuts="
              + vesperCuts);
      shot("vesper-afterwind");
      server(
          p -> {
            p.getInventory().clearContent();
            p.setItemSlot(
                EquipmentSlot.MAINHAND,
                dev.heavenlyrestriction.vesper.VesperItem.create(p.registryAccess()));
            p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(vesperObserver);
          });
      phase = 903;
      wait = 0;
      return;
    }
    if (phase == 903 && wait > 20) {
      mc.setScreen(new VesperScreen());
      phase = 904;
      wait = 0;
      return;
    }
    if (phase == 904 && wait > 20) {
      checkVesperTooltip(false);
      shot("vesper-item");
      server(
          p -> {
            var next = dev.heavenlyrestriction.chromatic.ChromaticOrb.upgrade(p.getMainHandItem());
            check(!next.isEmpty(), "Chromatic Orb could not upgrade Vesper");
            p.setItemSlot(EquipmentSlot.MAINHAND, next);
          });
      phase = 905;
      wait = 0;
      return;
    }
    if (phase == 905 && wait > 20) {
      checkVesperTooltip(true);
      shot("vesper-item-ii");
      Files.writeString(
          OUT.resolve("passed-vesper.txt"),
          "PASS: Kael wields Vesper; actual native Wind Cutter then melee produced exactly one"
              + " Afterwind; native 105 attack tooltip; Afterwind I/II show ordered 30%/75% effects"
              + " and formatted EP, after a real Chromatic Orb upgrade.\n");
      mc.stop();
      return;
    }
    if (phase == 2 && wait > 25) {
      cast(WayfarerCombat.Action.WIND_TRANSFORM, 12);
      phase = 20;
      wait = 0;
      return;
    }
    if (phase == 20 && wait > 12) {
      server(
          p ->
              check(
                  io.github.manasmods.tensura.ability.skill.MagicElementalTransformSkill
                      .hasMagicTransformEffect(
                          boss, io.github.manasmods.tensura.ability.magic.Element.WIND),
                  "Opening native Magic Wind Transform was not active"));
      shot("kael-native-wind-transform");
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
