package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.ChainedGameTests.near;

import dev.heavenlyrestriction.highrealm.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.entity.monster.*;
import io.github.manasmods.tensura.entity.template.subclass.IDaemon;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.*;
import net.tslat.smartbrainlib.util.BrainUtils;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class HighRealmLoadoutGameTests {
  static Mob mob(ServerLevel l, String name) {
    return (Mob)
        BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("tensura:" + name)).create(l);
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void engorgerReplacesPriorGreatSageWithoutRefillsOrLingeringBonuses(
      GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    var effect =
        io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(
            io.github.manasmods.tensura.registry.effect.TensuraMobEffects.ENGORGEMENT);
    var chantAttribute =
        io.github.manasmods.tensura.registry.attribute.TensuraAttributes.CHANT_SPEED;
    for (String species : List.of("greater_daemon", "lesser_daemon")) {
      var m = mob(l, species);
      m.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
      m.getPersistentData().putInt(HighRealmLoadouts.VERSION_KEY, 4);
      EnergyHelper.setBaseMaxEP(m, 3500000);
      double chant = m.getAttributeValue(chantAttribute),
          scale = m.getAttributeValue(Attributes.SCALE),
          attack = m.getAttributeValue(Attributes.ATTACK_DAMAGE);
      RestrictionData.learnReward(m, UniqueSkills.GREAT_SAGE.get());
      var sage = SkillAPI.getSkillsFrom(m).getSkill(UniqueSkills.GREAT_SAGE.get()).orElseThrow();
      sage.setMastery(sage.getMaxMastery());
      sage.setToggled(true);
      sage.onToggleOn(m);
      TensuraStorages.getAbilityFrom(m).setAbilitySlot(0, 0, UniqueSkills.GREAT_SAGE.get(), 0);
      TensuraStorages.getAbilityFrom(m).setAbilitySlot(0, 1, ExtraSkills.MAGIC_SENSE.get(), 0);
      m.setHealth(37);
      var pool = TensuraStorages.getExistenceFrom(m);
      pool.setAura(123);
      pool.setMagicule(234);
      pool.setSpiritualHealth(12);
      h.assertTrue(HighRealmLoadouts.apply(m), "Old daemon not updated");
      h.assertTrue(
          SkillAPI.getSkillsFrom(m).getSkill(UniqueSkills.GREAT_SAGE.get()).isEmpty(),
          "Replaced Great Sage retained");
      near(h, m.getAttributeValue(chantAttribute), chant, "Great Sage chant bonus retained");
      h.assertTrue(
          TensuraStorages.getAbilityFrom(m).getAbilitySlot(0, 0).getSkill() == null
              && TensuraStorages.getAbilityFrom(m).getAbilitySlot(0, 1).getSkill()
                  == ExtraSkills.MAGIC_SENSE.get(),
          "Preset retirement removed unrelated slot");
      var engorger = SkillAPI.getSkillsFrom(m).getSkill(UniqueSkills.ENGORGER.get()).orElseThrow();
      h.assertTrue(
          engorger.isMastered(m) && engorger.isToggled() && m.hasEffect(effect),
          "Engorger passive inactive");
      h.assertTrue(
          m.getAttributeValue(Attributes.SCALE) > scale
              && m.getAttributeValue(Attributes.ATTACK_DAMAGE) > attack,
          "Engorgement stats missing");
      near(h, m.getHealth(), 37, "Migration healed");
      near(h, pool.getAura(), 123, "Migration refilled aura");
      near(h, pool.getMagicule(), 234, "Migration refilled MP");
      near(h, pool.getSpiritualHealth(), 12, "Migration refilled SHP");
      double enlarged = m.getAttributeValue(Attributes.SCALE);
      h.assertTrue(!HighRealmLoadouts.apply(m), "Migration replayed");
      near(h, m.getAttributeValue(Attributes.SCALE), enlarged, "Scale stacked");
      m.setHealth(37);
      effect.value().applyEffectTick(m, 0);
      h.assertTrue(m.getHealth() > 37, "Native Engorgement does not heal daemon");
      var copy = mob(l, species);
      copy.load(m.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
      h.assertTrue(
          copy.hasEffect(effect)
              && SkillAPI.getSkillsFrom(copy).getSkill(UniqueSkills.ENGORGER.get()).isPresent()
              && !HighRealmLoadouts.apply(copy),
          "Engorger state lost on reload");
      var owned = mob(l, species);
      owned.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
      owned.getPersistentData().putInt(HighRealmLoadouts.VERSION_KEY, 4);
      TensuraStorages.getExistenceFrom(owned).setPermanentOwner(UUID.randomUUID());
      RestrictionData.learnReward(owned, UniqueSkills.GREAT_SAGE.get());
      h.assertTrue(
          !HighRealmLoadouts.apply(owned)
              && SkillAPI.getSkillsFrom(owned).getSkill(UniqueSkills.GREAT_SAGE.get()).isPresent(),
          "Owned daemon altered");
    }
    var arch = mob(l, "arch_daemon");
    HighRealmElites.empower(arch);
    h.assertTrue(
        SkillAPI.getSkillsFrom(arch).getSkill(UniqueSkills.MATHEMATICIAN.get()).isPresent()
            && SkillAPI.getSkillsFrom(arch).getSkill(UniqueSkills.ENGORGER.get()).isEmpty(),
        "Arch signature changed");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void everySpeciesHasAnEnabledNativeUniqueIncludingSavedElites(GameTestHelper h) {
    var signatures =
        Map.ofEntries(
            Map.entry("orc", UniqueSkills.BERSERKER.get()),
            Map.entry("armorsaurus", UniqueSkills.SURVIVOR.get()),
            Map.entry("horned_bear", UniqueSkills.FIGHTER.get()),
            Map.entry("direwolf", UniqueSkills.SHADOW_STRIKER.get()),
            Map.entry("blade_tiger", UniqueSkills.SEER.get()),
            Map.entry("knight_spider", UniqueSkills.OPPRESSOR.get()),
            Map.entry("lizardman", UniqueSkills.MARTIAL_MASTER.get()),
            Map.entry("basilisk", UniqueSkills.SURVIVOR.get()),
            Map.entry("greater_daemon", UniqueSkills.ENGORGER.get()),
            Map.entry("arch_daemon", UniqueSkills.MATHEMATICIAN.get()),
            Map.entry("lesser_daemon", UniqueSkills.ENGORGER.get()));
    var l = HighRealmGameTests.realm(h);
    for (var entry : signatures.entrySet()) {
      var m = mob(l, entry.getKey());
      // Simulate a 1.10.3 saved elite, with final form already applied.
      m.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
      m.getPersistentData().putInt(HighRealmLoadouts.VERSION_KEY, 3);
      m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
      EnergyHelper.setBaseMaxEP(m, 3500000);
      HighRealmEvolutions.apply(m);
      var pool = TensuraStorages.getExistenceFrom(m);
      m.setHealth(37);
      pool.setAura(123);
      pool.setMagicule(234);
      pool.setSpiritualHealth(12);
      double ep = EnergyHelper.getBaseMaxEP(m);
      h.assertTrue(HighRealmLoadouts.apply(m), "Unique migration skipped " + entry.getKey());
      var unique = SkillAPI.getSkillsFrom(m).getSkill(entry.getValue()).orElseThrow();
      h.assertTrue(unique.isMastered(m), "Signature not mastered " + entry.getKey());
      if (unique.canBeToggled(m))
        h.assertTrue(unique.isToggled(), "Disabled signature " + entry.getKey());
      near(h, m.getHealth(), 37, "Unique migration healed");
      near(h, pool.getAura(), 123, "Unique migration refilled aura");
      near(h, pool.getMagicule(), 234, "Unique migration refilled MP");
      near(h, pool.getSpiritualHealth(), 12, "Unique migration refilled SHP");
      h.assertTrue(EnergyHelper.getBaseMaxEP(m) >= ep, "Unique acquisition consumed EP");
      double chant =
          m.getAttributeValue(
              io.github.manasmods.tensura.registry.attribute.TensuraAttributes.CHANT_SPEED);
      h.assertTrue(!HighRealmLoadouts.apply(m), "Unique migration repeated");
      near(
          h,
          m.getAttributeValue(
              io.github.manasmods.tensura.registry.attribute.TensuraAttributes.CHANT_SPEED),
          chant,
          "Replayed chant multiplier");
      if (entry.getValue() == UniqueSkills.FIGHTER.get()
          || entry.getValue() == UniqueSkills.BERSERKER.get()
          || entry.getValue() == UniqueSkills.MARTIAL_MASTER.get())
        h.assertTrue(
            TensuraStorages.getAbilityFrom(m).getAbilitySlot(0, 0).getSkill() == entry.getValue(),
            "In-slot Unique unequipped");
      if (entry.getValue() == UniqueSkills.MATHEMATICIAN.get()) {
        unique.setToggled(false);
        unique.onToggleOff(m);
        h.assertTrue(
            chant
                > m.getAttributeValue(
                    io.github.manasmods.tensura.registry.attribute.TensuraAttributes.CHANT_SPEED),
            "Caster Unique does not improve chanting");
      }
      var restored = mob(l, entry.getKey());
      restored.load(m.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
      h.assertTrue(
          restored != null
              && SkillAPI.getSkillsFrom(restored).getSkill(entry.getValue()).isPresent(),
          "Unique missing on reload");
    }
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void newUniquePassivesAffectRealMobCombatHooks(GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    var wolf = mob(l, "direwolf");
    HighRealmElites.empower(wolf);
    var dodge =
        wolf.getAttribute(
            io.github.manasmods.tensura.registry.attribute.TensuraAttributes.DODGE_STRENGTH);
    h.assertTrue(
        dodge.getModifier(ResourceLocation.parse("tensura:shadow_striker")) != null,
        "Wolf acceleration/dodge inactive");
    var arch = mob(l, "arch_daemon");
    HighRealmElites.empower(arch);
    h.assertTrue(
        arch.getAttribute(
                    io.github.manasmods.tensura.registry.attribute.TensuraAttributes
                        .RESISTANCE_DEGRADATION)
                .getModifier(ResourceLocation.parse("tensura:mathematician"))
            != null,
        "Arch degradation inactive");
    var spider = mob(l, "knight_spider");
    HighRealmElites.empower(spider);
    var oppressor =
        SkillAPI.getSkillsFrom(spider).getSkill(UniqueSkills.OPPRESSOR.get()).orElseThrow();
    var arrow =
        new net.minecraft.world.entity.projectile.Arrow(
            l,
            wolf,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),
            null);
    h.assertTrue(
        !oppressor
            .getSkill()
            .onBeingDamaged(oppressor, spider, spider.damageSources().arrow(arrow, wolf), 100),
        "Spider arrow immunity inactive");
    var dragon = mob(l, "lizardman");
    HighRealmElites.empower(dragon);
    var martial =
        SkillAPI.getSkillsFrom(dragon).getSkill(UniqueSkills.MARTIAL_MASTER.get()).orElseThrow();
    var amount = io.github.manasmods.manascore.network.api.util.Changeable.of(100f);
    martial
        .getSkill()
        .onDamageEntity(martial, dragon, wolf, dragon.damageSources().mobAttack(dragon), amount);
    h.assertTrue(amount.get() > 100, "Dragonewt slotted physical boost inactive");
    var basilisk = mob(l, "basilisk");
    HighRealmElites.empower(basilisk);
    var survivor =
        SkillAPI.getSkillsFrom(basilisk).getSkill(UniqueSkills.SURVIVOR.get()).orElseThrow();
    h.assertTrue(
        ((io.github.manasmods.tensura.ability.skill.unique.SurvivorSkill) survivor.getSkill())
            .getImmuneEffects(survivor, basilisk)
            .contains(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
        "Basilisk native survival immunities missing");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void realmUsesUpgradedCasterTypesAndHighestNativeNonBossForms(GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    h.assertTrue(
        !BuiltInRegistries.ENTITY_TYPE
            .get(ResourceLocation.parse("tensura:lesser_daemon"))
            .is(HighRealmElites.TYPES),
        "Lesser Daemon remains in pool");
    var arch = mob(l, "arch_daemon");
    h.assertTrue(HighRealmElites.empower(arch), "Arch Daemon rejected as boss");
    h.assertTrue(
        HighRealmLoadouts.forMob(arch).spells().size() == 8, "Arch spell package mismatched");
    for (String species : List.of("orc", "direwolf", "lizardman")) {
      var m = mob(l, species);
      h.assertTrue(HighRealmElites.empower(m), "Evolved species failed");
      var evolution = (io.github.manasmods.tensura.entity.template.subclass.INameEvolution) m;
      h.assertTrue(
          evolution.getCurrentEvolutionState() == evolution.getMaxEvolutionState(),
          "Incomplete native evolution " + species);
      if (m instanceof DirewolfEntity wolf)
        h.assertTrue(
            wolf.isStar()
                && wolf.getVariant()
                    == io.github.manasmods.tensura.entity.variant.DirewolfVariant.TEMPEST_STAR_WOLF,
            "Wolf not highest star branch");
      if (m instanceof LizardmanEntity dragonewt)
        h.assertTrue(
            dragonewt.isDragonewt()
                && dragonewt.canFly()
                && SkillAPI.getSkillsFrom(m).getSkill(IntrinsicSkills.DRAGON_EYE.get()).isPresent(),
            "Dragonewt lacks native abilities");
      double scale = m.getAttributeValue(Attributes.SCALE),
          attack = m.getAttributeValue(Attributes.ATTACK_DAMAGE);
      h.assertTrue(!HighRealmEvolutions.apply(m), "Evolution replayed");
      near(h, m.getAttributeValue(Attributes.SCALE), scale, "Evolution grew twice");
      near(h, m.getAttributeValue(Attributes.ATTACK_DAMAGE), attack, "Evolution damage compounded");
      var tag = new net.minecraft.nbt.CompoundTag();
      m.saveWithoutId(tag);
      var copy = mob(l, species);
      copy.load(tag);
      h.assertTrue(
          ((io.github.manasmods.tensura.entity.template.subclass.INameEvolution) copy)
                      .getCurrentEvolutionState()
                  == evolution.getMaxEvolutionState()
              && !HighRealmEvolutions.apply(copy),
          "Evolution lost on reload");
    }
    for (String boss : List.of("orc_lord", "orc_disaster"))
      h.assertTrue(!HighRealmElites.empower(mob(l, boss)), "Boss added via evolution");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void savedWildFormsEvolveWithoutRefillsAndOwnedFormsStayUnchanged(
      GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    for (String species : List.of("orc", "direwolf", "lizardman")) {
      var m = mob(l, species);
      m.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
      m.getPersistentData().putInt(HighRealmLoadouts.VERSION_KEY, 2);
      m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
      m.setHealth(37);
      EnergyHelper.setBaseMaxEP(m, 3500000);
      var pool = TensuraStorages.getExistenceFrom(m);
      pool.setAura(123);
      pool.setMagicule(234);
      pool.setSpiritualHealth(12);
      HighRealmLoadouts.apply(m);
      near(h, m.getHealth(), 37, "Evolving healed HP");
      near(h, pool.getAura(), 123, "Evolving refilled aura");
      near(h, pool.getMagicule(), 234, "Evolving refilled MP");
      near(h, pool.getSpiritualHealth(), 12, "Evolving refilled SHP");
      h.assertTrue(EnergyHelper.getBaseMaxEP(m) >= 3500000, "Native evolution cost removed EP");
      var owned = mob(l, species);
      TensuraStorages.getExistenceFrom(owned).setPermanentOwner(UUID.randomUUID());
      h.assertTrue(!HighRealmEvolutions.apply(owned), "Owned form evolved");
    }
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void defenseRevisionUsesOneFighterAndThemedNullificationsAndBoosts(
      GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    int fighters = 0;
    for (var holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(HighRealmElites.TYPES)) {
      var m = (Mob) holder.value().create(l);
      HighRealmElites.empower(m);
      var plan = HighRealmLoadouts.forMob(m);
      h.assertTrue(plan.skills().size() >= 11, "Expanded package too small " + holder);
      h.assertTrue(
          plan.skills().stream()
              .anyMatch(k -> k.getRegistryName().getPath().endsWith("_nullification")),
          "Missing signature nullification " + holder);
      h.assertTrue(
          SkillAPI.getSkillsFrom(m)
              .getSkill(ResistanceSkills.SPIRITUAL_ATTACK_RESISTANCE.get())
              .isPresent(),
          "Missing SHP defense");
      if (plan.skills().contains(UniqueSkills.FIGHTER.get())) {
        fighters++;
        h.assertTrue(
            BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath().equals("horned_bear"),
            "Fighter shared outside brawler");
      }
      h.assertTrue(
          plan.skills().contains(CommonSkills.STRENGTH.get())
              || plan.skills().contains(ExtraSkills.STEEL_STRENGTH.get())
              || plan.skills().contains(ExtraSkills.MANA_MANIPULATION.get()),
          "Missing damage boost " + holder);
    }
    h.assertTrue(fighters == 1, "Fighter distribution incorrect");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 100)
  public static void revisionReplacesFighterAndPromotesDefensesWithoutRefilling(GameTestHelper h) {
    for (String species : List.of("blade_tiger", "lizardman")) {
      var m = mob(HighRealmGameTests.realm(h), species);
      m.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
      m.getPersistentData().putInt(HighRealmLoadouts.VERSION_KEY, 1);
      m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
      m.setHealth(37);
      EnergyHelper.setBaseMaxEP(m, 3500000);
      var pool = TensuraStorages.getExistenceFrom(m);
      pool.setAura(123);
      pool.setMagicule(234);
      pool.setSpiritualHealth(12);
      RestrictionData.learnReward(m, UniqueSkills.FIGHTER.get());
      var old = SkillAPI.getSkillsFrom(m).getSkill(UniqueSkills.FIGHTER.get()).orElseThrow();
      old.setToggled(true);
      old.onToggleOn(m);
      TensuraStorages.getAbilityFrom(m).setAbilitySlot(0, 0, UniqueSkills.FIGHTER.get(), 0);
      TensuraStorages.getAbilityFrom(m).setAbilitySlot(0, 1, ExtraSkills.MAGIC_SENSE.get(), 0);
      RestrictionData.learnReward(m, ResistanceSkills.WATER_ATTACK_RESISTANCE.get());
      HighRealmLoadouts.apply(m);
      h.assertTrue(
          SkillAPI.getSkillsFrom(m).getSkill(UniqueSkills.FIGHTER.get()).isEmpty()
              && TensuraStorages.getAbilityFrom(m).getAbilitySlot(0, 0).getSkill()
                  == (species.equals("lizardman") ? UniqueSkills.MARTIAL_MASTER.get() : null),
          "Retired Fighter retained");
      h.assertTrue(
          TensuraStorages.getAbilityFrom(m).getAbilitySlot(0, 1).getSkill()
              == ExtraSkills.MAGIC_SENSE.get(),
          "Unrelated preset erased");
      if (species.equals("lizardman"))
        h.assertTrue(
            SkillAPI.getSkillsFrom(m)
                .getSkill(ResistanceSkills.WATER_ATTACK_NULLIFICATION.get())
                .isPresent(),
            "Water nullification missing after promotion");
      near(h, m.getHealth(), 37, "Migration healed HP");
      near(h, pool.getAura(), 123, "Migration refilled aura");
      near(h, pool.getMagicule(), 234, "Migration refilled MP");
      near(h, pool.getSpiritualHealth(), 12, "Migration refilled SHP");
      h.assertTrue(!HighRealmLoadouts.apply(m), "Migration replayed");
    }
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 100)
  public static void fiveMillionEntryMigratesOldDefaultAndRetainsCustomChoices(GameTestHelper h) {
    h.assertTrue(
        HighRealmConfig.ENTRY_EP.get() == 5000000d, "Existing default did not migrate to 5M");
    double original = HighRealmConfig.ENTRY_EP.get();
    try {
      var d = new RealmData();
      HighRealmConfig.ENTRY_EP.set(10000000d);
      HighRealmConfig.upgradeLegacyDefault(d);
      near(h, HighRealmConfig.ENTRY_EP.get(), 5000000, "Old 10M setting not lowered");
      var restored =
          RealmData.load(
              d.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess()),
              h.getLevel().registryAccess());
      h.assertTrue(restored.balanceVersion == 2, "Migration marker not persisted");
      HighRealmConfig.ENTRY_EP.set(10000000d);
      HighRealmConfig.upgradeLegacyDefault(restored);
      near(h, HighRealmConfig.ENTRY_EP.get(), 10000000, "Later custom 10M choice overwritten");
      HighRealmConfig.ENTRY_EP.set(20000000d);
      HighRealmConfig.upgradeLegacyDefault(new RealmData());
      near(h, HighRealmConfig.ENTRY_EP.get(), 20000000, "Existing custom threshold overwritten");
    } finally {
      HighRealmConfig.ENTRY_EP.set(original);
    }
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 100)
  public static void eliteAlertTargetsHigherEpPlayersButNeverCreativeOrOwned(GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    var core = new BlockPos(128, 210, 128);
    for (int x = -4; x <= 8; x++)
      for (int z = -4; z <= 4; z++)
        for (int y = 0; y < 7; y++)
          l.setBlock(core.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
    var p =
        net.neoforged.neoforge.common.util.FakePlayerFactory.get(
            l, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "RealmIntruder"));
    p.setPos(128.5, 210, 128.5);
    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    EnergyHelper.setBaseMaxEP(p, 20000000);
    l.addNewPlayer(p);
    try {
      var m = mob(l, "orc");
      m.setPos(134.5, 210, 128.5);
      HighRealmElites.empower(m);
      HighRealmAggro.update(m);
      h.assertTrue(
          m.getTarget() == p && BrainUtils.getTargetOfEntity(m) == p,
          "Elite did not alert to stronger player");
      p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
      HighRealmAggro.update(m);
      h.assertTrue(
          m.getTarget() == null && BrainUtils.getTargetOfEntity(m) == null,
          "Creative target retained");
      p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
      TensuraStorages.getExistenceFrom(m).setPermanentOwner(p.getUUID());
      HighRealmAggro.update(m);
      h.assertTrue(m.getTarget() == null, "Owned mob made hostile");
    } finally {
      l.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void everySpawnIsTensuraWithMasteredUsableSpeciesLoadout(GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    int count = 0;
    for (var holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(HighRealmElites.TYPES)) {
      h.assertTrue(
          BuiltInRegistries.ENTITY_TYPE.getKey(holder.value()).getNamespace().equals("tensura"),
          "Vanilla spawn remains");
      var m = (Mob) holder.value().create(l);
      h.assertTrue(HighRealmElites.empower(m), "Profile rejected " + holder);
      var plan = HighRealmLoadouts.forMob(m);
      h.assertTrue(plan != null && plan.skills().size() >= 9, "Sparse loadout " + holder);
      count++;
      for (var skill : plan.skills()) {
        var i = SkillAPI.getSkillsFrom(m).getSkill(skill).orElseThrow();
        h.assertTrue(
            i.getMastery() == i.getMaxMastery(), "Unmastered skill " + skill.getRegistryName());
        if (i.canBeToggled(m))
          h.assertTrue(i.isToggled(), "Disabled passive " + skill.getRegistryName());
      }
      for (var spell : plan.spells())
        h.assertTrue(
            ((IDaemon) m).getMagic(m, spell) != null,
            "Native caster cannot access spell " + spell.getRegistryName());
      if (plan.skills().contains(UniqueSkills.FIGHTER.get()))
        h.assertTrue(
            TensuraStorages.getAbilityFrom(m).getAbilitySlot(0, 0).getSkill()
                == UniqueSkills.FIGHTER.get(),
            "Fighter not equipped");
    }
    h.assertTrue(count == 10, "Unexpected pool size " + count);
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 100)
  public static void existingElitesGainSkillsWithoutRefillsOrStatGrowth(GameTestHelper h) {
    var m = mob(HighRealmGameTests.realm(h), "orc");
    m.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
    m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
    m.setHealth(37);
    EnergyHelper.setBaseMaxEP(m, 5000000);
    var pool = TensuraStorages.getExistenceFrom(m);
    pool.setAura(123);
    pool.setMagicule(234);
    pool.setSpiritualHealth(12);
    HighRealmLoadouts.apply(m);
    h.assertTrue(
        SkillAPI.getSkillsFrom(m).getSkill(UniqueSkills.BERSERKER.get()).isPresent(),
        "Saved elite not upgraded");
    near(h, m.getHealth(), 37, "Migration healed");
    near(h, pool.getAura(), 123, "Migration refilled aura");
    near(h, pool.getMagicule(), 234, "Migration refilled magicules");
    near(h, pool.getSpiritualHealth(), 12, "Migration refilled SHP");
    double attack = m.getAttributeValue(Attributes.ATTACK_DAMAGE);
    h.assertTrue(!HighRealmLoadouts.apply(m), "Upgrade repeated");
    near(h, m.getAttributeValue(Attributes.ATTACK_DAMAGE), attack, "Modifiers stacked");
    var owned = mob((ServerLevel) m.level(), "orc");
    owned.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
    TensuraStorages.getExistenceFrom(owned).setPermanentOwner(UUID.randomUUID());
    h.assertTrue(!HighRealmLoadouts.apply(owned), "Owned mob modified");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 200)
  public static void realmSurfaceRulesPermitNativeMobsButPreserveObstructionsAndOtherWorlds(
      GameTestHelper h) {
    var l = HighRealmGameTests.realm(h);
    var m = mob(l, "lizardman");
    var at = new BlockPos(160, 200, 160);
    m.setPos(at.getX() + .5, at.getY(), at.getZ() + .5);
    for (int x = -3; x <= 3; x++)
      for (int z = -3; z <= 3; z++) {
        l.setBlock(at.offset(x, -1, z), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        for (int y = 0; y < 6; y++)
          l.setBlock(at.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        l.setBlock(at.offset(x, 6, z), Blocks.STONE.defaultBlockState(), 2);
      }
    var position = new MobSpawnEvent.PositionCheck(m, l, MobSpawnType.NATURAL, null);
    NeoForge.EVENT_BUS.post(position);
    h.assertTrue(
        position.getResult() == MobSpawnEvent.PositionCheck.Result.SUCCEED,
        "Lizardman still limited to below Y70 caves");
    l.setBlock(at, Blocks.STONE.defaultBlockState(), 2);
    var blocked = new MobSpawnEvent.PositionCheck(m, l, MobSpawnType.NATURAL, null);
    NeoForge.EVENT_BUS.post(blocked);
    h.assertTrue(
        blocked.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL,
        "Obstructed spawn accepted");
    var other =
        new MobSpawnEvent.SpawnPlacementCheck(
            m.getType(), h.getLevel(), MobSpawnType.NATURAL, at, l.random, false);
    NeoForge.EVENT_BUS.post(other);
    h.assertTrue(
        other.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT,
        "Overworld spawn rules changed");
    l.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
    h.runAfterDelay(
        10,
        () -> {
          boolean success = false;
          for (int n = 0; n < 24; n++) {
            var e =
                new MobSpawnEvent.SpawnPlacementCheck(
                    m.getType(), l, MobSpawnType.NATURAL, at, l.random, false);
            NeoForge.EVENT_BUS.post(e);
            success |= e.getPlacementCheckResult();
          }
          h.assertTrue(success, "Realm placement rule still rejects moss surface");
          h.succeed();
        });
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 500)
  public static void upgradedGreaterDaemonBrainActuallyCastsLearnedCombatMagic(GameTestHelper h) {
    fight(h, "greater_daemon", 224);
  }

  @GameTest(template = "empty", templateNamespace = "heavenlyrestriction", timeoutTicks = 500)
  public static void archDaemonBrainActuallyCastsLearnedCombatMagic(GameTestHelper h) {
    fight(h, "arch_daemon", 288);
  }

  private static void fight(GameTestHelper h, String species, int x) {
    var l = HighRealmGameTests.realm(h);
    var at = new BlockPos(x, 210, 224);
    for (int dx = -8; dx <= 24; dx++)
      for (int z = -8; z <= 8; z++) {
        l.setBlock(at.offset(dx, -1, z), Blocks.STONE.defaultBlockState(), 2);
        for (int y = 0; y < 9; y++)
          l.setBlock(at.offset(dx, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
    for (int dx = -1; dx <= 1; dx++)
      for (int dz = -1; dz <= 1; dz++)
        l.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, true);
    var mage = mob(l, species);
    mage.setPos(at.getX() + .5, at.getY(), at.getZ() + .5);
    var projectiles = new java.util.concurrent.atomic.AtomicInteger();
    var circles = new java.util.concurrent.atomic.AtomicInteger();
    java.util.function.Consumer<EntityJoinLevelEvent> watch =
        e -> {
          if (e.getEntity() instanceof Projectile projectile && projectile.getOwner() == mage) {
            if (projectile.getClass().getPackageName().contains("projectile.magic"))
              projectiles.incrementAndGet();
            if (projectile instanceof io.github.manasmods.tensura.entity.magic.MagicCircle circle
                && circle.getMpCost() > 0) circles.incrementAndGet();
          }
        };
    NeoForge.EVENT_BUS.addListener(watch);
    mage.setNoGravity(true);
    mage.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
    mage.getPersistentData().putBoolean(HighRealmElites.CANDIDATE, true);
    l.addFreshEntity(mage);
    var target = EntityType.PIG.create(l);
    target.setPos(at.getX() + 14.5, at.getY(), at.getZ() + .5);
    target.setNoAi(true);
    target.setNoGravity(true);
    target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
    target.setHealth(1024);
    l.addFreshEntity(target);
    mage.setLastHurtByMob(target);
    mage.setTarget(target);
    BrainUtils.setTargetOfEntity(mage, target);
    double initial = TensuraStorages.getExistenceFrom(mage).getMagicule();
    h.succeedWhen(
        () -> {
          h.assertTrue(
              projectiles.get() > 0,
              "Native brain has not spawned a magic projectile; age="
                  + mage.tickCount
                  + ", targetAlive="
                  + target.isAlive()
                  + ", circles="
                  + circles.get());
          h.assertTrue(circles.get() > 0, "Native casting circle with magicule cost missing");
          NeoForge.EVENT_BUS.unregister(watch);
          mage.discard();
          target.discard();
        });
  }
}
