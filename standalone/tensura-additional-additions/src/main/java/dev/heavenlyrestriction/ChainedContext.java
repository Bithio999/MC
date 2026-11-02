package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import net.minecraft.world.entity.LivingEntity;

/** Server-call provenance, never a client-provided exemption or an ambient learning window. */
public final class ChainedContext implements AutoCloseable {
    private record Frame(LivingEntity owner,ManasSkill skill,boolean inherited,boolean initialRace,int recoveryTier) {}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private final Frame previous;
    private dev.heavenlyrestriction.exponentiation.ExponentiationContext exponentiation;
    private ChainedContext(Frame next){previous=CURRENT.get();CURRENT.set(next);}
    public static ChainedContext ability(LivingEntity owner,ManasSkill skill){
        var p=CURRENT.get();boolean same=p!=null && p.owner==owner;
        var scope=new ChainedContext(new Frame(owner,skill,ChainedState.inherited(owner,skill) || same && p.inherited,same && p.initialRace,same?p.recoveryTier:0));
        scope.exponentiation=dev.heavenlyrestriction.exponentiation.ExponentiationContext.raw(owner,dev.heavenlyrestriction.exponentiation.ExponentiationContext.ALL);return scope;
    }
    public static ChainedContext reincarnation(LivingEntity owner,ManasSkill skill){return new ChainedContext(new Frame(owner,skill,true,initializing(owner),0));}
    public static ChainedContext initialRace(LivingEntity owner){return new ChainedContext(new Frame(owner,null,inherited(owner),true,0));}
    public static ChainedContext racialRecovery(LivingEntity owner,int tier){return new ChainedContext(new Frame(owner,null,inherited(owner),initializing(owner),tier));}
    public static boolean permitsRecovery(LivingEntity owner,ManasSkill skill){var f=CURRENT.get();return f!=null && f.owner==owner && skill instanceof AuraRegenerationSkill recovery && recovery.tier<=f.recoveryTier;}
    public static boolean inherited(LivingEntity owner){var f=CURRENT.get();return f!=null && f.owner==owner && f.inherited;}
    public static boolean initializing(LivingEntity owner){var f=CURRENT.get();return f!=null && f.owner==owner && f.initialRace;}
    public static ManasSkill battlewill(LivingEntity owner){var f=CURRENT.get();return f!=null && f.owner==owner && f.skill instanceof Battlewill?f.skill:null;}
    @Override public void close(){if(exponentiation!=null)exponentiation.close();if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
