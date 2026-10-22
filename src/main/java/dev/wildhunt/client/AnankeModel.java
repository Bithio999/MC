package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import dev.wildhunt.AnankeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Original six-winged ceremonial armor, articulated feather spars and an obsidian faceplate. */
final class AnankeModel extends EntityModel<AnankeEntity> {
    private final ModelPart root,head,leftArm,rightArm;
    private boolean damageFeedback;
    private final ModelPart[] wings=new ModelPart[6];
    AnankeModel(ModelPart root){this.root=root;head=root.getChild("head");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");for(int n=0;n<6;n++)wings[n]=root.getChild("wing_"+n);}
    static LayerDefinition layer(){
        var mesh=new MeshDefinition();var r=mesh.getRoot();
        r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-7,-16,-3,14,20,6)
            .texOffs(0,64).addBox(-7.5f,-17,-3.5f,15,3,7).addBox(-8,0,-4,16,4,8)
            .texOffs(64,0).addBox(-3,-13,-4,6,10,2).texOffs(64,64).addBox(-1.5f,-11,-4.6f,3,6,1),PartPose.ZERO);
        r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-5,-11,-4,10,11,8)
            .texOffs(64,0).addBox(-3.5f,-9,-4.7f,7,8,1).texOffs(0,64).addBox(-5.5f,-11.5f,-4.8f,11,2,9)
            .addBox(-6,-16,-1,2,7,3).addBox(4,-16,-1,2,7,3)
            .texOffs(64,64).addBox(-.7f,-8,-5,1.4f,5,1),PartPose.offset(0,-18,0));
        for(int side:new int[]{-1,1}){
            r.addOrReplaceChild(side<0?"left_arm":"right_arm",CubeListBuilder.create().texOffs(0,64).addBox(-3,-2,-3,6,5,6)
                .texOffs(64,0).addBox(-2,2,-2,4,15,4).texOffs(0,0).addBox(-2.6f,8,-2.6f,5.2f,7,5.2f)
                .texOffs(0,64).addBox(-2.8f,14,-2.8f,5.6f,3,5.6f),PartPose.offset(side*10,-13,0));
            r.addOrReplaceChild("leg_"+side,CubeListBuilder.create().texOffs(64,0).addBox(-2.5f,0,-2.5f,5,20,5).texOffs(0,64).addBox(-3,15,-3.5f,6,5,7),PartPose.offset(side*3.5f,4,0));
        }
        for(int n=0;n<7;n++){
            float x=(n-3)*2.5f;float length=19+(n%2)*3;
            r.addOrReplaceChild("robe_"+n,CubeListBuilder.create().texOffs(n%2==0?0:64,0).addBox(-1.6f,0,-.6f,3.2f,length,1.2f)
                .texOffs(0,64).addBox(-1.7f,length-2,-.8f,3.4f,2,1.6f),PartPose.offsetAndRotation(x,2,4.5f,0,0,(n-3)*.065f));
        }
        for(int n=0;n<3;n++)r.addOrReplaceChild("tabard_"+n,CubeListBuilder.create().texOffs(0,0).addBox(-1.8f,0,-.6f,3.6f,18+n%2*2,1.2f)
            .texOffs(0,64).addBox(-1.9f,16+n%2*2,-.8f,3.8f,2,1.6f),PartPose.offsetAndRotation((n-1)*4,3,-4.4f,0,0,(n-1)*.04f));
        for(int n=0;n<6;n++){
            int side=n%2==0?-1:1,tier=n/2;
            var wing=r.addOrReplaceChild("wing_"+n,CubeListBuilder.create().texOffs(0,64).addBox(side<0?-26:0,-1,0,26,2,2),PartPose.offsetAndRotation(side*5,-11+tier*5,4.5f,0,side*.12f,side*(tier-1)*.58f));
            for(int f=0;f<8;f++){
                float length=17-f*1.1f;float x=side*(4+f*3);
                wing.addOrReplaceChild("feather_"+f,CubeListBuilder.create().texOffs(0,0).addBox(-1.6f,0,-.7f,3.2f,length,1.4f)
                    .texOffs(0,64).addBox(-.45f,0,-.95f,.9f,length+1,1.8f).texOffs(64,64).addBox(-.7f,length-3,-1,1.4f,3.5f,2),PartPose.offsetAndRotation(x,0,0,0,0,side*.38f));
            }
        }
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(AnankeEntity entity,float limbSwing,float limbAmount,float age,float yaw,float pitch){
        damageFeedback=entity.hasDamageFeedback();
        root.getAllParts().forEach(ModelPart::resetPose);root.y=(float)Math.sin(age*.05)*.45f;head.yRot=yaw*(float)Math.PI/180;head.xRot=pitch*(float)Math.PI/180*.5f;
        float cast=entity.castPose(age-entity.tickCount);
        leftArm.xRot=-.15f-cast*.9f;rightArm.xRot=-.15f-cast*.9f;leftArm.zRot=.1f+cast*.3f;rightArm.zRot=-.1f-cast*.3f;
        for(int n=0;n<6;n++){int side=n%2==0?-1:1,tier=n/2;wings[n].zRot=side*((tier-1)*(.58f+entity.phase()*.04f)+.06f*(float)Math.sin(age*.065+tier));wings[n].yRot=side*(.14f+.1f*(float)Math.cos(age*.05+tier));}
    }
    @Override public void renderToBuffer(PoseStack p,VertexConsumer b,int light,int overlay,int color){// Bypass Tensura's global Pain Nullification overlay suppression for this
        // boss's confirmed-hit feedback only; the skill's gameplay protection stays ON.
        root.render(p,b,light,damageFeedback?net.minecraft.client.renderer.texture.OverlayTexture.pack(0,true):overlay,color);}
}
