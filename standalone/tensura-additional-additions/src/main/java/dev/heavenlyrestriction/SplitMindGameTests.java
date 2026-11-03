package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.ChainedGameTests.near;
import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.magic.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.magic.*;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class SplitMindGameTests {
  static ManasSkillInstance learn(LivingEntity p, ManasSkill skill, boolean master) {
    RestrictionData.learnReward(p, skill);
    var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
    i.setMastery(master ? i.getMaxMastery() : 0);
    return i;
  }

  static ManasSkillInstance mind(ServerPlayer p, boolean passive) {
    var i = learn(p, HeavenlyRestriction.SPLIT_MIND.get(), true);
    if (passive) i.onToggleOn(p);
    return i;
  }

  static void enable(ServerPlayer p, ManasSkillInstance mind, ManasSkillInstance magic) {
    if (!SplitMindCasting.assign(p, magic.getSkillId().toString()))
      throw new AssertionError("Could not assign owned magic");
    mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, true);
  }

  static void step(ServerPlayer p, int time) {
    p.tickCount = time;
    for (var held : List.copyOf(SkillAPI.getSkillsFrom(p).getHeldSkills()))
      if (!held.tick(SkillAPI.getSkillsFrom(p), p))
        SkillAPI.getSkillsFrom(p).getHeldSkills().remove(held);
    SplitMindCasting.update(p);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void modeAssignmentRetainsNativeUnlocksAndOldSaveDefaults(GameTestHelper h) {
    var p = player(h);
    var mind = mind(p, true);
    var stone = learn(p, AspectualMagics.STONE_SHOT.get(), false);
    var before = stone.toNBT();
    h.assertTrue(
        SplitMindModes.available(p, stone).equals(Set.of(0)), "Mastery-gated mode unlocked early");
    h.assertTrue(before.equals(stone.toNBT()), "Inspecting modes mutated learning state");
    h.assertTrue(
        !SplitMindCasting.assign(p, stone.getSkillId().toString(), 1)
            && !SplitMindCasting.assign(p, stone.getSkillId().toString(), -1)
            && !SplitMindCasting.assign(p, stone.getSkillId().toString(), 999),
        "Invalid or locked mode accepted");
    h.assertTrue(
        SplitMindCasting.assign(p, stone.getSkillId().toString()),
        "Legacy mode-zero assignment failed");
    mind.getOrCreateTag().remove(SplitMindCasting.MODE);
    h.assertTrue(
        SplitMindCasting.assignedMode(mind) == 0 && SplitMindCasting.assigned(p, mind).isPresent(),
        "Old assignment did not default to mode zero");
    var locked =
        SplitMindNetwork.snapshot(p, "stone", 1, 0)
            .getList("rows", 10)
            .getCompound(0)
            .getList("modes", 10);
    h.assertTrue(
        locked.size() == 2 && !locked.getCompound(1).getBoolean("available"),
        "Dropdown did not mark locked mode");
    stone.setMastery(stone.getMaxMastery());
    stone.setCoolDown(37, 0);
    stone.setCoolDown(9, 1);
    h.assertTrue(
        SplitMindCasting.assign(p, stone.getSkillId().toString(), 1),
        "Unlocked Chain assignment rejected");
    var copy = ManasSkillInstance.fromNBT(mind.toNBT());
    h.assertTrue(SplitMindCasting.assignedMode(copy) == 1, "Selected mode lost on reload");
    var state = SplitMindNetwork.snapshot(p, "stone", 1, 0);
    var options = state.getList("rows", 10).getCompound(0).getList("modes", 10);
    h.assertTrue(
        state.getInt("assignedMode") == 1
            && options.getCompound(1).getInt("cooldown") == 9
            && options.getCompound(0).getInt("cooldown") == 37,
        "Mode-specific state/cooldowns incorrect");
    stone.setMastery(0);
    mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, true);
    SplitMindCasting.update(p);
    h.assertTrue(
        !mind.getOrCreateTag().getBoolean(SplitMindCasting.AUTO),
        "Auto-cast continued after losing mode eligibility");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void autoCastUsesStoneShotChainModeAndItsOwnCooldown(GameTestHelper h) {
    var p = player(h);
    var mind = mind(p, true);
    var stone = learn(p, AspectualMagics.STONE_SHOT.get(), true);
    stone.setCoolDown(99, 0);
    h.assertTrue(
        SplitMindCasting.assign(p, stone.getSkillId().toString(), 1), "Chain assignment failed");
    mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, true);
    var target = target(h, p.position().add(12, 0, 0));
    var shots =
        new ArrayList<io.github.manasmods.tensura.entity.projectile.magic.StoneShotProjectile>();
    java.util.function.Consumer<EntityJoinLevelEvent> watch =
        e -> {
          if (e.getEntity()
                  instanceof
                  io.github.manasmods.tensura.entity.projectile.magic.StoneShotProjectile shot
              && shot.getOwner() == p) shots.add(shot);
        };
    NeoForge.EVENT_BUS.addListener(watch);
    afterTracking(
        h,
        List.of(target),
        () -> {
          try {
            double mp = TensuraStorages.getExistenceFrom(p).getMagicule();
            step(p, 5);
            step(p, 6);
            step(p, 7);
            h.assertTrue(
                shots.size() > 1
                    && shots.stream().mapToInt(s -> s.getDelayTick()).distinct().count() > 1,
                "Chain mode did not stagger native stones");
            near(
                h,
                mp - TensuraStorages.getExistenceFrom(p).getMagicule(),
                ((Magic) stone.getSkill()).getMagiculeCost(p, stone, 1)
                    * p.getAttributeValue(TensuraAttributes.MAGIC_COST_MULTIPLIER),
                "Alternate mode charged wrong cost");
            h.assertTrue(
                stone.getCoolDown(0) == 99 && stone.onCoolDown(1),
                "Alternate cast used/reset primary cooldown");
            p.setYRot(90);
            p.setXRot(0);
            for (var shot : shots) {
              shot.updateShootRotation();
              h.assertTrue(
                  shot.getDelayVec()
                          .normalize()
                          .dot(
                              target
                                  .getBoundingBox()
                                  .getCenter()
                                  .subtract(shot.position())
                                  .normalize())
                      > .95,
                  "Delayed Chain stone followed player camera");
              h.assertTrue(
                  shot.getHomingTarget() == null && shot.getLookDistance() > 0,
                  "Auto aim changed native homing/gravity behavior");
            }
            int count = shots.size();
            for (int n = 10; n < 25; n++) step(p, n);
            h.assertTrue(shots.size() == count, "Selected mode bypassed its cooldown");
            h.succeed();
          } finally {
            SplitMindCasting.cancel(p);
            NeoForge.EVENT_BUS.unregister(watch);
          }
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void autoCastMaintainsRepeatModeAndPaysForEachNativePulse(GameTestHelper h) {
    var p = player(h);
    var mind = mind(p, true);
    var fire = learn(p, AspectualMagics.FIRE_LANCE.get(), true);
    fire.setCoolDown(99, 0);
    h.assertTrue(
        SplitMindCasting.assign(p, fire.getSkillId().toString(), 1), "Repeat assignment failed");
    mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, true);
    var target = target(h, p.position().add(12, 0, 0));
    var shots = new ArrayList<Projectile>();
    java.util.function.Consumer<EntityJoinLevelEvent> watch =
        e -> {
          if (e.getEntity() instanceof Projectile shot
              && shot.getOwner() == p
              && shot.getClass().getSimpleName().equals("FireLanceProjectile")) shots.add(shot);
        };
    NeoForge.EVENT_BUS.addListener(watch);
    afterTracking(
        h,
        List.of(target),
        () -> {
          try {
            double mp = TensuraStorages.getExistenceFrom(p).getMagicule();
            int interval =
                ((io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig)
                        io.github.manasmods.manascore.config.ConfigRegistry.getConfig(
                            io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig
                                .class))
                    .FireLance.castTimeRepeat;
            step(p, 5);
            for (int n = 6; n <= 8 + interval * 2; n++) step(p, n);
            h.assertTrue(
                shots.size() >= 2 && !SkillAPI.getSkillsFrom(p).getHeldSkills().isEmpty(),
                "Repeat mode released before native pulses");
            near(
                h,
                mp - TensuraStorages.getExistenceFrom(p).getMagicule(),
                shots.size()
                    * ((Magic) fire.getSkill()).getMagiculeCost(p, fire, 1)
                    * p.getAttributeValue(TensuraAttributes.MAGIC_COST_MULTIPLIER),
                "Repeat pulses were free or double-charged");
            h.assertTrue(fire.getCoolDown(0) == 99, "Repeat mode touched Single cooldown");
            int count = shots.size();
            mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, false);
            step(p, 9 + interval * 2);
            for (int n = 10 + interval * 2; n < 10 + interval * 4; n++) step(p, n);
            h.assertTrue(
                shots.size() == count && SkillAPI.getSkillsFrom(p).getHeldSkills().isEmpty(),
                "Repeat did not stop after Auto-Cast disabled");
            h.succeed();
          } finally {
            SplitMindCasting.cancel(p);
            NeoForge.EVENT_BUS.unregister(watch);
          }
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void splitMindPassivesUseNativeChantAndSeparateThoughtModifiers(GameTestHelper h) {
    var p = player(h);
    var i = mind(p, false);
    var fire = learn(p, AspectualMagics.FIRE.get(), true);
    double learn = p.getAttributeValue(TensuraAttributes.ABILITY_LEARNING_GAIN),
        mastery = p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN),
        speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
    h.assertTrue(!MagicUtils.hasChantAnnulment(p), "Disabled passive annulled chanting");
    for (int n = 0; n < 3; n++) {
      i.onToggleOn(p);
      SplitMindSkill.refresh(p);
      near(
          h,
          p.getAttributeValue(TensuraAttributes.ABILITY_LEARNING_GAIN),
          learn + 9,
          "Learning gain not +9");
      near(
          h,
          p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN),
          mastery + 9,
          "Mastery gain not +9");
      h.assertTrue(
          MagicUtils.hasChantAnnulment(p) && ((Magic) fire.getSkill()).getCastingTime(fire, p) == 1,
          "Native instant-cast integration missing");
      h.assertTrue(
          p.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed,
          "Native thought acceleration missing");
      i.onToggleOff(p);
      near(
          h,
          p.getAttributeValue(TensuraAttributes.ABILITY_LEARNING_GAIN),
          learn,
          "Off-toggle retained points");
      near(
          h,
          p.getAttributeValue(Attributes.MOVEMENT_SPEED),
          speed,
          "Off-toggle retained thought modifiers");
    }
    var thought = learn(p, ExtraSkills.THOUGHT_ACCELERATION.get(), true);
    thought.setToggled(true);
    thought.onToggleOn(p);
    double ownSpeed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
    i.onToggleOn(p);
    i.onToggleOff(p);
    near(
        h,
        p.getAttributeValue(Attributes.MOVEMENT_SPEED),
        ownSpeed,
        "Split Mind erased separate Thought Acceleration");
    h.assertTrue(
        ChainedReincarnation.blocked(i.getSkill(), p),
        "Magic auto-caster leaked into Fettered pool");
    h.assertTrue(
        io.github.manasmods.tensura.menu.ReincarnationMenu.getSkillPool().contains(i.getSkill()),
        "Unique reincarnation pool missing skill");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void assignmentShowsOnlyLearnedAspectualAndSpiritualAndRejectsForgery(
      GameTestHelper h) {
    var p = player(h);
    var i = mind(p, true);
    var fire = learn(p, AspectualMagics.FIRE.get(), false);
    var spirit = learn(p, SpiritualMagics.FIRE.get(), false);
    var draft = learn(p, AspectualMagics.FIRE_BALL.get(), false);
    draft.setMastery(-1);
    learn(p, ExtraSkills.MAGIC_SENSE.get(), true);
    var all = SplitMindNetwork.snapshot(p, "", 0, 0);
    h.assertTrue(all.getInt("total") == 2, "Unowned/unlearned/skill entries leaked");
    h.assertTrue(
        SplitMindNetwork.snapshot(p, "", 1, 99).getInt("total") == 1
            && SplitMindNetwork.snapshot(p, "", 2, 0).getInt("total") == 1,
        "Category filters failed");
    h.assertTrue(
        !SplitMindCasting.assign(p, draft.getSkillId().toString())
            && !SplitMindCasting.assign(p, "not a valid id")
            && !SplitMindCasting.assign(
                p, AspectualMagics.THUNDER_ORB.get().getRegistryName().toString()),
        "Forged assignment accepted");
    h.assertTrue(
        SplitMindCasting.assign(p, spirit.getSkillId().toString()),
        "Learned spiritual magic rejected");
    var saved = i.toNBT();
    var copy = ManasSkillInstance.fromNBT(saved);
    h.assertTrue(
        copy.getOrCreateTag()
            .getString(SplitMindCasting.ASSIGNED)
            .equals(spirit.getSkillId().toString()),
        "Assignment lost on NBT reload");
    h.assertTrue(
        SplitMindCasting.assign(p, "") && SplitMindCasting.assigned(p, i).isEmpty(),
        "Clear assignment failed");
    i.setMastery(-1);
    h.assertTrue(
        !SplitMindCasting.assign(p, fire.getSkillId().toString()),
        "Unlearned Split Mind assigned magic");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void autoCastFindsOnlyVisibleHostilesAndCombatOpponents(GameTestHelper h) {
    var p = player(h);
    mind(p, true);
    var pig = EntityType.PIG.create(h.getLevel());
    pig.setPos(p.position().add(2, 0, 0));
    h.getLevel().addFreshEntity(pig);
    var own = target(h, p.position().add(3, 0, 0));
    TensuraStorages.getExistenceFrom(own).setPermanentOwner(p.getUUID());
    var nearest = target(h, p.position().add(8, 0, 0));
    var distant = target(h, p.position().add(34, 0, 0));
    afterTracking(
        h,
        List.of(pig, own, nearest, distant),
        () -> {
          h.assertTrue(SplitMindCasting.nearest(p) == nearest, "Did not select nearest real enemy");
          nearest.setPos(p.position().add(33, 0, 0));
          h.assertTrue(!SplitMindCasting.enemy(p, nearest), "Range limit ignored");
          pig.setLastHurtByMob(p);
          h.assertTrue(SplitMindCasting.enemy(p, pig), "Neutral combat opponent ignored");
          pig.setLastHurtByMob(null);
          h.assertTrue(
              !SplitMindCasting.enemy(p, pig) && !SplitMindCasting.enemy(p, own),
              "Passive/owned creature targeted");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void realMagicAutoCastKeepsAimPaysOnceAndHonorsCooldown(GameTestHelper h) {
    var p = player(h);
    var i = mind(p, true);
    var fire = learn(p, AspectualMagics.FIRE_BALL.get(), true);
    var target = target(h, p.position().add(12, 0, 0));
    enable(p, i, fire);
    p.setYRot(85);
    p.setXRot(-19);
    p.setYHeadRot(85);
    var projectiles = new ArrayList<Projectile>();
    java.util.function.Consumer<EntityJoinLevelEvent> watch =
        e -> {
          if (e.getEntity() instanceof Projectile projectile
              && projectile.getOwner() == p
              && projectile.getClass().getPackageName().contains("projectile.magic"))
            projectiles.add(projectile);
        };
    NeoForge.EVENT_BUS.addListener(watch);
    afterTracking(
        h,
        List.of(target),
        () -> {
          try {
            double mp = TensuraStorages.getExistenceFrom(p).getMagicule();
            h.assertTrue(
                !SkillAPI.getSkillsFrom(p).startHoldSkill(fire, 3, 0),
                "External virtual-slot activation bypassed native presets");
            step(p, 5);
            step(p, 6);
            step(p, 7);
            h.assertTrue(!projectiles.isEmpty(), "Native magic did not cast");
            double cost =
                ((Magic) fire.getSkill()).getMagiculeCost(p, fire, 0)
                    * p.getAttributeValue(TensuraAttributes.MAGIC_COST_MULTIPLIER);
            near(
                h,
                mp - TensuraStorages.getExistenceFrom(p).getMagicule(),
                cost,
                "Magic was free or charged twice");
            near(h, p.getYRot(), 85, "Auto-cast rotated camera yaw");
            near(h, p.getXRot(), -19, "Auto-cast rotated camera pitch");
            var expected =
                target.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
            h.assertTrue(
                projectiles.getFirst().getDeltaMovement().normalize().dot(expected) > .95,
                "Spell ignored selected target");
            h.assertTrue(fire.onCoolDown(0), "Native cooldown missing");
            int count = projectiles.size();
            for (int n = 10; n < 30; n++) step(p, n);
            h.assertTrue(projectiles.size() == count, "Auto-cast bypassed native cooldown");
            h.succeed();
          } finally {
            SplitMindCasting.cancel(p);
            NeoForge.EVENT_BUS.unregister(watch);
          }
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void autoCastWaitsForManaAndDoesNotHijackManualCasting(GameTestHelper h) {
    var p = player(h);
    var i = mind(p, true);
    var fire = learn(p, AspectualMagics.FIRE.get(), true);
    var target = target(h, p.position().add(10, 0, 0));
    enable(p, i, fire);
    afterTracking(
        h,
        List.of(target),
        () -> {
          var storage = SkillAPI.getSkillsFrom(p);
          var pool = TensuraStorages.getExistenceFrom(p);
          pool.setMagicule(1);
          step(p, 5);
          h.assertTrue(storage.getHeldSkills().isEmpty(), "Unfunded auto-cast started");
          pool.setMagicule(100000);
          TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, fire.getSkill(), 0);
          h.assertTrue(
              storage.startHoldSkill(fire, 0, 0),
              "Manual hold rejected: effects="
                  + p.getActiveEffects()
                  + ", canActivate="
                  + fire.canActivateSkill(p, 0));
          var manual = storage.getHeldSkills().getFirst();
          step(p, 10);
          h.assertTrue(
              storage.getHeldSkills().size() == 1 && storage.getHeldSkills().getFirst() == manual,
              "Auto-cast stole a manual hold");
          SplitMindCasting.cancel(p);
          h.assertTrue(storage.getHeldSkills().contains(manual), "Cancel removed manual hold");
          storage.getHeldSkills().clear();
          fire.removeAttributeModifiers(p, 0);
          p.addEffect(
              new net.minecraft.world.effect.MobEffectInstance(HeavenlyArsenal.SKILL_SEAL, 40));
          step(p, 15);
          h.assertTrue(storage.getHeldSkills().isEmpty(), "Sealed auto-cast activated");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void unmasteredMagicChargesOnlyAfterFullCastAndCancelsLostTarget(GameTestHelper h) {
    var p = player(h);
    var i = mind(p, false);
    i.setMastery(0);
    var fire = learn(p, AspectualMagics.FIRE.get(), false);
    var target = target(h, p.position().add(11, 0, 0));
    enable(p, i, fire);
    afterTracking(
        h,
        List.of(target),
        () -> {
          var pool = TensuraStorages.getExistenceFrom(p);
          double mp = pool.getMagicule();
          int ticks = ((Magic) fire.getSkill()).getCastingTime(fire, p);
          h.assertTrue(ticks > 1, "Unmastered magic unexpectedly instant");
          step(p, 5);
          step(p, 6);
          near(h, pool.getMagicule(), mp, "Incomplete cast charged MP");
          target.setPos(p.position().add(40, 0, 0));
          step(p, 7);
          near(h, pool.getMagicule(), mp, "Lost target charged MP");
          h.assertTrue(
              SkillAPI.getSkillsFrom(p).getHeldSkills().isEmpty(), "Lost target retained cast");
          target.setPos(p.position().add(11, 0, 0));
          // Entity section tracking follows a real world tick after crossing chunk boundaries.
          afterTracking(
              h,
              List.of(target),
              () -> {
                step(p, 10);
                for (int n = 11; n <= 16 + ticks; n++) step(p, n);
                h.assertTrue(
                    pool.getMagicule() < mp,
                    "Full native unmastered cast did not spend MP; held="
                        + SkillAPI.getSkillsFrom(p).getHeldSkills().size()
                        + ", nearest="
                        + (SplitMindCasting.nearest(p) == target)
                        + ", cast="
                        + ((Magic) fire.getSkill()).getCastingTime(fire, p));
                h.assertTrue(
                    i.getMastery() > 0, "Successful paid auto-cast did not train Split Mind");
                SplitMindCasting.cancel(p);
                i.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, false);
                step(p, 17 + ticks);
                h.assertTrue(
                    SkillAPI.getSkillsFrom(p).getHeldSkills().isEmpty(), "Auto off retained cast");
                h.succeed();
              });
        });
  }
}
