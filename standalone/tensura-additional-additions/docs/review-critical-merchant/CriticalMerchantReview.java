package dev.heavenlyrestriction;

import dev.heavenlyrestriction.critical.CriticalMerchant;
import dev.heavenlyrestriction.highrealm.HighRealm;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.ability.skill.Skill.SkillType;
import io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen.AbilityType;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import io.github.manasmods.tensura.entity.human.golem.TrainingDummyEntity;
import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class CriticalMerchantReview {
  private static final Path OUT = Path.of("/workspace/validation-wayfarer/capture");
  private static volatile int phase;
  private static int wait, age;
  private static volatile String error;
  private static volatile TrainingDummyEntity dummy;
  private static volatile boolean initialized, hit;

  private static void check(boolean b, String message) {
    if (!b) throw new IllegalStateException(message);
  }

  private static void server(java.util.function.Consumer<ServerPlayer> action) {
    var s = Minecraft.getInstance().getSingleplayerServer();
    s.execute(
        () -> {
          try {
            action.accept(s.getPlayerList().getPlayers().getFirst());
          } catch (Throwable e) {
            e.printStackTrace();
            error = e.toString();
          }
        });
  }

  private static void shot(String name) throws Exception {
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      Files.createDirectories(OUT);
      image.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  private static ManasSkillInstance local() {
    return CriticalMerchant.instance(Minecraft.getInstance().player).orElseThrow();
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    try {
      step();
    } catch (Throwable e) {
      e.printStackTrace();
      error = e.toString();
    }
  }

  private static void step() throws Exception {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
    if (error != null || ++age > 5000) {
      Files.createDirectories(OUT);
      Files.writeString(
          OUT.resolve("failed-critical-merchant.txt"),
          error == null ? "Timeout phase " + phase : error);
      mc.stop();
      return;
    }
    if (mc.player == null
        || mc.level == null
        || mc.getSingleplayerServer() == null
        || mc.getOverlay() != null) return;
    wait++;
    if (phase == 0 && wait > 80) {
      phase = 1;
      wait = 0;
      mc.setScreen(null);
      mc.options.guiScale().set(2);
      mc.options.fov().set(55);
      mc.options.hideGui = false;
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
                "Refusing a personal world");
            var level = p.server.getLevel(HighRealm.LEVEL);
            check(level != null, "Missing High Realm in review save");
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
            var root = new Vec3(1000.5, 161, 1000.5);
            for (var mob : level.getEntitiesOfClass(Mob.class, new AABB(root, root).inflate(30)))
              mob.discard();
            for (int x = -8; x <= 8; x++)
              for (int z = -8; z <= 8; z++) {
                level.setBlock(
                    new BlockPos(1000 + x, 160, 1000 + z),
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    2);
                for (int y = 161; y < 168; y++)
                  level.setBlock(
                      new BlockPos(1000 + x, y, 1000 + z), Blocks.AIR.defaultBlockState(), 2);
              }
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.setNoGravity(true);
            p.teleportTo(level, 1000.5, 161, 1003.5, 180, 0);
            p.setDeltaMovement(Vec3.ZERO);
            p.getInventory().clearContent();
            p.removeAllEffects();
            RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(), false);
            EnergyHelper.setBaseMaxEP(p, 20_000_000);
            p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
            RestrictionData.learnReward(p, HeavenlyRestriction.CRITICAL_MERCHANT.get());
            var i = CriticalMerchant.instance(p).orElseThrow();
            i.setMastery(0);
            i.setToggled(true);
            i.setCoolDown(0, 0);
            var energy = TensuraStorages.getExistenceFrom(p);
            energy.setMagicule(5000);
            var abilities = TensuraStorages.getAbilityFrom(p);
            abilities.setActivePreset(0);
            abilities.setAbilitySlot(0, 0, HeavenlyRestriction.CRITICAL_MERCHANT.get(), 0);
            abilities.markDirty();
            HeavenlyRestriction.CRITICAL_MERCHANT.get().onPressed(i, p, 0, 0);
            check(
                CriticalMerchant.remaining(i) == 10 && energy.getMagicule() == 4000,
                "Active failed or wrong magicule bill");
            dummy =
                (TrainingDummyEntity)
                    BuiltInRegistries.ENTITY_TYPE
                        .get(ResourceLocation.parse("tensura:training_dummy"))
                        .create(level);
            dummy.setPos(root.add(0, .25, 0));
            dummy.setNoAi(true);
            level.setBlock(
                dummy.blockPosition(), TensuraBlocks.TRAINING_DUMMY.get().defaultBlockState(), 3);
            level.addFreshEntity(dummy);
            ((StorageHolder) p).manasCore$sync();
            initialized = true;
          });
      return;
    }
    if (!initialized) return;
    mc.player.getAbilities().flying = true;
    mc.player.setNoGravity(true);
    if (phase == 1 && wait > 35) {
      check(
          local().getSkill().getName().getString().equals("Critical Merchant"),
          "Name not localized");
      check(CriticalMerchant.remaining(local()) == 10, "Client stock not synchronized");
      mc.setScreen(new AbilitySelectionScreen(AbilityType.SKILL, SkillType.UNIQUE, null));
      phase = 2;
      wait = 0;
      return;
    }
    if (phase == 2 && wait > 5) {
      var s = (AbilitySelectionScreen) mc.screen;
      s.selectedAbility = local();
      check(
          s.abilities.stream()
              .anyMatch(i -> i.getSkill() == HeavenlyRestriction.CRITICAL_MERCHANT.get()),
          "Missing from native Unique menu");
      phase = 3;
      wait = 0;
      return;
    }
    if (phase == 3 && wait > 20) {
      shot("critical-merchant-menu");
      mc.setScreen(null);
      phase = 4;
      wait = 0;
      return;
    }
    if (phase == 4 && wait > 15) {
      server(
          p -> {
            var i = CriticalMerchant.instance(p).orElseThrow();
            check(i.isToggled(), "Passive disabled");
            dummy.invulnerableTime = 0;
            p.resetAttackStrengthTicker();
            p.attack(dummy);
            check(
                CriticalMerchant.remaining(i) == 9 && dummy.lastDamageTaken > 0,
                "Live critical did not consume exactly one hit");
            ((StorageHolder) p).manasCore$sync();
            hit = true;
          });
      phase = 5;
      wait = 0;
      return;
    }
    if (phase == 5 && wait > 3 && hit && CriticalMerchant.remaining(local()) == 9) {
      shot("critical-merchant-hit");
      Files.writeString(
          OUT.resolve("passed-critical-merchant.txt"),
          "PASS: real single-player active cost exactly 1000 magicules; ten synchronized hits;"
              + " native Unique menu/name/icon; native player melee on a real training dummy;"
              + " critical payout and synchronized remaining stock 9.\n");
      mc.stop();
    }
  }
}
