package dev.heavenlyrestriction.highrealm;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.data.TensuraEntityTags;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.*;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;

public final class HighRealmElites {
  public static final String MARKER = "heavenlyrestriction:high_realm_elite",
      CANDIDATE = "heavenlyrestriction:high_realm_candidate";
  public static final TagKey<EntityType<?>> TYPES =
      TagKey.create(Registries.ENTITY_TYPE, HeavenlyRestriction.id("high_realm_elites"));
  private static final TagKey<EntityType<?>> BOSSES =
      TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));

  public enum Kind {
    BULWARK(5000000, 1024, 5000, 150, .27),
    SENTINEL(3500000, 850, 3200, 110, .29),
    SKIRMISHER(2500000, 700, 2400, 95, .38);
    public final double ep, hp, shp, damage, speed;

    Kind(double ep, double hp, double shp, double damage, double speed) {
      this.ep = ep;
      this.hp = hp;
      this.shp = shp;
      this.damage = damage;
      this.speed = speed;
    }
  }

  public static void register() {
    NeoForge.EVENT_BUS.addListener(HighRealmElites::spawning);
    NeoForge.EVENT_BUS.addListener(HighRealmElites::joined);
    NeoForge.EVENT_BUS.addListener(HighRealmElites::drops);
  }

  public static boolean elite(Entity mob) {
    return mob.getPersistentData().getBoolean(MARKER);
  }

  public static boolean candidate(Mob mob) {
    return mob.getType().is(TYPES)
        && mob.getType() != EntityType.WITHER
        && mob.getType() != EntityType.ENDER_DRAGON
        && !mob.getType().is(BOSSES)
        && !mob.getType().is(TensuraEntityTags.HERO_BOSS)
        && !mob.getType().is(TensuraEntityTags.CLONES);
  }

  public static Kind kind(Mob mob) {
    String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
    return Set.of("husk", "orc", "wither_skeleton").contains(id)
        ? Kind.BULWARK
        : Set.of("stray", "lizardman", "enderman").contains(id) ? Kind.SENTINEL : Kind.SKIRMISHER;
  }

  private static void spawning(FinalizeSpawnEvent e) {
    if (!e.getLevel().getLevel().dimension().equals(HighRealm.LEVEL)
        || e.getSpawnType() != MobSpawnType.NATURAL
            && e.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
    var mob = e.getEntity();
    var sanctuary = RealmData.get(e.getLevel().getServer()).sanctuary;
    if (!HighRealmConfig.ELITES.get()
        || !candidate(mob)
        || sanctuary != null
            && mob.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(sanctuary))
                < 48 * 48) {
      e.setSpawnCancelled(true);
      return;
    }
    mob.getPersistentData().putBoolean(CANDIDATE, true);
  }

  private static void joined(EntityJoinLevelEvent e) {
    if (e.getLevel().isClientSide()) return;
    if (e.getEntity() instanceof Mob mob
        && e.getLevel().dimension().equals(HighRealm.LEVEL)
        && mob.getPersistentData().getBoolean(CANDIDATE)) empower(mob);
    if (e.getEntity() instanceof AbstractArrow arrow
        && arrow.getOwner() instanceof Mob mob
        && elite(mob))
      arrow.setBaseDamage(Math.max(arrow.getBaseDamage(), 30 * HighRealmConfig.ELITE_POWER.get()));
  }

  private static void base(Mob mob, net.minecraft.core.Holder<Attribute> attribute, double value) {
    var a = mob.getAttribute(attribute);
    if (a != null) a.setBaseValue(value);
  }

  public static boolean empower(Mob mob) {
    if (elite(mob)
        || !candidate(mob)
        || !mob.level().dimension().equals(HighRealm.LEVEL)
        || TensuraStorages.getExistenceFrom(mob).getPermanentOwner() != null
        || TensuraStorages.getExistenceFrom(mob).getSummoner() != null
        || mob instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame())
      return false;
    var k = kind(mob);
    double power = HighRealmConfig.ELITE_POWER.get();
    mob.getPersistentData().putBoolean(MARKER, true);
    mob.getPersistentData().putString("heavenlyrestriction:elite_kind", k.name());
    base(
        mob,
        Attributes.MAX_HEALTH,
        Math.min(1024, Math.max(mob.getAttributeBaseValue(Attributes.MAX_HEALTH), k.hp * power)));
    base(
        mob,
        Attributes.ATTACK_DAMAGE,
        Math.max(mob.getAttributeValue(Attributes.ATTACK_DAMAGE), k.damage * power));
    base(mob, Attributes.ARMOR, k == Kind.BULWARK ? 20 : 12);
    base(mob, Attributes.ARMOR_TOUGHNESS, 8);
    base(
        mob,
        Attributes.MOVEMENT_SPEED,
        Math.max(mob.getAttributeValue(Attributes.MOVEMENT_SPEED), k.speed));
    base(mob, Attributes.KNOCKBACK_RESISTANCE, k == Kind.BULWARK ? .8 : .35);
    base(mob, Attributes.FOLLOW_RANGE, 40);
    EnergyHelper.setBaseMaxEP(mob, k.ep * power);
    base(mob, TensuraAttributes.MAX_SPIRITUAL_HEALTH, k.shp * power);
    mob.setHealth(mob.getMaxHealth());
    var energy = TensuraStorages.getExistenceFrom(mob);
    energy.setSpiritualHealth(mob.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));
    energy.setAura(EnergyHelper.getMaxAura(mob));
    energy.setMagicule(EnergyHelper.getMaxMagicule(mob));
    energy.markDirty();
    if (k == Kind.BULWARK) {
      RestrictionData.learnReward(mob, ResistanceSkills.PHYSICAL_ATTACK_RESISTANCE.get());
      RestrictionData.learnReward(mob, ExtraSkills.STRENGTHEN_BODY.get());
      SkillAPI.getSkillsFrom(mob)
          .getSkill(ExtraSkills.STRENGTHEN_BODY.get())
          .ifPresent(
              i -> {
                i.setToggled(true);
                i.onToggleOn(mob);
              });
    } else if (k == Kind.SENTINEL) {
      RestrictionData.learnReward(mob, ResistanceSkills.MAGIC_RESISTANCE.get());
      RestrictionData.learnReward(mob, ResistanceSkills.WATER_ATTACK_RESISTANCE.get());
    } else {
      RestrictionData.learnReward(mob, ResistanceSkills.POISON_RESISTANCE.get());
      RestrictionData.learnReward(mob, ResistanceSkills.PARALYSIS_RESISTANCE.get());
    }
    String title =
        k == Kind.BULWARK
            ? "High Bulwark "
            : k == Kind.SENTINEL ? "High Sentinel " : "High Stalker ";
    mob.setCustomName(
        Component.literal(title)
            .append(mob.getType().getDescription())
            .withStyle(net.minecraft.ChatFormatting.GOLD));
    if (mob.getType() == EntityType.HUSK || mob.getType() == EntityType.WITHER_SKELETON) {
      mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
      mob.setDropChance(EquipmentSlot.MAINHAND, 0);
      mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
      mob.setDropChance(EquipmentSlot.CHEST, 0);
    }
    return true;
  }

  private static void drops(LivingDropsEvent e) {
    if (!(e.getEntity() instanceof Mob mob) || !elite(mob)) return;
    var attacker = RestrictionData.attacker(e.getSource());
    if (!(attacker instanceof Player)
        && (attacker == null
            || !(SubordinateHelper.getSubordinateOwner(attacker) instanceof Player))) return;
    var random = mob.getRandom();
    add(e, new ItemStack(Items.AMETHYST_SHARD, 1 + random.nextInt(3)));
    if (random.nextInt(5) == 0) {
      var ore =
          BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tensura", "magic_ore"));
      if (ore != Items.AIR) add(e, new ItemStack(ore));
    }
    if (random.nextInt(50) == 0) add(e, new ItemStack(Items.NETHERITE_SCRAP));
  }

  private static void add(LivingDropsEvent event, ItemStack stack) {
    var mob = event.getEntity();
    event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), stack));
  }

  private HighRealmElites() {}
}
