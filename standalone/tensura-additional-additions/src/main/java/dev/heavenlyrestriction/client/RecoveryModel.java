package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

final class RecoveryModel extends GeoModel<ArtVisualEntity> {
    @Override public ResourceLocation getModelResource(ArtVisualEntity e){return HeavenlyRestriction.id("geo/vital_corolla.geo.json");}
    @Override public ResourceLocation getTextureResource(ArtVisualEntity e){return HeavenlyRestriction.id("textures/geo/grove.png");}
    @Override public ResourceLocation getAnimationResource(ArtVisualEntity e){return HeavenlyRestriction.id("animations/vital_corolla.animation.json");}
    @Override public void setCustomAnimations(ArtVisualEntity e,long id,software.bernie.geckolib.animation.AnimationState<ArtVisualEntity> state){
        super.setCustomAnimations(e,id,state);double age=e.tickCount+state.getPartialTick();
        float f=(float)(VfxMesh.smooth(age/Math.min(5,e.lifetime()*.2))*VfxMesh.smooth((e.lifetime()-age)/Math.min(8,e.lifetime()*.25)));
        getBone("root").ifPresent(b->{b.updateScale(f,f,f);double radius=Math.max(.4,Math.abs(e.end().x)),outer=Math.min(1.5,radius/.85),spread=radius/.85/outer;for(var child:b.getChildBones()){child.setPosX((float)(-child.getPivotX()*(spread-1)));child.setPosZ((float)(child.getPivotZ()*(spread-1)));}});
    }
}
