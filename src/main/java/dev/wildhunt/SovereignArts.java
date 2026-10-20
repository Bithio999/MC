package dev.wildhunt;

import dev.wildhunt.myths.MythsTiers;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.effect.template.TensuraMobEffectInstance;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.*;
import java.util.*;

/** Runic trap networks, water refraction and one-use allied rescue seeds. */
public final class SovereignArts {
    private SovereignArts() {}
    private static ListTag nodes(ManasSkillInstance i,long now){
        var list=i.getOrCreateTag().getList("anchors",Tag.TAG_COMPOUND);
        list.removeIf(v->((CompoundTag)v).getLong("until")<=now);
        i.getOrCreateTag().put("anchors",list);return list;
    }
    static boolean canCast(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        if(s.kind!=MythicKind.ANANSI)return true;
        var list=nodes(i,l.getGameTime());
        if(mode==0)return e.distanceToSqr(MythicArts.location(l,e,40))<=40*40;
        if(mode==2)return list.size()>=2;
        var anchor=aimedAnchor(l,i,e);
        return anchor!=null && MythicArts.safe(l,e,ApexArts.point(anchor,"p"));
    }
    private static CompoundTag aimedAnchor(ServerLevel l,ManasSkillInstance i,LivingEntity e){
        Vec3 eye=e.getEyePosition(),direction=e.getLookAngle();var cursor=MythicArts.location(l,e,48);CompoundTag best=null;double distance=Double.MAX_VALUE;
        for(var value:nodes(i,l.getGameTime())){
            var node=(CompoundTag)value;Vec3 p=ApexArts.point(node,"p").add(0,.8,0),delta=p.subtract(eye);double along=delta.dot(direction),offRay=delta.subtract(direction.scale(along)).lengthSqr();
            if(along<=0 || along>48 || !MythicArts.clearPath(l,e,eye,p))continue;
            double score=offRay<=1.75*1.75?along:p.distanceToSqr(cursor)<36?100+along:Double.MAX_VALUE;
            if(score<distance){distance=score;best=node;}
        }return best;
    }
    static void cast(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        var tag=i.getOrCreateTag();long now=l.getGameTime();
        if(s.kind==MythicKind.ANANSI){
            var list=nodes(i,now);
            if(mode==0){
                if(list.size()==3)removeNodeVisual(l,list.remove(0));
                var node=new CompoundTag();Vec3 p=MythicArts.location(l,e,40);ApexArts.point(node,"p",p);node.putLong("until",now+400);node.putLong("armed",now+12);
                var visual=ArtVisualEntity.spawn(l,ArtVisualEntity.FATE_ANCHOR,p,new Vec3(1,0,0),400);visual.setCaster(e);node.putUUID("visual",visual.getUUID());list.add(node);
            }else if(mode==1){
                var node=aimedAnchor(l,i,e);Vec3 before=e.position(),destination=ApexArts.point(node,"p");
                ArtVisualEntity.spawn(l,ArtVisualEntity.LOOM_TRANSIT,before.add(0,1,0),destination.subtract(before),20);
                MythicArts.teleport(e,destination);ApexArts.point(node,"p",before);node.putLong("armed",now+12);
                if(node.hasUUID("visual") && l.getEntity(node.getUUID("visual")) instanceof ArtVisualEntity v)v.updateVector(before,new Vec3(1,0,0));
            }else {
                tag.put("loom_nodes",list.copy());tag.putInt("loom_phase",0);tag.putLong("loom_at",now+24);
                var finishingVisuals=new ListTag();
                for(int n=0;n<list.size();n++){
                    var a=ApexArts.point(list.getCompound(n),"p");var b=ApexArts.point(list.getCompound((n+1)%list.size()),"p");
                    var edge=ArtVisualEntity.spawn(l,ArtVisualEntity.CHECKMATE_EDGE,a.add(0,.15,0),b.subtract(a),52);finishingVisuals.add(NbtUtils.createUUID(edge.getUUID()));
                    removeNodeVisual(l,list.getCompound(n));
                }
                tag.put("loom_visuals",finishingVisuals);list.clear();tag.remove("anansi_hits");
            }
            tag.put("anchors",list);rebuildThreads(l,i,e);
        }else if(s.kind==MythicKind.AEGIR){
            if(mode==0){
                Vec3 from=e.getEyePosition(),end=rayEnd(l,e,from,from.add(e.getLookAngle().scale(64)));var victims=new LinkedHashMap<UUID,LivingEntity>();
                harpoon(l,e,from,end,victims);
                if(tag.getLong("mirrorsea_until")>now){
                    Vec3 center=ApexArts.point(tag,"mirrorsea");Vec3 side=ApexArts.forward(e).cross(new Vec3(0,1,0)).normalize().scale(6);
                    for(int direction:new int[]{-1,1}){Vec3 origin=center.add(side.scale(direction)).add(0,2,0);harpoon(l,e,origin,rayEnd(l,e,origin,end),victims);}
                }
                for(var target:victims.values().stream().limit(8).toList())MythicArts.hit(s,i,e,target,tag.getLong("mirrorsea_until")>now?2200:1600,0,TensuraDamageTypes.WATER_ELEMENTAL);
            }else if(mode==1){
                Vec3 p=MythicArts.location(l,e,48);MythicArts.field(l,tag,"mirrorsea",p,200,ArtVisualEntity.MIRRORSEA,12);
            }else {
                boolean empowered=tag.getLong("mirrorsea_until")>now;Vec3 p=empowered?ApexArts.point(tag,"mirrorsea"):e.position();
                tag.remove("mirrorsea_until");MythicArts.endVisual(l,tag,"mirrorsea_visual");
                ApexArts.point(tag,"breaker",p);tag.putLong("breaker_start",now);tag.putLong("breaker_until",now+48);tag.putBoolean("breaker_empowered",empowered);tag.put("breaker_hits",new ListTag());
                MythicArts.endVisual(l,tag,"breaker_visual");var wave=ArtVisualEntity.spawn(l,ArtVisualEntity.SOVEREIGN_BREAKER,p,new Vec3(empowered?26:18,0,0),48).style(empowered?1:0);tag.putUUID("breaker_visual",wave.getUUID());
            }
        }
    }
    private static Vec3 rayEnd(ServerLevel l,LivingEntity e,Vec3 from,Vec3 to){return l.clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,e)).getLocation();}
    private static void harpoon(ServerLevel l,LivingEntity e,Vec3 from,Vec3 end,Map<UUID,LivingEntity> victims){
        ArtVisualEntity.spawn(l,ArtVisualEntity.REFRACTED_HARPOON,from,end.subtract(from),18);
        for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(1.1),v->HunterData.canHarm(e,v) && v.getBoundingBox().inflate(1.1).clip(from,end).isPresent() && MythicArts.clearPath(l,e,from,v.getEyePosition())).stream().sorted(Comparator.comparingDouble(v->v.distanceToSqr(from))).limit(8).toList())victims.putIfAbsent(target.getUUID(),target);
    }
    private static void removeNodeVisual(ServerLevel l,Tag value){var node=(CompoundTag)value;if(node.hasUUID("visual") && MythicArts.entityAcrossLevels(l,node.getUUID("visual")) instanceof ArtVisualEntity v)v.discard();}
    private static void rebuildThreads(ServerLevel l,ManasSkillInstance i,LivingEntity e){
        var tag=i.getOrCreateTag();for(var value:tag.getList("thread_visuals",Tag.TAG_INT_ARRAY))if(value instanceof IntArrayTag id){var entity=MythicArts.entityAcrossLevels(l,net.minecraft.nbt.NbtUtils.loadUUID(id));if(entity!=null)entity.discard();}
        var visuals=new ListTag();var list=nodes(i,l.getGameTime());int edges=list.size()==2?1:list.size()==3?3:0;
        for(int n=0;n<edges;n++){Vec3 a=ApexArts.point(list.getCompound(n),"p"),b=ApexArts.point(list.getCompound((n+1)%list.size()),"p");
            int life=(int)Math.max(1,Math.min(list.getCompound(n).getLong("until"),list.getCompound((n+1)%list.size()).getLong("until"))-l.getGameTime());
            var v=ArtVisualEntity.spawn(l,ArtVisualEntity.FATE_THREAD,a.add(0,1,0),b.subtract(a),life);v.setCaster(e);visuals.add(NbtUtils.createUUID(v.getUUID()));
        }tag.put("thread_visuals",visuals);tag.putInt("anchor_count",list.size());
    }
    static boolean tick(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e){
        var tag=i.getOrCreateTag();long now=l.getGameTime();boolean active=false;
        if(s.kind==MythicKind.ANANSI){
            var list=nodes(i,now);active=!list.isEmpty();if(tag.getInt("anchor_count")!=list.size())rebuildThreads(l,i,e);
            var hits=tag.getCompound("anansi_hits");for(var key:List.copyOf(hits.getAllKeys()))if(hits.getLong(key)<=now)hits.remove(key);
            int edges=list.size()==2?1:list.size()==3?3:0;
            for(int n=0;n<edges;n++){
                var first=list.getCompound(n);var second=list.getCompound((n+1)%list.size());if(now<Math.max(first.getLong("armed"),second.getLong("armed")))continue;
                Vec3 a=ApexArts.point(first,"p").add(0,1,0),b=ApexArts.point(second,"p").add(0,1,0);
                if(!MythicArts.clearPath(l,e,a,b))continue;
                for(var target:MythicArts.lineTargets(l,e,a,b,1.1,8)){
                    String id=target.getUUID().toString();if(hits.getLong(id)>now)continue;
                    hits.putLong(id,now+20);MythicArts.hit(s,i,e,target,1300,0,TensuraDamageTypes.SPACE_ELEMENTAL);
                    ArtVisualEntity.spawn(l,ArtVisualEntity.LOOM_TRANSIT,a,target.getEyePosition().subtract(a),14);
                }
            }tag.put("anansi_hits",hits);
            if(tag.contains("loom_at")){
                active=true;if(now>=tag.getLong("loom_at")){
                    var shape=tag.getList("loom_nodes",Tag.TAG_COMPOUND);int phase=tag.getInt("loom_phase");
                    if(shape.size()>=2){
                        Vec3 center=Vec3.ZERO;for(var value:shape)center=center.add(ApexArts.point((CompoundTag)value,"p"));center=center.scale(1d/shape.size());
                        for(var target:ApexArts.enemies(l,e,center,52).stream().filter(v->inside(shape,v.position())).limit(8).toList())MythicArts.hit(s,i,e,target,phase==0?2800:1900,2,phase==0?TensuraDamageTypes.SPACE_ELEMENTAL:TensuraDamageTypes.EARTH_ELEMENTAL);
                    }
                    if(phase==0){tag.putInt("loom_phase",1);tag.putLong("loom_at",now+16);}else {tag.remove("loom_at");tag.remove("loom_nodes");}
                }
            }
        }else if(s.kind==MythicKind.AEGIR){
            if(tag.getLong("mirrorsea_until")>now){
                active=true;Vec3 center=ApexArts.point(tag,"mirrorsea");
                for(var projectile:l.getEntitiesOfClass(Projectile.class,new AABB(center,center).inflate(12),p->p.distanceToSqr(center)<=144 && p.getOwner()!=e && (!(p.getOwner() instanceof LivingEntity owner) || HunterData.canHarm(e,owner))).stream().limit(12).toList()){
                    if(projectile.getDeltaMovement().dot(center.add(0,1,0).subtract(projectile.position()))<=0)continue;
                    Vec3 destination=projectile.getOwner() instanceof LivingEntity owner?owner.getEyePosition():projectile.position().subtract(projectile.getDeltaMovement().scale(12));
                    projectile.setOwner(e);projectile.setDeltaMovement(destination.subtract(projectile.position()).normalize().scale(Math.max(.7,projectile.getDeltaMovement().length())));projectile.hurtMarked=true;
                    ArtVisualEntity.spawn(l,ArtVisualEntity.WATER_REFRACTION,projectile.position(),new Vec3(1.2,0,0),16);
                }
            }else if(tag.contains("mirrorsea_until")){tag.remove("mirrorsea_until");MythicArts.endVisual(l,tag,"mirrorsea_visual");}
            if(tag.getLong("breaker_until")>now){
                active=true;Vec3 center=ApexArts.point(tag,"breaker");boolean empowered=tag.getBoolean("breaker_empowered");double radius=(empowered?26:18)*Math.clamp((now-tag.getLong("breaker_start"))/40d,0,1);
                var hits=tag.getList("breaker_hits",Tag.TAG_INT_ARRAY);
                for(var target:ApexArts.enemies(l,e,center,radius+1.5).stream().filter(v->Math.abs(v.getY()-center.y)<5 && Math.abs(v.position().subtract(center).horizontalDistance()-radius)<=1.7 && hits.stream().noneMatch(id->NbtUtils.loadUUID(id).equals(v.getUUID()))).limit(12).toList()){
                    hits.add(NbtUtils.createUUID(target.getUUID()));MythicArts.hit(s,i,e,target,empowered?3000:2100,2,TensuraDamageTypes.WATER_ELEMENTAL);
                    ApexArts.push(target,target.position().subtract(center).multiply(1,0,1).normalize().scale(.65).add(0,.18,0));
                }tag.put("breaker_hits",hits);
            }else {tag.remove("breaker_until");tag.remove("breaker_hits");MythicArts.endVisual(l,tag,"breaker_visual");}
        }
        return active;
    }
    static boolean inside(ListTag shape,Vec3 p){
        Vec3 a=ApexArts.point(shape.getCompound(0),"p"),b=ApexArts.point(shape.getCompound(1),"p");
        if(Math.abs(p.y-a.y)>6)return false;
        if(shape.size()==2)return segmentDistance(p,a,b)<=3.5;
        Vec3 c=ApexArts.point(shape.getCompound(2),"p");double ab=cross(a,b,p),bc=cross(b,c,p),ca=cross(c,a,p);
        return (!(ab<0 || bc<0 || ca<0) || !(ab>0 || bc>0 || ca>0)) && Math.abs(cross(a,b,c))>.01 || Math.min(segmentDistance(p,a,b),Math.min(segmentDistance(p,b,c),segmentDistance(p,c,a)))<=2;
    }
    private static double cross(Vec3 a,Vec3 b,Vec3 p){return (b.x-a.x)*(p.z-a.z)-(b.z-a.z)*(p.x-a.x);}
    private static double segmentDistance(Vec3 p,Vec3 a,Vec3 b){Vec3 d=b.subtract(a).multiply(1,0,1),q=p.subtract(a).multiply(1,0,1);double t=d.lengthSqr()<1e-6?0:Math.clamp(q.dot(d)/d.lengthSqr(),0,1);return q.subtract(d.scale(t)).length();}

    static void plantSeeds(ServerLevel l,ManasSkillInstance i,LivingEntity owner,List<LivingEntity> allies){
        clearSeeds(i,owner);var recipients=new ListTag();
        for(var ally:allies){
            var seed=new CompoundTag();seed.putUUID("owner",owner.getUUID());seed.putLong("until",l.getGameTime()+240);HunterData.of(ally).put("perennial_seed",seed);recipients.add(NbtUtils.createUUID(ally.getUUID()));
            var marker=ArtVisualEntity.spawn(l,ArtVisualEntity.PERENNIAL_SEED,ally.position(),new Vec3(1.2,0,0),240);marker.setCaster(ally);seed.putUUID("visual",marker.getUUID());cleanse(ally);
        }i.getOrCreateTag().put("seed_recipients",recipients);i.getOrCreateTag().putLong("seeds_until",l.getGameTime()+240);
    }
    public static void tickTarget(LivingEntity ally){
        var data=ally.getPersistentData().getCompound("wildhunt");if(!data.contains("perennial_seed"))return;
        var seed=data.getCompound("perennial_seed");
        if(!ally.isAlive() || seed.getLong("until")<=ally.level().getGameTime()){clearSeed(ally);return;}
        if(ally.getHealth()>ally.getMaxHealth()*.35)return;
        clearSeed(ally);MythicArts.heal(ally,.4,.4);cleanse(ally);
        if(ally.level() instanceof ServerLevel l)ArtVisualEntity.spawn(l,ArtVisualEntity.REBIRTH_BLOOM,ally.position(),new Vec3(2.5,0,0),34);
    }
    private static void clearSeed(LivingEntity ally){var data=HunterData.of(ally);if(ally.level() instanceof ServerLevel l)MythicArts.endVisual(l,data.getCompound("perennial_seed"),"visual");data.remove("perennial_seed");}
    private static void cleanse(LivingEntity ally){
        for(var effect:List.copyOf(ally.getActiveEffects())){
            if(!effect.is(MobEffects.POISON) && !effect.is(MobEffects.WITHER) && !effect.is(TensuraMobEffects.getReference(TensuraMobEffects.CHILL)) && !effect.is(TensuraMobEffects.getReference(TensuraMobEffects.WEBBED)))continue;
            var origin=((TensuraMobEffectInstance)effect).tensura$getSourceAbility();if(origin!=null && !origin.isEmpty() && MythsTiers.isGodly(origin.getSkill()))continue;
            ally.removeEffect(effect.getEffect());
        }
    }
    private static void clearSeeds(ManasSkillInstance i,LivingEntity owner){
        if(owner.level() instanceof ServerLevel l)for(var id:i.getOrCreateTag().getList("seed_recipients",Tag.TAG_INT_ARRAY)){
            UUID uuid=NbtUtils.loadUUID(id);var entity=owner.getUUID().equals(uuid)?owner:MythicArts.entityAcrossLevels(l,uuid);
            if(entity instanceof LivingEntity ally){var data=HunterData.of(ally);var seed=data.getCompound("perennial_seed");if(seed.hasUUID("owner") && seed.getUUID("owner").equals(owner.getUUID()))clearSeed(ally);}
        }i.getOrCreateTag().remove("seed_recipients");i.getOrCreateTag().remove("seeds_until");
    }
    static void clear(ManasSkillInstance i,LivingEntity e){
        var tag=i.getOrCreateTag();clearSeeds(i,e);
        if(e.level() instanceof ServerLevel l){for(var node:tag.getList("anchors",Tag.TAG_COMPOUND))removeNodeVisual(l,node);tag.remove("anchors");rebuildThreads(l,i,e);MythicArts.endVisual(l,tag,"mirrorsea_visual");MythicArts.endVisual(l,tag,"breaker_visual");for(var id:tag.getList("loom_visuals",Tag.TAG_INT_ARRAY)){var entity=MythicArts.entityAcrossLevels(l,NbtUtils.loadUUID(id));if(entity!=null)entity.discard();}}
        for(String key:List.of("anchors","thread_visuals","anansi_hits","loom_at","loom_nodes","loom_visuals","mirrorsea_until","breaker_until","breaker_hits"))tag.remove(key);
    }
}
