package dev.wildhunt.myths;

import dev.wildhunt.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.function.Consumer;

/** Only bounded intent crosses C2S. All progression and requirements are rebuilt server-side. */
public final class MythsNetwork {
    public static Consumer<CompoundTag> clientStateReceiver=tag->{};
    public record Request(String action,String argument) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(WildHunt.id("myths_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.action,24);b.writeUtf(p.argument,128);},b->new Request(b.readUtf(24),b.readUtf(128)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record State(CompoundTag data) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(WildHunt.id("myths_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->b.writeNbt(p.data),b->new State(b.readNbt()));
        @Override public Type<State> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("1");
        registrar.playToServer(Request.TYPE,Request.CODEC,(payload,context)->context.enqueueWork(()->{
            if(!(context.player() instanceof ServerPlayer p))return;
            if(java.util.Set.of("race_press","race_hold","race_release").contains(payload.action())) {SecondaryAbility.input(p,payload.action());return;}
            if(payload.action().equals("sync")){var sync=DuoSystem.data(p);long time=p.level().getGameTime();if(sync.contains("sync_time") && time-sync.getLong("sync_time")<10)return;sync.putLong("sync_time",time);var snapshot=snapshot(p,"");snapshot.putBoolean("silent",true);PacketDistributor.sendToPlayer(p,new State(snapshot));return;}
            long now=p.serverLevel().getGameTime(); var d=DuoSystem.data(p);
            if(d.contains("request_time") && now-d.getLong("request_time")<3)return;
            d.putLong("request_time",now);
            String result=switch(payload.action()) {
                case "open" -> "";
                case "select" -> RaceFamily.byKey(payload.argument()).map(f->DuoSystem.select(p,f)).orElse("Unknown secondary lineage.");
                case "evolve" -> {var id=ResourceLocation.tryParse(payload.argument()); yield id==null?"Invalid race ID.":DuoSystem.evolve(p,id);}
                case "awaken" -> DuoSystem.convergence(p);
                case "godly" -> {var parts=payload.argument().split("\\|",2);yield MythsProgression.ascend(p,parts[0],parts.length==2?parts[1]:"aion");}
                case "synthesize" -> MythicEvolution.synthesize(p,payload.argument());
                default -> "Unknown Myths action.";
            };
            if(!result.isEmpty())p.sendSystemMessage(Component.literal(result));
            send(p,result);
        }));
        registrar.playToClient(State.TYPE,State.CODEC,(payload,context)->context.enqueueWork(()->clientStateReceiver.accept(payload.data())));
    }
    public static void send(ServerPlayer p,String message) { PacketDistributor.sendToPlayer(p,new State(snapshot(p,message))); }
    public static CompoundTag snapshot(ServerPlayer p,String message) {
        var tag=new CompoundTag();tag.putString("message",message);
        tag.putString("primary",DuoSystem.primary(p).map(r->r.getDisplayName().getString()).orElse("No race"));
        tag.putString("secondary",DuoSystem.secondary(p).map(r->r.getDisplayName().getString()).orElse("Not selected"));
        tag.putBoolean("converged",DuoSystem.converged(p));tag.putBoolean("awakened",DuoSystem.awakened(p));
        tag.putDouble("ep",EnergyHelper.getBaseMaxEP(p));tag.putDouble("secondary_ep",MythsConfig.SECONDARY_EP.get());tag.putDouble("convergence_ep",DuoSystem.convergenceEP(p));tag.putDouble("godly_ep",MythsConfig.GODLY_EP.get());
        tag.putBoolean("primary_final",DuoSystem.primary(p).map(r->DuoSystem.terminal(r,p)).orElse(false));
        tag.putBoolean("secondary_final",DuoSystem.secondary(p).map(r->DuoSystem.terminal(r,p)).orElse(false));
        var alignment=io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p).getAlignment();
        var g=DuoSystem.gains(p,alignment.getAuraGainMultiplier(),alignment.getMagiculeGainMultiplier());tag.putDouble("aura",g.aura());tag.putDouble("magicules",g.magicules());
        var choices=new ListTag();
        if(DuoSystem.secondary(p).isEmpty() && !DuoSystem.converged(p)) {
            for(var family:RaceFamily.values())if(!DuoSystem.primaryFamily(p).equals(java.util.Optional.of(family))) {
                var row=new CompoundTag();row.putString("id",family.key());row.putString("race",family.baseRace().getRegistryName().toString());row.putString("name",family.title);row.putString("detail","Choose permanent secondary ancestry. +10% EP gain, retaining your primary energy split. Inherited by Convergent Awakening.");choices.add(row);
            }
            tag.putString("choice_action","select");
        } else if(!DuoSystem.converged(p)) {
            var secondary=DuoSystem.secondary(p).orElseThrow();
            for(var next:DuoSystem.options(p,secondary)) {
                var row=new CompoundTag();row.putString("id",next.getRegistryName().toString());row.putString("race",next.getRegistryName().toString());row.putString("name",next.getName().getString());
                double required=DuoSystem.secondaryEvolutionEP(p);
                float progress=(float)Math.min(DuoSystem.nativeProgress(p,secondary,next),Math.min(100,EnergyHelper.getBaseMaxEP(p)/required*100));
                row.putDouble("progress",progress);row.putBoolean("requirements_met",DuoSystem.secondaryRequirementsMet(p,secondary,next));
                row.putString("detail",String.format(java.util.Locale.ROOT,"Secondary evolution base EP: %,.0f / %,.0f\n",EnergyHelper.getBaseMaxEP(p),required)+DuoSystem.nativeRequirements(p,secondary,next));
                choices.add(row);
            }
            tag.putString("choice_action","evolve");
        }
        tag.put("choices",choices);
        var first=RaceFamily.byKey(DuoSystem.data(p).getString("primary_family"));var second=DuoSystem.secondaryFamily(p);
        if(first.isPresent() && second.isPresent() && first.get()!=second.get()) {
            var fusion=FusionCatalog.profile(first.get(),second.get());tag.putString("fusion_id",WildHunt.id("convergence/"+fusion.key()).toString());tag.putString("fusion_name",fusion.name());tag.putString("fusion_description",fusion.description());tag.putDouble("fusion_aura",fusion.aura());tag.putDouble("fusion_magicules",fusion.magicules());
        }
        var ascensions=new ListTag();
        for(var kind:MythicEvolution.ultimates()){var entry=WildHunt.MYTHIC.get(kind);SkillAPI.getSkillsFrom(p).getSkill(entry.get()).ifPresent(i->{
            var row=new CompoundTag();row.putString("id",entry.getId().toString());row.putString("name",entry.get().getName().getString());row.putBoolean("mastered",i.isMastered(p));ascensions.add(row);
        });}
        tag.put("ascensions",ascensions);tag.putBoolean("has_godly",SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).isPresent());
        tag.putBoolean("has_thoth",SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).isPresent());
        tag.putBoolean("has_lugh",SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.LUGH).get()).isPresent());
        tag.putBoolean("thread_mastered",SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.THREADWEAVER).get()).map(i->i.isMastered(p)).orElse(false));
        tag.putBoolean("rune_mastered",SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.RUNE_SMITH).get()).map(i->i.isMastered(p)).orElse(false));
        tag.putDouble("synthesis_ep",MythicEvolution.SYNTHESIS_EP);
        var recipes=new ListTag();for(var result:MythicEvolution.ultimates()){
            var row=new CompoundTag();row.putString("id",result.id());row.putString("name",result.title);row.putBoolean("ready",MythicEvolution.ready(p,result));
            row.putString("ingredients",MythicEvolution.ingredients(result).stream().map(k->k.getName().getString()+": "+SkillAPI.getSkillsFrom(p).getSkill(k).map(i->i.isMastered(p)?"Mastered":"Not mastered").orElse("Not learned")).collect(java.util.stream.Collectors.joining("\n")));recipes.add(row);
        }tag.put("recipes",recipes);
        return tag;
    }
    private MythsNetwork() {}
}
