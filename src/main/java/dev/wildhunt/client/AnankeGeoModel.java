package dev.wildhunt.client;

import dev.wildhunt.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

/** Original editable six-wing rig with separate flight and ritual controllers. */
final class AnankeGeoModel extends GeoModel<AnankeEntity> {
    @Override public ResourceLocation getModelResource(AnankeEntity e){return WildHunt.id("geo/myths/ananke.geo.json");}
    @Override public ResourceLocation getTextureResource(AnankeEntity e){return WildHunt.id("textures/geo/ananke.png");}
    @Override public ResourceLocation getAnimationResource(AnankeEntity e){return WildHunt.id("animations/myths/ananke.animation.json");}
    @Override public void setCustomAnimations(AnankeEntity e,long id,AnimationState<AnankeEntity> state){
        super.setCustomAnimations(e,id,state);var data=state.getData(DataTickets.ENTITY_MODEL_DATA);
        if(data!=null)getBone("head").ifPresent(b->{b.setRotX(data.headPitch()*(float)Math.PI/180);b.setRotY(data.netHeadYaw()*(float)Math.PI/180);});
        for(int n=0;n<6;n++){int side=n%2==0?-1:1;getBone("wing_"+n).ifPresent(b->b.setRotZ(b.getRotZ()+side*e.castPose(state.getPartialTick())*.16f));}
        for(int phase=2;phase<=3;phase++){int p=phase;getBone("phase_"+phase).ifPresent(b->b.setHidden(e.phase()<p));}
    }
}
