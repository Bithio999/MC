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
    public static ListTag archive(ManasSkillInstance i){
        var tag=i.getOrCreateTag();var list=tag.getList("archive",Tag.TAG_COMPOUND);
        if(list.size()!=3){
            list=new ListTag();list.add(pattern("Sovereign Edge","wildhunt:origin_edge",TensuraDamageTypes.AURA_SLASH.location(),0,3800));
            list.add(pattern("Heavenly Thread","wildhunt:origin_thread",TensuraDamageTypes.SPACE_ELEMENTAL.location(),1,3800));
            list.add(pattern("Founder's Rune","wildhunt:origin_rune",TensuraDamageTypes.EARTH_ELEMENTAL.location(),2,4200));tag.put("archive",list);
        }
        // Upgrade only untouched origin records. Appraised techniques keep their identity,
        // type and form; all saved intensities are brought into the current bounded budget.
        for(var value:list){
            var record=(CompoundTag)value;float damage=record.getFloat("damage");
            damage=switch(record.getString("source")){
                case "wildhunt:origin_edge","wildhunt:origin_thread" -> damage==2200?3800:damage;
                case "wildhunt:origin_rune" -> damage==2400?4200:damage;
                default -> damage;
            };
            record.putFloat("damage",bounded(damage));
        }
        return list;
    }
    public static String selectedName(ManasSkillInstance i){return archive(i).getCompound(Math.floorMod(i.getOrCreateTag().getInt("selected_art"),3)).getString("name");}
    public static void cycle(ManasSkillInstance i,LivingEntity e){
        i.getOrCreateTag().putInt("selected_art",Math.floorMod(i.getOrCreateTag().getInt("selected_art")+1,3));i.markDirty();message(e,"Reconstructed Art: "+selectedName(i));
    }
    /** Record the most recent actual damaging technique, before this addon's Godly bonus. */
    public static EventResult observeDamage(Skills storage,LivingEntity target,DamageSource source,Changeable<Float> amount){
        if(target.level().isClientSide() || !(source.getEntity() instanceof LivingEntity caster) || !Float.isFinite(amount.get()) || amount.get()<=0)return EventResult.pass();
        var instance=((TensuraDamageSource)source).tensura$getAbilityInstance();
        if(instance==null || instance.getSkill()==skill() || instance.getSkill().getRegistryName()==null || !HunterData.canHarm(caster,target))return EventResult.pass();
        var id=source.typeHolder().unwrapKey().map(ResourceKey::location).orElse(TensuraDamageTypes.MAGIC_GENERIC.location());
        boolean physical=TensuraDamageHelper.isPhysicalAttack(source);if(!SAFE_TYPES.contains(id))id=physical?TensuraDamageTypes.AURA_SLASH.location():TensuraDamageTypes.MAGIC_GENERIC.location();
        var old=HunterData.of(caster).getCompound("last_technique");String sourceId=instance.getSkillId().toString();long now=caster.level().getGameTime();
        boolean spread=old.getLong("time")==now && old.getString("source").equals(sourceId) && old.hasUUID("victim") && !old.getUUID("victim").equals(target.getUUID());
        int form=spread?2:source.getDirectEntity() instanceof Projectile || caster.distanceToSqr(target)>64?1:physical?0:2;
        var record=pattern(instance.getSkill().getName().getString(),sourceId,id,form,amount.get()*.8f);
        record.putLong("time",now);record.putUUID("victim",target.getUUID());HunterData.of(caster).put("last_technique",record);return EventResult.pass();
    }
    private static CompoundTag recent(LivingEntity target){
        var record=HunterData.of(target).getCompound("last_technique");long age=target.level().getGameTime()-record.getLong("time");
        if(!record.contains("source") || age<0 || age>600)return null;
        var id=ResourceLocation.tryParse(record.getString("source"));return id==null || SkillAPI.getSkillRegistry().get(id)==null?null:record;
    }
    static boolean canCast(ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        archive(i);if(mode==1 || mode==3)return true;
        var target=ApexArts.target(l,e,48);var record=target==null?null:recent(target);
        if(record==null){message(e,"Aim at an enemy that used a damaging technique in the last 30 seconds.");return false;}
        if(mode==2 && MythsTiers.isGodly(SkillAPI.getSkillRegistry().get(ResourceLocation.parse(record.getString("source"))))){message(e,"Countermand cannot seal a Godly technique.");return false;}
        return true;
    }
    static void cast(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        var tag=i.getOrCreateTag();long now=l.getGameTime();var list=archive(i);
        if(mode==0){
            var target=ApexArts.target(l,e,48);var record=recent(target).copy();int selected=Math.floorMod(tag.getInt("selected_art"),3);
            for(int n=0;n<3;n++)if(list.getCompound(n).getString("source").equals(record.getString("source"))){selected=n;break;}
            list.set(selected,pattern(record.getString("name"),record.getString("source"),safeType(record).location(),record.getInt("form"),record.getFloat("damage")));tag.put("archive",list);tag.putInt("selected_art",selected);
            ArtVisualEntity.spawn(l,ArtVisualEntity.SKILL_APPRAISAL,target.position(),new Vec3(2.2,0,0),40).style(record.getInt("form"));
            ArtVisualEntity.spawn(l,ArtVisualEntity.ARTISAN_THREAD,e.getEyePosition(),target.getEyePosition().subtract(e.getEyePosition()),22);
            message(e,"Archived "+selectedName(i)+" as "+formName(record.getInt("form"))+". Crouch + Reconstructed Art to select another entry.");
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
