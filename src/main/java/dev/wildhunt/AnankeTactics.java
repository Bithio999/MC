package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Range-aware decisions, with a paid gap closer between fixed-origin telegraphs. */
final class AnankeTactics {
    private AnankeTactics(){}
    static boolean inRange(AnankeEntity boss,LivingEntity target,int mode){
        var delta=target.getBoundingBox().getCenter().subtract(boss.position());
        return switch(mode){
            case 0->Math.abs(delta.y)<5 && delta.horizontalDistanceSqr()<=15*15;
            case 1->delta.lengthSqr()<=64*64;
            case 2->Math.abs(delta.y)<=3 && delta.horizontalDistanceSqr()>=3*3 && delta.horizontalDistanceSqr()<=25*25;
            case 3->delta.lengthSqr()<=32*32;
            default->false;
        };
    }
    static int chooseRite(AnankeEntity boss,ManasSkillInstance skill,LivingEntity target,int preferred){
        for(int mode:new int[]{preferred,1,0,2,3}){
            if(mode==3 && boss.phase()<3)continue;
            if(!skill.onCoolDown(mode) && inRange(boss,target,mode))return mode;
        }
        return -1;
    }
    static boolean reposition(AnankeEntity boss,LivingEntity target){
        if(boss.isCasting() || io.github.manasmods.tensura.ability.SkillUtils.shouldCancelInteraction(boss) || !boss.eligible(target) || boss.distanceToSqr(target)<=22*22 || boss.distanceToSqr(target)>64*64)return false;
        var energy=TensuraStorages.getExistenceFrom(boss);if(energy.getMagicule()<20_000)return false;
        var level=(ServerLevel)boss.level();var direction=boss.position().subtract(target.position()).multiply(1,0,1).normalize();
        if(direction.lengthSqr()<.01)direction=new Vec3(1,0,0);
        for(double angle:new double[]{0,Math.PI/3,-Math.PI/3,2*Math.PI/3,-2*Math.PI/3}){
            var at=target.position().add(direction.yRot((float)angle).scale(10));
            var bounds=boss.getBoundingBox().move(at.subtract(boss.position()));
            if(at.distanceToSqr(Vec3.atCenterOf(boss.anchor()))>60*60 || !level.hasChunkAt(net.minecraft.core.BlockPos.containing(at))
                || !level.getWorldBorder().isWithinBounds(bounds) || at.y<level.getMinBuildHeight() || bounds.maxY>=level.getMaxBuildHeight()
                || !level.noCollision(boss,bounds) || level.containsAnyLiquid(bounds))continue;
            shiftVisual(level,boss,boss.position());boss.getNavigation().stop();boss.teleportTo(at.x,at.y,at.z);boss.setDeltaMovement(Vec3.ZERO);
            shiftVisual(level,boss,at);energy.setMagicule(energy.getMagicule()-20_000);energy.markDirty();return true;
        }
        return false;
    }
    private static void shiftVisual(ServerLevel level,AnankeEntity boss,Vec3 at){
        ArtVisualEntity.spawn(level,ArtVisualEntity.ANANKE_SHIFT,at,new Vec3(3,0,0),24).setCaster(boss);
        level.playSound(null,at.x,at.y,at.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.HOSTILE,1.1f,.65f);
    }
}
