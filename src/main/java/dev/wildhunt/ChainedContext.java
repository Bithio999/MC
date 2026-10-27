package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import net.minecraft.world.entity.LivingEntity;

/** Server-call provenance, never a client-provided exemption or an ambient learning window. */
public final class ChainedContext implements AutoCloseable {
    private record Frame(LivingEntity owner,ManasSkill skill,boolean inherited,boolean initialRace) {}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private final Frame previous;
    private ChainedContext(Frame next){previous=CURRENT.get();CURRENT.set(next);}
    public static ChainedContext ability(LivingEntity owner,ManasSkill skill){
        var p=CURRENT.get();boolean same=p!=null && p.owner==owner;
        return new ChainedContext(new Frame(owner,skill,ChainedState.inherited(owner,skill) || same && p.inherited,same && p.initialRace));
    }
    public static ChainedContext reincarnation(LivingEntity owner,ManasSkill skill){return new ChainedContext(new Frame(owner,skill,true,initializing(owner)));}
    public static ChainedContext initialRace(LivingEntity owner){return new ChainedContext(new Frame(owner,null,inherited(owner),true));}
    public static boolean inherited(LivingEntity owner){var f=CURRENT.get();return f!=null && f.owner==owner && f.inherited;}
    public static boolean initializing(LivingEntity owner){var f=CURRENT.get();return f!=null && f.owner==owner && f.initialRace;}
    public static ManasSkill battlewill(LivingEntity owner){var f=CURRENT.get();return f!=null && f.owner==owner && f.skill instanceof Battlewill?f.skill:null;}
    @Override public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
