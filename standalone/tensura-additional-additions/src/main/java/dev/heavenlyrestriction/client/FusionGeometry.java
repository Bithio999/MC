package dev.heavenlyrestriction.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import static dev.heavenlyrestriction.client.VfxMesh.*;
/** Opposed silk helices collapse into one luminous body, then breathe gently during Fusion. */
final class FusionGeometry {
    static void render(PoseStack p,MultiBufferSource b,float age,boolean ending){
        double intro=Math.max(0,1-age/32d),base=.64+.035*Math.sin(age*.07),radius=ending?.7+age*.07:base+intro*1.8;
        double alpha=ending?Math.max(0,1-age/32d):.18+.65*intro;
        for(int side=0;side<2;side++){int color=side==0?0xff68ffe0:0xffffd080;Vec3 old=null;
            for(int n=0;n<=80;n++){double t=n/80d,a=t*Math.PI*4+age*.055+side*Math.PI,y=t*2.4;
                Vec3 at=new Vec3(Math.cos(a)*radius*(.65+.35*Math.sin(t*Math.PI)),y,Math.sin(a)*radius*(.65+.35*Math.sin(t*Math.PI)));
                if(old!=null){beam(p,b,old,at,.012+.03*intro,tint(color,alpha));if(intro>.01)beam(p,b,old,at,.07*intro,tint(color,alpha*.15));}old=at;
            }
        }
        ring(p,b,new Vec3(0,.03,0),radius*1.15,.016,age*.02,0,tint(0xff8debdc,alpha));
        if(intro>.01){ring(p,b,new Vec3(0,1.15,0),radius*.85,.035,age*.03,1,tint(0xffffda8d,intro*.65));glow(p,b,new Vec3(0,1.2,0),intro*2.1,tint(0xffe5fff2,intro*.35));}
    }
}
