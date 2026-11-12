package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.chromatic.ChromaticContent;
import dev.heavenlyrestriction.mentor.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("mentorreview")
@PrefixGameTestTemplate(false)
public final class MentorGameTests {
  private static ServerPlayer funded(GameTestHelper h) {
    var p = player(h);
    p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(8_000_000);
    p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(2_000_000);
    p.getInventory().add(new ItemStack(ChromaticContent.ORB.get(), 2));
    return p;
  }

  private static void near(GameTestHelper h, double actual, double expected, String message) {
    h.assertTrue(Math.abs(actual - expected) < .01, message + ": " + actual + " / " + expected);
  }

  private static boolean has(ServerPlayer p, ManasSkill s) {
    return SkillAPI.getSkillsFrom(p).getSkill(s).filter(i -> i.getMastery() >= 0).isPresent();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void oneLessonSpendsProportionalPermanentEpAndExactlyOneOrb(GameTestHelper h) {
    var p = funded(h);
    double cost = MentorConfig.COST.get();
    h.assertTrue(
        MentorService.purchase(p, HeavenlyRestriction.PROSPECTOR.getId()).equals("success"),
        "Purchase failed");
    near(h, EnergyHelper.getBaseMaxEP(p), 10_000_000 - cost, "Permanent EP charge");
    near(
        h,
        p.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue(),
        8_000_000 - cost * .8,
        "Aura ratio");
    near(
        h,
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue(),
        2_000_000 - cost * .2,
        "Magicule ratio");
    h.assertTrue(p.getInventory().countItem(ChromaticContent.ORB.get()) == 1, "Wrong Orb charge");
    var skill =
        SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PROSPECTOR.get()).orElseThrow();
    near(h, skill.getMastery(), 0, "Lesson should start unmastered");
    h.assertTrue(!skill.isTemporarySkill(), "Lesson temporary");
    h.assertTrue(
        MentorService.purchase(p, UniqueSkills.GODLY_CRAFTSMAN.getId()).equals("used"),
        "Second lesson succeeded");
    near(h, EnergyHelper.getBaseMaxEP(p), 10_000_000 - cost, "Repeat charged EP");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void equipmentEpAndCreativeModeCannotPayPermanentLessonOrSkipOrb(GameTestHelper h) {
    var p = player(h);
    double before = EnergyHelper.getBaseMaxEP(p);
    h.assertTrue(
        MentorService.purchase(p, HeavenlyRestriction.PROSPECTOR.getId()).equals("cost"),
        "No Orb accepted");
    near(h, EnergyHelper.getBaseMaxEP(p), before, "Failed purchase charged EP");
    p.getInventory().add(new ItemStack(ChromaticContent.ORB.get()));
    EnergyHelper.setBaseMaxEP(p, 500_000);
    p.getAttribute(TensuraAttributes.MAX_AURA)
        .addTransientModifier(
            new AttributeModifier(
                HeavenlyRestriction.id("mentor_test"),
                20_000_000,
                AttributeModifier.Operation.ADD_VALUE));
    h.assertTrue(
        EnergyHelper.getMaxEP(p) > MentorConfig.COST.get() && !MentorService.affordable(p),
        "Modifier capacity paid permanent EP");
    p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    h.assertTrue(
        MentorService.purchase(p, HeavenlyRestriction.PROSPECTOR.getId()).equals("cost"),
        "Creative skipped bill");
    h.assertTrue(
        MentorLedger.reconcile(p).isEmpty() && !has(p, HeavenlyRestriction.PROSPECTOR.get()),
        "Failed purchase used lesson");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void catalogueIncludesSynthesisPartnersAndRejectsProtectedTiersAndRestrictions(
      GameTestHelper h) {
    var p = funded(h);
    var catalog = MentorService.catalog(p);
    h.assertTrue(
        catalog.contains(UniqueSkills.GREAT_SAGE.get())
            && catalog.contains(UniqueSkills.GODLY_CRAFTSMAN.get())
            && catalog.contains(HeavenlyRestriction.SPLIT_MIND.get())
            && catalog.contains(HeavenlyRestriction.PROSPECTOR.get()),
        "Missing synthesis components");
    h.assertTrue(
        !catalog.contains(HeavenlyRestriction.CHAINED.get())
            && !catalog.contains(UniqueSkills.ANTI_SKILL.get())
            && !catalog.contains(HeavenlyRestriction.METIS.get())
            && !catalog.contains(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()),
        "Special/Ultimate/multiplayer-only exclusion failed");
    h.assertTrue(
        MentorService.purchase(p, UniqueSkills.ANTI_SKILL.getId()).equals("unavailable"),
        "Anti-Skill accepted by server-side purchase validation");
    near(h, EnergyHelper.getBaseMaxEP(p), 10_000_000, "Excluded lesson charged EP");
    h.assertTrue(
        MentorLedger.reconcile(p).isEmpty()
            && p.getInventory().countItem(ChromaticContent.ORB.get()) == 2,
        "Excluded lesson consumed Orb or redemption");
    RestrictionData.learnReward(p, HeavenlyRestriction.CHAINED.get());
    ChainedState.initialize(p);
    h.assertTrue(
        !MentorService.learnable(p, HeavenlyRestriction.PROSPECTOR.get())
            && MentorService.purchase(p, HeavenlyRestriction.PROSPECTOR.getId())
                .equals("unavailable"),
        "Mentor bypassed Chained");
    h.assertTrue(
        MentorLedger.reconcile(p).isEmpty()
            && p.getInventory().countItem(ChromaticContent.ORB.get()) == 2,
        "Blocked lesson charged");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void knownSkillsAndNativeUnlockVetoDoNotConsumeTheLesson(GameTestHelper h) {
    var p = funded(h);
    RestrictionData.learnReward(p, HeavenlyRestriction.PROSPECTOR.get());
    h.assertTrue(
        MentorService.purchase(p, HeavenlyRestriction.PROSPECTOR.getId()).equals("unavailable"),
        "Owned skill was sold");
    SkillEvents.UNLOCK_SKILL.register(
        (i, owner, message) ->
            owner.getPersistentData().getBoolean("mentor_test_veto")
                    && i.getSkill() == HeavenlyRestriction.SPLIT_MIND.get()
                ? dev.architectury.event.EventResult.interruptFalse()
                : dev.architectury.event.EventResult.pass());
    p.getPersistentData().putBoolean("mentor_test_veto", true);
    double before = EnergyHelper.getBaseMaxEP(p);
    h.assertTrue(
        MentorService.purchase(p, HeavenlyRestriction.SPLIT_MIND.getId()).equals("unavailable"),
        "Native veto ignored");
    near(h, EnergyHelper.getBaseMaxEP(p), before, "Veto charged EP");
    h.assertTrue(
        !has(p, HeavenlyRestriction.SPLIT_MIND.get())
            && MentorLedger.reconcile(p).isEmpty()
            && p.getInventory().countItem(ChromaticContent.ORB.get()) == 2,
        "Veto consumed lesson/Orb");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void redemptionSurvivesLostSkillPlayerDataResetAndTravelMarkerImport(
      GameTestHelper h) {
    var p = funded(h);
    var id = HeavenlyRestriction.PROSPECTOR.getId();
    h.assertTrue(MentorService.purchase(p, id).equals("success"), "Initial lesson failed");
    SkillAPI.getSkillsFrom(p).forgetSkill(HeavenlyRestriction.PROSPECTOR.get());
    p.getPersistentData().remove(HeavenlyRestriction.ID);
    h.assertTrue(
        MentorLedger.reconcile(p).equals(id.toString()),
        "Race/death data reset cleared redemption");
    h.assertTrue(
        MentorService.purchase(p, UniqueSkills.GREAT_SAGE.getId()).equals("used"),
        "Lost skill allowed repeat lesson");
    var saved = MentorLedger.get(p.server).save(new CompoundTag(), p.registryAccess());
    h.assertTrue(
        MentorLedger.load(saved, p.registryAccess()).lesson(p.getUUID()).equals(id.toString()),
        "Saved ledger lost redemption");
    var traveler = player(h);
    RestrictionData.of(traveler).merge(RestrictionData.of(p).copy());
    h.assertTrue(
        MentorLedger.reconcile(traveler).equals(id.toString())
            && MentorLedger.get(p.server).lesson(traveler.getUUID()).equals(id.toString()),
        "Carried World Jump marker not imported");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void networkRequiresNearbyMentorOwnerSessionAndRejectsReplayedPurchase(
      GameTestHelper h) {
    var p = funded(h);
    h.getLevel().addNewPlayer(p);
    var npc = MentorContent.MENTOR.get().create(h.getLevel());
    npc.setPos(p.position().add(2, 0, 0));
    h.getLevel().addFreshEntity(npc);
    afterTracking(
        h,
        List.of(npc),
        () -> {
          MentorNetwork.open(p, npc);
          var token = MentorNetwork.snapshot(p, "", 0).getUUID("session");
          MentorNetwork.handle(
              p,
              new MentorNetwork.Request(
                  UUID.randomUUID(),
                  "buy",
                  HeavenlyRestriction.PROSPECTOR.getId().toString(),
                  "",
                  0));
          h.assertTrue(MentorLedger.reconcile(p).isEmpty(), "Forged nonce accepted");
          var request =
              new MentorNetwork.Request(
                  token, "buy", HeavenlyRestriction.PROSPECTOR.getId().toString(), "", 0);
          MentorNetwork.handle(p, request);
          MentorNetwork.handle(p, request);
          h.assertTrue(
              has(p, HeavenlyRestriction.PROSPECTOR.get())
                  && p.getInventory().countItem(ChromaticContent.ORB.get()) == 1,
              "Purchase/replay handling failed");
          var other = funded(h);
          MentorNetwork.handle(other, request);
          h.assertTrue(MentorLedger.reconcile(other).isEmpty(), "Another player reused session");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void leavingMentorRangeInvalidatesThePurchaseSession(GameTestHelper h) {
    var p = funded(h);
    var npc = MentorContent.MENTOR.get().create(h.getLevel());
    npc.setPos(p.position().add(2, 0, 0));
    h.getLevel().addFreshEntity(npc);
    afterTracking(
        h,
        List.of(npc),
        () -> {
          MentorNetwork.open(p, npc);
          var token = MentorNetwork.snapshot(p, "", 0).getUUID("session");
          p.setPos(p.position().add(20, 0, 0));
          MentorNetwork.handle(
              p,
              new MentorNetwork.Request(
                  token, "buy", HeavenlyRestriction.PROSPECTOR.getId().toString(), "", 0));
          h.assertTrue(
              MentorLedger.reconcile(p).isEmpty()
                  && p.getInventory().countItem(ChromaticContent.ORB.get()) == 2,
              "Remote purchase succeeded");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void archiveTemplatePlacesRotatedLibraryAndRealProtectedMentor(GameTestHelper h) {
    var p = player(h);
    var origin = BlockPos.containing(p.position().add(40, 0, 0));
    var generator = h.getLevel().getChunkSource().getGenerator();
    var structure =
        h.getLevel()
            .registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
            .get(HeavenlyRestriction.id("mentor_archive"));
    var generated =
        structure.generate(
            h.getLevel().registryAccess(),
            generator,
            generator.getBiomeSource(),
            h.getLevel().getChunkSource().randomState(),
            h.getLevel().getStructureManager(),
            h.getLevel().getSeed(),
            new net.minecraft.world.level.ChunkPos(origin),
            0,
            h.getLevel(),
            biome -> true);
    h.assertTrue(
        generated.isValid() && generated.getPieces().size() == 1, "Native jigsaw start was empty");
    var template =
        h.getLevel()
            .getStructureManager()
            .get(HeavenlyRestriction.id("mentor_archive"))
            .orElseThrow();
    h.assertTrue(
        template.getSize().getX() == 31 && template.getSize().getZ() == 33,
        "Archive template missing");
    var settings = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90);
    h.assertTrue(
        template.placeInWorld(h.getLevel(), origin, origin, settings, h.getLevel().random, 2),
        "Native template placement failed");
    // Projecting facade trim must leave the entrance and both connecting passages open.
    for (var local :
        List.of(
            new BlockPos(16, 4, 8),
            new BlockPos(7, 4, 15),
            new BlockPos(8, 10, 15),
            new BlockPos(24, 4, 23))) {
      var rotated = origin.offset(-local.getZ(), local.getY(), local.getX());
      h.assertTrue(h.getLevel().getBlockState(rotated).isAir(), "Facade closed passage " + local);
    }
    for (int y : new int[] {6, 12, 19}) {
      var fixture = h.getLevel().getBlockState(origin.offset(-15, y, 4));
      h.assertTrue(
          fixture.is(net.minecraft.world.level.block.Blocks.LANTERN)
              && fixture.getValue(net.minecraft.world.level.block.LanternBlock.HANGING),
          "Tower floor missing hanging light " + y);
    }
    h.assertTrue(
        h.getLevel()
            .getBlockState(origin.offset(-24, 8, 27))
            .is(net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS),
        "Bay ceiling missing");
    h.assertTrue(
        h.getLevel()
            .getBlockState(origin.offset(-24, 9, 27))
            .is(net.minecraft.world.level.block.Blocks.WAXED_WEATHERED_CUT_COPPER),
        "Bay roof missing");
    h.runAfterDelay(
        5,
        () -> {
          var mentors =
              h.getLevel()
                  .getEntitiesOfClass(
                      MentorEntity.class, new net.minecraft.world.phys.AABB(origin).inflate(45));
          h.assertTrue(
              mentors.size() == 1
                  && mentors.getFirst().isInvulnerable()
                  && mentors.getFirst().getUniqueSkills().isEmpty(),
              "Template did not create exactly one protected teaching NPC");
          h.assertTrue(
              !mentors.getFirst().hurt(p.damageSources().playerAttack(p), 100), "Mentor farmable");
          h.succeed();
        });
  }
}
