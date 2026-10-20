package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.*;

/** Distinct, stack-free targeting and short-lived world effects. */
public final class ApexArts {
    private ApexArts() {}
    static Vec3 forward(LivingEntity e) {
        Vec3 look = e.getLookAngle().multiply(1, 0, 1).normalize();
        return look.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : look;
    }
    static Vec3 point(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "_x"), tag.getDouble(key + "_y"), tag.getDouble(key + "_z"));
    }
    static void point(CompoundTag tag, String key, Vec3 p) {
        tag.putDouble(key + "_x", p.x); tag.putDouble(key + "_y", p.y); tag.putDouble(key + "_z", p.z);
    }
    static Vec3 aim(ServerLevel level, LivingEntity caster, double range) {
        Vec3 from = caster.getEyePosition(), to = from.add(caster.getLookAngle().scale(range));
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation().subtract(0, 1.2, 0);
    }
    /** Surface under the crosshair; a miss ends at the bounded ray endpoint. */
    static Vec3 cursorPlacement(ServerLevel level, LivingEntity caster, double range) {
        Vec3 eye = caster.getEyePosition(), end = eye.add(caster.getLookAngle().normalize().scale(range));
        var hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
            return hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.05));
        return end;
    }
    static List<LivingEntity> enemies(ServerLevel level, LivingEntity caster, Vec3 center, double range) {
        return enemies(level, caster, center, range, true);
    }
    static List<LivingEntity> enemies(ServerLevel level, LivingEntity caster, Vec3 center, double range, boolean lineOfSight) {
        var list = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(range),
            e -> HunterData.canHarm(caster, e) && e.position().distanceToSqr(center) <= range * range && (!lineOfSight || caster.hasLineOfSight(e)));
        list.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));
        return list;
    }
    static LivingEntity target(ServerLevel level, LivingEntity caster, double range) {
        Vec3 from = caster.getEyePosition(), to = from.add(caster.getLookAngle().scale(range));
        Vec3 end = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1),
                e -> HunterData.canHarm(caster, e) && caster.hasLineOfSight(e)
                    && e.getBoundingBox().inflate(0.6).clip(from, end).isPresent())
            .stream().min(Comparator.comparingDouble(e -> e.getBoundingBox().inflate(0.6).clip(from, end).orElse(end).distanceToSqr(from))).orElse(null);
    }
    static void push(LivingEntity target, Vec3 vector) {
        double resistance = Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
        target.push(vector.x * (1 - resistance), vector.y * (1 - resistance), vector.z * (1 - resistance)); target.hurtMarked = true;
    }
    public static void primary(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime();
        switch (skill.apex) {
            case REY_DAU -> ricochet(skill, level, instance, caster);
            case UTH_DUNA -> {
                Vec3 direction = forward(caster), eye = caster.getEyePosition();
                point(tag, "travel_origin", caster.position()); point(tag, "travel_forward", direction);
                double reach = skill.ultimate ? 26 : 18;
                Vec3 end = level.clip(new ClipContext(eye, eye.add(direction.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
                tag.putDouble("travel_reach", Math.min(reach, eye.distanceTo(end)));
                tag.putString("travel_dimension", dimension(level)); tag.putLong("travel_until", now + 16); tag.putInt("travel_step", 0);
                ApexVisuals.tidalWave(level, caster.position(), direction, tag.getDouble("travel_reach"), skill.ultimate ? 5 : 4);
                tag.put("travel_hits", new ListTag()); advance(skill, level, instance, caster);
            }
            case NU_UDRA -> {
                Vec3 origin = caster.position().add(0, 0.7, 0), puddle = aim(level, caster, 10); int hits = 0;
                for (var enemy : enemies(level, caster, caster.position(), skill.ultimate ? 22 : 14)) {
                    if (enemy.getEyePosition().subtract(caster.getEyePosition()).normalize().dot(caster.getLookAngle()) < 0.55) continue;
                    ApexVisuals.lash(level, origin, enemy.getEyePosition());
                    if (skill.strike(level, instance, caster, enemy, skill.balance.primary(), 0, false)) {
                        push(enemy, caster.position().subtract(enemy.position()).normalize().scale(0.65)); puddle = enemy.position();
                    }
                    if (++hits >= (skill.ultimate ? 6 : 4)) break;
                }
                if (hits == 0) ApexVisuals.lash(level, origin, puddle.add(0, 0.8, 0));
                addPool(skill, level, instance, caster, puddle, false, 300);
            }
            case JIN_DAHAAD -> {
                var victim = target(level, caster, skill.ultimate ? 24 : 16);
                Vec3 center = victim == null ? aim(level, caster, skill.ultimate ? 24 : 16) : victim.position();
                ApexVisuals.iceBurst(level, caster, center, skill.ultimate ? 6 : 4);
                for (var enemy : enemies(level, caster, center, skill.ultimate ? 6 : 4)) {
                    if (skill.strike(level, instance, caster, enemy, skill.balance.primary(), 0, false))
                        push(enemy, enemy.position().subtract(center).multiply(1, 0, 1).normalize().scale(0.5).add(0, skill.ultimate ? 1.2 : 0.8, 0));
                }
            }
        }
    }
    static void ricochet(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        LivingEntity current = target(level, caster, skill.ultimate ? 56 : 36);
        Vec3 from = caster.getEyePosition(); Set<UUID> visited = new HashSet<>();
        if (current == null) {
            Vec3 to = level.clip(new ClipContext(from, from.add(caster.getLookAngle().scale(skill.ultimate ? 56 : 36)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
            ApexVisuals.beam(level, skill.apex, from, to, skill.ultimate); return;
        }
        for (int bounce = 0; current != null && bounce < (skill.ultimate ? 6 : 4); bounce++) {
            visited.add(current.getUUID()); ApexVisuals.beam(level, skill.apex, from, current.getEyePosition(), skill.ultimate);
            skill.strike(level, instance, caster, current, skill.balance.primary() * (float)Math.pow(0.9, bounce), 0, false);
            LivingEntity previous = current; from = current.getEyePosition();
            current = enemies(level, caster, previous.position(), 12, false).stream()
                .filter(e -> !visited.contains(e.getUUID())).findFirst().orElse(null);
        }
    }
    static void thunderStep(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        Vec3 start = caster.position(), direction = caster.getLookAngle().normalize(), end = start;
        // Sweep the body, not just the eyes: never dash through walls or low ceilings.
        for (double d = 0.5; d <= (skill.ultimate ? 24 : 16); d += 0.5) {
            Vec3 delta = direction.scale(d); AABB box = caster.getBoundingBox().move(delta);
            if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                    || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(caster, box)) break;
            end = start.add(delta);
        }
        Vec3 segment = end.subtract(start); double length = segment.lengthSqr();
        for (var enemy : enemies(level, caster, start.add(end).scale(0.5), Math.sqrt(length) / 2 + 3)) {
            double t = length < 0.01 ? 0 : Math.clamp(enemy.position().subtract(start).dot(segment) / length, 0, 1);
            if (enemy.position().distanceToSqr(start.add(segment.scale(t))) <= 2.5 * 2.5)
                skill.strike(level, instance, caster, enemy, skill.balance.burst(), 1, false);
        }
        Vec3 momentum = caster.getDeltaMovement();
        caster.teleportTo(end.x, end.y, end.z); caster.fallDistance = 0;
        caster.setDeltaMovement(momentum); caster.hasImpulse = true; caster.hurtMarked = true;
        if (caster instanceof net.minecraft.server.level.ServerPlayer player)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(caster));
        ApexVisuals.thunderStep(level,start.add(0,.8,0),end.add(0,.8,0));
        ApexVisuals.impact(level, skill.apex, end.add(0, 1, 0), skill.ultimate);
    }
    public static void secondary(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime();
        switch (skill.apex) {
            case REY_DAU -> thunderStep(skill, level, instance, caster);
            case UTH_DUNA -> {
                Vec3 center = cursorPlacement(level, caster, skill.ultimate ? 36 : 24); point(tag, "field", center);
                tag.putLong("field_until", now + 120); tag.putLong("field_next", now + 20); tag.putString("field_dimension", dimension(level));
                ApexVisuals.whirlpool(level, center, skill.radius(), 120);
                skill.area(level, instance, caster, center, skill.radius(), skill.balance.burst(), 1, false);
            }
            case NU_UDRA -> {
                boolean usable = false;
                for (Tag raw : tag.getList("oil_pools", Tag.TAG_COMPOUND)) {
                    var pool = (CompoundTag)raw;
                    if (validPool(pool, level, now) && point(pool, "pool").distanceToSqr(caster.position()) <= 32 * 32) usable = true;
                }
                if (!usable) addPool(skill, level, instance, caster, aim(level, caster, 6), false, 300);
                for (Tag raw : tag.getList("oil_pools", Tag.TAG_COMPOUND)) {
                    var pool = (CompoundTag)raw;
                    if (validPool(pool, level, now) && point(pool, "pool").distanceToSqr(caster.position()) <= 32 * 32) {
                        pool.putBoolean("lit", true); pool.putLong("until", now + 120);updatePoolVisual(level,pool,caster);
                    }
                }
                tag.putLong("field_until", now + 120); tag.putLong("field_next", now + 20); tag.putString("field_dimension", dimension(level));
                pools(skill, level, instance, caster, skill.balance.burst(), 1);
            }
            case JIN_DAHAAD -> {
                var victim = target(level, caster, skill.ultimate ? 28 : 20);
                if (victim == null) { ApexVisuals.iceBurst(level, caster, aim(level, caster, 12), 2); return; }
                if (skill.strike(level, instance, caster, victim, skill.balance.burst(), 1, false)) {
                    skill.addEffect(victim, instance, 1, caster, TensuraMobEffects.getReference(TensuraMobEffects.FROST), skill.ultimate ? 120 : 80, 0);
                    ApexVisuals.prison(level, caster, victim.position(), skill.ultimate ? 120 : 80);
                }
            }
        }
    }
    static String dimension(ServerLevel level) { return level.dimension().location().toString(); }
    public static void domain(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime();
        tag.putLong("domain_until", now + 200); tag.putLong("domain_next", now + 20);
        tag.putString("domain_dimension", dimension(level)); point(tag, "domain", caster.position()); tag.putInt("tide_phase", 0);
        if (skill.apex == Apex.NU_UDRA) {
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5;
                addPool(skill, level, instance, caster, caster.position().add(Math.cos(a) * 6, 0, Math.sin(a) * 6), true, 200);
            }
        }
        if (skill.apex == Apex.JIN_DAHAAD) {
            var victim = target(level, caster, 24); point(tag, "domain", victim == null ? aim(level, caster, 16) : victim.position());
        }
        ApexVisuals.circle(level, caster, skill.apex, point(tag, "domain").add(0, 0.1, 0), skill.apex == Apex.NU_UDRA ? 4 : 8, 200, false);
        pulseDomain(skill, level, instance, caster, true);
    }
    static boolean validPool(CompoundTag pool, ServerLevel level, long now) {
        return pool.getLong("until") > now && pool.getString("dimension").equals(dimension(level));
    }
    static void addPool(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, Vec3 p, boolean lit, int duration) {
        ListTag list = instance.getOrCreateTag().getList("oil_pools", Tag.TAG_COMPOUND);
        while (list.size() >= (skill.ultimate ? 5 : 3)) {finishPoolVisual(level,(CompoundTag)list.getFirst());list.removeFirst();}
        var pool = new CompoundTag(); point(pool, "pool", p); pool.putBoolean("lit", lit);
        pool.putLong("until", level.getGameTime() + duration); pool.putString("dimension", dimension(level));
        list.add(pool); instance.getOrCreateTag().put("oil_pools", list);
        updatePoolVisual(level,pool,caster);instance.markDirty();
    }
    private static void updatePoolVisual(ServerLevel level,CompoundTag pool,LivingEntity caster){
        if(!caster.isAlive())return;
        Vec3 stored=point(pool,"pool");
        var ground=level.clip(new ClipContext(stored.add(0,2,0),stored.add(0,-.5,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,caster));
        // Miss targeting historically records below the hit surface; only lift the cosmetic mesh.
        Vec3 surface=ground.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?ground.getLocation().add(0,.025,0):stored;
        ArtVisualEntity visual=pool.hasUUID("visual") && level.getEntity(pool.getUUID("visual")) instanceof ArtVisualEntity existing?existing:null;
        if(visual==null){visual=ArtVisualEntity.spawn(level,ArtVisualEntity.OIL_POOL,surface,new Vec3(pool.getBoolean("lit")?3:2.5,0,0),30);pool.putUUID("visual",visual.getUUID());}
        visual.updateVector(surface,new Vec3(pool.getBoolean("lit")?3:2.5,0,0));visual.style(pool.getBoolean("lit")?1:0);visual.remaining((int)Math.max(1,Math.min(30,pool.getLong("until")-level.getGameTime())));
    }
    private static void finishPoolVisual(ServerLevel level,CompoundTag pool){if(pool.hasUUID("visual") && level.getEntity(pool.getUUID("visual")) instanceof ArtVisualEntity visual)visual.remaining(6);}
    static void clearPoolVisuals(ManasSkillInstance instance,LivingEntity caster){if(caster.level() instanceof ServerLevel level)for(var raw:instance.getOrCreateTag().getList("oil_pools",Tag.TAG_COMPOUND))finishPoolVisual(level,(CompoundTag)raw);}
    static void pools(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, float damage, int mode) {
        Set<UUID> hit = new HashSet<>();
        for (Tag raw : instance.getOrCreateTag().getList("oil_pools", Tag.TAG_COMPOUND)) {
            var pool = (CompoundTag)raw;
            if (!validPool(pool, level, level.getGameTime()) || !pool.getBoolean("lit")) continue;
            Vec3 center = point(pool, "pool"); if (center.distanceToSqr(caster.position()) > 32 * 32) continue;
            for (var enemy : enemies(level, caster, center, skill.ultimate ? 4 : 3))
                if (hit.add(enemy.getUUID())) skill.strike(level, instance, caster, enemy, damage, mode, true);
            ApexVisuals.impact(level, skill.apex, center, skill.ultimate);
        }
    }
    static void advance(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); int step = tag.getInt("travel_step") + 1; tag.putInt("travel_step", step);
        Vec3 origin = point(tag, "travel_origin"), direction = point(tag, "travel_forward"), right = direction.yRot((float)(Math.PI / 2));
        double reach = tag.getDouble("travel_reach"), initial = Math.min(2, reach);
        double distance = initial + (reach - initial) * (step - 1) / 15;
        double previous = step == 1 ? 0 : initial + (reach - initial) * (step - 2) / 15;
        Vec3 front = origin.add(direction.scale(distance)), midpoint = origin.add(direction.scale((previous + distance) / 2));
        double halfLength = (distance - previous) / 2 + 0.6, width = skill.ultimate ? 5 : 4;
        ListTag hit = tag.getList("travel_hits", Tag.TAG_STRING);
        for (var enemy : enemies(level, caster, midpoint, Math.sqrt(halfLength * halfLength + width * width + 6.25))) {
            Vec3 d = enemy.position().subtract(midpoint);
            if (Math.abs(d.dot(direction)) > halfLength || Math.abs(d.dot(right)) > width || Math.abs(d.y) > 2.5 || hit.contains(StringTag.valueOf(enemy.getStringUUID()))) continue;
            if (skill.strike(level, instance, caster, enemy, skill.balance.primary(), 0, false)) {
                hit.add(StringTag.valueOf(enemy.getStringUUID())); push(enemy, direction.scale(0.85).add(0, 0.12, 0));
            }
        }
        tag.put("travel_hits", hit); ApexVisuals.wave(level, front, right, width); instance.markDirty();
    }
    static void pulseDomain(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, boolean first) {
        var tag = instance.getOrCreateTag(); float damage = first ? skill.balance.burst() : skill.balance.pulse() * 1.5f;
        switch (skill.apex) {
            case REY_DAU -> {
                var targets = enemies(level, caster, caster.position(), 14);
                targets.sort(Comparator.comparingDouble(LivingEntity::getMaxHealth).reversed());
                for (int i = 0; i < Math.min(5, targets.size()); i++) {
                    var victim = targets.get(i); skill.strike(level, instance, caster, victim, damage, 2, true);
                    ApexVisuals.beam(level, skill.apex, victim.position().add(0, 8, 0), victim.getEyePosition(), true);
                }
            }
            case UTH_DUNA -> {
                int phase = tag.getInt("tide_phase"); tag.putInt("tide_phase", phase + 1);
                for (var victim : enemies(level, caster, caster.position(), 14)) {
                    skill.strike(level, instance, caster, victim, damage, 2, true);
                    Vec3 direction = victim.position().subtract(caster.position()).multiply(1, 0, 1).normalize();
                    push(victim, direction.scale(phase % 2 == 0 ? -0.8 : 1.1).add(0, phase % 2 == 0 ? 0 : 0.2, 0));
                }
            }
            case NU_UDRA -> pools(skill, level, instance, caster, damage, 2);
            case JIN_DAHAAD -> {
                // Telegraph fixed positions: an enemy can leave before the ice lands.
                var victims = enemies(level, caster, point(tag, "domain"), 10);
                ListTag impacts = tag.getList("icefalls", Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(3, victims.size()); i++) {
                    Vec3 p = victims.get(i).position(); var mark = new CompoundTag(); point(mark, "impact", p);
                    mark.putLong("at", level.getGameTime() + 10); mark.putFloat("damage", first ? 100 : 45);
                    mark.putString("dimension", dimension(level)); impacts.add(mark);
                    ApexVisuals.circle(level, caster, skill.apex, p.add(0, 0.1, 0), 2.5f, 10, false);
                    ApexVisuals.fallingIce(level, p);
                }
                tag.put("icefalls", impacts);
            }
        }
        instance.markDirty();
    }
    static void resolveIcefalls(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var marks = instance.getOrCreateTag().getList("icefalls", Tag.TAG_COMPOUND); Set<UUID> hit = new HashSet<>();
        for (int i = marks.size() - 1; i >= 0; i--) {
            var mark = marks.getCompound(i);
            if (!mark.getString("dimension").equals(dimension(level))) { marks.remove(i); instance.markDirty(); continue; }
            if (mark.getLong("at") > level.getGameTime()) continue;
            Vec3 p = point(mark, "impact"); ApexVisuals.iceBurst(level, caster, p, 2.5);
            for (var victim : enemies(level, caster, p, 2.5))
                if (hit.add(victim.getUUID())) skill.strike(level, instance, caster, victim, mark.getFloat("damage"), 2, true);
            marks.remove(i); instance.markDirty();
        }
    }
    public static boolean tick(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime(); boolean active = false;
        if (tag.getLong("travel_until") > now && tag.getInt("travel_step") < 16 && tag.getString("travel_dimension").equals(dimension(level))) {
            active = true; advance(skill, level, instance, caster);
        }
        ListTag pools = tag.getList("oil_pools", Tag.TAG_COMPOUND); int oldCount = pools.size();
        for (int i = pools.size() - 1; i >= 0; i--) if (!validPool(pools.getCompound(i), level, now)) {finishPoolVisual(level,pools.getCompound(i));pools.remove(i);}
        if (pools.size() != oldCount) instance.markDirty();
        if (!pools.isEmpty()) {
            active = true;
            if (now % 20 == 0) for (Tag raw : pools) {
                var pool = (CompoundTag)raw;
                updatePoolVisual(level,pool,caster);
            }
        }
        boolean domain = tag.getLong("domain_until") > now && tag.getString("domain_dimension").equals(dimension(level));
        if (tag.getLong("field_until") > now && tag.getString("field_dimension").equals(dimension(level))) {
            active = true;
            if (skill.apex == Apex.UTH_DUNA && now % 4 == 0) {
                Vec3 center = point(tag, "field");
                for (var victim : enemies(level, caster, center, skill.radius())) push(victim, center.subtract(victim.position()).multiply(1, 0, 1).normalize().scale(0.22));
            }
            if (now >= tag.getLong("field_next")) {
                if (skill.apex == Apex.UTH_DUNA) skill.area(level, instance, caster, point(tag, "field"), skill.radius(), skill.balance.pulse(), 1, true);
                if (skill.apex == Apex.NU_UDRA && !domain) pools(skill, level, instance, caster, skill.balance.pulse(), 1);
                tag.putLong("field_next", now + 20); instance.markDirty();
            }
            if (now % 8 == 0 && skill.apex == Apex.UTH_DUNA) ApexVisuals.ring(level, skill.apex, point(tag, "field"), skill.radius(), 0.2, skill.ultimate);
        }
        if (domain) {
            active = true;
            if (now >= tag.getLong("domain_next")) {
                pulseDomain(skill, level, instance, caster, false); tag.putLong("domain_next", now + 20); instance.markDirty();
            }
            if (now % 8 == 0) ApexVisuals.domain(level, skill.apex, skill.apex == Apex.JIN_DAHAAD ? point(tag, "domain") : caster.position(), skill.apex == Apex.JIN_DAHAAD ? 10 : 14);
        }
        if (!tag.getList("icefalls", Tag.TAG_COMPOUND).isEmpty()) {
            resolveIcefalls(skill, level, instance, caster); active |= !tag.getList("icefalls", Tag.TAG_COMPOUND).isEmpty();
        }
        return active;
    }
    private static boolean passiveEnabled(LivingEntity entity, Apex apex) {
        if (!entity.getPersistentData().getCompound("wildhunt").getBoolean("active_skills")) return false;
        return SkillAPI.getSkillsFrom(entity).getLearnedSkills().stream().anyMatch(instance ->
            instance.isToggled() && instance.getSkill() instanceof ApexSkill skill && skill.apex == apex);
    }
    public static void onBreathe(net.neoforged.neoforge.event.entity.living.LivingBreatheEvent event) {
        if (passiveEnabled(event.getEntity(), Apex.UTH_DUNA)) {
            event.setCanBreathe(true); event.setConsumeAirAmount(0); event.setRefillAirAmount(4);
        }
    }
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getAmount() > 0) {
            for (var instance : SkillAPI.getSkillsFrom(event.getEntity()).getLearnedSkills()) {
                if (instance.isToggled() && instance.getSkill() instanceof ApexSkill skill && skill.barrierCapacity() > 0) {
                    instance.getOrCreateTag().putLong("barrier_ready", event.getEntity().level().getGameTime() + 200);
                    instance.markDirty();
                }
            }
        }
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && passiveEnabled(event.getEntity(), Apex.NU_UDRA)) event.setCanceled(true);
    }
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getNewDamage() <= 0 || !event.getEntity().isAlive()) return;
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !HunterData.canHarm(victim, attacker)) return;
        for (var instance : SkillAPI.getSkillsFrom(victim).getLearnedSkills()) {
            if (!(instance.getSkill() instanceof ApexSkill skill) || skill.apex != Apex.UTH_DUNA || !instance.isToggled()) continue;
            var tag = instance.getOrCreateTag(); long now = level.getGameTime();
            if (tag.getLong("counter_next") > now || victim.distanceToSqr(attacker) > 144 || !victim.hasLineOfSight(attacker)) continue;
            tag.putLong("counter_next", now + (skill.ultimate ? 20 : 60)); instance.markDirty();
            // Retaliation belongs to the passive; mode 1 supplies native source metadata.
            skill.strike(level, instance, victim, attacker, skill.ultimate ? 25 : 100, 1, false);
            push(attacker, attacker.position().subtract(victim.position()).normalize().scale(0.8));
        }
    }
}
