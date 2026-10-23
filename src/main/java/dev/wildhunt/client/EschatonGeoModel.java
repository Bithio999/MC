package dev.wildhunt.client;

import dev.wildhunt.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

final class EschatonGeoModel extends GeoModel<EschatonEntity> {
    @Override public ResourceLocation getModelResource(EschatonEntity e){return WildHunt.id("geo/myths/eschaton.geo.json");}
    @Override public ResourceLocation getTextureResource(EschatonEntity e){return WildHunt.id("textures/geo/eschaton.png");}
    @Override public ResourceLocation getAnimationResource(EschatonEntity e){return WildHunt.id("animations/myths/eschaton.animation.json");}
    @Override public void setCustomAnimations(EschatonEntity e,long id,AnimationState<EschatonEntity> s){
        super.setCustomAnimations(e,id,s);var data=s.getData(DataTickets.ENTITY_MODEL_DATA);
        if(data!=null)getBone("head").ifPresent(b->{b.setRotX(data.headPitch()*(float)Math.PI/180);b.setRotY(data.netHeadYaw()*(float)Math.PI/180);});
        float time=e.tickCount+s.getPartialTick(),unfold=(float)VfxMesh.smooth((e.level().getGameTime()+s.getPartialTick()-e.formStarted())/40d);
        for(String name:new String[]{"ruin_arms","ruin_mantle"})getBone(name).ifPresent(b->{boolean hide=e.form()<2;b.setHidden(hide);b.setChildrenHidden(hide);float scale=hide?.001f:Math.max(.001f,unfold);b.updateScale(scale,scale,scale);});
        getBone("last_crown").ifPresent(b->{boolean hide=e.form()<3;b.setHidden(hide);b.setChildrenHidden(hide);float scale=hide?.001f:Math.max(.001f,unfold);b.updateScale(scale,scale,scale);b.setRotY(time*.006f);});
        getBone("heart").ifPresent(b->{float pulse=1+.045f*(float)Math.sin(time*.11);b.updateScale(pulse,pulse,pulse);});
    }
}
