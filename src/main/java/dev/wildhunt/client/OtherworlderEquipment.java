package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildhunt.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Animated authored travel/work equipment, retaining native NPC bodies and equipment layers. */
final class OtherworlderEquipment extends GeoObjectRenderer<OtherworlderEquipment.Gear> {
    static final class Gear implements GeoAnimatable {
        final HunterOtherworlderEntity owner;
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        Gear(HunterOtherworlderEntity owner){this.owner=owner;}
        String name(){return owner.isAlma()?"alma_equipment":"gemma_equipment";}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public double getTick(Object related){return owner.tickCount;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"gear",4,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.myths.idle"))));}
    }
    private static final class Model extends GeoModel<Gear> {
        @Override public ResourceLocation getModelResource(Gear e){return WildHunt.id("geo/myths/"+e.name()+".geo.json");}
        @Override public ResourceLocation getTextureResource(Gear e){return WildHunt.id("textures/geo/"+(e.owner.isAlma()?"rune":"storm")+".png");}
        @Override public ResourceLocation getAnimationResource(Gear e){return WildHunt.id("animations/myths/"+e.name()+".animation.json");}
    }
    // Values hold their owner, so a WeakHashMap alone would still retain entities.
    private final java.util.Map<Integer,Gear> gears=new java.util.HashMap<>();
    OtherworlderEquipment(){super(new Model());}
    void renderEquipment(HunterOtherworlderEntity e,float partial,PoseStack p,MultiBufferSource buffers,int light){
        gears.values().removeIf(g->g.owner.isRemoved() || g.owner.level()!=e.level());
        if(e.isInvisible() || e.isSleeping())return;
        var gear=gears.compute(e.getId(),(id,old)->old!=null && old.owner==e?old:new Gear(e));
        p.pushPose();float yaw=net.minecraft.util.Mth.rotLerp(partial,e.yBodyRotO,e.yBodyRot);
        p.mulPose(Axis.YP.rotationDegrees(180-yaw));float scale=e.getScale();p.scale(scale,scale,scale);
        if(e.isOrderedToSit())p.translate(0,-.55,0);
        if(e.isVisuallySwimming())p.mulPose(Axis.XP.rotationDegrees(-80));
        // GeoObjectRenderer is block-centered by default; our authored equipment
        // uses the native character's feet as origin.
        p.translate(-.5,-.51,-.5);
        render(p,gear,buffers,null,null,light,partial);p.popPose();
    }
}
