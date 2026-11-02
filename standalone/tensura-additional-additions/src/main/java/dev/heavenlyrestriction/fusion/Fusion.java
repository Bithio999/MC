package dev.heavenlyrestriction.fusion;
import dev.heavenlyrestriction.exponentiation.*;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import java.util.*;

/** A reversible shared body. No target is consumed and no inventory is copied. */
public final class Fusion {
    public static final int DURATION=3600,PENALTY=6000;
    public static final String STATE="heavenlyrestriction:fusion",LOAN="heavenlyrestriction:fusion_loan",ORIGIN="heavenlyrestriction:fusion_origin",ORIGINAL_TIME="heavenlyrestriction:fusion_original_time",SHADOW="heavenlyrestriction:fusion_shadow";
    private static final ResourceLocation BOOST=HeavenlyRestriction.id("fusion_body");
    private static final List<Holder<Attribute>> FORM=List.of(Attributes.ATTACK_DAMAGE,Attributes.MOVEMENT_SPEED,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS);
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();
    private record Invitation(UUID token,ServerPlayer caster,ServerPlayer target,long until){}
    private static final Map<UUID,Invitation> INVITES=new HashMap<>();
    public static final class Session {
        public final UUID token=UUID.randomUUID();public final ServerPlayer caster,pilot;public final LivingEntity target,hidden;public final boolean subordinate;public final long until;
        public final double aura,magicules;public final float health,initialHealth;public final double spirit,initialSpirit;public final List<Loan> loans=new ArrayList<>();public ArtVisualEntity visual;
        private Session(ServerPlayer caster,LivingEntity target){this.caster=caster;this.target=target;subordinate=!(target instanceof ServerPlayer);pilot=subordinate?caster:(ServerPlayer)target;hidden=subordinate?target:caster;until=caster.serverLevel().getGameTime()+DURATION;
            health=caster.getMaxHealth()+target.getMaxHealth();spirit=caster.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)+target.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);initialHealth=caster.getHealth()+target.getHealth();initialSpirit=TensuraStorages.getExistenceFrom(caster).getSpiritualHealth()+TensuraStorages.getExistenceFrom(target).getSpiritualHealth();aura=1.5*(EnergyHelper.getMaxAura(caster)+EnergyHelper.getMaxAura(target));magicules=1.5*(EnergyHelper.getMaxMagicule(caster)+EnergyHelper.getMaxMagicule(target));}
    }
    private record Loan(LivingEntity owner,LivingEntity borrower,ManasSkill skill,int originalTime){}
    public static java.util.function.Predicate<LivingEntity> clientHidden=e->false;
    public static Session session(LivingEntity e){return e==null?null:ACTIVE.get(e.getUUID());}
    public static boolean body(LivingEntity e){var s=session(e);return s!=null && s.pilot==e;}
    public static boolean hidden(LivingEntity e){if(e.level().isClientSide())return clientHidden.test(e);var s=session(e);return s!=null && s.hidden==e;}
    public static boolean embedded(Entity e){return e instanceof LivingEntity living && hidden(living);}
    public static double regenerationFactor(LivingEntity e){return body(e)?10:1;}
    public static double damageFactor(LivingEntity e){return body(e)?2:1;}
    public static boolean maintained(ServerPlayer p){return SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.FUSION.get()).filter(i->i.getMastery()>=0).isPresent();}
    public static boolean owned(ServerPlayer p){return SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.FUSION.get()).filter(i->i.getMastery()>=0 && i.canInteractSkill(p) && i.canActivateSkill(p,0)).isPresent();}
    public static boolean eligible(ServerPlayer p,LivingEntity target){return target!=null && target!=p && target.isAlive() && p.isAlive() && !p.isSpectator() && !target.isSpectator() && p.level()==target.level() && p.distanceToSqr(target)<=256 && session(p)==null && session(target)==null && owned(p) && !p.isPassenger() && !target.isPassenger() && (target instanceof ServerPlayer || SubordinateHelper.isSubordinate(p,target));}
    public static void press(ServerPlayer p){if(session(p)!=null){end(p,"Fusion ended.");return;}
        LivingEntity target=ObjectSelectionHelper.getTargetingEntity(p,16,false);
        if(!eligible(p,target)){p.displayClientMessage(Component.literal("Aim at your subordinate or a nearby player to fuse."),true);return;}
        if(target instanceof ServerPlayer player){
            invite(p,player);
        }else fuseSubordinate(p,target);
    }
    public static UUID invite(ServerPlayer p,ServerPlayer player){
        if(!eligible(p,player))return null;
        if(INVITES.values().stream().anyMatch(v->v.caster==p && v.until>p.serverLevel().getGameTime())){p.displayClientMessage(Component.literal("Your fusion invitation is still pending."),true);return null;}
        var request=new Invitation(UUID.randomUUID(),p,player,p.serverLevel().getGameTime()+400);INVITES.put(player.getUUID(),request);
        var tag=new CompoundTag();tag.putBoolean("invite",true);tag.putUUID("nonce",request.token);tag.putString("name",p.getDisplayName().getString());FusionNetwork.send(player,tag);p.displayClientMessage(Component.literal("Fusion invitation sent."),true);return request.token;
    }
    public static boolean fuseSubordinate(ServerPlayer p,LivingEntity target){return !(target instanceof ServerPlayer) && start(p,target,false);}
    public static boolean reply(ServerPlayer target,UUID token,boolean accept){var invite=INVITES.get(target.getUUID());
        if(invite==null || !invite.token.equals(token))return false;INVITES.remove(target.getUUID());
        if(!accept){invite.caster.displayClientMessage(Component.literal("Fusion invitation declined."),true);return false;}
        if(invite.until<target.serverLevel().getGameTime() || !eligible(invite.caster,target)){target.displayClientMessage(Component.literal("That fusion invitation has expired."),true);return false;}
        return start(invite.caster,target,true);
    }
    /** Player targets require a server-validated invitation; direct callers cannot bypass consent. */
    private static boolean start(ServerPlayer caster,LivingEntity target,boolean consent){
        // Fusion combines existing effective pools, even when invoked inside a native skill callback.
        try(var primary=ExponentiationContext.raw(caster,ExponentiationContext.FINAL);var secondary=ExponentiationContext.raw(target,ExponentiationContext.FINAL)){
            return startResolved(caster,target,consent);
        }
    }
    private static boolean startResolved(ServerPlayer caster,LivingEntity target,boolean consent){
        if(!eligible(caster,target) || target instanceof ServerPlayer && !consent)return false;
        var s=new Session(caster,target);if(!Double.isFinite(s.aura+s.magicules+s.health+s.spirit) || s.health<=0 || s.spirit<0)return false;
        snapshot(caster,s);snapshot(target,s);ACTIVE.put(caster.getUUID(),s);ACTIVE.put(target.getUUID(),s);
        try{
            if(s.subordinate){
                for(var a:FORM){double value=target.getAttributeValue(a);if(a.equals(Attributes.MOVEMENT_SPEED))value=io.github.manasmods.tensura.ability.subclass.ICloning.getConvertedMovementSpeed(value,false);setEffective(s.pilot,a,value);}
                caster.teleportTo(target.getX(),target.getY(),target.getZ());
                loan(s,target,caster);loan(s,caster,target);
            }else loan(s,caster,target);
            setEffective(s.pilot,TensuraAttributes.MAX_AURA,s.aura);setEffective(s.pilot,TensuraAttributes.MAX_MAGICULE,s.magicules);setEffective(s.pilot,Attributes.MAX_HEALTH,s.health);setEffective(s.pilot,TensuraAttributes.MAX_SPIRITUAL_HEALTH,s.spirit);
            var ca=TensuraStorages.getExistenceFrom(caster);var ta=TensuraStorages.getExistenceFrom(target);double ap=1.5*(ca.getAura()+ta.getAura()),mp=1.5*(ca.getMagicule()+ta.getMagicule());
            try(var transfer=ExponentiationContext.raw(s.pilot,ExponentiationContext.RESTORE)){var pool=TensuraStorages.getExistenceFrom(s.pilot);s.pilot.setHealth(Math.min(s.health,s.initialHealth));pool.setSpiritualHealth(Math.min(s.spirit,s.initialSpirit));pool.setAura(Math.min(s.aura,ap));pool.setMagicule(Math.min(s.magicules,mp));pool.markDirty();}
            if(s.subordinate){setEffective(target,TensuraAttributes.MAX_AURA,s.aura);setEffective(target,TensuraAttributes.MAX_MAGICULE,s.magicules);if(target instanceof Mob mob){mob.getNavigation().stop();mob.setNoAi(true);mob.setTarget(null);}}
            else{caster.setGameMode(GameType.SPECTATOR);if(caster.server.getPlayerList().getPlayer(caster.getUUID())==caster)caster.setCamera(target);}
            s.hidden.setInvisible(true);s.hidden.setInvulnerable(true);s.hidden.setNoGravity(true);s.hidden.noPhysics=true;
            SkillAPI.getSkillsFrom(caster).getSkill(HeavenlyRestriction.FUSION.get()).ifPresent(i->i.addMasteryPoint(caster));
            s.visual=ArtVisualEntity.spawn(s.pilot.level(),ArtVisualEntity.FUSION,s.pilot.position(),Vec3.ZERO,DURATION);s.visual.setCaster(s.pilot);
            burst(s.pilot,true);broadcast(s,false);return true;
        }catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger("TensuraAdditionalAdditions/Fusion").error("Could not form fusion",ex);end(caster,"Fusion could not form. Both bodies were restored.");return false;}
    }
    private static void snapshot(LivingEntity e,Session s){var t=new CompoundTag();t.putInt("vitalsVersion",2);t.putFloat("lastHealth",Math.clamp(s.initialHealth/Math.max(.01f,s.health),0,1));t.putDouble("lastSpirit",Math.clamp(s.initialSpirit/Math.max(.01,s.spirit),0,1));t.putDouble("maxSpirit",e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));t.putUUID("token",s.token);t.putBoolean("invisible",e.isInvisible());t.putBoolean("invulnerable",e.isInvulnerable());t.putBoolean("gravity",e.isNoGravity());t.putBoolean("physics",e.noPhysics);t.putDouble("lastAura",Math.clamp(TensuraStorages.getExistenceFrom(e).getAura()/Math.max(1,EnergyHelper.getMaxAura(e)),0,1));t.putDouble("lastMagicule",Math.clamp(TensuraStorages.getExistenceFrom(e).getMagicule()/Math.max(1,EnergyHelper.getMaxMagicule(e)),0,1));t.putDouble("aura",EnergyHelper.getMaxAura(e));t.putDouble("magicules",EnergyHelper.getMaxMagicule(e));t.putDouble("spirit",TensuraStorages.getExistenceFrom(e).getSpiritualHealth());t.putFloat("health",e.getHealth());t.putFloat("maxHealth",e.getMaxHealth());
        if(e instanceof Mob mob)t.putBoolean("noAI",mob.isNoAi());if(e instanceof ServerPlayer p)t.putInt("gameMode",p.gameMode.getGameModeForPlayer().getId());e.getPersistentData().put(STATE,t);}
    /** Adjust effective capacity without rewriting base EP or other mods' modifiers. */
    public static void setEffective(LivingEntity e,Holder<Attribute> type,double desired){
        var a=e.getAttribute(type);if(a==null)return;
        // Solve the temporary modifier in the underlying linear attribute space.
        // The requested capacity already includes Exponentiation, so invert that lens once.
        if(desired>1 && Exponentiation.active(e) && Exponentiation.rule(type.value()).included())desired=Math.pow(desired,1/Exponentiation.POWER);
        try(var calculation=ExponentiationContext.raw(e,ExponentiationContext.ATTRIBUTES)){
            a.removeModifier(BOOST);double base=a.getValue();a.addTransientModifier(new AttributeModifier(BOOST,1,AttributeModifier.Operation.ADD_VALUE));double delta=a.getValue()-base;a.removeModifier(BOOST);if(Math.abs(delta)>1e-10)a.addTransientModifier(new AttributeModifier(BOOST,(desired-base)/delta,AttributeModifier.Operation.ADD_VALUE));
        }
    }
    private static void loan(Session s,LivingEntity owner,LivingEntity borrower){
        var skills=SkillAPI.getSkillsFrom(borrower);
        for(var original:List.copyOf(SkillAPI.getSkillsFrom(owner).getLearnedSkills())){
            var previous=skills.getSkill(original.getSkill());if(original.getMastery()<0 || original.isSubInstance() || original.getRemoveTime()==0 || previous.filter(i->i.getMastery()>=0).isPresent())continue;
            var copy=original.copy();copy.setRemoveTime(original.getRemoveTime()>0?Math.min(DURATION/20,original.getRemoveTime()):-2);copy.getOrCreateTag().putUUID(LOAN,s.token);previous.ifPresent(i->copy.getOrCreateTag().put(SHADOW,i.toNBT()));copy.getOrCreateTag().putUUID(ORIGIN,owner.getUUID());copy.getOrCreateTag().putInt(ORIGINAL_TIME,original.getRemoveTime());copy.setToggled(false);
            // Shared storage/banks must have one authoritative state, never two extractable inventories.
            skills.updateSkill(copy,true);if(skills.getSkill(copy.getSkill()).filter(i->i.getTag()!=null && i.getTag().hasUUID(LOAN)).isPresent())s.loans.add(new Loan(owner,borrower,original.getSkill(),original.getRemoveTime()));
        }
    }
    private static void syncLoans(Session s){for(var loan:s.loans){var storage=SkillAPI.getSkillsFrom(loan.borrower);storage.getSkill(loan.skill).filter(i->i.getTag()!=null && i.getTag().hasUUID(LOAN) && i.getTag().getUUID(LOAN).equals(s.token)).ifPresent(i->{
        var original=SkillAPI.getSkillsFrom(loan.owner).getSkill(loan.skill);if(original.isEmpty()){if(loan.originalTime!=-1)removeLoan(loan.borrower,i);return;}var copy=i.copy();copy.getOrCreateTag().remove(LOAN);copy.getOrCreateTag().remove(ORIGIN);copy.getOrCreateTag().remove(ORIGINAL_TIME);copy.getOrCreateTag().remove(SHADOW);copy.setRemoveTime(loan.originalTime>=0?Math.max(0,Math.min(original.get().getRemoveTime(),i.getRemoveTime())):loan.originalTime);copy.setToggled(original.get().isToggled());SkillAPI.getSkillsFrom(loan.owner).updateSkill(copy,true);
    });}}
    public static void end(LivingEntity participant,String reason){var s=session(participant);if(s==null)return;
        syncLoans(s);if(s.visual!=null)s.visual.discard();ACTIVE.remove(s.caster.getUUID());ACTIVE.remove(s.target.getUUID());
        double ap=s.aura<=0?0:Math.clamp(TensuraStorages.getExistenceFrom(s.pilot).getAura()/s.aura,0,1),mp=s.magicules<=0?0:Math.clamp(TensuraStorages.getExistenceFrom(s.pilot).getMagicule()/s.magicules,0,1);float hp=Math.clamp(s.pilot.getHealth()/Math.max(.01f,s.health),0,1);
        double spirit=Math.clamp(TensuraStorages.getExistenceFrom(s.pilot).getSpiritualHealth()/Math.max(.01,s.spirit),0,1);var at=s.pilot.position();for(var e:List.of(s.caster,s.target)){restore(e,ap,mp,e==s.pilot && !e.isAlive()?0:Math.max(.01f,hp),Math.max(.01,spirit));if(e.isAlive()){penalty(e);if(e.level()==s.pilot.level()){if(e instanceof ServerPlayer p)p.teleportTo(at.x,at.y,at.z);else e.teleportTo(at.x,at.y,at.z);}}}
        // Separate with room to move; never place a body inside a wall.
        Vec3 side=at.add(1.5,0,0);if(s.hidden.level()==s.pilot.level() && s.hidden.level().noCollision(s.hidden,s.hidden.getBoundingBox().move(side.subtract(s.hidden.position()))))s.hidden.teleportTo(side.x,side.y,side.z);
        var release=ArtVisualEntity.spawn(s.pilot.level(),ArtVisualEntity.FUSION,s.pilot.position(),Vec3.ZERO,32);release.style(1);burst(s.pilot,false);broadcast(s,true);s.caster.displayClientMessage(Component.literal(reason),true);if(s.target instanceof ServerPlayer p)p.displayClientMessage(Component.literal(reason),true);
    }
    private static void removeLoan(LivingEntity owner,ManasSkillInstance loan){var shadow=loan.getTag()!=null && loan.getTag().contains(SHADOW)?loan.getTag().getCompound(SHADOW).copy():null;
        SkillAPI.getSkillsFrom(owner).forgetSkill(loan);if(shadow!=null)SkillAPI.getSkillsFrom(owner).updateSkill(ManasSkillInstance.fromNBT(shadow),true);
    }
    private static void restore(LivingEntity e,double auraFraction,double magiculeFraction,float healthFraction,double spiritFraction){var tag=e.getPersistentData().getCompound(STATE);
        for(var a:FORM)if(e.getAttribute(a)!=null)e.getAttribute(a).removeModifier(BOOST);for(var a:List.of(TensuraAttributes.MAX_AURA,TensuraAttributes.MAX_MAGICULE,Attributes.MAX_HEALTH,TensuraAttributes.MAX_SPIRITUAL_HEALTH))if(e.getAttribute(a)!=null)e.getAttribute(a).removeModifier(BOOST);
        for(var i:List.copyOf(SkillAPI.getSkillsFrom(e).getLearnedSkills()))if(i.getTag()!=null && i.getTag().hasUUID(LOAN))removeLoan(e,i);
        ChainedState.refresh(e);
        if(tag.isEmpty())return;e.setInvisible(tag.getBoolean("invisible"));e.setInvulnerable(tag.getBoolean("invulnerable"));e.setNoGravity(tag.getBoolean("gravity"));e.noPhysics=tag.getBoolean("physics");if(e instanceof Mob mob)mob.setNoAi(tag.getBoolean("noAI"));
        if(e instanceof ServerPlayer p){if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.setCamera(p);if(tag.contains("gameMode"))p.setGameMode(GameType.byId(tag.getInt("gameMode")));}
        try(var transfer=ExponentiationContext.raw(e,ExponentiationContext.RESTORE|ExponentiationContext.FINAL)){var pool=TensuraStorages.getExistenceFrom(e);pool.setAura(Math.min(EnergyHelper.getMaxAura(e),tag.getDouble("aura")*auraFraction));pool.setMagicule(Math.min(EnergyHelper.getMaxMagicule(e),tag.getDouble("magicules")*magiculeFraction));pool.setSpiritualHealth(Math.min(e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),(tag.getInt("vitalsVersion")>=2?tag.getDouble("maxSpirit"):tag.getDouble("spirit"))*spiritFraction));pool.markDirty();
        if(e.isAlive())e.setHealth(Math.min(e.getMaxHealth(),(tag.getInt("vitalsVersion")>=2?tag.getFloat("maxHealth"):tag.getFloat("health"))*healthFraction));e.getPersistentData().remove(STATE);}
    }
    private static void penalty(LivingEntity e){e.forceAddEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.PARALYSIS),PENALTY,1),null);e.forceAddEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.FRAGILITY),PENALTY,1),null);}
    public static void recover(LivingEntity e){if(e.level().isClientSide() || session(e)!=null)return;
        for(var i:List.copyOf(SkillAPI.getSkillsFrom(e).getLearnedSkills()))if(i.getTag()!=null && i.getTag().hasUUID(LOAN)){
            if(i.getTag().hasUUID(ORIGIN)){var owner=i.getTag().getUUID(ORIGIN);var copy=i.copy();int originalTime=i.getTag().getInt(ORIGINAL_TIME);copy.setRemoveTime(originalTime>=0?Math.min(originalTime,Math.max(0,i.getRemoveTime())):originalTime);copy.getOrCreateTag().remove(LOAN);copy.getOrCreateTag().remove(ORIGIN);copy.getOrCreateTag().remove(ORIGINAL_TIME);copy.getOrCreateTag().remove(SHADOW);
                var server=e.getServer();if(server!=null){var returns=FusionReturns.get(server);returns.put(owner,copy);for(var level:server.getAllLevels()){var original=level.getEntity(owner);if(original instanceof LivingEntity living){returns.apply(living);break;}}}
            }
            removeLoan(e,i);
        }
        if(e.getServer()!=null)FusionReturns.get(e.getServer()).apply(e);
        if(e.getPersistentData().contains(STATE)){var state=e.getPersistentData().getCompound(STATE);restore(e,state.getDouble("lastAura"),state.getDouble("lastMagicule"),state.contains("lastHealth")?state.getFloat("lastHealth"):1,state.contains("lastSpirit")?state.getDouble("lastSpirit"):1);penalty(e);}
    }
    public static void expire(LivingEntity participant,long now){var s=session(participant);if(s!=null && now>=s.until)end(participant,"Fusion duration ended.");}
    private static void tick(MinecraftServer server){long now=server.overworld().getGameTime();INVITES.values().removeIf(v->v.until<now || v.caster.hasDisconnected() || v.target.hasDisconnected());
        for(var s:new HashSet<>(ACTIVE.values())){
            if(s.caster.server!=server)continue;expire(s.caster,s.pilot.level().getGameTime());if(session(s.caster)==null)continue;
            if(!s.caster.isAlive() || !s.target.isAlive() || s.target.isRemoved() || s.caster.hasDisconnected() || s.target instanceof ServerPlayer p && p.hasDisconnected() || s.caster.level()!=s.target.level() || !maintained(s.caster)){end(s.caster,"Fusion ended.");continue;}
            s.hidden.setPos(s.pilot.position());s.hidden.setDeltaMovement(Vec3.ZERO);s.hidden.setInvisible(true);s.hidden.setInvulnerable(true);
            if(s.subordinate){var pool=TensuraStorages.getExistenceFrom(s.target);var body=TensuraStorages.getExistenceFrom(s.pilot);try(var transfer=ExponentiationContext.raw(s.target,ExponentiationContext.RESTORE)){pool.setAura(body.getAura());pool.setMagicule(body.getMagicule());pool.markDirty();}}
            if(now%20==0){
                setEffective(s.pilot,TensuraAttributes.MAX_AURA,s.aura);setEffective(s.pilot,TensuraAttributes.MAX_MAGICULE,s.magicules);setEffective(s.pilot,Attributes.MAX_HEALTH,s.health);setEffective(s.pilot,TensuraAttributes.MAX_SPIRITUAL_HEALTH,s.spirit);
                for(var e:List.of(s.caster,s.target)){var state=e.getPersistentData().getCompound(STATE);state.putDouble("lastAura",Math.clamp(TensuraStorages.getExistenceFrom(s.pilot).getAura()/Math.max(1,s.aura),0,1));state.putDouble("lastMagicule",Math.clamp(TensuraStorages.getExistenceFrom(s.pilot).getMagicule()/Math.max(1,s.magicules),0,1));state.putFloat("lastHealth",s.pilot.getHealth()/Math.max(.01f,s.health));state.putDouble("lastSpirit",TensuraStorages.getExistenceFrom(s.pilot).getSpiritualHealth()/Math.max(.01,s.spirit));}
                syncLoans(s);broadcast(s,false);
            }
        }
    }
    private static void broadcast(Session s,boolean ended){var t=new CompoundTag();t.putUUID("token",s.token);t.putBoolean("ended",ended);t.putInt("pilot",s.pilot.getId());t.putInt("hidden",s.hidden.getId());t.putInt("caster",s.caster.getId());t.putBoolean("subordinate",s.subordinate);t.putInt("ticks",(int)Math.max(0,s.until-s.pilot.level().getGameTime()));t.putString("name",s.target.getDisplayName().getString());t.putDouble("ep",s.aura+s.magicules);for(var p:s.caster.server.getPlayerList().getPlayers())if(p.level()==s.pilot.level() && p.distanceToSqr(s.pilot)<256*256 || p==s.caster || p==s.target)FusionNetwork.send(p,t);}
    private static void burst(LivingEntity e,boolean joining){if(!(e.level() instanceof net.minecraft.server.level.ServerLevel l))return;
        for(int n=0;n<96;n++){double angle=n*Math.PI/12,r=joining?2.3*(1-n/110d):n/38d;var color=n%2==0?new Vector3f(.25f,1,.87f):new Vector3f(1,.72f,.22f);l.sendParticles(new DustParticleOptions(color,1.25f),e.getX()+Math.cos(angle)*r,e.getY()+.15+n/48d,e.getZ()+Math.sin(angle)*r,1,0,0,0,0);}
        l.sendParticles(ParticleTypes.FLASH,e.getX(),e.getY()+1,e.getZ(),1,0,0,0,0);l.playSound(null,e.blockPosition(),joining?SoundEvents.BEACON_ACTIVATE:SoundEvents.BEACON_DEACTIVATE,SoundSource.PLAYERS,1,joining?1.2f:.8f);
    }
    public static void register(){
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e)->tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e)->{for(var s:new HashSet<>(ACTIVE.values()))end(s.caster,"Fusion ended.");INVITES.clear();});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->{end(e.getEntity(),"Fusion ended: a participant disconnected.");INVITES.values().removeIf(i->i.caster==e.getEntity() || i.target==e.getEntity());});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->end(e.getEntity(),"Fusion ended: a participant changed dimension."));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->recover(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e)->{if(!e.getLevel().isClientSide() && e.getEntity() instanceof LivingEntity living)recover(living);});
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(LivingDeathEvent e)->end(e.getEntity(),"Fusion ended: the fused body fell."));
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,(LivingIncomingDamageEvent e)->{if(hidden(e.getEntity()))e.setCanceled(true);});
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(LivingDamageEvent.Pre e)->{var attacker=RestrictionData.attacker(e.getSource());e.setNewDamage((float)(e.getNewDamage()*damageFactor(attacker)));});
        io.github.manasmods.tensura.event.TensuraEntityEvents.SPIRITUAL_HURT_EVENT.register((target,attacker,amount,resistance,damage,source)->{if(attacker instanceof LivingEntity living && body(living))damage.set(damage.get()*2);return dev.architectury.event.EventResult.pass();});
    }
}
