package dev.heavenlyrestriction.mixin.client;
import dev.heavenlyrestriction.fusion.Fusion;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Suppress the embedded original for vanilla and GeckoLib renderers alike. Proxy draws call the renderer directly. */
@Mixin(EntityRenderDispatcher.class)
public abstract class FusionRendererMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void fusion$singleVisibleBody(Entity e,double x,double y,double z,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){if(e instanceof LivingEntity living && Fusion.hidden(living))ci.cancel();}
}
