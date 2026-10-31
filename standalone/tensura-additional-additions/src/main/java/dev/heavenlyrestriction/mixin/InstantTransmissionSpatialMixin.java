package dev.heavenlyrestriction.mixin;
import dev.heavenlyrestriction.InstantTransmissionArt;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.subclass.ISpatialMovement;
import io.github.manasmods.tensura.network.c2s.RequestSpatialActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Reuse native coordinate packets while replacing their hard-coded MP payment for this Battlewill. */
@Mixin(RequestSpatialActionPacket.class)
public abstract class InstantTransmissionSpatialMixin {
    @Inject(method="handleWarp",at=@At("HEAD"),cancellable=true,remap=false)
    private static void transmission$auraWarp(ServerPlayer player,ManasSkillInstance i,ISpatialMovement spatial,double x,double y,double z,ResourceKey<Level> level,boolean pad,CallbackInfo out){
        if(spatial instanceof InstantTransmissionArt art){art.requestWarp(player,i,x,y,z,level,pad);out.cancel();}
    }
    @Inject(method="handlePortal",at=@At("HEAD"),cancellable=true,remap=false)
    private static void transmission$noPortal(ServerPlayer player,ManasSkillInstance i,ISpatialMovement spatial,double x,double y,double z,ResourceKey<Level> level,boolean pad,CallbackInfo out){if(spatial instanceof InstantTransmissionArt)out.cancel();}
}
