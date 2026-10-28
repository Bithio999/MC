package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Native learning entry, followed by a reward-first replacement of the permanent predecessor. */
public final class ChainedEvolution {
    private ChainedEvolution(){}
    public static boolean ready(LivingEntity e){
        if(e==null)return false;
        boolean heavenbreaker=RaceAPI.getRaceFrom(e).getRace().filter(i->i.getRace() instanceof RestrictedRace r && r.stage==4).isPresent();
        var energy=TensuraStorages.getExistenceFrom(e);
        return heavenbreaker && (energy.isTrueHero() || energy.isTrueDemonLord()) && SkillAPI.getSkillsFrom(e).getSkill(WildHunt.CHAINED.get()).filter(i->!i.isTemporarySkill() && !i.isSubInstance() && i.isMastered(e)).isPresent();
    }
    public static void discover(LivingEntity e){
        if(e.level().isClientSide() || !ready(e))return;
        var skills=SkillAPI.getSkillsFrom(e);if(skills.getSkill(WildHunt.UNBOUND.get()).isPresent())return;
        var learning=WildHunt.UNBOUND.get().createLearningInstance(e);learning.setMastery(-1);learning.getOrCreateTag().putBoolean("NoMagiculeCost",true);SkillHelper.learnSkill(e,learning);
    }
    public static void finish(LivingEntity e,ManasSkillInstance target){
        if(e.level().isClientSide() || target.getMastery()<0)return;
        var skills=SkillAPI.getSkillsFrom(e);var source=skills.getSkill(WildHunt.CHAINED.get());
        if(source.isPresent()){
            var old=source.get();target.setTag(old.getOrCreateTag().copy());target.setToggled(old.isToggled());
            target.setMastery(target.getMaxMastery());target.setCoolDown(old.getCoolDown(0),0);target.setParentSkill(null);
            if(ChainedState.inherited(e,old.getSkill()))ChainedState.remember(e,target.getSkill());
            var slots=new ArrayList<int[]>();
            if(e instanceof ServerPlayer p){var ability=TensuraStorages.getAbilityFrom(p);for(int preset=0;preset<9;preset++)for(int slot=0;slot<3;slot++)if(ability.getAbilitySlot(preset,slot).getSkill()==old.getSkill())slots.add(new int[]{preset,slot});}
            skills.forgetSkill(old);
            if(e instanceof ServerPlayer p){var ability=TensuraStorages.getAbilityFrom(p);for(var slot:slots)ability.setAbilitySlot(slot[0],slot[1],target.getSkill(),0);ability.markDirty();}
        }
        ChainedState.initialize(e);skills.checkAndMarkDirty(target);
    }
}
