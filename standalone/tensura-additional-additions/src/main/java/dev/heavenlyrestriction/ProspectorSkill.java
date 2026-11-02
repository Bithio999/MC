package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.subclass.IRefining;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Uses Tensura's actual refining protocol, recipes, container and persisted ingredient storage. */
public final class ProspectorSkill extends Skill implements IRefining<ProspectorSkill> {
    public ProspectorSkill(){super(SkillType.UNIQUE);}
    @Override public MutableComponent getName(){return Component.translatableWithFallback("heavenlyrestriction.skill.prospector","Prospector");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(getType().getChatFormatting());}
    @Override public ResourceLocation getSkillIcon(){return HeavenlyRestriction.id("textures/skill/prospector.png");}
    @Override public MutableComponent getSkillDescription(){return Component.translatable("heavenlyrestriction.skill.prospector.description");}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return i.getMastery()>=0;}
    @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e){i.setToggled(true);}
    @Override public void onToggleOff(ManasSkillInstance i,LivingEntity e){i.setToggled(false);}
    @Override public boolean canTick(ManasSkillInstance i,LivingEntity e){return i.getMastery()>=0 && canInteractSkill(i,e);}
    @Override public void onTick(ManasSkillInstance i,LivingEntity e){if(e instanceof Player p && !e.level().isClientSide() && onRefiningTick(i,p))i.addMasteryPoint(e);}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "prospector.refine";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.translatableWithFallback("heavenlyrestriction.skill.mode.prospector.refine","Refine");}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){if(mode==0 && e instanceof ServerPlayer && i.getMastery()>=0 && canInteractSkill(i,e) && canActivateSkill(i,e,mode))openRefiningMenu(e,i);}
    @Override public int getSpatialStorageIdOffset(){return 11;}
    @Override public boolean isAutoRefiningAllowed(){return true;}
    @Override public SpatialStorageContainer getSpatialStorage(ManasSkillInstance i,HolderLookup.Provider registries){var storage=new SpatialStorageContainer(20,128);storage.fromTag(i.getOrCreateTag().getList("SpatialStorage",10),registries);return storage;}
}
