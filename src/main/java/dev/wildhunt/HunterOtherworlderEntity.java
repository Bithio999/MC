package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.entity.human.OtherworlderEntity;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.entity.ai.behaviour.TensuraBehaviourHelper;
import io.github.manasmods.tensura.entity.ai.behaviour.attack.InvalidateNeutralAttackTarget;
import io.github.manasmods.tensura.entity.ai.behaviour.path.SubordinateFollowOwner;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.*;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.*;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.*;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.*;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.*;
import java.util.List;

/** Native otherworlder: subordinate UI, ownership, home, AI and Unique drops. */
public final class HunterOtherworlderEntity extends OtherworlderEntity implements SmartBrainOwner<HunterOtherworlderEntity> {
    public HunterOtherworlderEntity(EntityType<? extends HunterOtherworlderEntity> type, Level level) {
        super(type, level);
        ((GroundPathNavigation)getNavigation()).setCanOpenDoors(true);
    }
    public boolean isAlma() { return getType() == HunterOtherworlders.ALMA.get(); }
    @Override public ResourceLocation getTextureLocation() { return WildHunt.id("textures/entity/otherworlder/" + (isAlma() ? "alma" : "gemma") + ".png"); }
    @Override public List<ManasSkill> getUniqueSkills() {
        return isAlma() ? List.of(UniqueSkills.ANALYST.get(), UniqueSkills.OBSERVER.get()) : List.of(UniqueSkills.GODLY_CRAFTSMAN.get());
    }
    @Override public HunterOtherworlderEntity getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        // Do this once, on spawn, so loading/taming never resets the NPC's progress.
        RaceAPI.getRaceFrom(this).setRace(WildHunt.HUNTERS.getFirst().get(), false);
        TensuraBehaviourHelper.setHome(this, level.getLevel(), blockPosition());
        setPersistenceRequired();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(isAlma() ? Items.BOOK : Items.IRON_AXE));
        inventory.setItem(getSlotId(EquipmentSlot.MAINHAND), getMainHandItem());
        updateContainerEquipment();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }
    @Override protected Brain.Provider<?> brainProvider() { return new SmartBrainProvider<>(this, true); }
    @Override protected void customServerAiStep() { super.customServerAiStep(); tickBrain(this); }
    @Override public List<ExtendedSensor<HunterOtherworlderEntity>> getSensors() {
        return List.of(new NearbyLivingEntitySensor<>(), new HurtBySensor<>());
    }
    @Override public BrainActivityGroup<HunterOtherworlderEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(new LookAtTarget<>(), new FloatToSurfaceOfFluid<>(), TensuraTamableEntity.getMoveToWalkTarget());
    }
    @Override public BrainActivityGroup<HunterOtherworlderEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
            new FirstApplicableBehaviour<>(TensuraBehaviourHelper.getMoveToWanderPos(),
                TensuraBehaviourHelper.getPreyTargeting(this, this::shouldTarget), new SubordinateFollowOwner<>(),
                new SetPlayerLookTarget<>(), new SetRandomLookTarget<>()),
            new InteractWithDoor<>(),
            new OneRandomBehaviour<HunterOtherworlderEntity>(new SetRandomWalkTarget<HunterOtherworlderEntity>(), new Idle<HunterOtherworlderEntity>().runFor(e -> e.getRandom().nextInt(30,60)))
                .startCondition(e -> !e.isOrderedToSit()));
    }
    @Override public BrainActivityGroup<HunterOtherworlderEntity> getFightTasks() {
        return BrainActivityGroup.fightTasks(new InvalidateNeutralAttackTarget<HunterOtherworlderEntity>().invalidateIf(this::shouldStopTarget),
            new SetWalkTargetToAttackTarget<>(), new AnimatableMeleeAttack<>(1).attackInterval(e -> 20)
                .whenStarting(e -> e.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true)));
    }
}
