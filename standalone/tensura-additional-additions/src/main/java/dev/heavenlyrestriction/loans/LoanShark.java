package dev.heavenlyrestriction.loans;
import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.exponentiation.*;
import dev.heavenlyrestriction.fusion.Fusion;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.world.TensuraGameRules;
import io.github.manasmods.tensura.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.lang.ref.WeakReference;
import java.util.*;

public final class LoanShark {
    public static final String TOKEN="heavenlyrestriction:loan_shark_contract";
    public static final long DEFAULT_TICKS=12000; // ten minutes at 20 TPS
    public static long claimTicks(LivingEntity lender){return SkillAPI.getSkillsFrom(lender).getSkill(HeavenlyRestriction.LOAN_SHARK.get()).filter(i->i.getMastery()>=0 && i.isMastered(lender)).isPresent()?9600:DEFAULT_TICKS;}
    private static final ThreadLocal<Integer> TRANSFERRING=ThreadLocal.withInitial(()->0);
    private static final Map<UUID,WeakReference<LivingEntity>> SEEN=new HashMap<>();
    private static final Map<UUID,Long> PULSES=new HashMap<>();
    private record Offer(UUID nonce,UUID sender,UUID receiver,ResourceLocation skill,long expires){}
    private static final Map<UUID,Offer> OFFERS=new HashMap<>();
    private record Session(UUID token,UUID target,long expires,int mode){}
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<Class<?>,Boolean> SLOT_PASSIVE=new HashMap<>(),PURE_PASSIVE=new HashMap<>();
    public static void register(){NeoForge.EVENT_BUS.addListener(LoanShark::tick);NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e)->{if(e.getEntity() instanceof LivingEntity living)borrowerLoaded(living);});NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e)->{if(e.getEntity() instanceof LivingEntity living)retireDeadBorrower(living);});NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e)->borrowerLoaded(e.getEntity()));NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event)->{var ability=((io.github.manasmods.tensura.damage.TensuraDamageSource)event.getSource()).tensura$getAbilityInstance();var owner=RestrictionData.attacker(event.getSource());if(ability!=null && event.getNewDamage()>0)pulse(owner,ability,20);});NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e)->{SEEN.clear();PULSES.clear();SESSIONS.clear();OFFERS.clear();});}
    public static void see(LivingEntity e){SEEN.put(e.getUUID(),new WeakReference<>(e));}
    public static LivingEntity resolve(MinecraftServer s,UUID id){
        var p=s.getPlayerList().getPlayer(id);if(p!=null)return p;
        for(var level:s.getAllLevels())if(level.getEntity(id) instanceof LivingEntity e){
            if(retireDeadBorrower(e))return null;
            if(!e.isRemoved())return e;
        }
        var ref=SEEN.get(id);var e=ref==null?null:ref.get();
        if(e!=null && e.getServer()==s){
            if(retireDeadBorrower(e)){SEEN.remove(id);return null;}
            if(!e.isRemoved() && (!(e instanceof ServerPlayer player) || !player.hasDisconnected()))return e;
        }
        SEEN.remove(id);return null;
    }
    private static void close(LoanLedger d,LoanLedger.Loan l,String status){
        l.status=status;l.wasActive=false;PULSES.remove(l.id);d.closed(l);
    }
    /** A removed-but-healthy mob may merely be unloaded. Player deaths preserve their loans. */
    public static boolean retireDeadBorrower(LivingEntity e){
        if(e.level().isClientSide() || e.getServer()==null || e instanceof net.minecraft.world.entity.player.Player)return false;
        if(e.getHealth()>0 && e.getRemovalReason()!=Entity.RemovalReason.KILLED)return false;
        var d=LoanLedger.get(e.getServer());
        for(var l:List.copyOf(d.forBorrower(e.getUUID())))close(d,l,"Deceased");
        return true;
    }
    /** Revoke a closed loan's marked copies if an unavailable borrower later loads again. */
    public static void borrowerLoaded(LivingEntity e){
        if(e.level().isClientSide() || e.getServer()==null || retireDeadBorrower(e))return;
        var d=LoanLedger.get(e.getServer());
        if(d.loans.isEmpty())return;
        for(var i:List.copyOf(SkillAPI.getSkillsFrom(e).getLearnedSkills())){
            if(i.getTag()==null || !i.getTag().hasUUID(TOKEN))continue;
            var loan=d.loans.get(i.getTag().getUUID(TOKEN));
            if(loan!=null && !loan.open())SkillAPI.getSkillsFrom(e).forgetSkill(i);
        }
    }
    public static boolean owns(ServerPlayer p){return p.isAlive() && !p.isSpectator() && SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.LOAN_SHARK.get()).filter(i->i.getMastery()>=0 && i.canInteractSkill(p)).isPresent();}
    public static boolean eligible(ServerPlayer p,LivingEntity target){return owns(p) && target!=null && target!=p && target.isAlive() && !target.isSpectator() && target.level()==p.level() && p.distanceToSqr(target)<=256 && !Fusion.hidden(target) && (target instanceof ServerPlayer || SubordinateHelper.isSubordinate(p,target));}
    public static boolean lendable(ManasSkillInstance i){return i.getMastery()>=0 && !i.isTemporarySkill() && !i.isSubInstance() && i.getSkill()!=HeavenlyRestriction.LOAN_SHARK.get() && (i.getTag()==null || !i.getTag().hasUUID(TOKEN));}
    /** Copy learned parameters, not bank inventories, active effects or other players' stored items. */
    public static ManasSkillInstance copy(ManasSkillInstance source){var c=source.getSkill().createDefaultInstance();c.setMastery(source.getMastery());c.setToggled(false);int modes=Math.min(256,source.getModes());for(int mode=0;mode<modes;mode++){c.setCoolDown(source.getCoolDown(mode),mode);if(source.getSkill() instanceof TensuraSkill skill && source.getTag()!=null){String key=skill.getModeId(source,mode);if(source.getTag().contains(key,Tag.TAG_ANY_NUMERIC))c.getOrCreateTag().put(key,source.getTag().get(key).copy());}}return c;}
    public static boolean offer(ServerPlayer p,ServerPlayer target,ResourceLocation id){
        if(!eligible(p,target))return false;var registered=SkillAPI.getSkillRegistry().get(id);if(registered==null)return false;var source=SkillAPI.getSkillsFrom(p).getSkill(registered);if(source.isEmpty() || !lendable(source.get()) || SkillAPI.getSkillsFrom(target).getSkill(source.get().getSkill()).isPresent())return false;
        var d=LoanLedger.get(p.server);var previous=OFFERS.get(target.getUUID());if(previous!=null && previous.expires>d.clock)return false;
        var offer=new Offer(UUID.randomUUID(),p.getUUID(),target.getUUID(),id,d.clock+600);OFFERS.put(target.getUUID(),offer);see(p);see(target);
        var tag=new CompoundTag();tag.putBoolean("offer",true);tag.putUUID("nonce",offer.nonce);tag.putString("sender",p.getDisplayName().getString());tag.putString("skill",id.toString());tag.putString("name",source.get().getSkill().getName().getString());tag.putDouble("mastery",source.get().getMastery());tag.putInt("maxMastery",source.get().getMaxMastery());LoanNetwork.send(target,tag);return true;
    }
    public static boolean reply(ServerPlayer target,UUID nonce,boolean accept){var offer=OFFERS.get(target.getUUID());if(offer==null || !offer.nonce.equals(nonce))return false;OFFERS.remove(target.getUUID());if(!accept)return false;var d=LoanLedger.get(target.server);var sender=resolve(target.server,offer.sender);return offer.expires>=d.clock && sender instanceof ServerPlayer p && issue(p,target,offer.skill,true)!=null;}
    public static UUID pendingOffer(ServerPlayer p){var o=OFFERS.get(p.getUUID());return o==null?null:o.nonce;}
    public static LoanLedger.Loan lend(ServerPlayer p,LivingEntity target,ResourceLocation skillId){return target instanceof ServerPlayer?null:issue(p,target,skillId,false);}
    private static LoanLedger.Loan issue(ServerPlayer p,LivingEntity target,ResourceLocation skillId,boolean consent){if(target instanceof ServerPlayer && !consent)return null;if(!eligible(p,target))return null;var registered=SkillAPI.getSkillRegistry().get(skillId);if(registered==null)return null;var source=SkillAPI.getSkillsFrom(p).getSkill(registered);if(source.isEmpty() || !lendable(source.get()) || SkillAPI.getSkillsFrom(target).getSkill(source.get().getSkill()).isPresent())return null;
        var d=LoanLedger.get(p.server);if(d.loans.values().stream().filter(l->l.lender.equals(p.getUUID()) && l.open()).count()>=512)return null;
        var l=new LoanLedger.Loan();l.id=UUID.randomUUID();l.lender=p.getUUID();l.borrower=target.getUUID();l.lenderName=p.getDisplayName().getString();l.borrowerName=target.getDisplayName().getString();l.skill=skillId.toString();l.created=d.clock;l.lastUsed=d.clock;
        var c=copy(source.get());c.setRemoveTime(-2);c.getOrCreateTag().putUUID(TOKEN,l.id);var before=new HashSet<ManasSkill>();for(var i:SkillAPI.getSkillsFrom(target).getLearnedSkills())before.add(i.getSkill());
        if(!SkillAPI.getSkillsFrom(target).learnSkill(c))return null;
        for(var i:List.copyOf(SkillAPI.getSkillsFrom(target).getLearnedSkills()))if(!before.contains(i.getSkill())){i.getOrCreateTag().putUUID(TOKEN,l.id);i.setRemoveTime(-2);i.markDirty();SkillAPI.getSkillsFrom(target).checkAndMarkDirty(i);}
        d.add(l);see(p);see(target);p.displayClientMessage(Component.literal("Loan issued to "+l.borrowerName+"."),true);if(target instanceof ServerPlayer receiver)receiver.sendSystemMessage(Component.literal(l.lenderName+" gifted you "+source.get().getSkill().getName().getString()+"."));return l;
    }
    public static boolean claimable(ManasSkillInstance i){return lendable(i) && !(i.getSkill() instanceof ChainedSkill) && !(i.getSkill() instanceof io.github.manasmods.tensura.ability.skill.Skill skill && skill.getType()==io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE);}
    private static LivingEntity acting(LivingEntity e){var fusion=Fusion.session(e);return fusion==null?e:fusion.pilot;}
    private static boolean contractUser(LoanLedger.Loan l,LivingEntity e){if(l.borrower.equals(e.getUUID()))return true;var f=Fusion.session(e);return f!=null && f.pilot==e && (l.borrower.equals(f.caster.getUUID()) || l.borrower.equals(f.target.getUUID()));}
    private static boolean tagged(ManasSkillInstance i,UUID token){return i.getTag()!=null && i.getTag().hasUUID(TOKEN) && i.getTag().getUUID(TOKEN).equals(token);}
    public static void pulse(LivingEntity owner,ManasSkillInstance i,int ticks){if(owner==null || Fusion.hidden(owner) || owner.level().isClientSide() || i.getMastery()<0 || i.getTag()==null || !i.getTag().hasUUID(TOKEN) || owner.getServer()==null)return;var d=LoanLedger.get(owner.getServer());UUID id=i.getTag().getUUID(TOKEN);var l=d.loans.get(id);if(l!=null && l.open() && contractUser(l,owner)){PULSES.put(id,Math.max(PULSES.getOrDefault(id,0L),d.clock+ticks));see(owner);}}
    private static boolean purePassive(ManasSkill skill){return PURE_PASSIVE.computeIfAbsent(skill.getClass(),type->Arrays.stream(type.getMethods()).noneMatch(m->Set.of("onPressed","onHeld","onRelease").contains(m.getName()) && !Set.of("ManasSkill","TensuraSkill","Skill").contains(m.getDeclaringClass().getSimpleName())));}
    private static boolean slotPassive(ManasSkill skill){if(skill instanceof ExponentiationSkill || purePassive(skill))return true;return SLOT_PASSIVE.computeIfAbsent(skill.getClass(),type->Arrays.stream(type.getMethods()).anyMatch(m->Set.of("onBeingDamaged","onTakenDamage","onDamageEntity").contains(m.getName()) && !Set.of("ManasSkill","TensuraSkill","Skill").contains(m.getDeclaringClass().getSimpleName())));}
    public static boolean active(LoanLedger d,LoanLedger.Loan l,LivingEntity e){var fusion=Fusion.session(e);if(l.skill.equals(HeavenlyRestriction.FUSION.get().getRegistryName().toString()) && fusion!=null && l.borrower.equals(fusion.caster.getUUID()) && SkillAPI.getSkillsFrom(fusion.caster).getLearnedSkills().stream().anyMatch(i->tagged(i,l.id)))return true;e=acting(e);if(e==null || !e.isAlive() || e.isSpectator())return false;for(var i:SkillAPI.getSkillsFrom(e).getLearnedSkills())if(tagged(i,l.id) && i.getMastery()>=0){if(i.isToggled() || i.getSkill() instanceof ResistSkill || i.getSkill() instanceof ChainedSkill || (!i.canBeToggled(e) && i.canTick(e)) || PULSES.getOrDefault(l.id,0L)>d.clock)return true;if(i.getSkill()==HeavenlyRestriction.AIR_SOVEREIGNTY.get() && e instanceof net.minecraft.world.entity.player.Player p && AirSovereigntyArt.authorizesFlight(p) && p.getAbilities().flying)return true;if(i.getSkill()==HeavenlyRestriction.INSTANT_TRANSMISSION.get() && e instanceof ServerPlayer p && InstantTransmissionArt.charging(p))return true;if(i.getSkill() instanceof TensuraSkill skill){if(purePassive(skill) && !i.canBeToggled(e) && !skill.canBeSlotted(i,e,0))return true;if(slotPassive(skill) && (!purePassive(skill) || !i.canBeToggled(e)) && skill.isInSlot(e,i))return true;}}return false;}
    public static void sample(LoanLedger d,LoanLedger.Loan l,LivingEntity e){boolean on=active(d,l,e);if(on){if(!l.wasActive){l.lastGap=Math.max(0,d.clock-l.lastUsed);l.longestGap=Math.max(l.longestGap,l.lastGap);}l.used++;l.lastUsed=d.clock;}l.wasActive=on;}
    public static void tick(ServerTickEvent.Post event){var s=event.getServer();var d=LoanLedger.get(s);d.clock++;for(var l:List.copyOf(d.active())){var e=resolve(s,l.borrower);if(l.open())sample(d,l,e);}if(d.clock%20==0){for(var id:List.copyOf(d.pending.keySet()))if(resolve(s,id) instanceof ServerPlayer p)pay(d,p);SESSIONS.entrySet().removeIf(e->e.getValue().expires<d.clock);OFFERS.entrySet().removeIf(e->e.getValue().expires<d.clock);PULSES.entrySet().removeIf(e->e.getValue()<=d.clock);if(d.clock%200==0)SEEN.entrySet().removeIf(e->{var entity=e.getValue().get();return entity==null || entity.isRemoved();});}d.setDirty();}
    public static boolean transferring(){return TRANSFERRING.get()>0;}
    private static void transfer(Runnable action){int before=TRANSFERRING.get();TRANSFERRING.set(before+1);try{action.run();}finally{TRANSFERRING.set(before);}}
    /** Transfer a diminishing share of the actual award, respecting native storage limits. */
    public static void gained(LivingEntity e,AttributeInstance a,double beforeBase,double beforeEffective){
        if(e==null || e.level().isClientSide() || e.getServer()==null || transferring() || ChainedContext.initializing(e) || ExponentiationContext.restoring(e))return;
        double delta=a.getBaseValue()-beforeBase,effective=a.getValue()-beforeEffective;
        if(delta<=0 || effective<=0 || !Double.isFinite(delta+effective))return;
        var d=LoanLedger.get(e.getServer());
        var applicable=new LinkedHashSet<>(d.forBorrower(e.getUUID()));
        var fusion=Fusion.session(e);
        if(fusion!=null && fusion.pilot==e){
            applicable.addAll(d.forBorrower(fusion.caster.getUUID()));
            applicable.addAll(d.forBorrower(fusion.target.getUUID()));
        }
        var active=applicable.stream().filter(l->l.open() && active(d,l,e)).toList();
        if(active.isEmpty())return;
        see(e);
        transfer(()->{
            double full=a.getValue(),factor=slope(a);
            if(factor<=0 || !Double.isFinite(factor))return;
            // Each gift takes 25% of the remaining gain: 25%, then 18.75%, then 14.0625%.
            double retained=Math.pow(.75,active.size());
            double wanted=beforeEffective+effective*retained;
            a.setBaseValue((wanted-offset(a))/factor);
            double taken=Math.max(0,full-a.getValue());
            if(taken<=0)return;
            // Equal shares keep entitlement independent of gift order.
            double share=taken/active.size();
            for(var l:active){
                l.earned+=share;
                d.pending.merge(l.lender,share,Double::sum);
            }
            d.setDirty();
            for(var l:active)if(resolve(e.getServer(),l.lender) instanceof ServerPlayer p)pay(d,p);
        });
    }
    private static double slope(AttributeInstance a){double base=1,total=1;for(var m:a.getModifiers())if(m.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)base+=m.amount();else if(m.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)total*=1+m.amount();return base*total;}
    private static double offset(AttributeInstance a){double flat=0;for(var m:a.getModifiers())if(m.operation()==AttributeModifier.Operation.ADD_VALUE)flat+=m.amount();return flat*slope(a);}
    private static double credit(LivingEntity e,net.minecraft.core.Holder<Attribute> type,double amount){var a=e.getAttribute(type);if(a==null || amount<=0 || type.equals(TensuraAttributes.MAX_MAGICULE) && ChainedState.owned(e))return 0;double factor=slope(a);if(factor<=0 || !Double.isFinite(factor))return 0;double before=a.getValue(),limit=a.getAttribute().value() instanceof RangedAttribute r?r.getMaxValue():Double.MAX_VALUE;double desired=Math.min(limit,before+amount);if(desired<=before)return 0;a.setBaseValue((desired-offset(a))/factor);return Math.min(amount,Math.max(0,a.getValue()-before));}

    public static void pay(LoanLedger d,ServerPlayer p){double owed=d.pending.getOrDefault(p.getUUID(),0d);if(owed<=0 || !p.isAlive() || p.isSpectator())return;transfer(()->{var align=TensuraStorages.getExistenceFrom(p).getAlignment();double ap=align.getAuraGainMultiplier(),mp=align.getMagiculeGainMultiplier();if(ChainedState.owned(p)){ap=1;mp=0;}double sum=ap+mp;if(sum<=0)return;double paid=credit(p,TensuraAttributes.MAX_AURA,owed*ap/sum)+credit(p,TensuraAttributes.MAX_MAGICULE,owed*mp/sum);double rest=Math.max(0,owed-paid);if(rest<.00001)d.pending.remove(p.getUUID());else d.pending.put(p.getUUID(),rest);d.setDirty();});}
    public static String claim(ServerPlayer p,UUID id){return claim(p,id,true);}
    public static String cancel(ServerPlayer p,UUID id){return claim(p,id,false);}
    private static String claim(ServerPlayer p,UUID id,boolean allowReward){
        if(!owns(p))return "Loan Shark is unavailable.";
        var d=LoanLedger.get(p.server);var l=d.loans.get(id);
        if(l==null || !l.lender.equals(p.getUUID()) || !l.open())return "That loan is not yours or is already closed.";
        var borrower=resolve(p.server,l.borrower);
        if(!l.open())return "The borrower has died. Loan closed.";
        if(borrower==null){close(d,l,"Cancelled");return "Loan closed. Recall queued if the borrower returns. No extra skill taken.";}
        boolean rewardEligible=allowReward && !active(d,l,borrower) && d.clock-l.lastUsed>=claimTicks(p);
        var holders=new ArrayList<LivingEntity>();holders.add(borrower);
        var body=acting(borrower);if(body!=borrower)holders.add(body);
        for(var holder:holders)for(var i:List.copyOf(SkillAPI.getSkillsFrom(holder).getLearnedSkills()))if(tagged(i,id))SkillAPI.getSkillsFrom(holder).forgetSkill(i);
        for(var holder:holders)if(SkillAPI.getSkillsFrom(holder).getLearnedSkills().stream().anyMatch(i->tagged(i,id)))return "The gifted skill could not be recalled.";
        close(d,l,"Recalled");
        if(!rewardEligible)return "Loan recalled. No skill taken.";
        // Check the live native rule when claiming, rather than when the gift was issued.
        boolean removeOriginal=TensuraGameRules.canStealSkill(borrower.level());
        var candidates=new ArrayList<>(SkillAPI.getSkillsFrom(borrower).getLearnedSkills().stream()
            .filter(LoanShark::claimable)
            .filter(i->SkillAPI.getSkillsFrom(p).getSkill(i.getSkill()).isEmpty())
            .filter(i->ChainedState.canLearn(p,i)).toList());
        Collections.shuffle(candidates,new Random(p.getRandom().nextLong()));
        for(var i:candidates)if(SkillAPI.getSkillsFrom(p).learnSkill(copy(i))){
            if(removeOriginal){
                SkillAPI.getSkillsFrom(borrower).forgetSkill(i);
                if(SkillAPI.getSkillsFrom(borrower).getSkill(i.getSkill()).isPresent()){
                    SkillAPI.getSkillsFrom(p).getSkill(i.getSkill()).ifPresent(gained->SkillAPI.getSkillsFrom(p).forgetSkill(gained));
                    continue;
                }
            }
            l.status=removeOriginal?"Claimed":"Copied";
            l.stolen=i.getSkillId().toString();
            return "Loan recalled. "+(removeOriginal?"Claimed ":"Copied ")+i.getSkill().getName().getString()+".";
        }
        return "Loan recalled. No eligible skill could be claimed.";
    }
    public static void open(ServerPlayer p,int mode){if(!owns(p))return;LivingEntity target=mode==0?ObjectSelectionHelper.getTargetingEntity(p,16,false):null;if(mode==0 && !eligible(p,target)){p.displayClientMessage(Component.literal("Aim at your subordinate or a player within 16 blocks."),true);return;}var d=LoanLedger.get(p.server);see(p);if(target!=null)see(target);var session=new Session(UUID.randomUUID(),target==null?null:target.getUUID(),d.clock+2400,mode);SESSIONS.put(p.getUUID(),session);send(p,session,"",0,"",true);}
    public static void request(ServerPlayer p,UUID token,String action,String value,String query,int page){var session=SESSIONS.get(p.getUUID());if(session==null || !session.token.equals(token) || !owns(p) || session.expires<LoanLedger.get(p.server).clock)return;session=new Session(session.token,session.target,LoanLedger.get(p.server).clock+2400,session.mode);SESSIONS.put(p.getUUID(),session);String message="";
        if(action.equals("lend") && session.mode==0){ResourceLocation id=ResourceLocation.tryParse(value);var target=session.target==null?null:resolve(p.server,session.target);message=id==null?"Invalid skill.":target instanceof ServerPlayer receiver?offer(p,receiver,id)?"Gift offer sent. Awaiting acceptance.":"Unable to send offer.":lend(p,target,id)!=null?"Loan issued. Mastery retained.":"Unable to lend: target unavailable, skill owned, restricted or ineligible.";}
        else if((action.equals("claim") || action.equals("close")) && session.mode==1){try{message=action.equals("close")?cancel(p,UUID.fromString(value)):claim(p,UUID.fromString(value));}catch(IllegalArgumentException ignored){message="Invalid loan.";}}
        else if(!action.equals("refresh"))return;
        send(p,session,query,page,message,false);
    }
    public static List<ManasSkillInstance> loanChoices(ServerPlayer p){return SkillAPI.getSkillsFrom(p).getLearnedSkills().stream().filter(LoanShark::lendable).toList();}
    public static List<LoanLedger.Loan> reportLoans(ServerPlayer p){var d=LoanLedger.get(p.server);var owned=d.active().stream().filter(l->l.lender.equals(p.getUUID())).toList();for(var l:owned)resolve(p.server,l.borrower);return owned.stream().filter(LoanLedger.Loan::open).toList();}
    private static String name(String id){var skill=SkillAPI.getSkillRegistry().get(ResourceLocation.tryParse(id));return skill==null?id:skill.getName().getString();}
    private static void send(ServerPlayer p,Session session,String query,int page,String message,boolean open){var d=LoanLedger.get(p.server);var state=new CompoundTag();state.putUUID("session",session.token);state.putBoolean("open",open);state.putInt("mode",session.mode);state.putLong("claimTicks",claimTicks(p));state.putString("message",message);state.putString("query",query);var items=new ArrayList<CompoundTag>();String q=query.toLowerCase(Locale.ROOT);
        var target=session.target==null?null:resolve(p.server,session.target);state.putString("target",target==null?"Unavailable":target.getDisplayName().getString());state.putDouble("pending",d.pending.getOrDefault(p.getUUID(),0d));
        if(session.mode==0){for(var i:loanChoices(p)){String label=i.getSkill().getName().getString();if(!label.toLowerCase(Locale.ROOT).contains(q))continue;var row=new CompoundTag();row.putString("id",i.getSkillId().toString());row.putString("skill",i.getSkillId().toString());row.putString("name",label);row.putDouble("mastery",i.getMastery());row.putInt("maxMastery",i.getMaxMastery());row.putBoolean("enabled",eligible(p,target) && lendable(i) && SkillAPI.getSkillsFrom(target).getSkill(i.getSkill()).isEmpty());row.putString("status",!lendable(i)?"Not lendable":target!=null && SkillAPI.getSkillsFrom(target).getSkill(i.getSkill()).isPresent()?"Already owned":"Mastery retained");items.add(row);}}
        else for(var l:reportLoans(p))if((name(l.skill)+" "+l.borrowerName+" "+l.status).toLowerCase(Locale.ROOT).contains(q)){var e=resolve(p.server,l.borrower);boolean active=active(d,l,e);var row=new CompoundTag();row.putString("id",l.id.toString());row.putString("skill",l.skill);row.putString("name",name(l.skill));row.putString("borrower",l.borrowerName);row.putString("status",l.open()?e==null?"Unavailable":active?"In use":"Idle":l.status);row.putDouble("ep",l.earned);row.putLong("used",l.used);row.putLong("idle",active?0:Math.max(0,d.clock-l.lastUsed));row.putLong("gap",l.lastGap);row.putLong("longestGap",l.longestGap);row.putBoolean("enabled",l.open());row.putBoolean("unavailable",e==null);row.putBoolean("steal",l.open() && e!=null && !active && d.clock-l.lastUsed>=claimTicks(p));row.putBoolean("copy",e!=null && !TensuraGameRules.canStealSkill(e.level()));row.putString("stolen",l.stolen.isEmpty()?"":name(l.stolen));items.add(row);}
        items.sort(Comparator.comparing(t->t.getString("name"),String.CASE_INSENSITIVE_ORDER));int pages=Math.max(1,(items.size()+5)/6);page=Math.clamp(page,0,pages-1);state.putInt("page",page);state.putInt("pages",pages);state.putInt("count",items.size());var rows=new ListTag();for(int n=page*6;n<Math.min(items.size(),page*6+6);n++)rows.add(items.get(n));state.put("rows",rows);LoanNetwork.send(p,state);
    }
    private LoanShark(){}
}
