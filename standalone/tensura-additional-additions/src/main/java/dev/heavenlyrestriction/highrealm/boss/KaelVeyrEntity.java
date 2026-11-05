package dev.heavenlyrestriction.highrealm.boss;

import dev.heavenlyrestriction.RestrictionData;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.entity.ai.behaviour.TensuraBehaviourHelper;
import io.github.manasmods.tensura.entity.ai.behaviour.attack.InvalidateNeutralAttackTarget;
import io.github.manasmods.tensura.entity.ai.behaviour.path.SubordinateFollowOwner;
import io.github.manasmods.tensura.entity.human.OtherworlderEntity;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.*;
import net.tslat.smartbrainlib.api.core.behaviour.*;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.*;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.*;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.*;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.*;
import net.tslat.smartbrainlib.util.BrainUtils;

/**
 * Native otherworlder: real race/resources/skills, inventory, animations and taming compatibility.
 */
public final class KaelVeyrEntity extends OtherworlderEntity
    implements SmartBrainOwner<KaelVeyrEntity> {
  public static final double EP = 15_000_000, HP = 8_000, SHP = 24_000;
  private static final EntityDataAccessor<Boolean> CASTING =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Long> HIT_UNTIL =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.LONG);
  private final ServerBossEvent healthBar =
      new ServerBossEvent(
          Component.translatable("boss.heavenlyrestriction.kael_veyr"),
          BossEvent.BossBarColor.BLUE,
          BossEvent.BossBarOverlay.NOTCHED_20);
  private final Set<ServerPlayer> watchers = new HashSet<>();
  private final WayfarerCombat combat = new WayfarerCombat(this);
  private boolean initialized, observed;
  private float oldHP;
  private double oldSHP;
  private BlockPos anchor;

  public KaelVeyrEntity(EntityType<? extends KaelVeyrEntity> type, Level level) {
    super(type, level);
    setPersistenceRequired();
    setCanPickUpLoot(false);
    xpReward = 1500;
    ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
  }

  public static AttributeSupplier.Builder attributes() {
    return TensuraTamableEntity.setAttributes()
        .add(Attributes.MAX_HEALTH, HP)
        .add(Attributes.ATTACK_DAMAGE, 100)
        .add(Attributes.ARMOR, 24)
        .add(Attributes.ARMOR_TOUGHNESS, 12)
        .add(Attributes.MOVEMENT_SPEED, .30)
        .add(Attributes.FOLLOW_RANGE, 48)
        .add(Attributes.KNOCKBACK_RESISTANCE, .85)
        .add(Attributes.STEP_HEIGHT, 1.2)
        .add(TensuraAttributes.MAX_SPIRITUAL_HEALTH, SHP)
        .add(TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION, 25)
        .add(TensuraAttributes.AURA_REGENERATION_MULTIPLIER, 4)
        .add(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, 4)
        .add(TensuraAttributes.PRESENCE_SENSE, 7);
  }

  @Override
  public ResourceLocation getTextureLocation() {
    return dev.heavenlyrestriction.HeavenlyRestriction.id("textures/entity/kael_veyr.png");
  }

  @Override
  public List<ManasSkill> getUniqueSkills() {
    return WayfarerLoadout.uniques();
  }

  @Override
  public KaelVeyrEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
    return null;
  }

  @Override
  public boolean removeWhenFarAway(double distance) {
    return false;
  }

  @Override
  public boolean wantsToPickUp(ItemStack stack) {
    return false;
  }

  @Override
  public boolean isFood(ItemStack stack) {
    return false;
  }

  @Override
  public boolean shouldShowName() {
    return false;
  }

  @Override
  public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
    return !WayfarerSpawns.natural(reason)
        || level instanceof ServerLevelAccessor world
            && WayfarerSpawns.validSurface(getType(), world, blockPosition());
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(CASTING, false);
    b.define(HIT_UNTIL, 0L);
  }

  public void initialize() {
    if (level().isClientSide()) return;
    if (initialized) {
      WayfarerLoadout.upgrade(this);
      capEP();
      return;
    }
    setAge(0);
    anchor = blockPosition();
    RaceAPI.getRaceFrom(this).setRace(TensuraRaces.DIVINE_HUMAN.get(), false);
    var weapon = BuiltInRegistries.ITEM.get(ResourceLocation.parse("tensura:hihiirokane_sword"));
    inventory.setItem(getSlotId(EquipmentSlot.MAINHAND), new ItemStack(weapon));
    updateContainerEquipment();
    TensuraStorages.getExistenceFrom(this).setTrueHero(true);
    WayfarerLoadout.apply(this);
    base(Attributes.MAX_HEALTH, HP);
    base(TensuraAttributes.MAX_SPIRITUAL_HEALTH, SHP);
    EnergyHelper.setBaseMaxEP(this, EP);
    var pool = TensuraStorages.getExistenceFrom(this);
    pool.setTrueHero(true);
    pool.setAura(EnergyHelper.getMaxAura(this));
    pool.setMagicule(EnergyHelper.getMaxMagicule(this));
    pool.setSpiritualHealth(getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));
    pool.markDirty();
    setHealth(getMaxHealth());
    setDropChance(EquipmentSlot.MAINHAND, 0);
    TensuraBehaviourHelper.setHome(this, (ServerLevel) level(), anchor);
    combat.delay(80);
    initialized = true;
    observe();
  }

  /** Hard cap actual EP, including native kill rewards; never refill wounded resource pools. */
  public void capEP() {
    if (level().isClientSide() || !initialized) return;
    double total = EnergyHelper.getMaxEP(this);
    if (total <= EP) return;
    double ratio = EP / total;
    capCapacity(getAttribute(TensuraAttributes.MAX_AURA), EnergyHelper.getMaxAura(this) * ratio);
    capCapacity(
        getAttribute(TensuraAttributes.MAX_MAGICULE), EnergyHelper.getMaxMagicule(this) * ratio);
    var pool = TensuraStorages.getExistenceFrom(this);
    pool.setAura(Math.min(pool.getAura(), EnergyHelper.getMaxAura(this)));
    pool.setMagicule(Math.min(pool.getMagicule(), EnergyHelper.getMaxMagicule(this)));
    pool.markDirty();
  }

  private static void capCapacity(AttributeInstance attribute, double desired) {
    if (attribute == null) return;
    attribute.setBaseValue(Math.max(0, baseForCapacity(attribute, desired)));
  }

  /** Clamp before Tensura's growth listener can award stats/healing for rejected EP. */
  public double capGrowth(AttributeInstance attribute, double proposed) {
    if (!initialized || level().isClientSide() || proposed <= attribute.getBaseValue())
      return proposed;
    boolean aura = attribute.getAttribute().equals(TensuraAttributes.MAX_AURA);
    if (!aura && !attribute.getAttribute().equals(TensuraAttributes.MAX_MAGICULE)) return proposed;
    if (EnergyHelper.getMaxEP(this) >= EP - .000001) return attribute.getBaseValue();
    double other = aura ? EnergyHelper.getMaxMagicule(this) : EnergyHelper.getMaxAura(this);
    double maximum = Math.max(0, baseForCapacity(attribute, Math.max(0, EP - other)));
    return Math.min(proposed, Math.max(attribute.getBaseValue(), maximum));
  }

  private static double baseForCapacity(AttributeInstance attribute, double desired) {
    double added = 0, multipliedBase = 1, multipliedTotal = 1;
    for (var modifier : attribute.getModifiers()) {
      switch (modifier.operation()) {
        case ADD_VALUE -> added += modifier.amount();
        case ADD_MULTIPLIED_BASE -> multipliedBase += modifier.amount();
        case ADD_MULTIPLIED_TOTAL -> multipliedTotal *= 1 + modifier.amount();
      }
    }
    double multiplier = multipliedBase * multipliedTotal;
    return multiplier > 0 ? desired / multiplier - added : attribute.getBaseValue();
  }

  private void base(net.minecraft.core.Holder<Attribute> key, double value) {
    var attr = getAttribute(key);
    if (attr != null) attr.setBaseValue(value);
  }

  public boolean initialized() {
    return initialized;
  }

  public WayfarerCombat combat() {
    return combat;
  }

  public boolean casting() {
    return entityData.get(CASTING);
  }

  void setCasting(boolean casting) {
    entityData.set(CASTING, casting);
  }

  public boolean hitFlash() {
    return entityData.get(HIT_UNTIL) > level().getGameTime();
  }

  public boolean eligible(LivingEntity target) {
    if (target == null
        || target == this
        || !target.isAlive()
        || !RestrictionData.canHarm(this, target)) return false;
    if (!isTame()
        && anchor != null
        && target.position().distanceToSqr(Vec3.atCenterOf(anchor)) > 128 * 128) return false;
    return target instanceof Player
        || SubordinateHelper.getSubordinateOwner(target) instanceof Player
        || target instanceof Mob mob && (mob.getTarget() == this || getLastHurtByMob() == mob);
  }

  @Override
  public boolean shouldTarget(LivingEntity target) {
    return eligible(target);
  }

  private void observe() {
    var shp = TensuraStorages.getExistenceFrom(this).getSpiritualHealth();
    if (observed && (getHealth() < oldHP - .01 || shp < oldSHP - .01))
      entityData.set(HIT_UNTIL, level().getGameTime() + 6);
    oldHP = getHealth();
    oldSHP = shp;
    observed = true;
  }

  private void updateBars() {
    if (!(level() instanceof ServerLevel level)) return;
    healthBar.setProgress(Math.clamp(getHealth() / Math.max(1, getMaxHealth()), 0, 1));
    healthBar.setColor(
        getHealth() < getMaxHealth() * .5
            ? BossEvent.BossBarColor.YELLOW
            : BossEvent.BossBarColor.BLUE);
    var near = new HashSet<ServerPlayer>();
    if (isAlive() && !isTame())
      for (var p : level.players())
        if (!p.isSpectator() && distanceToSqr(p) <= 64 * 64) near.add(p);
    for (var p : List.copyOf(watchers))
      if (!near.contains(p)) {
        healthBar.removePlayer(p);
        watchers.remove(p);
      }
    for (var p : near)
      if (watchers.add(p)) {
        healthBar.addPlayer(p);
      }
  }

  @Override
  public void stopSeenByPlayer(ServerPlayer p) {
    super.stopSeenByPlayer(p);
    watchers.remove(p);
    healthBar.removePlayer(p);
  }

  @Override
  public void remove(Entity.RemovalReason reason) {
    combat.cancel();
    healthBar.removeAllPlayers();
    watchers.clear();
    super.remove(reason);
  }

  @Override
  protected void applyTamingSideEffects() {
    super.applyTamingSideEffects();
    combat.cancel();
    healthBar.removeAllPlayers();
    watchers.clear();
  }

  @Override
  protected Brain.Provider<?> brainProvider() {
    return new SmartBrainProvider<>(this, true);
  }

  @Override
  protected void customServerAiStep() {
    super.customServerAiStep();
    initialize();
    tickBrain(this);
    if (isTame()) {
      combat.cancel();
      return;
    }
    if (tickCount % 10 == 0) {
      var target = getTarget();
      if (!eligible(target) || distanceToSqr(target) > 80 * 80) {
        var attacker = getLastHurtByMob();
        target =
            eligible(attacker) && distanceToSqr(attacker) <= 48 * 48
                ? attacker
                : ((ServerLevel) level())
                    .players().stream()
                        .filter(this::eligible)
                        .filter(p -> distanceToSqr(p) <= 32 * 32 && getSensing().hasLineOfSight(p))
                        .min(Comparator.comparingDouble(this::distanceToSqr))
                        .orElse(null);
        setTarget(target);
      }
      if (target != null) BrainUtils.setTargetOfEntity(this, target);
    }
    combat.tick();
  }

  @Override
  public void tick() {
    super.tick();
    if (!level().isClientSide()) {
      if (initialized) {
        capEP();
        observe();
      }
      if (tickCount % 10 == 0) updateBars();
    }
  }

  @Override
  public List<ExtendedSensor<KaelVeyrEntity>> getSensors() {
    return List.of(new NearbyLivingEntitySensor<>(), new HurtBySensor<>());
  }

  @Override
  public BrainActivityGroup<KaelVeyrEntity> getCoreTasks() {
    return BrainActivityGroup.coreTasks(
        new LookAtTarget<>(),
        new FloatToSurfaceOfFluid<>(),
        TensuraTamableEntity.getMoveToWalkTarget());
  }

  @Override
  public BrainActivityGroup<KaelVeyrEntity> getIdleTasks() {
    return BrainActivityGroup.idleTasks(
        new FirstApplicableBehaviour<>(
            TensuraBehaviourHelper.getMoveToWanderPos(),
            new SubordinateFollowOwner<>(),
            new SetPlayerLookTarget<>(),
            new SetRandomLookTarget<>()),
        new InteractWithDoor<>(),
        new OneRandomBehaviour<KaelVeyrEntity>(
                new SetRandomWalkTarget<KaelVeyrEntity>().setRadius(12),
                new Idle<KaelVeyrEntity>().runFor(e -> 40))
            .startCondition(e -> !e.isOrderedToSit()));
  }

  @Override
  public BrainActivityGroup<KaelVeyrEntity> getFightTasks() {
    return BrainActivityGroup.fightTasks(
        new InvalidateNeutralAttackTarget<KaelVeyrEntity>()
            .invalidateIf(
                (boss, target) -> !boss.eligible(target) || boss.distanceToSqr(target) > 80 * 80),
        new SetWalkTargetToAttackTarget<KaelVeyrEntity>()
            .speedMod((e, t) -> 1.15f)
            .startCondition(e -> !e.casting()),
        new AnimatableMeleeAttack<KaelVeyrEntity>(0)
            .attackInterval(e -> 24)
            .startCondition(e -> !e.casting())
            .whenStarting(e -> e.swing(InteractionHand.MAIN_HAND, true)));
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putBoolean("WayfarerInitialized", initialized);
    if (anchor != null) tag.putLong("WayfarerAnchor", anchor.asLong());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    initialized = tag.getBoolean("WayfarerInitialized");
    anchor =
        tag.contains("WayfarerAnchor")
            ? BlockPos.of(tag.getLong("WayfarerAnchor"))
            : blockPosition();
    observed = false;
    combat.cancel();
    combat.delay(60);
  }
}
