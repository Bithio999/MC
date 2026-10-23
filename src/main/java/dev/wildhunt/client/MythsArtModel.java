package dev.wildhunt.client;

import dev.wildhunt.*;
import dev.wildhunt.visual.VisualRig;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Editor-authored geometry/keyframes. Gameplay shape/timing remain server owned. */
final class MythsArtModel extends GeoModel<ArtVisualEntity> {
    @Override public ResourceLocation getModelResource(ArtVisualEntity e){
        String name=VisualRig.forKind(e.kind()).asset();
        if(e.kind()==ArtVisualEntity.ELEMENT_IMPACT && e.style()>0)name+="_"+(e.style()==1?"tide":"volcanic");
        return WildHunt.id("geo/myths/"+name+".geo.json");
    }
    @Override public ResourceLocation getTextureResource(ArtVisualEntity e){
        String theme=VisualRig.forKind(e.kind()).theme();if(e.kind()==ArtVisualEntity.ELEMENT_IMPACT)theme=e.style()==1?"tide":e.style()==2?"volcanic":theme;
        return WildHunt.id("textures/geo/"+theme+".png");
    }
    @Override public ResourceLocation getAnimationResource(ArtVisualEntity e){return WildHunt.id("animations/myths/"+VisualRig.forKind(e.kind()).asset()+".animation.json");}
    @Override public void setCustomAnimations(ArtVisualEntity e,long id,software.bernie.geckolib.animation.AnimationState<ArtVisualEntity> state){
        super.setCustomAnimations(e,id,state);
        float age=e.tickCount+state.getPartialTick();
        if(e.kind()==ArtVisualEntity.ESCHATON_GENESIS){
            for(int n=0;n<7;n++){int index=n;getBone("star_"+n).ifPresent(b->{
                var plan=e.barrage();double since=e.barrageAge(state.getPartialTick())-EschatonPattern.meteorTick(index);boolean visible=index<plan.getInt("warned") && since<8;
                b.setHidden(!visible);b.setChildrenHidden(!visible);var at=EschatonPattern.meteor(plan,index).add(0,since<0?16+3*Math.sin(index):Math.max(0,16-since*4),0);
                b.setPosX((float)(-at.x*16));b.setPosY((float)(at.y*16-8));b.setPosZ((float)(at.z*16));
            });}
        }
        if(e.kind()==ArtVisualEntity.LASH){
            double length=e.end().length(),height=.7+length*.04;
            for(int n=0;n<10;n++){
                double t=n/10d,along=n*(length/10-.125)*16,curve=Math.sin(t*Math.PI)*height,slope=Math.cos(t*Math.PI)*Math.PI*height/Math.max(1,length);
                getBone("tendon_"+n).ifPresent(b->{b.setPosY((float)(curve*16));b.setPosZ((float)along);b.updateScale(1,1,(float)Math.max(.1,length/1.25));b.setRotX((float)-Math.atan(slope));});
            }
        }
        if(e.kind()==ArtVisualEntity.RECONSTRUCTED_CROWN)for(int n=0;n<7;n++)getBone("blade_"+n).ifPresent(b->b.setPosY((float)(64*VfxMesh.smooth(age/10d))));
        // Smooth entrance/release independent of render FPS; no time-based attack logic here.
        float f=(float)(VfxMesh.smooth(age/Math.min(5,e.lifetime()*.2))*VfxMesh.smooth((e.lifetime()-age)/Math.min(8,e.lifetime()*.25)));
        getBone("root").ifPresent(b->{
            b.updateScale(f,f,f);
            var placement=VisualRig.forKind(e.kind()).placement();
            boolean radial=placement==VisualRig.Placement.RADIAL || placement==VisualRig.Placement.FOLLOW || placement==VisualRig.Placement.LIBRARY;
            if(radial){
                double radius=Math.max(.4,Math.abs(e.end().x)),outer=Math.min(1.5,radius/.85),spread=radius/.85/outer;
                for(var child:b.getChildBones()){
                    // Spread authored assemblies around the actual field without
                    // inflating books, individual trees or mirror facets into walls.
                    child.setPosX((float)(-child.getPivotX()*(spread-1)));
                    child.setPosZ((float)(child.getPivotZ()*(spread-1)));
                }
            }
        });
    }
}
