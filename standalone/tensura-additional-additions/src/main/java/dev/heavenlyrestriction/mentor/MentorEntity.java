package dev.heavenlyrestriction.mentor;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.tensura.entity.human.OtherworlderEntity;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.*;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.*;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;

/** A protected teaching NPC, using Tensura's native player model and animation system. */
public final class MentorEntity extends OtherworlderEntity
    implements SmartBrainOwner<MentorEntity> {
  public MentorEntity(EntityType<? extends MentorEntity> type, Level level) {
    super(type, level);
    setPersistenceRequired();
    setCanPickUpLoot(false);
    setInvulnerable(true);
    xpReward = 0;
  }

  public static AttributeSupplier.Builder attributes() {
    return TensuraTamableEntity.setAttributes()
        .add(Attributes.MAX_HEALTH, 100)
        .add(Attributes.ATTACK_DAMAGE, 0)
        .add(Attributes.MOVEMENT_SPEED, 0)
        .add(Attributes.KNOCKBACK_RESISTANCE, 1);
  }

  @Override
  public ResourceLocation getTextureLocation() {
    return HeavenlyRestriction.id("textures/entity/elias.png");
  }

  @Override
  public MentorEntity getBreedOffspring(ServerLevel level, AgeableMob other) {
    return null;
  }

  @Override
  public boolean removeWhenFarAway(double distance) {
    return false;
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  public void push(Entity other) {}

  @Override
  public boolean isFood(ItemStack stack) {
    return false;
  }

  @Override
  public boolean wantsToPickUp(ItemStack stack) {
    return false;
  }

  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND)
      MentorNetwork.open(p, this);
    return InteractionResult.sidedSuccess(level().isClientSide());
  }

  @Override
  protected Brain.Provider<?> brainProvider() {
    return new SmartBrainProvider<>(this, true);
  }

  @Override
  protected void customServerAiStep() {
    super.customServerAiStep();
    tickBrain(this);
  }

  @Override
  public List<ExtendedSensor<MentorEntity>> getSensors() {
    return List.of(new NearbyLivingEntitySensor<>());
  }

  @Override
  public BrainActivityGroup<MentorEntity> getCoreTasks() {
    return BrainActivityGroup.coreTasks(new LookAtTarget<>());
  }

  @Override
  public BrainActivityGroup<MentorEntity> getIdleTasks() {
    return BrainActivityGroup.idleTasks(new SetPlayerLookTarget<>(), new SetRandomLookTarget<>());
  }

  @Override
  public BrainActivityGroup<MentorEntity> getFightTasks() {
    return BrainActivityGroup.fightTasks(new SetRandomLookTarget<>());
  }
}
