package dev.wildhunt;

import dev.architectury.event.EventResult;
import dev.wildhunt.myths.MythsTiers;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.*;
import java.util.*;

/** Lugh reconstructs bounded combat forms, never invokes another mod's skill implementation. */
public final class LughArts {
    private LughArts() {}
    private static final Set<ResourceLocation> SAFE_TYPES=Set.of(TensuraDamageTypes.AURA_SLASH.location(),TensuraDamageTypes.MAGIC_GENERIC.location(),
        TensuraDamageTypes.EARTH_ELEMENTAL.location(),TensuraDamageTypes.FIRE_ELEMENTAL.location(),TensuraDamageTypes.WATER_ELEMENTAL.location(),
        TensuraDamageTypes.ICE_ELEMENTAL.location(),TensuraDamageTypes.WIND_ELEMENTAL.location(),TensuraDamageTypes.LIGHTNING_ELEMENTAL.location(),
        TensuraDamageTypes.LIGHT_ELEMENTAL.location(),TensuraDamageTypes.DARKNESS_ELEMENTAL.location(),TensuraDamageTypes.SPACE_ELEMENTAL.location(),TensuraDamageTypes.GRAVITY_ELEMENTAL.location());
    private static MythicSkill skill(){return WildHunt.MYTHIC.get(MythicKind.LUGH).get();}
    private static float bounded(float value){return Float.isFinite(value)?Math.clamp(value,3500,6000):3800;}
    private static CompoundTag pattern(String name,String source,ResourceLocation type,int form,float damage){
        var t=new CompoundTag();t.putString("name",name);t.putString("source",source);t.putString("type",type.toString());t.putInt("form",Math.clamp(form,0,2));t.putFloat("damage",bounded(damage));return t;
    }
    public static final int LIBRARY_LIMIT=256;
    private static ListTag origins(){
        var list=new ListTag();list.add(pattern("Sovereign Edge","wildhunt:origin_edge",TensuraDamageTypes.AURA_SLASH.location(),0,3800));
        list.add(pattern("Heavenly Thread","wildhunt:origin_thread",TensuraDamageTypes.SPACE_ELEMENTAL.location(),1,3800));
        list.add(pattern("Founder's Rune","wildhunt:origin_rune",TensuraDamageTypes.EARTH_ELEMENTAL.location(),2,4200));return list;
    }
    /** Append-only records have stable server-generated IDs; old three-slot saves migrate once. */
    public static ListTag library(ManasSkillInstance i){
        var tag=i.getOrCreateTag();
        if(!tag.contains("art_library",Tag.TAG_LIST)){
            var old=tag.getList("archive",Tag.TAG_COMPOUND);var list=old.size()==3?old.copy():origins();var bindings=new ListTag();
            for(var value:list){var record=(CompoundTag)value;record.putString("entry",UUID.randomUUID().toString());record.putInt("mode",-1);bindings.add(StringTag.valueOf(record.getString("entry")));}
            // Keep every old appraised record and restore any origins that it had replaced.
            for(var value:origins()){var origin=(CompoundTag)value;if(list.stream().noneMatch(v->((CompoundTag)v).getString("source").equals(origin.getString("source")))){origin.putString("entry",UUID.randomUUID().toString());origin.putInt("mode",-1);list.add(origin);}}
            tag.put("art_library",list);tag.put("art_bindings",bindings);tag.remove("archive");i.markDirty();
        }
        var list=tag.getList("art_library",Tag.TAG_COMPOUND);
        for(var value:list){var record=(CompoundTag)value;float damage=record.getFloat("damage");
            damage=switch(record.getString("source")){case "wildhunt:origin_edge","wildhunt:origin_thread"->damage==2200?3800:damage;case "wildhunt:origin_rune"->damage==2400?4200:damage;default->damage;};record.putFloat("damage",bounded(damage));
        }
        return list;
    }
    /** Three live loadout references, separate from the permanent appraisal library. */
    public static ListTag archive(ManasSkillInstance i){
        var list=library(i);var bindings=i.getOrCreateTag().getList("art_bindings",Tag.TAG_STRING);var result=new ListTag();
        for(int n=0;n<3;n++){
            final String id=n<bindings.size()?bindings.getString(n):"";
            var record=list.stream().map(v->(CompoundTag)v).filter(v->v.getString("entry").equals(id)).findFirst().orElse(list.getCompound(Math.min(n,list.size()-1)));result.add(record);
        }return result;
    }
    private static String text(String text){return text.substring(0,Math.min(256,text.length()));}
    private static int storedIndex(ListTag list,CompoundTag record){
        for(int n=0;n<list.size();n++){var stored=list.getCompound(n);if(stored.getString("source").equals(record.getString("source")) && (stored.getInt("mode")==record.getInt("mode") || stored.getInt("mode")==-1))return n;}return -1;
    }
    static boolean remember(ManasSkillInstance i,CompoundTag observation){
        var list=library(i);int index=storedIndex(list,observation);if(index<0 && list.size()>=LIBRARY_LIMIT)return false;
        var record=pattern(text(observation.getString("name")),observation.getString("source"),safeType(observation).location(),observation.getInt("form"),observation.getFloat("damage"));
        record.putString("entry",index<0?UUID.randomUUID().toString():list.getCompound(index).getString("entry"));record.putInt("mode",observation.getInt("mode"));
        record.putString("mode_name",text(observation.getString("mode_name")));record.putString("source_type",observation.getString("source_type"));
        record.putFloat("observed_damage",Math.max(0,observation.getFloat("observed_damage")));record.putBoolean("observed",true);
        if(index>=0 && record.equals(list.getCompound(index)))return true;
        if(index<0)list.add(record);else list.set(index,record);i.markDirty();return true;
    }
    /** The client supplies only an ID and slot. Damage and other parameters never cross C2S. */
    public static boolean configure(net.minecraft.server.level.ServerPlayer p,String action,String argument){
        var optional=SkillAPI.getSkillsFrom(p).getSkill(skill());if(optional.isEmpty() || !p.isAlive())return false;
        var i=optional.get();if(!skill().canInteractSkill(i,p) || !skill().canActivateSkill(i,p,0))return false;
        var parts=argument.split("\\|",-1);int slot;
        try{slot=Integer.parseInt(parts[0]);}catch(NumberFormatException ex){return false;}
        if(slot<0 || slot>2)return false;
        if(action.equals("lugh_bind")){
            var list=library(i);if(parts.length!=2 || list.stream().noneMatch(v->((CompoundTag)v).getString("entry").equals(parts[1])))return false;
            var bindings=i.getOrCreateTag().getList("art_bindings",Tag.TAG_STRING);if(bindings.size()!=3)return false;bindings.set(slot,StringTag.valueOf(parts[1]));
        }else if(!action.equals("lugh_select") || parts.length!=1)return false;
        i.getOrCreateTag().putInt("selected_art",slot);i.markDirty();return true;
    }
    public static CompoundTag snapshot(net.minecraft.server.level.ServerPlayer p){
        var state=new CompoundTag();state.putBoolean("lugh_library",true);
        SkillAPI.getSkillsFrom(p).getSkill(skill()).ifPresent(i->{
            state.putBoolean("owned",true);state.put("entries",library(i).copy());state.put("bindings",i.getOrCreateTag().getList("art_bindings",Tag.TAG_STRING).copy());state.putInt("selected",Math.floorMod(i.getOrCreateTag().getInt("selected_art"),3));
            state.putFloat("mastery_scale",1+.5f*(float)Math.clamp(i.getMastery()/i.getMaxMastery(),0,1));
        });return state;
    }
    public static String techniqueName(CompoundTag record){var mode=record.getString("mode_name");return mode.isBlank()?record.getString("name"):mode;}
    public static String selectedName(ManasSkillInstance i){return techniqueName(archive(i).getCompound(Math.floorMod(i.getOrCreateTag().getInt("selected_art"),3)));}
    public static void cycle(ManasSkillInstance i,LivingEntity e){
        i.getOrCreateTag().putInt("selected_art",Math.floorMod(i.getOrCreateTag().getInt("selected_art")+1,3));i.markDirty();message(e,"Reconstructed Art: "+selectedName(i));
    }
    /** Record the most recent actual damaging technique, before this addon's Godly bonus. */
    public static EventResult observeDamage(Skills storage,LivingEntity target,DamageSource source,Changeable<Float> amount){
        if(target.level().isClientSide() || !(source.getEntity() instanceof LivingEntity caster) || !Float.isFinite(amount.get()) || amount.get()<=0)return EventResult.pass();
        var instance=((TensuraDamageSource)source).tensura$getAbilityInstance();
        if(instance==null || instance.getSkill()==skill() || instance.getSkill().getRegistryName()==null || !HunterData.canHarm(caster,target))return EventResult.pass();
        var originalType=source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(TensuraDamageTypes.MAGIC_GENERIC.location());
        var id=source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(TensuraDamageTypes.MAGIC_GENERIC.location());
        boolean physical=TensuraDamageHelper.isPhysicalAttack(source);if(!SAFE_TYPES.contains(id))id=physical?TensuraDamageTypes.AURA_SLASH.location():TensuraDamageTypes.MAGIC_GENERIC.location();
        var old=HunterData.of(caster).getCompound("last_technique");String sourceId=instance.getSkillId().toString();long now=caster.level().getGameTime();
        boolean spread=old.getLong("time")==now && old.getInt("mode")==((TensuraDamageSource)source).tensura$getAbilityMode() && old.getString("source").equals(sourceId) && old.hasUUID("victim") && !old.getUUID("victim").equals(target.getUUID());
        int form=spread?2:source.getDirectEntity() instanceof Projectile || caster.distanceToSqr(target)>64?1:physical?0:2;
        var record=pattern(instance.getSkill().getName().getString(),sourceId,id,form,amount.get()*.8f);
        int mode=((TensuraDamageSource)source).tensura$getAbilityMode();record.putInt("mode",mode);
        record.putString("source_type",originalType.toString());record.putFloat("observed_damage",amount.get());
        if(instance.getSkill() instanceof io.github.manasmods.tensura.ability.TensuraSkill nativeSkill && mode>=0 && mode<instance.getModes())record.putString("mode_name",text(nativeSkill.getModeName(instance,mode).getString()));
        record.putLong("time",now);record.putUUID("victim",target.getUUID());HunterData.of(caster).put("last_technique",record);
        for(var observer:((ServerLevel)caster.level()).getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(64),e->e.distanceToSqr(caster)<=64*64))collect(observer,caster,record);
        return EventResult.pass();
    }
    /** Live toggle authorization is shared by collection and cooldown recovery. */
    public static boolean active(LivingEntity owner){
        if(!owner.isAlive())return false;
        return SkillAPI.getSkillsFrom(owner).getSkill(skill()).filter(i->i.isToggled() && skill().canInteractSkill(i,owner) && skill().canActivateSkill(i,owner,0)
            && (owner.hasInfiniteMaterials() || i.getOrCreateTag().getLong("upkeep")>owner.level().getGameTime() || io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(owner).getMagicule()>=MythicKind.LUGH.upkeep)).isPresent();
    }
    static void collect(LivingEntity observer,LivingEntity caster,CompoundTag record){
        if(observer.level().isClientSide() || observer.level()!=caster.level() || observer.distanceToSqr(caster)>64*64 || !active(observer))return;
        var i=SkillAPI.getSkillsFrom(observer).getSkill(skill()).orElseThrow();var list=library(i);int previous=storedIndex(list,record);
        var before=previous>=0?list.getCompound(previous).copy():null;
        if(!remember(i,record) || before!=null && before.equals(library(i).getCompound(previous)))return;
        long now=observer.level().getGameTime();var tag=i.getOrCreateTag();
        // Coalesce scan optics, never the captured data; multi-hit attacks cannot flood visual entities.
        if(now<tag.getLong("bank_visual_at"))return;tag.putLong("bank_visual_at",now+24);
        var level=(ServerLevel)observer.level();
        ArtVisualEntity.spawn(level,ArtVisualEntity.SKILL_APPRAISAL,caster.position(),new Vec3(2.2,0,0),32).style(record.getInt("form"));
        ArtVisualEntity.spawn(level,ArtVisualEntity.ARTISAN_THREAD,observer.getEyePosition(),caster.getEyePosition().subtract(observer.getEyePosition()),18);
    }
    /** Double native cooldown recovery = 50% recovery time; Lugh itself never benefits. */
    public static EventResult cooldown(ManasSkillInstance instance,LivingEntity owner,int mode,int previous,Changeable<Integer> remaining){
        if(!owner.level().isClientSide() && instance.getSkill()!=skill() && instance.getSkill() instanceof io.github.manasmods.tensura.ability.skill.Skill && active(owner)){
            int elapsed=Math.max(0,previous-remaining.get());
            if(elapsed>0)remaining.set(Math.max(0,remaining.get()-elapsed));
        }return EventResult.pass();
    }
    private static CompoundTag recent(LivingEntity target){
        var record=HunterData.of(target).getCompound("last_technique");long age=target.level().getGameTime()-record.getLong("time");
        if(!record.contains("source") || age<0 || age>600)return null;
        var id=ResourceLocation.tryParse(record.getString("source"));return id==null || SkillAPI.getSkillRegistry().get(id)==null?null:record;
    }
    static boolean canCast(ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        archive(i);if(mode==0 || mode==1 || mode==3)return true;
        var target=ApexArts.target(l,e,48);var record=target==null?null:recent(target);
        if(record==null){message(e,"Aim at an enemy that used a damaging technique in the last 30 seconds.");return false;}
        if(mode==2 && MythsTiers.isGodly(SkillAPI.getSkillRegistry().get(ResourceLocation.parse(record.getString("source"))))){message(e,"Countermand cannot seal a Godly technique.");return false;}
        return true;
    }
    static void cast(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        var tag=i.getOrCreateTag();long now=l.getGameTime();var list=archive(i);
        if(mode==0){
            if(e instanceof net.minecraft.server.level.ServerPlayer p)dev.wildhunt.myths.MythsNetwork.sendLugh(p,true);
        }else if(mode==1){
            var record=list.getCompound(Math.floorMod(tag.getInt("selected_art"),3));reconstruct(s,l,i,e,record,null,1,false);
        }else if(mode==2){
            var target=ApexArts.target(l,e,48);var record=recent(target);var seal=new CompoundTag();seal.putString("skill",record.getString("source"));seal.putUUID("owner",e.getUUID());seal.putLong("until",now+120);
            clearSeal(target);
            HunterData.of(target).put("lugh_seal",seal);var recipients=tag.getList("sealed_targets",Tag.TAG_INT_ARRAY);while(recipients.size()>=8)recipients.remove(0);recipients.add(NbtUtils.createUUID(target.getUUID()));tag.put("sealed_targets",recipients);
            var glyph=ArtVisualEntity.spawn(l,ArtVisualEntity.COUNTERMAND,target.position(),new Vec3(1.6,0,0),120);glyph.setCaster(target);seal.putUUID("visual",glyph.getUUID());
            message(target,"Countermand sealed "+record.getString("name")+" for six seconds.");
            message(e,"Countermand: "+record.getString("name")+" sealed for six seconds.");
        }else {
            Vec3 center=MythicArts.location(l,e,64);ApexArts.point(tag,"synthesis",center);tag.put("synthesis_plan",list.copy());tag.putInt("synthesis_phase",0);tag.putLong("synthesis_at",now+20);tag.putLong("synthesis_start",now);
            MythicArts.endVisual(l,tag,"synthesis_visual");
            var formation=ArtVisualEntity.spawn(l,ArtVisualEntity.GRAND_SYNTHESIS,center,new Vec3(10,0,0),74).style(list.getCompound(0).getInt("form")+3*list.getCompound(1).getInt("form")+9*list.getCompound(2).getInt("form"));tag.putUUID("synthesis_visual",formation.getUUID());
        }
    }
    public static String formName(int form){return switch(Math.clamp(form,0,2)){case 0->"Blade";case 1->"Lance";default->"Crown";};}
    private static ResourceKey<DamageType> safeType(CompoundTag pattern){
        var id=ResourceLocation.tryParse(pattern.getString("type"));return ResourceKey.create(Registries.DAMAGE_TYPE,id!=null && SAFE_TYPES.contains(id)?id:TensuraDamageTypes.MAGIC_GENERIC.location());
    }
    private static void reconstruct(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,CompoundTag record,Vec3 center,int mode,boolean synthesis){
        int form=Math.clamp(record.getInt("form"),0,2);float damage=bounded(record.getFloat("damage"))*(synthesis?1.6f:1);var type=safeType(record);List<LivingEntity> targets;
        if(center!=null){
            final Vec3 origin=center;
            targets=ApexArts.enemies(l,e,center,10).stream().filter(v->form==2 || form==0?Math.abs(v.getY()-origin.y)<6:Math.abs(v.getX()-origin.x)<2.5 || Math.abs(v.getZ()-origin.z)<2.5).limit(8).toList();
            if(form==1)for(var direction:List.of(new Vec3(10,0,0),new Vec3(-10,0,0),new Vec3(0,0,10),new Vec3(0,0,-10)))ArtVisualEntity.spawn(l,ArtVisualEntity.RECONSTRUCTED_LANCE,center.add(0,1,0),direction,24);
            else ArtVisualEntity.spawn(l,form==0?ArtVisualEntity.RECONSTRUCTED_BLADE:ArtVisualEntity.RECONSTRUCTED_CROWN,center.add(0,.2,0),new Vec3(10,0,0),24).style(1);
        }else if(form==0){
            targets=ApexArts.enemies(l,e,e.position(),16).stream().filter(v->v.getEyePosition().subtract(e.getEyePosition()).normalize().dot(e.getLookAngle())>.35).limit(8).toList();
            ArtVisualEntity.spawn(l,ArtVisualEntity.RECONSTRUCTED_BLADE,e.position().add(0,1,0),new Vec3(16,0,-Math.PI/2-Math.toRadians(e.getYRot())),24);
        }else if(form==1){
            Vec3 from=e.getEyePosition(),end=l.clip(new net.minecraft.world.level.ClipContext(from,from.add(e.getLookAngle().scale(64)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,e)).getLocation();
            targets=MythicArts.lineTargets(l,e,from,end,1.4,8);ArtVisualEntity.spawn(l,ArtVisualEntity.RECONSTRUCTED_LANCE,from,end.subtract(from),20);
        }else {
            center=MythicArts.location(l,e,48);targets=ApexArts.enemies(l,e,center,9).stream().limit(8).toList();
            ArtVisualEntity.spawn(l,ArtVisualEntity.RECONSTRUCTED_CROWN,center,new Vec3(9,0,0),26);
        }
        for(var target:targets)MythicArts.hit(s,i,e,target,damage,mode,type);
    }
    static boolean tick(ServerLevel l,ManasSkillInstance i,LivingEntity e){
        var tag=i.getOrCreateTag();if(!tag.contains("synthesis_at"))return false;
        if(l.getGameTime()>=tag.getLong("synthesis_at")){
            int phase=tag.getInt("synthesis_phase");var plan=tag.getList("synthesis_plan",Tag.TAG_COMPOUND);
            if(plan.size()!=3){tag.remove("synthesis_at");MythicArts.endVisual(l,tag,"synthesis_visual");return false;}
            reconstruct(skill(),l,i,e,plan.getCompound(phase),ApexArts.point(tag,"synthesis"),3,true);
            if(phase<2){tag.putInt("synthesis_phase",phase+1);tag.putLong("synthesis_at",tag.getLong("synthesis_start")+20+(phase+1)*16);}else {tag.remove("synthesis_at");tag.remove("synthesis_plan");}
        }return tag.contains("synthesis_at");
    }
    public static boolean sealed(LivingEntity target,ManasSkill ability){
        var data=target.getPersistentData().getCompound("wildhunt");if(!data.contains("lugh_seal") || MythsTiers.isGodly(ability))return false;
        var seal=data.getCompound("lugh_seal");return seal.getLong("until")>target.level().getGameTime() && ability.getRegistryName()!=null && ability.getRegistryName().toString().equals(seal.getString("skill"));
    }
    public static EventResult activate(Changeable<ManasSkillInstance> instance,LivingEntity e,int key,int mode){return sealed(e,instance.get().getSkill())?EventResult.interruptFalse():EventResult.pass();}
    private static void clearSeal(LivingEntity target){var data=HunterData.of(target);var seal=data.getCompound("lugh_seal");if(target.level() instanceof ServerLevel l)MythicArts.endVisual(l,seal,"visual");data.remove("lugh_seal");}
    public static void tickSeal(LivingEntity target){
        var data=target.getPersistentData().getCompound("wildhunt");if(data.contains("lugh_seal") && (!target.isAlive() || data.getCompound("lugh_seal").getLong("until")<=target.level().getGameTime()))clearSeal(target);
    }
    static void clear(ManasSkillInstance i,LivingEntity e){
        if(!(i.getSkill() instanceof MythicSkill s) || s.kind!=MythicKind.LUGH)return;var tag=i.getOrCreateTag();
        if(e.level() instanceof ServerLevel l){
            MythicArts.endVisual(l,tag,"synthesis_visual");
            for(var id:tag.getList("sealed_targets",Tag.TAG_INT_ARRAY))if(MythicArts.entityAcrossLevels(l,NbtUtils.loadUUID(id)) instanceof LivingEntity target){var data=HunterData.of(target);var seal=data.getCompound("lugh_seal");if(seal.hasUUID("owner") && seal.getUUID("owner").equals(e.getUUID()))clearSeal(target);}
        }
        for(var key:List.of("synthesis_at","synthesis_plan","sealed_targets"))tag.remove(key);
        // Learned techniques persist with the skill; pending attacks and applied seals do not.
    }
    private static void message(LivingEntity e,String message){if(e instanceof net.minecraft.world.entity.player.Player p)p.displayClientMessage(Component.literal(message),true);}
}
