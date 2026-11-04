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
  public static final int RIFT_DRAW = 1, CONSTELLATION = 2, TEMPEST = 3, WALTZ = 4;
  private static final EntityDataAccessor<Integer> PATTERN =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> DURATION =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Long> START =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Long> HIT_UNTIL =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<org.joml.Vector3f> ORIGIN =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.VECTOR3);
  private static final EntityDataAccessor<org.joml.Vector3f> AIM =
      SynchedEntityData.defineId(KaelVeyrEntity.class, EntityDataSerializers.VECTOR3);
  private final ServerBossEvent healthBar =
      new ServerBossEvent(
          Component.translatable("boss.heavenlyrestriction.kael_veyr"),
          BossEvent.BossBarColor.BLUE,
          BossEvent.BossBarOverlay.NOTCHED_20);
  private final ServerBossEvent soulBar =
      new ServerBossEvent(
          Component.translatable("boss.heavenlyrestriction.kael_veyr_soul"),
          BossEvent.BossBarColor.PURPLE,
          BossEvent.BossBarOverlay.PROGRESS);
  private final Set<ServerPlayer> watchers = new HashSet<>();
  private final Set<UUID> waveHits = new HashSet<>();
  private boolean initialized, observed;
  private float oldHP;
  private double oldSHP;
  private BlockPos anchor;
  private long nextSpecial;
  private int sequence, tempestCasts;
  private ManasSkill activeMagic;
  private UUID castTarget;

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
    b.define(PATTERN, 0);
    b.define(DURATION, 0);
    b.define(START, 0L);
    b.define(HIT_UNTIL, 0L);
    b.define(ORIGIN, new org.joml.Vector3f());
    b.define(AIM, new org.joml.Vector3f());
  }

  public void initialize() {
    if (initialized || level().isClientSide()) return;
    setAge(0);
    anchor = blockPosition();
    RaceAPI.getRaceFrom(this).setRace(TensuraRaces.HUMAN_SAINT.get(), false);
    var weapon = BuiltInRegistries.ITEM.get(ResourceLocation.parse("tensura:hihiirokane_sword"));
    inventory.setItem(getSlotId(EquipmentSlot.MAINHAND), new ItemStack(weapon));
    updateContainerEquipment();
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
    nextSpecial = level().getGameTime() + 80;
    initialized = true;
    observe();
  }

  private void base(net.minecraft.core.Holder<Attribute> key, double value) {
    var attr = getAttribute(key);
    if (attr != null) attr.setBaseValue(value);
  }

  public boolean initialized() {
    return initialized;
  }

  public int pattern() {
    return entityData.get(PATTERN);
  }

  public int duration() {
    return entityData.get(DURATION);
  }

  public float patternAge(float partial) {
    return (float) Math.max(0, level().getGameTime() - entityData.get(START)) + partial;
  }

  public Vec3 patternOrigin() {
    var p = entityData.get(ORIGIN);
    return position().add(p.x, p.y, p.z);
  }

  public Vec3 patternAim() {
    var p = entityData.get(AIM);
    return position().add(p.x, p.y, p.z);
  }

  public void preservePatternOrigin(Vec3 origin, Vec3 aim) {
    entityData.set(ORIGIN, origin.subtract(position()).toVector3f());
    entityData.set(AIM, aim.subtract(position()).toVector3f());
  }

  public boolean hitFlash() {
    return entityData.get(HIT_UNTIL) > level().getGameTime();
  }

  public Set<UUID> waveHits() {
    return waveHits;
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

  public boolean beginPattern(int mode) {
    var target = getTarget();
    if (!initialized
        || isTame()
        || isNoAi()
        || pattern() != 0
        || !eligible(target)
        || distanceToSqr(target) > 48 * 48) return false;
    if (!WayfarerCombat.ready(this, mode)) return false;
    if (mode == TEMPEST) activeMagic = WayfarerCombat.spell(this).getSkill();
    int time = WayfarerCombat.duration(this, mode);
    entityData.set(PATTERN, mode);
    entityData.set(DURATION, time);
    entityData.set(START, level().getGameTime());
    entityData.set(ORIGIN, new org.joml.Vector3f());
    entityData.set(AIM, target.getBoundingBox().getCenter().subtract(position()).toVector3f());
    castTarget = target.getUUID();
    waveHits.clear();
    getNavigation().stop();
    setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
    nextSpecial = level().getGameTime() + time + (getHealth() < getMaxHealth() * .5 ? 40 : 65);
    WayfarerCombat.start(this, mode);
    return true;
  }

  public LivingEntity castTarget() {
    return level() instanceof ServerLevel level
            && castTarget != null
            && level.getEntity(castTarget) instanceof LivingEntity e
        ? e
        : null;
  }

  public ManasSkill activeMagic() {
    return activeMagic;
  }

  public int spellPreference() {
    return tempestCasts;
  }

  public void magicReleased() {
    tempestCasts++;
  }

  public void cancelPattern() {
    activeMagic = null;
    entityData.set(PATTERN, 0);
    castTarget = null;
    waveHits.clear();
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
    soulBar.setProgress(
        (float)
            Math.clamp(
                TensuraStorages.getExistenceFrom(this).getSpiritualHealth()
                    / Math.max(1, getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)),
                0,
                1));
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
        soulBar.removePlayer(p);
        watchers.remove(p);
      }
    for (var p : near)
      if (watchers.add(p)) {
        healthBar.addPlayer(p);
        soulBar.addPlayer(p);
      }
  }

  @Override
  public void stopSeenByPlayer(ServerPlayer p) {
    super.stopSeenByPlayer(p);
    watchers.remove(p);
    healthBar.removePlayer(p);
    soulBar.removePlayer(p);
  }

  @Override
  public void remove(Entity.RemovalReason reason) {
    healthBar.removeAllPlayers();
    soulBar.removeAllPlayers();
    watchers.clear();
    super.remove(reason);
  }

  @Override
  protected void applyTamingSideEffects() {
    super.applyTamingSideEffects();
    cancelPattern();
    healthBar.removeAllPlayers();
    soulBar.removeAllPlayers();
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
      if (pattern() != 0) cancelPattern();
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
    if (pattern() != 0) {
      if (!eligible(castTarget())
          || io.github.manasmods.tensura.ability.SkillUtils.shouldCancelInteraction(this))
        cancelPattern();
      else {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        WayfarerCombat.tick(this, (int) patternAge(0));
        if (patternAge(0) > duration()) cancelPattern();
      }
    } else if (level().getGameTime() >= nextSpecial
        && eligible(getTarget())
        && hasLineOfSight(getTarget())) {
      int[] order =
          getHealth() < getMaxHealth() * .5
              ? new int[] {WALTZ, TEMPEST, CONSTELLATION, RIFT_DRAW}
              : new int[] {RIFT_DRAW, TEMPEST, CONSTELLATION, TEMPEST};
      if (!beginPattern(order[Math.floorMod(sequence++, order.length)]))
        nextSpecial = level().getGameTime() + 20;
    }
  }

  @Override
  public void tick() {
    super.tick();
    if (!level().isClientSide()) {
      if (initialized) observe();
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
            .startCondition(e -> e.pattern() == 0),
        new AnimatableMeleeAttack<KaelVeyrEntity>(0)
            .attackInterval(e -> 24)
            .startCondition(e -> e.pattern() == 0)
            .whenStarting(e -> e.swing(InteractionHand.MAIN_HAND, true)));
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putBoolean("WayfarerInitialized", initialized);
    tag.putLong("WayfarerNextCast", nextSpecial);
    tag.putInt("WayfarerSequence", sequence);
    tag.putInt("WayfarerTempestCasts", tempestCasts);
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
    nextSpecial = Math.max(level().getGameTime() + 60, tag.getLong("WayfarerNextCast"));
    sequence = tag.getInt("WayfarerSequence");
    tempestCasts = tag.getInt("WayfarerTempestCasts");
    observed = false;
    cancelPattern();
  }
}
