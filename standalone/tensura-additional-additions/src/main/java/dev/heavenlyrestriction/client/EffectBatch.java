package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import java.util.*;

/** Reuse primitive storage and submit each material once per entity, avoiding per-facet draw calls. */
final class EffectBatch implements MultiBufferSource {
    private static final int STRIDE=11;
    private final Map<RenderType,Bucket> buckets=new HashMap<>();
    private final List<Bucket> active=new ArrayList<>();
    void begin(){for(var bucket:active){bucket.size=0;bucket.active=false;}active.clear();}
    @Override public VertexConsumer getBuffer(RenderType type){
        var bucket=buckets.computeIfAbsent(type,Bucket::new);
        if(!bucket.active){bucket.active=true;active.add(bucket);}return bucket;
    }
    void flush(MultiBufferSource destination){
        for(var bucket:active){if(bucket.size==0)continue;var mesh=destination.getBuffer(bucket.type);
            for(int n=0;n<bucket.size;n+=STRIDE){var v=bucket.vertices;
                mesh.addVertex(Float.intBitsToFloat(v[n]),Float.intBitsToFloat(v[n+1]),Float.intBitsToFloat(v[n+2]))
                    .setColor(v[n+3]).setUv(Float.intBitsToFloat(v[n+4]),Float.intBitsToFloat(v[n+5]))
                    .setOverlay(v[n+6]).setLight(v[n+7]).setNormal(Float.intBitsToFloat(v[n+8]),Float.intBitsToFloat(v[n+9]),Float.intBitsToFloat(v[n+10]));
            }
        }
    }
    private static final class Bucket implements VertexConsumer {
        final RenderType type;int[] vertices=new int[STRIDE*128];int size;boolean active;
        Bucket(RenderType type){this.type=type;}
        @Override public VertexConsumer addVertex(float x,float y,float z){
            if(size+STRIDE>vertices.length)vertices=Arrays.copyOf(vertices,vertices.length*2);
            vertices[size]=Float.floatToRawIntBits(x);vertices[size+1]=Float.floatToRawIntBits(y);vertices[size+2]=Float.floatToRawIntBits(z);size+=STRIDE;return this;
        }
        @Override public VertexConsumer setColor(int r,int g,int b,int a){vertices[size-STRIDE+3]=a<<24|r<<16|g<<8|b;return this;}
        @Override public VertexConsumer setUv(float u,float v){vertices[size-STRIDE+4]=Float.floatToRawIntBits(u);vertices[size-STRIDE+5]=Float.floatToRawIntBits(v);return this;}
        @Override public VertexConsumer setUv1(int u,int v){vertices[size-STRIDE+6]=u|(v<<16);return this;}
        @Override public VertexConsumer setUv2(int u,int v){vertices[size-STRIDE+7]=u|(v<<16);return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){vertices[size-STRIDE+8]=Float.floatToRawIntBits(x);vertices[size-STRIDE+9]=Float.floatToRawIntBits(y);vertices[size-STRIDE+10]=Float.floatToRawIntBits(z);return this;}
    }
}
