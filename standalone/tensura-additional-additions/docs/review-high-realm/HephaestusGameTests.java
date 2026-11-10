package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.forge.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.subclass.*;
import io.github.manasmods.tensura.entity.human.golem.TrainingDummyEntity;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.race.RaceHelper;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("forgereview")
@PrefixGameTestTemplate(false)
public final class HephaestusGameTests {
  private static ManasSkillInstance grant(ServerPlayer p, ManasSkill skill) {
    RestrictionData.learnReward(p, skill);
    var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
    i.setMastery(i.getMaxMastery());
    return i;
  }

  private static ServerPlayer smith(GameTestHelper h) {
    var p = player(h);
    grant(p, HeavenlyRestriction.HEPHAESTUS.get()).setToggled(true);
    p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(1000000);
    TensuraStorages.getExistenceFrom(p).setMagicule(1000000);
    return p;
  }

  private static ServerPlayer candidate(GameTestHelper h) {
    var p = player(h);
    grant(p, HeavenlyRestriction.PROSPECTOR.get());
    grant(p, UniqueSkills.GODLY_CRAFTSMAN.get());
    p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(10);
    p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(300000);
    TensuraStorages.getExistenceFrom(p).setMagicule(300000);
    RestrictionData.of(p).putBoolean("hephaestus_forged_high_tier", true);
    ForgeSynthesis.discover(p);
    return p;
  }

  private static void near(GameTestHelper h, double a, double b, String label) {
    h.assertTrue(Math.abs(a - b) < .01, label + ": " + a + " / " + b);
  }

  private static double attack(ItemStack s) {
    double[] damage = {1};
    s.forEachModifier(
        EquipmentSlot.MAINHAND,
        (a, m) -> {
          if (a.equals(Attributes.ATTACK_DAMAGE)
              && m.operation() == AttributeModifier.Operation.ADD_VALUE) damage[0] += m.amount();
        });
    return damage[0];
  }

  private static void material(ServerPlayer p, int slot, Item item, int count) {
    p.getInventory().setItem(slot, new ItemStack(item, count));
  }

  private static void forge(ServerPlayer p, ItemStack s) {
    ForgeItems.stamp(p, s);
  }

  private static TrainingDummyEntity dummy(GameTestHelper h, ServerPlayer p) {
    var t =
        (TrainingDummyEntity)
            BuiltInRegistries.ENTITY_TYPE
                .get(net.minecraft.resources.ResourceLocation.parse("tensura:training_dummy"))
                .create(h.getLevel());
    t.setPos(p.position().add(0, 0, 4));
    t.setNoAi(true);
    h.getLevel().setBlock(t.blockPosition().below(), Blocks.STONE.defaultBlockState(), 3);
    h.getLevel()
        .setBlock(t.blockPosition(), TensuraBlocks.TRAINING_DUMMY.get().defaultBlockState(), 3);
    h.getLevel().addFreshEntity(t);
    return t;
  }

  private static void temper(ItemStack s, ForgeItems.Temper temper, double strength) {
    var tag = ForgeItems.data(s).copy();
    tag.putInt("temper", temper.ordinal());
    tag.putDouble("quality", strength);
    s.set(ForgeContent.WORK.get(), tag);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void hephaestusUsesNativeUltimateAndRetainsBothSourceInterfaces(GameTestHelper h) {
    var s = HeavenlyRestriction.HEPHAESTUS.get();
    h.assertTrue(
        s.getModes(s.createDefaultInstance()) == 3
            && s.getType() == Skill.SkillType.ULTIMATE
            && s instanceof IRefining
            && s instanceof IResearcherEnchanter,
        "Wrong classification or lost native interfaces");
    h.assertTrue(
        s.getMaximumBonusLevel()
            == ((io.github.manasmods.tensura.ability.skill.unique.GodlyCraftsmanSkill)
                    UniqueSkills.GODLY_CRAFTSMAN.get())
                .getMaximumBonusLevel(),
        "Engraving configuration diverged");
    var p = candidate(h);
    var priced = s.createDefaultInstance();
    priced.setMastery(0);
    near(
        h,
        s.getAcquiringMagiculeCost(priced),
        ForgeConfig.ACQUISITION.get(),
        "Native acquisition price must remain visible");
    priced.setMastery(-1);
    near(h, s.getAcquiringMagiculeCost(priced), 0, "Unfinished entry must not charge early");
    h.assertTrue(
        s.nextMode(p, s.createDefaultInstance(), 2, false) == 0
            && s.nextMode(p, s.createDefaultInstance(), 0, true) == 2,
        "Unexpected extra active / Overforge");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void synthesisHasNoEPGateAndReplacesBothMasteredSourcesOnce(GameTestHelper h) {
    var p = candidate(h);
    // A stored world's older clock must not prolong a previous world's two-tick guard.
    RestrictionData.of(p).putLong("hephaestus_awaken_after", Long.MAX_VALUE);
    var skills = SkillAPI.getSkillsFrom(p);
    h.assertTrue(ForgeSynthesis.ready(p), "300,010 base EP was incorrectly rejected");
    double before = p.getAttributeValue(TensuraAttributes.MAX_MAGICULE),
        price = ForgeSynthesis.cost(p);
    h.assertTrue(ForgeSynthesis.synthesize(p), "Valid synthesis failed");
    var learned = skills.getSkill(HeavenlyRestriction.HEPHAESTUS.get()).orElseThrow();
    h.assertTrue(
        learned.getMastery() == 0 && !learned.isMastered(p) && learned.getParentSkill() == null,
        "Ultimate was not learned/unmastered/independent");
    h.assertTrue(
        skills.getSkill(HeavenlyRestriction.PROSPECTOR.get()).isEmpty()
            && skills.getSkill(UniqueSkills.GODLY_CRAFTSMAN.get()).isEmpty(),
        "Components retained");
    near(
        h,
        p.getAttributeValue(TensuraAttributes.MAX_MAGICULE),
        before - price,
        "One acquisition bill");
    h.assertTrue(!ForgeSynthesis.synthesize(p), "Repeat synthesis succeeded");
    near(
        h,
        p.getAttributeValue(TensuraAttributes.MAX_MAGICULE),
        before - price,
        "Repeat charged again");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void synthesisRejectsMissingMasteryStatusAchievementAndInsufficientCapacity(
      GameTestHelper h) {
    var p = candidate(h);
    var i = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PROSPECTOR.get()).orElseThrow();
    i.setMastery(0);
    h.assertTrue(!ForgeSynthesis.synthesize(p), "Unmastered accepted");
    i.setMastery(i.getMaxMastery());
    TensuraStorages.getExistenceFrom(p).setTrueHero(false);
    h.assertTrue(!ForgeSynthesis.synthesize(p), "Unawakened accepted");
    TensuraStorages.getExistenceFrom(p).setTrueHero(true);
    RestrictionData.of(p).putBoolean("hephaestus_forged_high_tier", false);
    h.assertTrue(!ForgeSynthesis.synthesize(p), "Unforged accepted");
    RestrictionData.of(p).putBoolean("hephaestus_forged_high_tier", true);
    p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(ForgeSynthesis.cost(p));
    h.assertTrue(!ForgeSynthesis.synthesize(p), "Unpayable acquisition accepted");
    h.assertTrue(
        SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PROSPECTOR.get()).isPresent(),
        "Failure ate source skill");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void nativeAwakeningAppliesItsResourceBonusBeforeTheSynthesisBill(
      GameTestHelper h) {
    var p = candidate(h);
    TensuraStorages.getExistenceFrom(p).setTrueHero(false);
    p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(100000);
    RaceHelper.awakening(p, true);
    double after = p.getAttributeValue(TensuraAttributes.MAX_MAGICULE);
    h.assertTrue(after > 100000, "Native awakening did not increase magicules");
    h.assertTrue(!ForgeSynthesis.synthesize(p), "Synthesis ran inside the awakening window");
    near(h, p.getAttributeValue(TensuraAttributes.MAX_MAGICULE), after, "Early charge");
    h.runAfterDelay(
        3,
        () -> {
          h.assertTrue(ForgeSynthesis.synthesize(p), "Post-awakening synthesis unavailable");
          near(
              h,
              p.getAttributeValue(TensuraAttributes.MAX_MAGICULE),
              after - ForgeSynthesis.cost(p),
              "Cost taken before awakening multiplier");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void synthesisKeepsEngravingKnowledgeBothContainersAndPresets(GameTestHelper h) {
    var p = candidate(h);
    var storage = SkillAPI.getSkillsFrom(p);
    var crafts = storage.getSkill(UniqueSkills.GODLY_CRAFTSMAN.get()).orElseThrow();
    var prospect = storage.getSkill(HeavenlyRestriction.PROSPECTOR.get()).orElseThrow();
    var first = new SpatialStorageContainer(54, 256);
    first.setItem(53, new ItemStack(Items.DIAMOND, 100));
    crafts.getOrCreateTag().put("SpatialStorage", first.createTag(p.registryAccess()));
    crafts.getOrCreateTag().putString("KnowledgeProof", "retained");
    var second = new SpatialStorageContainer(20, 128);
    second.setItem(19, new ItemStack(Items.EMERALD, 80));
    prospect.getOrCreateTag().put("SpatialStorage", second.createTag(p.registryAccess()));
    TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, prospect.getSkill(), 0);
    h.assertTrue(ForgeSynthesis.synthesize(p), "Synthesis failed");
    var i = ForgeSynthesis.owned(p).orElseThrow();
    var kept = HeavenlyRestriction.HEPHAESTUS.get().getSpatialStorage(i, p.registryAccess());
    h.assertTrue(
        kept.getItem(53).getCount() == 100
            && i.getOrCreateTag().getString("KnowledgeProof").equals("retained"),
        "Craftsman data lost");
    second.clearContent();
    second.fromTag(i.getOrCreateTag().getList("ProspectorStorage", 10), p.registryAccess());
    h.assertTrue(second.getItem(19).getCount() == 80, "Prospector ingredients lost");
    h.assertTrue(
        TensuraStorages.getAbilityFrom(p).getAbilitySlot(0, 0).getSkill() == i.getSkill(),
        "Preset not migrated");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void genuineCraftingAwardsTheAchievementAndSignedStatsOnlyOnce(GameTestHelper h) {
    var p = smith(h);
    var diamond = new ItemStack(Items.DIAMOND_SWORD);
    diamond.onCraftedBy(p.level(), p, 1);
    h.assertTrue(ForgeItems.own(p, diamond), "Actual craft was not signed");
    near(h, attack(diamond), 8.4, "Forged sword stats");
    double first = attack(diamond);
    diamond.onCraftedBy(p.level(), p, 1);
    near(h, attack(diamond), first, "Repeated craft callback stacked stats");
    h.assertTrue(
        !RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier"),
        "Diamond met high-tier achievement");
    var netherite = new ItemStack(Items.NETHERITE_SWORD);
    netherite.onCraftedBy(p.level(), p, 1);
    h.assertTrue(
        RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier"),
        "Smithing craft hook missed Netherite");
    h.assertTrue(
        diamond.getMaxDamage() == Items.DIAMOND_SWORD.getDefaultInstance().getMaxDamage(),
        "Maximum durability was multiplied");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void fractionalWearAndNativeEPDurabilityAreBothReduced(GameTestHelper h) {
    var p = smith(h);
    var sword = new ItemStack(Items.DIAMOND_SWORD);
    forge(p, sword);
    for (int n = 0; n < 4; n++) sword.hurtAndBreak(1, p.serverLevel(), p, item -> {});
    h.assertTrue(
        sword.getDamageValue() == 2,
        "One-point wear was rounded away or reduction failed: " + sword.getDamageValue());
    sword.set(TensuraDataComponents.EP_DURABILITY.get(), 1000d);
    sword.set(TensuraDataComponents.EP_DURABILITY.get(), 900d);
    near(h, sword.get(TensuraDataComponents.EP_DURABILITY.get()), 950, "Native EP wear");
    sword.set(TensuraDataComponents.EP_DURABILITY.get(), 1000d);
    near(
        h,
        sword.get(TensuraDataComponents.EP_DURABILITY.get()),
        1000,
        "Healing was incorrectly reduced");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void temperingAndRepairConsumeRealMaterialsAndPreserveOrbAndEngravings(
      GameTestHelper h) {
    var p = smith(h);
    var sword = new ItemStack(Items.DIAMOND_SWORD);
    forge(p, sword);
    sword.set(dev.heavenlyrestriction.chromatic.ChromaticContent.INFUSED.get(), true);
    var sharp =
        p.registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.SHARPNESS);
    sword.enchant(sharp, 6);
    sword.setDamageValue(200);
    p.getInventory().setItem(0, sword);
    material(p, 5, Items.DIAMOND, 3);
    double before = TensuraStorages.getExistenceFrom(p).getMagicule();
    h.assertTrue(ForgeService.work(p, 0, "temper", 11), "Keen temper failed");
    near(h, ForgeItems.strength(sword), .2, "Diamond quality");
    h.assertTrue(ForgeItems.temper(sword) == ForgeItems.Temper.KEEN, "Wrong temper");
    h.assertTrue(ForgeService.work(p, 0, "repair", 1), "Repair failed");
    h.assertTrue(
        sword.getDamageValue() < 200
            && EnchantmentHelper.getItemEnchantmentLevel(sharp, sword) == 6
            && dev.heavenlyrestriction.chromatic.ChromaticOrb.infused(sword),
        "Repair reset an engraving or Orb");
    near(h, TensuraStorages.getExistenceFrom(p).getMagicule(), before - 2500, "Forge/repair bill");
    h.assertTrue(p.getInventory().getItem(5).getCount() == 1, "Materials not consumed once");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void masterpieceIsUniqueAcrossItemsAndLedgerRoundTrips(GameTestHelper h) {
    var p = smith(h);
    var a = new ItemStack(Items.DIAMOND_SWORD);
    var b = new ItemStack(Items.NETHERITE_SWORD);
    forge(p, a);
    forge(p, b);
    p.getInventory().setItem(0, a);
    p.getInventory().setItem(1, b);
    material(p, 6, Items.DIAMOND, 4);
    h.assertTrue(ForgeService.work(p, 0, "opus", 1), "First masterpiece failed");
    near(h, attack(a), 10.5, "Masterpiece damage");
    near(h, ForgeItems.wearMultiplier(a), .25, "Masterpiece wear");
    h.assertTrue(ForgeService.work(p, 1, "opus", 1), "Transfer failed");
    h.assertTrue(
        !ForgeItems.masterpiece(a) && ForgeItems.masterpiece(b), "Two active masterpieces");
    near(h, attack(a), 8.4, "Previous piece retained bonus");
    var ledger = ForgeLedger.get(p.getServer());
    var saved = ForgeLedger.load(ledger.snapshot(), p.registryAccess());
    h.assertTrue(saved.snapshot().equals(ledger.snapshot()), "Ledger persistence lost designation");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void arsenalEscrowsActualItemsAndRecallCannotDuplicateThem(GameTestHelper h) {
    var p = smith(h);
    var sword = new ItemStack(Items.DIAMOND_SWORD);
    forge(p, sword);
    var sharp =
        p.registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.SHARPNESS);
    sword.enchant(sharp, 5);
    p.getInventory().setItem(0, sword);
    h.assertTrue(!ForgeArsenal.deploy(p, List.of(0, 0)), "Duplicate slot accepted");
    h.assertTrue(ForgeArsenal.deploy(p, List.of(0)), "Deployment failed");
    h.assertTrue(
        p.getInventory().getItem(0).isEmpty()
            && ForgeArsenal.item(p, 0)
                .getOrDefault(ForgeContent.WORK.get(), new CompoundTag())
                .equals(ForgeItems.data(sword)),
        "Actual weapon not escrowed intact");
    ForgeArsenal.recall(p);
    ForgeArsenal.recall(p);
    int count = 0;
    for (var item : p.getInventory().items)
      if (item.is(Items.DIAMOND_SWORD)) {
        count += item.getCount();
        h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sharp, item) == 5, "Enchant lost");
      }
    h.assertTrue(count == 1 && !ForgeArsenal.active(p), "Recall duplicated or lost item");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void arsenalRejectsEmptyAndUnfundedDeploymentWithoutTouchingInventory(
      GameTestHelper h) {
    var p = smith(h);
    material(p, 0, Items.DIAMOND_SWORD, 1);
    TensuraStorages.getExistenceFrom(p).setMagicule(0);
    h.assertTrue(
        !ForgeArsenal.deploy(p, List.of(0))
            && !ForgeArsenal.deploy(p, List.of())
            && !p.getInventory().getItem(0).isEmpty(),
        "Failed deployment consumed weapon");
    ForgeService.request(p, UUID.randomUUID(), "opus", 0, 1);
    h.assertTrue(
        !ForgeItems.masterpiece(p.getInventory().getItem(0)),
        "Forged client request bypassed session");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 150)
  public static void arsenalActuallyStrikesItsTargetWithTheDeployedSpearEngraving(
      GameTestHelper h) {
    var p = smith(h);
    var spear = new ItemStack(HeavenlyArsenal.SPEAR.get());
    InvertedSpearItem.engrave(spear, p.registryAccess());
    forge(p, spear);
    p.getInventory().setItem(0, spear);
    var t =
        (TrainingDummyEntity)
            BuiltInRegistries.ENTITY_TYPE
                .get(net.minecraft.resources.ResourceLocation.parse("tensura:training_dummy"))
                .create(h.getLevel());
    t.setPos(p.position().add(0, 0, 4));
    t.setNoAi(true);
    h.getLevel().setBlock(t.blockPosition().below(), Blocks.STONE.defaultBlockState(), 3);
    h.getLevel()
        .setBlock(t.blockPosition(), TensuraBlocks.TRAINING_DUMMY.get().defaultBlockState(), 3);
    h.getLevel().addFreshEntity(t);
    p.setLastHurtMob(t);
    h.assertTrue(ForgeArsenal.deploy(p, List.of(0)), "Deployment failed");
    h.startSequence()
        .thenWaitUntil(
            () -> {
              h.assertTrue(t.lastDamageTaken > 0, "Waiting for autonomous slash");
              h.assertTrue(
                  t.hasEffect(HeavenlyArsenal.SKILL_SEAL),
                  "Deployed Nullification engraving did not activate");
            })
        .thenExecute(
            () -> {
              ForgeArsenal.recall(p);
              t.discard();
            })
        .thenSucceed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void masteredSourceCannotBeTemporaryAndSynthesisDoesNotChargeEarly(
      GameTestHelper h) {
    var p = candidate(h);
    var source =
        SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.GODLY_CRAFTSMAN.get()).orElseThrow();
    source.setRemoveTime(100);
    double magicules = p.getAttributeValue(TensuraAttributes.MAX_MAGICULE);
    h.assertTrue(!ForgeSynthesis.synthesize(p), "Temporary source accepted");
    near(
        h,
        p.getAttributeValue(TensuraAttributes.MAX_MAGICULE),
        magicules,
        "Failed synthesis charged");
    h.assertTrue(
        SkillAPI.getSkillsFrom(p)
                .getSkill(HeavenlyRestriction.HEPHAESTUS.get())
                .orElseThrow()
                .getMastery()
            < 0,
        "Learning entry completed accidentally");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void keenTemperAmplifiesActualGuaranteedCriticalDamage(GameTestHelper h) {
    var p = smith(h);
    grant(p, HeavenlyRestriction.CRITICAL_MERCHANT.get()).setToggled(false);
    var crit = dev.heavenlyrestriction.critical.CriticalMerchant.instance(p).orElseThrow();
    dev.heavenlyrestriction.critical.CriticalMerchant.press(crit, p);
    var sword = new ItemStack(Items.DIAMOND_SWORD);
    forge(p, sword);
    p.getInventory().setItem(0, sword);
    p.getInventory().selected = 0;
    var t = dummy(h, p);
    h.runAfterDelay(
        5,
        () -> {
          t.hurt(p.damageSources().playerAttack(p), 20);
          float baseline = t.lastDamageTaken;
          t.invulnerableTime = 0;
          temper(sword, ForgeItems.Temper.KEEN, .2);
          t.hurt(p.damageSources().playerAttack(p), 20);
          near(h, t.lastDamageTaken, baseline * 1.2, "Keen critical bonus");
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void conductorTemperAmplifiesAnActualNativeElementalHit(GameTestHelper h) {
    var p = smith(h);
    var sword = new ItemStack(Items.DIAMOND_SWORD);
    forge(p, sword);
    p.getInventory().setItem(0, sword);
    p.getInventory().selected = 0;
    var t = dummy(h, p);
    h.runAfterDelay(
        5,
        () -> {
          var source = p.damageSources().playerAttack(p);
          ((io.github.manasmods.tensura.damage.TensuraDamageSource) source)
              .tensura$setElement(io.github.manasmods.tensura.ability.magic.Element.WIND);
          t.hurt(source, 20);
          float baseline = t.lastDamageTaken;
          t.invulnerableTime = 0;
          temper(sword, ForgeItems.Temper.CONDUCTOR, .2);
          t.hurt(source, 20);
          near(h, t.lastDamageTaken, baseline * 1.2, "Conductor elemental bonus");
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void piercingRestoresOnlyPartOfArmorMitigationAndEnduringReducesWear(
      GameTestHelper h) {
    var p = smith(h);
    var sword = new ItemStack(Items.DIAMOND_SWORD);
    forge(p, sword);
    p.getInventory().setItem(0, sword);
    p.getInventory().selected = 0;
    var t = target(h, p.position().add(0, 0, 4));
    t.getAttribute(Attributes.ARMOR).setBaseValue(20);
    var armor = new ArrayList<Float>();
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post>
        reductions =
            e -> {
              if (e.getEntity() == t)
                armor.add(
                    e.getReduction(
                        net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction
                            .ARMOR));
            };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(reductions);
    h.runAfterDelay(
        5,
        () -> {
          float hp = t.getHealth();
          t.hurt(p.damageSources().playerAttack(p), 20);
          float baseline = hp - t.getHealth();
          t.setHealth(hp);
          t.invulnerableTime = 0;
          temper(sword, ForgeItems.Temper.PIERCING, .2);
          t.hurt(p.damageSources().playerAttack(p), 20);
          float piercing = hp - t.getHealth();
          h.assertTrue(
              piercing > baseline && piercing <= 20,
              "Piercing ignored too much or no armor: " + baseline + " / " + piercing);
          net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(reductions);
          h.assertTrue(armor.size() == 2, "Missing native armor accounting");
          near(
              h,
              armor.get(1),
              armor.get(0) * .8,
              "Piercing must ignore exactly 20% of armor reduction");
          temper(sword, ForgeItems.Temper.ENDURING, .2);
          near(h, ForgeItems.wearMultiplier(sword), .4, "Enduring multiplier");
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 150)
  public static void deployedVesperRetainsItsWindMarkAndDelayedAfterwindHit(GameTestHelper h) {
    var p = smith(h);
    var sword = dev.heavenlyrestriction.vesper.VesperItem.create(p.registryAccess());
    forge(p, sword);
    p.getInventory().setItem(0, sword);
    var t = dummy(h, p);
    h.assertTrue(ForgeArsenal.deploy(p, List.of(0)), "Vesper deployment failed");
    int[] cuts = {0};
    java.util.function.Consumer<
            net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent>
        listener =
            e -> {
              if (e.getEntity() == t
                  && e.getSource().is(dev.heavenlyrestriction.vesper.Afterwind.DAMAGE)) cuts[0]++;
            };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
    h.runAfterDelay(
        5,
        () -> {
          var source = p.damageSources().playerAttack(p);
          ((io.github.manasmods.tensura.damage.TensuraDamageSource) source)
              .tensura$setElement(io.github.manasmods.tensura.ability.magic.Element.WIND);
          t.hurt(source, 20);
          t.invulnerableTime = 0;
          var blade =
              h.getLevel()
                  .getEntitiesOfClass(ArsenalBlade.class, p.getBoundingBox().inflate(5))
                  .getFirst();
          h.assertTrue(ForgeArsenal.hit(p, t, 0, blade), "Vesper did not land its weapon hit");
        });
    h.runAfterDelay(
        11,
        () -> {
          try {
            h.assertTrue(
                cuts[0] == 1,
                "Delayed Afterwind did not land exactly once with Vesper deployed: "
                    + cuts[0]
                    + " count "
                    + t.damageNumber);
            ForgeArsenal.recall(p);
            t.discard();
            h.succeed();
          } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
          }
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void deployedWeaponOffensiveAttributesReplaceTheHeldWeaponOnlyDuringItsHit(
      GameTestHelper h) {
    var p = smith(h);
    var held = new ItemStack(Items.DIAMOND_SWORD);
    var deployed = new ItemStack(Items.NETHERITE_SWORD);
    var id = HeavenlyRestriction.id("review_wind_boost");
    var mod = new AttributeModifier(id, 2, AttributeModifier.Operation.ADD_VALUE);
    held.set(
        net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
        net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(TensuraAttributes.WIND_BOOST, mod, EquipmentSlotGroup.MAINHAND)
            .build());
    var own = new AttributeModifier(id, 5, AttributeModifier.Operation.ADD_VALUE);
    deployed.set(
        net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
        net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(TensuraAttributes.WIND_BOOST, own, EquipmentSlotGroup.MAINHAND)
            .build());
    p.getInventory().setItem(0, held);
    p.getInventory().selected = 0;
    var attr = p.getAttribute(TensuraAttributes.WIND_BOOST);
    attr.addTransientModifier(mod);
    double before = attr.getValue();
    try (var context = ForgeOffenseContext.open(p, deployed)) {
      near(h, attr.getValue(), before + 3, "Deployed boost");
    }
    near(h, attr.getValue(), before, "Held boost restoration");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void tensuraBenchNormalTakeSignsTheRealEquipment(GameTestHelper h) {
    var p = smith(h);
    var bench = bench(p, Items.DIAMOND_SWORD);
    h.assertTrue(bench.canCraft(), "Bench recipe unavailable");
    var output = bench.getSlot(36).remove(1);
    bench.getSlot(36).onTake(p, output);
    h.assertTrue(ForgeItems.own(p, output), "Tensura bench output was not signed");
    near(h, attack(output), 8.4, "Tensura bench actual crafted bonus");
    h.assertTrue(
        !RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier"),
        "Diamond credited high-tier");
    h.succeed();
  }

  private static io.github.manasmods.tensura.menu.SmithingBenchMenu bench(
      ServerPlayer p, Item result) {
    p.getInventory().clearContent();
    TensuraStorages.getPlayerDataFrom(p)
        .getKnownSchematics()
        .addAll(
            p
                .level()
                .getRecipeManager()
                .getAllRecipesFor(
                    io.github.manasmods.tensura.registry.recipe.TensuraRecipes.SMITHING_BENCH_TYPE
                        .get())
                .stream()
                .flatMap(r -> r.value().getRequiredSchematics().stream())
                .distinct()
                .toList());
    var bench =
        new io.github.manasmods.tensura.menu.SmithingBenchMenu(
            1, p.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL);
    for (int n = 0; n < bench.getRecipes().size(); n++) {
      var recipe = bench.getRecipes().get(n).value();
      if (!recipe.getOutput().is(result)) continue;
      var ingredients = recipe.getIngredients();
      var amounts = recipe.getIngredientAmount();
      for (int i = 0; i < ingredients.size(); i++)
        if (!ingredients.get(i).isEmpty() && amounts.get(i) > 0) {
          var item = ingredients.get(i).getItems()[0].copy();
          item.setCount(amounts.get(i));
          p.getInventory().add(item);
        }
      bench.clickMenuButton(p, n);
      return bench;
    }
    throw new IllegalStateException("Missing bench recipe for " + result);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void tensuraBenchShiftTakeRetainsForgingStatsAndHighTierCredit(GameTestHelper h) {
    var p = smith(h);
    var hihi =
        net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            net.minecraft.resources.ResourceLocation.parse("tensura:hihiirokane_sword"));
    var bench = bench(p, hihi);
    h.assertTrue(
        !RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier"),
        "Preview awarded achievement");
    h.assertTrue(!bench.quickMoveStack(p, 36).isEmpty(), "Tensura quick take failed");
    var crafted = p.getInventory().items.stream().filter(i -> i.is(hihi)).findFirst().orElseThrow();
    h.assertTrue(ForgeItems.own(p, crafted), "Shift-transferred item lost signature");
    h.assertTrue(
        RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier"),
        "Bench take missed high-tier credit");
    near(h, ForgeItems.statBonus(crafted), .2, "Bench forging bonus");
    var ordinary = crafted.copy();
    ordinary.remove(ForgeContent.WORK.get());
    near(h, attack(crafted), attack(ordinary) * 1.2, "Native Hihiirokane attack bonus");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void craftingTableShiftTakeBonusSurvivesTransferAndEquipping(GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    var menu =
        new net.minecraft.world.inventory.CraftingMenu(
            1,
            p.getInventory(),
            net.minecraft.world.inventory.ContainerLevelAccess.create(
                p.level(), p.blockPosition()));
    menu.getSlot(2).set(new ItemStack(Items.DIAMOND));
    menu.getSlot(5).set(new ItemStack(Items.DIAMOND));
    menu.getSlot(8).set(new ItemStack(Items.STICK));
    h.assertTrue(!menu.quickMoveStack(p, 0).isEmpty(), "Real recipe shift craft failed");
    var sword =
        p.getInventory().items.stream()
            .filter(i -> i.is(Items.DIAMOND_SWORD))
            .findFirst()
            .orElseThrow();
    h.assertTrue(ForgeItems.own(p, sword), "Transferred sword unsigned");
    // FakePlayer intentionally skips equipment ticking; use a real tracked living entity.
    var wearer = target(h, p.position().add(2, 0, 0));
    wearer.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1);
    wearer.setItemSlot(EquipmentSlot.MAINHAND, sword);
    h.runAfterDelay(
        5,
        () -> {
          near(
              h, wearer.getAttributeValue(Attributes.ATTACK_DAMAGE), 8.4, "Equipped forged damage");
          wearer.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void individualRecallAndIncrementalDeploymentKeepOtherWeaponsIntact(
      GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    for (int n = 0; n < 3; n++) material(p, n, Items.DIAMOND_SWORD, 1);
    h.assertTrue(ForgeArsenal.deploy(p, List.of(0, 1)), "First deployment failed");
    var retained = ForgeArsenal.item(p, 1).copy();
    h.assertTrue(ForgeArsenal.recall(p, 0), "Individual recall failed");
    h.assertTrue(!ForgeArsenal.recall(p, 0), "Repeated recall duplicated");
    h.assertTrue(
        ForgeArsenal.count(p) == 1 && ItemStack.matches(retained, ForgeArsenal.item(p, 1)),
        "Other weapon changed");
    h.assertTrue(ForgeArsenal.deploy(p, List.of(2)), "Incremental deployment failed");
    h.assertTrue(
        ForgeArsenal.count(p) == 2 && !ForgeArsenal.item(p, 0).isEmpty(), "Escrow hole not reused");
    ForgeArsenal.recall(p);
    int count =
        p.getInventory().items.stream()
            .filter(i -> i.is(Items.DIAMOND_SWORD))
            .mapToInt(ItemStack::getCount)
            .sum();
    h.assertTrue(count == 3, "Individual controls lost or duplicated weapons");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void vanillaSmithingShiftTakePreservesAndEquipsForgedBonus(GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    var menu = new net.minecraft.world.inventory.SmithingMenu(1, p.getInventory());
    menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
    menu.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
    menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
    h.assertTrue(!menu.quickMoveStack(p, 3).isEmpty(), "Vanilla smithing failed");
    var sword =
        p.getInventory().items.stream()
            .filter(i -> i.is(Items.NETHERITE_SWORD))
            .findFirst()
            .orElseThrow();
    near(h, attack(sword), 9.6, "Transferred netherite sword");
    h.assertTrue(
        ForgeItems.own(p, sword) && RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier"),
        "Smithing lost bonus/achievement");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void tensuraBenchArmorBonusChangesEquippedArmorAndToughness(GameTestHelper h) {
    var p = smith(h);
    var bench = bench(p, Items.DIAMOND_CHESTPLATE);
    bench.quickMoveStack(p, 36);
    var armor =
        p.getInventory().items.stream()
            .filter(i -> i.is(Items.DIAMOND_CHESTPLATE))
            .findFirst()
            .orElseThrow();
    var wearer = target(h, p.position().add(2, 0, 0));
    wearer.getAttribute(Attributes.ARMOR).setBaseValue(0);
    wearer.setItemSlot(EquipmentSlot.CHEST, armor);
    h.runAfterDelay(
        5,
        () -> {
          near(h, wearer.getAttributeValue(Attributes.ARMOR), 9.6, "Equipped forged armor");
          near(
              h,
              wearer.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
              2.4,
              "Equipped forged toughness");
          wearer.discard();
          h.succeed();
        });
  }

  private static ItemStack plate(
      ServerPlayer smith, Item item, ForgeItems.Temper type, double quality) {
    var stack = new ItemStack(item);
    ForgeItems.stamp(smith, stack);
    temper(stack, type, quality);
    return stack;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void armorTempersWeightOnlyCorrectlyWornSlotsAndNeverExceedOneSet(
      GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    var helmet = plate(p, Items.DIAMOND_HELMET, ForgeItems.Temper.KEEN, .35);
    var chest = plate(p, Items.DIAMOND_CHESTPLATE, ForgeItems.Temper.KEEN, .35);
    p.setItemSlot(EquipmentSlot.MAINHAND, chest);
    near(h, ForgeTempering.armorPower(p, ForgeItems.Temper.KEEN), 0, "Carried armor activated");
    p.setItemSlot(EquipmentSlot.HEAD, chest);
    near(h, ForgeTempering.armorPower(p, ForgeItems.Temper.KEEN), 0, "Wrong-slot armor activated");
    p.setItemSlot(EquipmentSlot.HEAD, helmet);
    p.setItemSlot(EquipmentSlot.CHEST, chest);
    near(h, ForgeTempering.armorPower(p, ForgeItems.Temper.KEEN), .35 * .55, "Helmet/chest weight");
    p.setItemSlot(
        EquipmentSlot.LEGS, plate(p, Items.DIAMOND_LEGGINGS, ForgeItems.Temper.KEEN, .35));
    p.setItemSlot(EquipmentSlot.FEET, plate(p, Items.DIAMOND_BOOTS, ForgeItems.Temper.KEEN, .35));
    near(
        h,
        ForgeTempering.armorPower(p, ForgeItems.Temper.KEEN),
        .35,
        "Full set stacked four times");
    p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
    near(
        h,
        ForgeTempering.armorPower(p, ForgeItems.Temper.KEEN),
        .35 * .6,
        "Unequipping did not remove contribution");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void keenArmorBuffsBareHandCriticalsWithoutGivingDefensiveOrElementalEffects(
      GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    grant(p, HeavenlyRestriction.CRITICAL_MERCHANT.get()).setToggled(false);
    dev.heavenlyrestriction.critical.CriticalMerchant.press(
        dev.heavenlyrestriction.critical.CriticalMerchant.instance(p).orElseThrow(), p);
    var victim = dummy(h, p);
    h.runAfterDelay(
        5,
        () -> {
          var source = p.damageSources().playerAttack(p);
          victim.hurt(source, 20);
          float baseline = victim.lastDamageTaken;
          victim.invulnerableTime = 0;
          p.setItemSlot(
              EquipmentSlot.CHEST, plate(p, Items.DIAMOND_CHESTPLATE, ForgeItems.Temper.KEEN, .2));
          victim.hurt(source, 20);
          near(h, victim.lastDamageTaken, baseline * 1.08, "Keen armor critical bonus");
          near(
              h,
              ForgeTempering.armorPower(p, ForgeItems.Temper.CONDUCTOR),
              0,
              "Keen also granted elemental protection");
          victim.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void piercingArmorContributesToActualArmorPenetrationAdditivelyWithWeapon(
      GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    var sword = new ItemStack(Items.DIAMOND_SWORD);
    forge(p, sword);
    temper(sword, ForgeItems.Temper.PIERCING, .2);
    p.setItemSlot(EquipmentSlot.MAINHAND, sword);
    var victim = target(h, p.position().add(0, 0, 3));
    victim.getAttribute(Attributes.ARMOR).setBaseValue(20);
    var reductions = new ArrayList<Float>();
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post>
        listener =
            event -> {
              if (event.getEntity() == victim)
                reductions.add(
                    event.getReduction(
                        net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction
                            .ARMOR));
            };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
    h.runAfterDelay(
        5,
        () -> {
          try {
            victim.hurt(p.damageSources().playerAttack(p), 20);
            victim.setHealth(1024);
            victim.invulnerableTime = 0;
            p.setItemSlot(
                EquipmentSlot.CHEST,
                plate(p, Items.DIAMOND_CHESTPLATE, ForgeItems.Temper.PIERCING, .2));
            victim.hurt(p.damageSources().playerAttack(p), 20);
            h.assertTrue(reductions.size() == 2, "Missing mitigation records");
            near(
                h,
                reductions.get(1),
                reductions.get(0) * .72 / .8,
                "Weapon and armor penetration compounded");
            near(
                h,
                ForgeTempering.armorPower(p, ForgeItems.Temper.KEEN),
                0,
                "Piercing also granted critical damage");
            victim.discard();
            h.succeed();
          } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
          }
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void conductorArmorReducesElementalHPHitsButNotPhysicalDamageAndNeedsNoAttacker(
      GameTestHelper h) {
    var p = smith(h);
    var victim = target(h, p.position().add(0, 0, 3));
    var chest = plate(p, Items.DIAMOND_CHESTPLATE, ForgeItems.Temper.NONE, .2);
    victim.setItemSlot(EquipmentSlot.CHEST, chest);
    h.runAfterDelay(
        5,
        () -> {
          float hp = victim.getHealth();
          var fire = victim.damageSources().onFire();
          victim.hurt(fire, 20);
          float baseline = hp - victim.getHealth();
          victim.setHealth(hp);
          victim.invulnerableTime = 0;
          temper(chest, ForgeItems.Temper.CONDUCTOR, .2);
          victim.hurt(fire, 20);
          near(h, hp - victim.getHealth(), baseline * .92, "Environmental elemental protection");
          victim.setHealth(hp);
          victim.invulnerableTime = 0;
          var physical = p.damageSources().playerAttack(p);
          victim.hurt(physical, 20);
          float guarded = hp - victim.getHealth();
          victim.setHealth(hp);
          victim.invulnerableTime = 0;
          temper(chest, ForgeItems.Temper.NONE, .2);
          victim.hurt(physical, 20);
          near(h, hp - victim.getHealth(), guarded, "Conductor reduced physical damage");
          victim.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void
      conductorArmorProtectsNativeDirectSpiritualDamageAndKeenArmorBoostsItsCriticals(
          GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    var victim = target(h, p.position().add(0, 0, 3));
    io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(victim)
        .setRace(io.github.manasmods.tensura.registry.race.TensuraRaces.HUMAN_SAINT.get(), false);
    var energy = TensuraStorages.getExistenceFrom(victim);
    h.runAfterDelay(
        5,
        () -> {
          var source = p.damageSources().playerAttack(p);
          ((io.github.manasmods.tensura.damage.TensuraDamageSource) source)
              .tensura$setElement(io.github.manasmods.tensura.ability.magic.Element.WIND);
          energy.setSpiritualHealth(1000);
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              victim, p, source, 20, 0);
          double baseline = 1000 - energy.getSpiritualHealth();
          h.assertTrue(baseline > 0, "SHP fixture took no damage");
          victim.setItemSlot(
              EquipmentSlot.CHEST,
              plate(p, Items.DIAMOND_CHESTPLATE, ForgeItems.Temper.CONDUCTOR, .2));
          energy.setSpiritualHealth(1000);
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              victim, p, source, 20, 0);
          near(h, 1000 - energy.getSpiritualHealth(), baseline * .92, "Conductor SHP protection");
          grant(p, HeavenlyRestriction.CRITICAL_MERCHANT.get()).setToggled(false);
          dev.heavenlyrestriction.critical.CriticalMerchant.press(
              dev.heavenlyrestriction.critical.CriticalMerchant.instance(p).orElseThrow(), p);
          energy.setSpiritualHealth(1000);
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              victim, p, source, 20, 0);
          double critical = 1000 - energy.getSpiritualHealth();
          p.setItemSlot(
              EquipmentSlot.CHEST, plate(p, Items.DIAMOND_CHESTPLATE, ForgeItems.Temper.KEEN, .2));
          energy.setSpiritualHealth(1000);
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              victim, p, source, 20, 0);
          near(h, 1000 - energy.getSpiritualHealth(), critical * 1.08, "Keen SHP critical bonus");
          victim.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void armorTemperingUsesRealForgeAndEnduringReducesBothWearPools(GameTestHelper h) {
    var p = smith(h);
    p.getInventory().clearContent();
    p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_CHESTPLATE));
    material(p, 5, Items.DIAMOND, 2);
    h.assertTrue(ForgeService.work(p, 0, "temper", 41), "Armor was rejected by forge");
    var chest = p.getInventory().getItem(0);
    near(h, ForgeItems.armorWeight(chest), .4, "Chest weight");
    chest.hurtAndBreak(20, p.serverLevel(), p, b -> {});
    h.assertTrue(chest.getDamageValue() == 8, "Enduring armor wear reduction failed");
    chest.set(TensuraDataComponents.EP_DURABILITY.get(), 1000d);
    chest.set(TensuraDataComponents.EP_DURABILITY.get(), 900d);
    near(h, chest.get(TensuraDataComponents.EP_DURABILITY.get()), 960, "Enduring EP wear");
    h.assertTrue(ForgeService.work(p, 0, "temper", 31), "Replacing armor temper failed");
    h.assertTrue(
        ForgeItems.temper(chest) == ForgeItems.Temper.CONDUCTOR, "Old temper survived replacement");
    near(h, ForgeItems.wearMultiplier(chest), .5, "Replaced Enduring kept wear benefit");
    h.succeed();
  }
}
