package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.exponentiation.ExponentiationContext;
import dev.heavenlyrestriction.highrealm.boss.*;
import dev.heavenlyrestriction.vesper.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vesperreview")
@PrefixGameTestTemplate(false)
public final class VesperGameTests {
  private static ServerPlayer wielder(GameTestHelper h) {
    var p = player(h);
    p.setItemSlot(EquipmentSlot.MAINHAND, VesperItem.create(p.registryAccess()));
    p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(100);
    return p;
  }

  private static net.minecraft.world.entity.monster.Husk enemy(GameTestHelper h, ServerPlayer p) {
    var t = target(h, p.position().add(0, 0, 3));
    t.getAttribute(Attributes.ARMOR).setBaseValue(0);
    return t;
  }

  private static DamageSource wind(ServerPlayer p) {
    var source =
        new DamageSource(
            p.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.MAGIC),
            p);
    ((TensuraDamageSource) source).tensura$setElement(Element.WIND);
    return source;
  }

  private static double stat(
      ItemStack stack,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      double base) {
    double[] result = {base};
    stack.forEachModifier(
        EquipmentSlot.MAINHAND,
        (a, m) -> {
          if (a.equals(attribute)) {
            if (m.operation()
                != net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
              throw new IllegalStateException("Unexpected reference modifier");
            result[0] += m.amount();
          }
        });
    return result[0];
  }

  private static void near(GameTestHelper h, double actual, double expected, String message) {
    h.assertTrue(Math.abs(actual - expected) < .001, message + ": " + actual + " / " + expected);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void vesperIsSlightlyStrongerThanNightRoseWithoutRequiringItsAddon(
      GameTestHelper h) {
    var stack = VesperItem.create(h.getLevel().registryAccess());
    near(h, stat(stack, Attributes.ATTACK_DAMAGE, 1), 96, "Vesper baseline damage");
    near(h, stat(stack, Attributes.ATTACK_SPEED, 4), 1.7, "Vesper attack speed");
    near(h, stat(stack, Attributes.ENTITY_INTERACTION_RANGE, 0), 1, "Vesper extra reach");
    h.assertTrue(stack.getMaxDamage() == 3960, "Incorrect actual durability");
    var key = ResourceLocation.parse("tensura_neb:night_rose");
    if (BuiltInRegistries.ITEM.containsKey(key)) {
      var reference = new ItemStack(BuiltInRegistries.ITEM.get(key));
      double damage = stat(reference, Attributes.ATTACK_DAMAGE, 1),
          speed = stat(reference, Attributes.ATTACK_SPEED, 4);
      System.out.println(
          "Vesper/Night Rose actual comparison: damage=96/"
              + damage
              + ", speed=1.7/"
              + speed
              + ", durability="
              + stack.getMaxDamage()
              + "/"
              + reference.getMaxDamage());
      h.assertTrue(96 > damage && 96 / damage <= 1.1, "Damage exceeds slight upgrade budget");
      near(h, 1.7, speed, "Reference speed mismatch");
      near(
          h,
          stat(stack, Attributes.ENTITY_INTERACTION_RANGE, 0),
          stat(reference, Attributes.ENTITY_INTERACTION_RANGE, 0),
          "Reference reach mismatch");
      h.assertTrue(
          stack.getMaxDamage() > reference.getMaxDamage()
              && stack.getMaxDamage() / (double) reference.getMaxDamage() <= 1.15,
          "Durability exceeds slight upgrade budget");
    }
    h.assertTrue(
        stack.is(net.minecraft.tags.ItemTags.SWORDS) && Afterwind.level(wielder(h)) == 1,
        "Native sword tag/default engraving missing");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 160)
  public static void realWindThenSwordDealsOneDelayedNativeWindHitAndCannotRearmItself(
      GameTestHelper h) {
    var p = wielder(h);
    var t = enemy(h, p);
    var seen = new ArrayList<Float>();
    java.util.function.Consumer<LivingIncomingDamageEvent> observer =
        e -> {
          if (e.getEntity() == t && e.getSource().is(Afterwind.DAMAGE)) {
            h.assertTrue(
                ((TensuraDamageSource) e.getSource()).tensura$getElement() == Element.WIND,
                "Aftercut lost Wind element");
            seen.add(e.getAmount());
          }
        };
    NeoForge.EVENT_BUS.addListener(observer);
    h.runAfterDelay(
        5,
        () -> {
          double attack;
          try (var raw = ExponentiationContext.raw(p, ExponentiationContext.DAMAGE)) {
            attack = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
          }
          h.assertTrue(t.hurt(wind(p), 5), "Wind did not land");
          t.invulnerableTime = 0;
          h.assertTrue(t.hurt(p.damageSources().playerAttack(p), 15), "Sword did not land");
          h.assertTrue(seen.isEmpty(), "Aftercut was not delayed");
          h.runAfterDelay(
              7,
              () -> {
                h.assertTrue(seen.size() == 1, "Aftercut did not land exactly once");
                near(h, seen.getFirst(), attack * .2, "Aftercut used amplified primary damage");
                t.invulnerableTime = 0;
                t.hurt(wind(p), 5);
                t.invulnerableTime = 0;
                t.hurt(p.damageSources().playerAttack(p), 15);
                h.runAfterDelay(
                    Afterwind.COOLDOWN_TICKS + 3,
                    () -> {
                      t.invulnerableTime = 0;
                      t.hurt(p.damageSources().playerAttack(p), 15);
                      h.runAfterDelay(
                          7,
                          () -> {
                            h.assertTrue(
                                seen.size() == 1, "Cooldown or recursive wind rearming failed");
                            NeoForge.EVENT_BUS.unregister(observer);
                            t.discard();
                            h.succeed();
                          });
                    });
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 180)
  public static void unprimedAndExpiredMarksDoNotProduceAnAftercut(GameTestHelper h) {
    var p = wielder(h);
    var t = enemy(h, p);
    int[] hits = {0};
    java.util.function.Consumer<LivingIncomingDamageEvent> observer =
        e -> {
          if (e.getEntity() == t && e.getSource().is(Afterwind.DAMAGE)) hits[0]++;
        };
    NeoForge.EVENT_BUS.addListener(observer);
    h.runAfterDelay(
        5,
        () -> {
          t.hurt(p.damageSources().playerAttack(p), 10);
          h.runAfterDelay(
              7,
              () -> {
                h.assertTrue(hits[0] == 0, "Ordinary unprimed hit caused aftercut");
                t.invulnerableTime = 0;
                t.hurt(wind(p), 5);
                h.runAfterDelay(
                    Afterwind.MARK_TICKS + 2,
                    () -> {
                      t.invulnerableTime = 0;
                      t.hurt(p.damageSources().playerAttack(p), 10);
                      h.runAfterDelay(
                          7,
                          () -> {
                            h.assertTrue(hits[0] == 0, "Expired mark caused aftercut");
                            NeoForge.EVENT_BUS.unregister(observer);
                            t.discard();
                            h.succeed();
                          });
                    });
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void immuneHitsDoNotPrimeAndSwitchingWeaponsCancelsAPendingCut(GameTestHelper h) {
    var p = wielder(h);
    var t = enemy(h, p);
    int[] hits = {0};
    java.util.function.Consumer<LivingIncomingDamageEvent> observer =
        e -> {
          if (e.getEntity() == t && e.getSource().is(Afterwind.DAMAGE)) hits[0]++;
        };
    NeoForge.EVENT_BUS.addListener(observer);
    h.runAfterDelay(
        5,
        () -> {
          t.setInvulnerable(true);
          t.hurt(wind(p), 5);
          t.setInvulnerable(false);
          t.invulnerableTime = 0;
          t.hurt(p.damageSources().playerAttack(p), 10);
          h.runAfterDelay(
              7,
              () -> {
                h.assertTrue(hits[0] == 0, "Immune wind charged the engraving");
                t.invulnerableTime = 0;
                t.hurt(wind(p), 5);
                t.invulnerableTime = 0;
                t.hurt(p.damageSources().playerAttack(p), 10);
                p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                h.runAfterDelay(
                    7,
                    () -> {
                      h.assertTrue(hits[0] == 0, "Sheathed weapon still released a cut");
                      NeoForge.EVENT_BUS.unregister(observer);
                      t.discard();
                      h.succeed();
                    });
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void spiritualHealthDamageCanPrimeAndConsumeAfterwind(GameTestHelper h) {
    var p = wielder(h);
    var t = enemy(h, p);
    RaceAPI.getRaceFrom(t).setRace(TensuraRaces.HUMAN_SAINT.get(), false);
    var energy = TensuraStorages.getExistenceFrom(t);
    energy.setSpiritualHealth(1000);
    float hp = t.getHealth();
    int[] hits = {0};
    java.util.function.Consumer<LivingIncomingDamageEvent> observer =
        e -> {
          if (e.getEntity() == t && e.getSource().is(Afterwind.DAMAGE)) hits[0]++;
        };
    NeoForge.EVENT_BUS.addListener(observer);
    h.runAfterDelay(
        5,
        () -> {
          var spiritualWind = wind(p);
          ((TensuraDamageSource) spiritualWind).tensura$setSpiritual();
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              t, p, spiritualWind, 10, 0);
          h.assertTrue(energy.getSpiritualHealth() < 1000, "Wind did not touch SHP");
          near(h, t.getHealth(), hp, "Wind should damage SHP before HP");
          t.invulnerableTime = 0;
          var spiritualStrike = p.damageSources().playerAttack(p);
          ((TensuraDamageSource) spiritualStrike).tensura$setSpiritual();
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              t, p, spiritualStrike, 15, 0);
          h.runAfterDelay(
              7,
              () -> {
                h.assertTrue(hits[0] == 1, "SHP-only damage did not enable aftercut");
                NeoForge.EVENT_BUS.unregister(observer);
                t.discard();
                h.succeed();
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticUpgradeImprovesAfterwindWithoutReplacingOtherEngravings(
      GameTestHelper h) {
    var p = wielder(h);
    var stack = p.getMainHandItem();
    var afterwind =
        p.registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Afterwind.ENGRAVING);
    stack.enchant(afterwind, 2);
    VesperItem.engrave(stack, p.registryAccess());
    h.assertTrue(Afterwind.level(p) == 2, "Default engraving reset infused level");
    near(h, Afterwind.coefficient(1), .2, "Afterwind I damage");
    near(h, Afterwind.coefficient(2), .25, "Afterwind II damage");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void savedKaelUpgradesHisOriginalSwordWithoutRefillingResources(GameTestHelper h) {
    var p = player(h);
    var e = WayfarerContent.KAEL.get().create(h.getLevel());
    e.setPos(p.position().add(0, 0, 5));
    e.setNoAi(true);
    h.getLevel().addFreshEntity(e);
    h.assertTrue(
        e.getMainHandItem().getItem() instanceof VesperItem, "New Kael did not equip Vesper");
    var old =
        new ItemStack(
            BuiltInRegistries.ITEM.get(ResourceLocation.parse("tensura:hihiirokane_sword")));
    old.setDamageValue(300);
    var sharp =
        p.registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.SHARPNESS);
    old.enchant(sharp, 2);
    e.setItemSlot(EquipmentSlot.MAINHAND, old);
    e.getPersistentData().putInt("heavenlyrestriction:wayfarer_loadout", 4);
    var pool = TensuraStorages.getExistenceFrom(e);
    e.setHealth(3000);
    pool.setSpiritualHealth(12000);
    pool.setAura(12345);
    pool.setMagicule(23456);
    e.initialize();
    h.assertTrue(
        e.getMainHandItem().getItem() instanceof VesperItem && Afterwind.level(e) == 1,
        "Old Kael retained original sword");
    h.assertTrue(
        e.getMainHandItem().getDamageValue() == 330
            && EnchantmentHelper.getItemEnchantmentLevel(sharp, e.getMainHandItem()) == 2,
        "Wear/native engravings lost on upgrade");
    near(h, e.getHealth(), 3000, "Migration healed HP");
    near(h, pool.getSpiritualHealth(), 12000, "Migration healed SHP");
    near(h, pool.getAura(), 12345, "Migration refilled aura");
    near(h, pool.getMagicule(), 23456, "Migration refilled magicules");
    near(h, EnergyHelper.getMaxEP(e), 15000000, "Weapon changed EP cap");
    e.discard();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelFullNativeDeathLootDropsOneEquippedVesperWithoutDuplication(
      GameTestHelper h) {
    var p = player(h);
    var boss = WayfarerContent.KAEL.get().create(h.getLevel());
    boss.setPos(p.position().add(0, 0, 5));
    boss.setNoAi(true);
    h.getLevel().addFreshEntity(boss);
    var engraving =
        h.getLevel()
            .registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Afterwind.ENGRAVING);
    boss.getMainHandItem().enchant(engraving, 2);
    boss.getMainHandItem().setDamageValue(100);
    var area = boss.getBoundingBox().inflate(8);
    // Exercise the complete loot transaction after death is accepted, without the boss's
    // regeneration/death-prevention callbacks cancelling this loot-specific fixture.
    try {
      var drop =
          LivingEntity.class.getDeclaredMethod(
              "dropAllDeathLoot", net.minecraft.server.level.ServerLevel.class, DamageSource.class);
      drop.setAccessible(true);
      drop.invoke(boss, h.getLevel(), boss.damageSources().genericKill());
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Could not invoke native death loot transaction", exception);
    }
    h.runAfterDelay(
        2,
        () -> {
          var swords =
              h
                  .getLevel()
                  .getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)
                  .stream()
                  .map(net.minecraft.world.entity.item.ItemEntity::getItem)
                  .filter(stack -> stack.is(HeavenlyArsenal.VESPER.get()))
                  .toList();
          h.assertTrue(
              swords.size() == 1 && swords.getFirst().getCount() == 1,
              "Native death loot did not contain exactly one Vesper: count="
                  + swords.size()
                  + " held="
                  + boss.getMainHandItem()
                  + " dead="
                  + boss.isDeadOrDying());
          h.assertTrue(
              EnchantmentHelper.getItemEnchantmentLevel(engraving, swords.getFirst()) == 2
                  && swords.getFirst().getDamageValue() == 100,
              "Native death loot replaced the equipped sword's engraving/wear");
          boss.discard();
          h.succeed();
        });
  }
}
