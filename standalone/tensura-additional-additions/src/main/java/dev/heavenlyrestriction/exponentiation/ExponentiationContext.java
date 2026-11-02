package dev.heavenlyrestriction.exponentiation;
import net.minecraft.world.entity.LivingEntity;
/** Exception-safe calculation provenance. Final output writers can opt back into final capacities. */
public final class ExponentiationContext implements AutoCloseable {
    public static final int DAMAGE=1,RECOVERY=2,GROWTH=4,LEARNING=8,MINING=16,ALL=31,FINAL=32,RESTORE=64,CAPACITY=128,ATTRIBUTES=256;
    private static final ThreadLocal<ExponentiationContext> CURRENT=new ThreadLocal<>();
    private final LivingEntity owner;private final int mask;private final ExponentiationContext previous;
    private ExponentiationContext(LivingEntity owner,int mask){this.owner=owner;this.mask=mask;previous=CURRENT.get();CURRENT.set(this);}
    public static ExponentiationContext raw(LivingEntity owner,int mask){return new ExponentiationContext(owner,mask);}
    public static boolean rawAttribute(LivingEntity owner,int role){for(var f=CURRENT.get();f!=null;f=f.previous)if(f.owner==owner){if((f.mask&FINAL)!=0 && (role&CAPACITY)!=0)return false;if((f.mask&ATTRIBUTES)!=0 || (f.mask&role)!=0)return true;}return false;}
    public static boolean restoring(LivingEntity owner){for(var f=CURRENT.get();f!=null;f=f.previous)if(f.owner==owner && (f.mask&RESTORE)!=0)return true;return false;}
    @Override public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
